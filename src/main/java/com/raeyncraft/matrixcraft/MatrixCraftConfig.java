package com.raeyncraft.matrixcraft;

import net.neoforged.neoforge.common.ModConfigSpec;

public class MatrixCraftConfig {
    public static final ModConfigSpec SPEC;
    
    // Bullet Trail Settings
    public static final ModConfigSpec.BooleanValue TRAILS_ENABLED;
    public static final ModConfigSpec.IntValue TRAIL_LENGTH;
    public static final ModConfigSpec.IntValue TRAIL_DENSITY;
    public static final ModConfigSpec.DoubleValue TRAIL_WIDTH;
    public static final ModConfigSpec.IntValue TRAIL_RED;
    public static final ModConfigSpec.IntValue TRAIL_GREEN;
    public static final ModConfigSpec.IntValue TRAIL_BLUE;
    public static final ModConfigSpec.DoubleValue TRAIL_ALPHA;
    public static final ModConfigSpec.BooleanValue TRAIL_GLOW;
    
    // Impact Effects Settings
    public static final ModConfigSpec.BooleanValue IMPACTS_ENABLED;
    public static final ModConfigSpec.IntValue IMPACT_PARTICLE_COUNT;
    public static final ModConfigSpec.DoubleValue IMPACT_PARTICLE_SPEED;
    public static final ModConfigSpec.DoubleValue IMPACT_RADIUS;
    
    // Performance Settings
    public static final ModConfigSpec.DoubleValue MAX_RENDER_DISTANCE;
    public static final ModConfigSpec.IntValue MAX_TRAILS_PER_TICK;
    
    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        
        builder.comment("Bullet Trail Settings").push("trails");
        
        TRAILS_ENABLED = builder
            .comment("Enable bullet trails")
            .define("enabled", true);
            
        TRAIL_LENGTH = builder
            .comment("Length of bullet trails in ticks (higher = longer trails)")
            .defineInRange("length", 15, 1, 100);
            
        TRAIL_DENSITY = builder
            .comment("Number of particles spawned per tick (higher = denser trail)")
            .defineInRange("density", 2, 1, 10);
            
        TRAIL_WIDTH = builder
            .comment("Width/size of trail particles")
            .defineInRange("width", 0.08, 0.01, 1.0);  // Reduced from 0.15 to 0.08
            
        TRAIL_RED = builder
            .comment("Red color component (0-255)")
            .defineInRange("red", 100, 0, 255);
            
        TRAIL_GREEN = builder
            .comment("Green color component (0-255)")
            .defineInRange("green", 255, 0, 255);
            
        TRAIL_BLUE = builder
            .comment("Blue color component (0-255)")
            .defineInRange("blue", 100, 0, 255);
            
        TRAIL_ALPHA = builder
            .comment("Trail transparency (0.0 = invisible, 1.0 = fully opaque)")
            .defineInRange("alpha", 0.5, 0.0, 1.0);  // Reduced from 0.8 to 0.5
            
        TRAIL_GLOW = builder
            .comment("Enable glowing effect on trails")
            .define("glow", true);
        
        builder.pop();
        
        builder.comment("Impact Effect Settings").push("impacts");
        
        IMPACTS_ENABLED = builder
            .comment("Enable impact particle effects")
            .define("enabled", true);
            
        IMPACT_PARTICLE_COUNT = builder
            .comment("Number of particles spawned on impact")
            .defineInRange("particleCount", 15, 1, 50);
            
        IMPACT_PARTICLE_SPEED = builder
            .comment("Speed of impact particles")
            .defineInRange("particleSpeed", 0.5, 0.1, 2.0);
            
        IMPACT_RADIUS = builder
            .comment("Spread radius of impact particles")
            .defineInRange("radius", 0.5, 0.1, 2.0);
        
        builder.pop();
        
        builder.comment("Performance Settings").push("performance");
        
        MAX_RENDER_DISTANCE = builder
            .comment("Maximum distance to render bullet trails (in blocks)")
            .defineInRange("maxRenderDistance", 64.0, 16.0, 256.0);
            
        MAX_TRAILS_PER_TICK = builder
            .comment("Maximum number of trails to process per tick (prevents lag)")
            .defineInRange("maxTrailsPerTick", 100, 10, 500);
        
        builder.pop();
        
        SPEC = builder.build();
    }
}
