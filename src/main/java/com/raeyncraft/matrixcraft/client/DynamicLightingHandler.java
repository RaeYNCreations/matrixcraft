package com.raeyncraft.matrixcraft.client;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import net.minecraft.client.particle.Particle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@EventBusSubscriber(modid = MatrixCraftMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class DynamicLightingHandler {
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                // Use reflection to register with LambDynamicLights if present
                Class<?> handlersClass = Class.forName("dev.lambdaurora.lambdynlights.api.DynamicLightHandlers");
                Method registerMethod = handlersClass.getMethod("registerDynamicLightHandler", Class.class, java.util.function.Function.class);
                
                // Register bullet trail particles
                registerMethod.invoke(null, 
                    com.raeyncraft.matrixcraft.particle.MatrixParticles.BulletTrailParticle.class,
                    (java.util.function.Function<Particle, Integer>) DynamicLightingHandler::getBulletTrailLuminance
                );
                
                // Register impact particles
                registerMethod.invoke(null,
                    com.raeyncraft.matrixcraft.particle.MatrixParticles.BulletImpactParticle.class,
                    (java.util.function.Function<Particle, Integer>) DynamicLightingHandler::getImpactLuminance
                );
                
                MatrixCraftMod.LOGGER.info("Successfully registered dynamic lighting for MatrixCraft particles!");
            } catch (ClassNotFoundException e) {
                MatrixCraftMod.LOGGER.info("LambDynamicLights not found - particles will still be emissive but won't emit dynamic light");
            } catch (Exception e) {
                MatrixCraftMod.LOGGER.warn("Failed to register dynamic lighting: " + e.getMessage());
            }
        });
    }
    
    // Calculate light level for bullet trail particles
    private static int getBulletTrailLuminance(Particle particle) {
        try {
            // Use reflection to access particle age fields
            Field ageField = Particle.class.getDeclaredField("age");
            Field lifetimeField = Particle.class.getDeclaredField("lifetime");
            ageField.setAccessible(true);
            lifetimeField.setAccessible(true);
            
            int age = ageField.getInt(particle);
            int lifetime = lifetimeField.getInt(particle);
            
            if (lifetime == 0) return 8;
            
            float ageRatio = age / (float) lifetime;
            
            // Start at light level 12, fade to 4 over lifetime
            int lightLevel = (int) (12 - (ageRatio * 8));
            return Math.max(4, Math.min(15, lightLevel));
        } catch (Exception e) {
            return 8; // Default medium brightness if reflection fails
        }
    }
    
    // Calculate light level for impact particles
    private static int getImpactLuminance(Particle particle) {
        try {
            // Use reflection to access particle age fields
            Field ageField = Particle.class.getDeclaredField("age");
            Field lifetimeField = Particle.class.getDeclaredField("lifetime");
            ageField.setAccessible(true);
            lifetimeField.setAccessible(true);
            
            int age = ageField.getInt(particle);
            int lifetime = lifetimeField.getInt(particle);
            
            if (lifetime == 0) return 12;
            
            float ageRatio = age / (float) lifetime;
            
            // Start very bright (15), fade quickly
            int lightLevel = (int) (15 - (ageRatio * 13));
            return Math.max(2, Math.min(15, lightLevel));
        } catch (Exception e) {
            return 12; // Default bright if reflection fails
        }
    }
}
