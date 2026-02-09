package com.raeyncraft.matrixcraft;

import com.raeyncraft.matrixcraft.registry.ModBlocks;
import com.raeyncraft.matrixcraft.bullettime.registry.BulletTimeRegistry;
import com.raeyncraft.matrixcraft.wallrun.MatrixWallRunEventHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.config.ModConfig;  // Add this import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.raeyncraft.matrixcraft.registry.ModItems;
import com.raeyncraft.matrixcraft.silenthill.SilentHillMode;
import com.raeyncraft.matrixcraft.registry.ModCreativeTabEvents;
import com.raeyncraft.matrixcraft.client.SilentHillEffects;

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
        
        // MANUALLY REGISTER WALL RUN EVENT HANDLER
        NeoForge.EVENT_BUS.register(MatrixWallRunEventHandler.class);
        LOGGER.info("Wall run event handler registered!");

        // REGISTER SILENT HILL MODE EVENTS
        NeoForge.EVENT_BUS.register(SilentHillMode.class);
        LOGGER.info("Silent Hill Mode registered!");

        // Register Custom Blocks
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        NeoForge.EVENT_BUS.register(SilentHillMode.class);

        // Register configs
        modContainer.registerConfig(ModConfig.Type.COMMON, MatrixCraftConfig.COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, MatrixCraftConfig.CLIENT_SPEC);
        modEventBus.register(ModCreativeTabEvents.class);
        LOGGER.info("Config registered!");
                
        // Client setup
        modEventBus.addListener(this::clientSetup);
        
        LOGGER.info("========================================");
        LOGGER.info("MatrixCraft loaded successfully!");
        LOGGER.info("========================================");
    }
    
    private void clientSetup(FMLClientSetupEvent event) {
        LOGGER.info("MatrixCraft client setup complete!");
    }
}