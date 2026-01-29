package com.raeyncraft.matrixcraft.client.lighting;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.client.BulletTrailLighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Packs active trail lights into a small 2D texture (two rows total).
 *
 * Row 0: Position (RGB) + Intensity (A)
 * Row 1: Color (RGB) + Unused (A)
 *
 * Shader-side include will sample sampler2D matrixcraft_trail_lights;
 *
 * This class uses reflection to construct a ResourceLocation that is compatible
 * across mappings/MC versions that expose either a single-string or two-string
 * constructor.
 */
public class DynamicLightTextureManager {
    private static final ResourceLocation TEX_LOC = createResourceLocation("matrixcraft", "trail_lights");
    private static DynamicTexture dynamicTexture;
    private static NativeImage nativeImage;

    private static final int MAX_TRAIL_LIGHTS = 64;
    private static final float POSITION_RANGE = 256.0f;
    private static final int TEXTURE_UNIT = 12;

    private static boolean initialized = false;

    @SuppressWarnings("unchecked")
    private static ResourceLocation createResourceLocation(String namespace, String path) {
        // Try declared constructors via reflection without referencing them at compile time.
        try {
            // First try (String, String)
            Constructor<?> ctor2 = null;
            Constructor<?> ctor1 = null;
            for (Constructor<?> c : ResourceLocation.class.getDeclaredConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                if (params.length == 2 && params[0] == String.class && params[1] == String.class) {
                    ctor2 = c;
                    break;
                } else if (params.length == 1 && params[0] == String.class) {
                    ctor1 = c; // keep as fallback
                }
            }
            if (ctor2 != null) {
                ctor2.setAccessible(true);
                return (ResourceLocation) ctor2.newInstance(namespace, path);
            }
            if (ctor1 != null) {
                ctor1.setAccessible(true);
                return (ResourceLocation) ctor1.newInstance(namespace + ":" + path);
            }
        } catch (Throwable t) {
            MatrixCraftMod.LOGGER.warn("[DynamicLightTextureManager] Reflection ResourceLocation construction failed", t);
        }

        MatrixCraftMod.LOGGER.error("[DynamicLightTextureManager] Could not construct ResourceLocation for trail texture. Dynamic trail texture disabled.");
        return null;
    }

    public static void ensureInit() {
        if (initialized) return;
        initialized = true;

        if (TEX_LOC == null) {
            MatrixCraftMod.LOGGER.warn("[DynamicLightTextureManager] ResourceLocation is null; aborting initialization");
            return;
        }

        nativeImage = new NativeImage(MAX_TRAIL_LIGHTS, 2, false); // 2 rows: position + color
        for (int x = 0; x < MAX_TRAIL_LIGHTS; x++) {
            nativeImage.setPixelRGBA(x, 0, 0); // Row 0: position + intensity
            nativeImage.setPixelRGBA(x, 1, 0); // Row 1: color
        }
        dynamicTexture = new DynamicTexture(nativeImage);
        TextureManager tm = Minecraft.getInstance().getTextureManager();
        tm.register(TEX_LOC, dynamicTexture);
        MatrixCraftMod.LOGGER.info("[DynamicLightTextureManager] Initialized trail-light texture (max=" + MAX_TRAIL_LIGHTS + ")");
    }

    /**
     * Call each client tick (after BulletTrailLighting.tick()) to update texture contents.
     */
    public static void updateTexture() {
        if (!initialized) ensureInit();
        if (!initialized) return;
    
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
    
        Vec3 cam = mc.player == null ? Vec3.ZERO : mc.player.position();
    
        Map<net.minecraft.core.BlockPos, BulletTrailLighting.LightSource> active = BulletTrailLighting.getActiveLights();
    
        List<BulletTrailLighting.LightSource> list = new ArrayList<>();
        if (active != null && !active.isEmpty()) {
            list.addAll(active.values());
        }
    
        // DEBUG: Log first few texture slots to verify clearing
        if (list.isEmpty()) {
            MatrixCraftMod.LOGGER.info("[DynamicLightTextureManager] No lights - checking if texture is clear...");
            for (int i = 0; i < 5; i++) {
                int pixel0 = nativeImage.getPixelRGBA(i, 0);
                int pixel1 = nativeImage.getPixelRGBA(i, 1);
                MatrixCraftMod.LOGGER.info("  Slot " + i + ": row0=" + pixel0 + " row1=" + pixel1);
            }
        }
    
        int written = 0;
        for (int i = 0; i < MAX_TRAIL_LIGHTS; i++) {
            if (i < list.size()) {
                BulletTrailLighting.LightSource light = list.get(i);
                
                // Encode position relative to camera
                double dx = light.position.getX() + 0.5 - cam.x;
                double dy = light.position.getY() + 0.5 - cam.y;
                double dz = light.position.getZ() + 0.5 - cam.z;
                
                // Normalize to -1..1 range, then to 0..1 for texture
                float px = clamp01((float)(dx / POSITION_RANGE) * 0.5f + 0.5f);
                float py = clamp01((float)(dy / POSITION_RANGE) * 0.5f + 0.5f);
                float pz = clamp01((float)(dz / POSITION_RANGE) * 0.5f + 0.5f);
                
                // Calculate intensity from remaining time
                float intensity = clamp01((float)light.ticksRemaining / (float)light.maxTicks);
                
                // Row 0: Position (RGB) + Intensity (A)
                int r0 = (int)(px * 255);
                int g0 = (int)(py * 255);
                int b0 = (int)(pz * 255);
                int a0 = (int)(intensity * 255);
                int rgba0 = (a0 << 24) | (b0 << 16) | (g0 << 8) | r0;
                nativeImage.setPixelRGBA(i, 0, rgba0);
                
                // Row 1: Color (RGB)
                int r1 = (int)(clamp01(light.red) * 255);
                int g1 = (int)(clamp01(light.green) * 255);
                int b1 = (int)(clamp01(light.blue) * 255);
                int rgba1 = (255 << 24) | (b1 << 16) | (g1 << 8) | r1;
                nativeImage.setPixelRGBA(i, 1, rgba1);
                
                written++;
            } else {
                nativeImage.setPixelRGBA(i, 0, 0);
                nativeImage.setPixelRGBA(i, 1, 0);
            }
        }
    
        dynamicTexture.upload();
        RenderSystem.setShaderTexture(TEXTURE_UNIT, TEX_LOC);
    
        MatrixCraftMod.LOGGER.info("[DynamicLightTextureManager] Texture update: " + written + " lights written, " + 
            (active == null ? 0 : active.size()) + " active lights total");
    }

    private static float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    public static void clearTexture() {
        if (!initialized) return;
        for (int x = 0; x < MAX_TRAIL_LIGHTS; x++) {
            nativeImage.setPixelRGBA(x, 0, 0); // Clear position row
            nativeImage.setPixelRGBA(x, 1, 0); // Clear color row
        }
        dynamicTexture.upload();
    }

    public static int getMaxLights() {
        return MAX_TRAIL_LIGHTS;
    }

    public static float getRange() {
        return POSITION_RANGE;
    }
}