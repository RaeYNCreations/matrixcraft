package com.raeyncraft.matrixcraft.client;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import net.minecraft.client.particle.Particle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@EventBusSubscriber(modid = MatrixCraftMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class DynamicLightingHandler {
    
    private static boolean lambDynLightsAvailable = false;
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                Class<?> handlersClass = Class.forName("dev.lambdaurora.lambdynlights.api.DynamicLightHandlers");
                
                MatrixCraftMod.LOGGER.info("LambDynamicLights API found! Examining registerDynamicLightHandler method...");
                
                // Find the 2-param registerDynamicLightHandler method and check its types
                Method registerMethod = null;
                for (Method m : handlersClass.getMethods()) {
                    if (m.getName().equals("registerDynamicLightHandler") && m.getParameterCount() == 2) {
                        registerMethod = m;
                        Type[] paramTypes = m.getGenericParameterTypes();
                        MatrixCraftMod.LOGGER.info("Found method with parameters:");
                        MatrixCraftMod.LOGGER.info("  Param 0: " + paramTypes[0]);
                        MatrixCraftMod.LOGGER.info("  Param 1: " + paramTypes[1]);
                        break;
                    }
                }
                
                if (registerMethod != null) {
                    // Register trail particles
                    try {
                        registerMethod.invoke(null,
                            com.raeyncraft.matrixcraft.particle.MatrixParticles.BulletTrailParticle.class,
                            (java.util.function.Function<Particle, Integer>) DynamicLightingHandler::getBulletTrailLuminance
                        );
                        MatrixCraftMod.LOGGER.info("✓ Registered bullet trail particles with dynamic lighting!");
                    } catch (Exception e) {
                        MatrixCraftMod.LOGGER.warn("Failed to register trail particles: " + e.getMessage());
                        e.printStackTrace();
                    }
                    
                    // Register impact particles
                    try {
                        registerMethod.invoke(null,
                            com.raeyncraft.matrixcraft.particle.MatrixParticles.BulletImpactParticle.class,
                            (java.util.function.Function<Particle, Integer>) DynamicLightingHandler::getImpactLuminance
                        );
                        MatrixCraftMod.LOGGER.info("✓ Registered impact particles with dynamic lighting!");
                    } catch (Exception e) {
                        MatrixCraftMod.LOGGER.warn("Failed to register impact particles: " + e.getMessage());
                        e.printStackTrace();
                    }
                    
                    lambDynLightsAvailable = true;
                    MatrixCraftMod.LOGGER.info("========================================");
                    MatrixCraftMod.LOGGER.info("✓ Dynamic lighting fully operational!");
                    MatrixCraftMod.LOGGER.info("========================================");
                }
                
            } catch (ClassNotFoundException e) {
                MatrixCraftMod.LOGGER.info("LambDynamicLights not found - particles will glow but won't emit world light");
            } catch (Exception e) {
                MatrixCraftMod.LOGGER.warn("Failed to register dynamic lighting: " + e.getClass().getSimpleName());
                e.printStackTrace();
            }
        });
    }
    
    private static int getBulletTrailLuminance(Particle particle) {
        try {
            Field ageField = Particle.class.getDeclaredField("age");
            Field lifetimeField = Particle.class.getDeclaredField("lifetime");
            ageField.setAccessible(true);
            lifetimeField.setAccessible(true);
            
            int age = ageField.getInt(particle);
            int lifetime = lifetimeField.getInt(particle);
            
            if (lifetime == 0) return 12;
            
            float ageRatio = age / (float) lifetime;
            
            // Bright trail: 14 → 8
            int lightLevel = (int) (14 - (ageRatio * 6));
            return Math.max(8, Math.min(15, lightLevel));
        } catch (Exception e) {
            return 12;
        }
    }
    
    private static int getImpactLuminance(Particle particle) {
        try {
            Field ageField = Particle.class.getDeclaredField("age");
            Field lifetimeField = Particle.class.getDeclaredField("lifetime");
            ageField.setAccessible(true);
            lifetimeField.setAccessible(true);
            
            int age = ageField.getInt(particle);
            int lifetime = lifetimeField.getInt(particle);
            
            if (lifetime == 0) return 15;
            
            float ageRatio = age / (float) lifetime;
            
            // Very bright impact: 15 → 6
            int lightLevel = (int) (15 - (ageRatio * 9));
            return Math.max(6, Math.min(15, lightLevel));
        } catch (Exception e) {
            return 15;
        }
    }
    
    public static boolean isDynamicLightingAvailable() {
        return lambDynLightsAvailable;
    }
}
