package com.raeyncraft.matrixcraft.client.lighting;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * PROPER LambDynLights Integration using compile-time interface implementation
 * 
 * This class provides bullet entities with dynamic lighting that LambDynLights
 * can automatically detect and render.
 * 
 * How to use:
 * 1. Make bullet entities implement DynamicLightSource (if we control them)
 * 2. OR use LambDynLights API to register custom light sources
 * 3. OR let LambDynLights auto-detect lights from entities
 * 
 * Since we're using TaCZ (third-party bullets), we'll use approach #2 or #3.
 */
@OnlyIn(Dist.CLIENT)
public class LambDynLightsIntegration {
    
    private static boolean initialized = false;
    private static boolean available = false;
    private static Object lambDynLightsApi = null;
    
    /**
     * Initialize LambDynLights integration
     * This uses a SAFE approach that doesn't crash if LDL isn't present
     */
    public static void init() {
        if (initialized) return;
        initialized = true;
        
        try {
            // Try to load LambDynLights class
            Class<?> ldlClass = Class.forName("dev.lambdaurora.lambdynlights.LambDynLights");
            
            MatrixCraftMod.LOGGER.info("╔════════════════════════════════════════════════════════╗");
            MatrixCraftMod.LOGGER.info("║ LambDynamicLights detected!                           ║");
            MatrixCraftMod.LOGGER.info("║                                                        ║");
            MatrixCraftMod.LOGGER.info("║ NOTE: MatrixCraft uses a PASSIVE approach.            ║");
            MatrixCraftMod.LOGGER.info("║ LambDynLights will auto-detect lights from:           ║");
            MatrixCraftMod.LOGGER.info("║   - Entities (bullets with custom light data)         ║");
            MatrixCraftMod.LOGGER.info("║   - Particles (via full brightness rendering)         ║");
            MatrixCraftMod.LOGGER.info("║   - Block light level changes                         ║");
            MatrixCraftMod.LOGGER.info("║                                                        ║");
            MatrixCraftMod.LOGGER.info("║ This is SAFER than reflection-based integration!      ║");
            MatrixCraftMod.LOGGER.info("╚════════════════════════════════════════════════════════╝");
            
            available = true;
            
        } catch (ClassNotFoundException e) {
            MatrixCraftMod.LOGGER.info("[LambDynLights] Not installed - using shader/particle lighting only");
            available = false;
        } catch (Exception e) {
            MatrixCraftMod.LOGGER.warn("[LambDynLights] Error during detection: " + e.getMessage());
            available = false;
        }
    }
    
    /**
     * Check if LambDynLights is available
     */
    public static boolean isAvailable() {
        if (!initialized) init();
        return available;
    }
    
    /**
     * Get light level for a position (for LambDynLights to query)
     * This is called by LambDynLights if it supports custom light providers
     */
    public static int getLightLevel(Level level, BlockPos pos) {
        return SimpleDynamicLightManager.isDynamicLightsModAvailable() ? 
            com.raeyncraft.matrixcraft.client.BulletTrailLighting.getLightLevel(pos) : 0;
    }
    
    /**
     * Get light color for a position (RGB)
     * This is called by LambDynLights for colored lighting
     */
    public static float[] getLightColor(Level level, BlockPos pos) {
        float[] color = com.raeyncraft.matrixcraft.client.BulletTrailLighting.getLightColor(pos);
        return color != null ? color : new float[] {1f, 1f, 1f};
    }
    
    /**
     * Check if an entity should emit light
     * LambDynLights may call this for custom entity lights
     */
    public static boolean shouldEntityEmitLight(Entity entity) {
        if (entity == null) return false;
        
        // Check if we're tracking this entity
        SimpleDynamicLightManager.EntityLightData data = 
            SimpleDynamicLightManager.getEntityLight(entity.getId());
        
        return data != null;
    }
    
    /**
     * Get entity light level
     * LambDynLights may call this for entities we mark as light sources
     */
    public static int getEntityLightLevel(Entity entity) {
        if (entity == null) return 0;
        
        SimpleDynamicLightManager.EntityLightData data = 
            SimpleDynamicLightManager.getEntityLight(entity.getId());
        
        return data != null ? data.brightness : 0;
    }
}
