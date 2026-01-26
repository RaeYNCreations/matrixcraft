package com.raeyncraft.matrixcraft.client.compat;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.client.BulletTrailLighting;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;

/**
 * RyoamicLights compatibility layer.
 * 
 * RyoamicLights is a fork of LambDynamicLights that provides dynamic lighting.
 * This class provides a bridge between our bullet trail lighting system and RyoamicLights.
 * 
 * Since RyoamicLights works primarily with entities and items, our position-based
 * lighting works best with the shader glow effects. The particles already render
 * at full brightness, which works with shader bloom.
 */
@OnlyIn(Dist.CLIENT)
public class RyoamicLightsCompat {
    
    private static boolean initialized = false;
    private static boolean available = false;
    
    /**
     * Initialize RyoamicLights integration
     */
    public static void init() {
        if (initialized) return;
        initialized = true;
        
        try {
            // Try to find RyoamicLights main class
            Class<?> ryoamicClass = Class.forName("org.thinkingstudio.ryoamiclights.RyoamicLights");
            available = true;
            MatrixCraftMod.LOGGER.info("[MatrixCraft] RyoamicLights integration initialized!");
        } catch (ClassNotFoundException e) {
            // Try LambDynamicLights API (RyoamicLights uses same API)
            try {
                Class.forName("dev.lambdaurora.lambdynlights.LambDynLights");
                available = true;
                MatrixCraftMod.LOGGER.info("[MatrixCraft] LambDynamicLights-compatible API found!");
            } catch (ClassNotFoundException e2) {
                available = false;
                MatrixCraftMod.LOGGER.info("[MatrixCraft] No dynamic lights mod found - using shader-only lighting");
            }
        }
    }
    
    /**
     * Check if RyoamicLights (or compatible) is available
     */
    public static boolean isAvailable() {
        if (!initialized) init();
        return available;
    }
    
    /**
     * Get the light level for a position from our tracking system
     */
    public static int getLightLevel(double x, double y, double z) {
        if (!BulletTrailLighting.isDynamicLightingEnabled()) return 0;
        
        BlockPos pos = BlockPos.containing(x, y, z);
        return BulletTrailLighting.getLightLevel(pos);
    }
    
    /**
     * Get the light color at a position (RGB 0-1)
     */
    public static float[] getLightColor(double x, double y, double z) {
        if (!BulletTrailLighting.isDynamicLightingEnabled()) return null;
        
        BlockPos pos = BlockPos.containing(x, y, z);
        return BulletTrailLighting.getLightColor(pos);
    }
    
    /**
     * Check if dynamic lighting is enabled
     */
    public static boolean isDynamicLightingEnabled() {
        return BulletTrailLighting.isDynamicLightingEnabled();
    }
    
    /**
     * Get all active light sources for custom integration
     */
    public static Map<BlockPos, BulletTrailLighting.LightSource> getActiveLights() {
        return BulletTrailLighting.getActiveLights();
    }
    
    /**
     * Get count of active lights
     */
    public static int getActiveLightCount() {
        return BulletTrailLighting.getActiveLightCount();
    }
}
