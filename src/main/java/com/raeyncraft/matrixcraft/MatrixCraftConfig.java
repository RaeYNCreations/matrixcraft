package com.raeyncraft.matrixcraft;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;

public class MatrixCraftConfig {
    private static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

    public static ModConfigSpec COMMON_SPEC;
    public static ModConfigSpec CLIENT_SPEC;

    // Bullet Time Configs
    public static BooleanValue FOCUS_ENABLED;
    public static IntValue FOCUS_DURATION_SECONDS;
    public static IntValue FOCUS_COOLDOWN_SECONDS;
    public static IntValue FOCUS_BAR_COLOR_R;
    public static IntValue FOCUS_BAR_COLOR_G;
    public static IntValue FOCUS_BAR_COLOR_B;
    public static IntValue FOCUS_TINT_COLOR_R;
    public static IntValue FOCUS_TINT_COLOR_G;
    public static IntValue FOCUS_TINT_COLOR_B;
    public static DoubleValue FOCUS_TINT_INTENSITY;
    public static DoubleValue FOCUS_VIGNETTE_INTENSITY;
    public static BooleanValue FOCUS_LAVA_IMMUNITY;
    public static BooleanValue FOCUS_COBWEB_BYPASS;
    public static BooleanValue FOCUS_WATER_BYPASS;

    // Wall Run Configs
    public static BooleanValue WALLRUN_HORIZONTAL_ENABLED;
    public static IntValue WALLRUN_HORIZONTAL_MAX_DISTANCE;
    public static IntValue WALLRUN_HORIZONTAL_ANGLE_MIN;
    public static IntValue WALLRUN_HORIZONTAL_ANGLE_MAX;
    public static BooleanValue WALLRUN_VERTICAL_ENABLED;
    public static DoubleValue WALLRUN_VERTICAL_MAX_DISTANCE;
    public static IntValue WALLRUN_VERTICAL_ANGLE_MIN;
    public static IntValue WALLRUN_VERTICAL_ANGLE_MAX;

    // Bullet Trails Configs
    public static BooleanValue TRAILS_ENABLED;
    public static IntValue TRAIL_LENGTH;
    public static IntValue TRAIL_DENSITY;
    public static DoubleValue TRAIL_WIDTH;
    public static IntValue TRAIL_COLOR_R;
    public static IntValue TRAIL_COLOR_G;
    public static IntValue TRAIL_COLOR_B;
    public static DoubleValue TRAIL_ALPHA;
    public static BooleanValue TRAIL_GLOW;
    public static DoubleValue MAX_RENDER_DISTANCE;
    public static IntValue MAX_TRAILS_PER_TICK;
    public static BooleanValue TRAIL_DYNAMIC_LIGHTING;
    public static IntValue TRAIL_LIGHT_LEVEL;
    public static IntValue TRAIL_LIGHT_SPACING;
    public static IntValue TRAIL_LIGHT_DURATION_TICKS;
    public static BooleanValue TRAIL_CHAIN_ENABLED;
    public static IntValue TRAIL_CHAIN_COUNT;
    public static DoubleValue TRAIL_CHAIN_SPACING;

    // Safe Haven Configs
    public static IntValue SAFE_HAVEN_RADIUS;
    public static BooleanValue SAFE_HAVEN_DESPAWN_ENABLED;

    // Silent Hill Configs
    public static IntValue OBELISK_DURATION;
    public static IntValue OBELISK_COOLDOWN;

