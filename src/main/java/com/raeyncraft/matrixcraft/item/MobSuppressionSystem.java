package com.raeyncraft.matrixcraft.item;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages mob suppression zones created by Safe Haven Obelisks.
 * 
 * Prevents all non-player mobs from spawning within the configured radius
 * of each registered suppressor.
 * 
 * Special handling for flying mobs like Phantoms - uses extended vertical range
 * and also removes them if they enter the zone after spawning.
 */
@EventBusSubscriber(modid = MatrixCraftMod.MODID)
public class MobSuppressionSystem {
    
    // Map of level -> (position -> radius)
    private static final Map<ServerLevel, Map<BlockPos, Integer>> suppressors = new ConcurrentHashMap<>();
    
    // Cache for quick lookups
    private static final Map<ServerLevel, Set<BlockPos>> allSuppressorPositions = new ConcurrentHashMap<>();
    
    // Extended vertical range for flying mobs (phantoms spawn high up)
    private static final int VERTICAL_EXTENSION = 128;
    
    /**
     * Add a suppressor at the given position
     */
    public static void addSuppressor(ServerLevel level, BlockPos pos, int radius) {
        suppressors.computeIfAbsent(level, k -> new ConcurrentHashMap<>()).put(pos.immutable(), radius);
        allSuppressorPositions.computeIfAbsent(level, k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
        
        MatrixCraftMod.LOGGER.info("[MobSuppression] Added suppressor at " + pos + " with radius " + radius);
    }
    
    /**
     * Remove a suppressor at the given position
     */
    public static void removeSuppressor(ServerLevel level, BlockPos pos) {
        Map<BlockPos, Integer> levelSuppressors = suppressors.get(level);
        if (levelSuppressors != null) {
            levelSuppressors.remove(pos);
        }
        
        Set<BlockPos> positions = allSuppressorPositions.get(level);
        if (positions != null) {
            positions.remove(pos);
        }
        
        MatrixCraftMod.LOGGER.info("[MobSuppression] Removed suppressor at " + pos);
    }
    
    /**
     * Check if a position is within any suppression zone
     * Uses cylinder check - extended vertical range for flying mobs
     */
    public static boolean isInSuppressionZone(ServerLevel level, BlockPos pos) {
        return isInSuppressionZone(level, pos, false);
    }
    
    /**
     * Check if a position is within any suppression zone
     * @param extendedVertical if true, uses extended vertical range for flying mobs
     */
    public static boolean isInSuppressionZone(ServerLevel level, BlockPos pos, boolean extendedVertical) {
        Map<BlockPos, Integer> levelSuppressors = suppressors.get(level);
        if (levelSuppressors == null || levelSuppressors.isEmpty()) {
            return false;
        }
        
        for (Map.Entry<BlockPos, Integer> entry : levelSuppressors.entrySet()) {
            BlockPos suppressorPos = entry.getKey();
            int radius = entry.getValue();
            
            // Check horizontal distance (XZ plane)
            double dx = pos.getX() - suppressorPos.getX();
            double dz = pos.getZ() - suppressorPos.getZ();
            double horizontalDistSq = dx * dx + dz * dz;
            
            if (horizontalDistSq <= radius * radius) {
                // Check vertical distance
                int verticalRange = extendedVertical ? VERTICAL_EXTENSION : radius;
                int dy = Math.abs(pos.getY() - suppressorPos.getY());
                
                if (dy <= verticalRange) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Get all active suppressors in a level
     */
    public static Map<BlockPos, Integer> getSuppressors(ServerLevel level) {
        return suppressors.getOrDefault(level, Collections.emptyMap());
    }
    
    /**
     * Get count of active suppressors
     */
    public static int getSuppressorCount(ServerLevel level) {
        Map<BlockPos, Integer> levelSuppressors = suppressors.get(level);
        return levelSuppressors == null ? 0 : levelSuppressors.size();
    }
    
    // ==================== EVENT HANDLERS ====================
    
    /**
     * Prevent mobs from joining the world in suppression zones
     * This catches all mob spawns including natural, spawner, and command spawns
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        
        Entity entity = event.getEntity();
        
        // Allow players
        if (entity instanceof Player) return;
        
        // Only block mobs
        if (!(entity instanceof Mob)) return;
        
        // Check if in suppression zone
        BlockPos spawnPos = entity.blockPosition();
        
        // For flying mobs (phantoms), use extended vertical check
        boolean isFlying = entity instanceof Phantom;
        
        if (isInSuppressionZone(serverLevel, spawnPos, isFlying)) {
            // Cancel the spawn
            event.setCanceled(true);
            MatrixCraftMod.LOGGER.debug("[MobSuppression] Blocked spawn of " + 
                entity.getType().getDescriptionId() + " at " + spawnPos);
        }
    }
    
    /**
     * Remove suppressor when the lodestone is broken
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        
        BlockPos pos = event.getPos();
        
        // Check if this is a suppressor location
        Set<BlockPos> positions = allSuppressorPositions.get(serverLevel);
        if (positions != null && positions.contains(pos)) {
            removeSuppressor(serverLevel, pos);
            
            if (event.getPlayer() != null) {
                event.getPlayer().displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Safe Haven deactivated.")
                        .withStyle(net.minecraft.ChatFormatting.DARK_RED), true);
            }
        }
    }
    
    /**
     * Clean up when level unloads
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            suppressors.remove(serverLevel);
            allSuppressorPositions.remove(serverLevel);
        }
    }
    
    /**
     * Periodically:
     * 1. Validate suppressors (check if lodestone still exists)
     * 2. Remove any flying mobs that entered the zone after spawning
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        int tick = event.getServer().getTickCount();
        
        // Every 20 ticks (1 second): Remove flying mobs in zones
        if (tick % 20 == 0) {
            removeFlyingMobsInZones(event);
        }
        
        // Every 100 ticks (5 seconds): Validate suppressors
        if (tick % 100 == 0) {
            validateSuppressors(event);
        }
    }
    
    /**
     * Remove flying mobs (like phantoms) that entered suppression zones
     */
    private static void removeFlyingMobsInZones(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            Map<BlockPos, Integer> levelSuppressors = suppressors.get(level);
            if (levelSuppressors == null || levelSuppressors.isEmpty()) continue;
            
            // Find all mobs in suppression zones
            List<Mob> toRemove = new ArrayList<>();
            
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof Mob mob)) continue;
                if (entity instanceof Player) continue;
                
                // Check if this mob is in a suppression zone
                boolean isFlying = entity instanceof Phantom;
                if (isInSuppressionZone(level, entity.blockPosition(), isFlying)) {
                    toRemove.add(mob);
                }
            }
            
            // Remove the mobs
            for (Mob mob : toRemove) {
                mob.discard();
                MatrixCraftMod.LOGGER.debug("[MobSuppression] Removed " + 
                    mob.getType().getDescriptionId() + " from suppression zone");
            }
        }
    }
    
    /**
     * Validate that all suppressor lodestones still exist
     */
    private static void validateSuppressors(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            Map<BlockPos, Integer> levelSuppressors = suppressors.get(level);
            if (levelSuppressors == null || levelSuppressors.isEmpty()) continue;
            
            // Check each suppressor
            List<BlockPos> toRemove = new ArrayList<>();
            for (BlockPos pos : levelSuppressors.keySet()) {
                // If the chunk isn't loaded, skip
                if (!level.isLoaded(pos)) continue;
                
                // Check if lodestone still exists
                if (!level.getBlockState(pos).is(Blocks.LODESTONE)) {
                    toRemove.add(pos);
                }
            }
            
            // Remove invalid suppressors
            for (BlockPos pos : toRemove) {
                removeSuppressor(level, pos);
                MatrixCraftMod.LOGGER.info("[MobSuppression] Auto-removed suppressor at " + pos + " (lodestone missing)");
            }
        }
    }
}
