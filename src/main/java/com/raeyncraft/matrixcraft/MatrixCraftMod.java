package com.raeyncraft.matrixcraft;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
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
        
        // Register particles - THIS IS CRITICAL!
        com.raeyncraft.matrixcraft.particle.MatrixParticles.register(modEventBus);
        LOGGER.info("Particles registered!");
        
        // Register config
        modContainer.registerConfig(ModConfig.Type.CLIENT, MatrixCraftConfig.SPEC);
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
