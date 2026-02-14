package com.raeyncraft.matrixcraft.client;

import com.raeyncraft.matrixcraft.MatrixCraftConfig;
import com.raeyncraft.matrixcraft.MatrixCraftMod;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages dynamic lighting for bullet trails - "Torch Bullets"
 * 
 * Features:
 * - Colored light based on trail color config
 * - Configurable light level (1-15)
 * - Fading light as trails decay
 * - Integration with LambDynLights (if available)
 * - Shader-friendly data for Iris/Optifine
 * 
 * The light color matches the bullet trail color from config,
 * so red trails make red light, green trails make green light, etc.
 */
@OnlyIn(Dist.CLIENT)
public class BulletTrailLighting {
    
    // Track active light sources: position -> light data
    private static final Map<BlockPos, LightSource> activeLights = new ConcurrentHashMap<>();
    
    // Dynamic lights availability flag
    private static boolean dynamicLightsAvailable = false;
    private static boolean checkedForDynamicLights = false;
    
    // Light settings
    private static final int MAX_LIGHTS = 300; // Prevent too many light sources
    
    /**
     * Light source data with color information
     */
    public static class LightSource {
        public int brightness;
        public int ticksRemaining;
        public int maxTicks;
        public float red, green, blue;
        public BlockPos position;
        
        public LightSource(BlockPos pos, int brightness, int duration, float r, float g, float b) {
            this.position = pos;
            this.brightness = brightness;
            this.ticksRemaining = duration;
            this.maxTicks = duration;
            this.red = r;
            this.green = g;
            this.blue = b;
        }
        
        // Returns current brightness based on remaining time (fades out)
        public int getCurrentBrightness() {
            float progress = (float) ticksRemaining / maxTicks;
            return Math.max(1, (int) (brightness * progress));
        }
        
        // Get color as packed ARGB int
        public int getPackedColor() {
            int ri = (int) (red * 255);
            int gi = (int) (green * 255);
            int bi = (int) (blue * 255);
            int a = (int) (255 * ((float) ticksRemaining / maxTicks));
            return (a << 24) | (ri << 16) | (gi << 8) | bi;
        }
    }
    
