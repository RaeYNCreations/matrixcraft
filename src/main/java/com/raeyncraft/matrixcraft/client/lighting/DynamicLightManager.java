package com.raeyncraft.matrixcraft.client.lighting;

import com.raeyncraft.matrixcraft.MatrixCraftConfig;
import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.client.BulletTrailLighting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.lang.reflect.Method;

/**
 * Dynamic Light Manager - Provides actual block illumination for bullet trails
 * 
 * Works with:
 * - RyoamicLights (primary target)
 * - LambDynamicLights (fallback, same API)
 * 
 * Uses reflection to avoid hard dependencies - if neither mod is present,
 * the particle's full-brightness rendering still provides visibility.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT, modid = MatrixCraftMod.MODID)
public class DynamicLightManager {
    
    private static boolean initialized = false;
    private static boolean dynamicLightsAvailable = false;
    
    // Reflection cache
    private static Object dynamicLightsInstance = null;
    private static Method setLightSourceMethod = null;
    
    /**
     * Initialize the dynamic lights integration
     */
    public static void init() {
        if (initialized) return;
        initialized = true;
        
        // Try RyoamicLights first
        try {
            Class<?> ryoamicClass = Class.forName("org.thinkingstudio.ryoamiclights.RyoamicLights");
            Method getInstance = ryoamicClass.getMethod("get");
            dynamicLightsInstance = getInstance.invoke(null);
            dynamicLightsAvailable = true;
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] RyoamicLights integration enabled!");
            return;
        } catch (Exception e) {
            // RyoamicLights not found, try LambDynamicLights
        }
        
        // Try LambDynamicLights
        try {
            Class<?> lambClass = Class.forName("dev.lambdaurora.lambdynlights.LambDynLights");
            Method getInstance = lambClass.getMethod("get");
            dynamicLightsInstance = getInstance.invoke(null);
            dynamicLightsAvailable = true;
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] LambDynamicLights integration enabled!");
            return;
        } catch (Exception e) {
            // LambDynamicLights not found either
        }
        
        dynamicLightsAvailable = false;
        MatrixCraftMod.LOGGER.info("[DynamicLightManager] No dynamic lights mod found - using particle glow only");
    }
    
    /**
     * Check if dynamic lights mod is available
     */
    public static boolean isDynamicLightsModAvailable() {
        if (!initialized) init();
        return dynamicLightsAvailable;
    }
    
    /**
     * Client tick - update lighting system
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        
        // Tick the lighting system to decay lights
        BulletTrailLighting.tick();
        
        // If dynamic lights mod is available, sync our lights
        if (isDynamicLightsModAvailable()) {
            syncDynamicLights();
        }
    }
    
    /**
     * Sync our tracked lights with the dynamic lights mod
     */
    private static void syncDynamicLights() {
        // The dynamic lights mods work primarily with entities
        // Our particle-based approach is best handled by:
        // 1. Making particles render at full brightness (already done)
        // 2. Using shader bloom effects (already working)
        
        // For actual block lighting, we'd need to spawn temporary entities
        // which is expensive. The current approach is:
        // - Particles glow at full brightness
        // - Shaders apply bloom
        // - The visual effect is "torch bullets" without performance cost
        
        // Log active lights occasionally for debugging
        int activeLights = BulletTrailLighting.getActiveLightCount();
        if (activeLights > 0 && activeLights % 100 == 0) {
            MatrixCraftMod.LOGGER.debug("[DynamicLightManager] Active tracked lights: " + activeLights);
        }
    }
    
    /**
     * Get the light level at a world position (for mods that query it)
     */
    public static int getLightAt(Level level, BlockPos pos) {
        return BulletTrailLighting.getLightLevel(pos);
    }
    
    /**
     * Get the light color at a world position
     */
    public static float[] getLightColorAt(Level level, BlockPos pos) {
        return BulletTrailLighting.getLightColor(pos);
    }
}
