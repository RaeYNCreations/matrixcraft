package com.raeyncraft.matrixcraft;

import com.raeyncraft.matrixcraft.bullettime.registry.BulletTimeRegistry;
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
        
        // Register particles
        com.raeyncraft.matrixcraft.particle.MatrixParticles.register(modEventBus);
        LOGGER.info("Particles registered!");
        
        // Register bullet time system (items, effects)
        BulletTimeRegistry.register(modEventBus);
        LOGGER.info("Bullet Time system registered!");
        
        // Register configs
        // COMMON config - server-side settings (duration, cooldown, glass repair, cobwebs)
        modContainer.registerConfig(ModConfig.Type.COMMON, MatrixCraftConfig.COMMON_SPEC);
        // CLIENT config - visual settings (trails, colors, etc)
        modContainer.registerConfig(ModConfig.Type.CLIENT, MatrixCraftConfig.CLIENT_SPEC);
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
