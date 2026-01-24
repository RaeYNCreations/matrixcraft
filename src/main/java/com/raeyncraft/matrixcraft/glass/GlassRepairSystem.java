package com.raeyncraft.matrixcraft.glass;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

/**
 * Tracks glass blocks near players and repairs them when destroyed.
 */
@EventBusSubscriber(modid = MatrixCraftMod.MODID)
public class GlassRepairSystem {
    
    private static final Map<ServerLevel, GlassTracker> trackers = new HashMap<>();
    private static int repairDelayTicks = 60; // Default 3 seconds
    private static boolean enabled = true;
    private static int scanRadius = 64; // Scan 64 blocks around players
    private static int tickCounter = 0;
    
    private static class GlassTracker {
        Map<BlockPos, BlockState> knownGlass = new HashMap<>();
        List<BrokenGlass> brokenGlass = new ArrayList<>();
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
    public static void onLevelLoad(net.neoforged.neoforge.event.level.LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            trackers.putIfAbsent(serverLevel, new GlassTracker());
            MatrixCraftMod.LOGGER.info("[GlassRepair] Initialized for level: " + serverLevel.dimension().location());
        }
    }
    
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!enabled) return;
        
        tickCounter++;
        
        // Scan for glass every 20 ticks (1 second)
        if (tickCounter % 20 == 0) {
            scanForGlassNearPlayers();
        }
        
        // Process repairs every tick
        processRepairs();
    }
    
    private static void scanForGlassNearPlayers() {
        for (Map.Entry<ServerLevel, GlassTracker> entry : trackers.entrySet()) {
            ServerLevel level = entry.getKey();
            GlassTracker tracker = entry.getValue();
            
            int playerCount = level.players().size();
            if (playerCount == 0) continue;
            
            Set<BlockPos> currentGlass = new HashSet<>();
            int glassFound = 0;
            int newGlassFound = 0;
            
            // Scan around each player
            for (ServerPlayer player : level.players()) {
                BlockPos playerPos = player.blockPosition();
                
                // Scan a cube around the player
                for (int x = -scanRadius; x <= scanRadius; x += 2) {
                    for (int y = -scanRadius; y <= scanRadius; y += 2) {
                        for (int z = -scanRadius; z <= scanRadius; z += 2) {
                            BlockPos checkPos = playerPos.offset(x, y, z);
                            BlockState state = level.getBlockState(checkPos);
                            
                            if (isGlassBlock(state.getBlock())) {
                                BlockPos immutablePos = checkPos.immutable();
                                currentGlass.add(immutablePos);
                                glassFound++;
                                
                                // Track new glass ONLY if we haven't seen it before
                                if (!tracker.knownGlass.containsKey(immutablePos)) {
                                    tracker.knownGlass.put(immutablePos, state);
                                    newGlassFound++;
                                    MatrixCraftMod.LOGGER.info("[GlassRepair] Found NEW glass at " + immutablePos);
                                }
                            }
                        }
                    }
                }
            }
            
            if (tickCounter % 100 == 0) {
                MatrixCraftMod.LOGGER.info("[GlassRepair] Scan: " + playerCount + " players, " + glassFound + " glass scanned, " + newGlassFound + " new, " + tracker.knownGlass.size() + " tracked, " + tracker.brokenGlass.size() + " pending");
            }
            
            // Check for missing glass
            Iterator<Map.Entry<BlockPos, BlockState>> iterator = tracker.knownGlass.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, BlockState> glassEntry = iterator.next();
                BlockPos pos = glassEntry.getKey();
                
                // Check if glass is still visible (near a player)
                boolean nearPlayer = false;
                for (ServerPlayer player : level.players()) {
                    if (player.blockPosition().distSqr(pos) <= scanRadius * scanRadius) {
                        nearPlayer = true;
                        break;
                    }
                }
                
                if (!nearPlayer) {
                    // Too far from players, stop tracking
                    iterator.remove();
                    continue;
                }
                
                // Check if the block is still glass by actually checking the block
                BlockState currentState = level.getBlockState(pos);
                
                if (!isGlassBlock(currentState.getBlock())) {
                    // Glass is gone!
                    if (currentState.isAir()) {
                        // It was destroyed (air now)
                        tracker.brokenGlass.add(new BrokenGlass(pos, glassEntry.getValue(), repairDelayTicks));
                        MatrixCraftMod.LOGGER.info("[GlassRepair] Detected broken glass at " + pos + " - will repair in " + (repairDelayTicks / 20) + " seconds");
                    } else {
                        // Replaced with something else, stop tracking
                        MatrixCraftMod.LOGGER.info("[GlassRepair] Glass replaced with " + currentState.getBlock().getName().getString() + " at " + pos);
                    }
                    
                    iterator.remove();
                }
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
                    
                    if (currentState.isAir()) {
                        level.setBlock(glass.position, glass.originalState, 3);
                        tracker.knownGlass.put(glass.position, glass.originalState);
                        MatrixCraftMod.LOGGER.info("[GlassRepair] Repaired glass at " + glass.position);
                    }
                    
                    iterator.remove();
                }
            }
        }
    }
    
    private static boolean isGlassBlock(Block block) {
        return block == Blocks.GLASS ||
               block == Blocks.WHITE_STAINED_GLASS ||
               block == Blocks.ORANGE_STAINED_GLASS ||
               block == Blocks.MAGENTA_STAINED_GLASS ||
               block == Blocks.LIGHT_BLUE_STAINED_GLASS ||
               block == Blocks.YELLOW_STAINED_GLASS ||
               block == Blocks.LIME_STAINED_GLASS ||
               block == Blocks.PINK_STAINED_GLASS ||
               block == Blocks.GRAY_STAINED_GLASS ||
               block == Blocks.LIGHT_GRAY_STAINED_GLASS ||
               block == Blocks.CYAN_STAINED_GLASS ||
               block == Blocks.PURPLE_STAINED_GLASS ||
               block == Blocks.BLUE_STAINED_GLASS ||
               block == Blocks.BROWN_STAINED_GLASS ||
               block == Blocks.GREEN_STAINED_GLASS ||
               block == Blocks.RED_STAINED_GLASS ||
               block == Blocks.BLACK_STAINED_GLASS ||
               block == Blocks.GLASS_PANE ||
               block == Blocks.WHITE_STAINED_GLASS_PANE ||
               block == Blocks.ORANGE_STAINED_GLASS_PANE ||
               block == Blocks.MAGENTA_STAINED_GLASS_PANE ||
               block == Blocks.LIGHT_BLUE_STAINED_GLASS_PANE ||
               block == Blocks.YELLOW_STAINED_GLASS_PANE ||
               block == Blocks.LIME_STAINED_GLASS_PANE ||
               block == Blocks.PINK_STAINED_GLASS_PANE ||
               block == Blocks.GRAY_STAINED_GLASS_PANE ||
               block == Blocks.LIGHT_GRAY_STAINED_GLASS_PANE ||
               block == Blocks.CYAN_STAINED_GLASS_PANE ||
               block == Blocks.PURPLE_STAINED_GLASS_PANE ||
               block == Blocks.BLUE_STAINED_GLASS_PANE ||
               block == Blocks.BROWN_STAINED_GLASS_PANE ||
               block == Blocks.GREEN_STAINED_GLASS_PANE ||
               block == Blocks.RED_STAINED_GLASS_PANE ||
               block == Blocks.BLACK_STAINED_GLASS_PANE ||
               block == Blocks.TINTED_GLASS;
    }
    
    // Command access methods
    public static void setRepairDelay(int seconds) {
        repairDelayTicks = seconds * 20;
        MatrixCraftMod.LOGGER.info("[GlassRepair] Delay set to " + seconds + " seconds");
    }
    
    public static int getRepairDelaySeconds() {
        return repairDelayTicks / 20;
    }
    
    public static void setEnabled(boolean enable) {
        enabled = enable;
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
            tracker.brokenGlass.clear();
        }
        MatrixCraftMod.LOGGER.info("[GlassRepair] Cleared all pending repairs");
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
}
