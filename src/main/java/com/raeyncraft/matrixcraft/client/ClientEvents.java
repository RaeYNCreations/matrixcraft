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
        event.registerSpriteSet(MatrixParticles.BULLET_TRAIL.get(), 
            MatrixParticles.BulletTrailParticle.Provider::new);
        event.registerSpriteSet(MatrixParticles.BULLET_IMPACT.get(), 
            MatrixParticles.BulletImpactParticle.Provider::new);
    }
}