    static {
        COMMON_BUILDER.comment("MatrixCraft Common Configuration").push("matrixcraft");

        // Bullet Time Settings
        COMMON_BUILDER.comment("Bullet Time / Focus Mode Settings").push("focus");
        FOCUS_ENABLED = COMMON_BUILDER
                .comment("Enable Bullet Time / Focus Mode")
                .define("enabled", true);
        FOCUS_DURATION_SECONDS = COMMON_BUILDER
                .comment("Duration of Focus Mode in seconds")
                .defineInRange("duration_seconds", 5, 1, 120);
        FOCUS_COOLDOWN_SECONDS = COMMON_BUILDER
                .comment("Cooldown of Focus Mode in seconds")
                .defineInRange("cooldown_seconds", 30, 0, 600);
        FOCUS_LAVA_IMMUNITY = COMMON_BUILDER
                .comment("Immune to lava damage during Focus Mode")
                .define("lava_immunity", false);
        FOCUS_COBWEB_BYPASS = COMMON_BUILDER
                .comment("Cobwebs don't slow you during Focus Mode")
                .define("cobweb_bypass", false);
        FOCUS_WATER_BYPASS = COMMON_BUILDER
                .comment("Water doesn't slow you during Focus Mode")
                .define("water_bypass", false);
        COMMON_BUILDER.pop();

        // Wall Run Settings
        COMMON_BUILDER.comment("Wall Run Settings").push("wallrun");
        COMMON_BUILDER.comment("Horizontal Wall Run").push("horizontal");
        WALLRUN_HORIZONTAL_ENABLED = COMMON_BUILDER
                .comment("Enable Horizontal Wall Run")
                .define("enabled", true);
        WALLRUN_HORIZONTAL_MAX_DISTANCE = COMMON_BUILDER
                .comment("Maximum horizontal distance for wall run (blocks)")
                .defineInRange("max_distance", 20, 1, 50);
        WALLRUN_HORIZONTAL_ANGLE_MIN = COMMON_BUILDER
                .comment("Minimum angle for horizontal wall run (degrees)")
                .defineInRange("angle_min", 0, 0, 89);
        WALLRUN_HORIZONTAL_ANGLE_MAX = COMMON_BUILDER
                .comment("Maximum angle for horizontal wall run (degrees)")
                .defineInRange("angle_max", 45, 1, 90);
        COMMON_BUILDER.pop();
        COMMON_BUILDER.comment("Vertical Wall Run").push("vertical");
        WALLRUN_VERTICAL_ENABLED = COMMON_BUILDER
                .comment("Enable Vertical Wall Run")
                .define("enabled", true);
        WALLRUN_VERTICAL_MAX_DISTANCE = COMMON_BUILDER
                .comment("Maximum vertical distance for wall run (blocks)")
                .defineInRange("max_distance", 5.0, 1.0, 20.0);
        WALLRUN_VERTICAL_ANGLE_MIN = COMMON_BUILDER
                .comment("Minimum angle for vertical wall run (degrees)")
                .defineInRange("angle_min", 0, 0, 44);
        WALLRUN_VERTICAL_ANGLE_MAX = COMMON_BUILDER
                .comment("Maximum angle for vertical wall run (degrees)")
                .defineInRange("angle_max", 45, 1, 45);
        COMMON_BUILDER.pop();
        COMMON_BUILDER.pop();

        // Safe Haven Settings
        COMMON_BUILDER.comment("Safe Haven Obelisk Settings").push("safe_haven");
        SAFE_HAVEN_RADIUS = COMMON_BUILDER
                .comment("Radius of Safe Haven zone (blocks)")
                .defineInRange("radius", 32, 8, 128);
        SAFE_HAVEN_DESPAWN_ENABLED = COMMON_BUILDER
                .comment("Despawn hostile mobs entering Safe Haven zones")
                .define("despawn_enabled", true);
        COMMON_BUILDER.pop();

        // Silent Hill Mode Settings
        COMMON_BUILDER.comment("Silent Hill Mode Settings").push("silent_hill");
        OBELISK_DURATION = COMMON_BUILDER
            .comment("Duration of Silent Hill Mode in seconds")
            .defineInRange("obelisk_duration", 60, 10, 600);
        OBELISK_COOLDOWN = COMMON_BUILDER
            .comment("Cooldown of The Obelisk in seconds")
            .defineInRange("obelisk_cooldown", 120, 30, 1200);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.pop(); // End matrixcraft

        COMMON_SPEC = COMMON_BUILDER.build();

        // Client Configs
        CLIENT_BUILDER.comment("MatrixCraft Client Configuration").push("matrixcraft");

        // Focus Effects
        CLIENT_BUILDER.comment("Focus Mode Visual Effects").push("focus_effects");
        FOCUS_BAR_COLOR_R = CLIENT_BUILDER
                .comment("Focus bar color - Red component")
                .defineInRange("bar_color_r", 0, 0, 255);
        FOCUS_BAR_COLOR_G = CLIENT_BUILDER
                .comment("Focus bar color - Green component")
                .defineInRange("bar_color_g", 255, 0, 255);
        FOCUS_BAR_COLOR_B = CLIENT_BUILDER
                .comment("Focus bar color - Blue component")
                .defineInRange("bar_color_b", 0, 0, 255);
        FOCUS_TINT_COLOR_R = CLIENT_BUILDER
                .comment("Screen tint color - Red component")
                .defineInRange("tint_color_r", 0, 0, 255);
        FOCUS_TINT_COLOR_G = CLIENT_BUILDER
                .comment("Screen tint color - Green component")
                .defineInRange("tint_color_g", 255, 0, 255);
        FOCUS_TINT_COLOR_B = CLIENT_BUILDER
                .comment("Screen tint color - Blue component")
                .defineInRange("tint_color_b", 0, 0, 255);
        FOCUS_TINT_INTENSITY = CLIENT_BUILDER
                .comment("Screen tint intensity (0.0 = none, 1.0 = full)")
                .defineInRange("tint_intensity", 0.15, 0.0, 1.0);
        FOCUS_VIGNETTE_INTENSITY = CLIENT_BUILDER
                .comment("Vignette intensity (0.0 = none, 1.0 = full)")
                .defineInRange("vignette_intensity", 0.4, 0.0, 1.0);
        CLIENT_BUILDER.pop();

        // Bullet Trails
        CLIENT_BUILDER.comment("Bullet Trail Effects").push("bullet_trails");
        TRAILS_ENABLED = CLIENT_BUILDER
                .comment("Enable bullet trail effects")
                .define("enabled", true);
        TRAIL_LENGTH = CLIENT_BUILDER
                .comment("Length of bullet trails (ticks)")
                .defineInRange("length", 20, 1, 100);
        TRAIL_DENSITY = CLIENT_BUILDER
                .comment("Density of trail particles (particles per tick)")
                .defineInRange("density", 2, 1, 10);
        TRAIL_WIDTH = CLIENT_BUILDER
                .comment("Width of trail particles")
                .defineInRange("width", 0.1, 0.01, 1.0);
        TRAIL_COLOR_R = CLIENT_BUILDER
                .comment("Trail color - Red component")
                .defineInRange("color_r", 255, 0, 255);
        TRAIL_COLOR_G = CLIENT_BUILDER
                .comment("Trail color - Green component")
                .defineInRange("color_g", 255, 0, 255);
        TRAIL_COLOR_B = CLIENT_BUILDER
                .comment("Trail color - Blue component")
                .defineInRange("color_b", 0, 0, 255);
        TRAIL_ALPHA = CLIENT_BUILDER
                .comment("Trail transparency (0.0 = invisible, 1.0 = opaque)")
                .defineInRange("alpha", 0.8, 0.0, 1.0);
        TRAIL_GLOW = CLIENT_BUILDER
                .comment("Trail particles glow in the dark")
                .define("glow", true);
        MAX_RENDER_DISTANCE = CLIENT_BUILDER
                .comment("Maximum render distance for trails (blocks)")
                .defineInRange("max_render_distance", 128.0, 16.0, 256.0);
        MAX_TRAILS_PER_TICK = CLIENT_BUILDER
                .comment("Maximum trail particles per tick")
                .defineInRange("max_per_tick", 100, 10, 500);
        TRAIL_DYNAMIC_LIGHTING = CLIENT_BUILDER
                .comment("Trails emit dynamic light")
                .define("dynamic_lighting", true);
        TRAIL_LIGHT_LEVEL = CLIENT_BUILDER
                .comment("Light level emitted by trails")
                .defineInRange("light_level", 10, 1, 15);
        TRAIL_LIGHT_SPACING = CLIENT_BUILDER
                .comment("Spacing between light sources along trail")
                .defineInRange("light_spacing", 5, 1, 50);
        TRAIL_LIGHT_DURATION_TICKS = CLIENT_BUILDER
                .comment("Duration of light emission (ticks)")
                .defineInRange("light_duration_ticks", 60, 1, 1200);
        TRAIL_CHAIN_ENABLED = CLIENT_BUILDER
                .comment("Enable chained light effects")
                .define("chain_enabled", true);
        TRAIL_CHAIN_COUNT = CLIENT_BUILDER
                .comment("Number of chained lights")
                .defineInRange("chain_count", 3, 1, 8);
        TRAIL_CHAIN_SPACING = CLIENT_BUILDER
                .comment("Spacing between chained lights")
                .defineInRange("chain_spacing", 1.0, 0.0, 5.0);
        CLIENT_BUILDER.pop();

        CLIENT_BUILDER.pop(); // End matrixcraft

        CLIENT_SPEC = CLIENT_BUILDER.build();
    }

