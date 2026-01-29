package com.raeyncraft.matrixcraft.client;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.particle.MatrixParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = MatrixCraftMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    
    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        MatrixCraftMod.LOGGER.info("========================================");
        MatrixCraftMod.LOGGER.info("REGISTERING PARTICLE PROVIDERS!");
        
        try {
            event.registerSpriteSet(MatrixParticles.BULLET_TRAIL.get(), 
                MatrixParticles.BulletTrailParticle.Provider::new);
            MatrixCraftMod.LOGGER.info("✓ Registered BULLET_TRAIL provider");
        } catch (Exception e) {
            MatrixCraftMod.LOGGER.error("✗ Failed to register BULLET_TRAIL: " + e.getMessage());
            e.printStackTrace();
        }
        
        // ADD THIS:
        try {
            event.registerSpriteSet(MatrixParticles.BULLET_IMPACT.get(), 
                MatrixParticles.BulletTrailParticle.Provider::new);
            MatrixCraftMod.LOGGER.info("✓ Registered BULLET_IMPACT provider");
        } catch (Exception e) {
            MatrixCraftMod.LOGGER.error("✗ Failed to register BULLET_IMPACT: " + e.getMessage());
            e.printStackTrace();
        }
        
        MatrixCraftMod.LOGGER.info("========================================");
    }
}
