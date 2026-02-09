package com.raeyncraft.matrixcraft;

import com.raeyncraft.matrixcraft.registry.ModBlocks;
import com.raeyncraft.matrixcraft.bullettime.registry.BulletTimeRegistry;
import com.raeyncraft.matrixcraft.wallrun.MatrixWallRunEventHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MatrixCraftMod.MODID)
public class MatrixCraftMod {
    public static final String MODID = "matrixcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MatrixCraftMod.class);

    public MatrixCraftMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("========================================");
        LOGGER.info("MatrixCraft initializing...");
        LOGGER.info("========================================");
        
        // Register particles
        com.raeyncraft.matrixcraft.particle.MatrixParticles.register(modEventBus);
        LOGGER.info("Particles registered!");
        
        // Register bullet time system (items, effects)
        BulletTimeRegistry.register(modEventBus);
        LOGGER.info("Bullet Time system registered!");

        // Register items (The Obelisk, etc.)
        ModItems.ITEMS.register(modEventBus);
        LOGGER.info("Items registered!");
        
        // MANUALLY REGISTER WALL RUN EVENT HANDLER
        NeoForge.EVENT_BUS.register(MatrixWallRunEventHandler.class);
        LOGGER.info("Wall run event handler registered!");

        // REGISTER SILENT HILL MODE EVENTS
        NeoForge.EVENT_BUS.register(SilentHillMode.class);
        LOGGER.info("Silent Hill Mode registered!");

        // Register Custom Blocks
        ModBlocks.BLOCKS.register(modEventBus);

        // Register configs
        modContainer.registerConfig(ModConfig.Type.COMMON, MatrixCraftConfig.COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, MatrixCraftConfig.CLIENT_SPEC);
        LOGGER.info("Config registered!");
        
        // Client setup
        modEventBus.addListener(this::clientSetup);
        
        LOGGER.info("========================================");
        LOGGER.info("MatrixCraft loaded successfully!");
        LOGGER.info("========================================");
    }
    
    // Silent Hill / Obelisk Config
    public static ModConfigSpec.IntValue OBELISK_DURATION;
    public static ModConfigSpec.IntValue OBELISK_COOLDOWN;
    
    static {
        ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
        ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();
        
        // ... (your existing config)
        
        // Silent Hill Mode Settings
        COMMON_BUILDER.comment("Silent Hill Mode Settings").push("silent_hill");
        
        OBELISK_DURATION = COMMON_BUILDER
            .comment("Duration of Silent Hill Mode in seconds")
            .defineInRange("obelisk_duration", 60, 10, 600);
        
        OBELISK_COOLDOWN = COMMON_BUILDER
            .comment("Cooldown of The Obelisk in seconds")
            .defineInRange("obelisk_cooldown", 120, 30, 1200);
        
        COMMON_BUILDER.pop();
        
        COMMON_SPEC = COMMON_BUILDER.build();
        CLIENT_SPEC = CLIENT_BUILDER.build();
    }
    
    // Save method (add this if you don't have it)
    public static void saveCommonConfig() {
        COMMON_SPEC.save();
    }

    private void clientSetup(FMLClientSetupEvent event) {
        LOGGER.info("MatrixCraft client setup complete!");
    }
}