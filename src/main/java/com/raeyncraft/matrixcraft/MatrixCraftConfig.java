package com.raeyncraft.matrixcraft;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * MatrixCraft configuration
 */
public class MatrixCraftConfig {
    public static final Common COMMON;
    public static final ModConfigSpec COMMON_SPEC;
    
    public static final Client CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;
    
    // Backwards compatibility alias
    public static final ModConfigSpec SPEC;
    
    static {
        Pair<Common, ModConfigSpec> commonPair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = commonPair.getLeft();
        COMMON_SPEC = commonPair.getRight();
        
        Pair<Client, ModConfigSpec> clientPair = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT = clientPair.getLeft();
        CLIENT_SPEC = clientPair.getRight();
        
        // Backwards compatibility - SPEC points to CLIENT_SPEC
        SPEC = CLIENT_SPEC;
    }
    
    /**
     * Force save the client config to disk
     */
    public static void saveClientConfig() {
        if (CLIENT_SPEC.isLoaded()) {
            CLIENT_SPEC.save();
        }
    }
    
    /**
     * Force save the common config to disk
     */
    public static void saveCommonConfig() {
        if (COMMON_SPEC.isLoaded()) {
            COMMON_SPEC.save();
        }
    }
    
    // ==================== EASY ACCESS (for backwards compatibility) ====================
    
    // Bullet Trails
    public static ModConfigSpec.BooleanValue TRAILS_ENABLED;
    public static ModConfigSpec.IntValue TRAIL_LENGTH;
    public static ModConfigSpec.IntValue TRAIL_DENSITY;
    public static ModConfigSpec.DoubleValue TRAIL_WIDTH;
    public static ModConfigSpec.IntValue TRAIL_COLOR_R;
    public static ModConfigSpec.IntValue TRAIL_COLOR_G;
    public static ModConfigSpec.IntValue TRAIL_COLOR_B;
    public static ModConfigSpec.DoubleValue TRAIL_ALPHA;
    public static ModConfigSpec.BooleanValue TRAIL_GLOW;
    public static ModConfigSpec.DoubleValue MAX_RENDER_DISTANCE;
    public static ModConfigSpec.IntValue MAX_TRAILS_PER_TICK;
    
    // Glass Repair
    public static ModConfigSpec.BooleanValue GLASS_REPAIR_ENABLED;
    public static ModConfigSpec.IntValue GLASS_REPAIR_DELAY;
    
    // Cobwebs
    public static ModConfigSpec.BooleanValue COBWEBS_ENABLED;
    
    // Bullet Time / Focus Colors
    public static ModConfigSpec.IntValue FOCUS_BAR_COLOR_R;
    public static ModConfigSpec.IntValue FOCUS_BAR_COLOR_G;
    public static ModConfigSpec.IntValue FOCUS_BAR_COLOR_B;
    public static ModConfigSpec.IntValue FOCUS_TINT_COLOR_R;
    public static ModConfigSpec.IntValue FOCUS_TINT_COLOR_G;
    public static ModConfigSpec.IntValue FOCUS_TINT_COLOR_B;
    public static ModConfigSpec.DoubleValue FOCUS_TINT_INTENSITY;
    public static ModConfigSpec.DoubleValue FOCUS_VIGNETTE_INTENSITY;
    
    // Bullet Time Duration and Cooldown
    public static ModConfigSpec.IntValue FOCUS_DURATION_SECONDS;
    public static ModConfigSpec.IntValue FOCUS_COOLDOWN_SECONDS;
    
