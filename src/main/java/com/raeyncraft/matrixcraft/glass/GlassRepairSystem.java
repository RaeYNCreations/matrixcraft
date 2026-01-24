package com.raeyncraft.matrixcraft.glass;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

/**
 * Tracks glass blocks near players and repairs them when destroyed.
 * Uses both event-based detection AND polling for maximum coverage.
 */
@EventBusSubscriber(modid = MatrixCraftMod.MODID)
public class GlassRepairSystem {
    
    private static final Map<ServerLevel, GlassTracker> trackers = new HashMap<>();
    private static int repairDelayTicks = 60; // Default 3 seconds
    private static boolean enabled = true;
    private static int scanRadius = 64;
    private static int tickCounter = 0;
    
    private static class GlassTracker {
        // Maps position -> original block state
        Map<BlockPos, BlockState> knownGlass = new HashMap<>();
        // Glass waiting to be repaired
        List<BrokenGlass> brokenGlass = new ArrayList<>();
        // Snapshot of glass states from last tick for change detection
        Map<BlockPos, BlockState> lastTickSnapshot = new HashMap<>();
    }
    
    private static class BrokenGlass {
        BlockPos position;
        BlockState originalState;
        int ticksUntilRepair;
        
        BrokenGlass(BlockPos pos, BlockState state, int delay) {
            this.position = pos.immutable();
            this.originalState = state;
            this.ticksUntilRepair = delay;
        }
    }
    
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            trackers.putIfAbsent(serverLevel, new GlassTracker());
            MatrixCraftMod.LOGGER.info("[GlassRepair] Initialized for level: " + serverLevel.dimension().location());
        }
    }
    
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            trackers.remove(serverLevel);
            MatrixCraftMod.LOGGER.info("[GlassRepair] Unloaded tracker for level: " + serverLevel.dimension().location());
        }
    }
    
    /**
     * EVENT-BASED DETECTION: Catches normal block breaks (player, explosion, etc.)
     * This won't catch TacZ bullets if they bypass the event system, but it's a good backup.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!enabled) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        
        BlockState state = event.getState();
        if (!isGlassBlock(state.getBlock())) return;
        
        BlockPos pos = event.getPos().immutable();
        GlassTracker tracker = trackers.get(serverLevel);
        if (tracker == null) return;
        
        // Schedule repair
        tracker.brokenGlass.add(new BrokenGlass(pos, state, repairDelayTicks));
        tracker.knownGlass.remove(pos);
        
        MatrixCraftMod.LOGGER.info("[GlassRepair] EVENT: Glass broken at " + pos + " by " + 
            (event.getPlayer() != null ? event.getPlayer().getName().getString() : "unknown") +
            " - will repair in " + (repairDelayTicks / 20) + " seconds");
    }
    
    /**
     * Also listen for NeighborNotify events which fire when adjacent blocks change
     * This can help catch some edge cases
     */
    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!enabled) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        
        GlassTracker tracker = trackers.get(serverLevel);
        if (tracker == null) return;
        
        BlockPos changedPos = event.getPos().immutable();
        
        // Check if any known glass was at this position
        BlockState knownState = tracker.knownGlass.get(changedPos);
        if (knownState != null) {
            BlockState currentState = serverLevel.getBlockState(changedPos);
            if (!isGlassBlock(currentState.getBlock())) {
                // Glass was here, now it's gone!
                tracker.brokenGlass.add(new BrokenGlass(changedPos, knownState, repairDelayTicks));
                tracker.knownGlass.remove(changedPos);
                MatrixCraftMod.LOGGER.info("[GlassRepair] NEIGHBOR: Detected glass removal at " + changedPos);
            }
        }
    }
    
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!enabled) return;
        
        tickCounter++;
        
        // EVERY TICK: Check for direct block changes (catches TacZ bullets!)
        checkForDirectBlockChanges();
        
        // Every 20 ticks: Full scan for new glass
        if (tickCounter % 20 == 0) {
            scanForGlassNearPlayers();
        }
        
        // Every tick: Process repairs
        processRepairs();
    }
    
    /**
     * DIRECT CHANGE DETECTION: Compares current block states to our snapshot
     * This catches TacZ bullets and anything else that bypasses events!
     */
    private static void checkForDirectBlockChanges() {
        for (Map.Entry<ServerLevel, GlassTracker> entry : trackers.entrySet()) {
            ServerLevel level = entry.getKey();
            GlassTracker tracker = entry.getValue();
            
            if (level.players().isEmpty()) continue;
            
            // Check all known glass positions
            Iterator<Map.Entry<BlockPos, BlockState>> iterator = tracker.knownGlass.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, BlockState> glassEntry = iterator.next();
                BlockPos pos = glassEntry.getKey();
                BlockState expectedState = glassEntry.getValue();
                
                // Check the actual block
                BlockState currentState = level.getBlockState(pos);
                
                if (!isGlassBlock(currentState.getBlock())) {
                    // Glass is GONE!
                    if (currentState.isAir()) {
                        // It was destroyed - schedule repair
                        // Check if we already have this pending
                        boolean alreadyPending = tracker.brokenGlass.stream()
                            .anyMatch(bg -> bg.position.equals(pos));
                        
                        if (!alreadyPending) {
                            tracker.brokenGlass.add(new BrokenGlass(pos, expectedState, repairDelayTicks));
                            MatrixCraftMod.LOGGER.info("[GlassRepair] DIRECT: Detected glass destruction at " + pos + 
                                " - will repair in " + (repairDelayTicks / 20) + " seconds");
                        }
                    } else {
                        // Replaced with something else
                        MatrixCraftMod.LOGGER.info("[GlassRepair] Glass at " + pos + 
                            " was replaced with " + currentState.getBlock().getName().getString());
                    }
                    iterator.remove();
                }
            }
        }
    }
    
    /**
     * Scan for new glass near players - only adds NEW glass, doesn't remove
     * OPTIMIZED: Only scans a subset of blocks per tick to reduce lag
     */
    private static void scanForGlassNearPlayers() {
        for (Map.Entry<ServerLevel, GlassTracker> entry : trackers.entrySet()) {
            ServerLevel level = entry.getKey();
            GlassTracker tracker = entry.getValue();
            
            if (level.players().isEmpty()) continue;
            
            int newGlassFound = 0;
            int blocksChecked = 0;
            
            // Scan around each player - use a smaller area with complete coverage
            // 32 blocks = reasonable for most builds, and much faster
            int effectiveRadius = Math.min(scanRadius, 32);
            
            for (ServerPlayer player : level.players()) {
                BlockPos playerPos = player.blockPosition();
                
                // Full scan but with reasonable radius
                for (int x = -effectiveRadius; x <= effectiveRadius; x++) {
                    for (int y = -effectiveRadius; y <= effectiveRadius; y++) {
                        for (int z = -effectiveRadius; z <= effectiveRadius; z++) {
                            BlockPos checkPos = playerPos.offset(x, y, z);
                            blocksChecked++;
                            
                            // Skip if already tracked
                            BlockPos immutablePos = checkPos.immutable();
                            if (tracker.knownGlass.containsKey(immutablePos)) {
                                continue;
                            }
                            
                            BlockState state = level.getBlockState(checkPos);
                            
                            if (isGlassBlock(state.getBlock())) {
                                // Only track if not pending repair
                                boolean isPending = tracker.brokenGlass.stream()
                                    .anyMatch(bg -> bg.position.equals(immutablePos));
                                
                                if (!isPending) {
                                    tracker.knownGlass.put(immutablePos, state);
                                    newGlassFound++;
                                }
                            }
                        }
                    }
                }
            }
            
            // Log status every 5 seconds
            if (tickCounter % 100 == 0) {
                MatrixCraftMod.LOGGER.info("[GlassRepair] Status: " + 
                    tracker.knownGlass.size() + " glass tracked, " + 
                    tracker.brokenGlass.size() + " pending repair" +
                    (newGlassFound > 0 ? ", +" + newGlassFound + " new" : ""));
            }
        }
    }
    
    private static void processRepairs() {
        for (Map.Entry<ServerLevel, GlassTracker> entry : trackers.entrySet()) {
            ServerLevel level = entry.getKey();
            GlassTracker tracker = entry.getValue();
            
            Iterator<BrokenGlass> iterator = tracker.brokenGlass.iterator();
            while (iterator.hasNext()) {
                BrokenGlass glass = iterator.next();
                glass.ticksUntilRepair--;
                
                if (glass.ticksUntilRepair <= 0) {
                    BlockState currentState = level.getBlockState(glass.position);
                    
                    // Only repair if still air (don't replace player-placed blocks)
                    if (currentState.isAir()) {
                        level.setBlock(glass.position, glass.originalState, 3);
                        tracker.knownGlass.put(glass.position, glass.originalState);
                        MatrixCraftMod.LOGGER.info("[GlassRepair] Repaired glass at " + glass.position);
                    } else {
                        MatrixCraftMod.LOGGER.info("[GlassRepair] Skipped repair at " + glass.position + 
                            " - block is now " + currentState.getBlock().getName().getString());
                    }
                    
                    iterator.remove();
                }
            }
        }
    }
    
    /**
     * Checks if a block is any vanilla glass block.
     * Explicitly lists all vanilla glass to avoid false positives/negatives.
     */
    private static boolean isGlassBlock(Block block) {
        // Regular glass
        if (block == Blocks.GLASS) return true;
        if (block == Blocks.GLASS_PANE) return true;
        if (block == Blocks.TINTED_GLASS) return true;
        
        // All 16 stained glass blocks
        if (block == Blocks.WHITE_STAINED_GLASS) return true;
        if (block == Blocks.ORANGE_STAINED_GLASS) return true;
        if (block == Blocks.MAGENTA_STAINED_GLASS) return true;
        if (block == Blocks.LIGHT_BLUE_STAINED_GLASS) return true;
        if (block == Blocks.YELLOW_STAINED_GLASS) return true;
        if (block == Blocks.LIME_STAINED_GLASS) return true;
        if (block == Blocks.PINK_STAINED_GLASS) return true;
        if (block == Blocks.GRAY_STAINED_GLASS) return true;
        if (block == Blocks.LIGHT_GRAY_STAINED_GLASS) return true;
        if (block == Blocks.CYAN_STAINED_GLASS) return true;
        if (block == Blocks.PURPLE_STAINED_GLASS) return true;
        if (block == Blocks.BLUE_STAINED_GLASS) return true;
        if (block == Blocks.BROWN_STAINED_GLASS) return true;
        if (block == Blocks.GREEN_STAINED_GLASS) return true;
        if (block == Blocks.RED_STAINED_GLASS) return true;
        if (block == Blocks.BLACK_STAINED_GLASS) return true;
        
        // All 16 stained glass panes
        if (block == Blocks.WHITE_STAINED_GLASS_PANE) return true;
        if (block == Blocks.ORANGE_STAINED_GLASS_PANE) return true;
        if (block == Blocks.MAGENTA_STAINED_GLASS_PANE) return true;
        if (block == Blocks.LIGHT_BLUE_STAINED_GLASS_PANE) return true;
        if (block == Blocks.YELLOW_STAINED_GLASS_PANE) return true;
        if (block == Blocks.LIME_STAINED_GLASS_PANE) return true;
        if (block == Blocks.PINK_STAINED_GLASS_PANE) return true;
        if (block == Blocks.GRAY_STAINED_GLASS_PANE) return true;
        if (block == Blocks.LIGHT_GRAY_STAINED_GLASS_PANE) return true;
        if (block == Blocks.CYAN_STAINED_GLASS_PANE) return true;
        if (block == Blocks.PURPLE_STAINED_GLASS_PANE) return true;
        if (block == Blocks.BLUE_STAINED_GLASS_PANE) return true;
        if (block == Blocks.BROWN_STAINED_GLASS_PANE) return true;
        if (block == Blocks.GREEN_STAINED_GLASS_PANE) return true;
        if (block == Blocks.RED_STAINED_GLASS_PANE) return true;
        if (block == Blocks.BLACK_STAINED_GLASS_PANE) return true;
        
        return false;
    }
    
    // ========== Command Methods ==========
    
    public static void setRepairDelay(int seconds) {
        repairDelayTicks = seconds * 20;
        MatrixCraftMod.LOGGER.info("[GlassRepair] Delay set to " + seconds + " seconds (" + repairDelayTicks + " ticks)");
    }
    
    public static int getRepairDelaySeconds() {
        return repairDelayTicks / 20;
    }
    
    public static void setEnabled(boolean enable) {
        enabled = enable;
        if (!enabled) {
            // Clear all pending repairs when disabled
            for (GlassTracker tracker : trackers.values()) {
                tracker.brokenGlass.clear();
            }
        }
        MatrixCraftMod.LOGGER.info("[GlassRepair] System " + (enabled ? "enabled" : "disabled"));
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static int getPendingRepairCount(ServerLevel level) {
        GlassTracker tracker = trackers.get(level);
        return tracker == null ? 0 : tracker.brokenGlass.size();
    }
    
    public static void clearPendingRepairs(ServerLevel level) {
        GlassTracker tracker = trackers.get(level);
        if (tracker != null) {
            int count = tracker.brokenGlass.size();
            tracker.brokenGlass.clear();
            MatrixCraftMod.LOGGER.info("[GlassRepair] Cleared " + count + " pending repairs");
        }
    }
    
    public static void repairAllNow(ServerLevel level) {
        GlassTracker tracker = trackers.get(level);
        if (tracker == null || tracker.brokenGlass.isEmpty()) {
            MatrixCraftMod.LOGGER.info("[GlassRepair] No glass to repair");
            return;
        }
        
        int repaired = 0;
        for (BrokenGlass g : tracker.brokenGlass) {
            if (level.getBlockState(g.position).isAir()) {
                level.setBlock(g.position, g.originalState, 3);
                tracker.knownGlass.put(g.position, g.originalState);
                repaired++;
            }
        }
        
        tracker.brokenGlass.clear();
        MatrixCraftMod.LOGGER.info("[GlassRepair] Instantly repaired " + repaired + " glass blocks");
    }
    
    public static int getTrackedGlassCount(ServerLevel level) {
        GlassTracker tracker = trackers.get(level);
        return tracker == null ? 0 : tracker.knownGlass.size();
    }
    
    /**
     * Force re-scan and re-track all glass (useful for debugging)
     */
    public static void rescan(ServerLevel level) {
        GlassTracker tracker = trackers.get(level);
        if (tracker != null) {
            tracker.knownGlass.clear();
            MatrixCraftMod.LOGGER.info("[GlassRepair] Cleared tracking data, will rescan on next tick");
        }
    }
}
