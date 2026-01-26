package com.raeyncraft.matrixcraft.client.lighting;

import com.raeyncraft.matrixcraft.client.BulletTrailLighting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper that collects nearest active bullet-trail lights and prepares a compact
 * list for shader upload. This class does not directly upload GL uniforms (that
 * depends on how your shader pipeline is wired), but it shows the data format
 * and provides a single point to call from a render hook.
 */
public class ShaderLightUploader {

    public static class ShaderLight {
        public final double x, y, z;
        public final float r, g, b;
        public final float intensity; // 0..1
        public ShaderLight(double x, double y, double z, float r, float g, float b, float intensity) {
            this.x = x; this.y = y; this.z = z; this.r = r; this.g = g; this.b = b; this.intensity = intensity;
        }
    }

    /**
     * Retrieve up to maxLights nearest active bullet-trail lights to the player.
     * Call from the render-thread and then upload to shader uniforms (implementation-specific).
     */
    public static List<ShaderLight> getNearestLights(int maxLights, double maxDistance) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return Collections.emptyList();

        final double px = mc.player.getX();
        final double py = mc.player.getY();
        final double pz = mc.player.getZ();

        // Convert active lights map into a list and sort by distance to player
        List<ShaderLight> lights = BulletTrailLighting.getActiveLights().values().stream()
            .map(ls -> {
                BlockPos pos = ls.position;
                double lx = pos.getX() + 0.5;
                double ly = pos.getY() + 0.5;
                double lz = pos.getZ() + 0.5;
                float intensity = (float) ls.getCurrentBrightness() / 15f;
                return new AbstractMap.SimpleEntry<>( (lx - px)*(lx - px) + (ly - py)*(ly - py) + (lz - pz)*(lz - pz),
                    new ShaderLight(lx, ly, lz, ls.red, ls.green, ls.blue, intensity));
            })
            .filter(entry -> entry.getKey() <= maxDistance * maxDistance)
            .sorted(Comparator.comparingDouble(Map.Entry::getKey))
            .limit(maxLights)
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());

        return lights;
    }

    // Example: one would call getNearestLights(...) each render pass then upload into the shader.
}