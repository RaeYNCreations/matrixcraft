package com.raeyncraft.matrixcraft;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MatrixCraftMod.MODID)
public class MatrixCraftMod {
    public static final String MODID = "matrixcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MatrixCraftMod.class);

    public MatrixCraftMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("MatrixCraft initializing...");
        
        // Register config
        modContainer.registerConfig(ModConfig.Type.CLIENT, MatrixCraftConfig.SPEC);
        
        LOGGER.info("MatrixCraft loaded successfully!");
    }
}