package com.raeyncraft.matrixcraft.client;

import com.raeyncraft.matrixcraft.MatrixCraftConfig;
import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.particle.MatrixParticles;
import com.raeyncraft.matrixcraft.client.lighting.DynamicLightManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Bullet Trail Tracker - working copy adjusted to use config-driven lighting parameters.
 *
 * Uses config keys:
 *  - MatrixCraftConfig.TRAIL_LIGHT_SPACING
 *  - MatrixCraftConfig.TRAIL_LIGHT_DURATION_TICKS
 *  - MatrixCraftConfig.TRAIL_CHAIN_ENABLED
 *  - MatrixCraftConfig.TRAIL_CHAIN_COUNT
 *  - MatrixCraftConfig.TRAIL_CHAIN_SPACING
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class BulletTrailTracker {

    private static long lastTrailTime = 0;
    private static final long TRAIL_COOLDOWN_MS = 30;

    private static final Set<Integer> processedBullets = new HashSet<>();
    private static final Map<Integer, Vec3> bulletLastPos = new HashMap<>();

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!MatrixCraftConfig.TRAILS_ENABLED.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return;

        tickCounter++;

        BulletTrailLighting.tick();

        scanBulletEntities(mc);

        if (tickCounter % 40 == 0) {
            cleanupOldEntries(mc);
        }
    }

    private static void scanBulletEntities(Minecraft mc) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!isTaczBullet(entity)) continue;

            int entityId = entity.getId();
            Vec3 currentPos = entity.position();
            Vec3 velocity = entity.getDeltaMovement();

            double distSq = mc.player.distanceToSqr(entity);
            double maxDist = MatrixCraftConfig.MAX_RENDER_DISTANCE.get();
            if (distSq > maxDist * maxDist) continue;

            if (!processedBullets.contains(entityId)) {
                processedBullets.add(entityId);

                if (velocity.lengthSqr() > 1.0) {
                    spawnTrailFromBullet(currentPos, velocity, (ClientLevel) mc.level);
                    MatrixCraftMod.LOGGER.debug("[BulletTrail] Spawned trail for bullet " + entityId);
                }

                bulletLastPos.put(entityId, currentPos);

                // register lights (single or chain depending on config)
                try {
                    int brightness = BulletTrailLighting.getConfiguredLightLevel();
                    float[] color = BulletTrailLighting.getTrailColor();

                    DynamicLightManager.ensureInit();

                    if (MatrixCraftConfig.TRAIL_CHAIN_ENABLED.get()) {
                        int chainCount = MatrixCraftConfig.TRAIL_CHAIN_COUNT.get();
                        double chainSpacing = MatrixCraftConfig.TRAIL_CHAIN_SPACING.get();
                        DynamicLightManager.trackEntityLightChain(entity, chainCount, chainSpacing, brightness, color[0], color[1], color[2]);
                    } else {
                        DynamicLightManager.trackEntityLight(entity, brightness, color[0], color[1], color[2]);
                    }
                    MatrixCraftMod.LOGGER.info("[BulletTrailTracker] Registered entity-backed dynamic light for entity id=" + entityId);
                } catch (Throwable ex) {
                    MatrixCraftMod.LOGGER.info("[BulletTrailTracker] Failed to register entity dynamic light for id=" + entityId + ": " + ex.getMessage());
                }
            }

            // ping so TTL doesn't remove the light
            try { DynamicLightManager.pingEntity(entityId); } catch (Throwable ignored) {}

            Vec3 lastPos = bulletLastPos.get(entityId);
            if (lastPos != null && currentPos.distanceToSqr(lastPos) > 0.01) {
                spawnTrailSegment(lastPos, currentPos, (ClientLevel) mc.level);
            }
            bulletLastPos.put(entityId, currentPos);
        }
    }

    private static void spawnTrailFromPlayer(Player player, ClientLevel level) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookDir = player.getLookAngle();
        Vec3 muzzle = eyePos.add(lookDir.scale(0.5));

        double trailLength = 100.0;
        int particleCount = 150;

        boolean addLights = isGlowEnabled();

        int spacing = MatrixCraftConfig.TRAIL_LIGHT_SPACING.get();

        for (int i = 0; i < particleCount; i++) {
            double t = (double) i / particleCount;
            Vec3 pos = muzzle.add(lookDir.scale(t * trailLength));

            double ox = (Math.random() - 0.5) * 0.04;
            double oy = (Math.random() - 0.5) * 0.04;
            double oz = (Math.random() - 0.5) * 0.04;

            level.addAlwaysVisibleParticle(
                    MatrixParticles.BULLET_TRAIL.get(),
                    true,
                    pos.x + ox, pos.y + oy, pos.z + oz,
                    0, 0, 0
            );

            if (addLights && i % spacing == 0) {
                BulletTrailLighting.addLightSource(pos.x, pos.y, pos.z);
            }
        }
    }

    private static void spawnTrailFromBullet(Vec3 bulletPos, Vec3 velocity, ClientLevel level) {
        Vec3 direction = velocity.normalize();

        double trailLength = 80.0;
        int particleCount = 120;

        boolean addLights = isGlowEnabled();

        int spacing = MatrixCraftConfig.TRAIL_LIGHT_SPACING.get();

        for (int i = 0; i < particleCount; i++) {
            double t = (double) i / particleCount;
            Vec3 pos = bulletPos.subtract(direction.scale(t * trailLength));

            double ox = (Math.random() - 0.5) * 0.04;
            double oy = (Math.random() - 0.5) * 0.04;
            double oz = (Math.random() - 0.5) * 0.04;

            level.addAlwaysVisibleParticle(
                    MatrixParticles.BULLET_TRAIL.get(),
                    true,
                    pos.x + ox, pos.y + oy, pos.z + oz,
                    0, 0, 0
            );

            if (addLights && i % spacing == 0) {
                BulletTrailLighting.addLightSource(pos.x, pos.y, pos.z);
            }
        }
    }

    private static void spawnTrailSegment(Vec3 from, Vec3 to, ClientLevel level) {
        double distance = from.distanceTo(to);
        if (distance < 0.1) return;

        int count = Math.max(3, (int)(distance * 3));
        count = Math.min(count, 20);

        boolean addLights = isGlowEnabled();
        int spacing = MatrixCraftConfig.TRAIL_LIGHT_SPACING.get();

        for (int i = 0; i < count; i++) {
            double t = (double) i / count;
            Vec3 pos = from.lerp(to, t);

            double ox = (Math.random() - 0.5) * 0.03;
            double oy = (Math.random() - 0.5) * 0.03;
            double oz = (Math.random() - 0.5) * 0.03;

            level.addAlwaysVisibleParticle(
                    MatrixParticles.BULLET_TRAIL.get(),
                    true,
                    pos.x + ox, pos.y + oy, pos.z + oz,
                    0, 0, 0
            );

            if (addLights && i % spacing == 0) {
                BulletTrailLighting.addLightSource(pos.x, pos.y, pos.z);
            }
        }
    }

    private static void cleanupOldEntries(Minecraft mc) {
        processedBullets.removeIf(id -> {
            Entity e = mc.level.getEntity(id);
            boolean removed = (e == null || e.isRemoved());
            if (removed) {
                try {
                    DynamicLightManager.untrackEntityLightById(id);
                } catch (Throwable ignored) {}
            }
            return removed;
        });

        bulletLastPos.entrySet().removeIf(entry -> {
            int id = entry.getKey();
            Entity e = mc.level.getEntity(id);
            boolean removed = (e == null || e.isRemoved());
            if (removed) {
                try {
                    DynamicLightManager.untrackEntityLightById(id);
                } catch (Throwable ignored) {}
            }
            return removed;
        });
    }

    private static boolean isTaczBullet(Entity entity) {
        return entity.getClass().getName().equals("com.tacz.guns.entity.EntityKineticBullet")
                || String.valueOf(entity.getType()).toLowerCase().contains("tacz");
    }

    private static boolean isGlowEnabled() {
        try {
            return MatrixCraftConfig.TRAIL_GLOW.get();
        } catch (Exception e) {
            return true;
        }
    }
}