    public static class Common {
        public Common(ModConfigSpec.Builder builder) {
            builder.comment("MatrixCraft Common Configuration").push("common");
            
            // Glass Repair
            builder.comment("Glass Repair System").push("glassrepair");
            GLASS_REPAIR_ENABLED = builder
                .comment("Enable automatic glass repair")
                .define("enabled", true);
            GLASS_REPAIR_DELAY = builder
                .comment("Delay in seconds before glass repairs")
                .defineInRange("delay", 5, 1, 3600);
            builder.pop();
            
            // Cobwebs
            builder.comment("Cobweb Settings").push("cobwebs");
            COBWEBS_ENABLED = builder
                .comment("Enable cobweb slowdown (false = walk through freely)")
                .define("enabled", true);
            builder.pop();
            
            // Bullet Time Duration and Cooldown (server-side settings)
            builder.comment("Bullet Time Duration and Cooldown").push("bullettime");
            FOCUS_DURATION_SECONDS = builder
                .comment("Focus mode duration in seconds")
                .defineInRange("durationSeconds", 10, 1, 120);
            FOCUS_COOLDOWN_SECONDS = builder
                .comment("Cooldown between uses in seconds")
                .defineInRange("cooldownSeconds", 60, 0, 600);
            builder.pop();
            
            builder.pop();
        }
    }
    
