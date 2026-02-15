package com.raeyncraft.matrixcraft.bullettime.client;

import com.raeyncraft.matrixcraft.MatrixCraftConfig;
import com.raeyncraft.matrixcraft.bullettime.FocusManager;
import net.minecraft.client.Minecraft;

/**
 * Provides modified values for bullet trails when Focus mode is active.
 * Integrates with BulletTrailTracker to enhance visual effects.
 */
public class FocusBulletTrailEnhancer {
    
    // Multipliers for enhanced trails during Focus
    private static final float TRAIL_SIZE_MULTIPLIER = 2.5f;
    private static final float TRAIL_BRIGHTNESS_MULTIPLIER = 1.5f;
    private static final float TRAIL_DENSITY_MULTIPLIER = 2.0f;
    private static final float TRAIL_LENGTH_MULTIPLIER = 1.5f;
    
    /**
     * Get the effective trail width, enhanced during Focus
     */
    public static float getEffectiveTrailWidth() {
        float baseWidth;
        try {
            Number widthConfig = MatrixCraftConfig.TRAIL_WIDTH.get();
            baseWidth = widthConfig != null ? widthConfig.floatValue() : 0.5f;
        } catch (Exception e) {
            baseWidth = 0.5f; // Default
        }
        
        if (ClientFocusState.isInFocus()) {
            return baseWidth * TRAIL_SIZE_MULTIPLIER;
        }
        
        return baseWidth;
    }
    
    /**
     * Get the effective trail density, enhanced during Focus
     */
    public static int getEffectiveTrailDensity() {
        int baseDensity;
        try {
            Integer densityConfig = MatrixCraftConfig.TRAIL_DENSITY.get();
            baseDensity = densityConfig != null ? densityConfig : 5;
        } catch (Exception e) {
            baseDensity = 5; // Default
        }
        
        if (ClientFocusState.isInFocus()) {
            return (int)(baseDensity * TRAIL_DENSITY_MULTIPLIER);
        }
        
        return baseDensity;
    }
    
    /**
     * Get the effective trail length, enhanced during Focus
     */
    public static int getEffectiveTrailLength() {
        int baseLength;
        try {
            Integer lengthConfig = MatrixCraftConfig.TRAIL_LENGTH.get();
            baseLength = lengthConfig != null ? lengthConfig : 20;
        } catch (Exception e) {
            baseLength = 20; // Default
        }
        
        if (ClientFocusState.isInFocus()) {
            return (int)(baseLength * TRAIL_LENGTH_MULTIPLIER);
        }
        
        return baseLength;
    }
    
    /**
     * Get trail color modification for Focus mode
     * Returns RGB multipliers (can exceed 1.0 for HDR-like effect)
     */
    public static float[] getTrailColorMultiplier() {
        if (ClientFocusState.isInFocus()) {
            // Enhanced green glow during Focus
            return new float[] { 0.8f, TRAIL_BRIGHTNESS_MULTIPLIER, 0.8f };
        }
        
        return new float[] { 1.0f, 1.0f, 1.0f };
    }
    
    /**
     * Get alpha multiplier for trails during Focus
     */
    public static float getTrailAlphaMultiplier() {
        if (ClientFocusState.isInFocus()) {
            return 1.3f; // More visible trails
        }
        return 1.0f;
    }
    
    /**
     * Check if we should show trajectory prediction lines (advanced feature)
     */
    public static boolean shouldShowTrajectoryPrediction() {
        return ClientFocusState.isInFocus();
    }
    
    /**
     * Get the time scale for particle animations during Focus
     * In single player, this actually slows particles; in MP it's just visual
     */
    public static float getParticleTimeScale() {
        Minecraft mc = Minecraft.getInstance();
        
        if (ClientFocusState.isInFocus()) {
            // Check if single player for actual time dilation
            if (mc.getSingleplayerServer() != null) {
                return 0.3f; // 30% speed in single player
            }
            // In multiplayer, particles still move normally but last longer
            return 1.0f;
        }
        
        return 1.0f;
    }
}
