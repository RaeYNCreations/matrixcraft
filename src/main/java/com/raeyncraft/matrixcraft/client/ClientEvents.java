package com.raeyncraft.matrixcraft.client;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.particle.MatrixParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

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

@EventBusSubscriber(modid = MatrixCraftMod.MODID, value = Dist.CLIENT)
class ClientForgeEvents {
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        // Reset the per-tick counter for performance limiting
        BulletTrailTracker.resetTickCounter();
    }
}