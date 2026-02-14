package com.raeyncraft.matrixcraft.client;

import com.raeyncraft.matrixcraft.MatrixCraftConfig;
import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.client.lighting.SimpleDynamicLightManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles dynamic lighting for entities hit by bullets.
 * When a bullet hits an entity, it briefly lights up that entity.
 * 
 * RGB Color System:
 * - Uses the SAME RGB color as bullet trails (TRAIL_COLOR_R/G/B config)
 * - Default: Green (R=0, G=255, B=0)
 * - Configurable in MatrixCraftConfig.TRAIL_COLOR_R/G/B (0-255 each)
 * - Examples:
 *   - Red bullets: R=255, G=0, B=0
 *   - Blue bullets: R=0, G=0, B=255
 *   - Purple bullets: R=128, G=0, B=255
 *   - Orange bullets: R=255, G=128, B=0
 * 
 * The lighting system automatically matches bullet trail colors for visual consistency.
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = MatrixCraftMod.MODID)
public class HitEntityLightingHandler {
    
    // Track entities that have been hit and should glow
    private static final Map<Integer, HitLightState> hitEntities = new ConcurrentHashMap<>();
    
    // How long entities glow after being hit (in ticks)
    private static final int HIT_GLOW_DURATION = 20; // 1 second
    
    /**
     * State for an entity that's been hit and should glow
     */
    private static class HitLightState {
        final int entityId;
        int ticksRemaining;
        final float red, green, blue;
        
        HitLightState(int entityId, float r, float g, float b) {
            this.entityId = entityId;
            this.ticksRemaining = HIT_GLOW_DURATION;
            this.red = r;
            this.green = g;
            this.blue = b;
        }
    }
    
    /**
     * Listen for damage events to detect bullet hits
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!isHitLightingEnabled()) {
            return;
        }
        
        LivingEntity entity = event.getEntity();
        if (entity == null || !entity.level().isClientSide) {
            return;
        }
        
        // Check if damage is from a projectile (likely a bullet)
        if (event.getSource().getDirectEntity() != null) {
            Entity projectile = event.getSource().getDirectEntity();
            
            // Check if it's a TacZ bullet
            if (isTaczBullet(projectile)) {
                addHitEntityLight(entity);
            }
        }
    }
    
    /**
     * Add dynamic lighting to an entity that was hit
     * Uses RGB color from trail configuration for consistency
     */
    private static void addHitEntityLight(LivingEntity entity) {
        if (entity == null) return;
        
        try {
            int entityId = entity.getId();
            
            // Get RGB color from trail configuration (TRAIL_COLOR_R/G/B)
            // This ensures hit entities glow with the same color as bullet trails
            float[] color = BulletTrailLighting.getTrailColor();
            int brightness = BulletTrailLighting.getConfiguredLightLevel();
            
            // Create or update hit light state with RGB values
            hitEntities.put(entityId, new HitLightState(entityId, color[0], color[1], color[2]));
            
            // Register with dynamic light manager (passes RGB to LambDynLights)
            SimpleDynamicLightManager.ensureInit();
            SimpleDynamicLightManager.trackEntityLight(entity, brightness, color[0], color[1], color[2]);
            
            MatrixCraftMod.LOGGER.debug("[HitEntityLighting] Added RGB light to entity " + entityId + 
                " (R=" + (int)(color[0]*255) + ", G=" + (int)(color[1]*255) + ", B=" + (int)(color[2]*255) + ")");
        } catch (Exception e) {
            MatrixCraftMod.LOGGER.warn("[HitEntityLighting] Failed to add light to hit entity: " + e.getMessage());
        }
    }
    
    /**
     * Update hit entity lights each tick
     */
    public static void tick(net.minecraft.client.multiplayer.ClientLevel level) {
        if (!isHitLightingEnabled()) {
            return;
        }
        
        if (hitEntities.isEmpty()) {
            return;
        }
        
        // Update all hit entity lights
        hitEntities.entrySet().removeIf(entry -> {
            int entityId = entry.getKey();
            HitLightState state = entry.getValue();
            
            state.ticksRemaining--;
            
            // Remove if expired
            if (state.ticksRemaining <= 0) {
                try {
                    SimpleDynamicLightManager.untrackEntityLightById(entityId);
                } catch (Exception e) {
                    MatrixCraftMod.LOGGER.debug("[HitEntityLighting] Failed to remove light: " + e.getMessage());
                }
                return true;
            }
            
            // Ping to keep alive
            try {
                SimpleDynamicLightManager.pingEntity(entityId);
            } catch (Exception e) {
                MatrixCraftMod.LOGGER.debug("[HitEntityLighting] Failed to ping entity: " + e.getMessage());
            }
            
            return false;
        });
    }
    
    /**
     * Clear all hit entity lights
     */
    public static void clearAll() {
        for (Integer entityId : hitEntities.keySet()) {
            try {
                SimpleDynamicLightManager.untrackEntityLightById(entityId);
            } catch (Exception e) {
                MatrixCraftMod.LOGGER.debug("[HitEntityLighting] Failed to clear light: " + e.getMessage());
            }
        }
        hitEntities.clear();
    }
    
    /**
     * Check if entity is a TacZ bullet
     */
    private static boolean isTaczBullet(Entity entity) {
        if (entity == null) return false;
        // Cache the class name check for efficiency
        String className = entity.getClass().getName();
        return "com.tacz.guns.entity.EntityKineticBullet".equals(className)
                || entity.getType().toString().toLowerCase().contains("tacz");
    }
    
    /**
     * Check if hit entity lighting is enabled in config
     */
    private static boolean isHitLightingEnabled() {
        try {
            return MatrixCraftConfig.TRAIL_DYNAMIC_LIGHTING.get();
        } catch (Exception e) {
            return true;
        }
    }
}