    // Helper methods for client-side calculations
    public static int getFocusDurationTicks() {
        return FOCUS_DURATION_SECONDS.get() * 20;
    }
    
    public static int getFocusCooldownTicks() {
        return FOCUS_COOLDOWN_SECONDS.get() * 20;
    }
    
    public static int getVignetteAlpha(float intensity) {
        return (int) (intensity * 255);
    }
    
    public static int getFocusTintColor(float intensity) {
        int r = (int) (FOCUS_TINT_COLOR_R.get() * intensity);
        int g = (int) (FOCUS_TINT_COLOR_G.get() * intensity);
        int b = (int) (FOCUS_TINT_COLOR_B.get() * intensity);
        return (r << 16) | (g << 8) | b | 0xFF000000;
    }
    
    public static int getFocusBarBorderColor() {
        return 0xFF000000; // Black border
    }
    
    public static int getFocusBarColor() {
        int r = FOCUS_BAR_COLOR_R.get();
        int g = FOCUS_BAR_COLOR_G.get();
        int b = FOCUS_BAR_COLOR_B.get();
        return (r << 16) | (g << 8) | b | 0xFF000000;
    }
    
    public static int getFocusBarHighlightColor() {
        int r = Math.min(255, FOCUS_BAR_COLOR_R.get() + 50);
        int g = Math.min(255, FOCUS_BAR_COLOR_G.get() + 50);
        int b = Math.min(255, FOCUS_BAR_COLOR_B.get() + 50);
        return (r << 16) | (g << 8) | b | 0xFF000000;
    }
    
    public static int getFocusTextColor(float alpha) {
        return ((int) (alpha * 255) << 24) | 0x00FFFFFF;
    }
    
    public static int getFocusTextShadowColor() {
        return 0xFF000000;
    }

    public static void saveCommonConfig() {
        // Save common config if needed
    }

    public static void saveClientConfig() {
        // Save client config if needed
    }
}