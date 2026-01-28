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
        double camX = cam.x;
        double camY = cam.y;
        double camZ = cam.z;

        Map<net.minecraft.core.BlockPos, BulletTrailLighting.LightSource> active = BulletTrailLighting.getActiveLights();

        List<BulletTrailLighting.LightSource> list = new ArrayList<>();
        if (active != null && !active.isEmpty()) {
            list.addAll(active.values());
        }

        int written = 0;
        for (int i = 0; i < MAX_TRAIL_LIGHTS; i++) {
            if (i < list.size()) {
                BulletTrailLighting.LightSource ls = list.get(i);
                double lx = ls.position.getX() + 0.5;
                double ly = ls.position.getY() + 0.5;
                double lz = ls.position.getZ() + 0.5;

                double dx = lx - camX;
                double dy = ly - camY;
                double dz = lz - camZ;

                float range = POSITION_RANGE;
                if (Math.abs(dx) > range || Math.abs(dy) > range || Math.abs(dz) > range) {
                    nativeImage.setPixelRGBA(i, 0, 0); // Clear position row
                    nativeImage.setPixelRGBA(i, 1, 0); // Clear color row
                    continue;
                }

                float nx = (float) ((dx / range + 1.0) * 0.5);
                float ny = (float) ((dy / range + 1.0) * 0.5);
                float nz = (float) ((dz / range + 1.0) * 0.5);

                nx = clamp01(nx);
                ny = clamp01(ny);
                nz = clamp01(nz);

                float intensity = Math.max(0f, Math.min(1f, (float) ls.getCurrentBrightness() / 15.0f));

                int R = (int) (nx * 255.0f) & 0xFF;
                int G = (int) (ny * 255.0f) & 0xFF;
                int B = (int) (nz * 255.0f) & 0xFF;
                int A = (int) (intensity * 255.0f) & 0xFF;

                int pixel = (A << 24) | (R << 16) | (G << 8) | B;
                nativeImage.setPixelRGBA(i, 0, pixel);
                
                // Row 1: encode RGB color from light source
                int colorR = (int) (ls.red * 255.0f) & 0xFF;
                int colorG = (int) (ls.green * 255.0f) & 0xFF;
                int colorB = (int) (ls.blue * 255.0f) & 0xFF;
                int colorPixel = (255 << 24) | (colorR << 16) | (colorG << 8) | colorB;
                nativeImage.setPixelRGBA(i, 1, colorPixel);
                
                written++;
            } else {
                nativeImage.setPixelRGBA(i, 0, 0); // Clear position row
                nativeImage.setPixelRGBA(i, 1, 0); // Clear color row
            }
        }

        dynamicTexture.upload();
        RenderSystem.setShaderTexture(TEXTURE_UNIT, TEX_LOC);

        if (written > 0) {
            MatrixCraftMod.LOGGER.debug("[DynamicLightTextureManager] wrote " + written + " trail light texels");
        }
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