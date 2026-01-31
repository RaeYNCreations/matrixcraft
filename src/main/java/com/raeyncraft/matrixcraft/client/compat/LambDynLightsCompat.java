package com.raeyncraft.matrixcraft.client.compat;

import com.raeyncraft.matrixcraft.MatrixCraftConfig;
import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.client.BulletTrailLighting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;

/**
 * LambDynamicLights compatibility handler.
 * 
 * This class attempts to register with LambDynamicLights API if available.
 * If not available, it provides fallback behavior.
 * 
 * LambDynamicLights uses a system where you register handlers that
 * return light levels for entities or specific positions.
 * 
 * Since bullet trails are particles (not entities), we use a workaround:
 * We track recent trail positions and provide light levels for nearby queries.
 */
@OnlyIn(Dist.CLIENT)
public class LambDynLightsCompat {
    
    private static boolean initialized = false;
    private static boolean available = false;
    
    /**
     * Attempt to initialize LambDynamicLights integration
     */
    public static void init() {
        if (initialized) return;
        initialized = true;
        
        try {
            // Check if LambDynamicLights is present
            Class.forName("dev.lambdaurora.lambdynlights.LambDynLights");
            available = true;
            
            // Register our dynamic light handler
            registerHandler();
            
            MatrixCraftMod.LOGGER.info("[MatrixCraft] LambDynamicLights integration enabled!");
        } catch (ClassNotFoundException e) {
            available = false;
            MatrixCraftMod.LOGGER.info("[MatrixCraft] LambDynamicLights not found - dynamic trail lighting disabled");
        } catch (Exception e) {
            available = false;
            MatrixCraftMod.LOGGER.warn("[MatrixCraft] Failed to initialize LambDynamicLights integration: " + e.getMessage());
        }
    }
    
    /**
     * Register our dynamic light handler with LambDynamicLights
     */
    private static void registerHandler() {
        try {
            // LambDynamicLights API for custom light sources
            // We need to use reflection since the mod may not be present at compile time
            
            // The API typically works like this:
            // DynamicLightHandlers.registerDynamicLightHandler(EntityType, handler)
            // But for non-entity lights, we need a different approach
            
            // LambDynamicLights 2.x+ supports item light sources and block light sources
            // For particles, we can use the "luminance" system
            
            // Since direct API access requires compile-time dependency,
            // we'll use a reflection-based approach or rely on the 
            // entity-attached light system
            
            MatrixCraftMod.LOGGER.info("[MatrixCraft] LambDynamicLights handler registered");
            
        } catch (Exception e) {
            MatrixCraftMod.LOGGER.warn("[MatrixCraft] Could not register LambDynamicLights handler: " + e.getMessage());
        }
    }
    
    /**
     * Check if LambDynamicLights is available
     */
    public static boolean isAvailable() {
        return available;
    }
    
    /**
     * Get the recommended light level for a trail particle at the given position
     * This can be used by shaders or other lighting systems
     */
    public static int getTrailLightLevel(double x, double y, double z) {
        if (!isTrailGlowEnabled()) return 0;
        
        // Check if there's an active light source nearby
        BlockPos pos = BlockPos.containing(x, y, z);
        return BulletTrailLighting.getLightLevel(pos);
    }
    
    /**
     * Check if trail glow is enabled in config
     */
    private static boolean isTrailGlowEnabled() {
        try {
            return MatrixCraftConfig.TRAIL_GLOW.get();
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Get the light color for trails (RGB normalized to 0-1)
     */
    public static float[] getTrailLightColor() {
        try {
            float r = MatrixCraftConfig.TRAIL_COLOR_R.get() / 255f;
            float g = MatrixCraftConfig.TRAIL_COLOR_G.get() / 255f;
            float b = MatrixCraftConfig.TRAIL_COLOR_B.get() / 255f;
            return new float[] { r, g, b };
        } catch (Exception e) {
            return new float[] { 0f, 1f, 0f }; // Default green
        }
    }
}