    /**
     * Check if dynamic lighting is enabled
     */
    public static boolean isDynamicLightingEnabled() {
        try {
            return MatrixCraftConfig.TRAIL_DYNAMIC_LIGHTING.get();
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Get configured light level
     */
    public static int getConfiguredLightLevel() {
        try {
            return MatrixCraftConfig.TRAIL_LIGHT_LEVEL.get();
        } catch (Exception e) {
            return 12;
        }
    }
    
    /**
     * Get trail color from config (normalized 0-1)
     */
    public static float[] getTrailColor() {
        try {
            float r = MatrixCraftConfig.TRAIL_COLOR_R.get() / 255f;
            float g = MatrixCraftConfig.TRAIL_COLOR_G.get() / 255f;
            float b = MatrixCraftConfig.TRAIL_COLOR_B.get() / 255f;
            return new float[] { r, g, b };
        } catch (Exception e) {
            return new float[] { 0f, 1f, 0f }; // Default green
        }
    }
    
    /**
     * Check if LambDynLights is available
     */
    public static boolean isDynamicLightsAvailable() {
        if (!checkedForDynamicLights) {
            checkedForDynamicLights = true;
            try {
                // Check if LambDynLights API class exists
                Class.forName("dev.lambdaurora.lambdynlights.api.DynamicLightHandlers");
                dynamicLightsAvailable = true;
                MatrixCraftMod.LOGGER.info("[BulletTrailLighting] LambDynLights detected!");
            } catch (ClassNotFoundException e) {
                dynamicLightsAvailable = false;
                MatrixCraftMod.LOGGER.info("[BulletTrailLighting] No dynamic lights mod found, using shader-based lighting");
            }
        }
        return dynamicLightsAvailable;
    }
    
    /**
     * Register a light source at a position (called when spawning trail particles)
     * Uses the configured trail color for the light color.
     */
    public static void addLightSource(double x, double y, double z) {
        if (!isDynamicLightingEnabled()) {
            return;
        }
    
        if (activeLights.size() >= MAX_LIGHTS) {
            // Remove oldest lights if at capacity
            pruneOldestLights(50);
        }
    
        BlockPos pos = BlockPos.containing(x, y, z);
    
        // Get color from config
        float[] color = getTrailColor();
        int brightness = getConfiguredLightLevel();
    
        int durationTicks = MatrixCraftConfig.TRAIL_LIGHT_DURATION_TICKS.get();
    
        activeLights.put(pos, new LightSource(pos, brightness, durationTicks,
            color[0], color[1], color[2]));
    }
    
    /**
     * Add a light source with custom color (for special effects)
     */
    public static void addLightSourceWithColor(double x, double y, double z, float r, float g, float b) {
        if (!isDynamicLightingEnabled()) {
            return;
        }
    
        if (activeLights.size() >= MAX_LIGHTS) {
            pruneOldestLights(50);
        }
    
        BlockPos pos = BlockPos.containing(x, y, z);
        int brightness = getConfiguredLightLevel();
    
        int durationTicks = MatrixCraftConfig.TRAIL_LIGHT_DURATION_TICKS.get();
    
        activeLights.put(pos, new LightSource(pos, brightness, durationTicks, r, g, b));
    }
    
    /**
     * Add multiple light sources along a trail segment
     */
    public static void addTrailSegmentLights(double x1, double y1, double z1, 
                                              double x2, double y2, double z2, 
                                              int numLights) {
        if (!isDynamicLightingEnabled() || numLights <= 0) {
            return;
        }
        
        for (int i = 0; i < numLights; i++) {
            double t = (double) i / numLights;
            double x = x1 + (x2 - x1) * t;
            double y = y1 + (y2 - y1) * t;
            double z = z1 + (z2 - z1) * t;
            addLightSource(x, y, z);
        }
    }
    
    /**
     * Called every client tick to update light sources
     */
    public static void tick() {
        if (activeLights.isEmpty()) {
            return;
        }
        
        // Create list of positions to remove (avoid concurrent modification)
        java.util.List<BlockPos> toRemove = new java.util.ArrayList<>();
        
        // Update all lights and collect expired ones
        for (Map.Entry<BlockPos, LightSource> entry : activeLights.entrySet()) {
            LightSource light = entry.getValue();
            
            light.ticksRemaining--;
            
            if (light.ticksRemaining <= 0) {
                toRemove.add(entry.getKey());
            }
        }
        
        // Remove expired lights
        for (BlockPos pos : toRemove) {
            activeLights.remove(pos);
        }
        
        // Update trail-light texture for shader ACL
        try {
            com.raeyncraft.matrixcraft.client.lighting.DynamicLightTextureManager.ensureInit();
            com.raeyncraft.matrixcraft.client.lighting.DynamicLightTextureManager.updateTexture();
        } catch (Throwable e) {
            MatrixCraftMod.LOGGER.debug("[BulletTrailLighting] Texture update failed: " + e.getMessage());
        }
    }
    
    /**
     * Get the light level at a position (for dynamic lights integration)
     */
    public static int getLightLevel(BlockPos pos) {
        LightSource light = activeLights.get(pos);
        if (light != null) {
            return light.getCurrentBrightness();
        }
        return 0;
    }
    
    /**
     * Get the light color at a position (RGB 0-1 range)
     */
    public static float[] getLightColor(BlockPos pos) {
        LightSource light = activeLights.get(pos);
        if (light != null) {
            return new float[] { light.red, light.green, light.blue };
        }
        return null;
    }
    
    /**
     * Get all active light sources
     * Useful for shader integration and custom rendering
     */
    public static Map<BlockPos, LightSource> getActiveLights() {
        return activeLights;
    }
    
    /**
     * Get nearest light source to a position within range
     */
    public static LightSource getNearestLight(BlockPos pos, double maxRange) {
        LightSource nearest = null;
        double nearestDist = maxRange * maxRange;
        
        for (LightSource light : activeLights.values()) {
            double dist = light.position.distSqr(pos);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = light;
            }
        }
        
        return nearest;
    }
    
    /**
     * Clear all light sources
     */
    public static void clearAll() {
        activeLights.clear();
        try {
            com.raeyncraft.matrixcraft.client.lighting.DynamicLightTextureManager.clearTexture();
        } catch (Throwable e) {
            MatrixCraftMod.LOGGER.debug("[BulletTrailLighting] Clear texture failed: " + e.getMessage());
        }
    }
    
    /**
     * Prune oldest lights when at capacity
     */
    private static void pruneOldestLights(int count) {
        // Optimized approach: use partial sort (select N smallest without full sort)
        // This is O(n) instead of O(n log n)
        if (activeLights.size() <= count) {
            activeLights.clear();
            return;
        }
        
        // Find the Nth oldest light's ticksRemaining value
        int threshold = activeLights.values().stream()
            .mapToInt(ls -> ls.ticksRemaining)
            .sorted()
            .skip(count - 1)
            .findFirst()
            .orElse(Integer.MAX_VALUE);
        
        // Remove all lights with ticksRemaining <= threshold
        int removed = 0;
        Iterator<Map.Entry<BlockPos, LightSource>> it = activeLights.entrySet().iterator();
        while (it.hasNext() && removed < count) {
            Map.Entry<BlockPos, LightSource> entry = it.next();
            if (entry.getValue().ticksRemaining <= threshold) {
                it.remove();
                removed++;
            }
        }
    }
    
    /**
     * Get count of active lights (for debugging)
     */
    public static int getActiveLightCount() {
        return activeLights.size();
    }
}