    public static class Client {
        public Client(ModConfigSpec.Builder builder) {
            builder.comment("MatrixCraft Client Configuration").push("client");
            
            // ==================== BULLET TRAILS ====================
            builder.comment("Bullet Trail Settings").push("bullettrails");
            
            TRAILS_ENABLED = builder
                .comment("Enable bullet trails")
                .define("enabled", true);
            TRAIL_LENGTH = builder
                .comment("Trail length in ticks")
                .defineInRange("length", 20, 1, 100);
            TRAIL_DENSITY = builder
                .comment("Particles per tick")
                .defineInRange("density", 3, 1, 10);
            TRAIL_WIDTH = builder
                .comment("Trail width")
                .defineInRange("width", 0.05, 0.01, 1.0);
            TRAIL_COLOR_R = builder
                .comment("Trail color - Red (0-255)")
                .defineInRange("colorR", 0, 0, 255);
            TRAIL_COLOR_G = builder
                .comment("Trail color - Green (0-255)")
                .defineInRange("colorG", 255, 0, 255);
            TRAIL_COLOR_B = builder
                .comment("Trail color - Blue (0-255)")
                .defineInRange("colorB", 0, 0, 255);
            TRAIL_ALPHA = builder
                .comment("Trail transparency (0.0-1.0)")
                .defineInRange("alpha", 0.8, 0.0, 1.0);
            TRAIL_GLOW = builder
                .comment("Enable glow effect on trails")
                .define("glow", true);
            MAX_RENDER_DISTANCE = builder
                .comment("Maximum render distance for trails")
                .defineInRange("maxDistance", 64.0, 16.0, 256.0);
            MAX_TRAILS_PER_TICK = builder
                .comment("Maximum trails to render per tick")
                .defineInRange("maxPerTick", 100, 10, 500);
            
            builder.pop();
            
            // ==================== BULLET TIME / FOCUS ====================
            builder.comment("Bullet Time (Focus Mode) Visual Settings").push("bullettime");
            
            // Focus Bar Color
            builder.comment("Focus Bar Color").push("focusbar");
            FOCUS_BAR_COLOR_R = builder
                .comment("Focus bar color - Red (0-255)")
                .defineInRange("colorR", 0, 0, 255);
            FOCUS_BAR_COLOR_G = builder
                .comment("Focus bar color - Green (0-255)")
                .defineInRange("colorG", 255, 0, 255);
            FOCUS_BAR_COLOR_B = builder
                .comment("Focus bar color - Blue (0-255)")
                .defineInRange("colorB", 0, 0, 255);
            builder.pop();
            
            // Screen Tint Color
            builder.comment("Screen Tint / Vignette Color").push("screentint");
            FOCUS_TINT_COLOR_R = builder
                .comment("Screen tint color - Red (0-255)")
                .defineInRange("colorR", 0, 0, 255);
            FOCUS_TINT_COLOR_G = builder
                .comment("Screen tint color - Green (0-255)")
                .defineInRange("colorG", 255, 0, 255);
            FOCUS_TINT_COLOR_B = builder
                .comment("Screen tint color - Blue (0-255)")
                .defineInRange("colorB", 0, 0, 255);
            FOCUS_TINT_INTENSITY = builder
                .comment("Screen tint intensity (0.0-1.0)")
                .defineInRange("tintIntensity", 0.15, 0.0, 1.0);
            FOCUS_VIGNETTE_INTENSITY = builder
                .comment("Vignette (edge darkening) intensity (0.0-1.0)")
                .defineInRange("vignetteIntensity", 0.4, 0.0, 1.0);
            builder.pop();
            
            builder.pop();
            
            builder.pop();
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Safely get an int config value with default
     */
    private static int safeGetInt(ModConfigSpec.IntValue config, int defaultVal) {
        try {
            if (config != null) {
                return config.get();
            }
        } catch (Exception e) {
            // Config not loaded yet
        }
        return defaultVal;
    }
    
    /**
     * Safely get a double config value with default
     */
    private static double safeGetDouble(ModConfigSpec.DoubleValue config, double defaultVal) {
        try {
            if (config != null) {
                return config.get();
            }
        } catch (Exception e) {
            // Config not loaded yet
        }
        return defaultVal;
    }
    
    /**
     * Get focus bar color as ARGB int
     */
    public static int getFocusBarColor() {
        int r = safeGetInt(FOCUS_BAR_COLOR_R, 0);
        int g = safeGetInt(FOCUS_BAR_COLOR_G, 255);
        int b = safeGetInt(FOCUS_BAR_COLOR_B, 0);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Get focus bar border color (darker version)
     */
    public static int getFocusBarBorderColor() {
        int r = safeGetInt(FOCUS_BAR_COLOR_R, 0);
        int g = safeGetInt(FOCUS_BAR_COLOR_G, 255);
        int b = safeGetInt(FOCUS_BAR_COLOR_B, 0);
        return 0xFF000000 | ((r / 3) << 16) | ((g / 3) << 8) | (b / 3);
    }
    
    /**
     * Get focus bar highlight color (lighter version)
     */
    public static int getFocusBarHighlightColor() {
        int r = Math.min(255, safeGetInt(FOCUS_BAR_COLOR_R, 0) + 68);
        int g = Math.min(255, safeGetInt(FOCUS_BAR_COLOR_G, 255) + 68);
        int b = Math.min(255, safeGetInt(FOCUS_BAR_COLOR_B, 0) + 68);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Get focus tint color as ARGB int with intensity
     */
    public static int getFocusTintColor(float transitionProgress) {
        double intensity = safeGetDouble(FOCUS_TINT_INTENSITY, 0.15);
        int r = safeGetInt(FOCUS_TINT_COLOR_R, 0);
        int g = safeGetInt(FOCUS_TINT_COLOR_G, 255);
        int b = safeGetInt(FOCUS_TINT_COLOR_B, 0);
        int alpha = (int)(255 * intensity * transitionProgress);
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Get vignette alpha based on intensity and transition
     */
    public static int getVignetteAlpha(float transitionProgress) {
        double intensity = safeGetDouble(FOCUS_VIGNETTE_INTENSITY, 0.4);
        return (int)(200 * intensity * transitionProgress);
    }
    
    /**
     * Get text color matching focus bar
     */
    public static int getFocusTextColor(float alpha) {
        int r = safeGetInt(FOCUS_BAR_COLOR_R, 0);
        int g = safeGetInt(FOCUS_BAR_COLOR_G, 255);
        int b = safeGetInt(FOCUS_BAR_COLOR_B, 0);
        int a = (int)(255 * alpha);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Get text shadow color (darker version of bar color)
     */
    public static int getFocusTextShadowColor() {
        int r = safeGetInt(FOCUS_BAR_COLOR_R, 0);
        int g = safeGetInt(FOCUS_BAR_COLOR_G, 255);
        int b = safeGetInt(FOCUS_BAR_COLOR_B, 0);
        return 0xFF000000 | ((r / 4) << 16) | ((g / 4) << 8) | (b / 4);
    }
    
    /**
     * Get focus duration in ticks (20 ticks = 1 second)
     */
    public static int getFocusDurationTicks() {
        return safeGetInt(FOCUS_DURATION_SECONDS, 10) * 20;
    }
    
    /**
     * Get focus cooldown in ticks (20 ticks = 1 second)
     */
    public static int getFocusCooldownTicks() {
        return safeGetInt(FOCUS_COOLDOWN_SECONDS, 60) * 20;
    }
}
