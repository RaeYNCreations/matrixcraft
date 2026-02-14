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
 * RGB Dynamic Lighting System:
 * - Bullets emit colored dynamic lights matching trail color
 * - Uses TRAIL_COLOR_R/G/B config (0-255 each component)
 * - Supports both single light and chained light modes
 * - Color automatically syncs with bullet trail particles
 * 
 * Configuration Options:
 *  - MatrixCraftConfig.TRAIL_LIGHT_SPACING: Distance between lights
 *  - MatrixCraftConfig.TRAIL_LIGHT_DURATION_TICKS: How long lights last
 *  - MatrixCraftConfig.TRAIL_CHAIN_ENABLED: Enable light chains
 *  - MatrixCraftConfig.TRAIL_CHAIN_COUNT: Number of lights in chain
 *  - MatrixCraftConfig.TRAIL_CHAIN_SPACING: Spacing between chain lights
 *  - MatrixCraftConfig.TRAIL_COLOR_R/G/B: RGB color components (0-255)
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class BulletTrailTracker {
    
    // Trail rendering constants
    private static final double PLAYER_TRAIL_LENGTH = 100.0;
    private static final int PLAYER_TRAIL_PARTICLE_COUNT = 150;
    private static final double BULLET_TRAIL_LENGTH = 80.0;
    private static final int BULLET_TRAIL_PARTICLE_COUNT = 120;
    private static final double TRAIL_PARTICLE_OFFSET_LARGE = 0.04;
    private static final double TRAIL_PARTICLE_OFFSET_SMALL = 0.03;
    private static final int TRAIL_SEGMENT_MULTIPLIER = 3;
    private static final int TRAIL_SEGMENT_MAX_COUNT = 20;
    private static final int TRAIL_SEGMENT_MIN_COUNT = 3;
    private static final double TRAIL_SEGMENT_MIN_DISTANCE = 0.1;

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
        
        // Update hit entity lighting
        HitEntityLightingHandler.tick(mc.level);

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

            // Use atomic add operation - only executes if bullet wasn't already processed
            if (processedBullets.add(entityId)) {

                long now = System.currentTimeMillis();
                if (now - lastTrailTime > 100 && velocity.lengthSqr() > 1.0) {
                    spawnTrailFromBullet(currentPos, velocity, (ClientLevel) mc.level);
                }

                bulletLastPos.put(entityId, currentPos);

                // register lights (single or chain depending on config)
                // Uses RGB color from TRAIL_COLOR_R/G/B configuration
                try {
                    int brightness = BulletTrailLighting.getConfiguredLightLevel();
                    float[] color = BulletTrailLighting.getTrailColor(); // RGB normalized 0-1

                    DynamicLightManager.ensureInit();

                    if (MatrixCraftConfig.TRAIL_CHAIN_ENABLED.get()) {
                        int chainCount = MatrixCraftConfig.TRAIL_CHAIN_COUNT.get();
                        double chainSpacing = MatrixCraftConfig.TRAIL_CHAIN_SPACING.get();
                        // Create chain of RGB lights trailing the bullet
                        DynamicLightManager.trackEntityLightChain(entity, chainCount, chainSpacing, brightness, color[0], color[1], color[2]);
                    } else {
                        // Create single RGB light at bullet position
                        DynamicLightManager.trackEntityLight(entity, brightness, color[0], color[1], color[2]);
                    }
                } catch (Throwable ex) {
                    MatrixCraftMod.LOGGER.warn("[BulletTrailTracker] Failed to register entity dynamic light for id=" + entityId + ": " + ex.getMessage());
                }
            }

            // ping so TTL doesn't remove the light
            try { 
                DynamicLightManager.pingEntity(entityId); 
            } catch (Throwable e) {
                MatrixCraftMod.LOGGER.debug("[BulletTrailTracker] Ping entity failed for id=" + entityId + ": " + e.getMessage());
            }

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

        boolean addLights = isGlowEnabled();

        int spacing = MatrixCraftConfig.TRAIL_LIGHT_SPACING.get();

        for (int i = 0; i < PLAYER_TRAIL_PARTICLE_COUNT; i++) {
            double t = (double) i / PLAYER_TRAIL_PARTICLE_COUNT;
            Vec3 pos = muzzle.add(lookDir.scale(t * PLAYER_TRAIL_LENGTH));

            double ox = (Math.random() - 0.5) * TRAIL_PARTICLE_OFFSET_LARGE;
            double oy = (Math.random() - 0.5) * TRAIL_PARTICLE_OFFSET_LARGE;
            double oz = (Math.random() - 0.5) * TRAIL_PARTICLE_OFFSET_LARGE;

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

        boolean addLights = isGlowEnabled();

        int spacing = MatrixCraftConfig.TRAIL_LIGHT_SPACING.get();

        for (int i = 0; i < BULLET_TRAIL_PARTICLE_COUNT; i++) {
            double t = (double) i / BULLET_TRAIL_PARTICLE_COUNT;
            Vec3 pos = bulletPos.subtract(direction.scale(t * BULLET_TRAIL_LENGTH));

            double ox = (Math.random() - 0.5) * TRAIL_PARTICLE_OFFSET_LARGE;
            double oy = (Math.random() - 0.5) * TRAIL_PARTICLE_OFFSET_LARGE;
            double oz = (Math.random() - 0.5) * TRAIL_PARTICLE_OFFSET_LARGE;

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
        if (distance < TRAIL_SEGMENT_MIN_DISTANCE) return;

        int count = Math.max(TRAIL_SEGMENT_MIN_COUNT, (int)(distance * TRAIL_SEGMENT_MULTIPLIER));
        count = Math.min(count, TRAIL_SEGMENT_MAX_COUNT);

        boolean addLights = isGlowEnabled();
        int spacing = MatrixCraftConfig.TRAIL_LIGHT_SPACING.get();

        for (int i = 0; i < count; i++) {
            double t = (double) i / count;
            Vec3 pos = from.lerp(to, t);

            double ox = (Math.random() - 0.5) * TRAIL_PARTICLE_OFFSET_SMALL;
            double oy = (Math.random() - 0.5) * TRAIL_PARTICLE_OFFSET_SMALL;
            double oz = (Math.random() - 0.5) * TRAIL_PARTICLE_OFFSET_SMALL;

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
                } catch (Throwable ex) {
                    MatrixCraftMod.LOGGER.debug("[BulletTrailTracker] Untrack entity failed in processedBullets cleanup for id=" + id + ": " + ex.getMessage());
                }
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
                } catch (Throwable ex) {
                    MatrixCraftMod.LOGGER.debug("[BulletTrailTracker] Untrack entity failed in bulletLastPos cleanup for id=" + id + ": " + ex.getMessage());
                }
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