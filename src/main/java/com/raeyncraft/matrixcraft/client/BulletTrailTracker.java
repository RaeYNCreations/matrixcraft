package com.raeyncraft.matrixcraft.client;

import com.raeyncraft.matrixcraft.MatrixCraftConfig;
import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.particle.MatrixParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.*;

@EventBusSubscriber(value = Dist.CLIENT)
public class BulletTrailTracker {
    private static final Map<Integer, BulletTrailData> trackedBullets = new HashMap<>();
    private static int processedThisTick = 0;
    
    private static class BulletTrailData {
        Vec3 lastPos;
        Vec3 lastVelocity;
        Vec3 initialSpawnPos;  // Where the bullet first appeared
        int ticksSinceSpawn;
        boolean initialTrailSpawned;
        
        BulletTrailData(Vec3 pos, Vec3 velocity) {
            this.lastPos = pos;
            this.lastVelocity = velocity;
            this.initialSpawnPos = pos;
            this.ticksSinceSpawn = 0;
            this.initialTrailSpawned = false;
        }
    }
    
    /**
     * CRITICAL: Catch bullets the MOMENT they spawn using EntityJoinLevelEvent
     * This fires before the entity even renders for the first time!
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        
        if (!MatrixCraftConfig.TRAILS_ENABLED.get()) {
            return;
        }
        
        Entity entity = event.getEntity();
        if (!isTaczBullet(entity)) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        
        // Distance culling
        double distance = mc.player.distanceToSqr(entity);
        double maxDist = MatrixCraftConfig.MAX_RENDER_DISTANCE.get();
        if (distance > maxDist * maxDist) {
            return;
        }
        
        Vec3 bulletPos = entity.position();
        Vec3 bulletVelocity = entity.getDeltaMovement();
        
        // If velocity is zero (common on first tick), estimate from position relative to player
        if (bulletVelocity.lengthSqr() < 0.001) {
            // Try to find the shooter (usually the nearest player)
            Player shooter = findNearestPlayer(entity);
            if (shooter != null) {
                Vec3 shooterEyePos = shooter.getEyePosition();
                bulletVelocity = bulletPos.subtract(shooterEyePos).normalize().scale(3.0);
            }
        }
        
        int entityId = entity.getId();
        
        // Don't re-add if already tracked
        if (trackedBullets.containsKey(entityId)) {
            return;
        }
        
        BulletTrailData data = new BulletTrailData(bulletPos, bulletVelocity);
        trackedBullets.put(entityId, data);
        
        // Immediately spawn the backward trail from spawn point to shooter
        if (bulletVelocity.lengthSqr() > 0.001) {
            spawnInitialBackwardTrail(entity, bulletPos, bulletVelocity);
            data.initialTrailSpawned = true;
        }
        
        // Also spawn particles at the current position
        spawnParticlesAtPosition(entity, bulletPos);
    }
    
    /**
     * Find the player who likely shot this bullet (nearest player looking toward bullet)
     */
    private static Player findNearestPlayer(Entity bullet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;
        
        for (Player player : mc.level.players()) {
            double dist = player.distanceToSqr(bullet);
            if (dist < nearestDist && dist < 400) { // Within 20 blocks
                nearest = player;
                nearestDist = dist;
            }
        }
        
        return nearest;
    }
    
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        
        if (!MatrixCraftConfig.TRAILS_ENABLED.get()) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        
        processedThisTick = 0;
        
        // Process all tracked bullets
        Iterator<Map.Entry<Integer, BulletTrailData>> iterator = trackedBullets.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<Integer, BulletTrailData> entry = iterator.next();
            int entityId = entry.getKey();
            BulletTrailData data = entry.getValue();
            
            Entity entity = mc.level.getEntity(entityId);
            
            // Remove if entity no longer exists
            if (entity == null || entity.isRemoved()) {
                iterator.remove();
                continue;
            }
            
            if (processedThisTick >= MatrixCraftConfig.MAX_TRAILS_PER_TICK.get()) {
                continue;
            }
            
            // Distance culling
            double distance = mc.player.distanceToSqr(entity);
            double maxDist = MatrixCraftConfig.MAX_RENDER_DISTANCE.get();
            if (distance > maxDist * maxDist) {
                iterator.remove();
                continue;
            }
            
            Vec3 currentPos = entity.position();
            Vec3 currentVelocity = entity.getDeltaMovement();
            
            // If we didn't spawn initial trail yet (velocity was zero on spawn), try now
            if (!data.initialTrailSpawned && currentVelocity.lengthSqr() > 0.001) {
                spawnInitialBackwardTrail(entity, data.initialSpawnPos, currentVelocity);
                data.initialTrailSpawned = true;
            }
            
            // Spawn trail between last and current position
            if (!currentPos.equals(data.lastPos)) {
                spawnTrailBetweenPositions(entity, data.lastPos, currentPos);
            }
            
            // Spawn particles at current position
            spawnParticlesAtPosition(entity, currentPos);
            
            data.lastPos = currentPos;
            data.lastVelocity = currentVelocity;
            data.ticksSinceSpawn++;
            processedThisTick++;
            
            // Remove after trail length exceeded
            if (data.ticksSinceSpawn > MatrixCraftConfig.TRAIL_LENGTH.get()) {
                iterator.remove();
            }
        }
        
        // Also scan for any bullets we might have missed (backup)
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!isTaczBullet(entity)) {
                continue;
            }
            
            int entityId = entity.getId();
            if (trackedBullets.containsKey(entityId)) {
                continue; // Already tracking
            }
            
            if (processedThisTick >= MatrixCraftConfig.MAX_TRAILS_PER_TICK.get()) {
                break;
            }
            
            // Distance culling
            double distance = mc.player.distanceToSqr(entity);
            double maxDist = MatrixCraftConfig.MAX_RENDER_DISTANCE.get();
            if (distance > maxDist * maxDist) {
                continue;
            }
            
            Vec3 currentPos = entity.position();
            Vec3 currentVelocity = entity.getDeltaMovement();
            
            BulletTrailData data = new BulletTrailData(currentPos, currentVelocity);
            trackedBullets.put(entityId, data);
            
            // Spawn backward trail if we have velocity
            if (currentVelocity.lengthSqr() > 0.001) {
                spawnInitialBackwardTrail(entity, currentPos, currentVelocity);
                data.initialTrailSpawned = true;
            }
            
            spawnParticlesAtPosition(entity, currentPos);
            processedThisTick++;
        }
    }
    
    /**
     * Spawn the initial trail BACKWARD from where the bullet is to where it came from (the gun)
     */
    private static void spawnInitialBackwardTrail(Entity bullet, Vec3 currentPos, Vec3 velocity) {
        if (!(bullet.level() instanceof ClientLevel clientLevel)) {
            return;
        }
        
        Vec3 direction = velocity.normalize();
        
        // Calculate how far back to draw the trail
        // Bullets travel fast, so we trace back more steps
        int backwardSteps = 30;  // Increased from 20
        double stepSize = 0.4;   // Slightly smaller steps for smoother trail
        
        // Also try to find the shooter's position for more accurate origin
        Player shooter = findNearestPlayer(bullet);
        Vec3 trailEndPoint;
        
        if (shooter != null) {
            // Draw all the way back to the shooter's eye position
            Vec3 shooterEyePos = shooter.getEyePosition();
            double distToShooter = currentPos.distanceTo(shooterEyePos);
            backwardSteps = Math.max(30, (int)(distToShooter / stepSize) + 5);
            trailEndPoint = shooterEyePos;
        } else {
            // Fallback: just trace backward along velocity
            trailEndPoint = currentPos.subtract(direction.scale(backwardSteps * stepSize));
        }
        
        // Spawn particles from current position back toward shooter
        for (int i = 0; i < backwardSteps; i++) {
            double t = i / (double) backwardSteps;
            Vec3 particlePos;
            
            if (shooter != null) {
                // Interpolate between bullet position and shooter
                particlePos = currentPos.lerp(trailEndPoint, t);
            } else {
                // Just go backward along velocity
                particlePos = currentPos.subtract(direction.scale(i * stepSize));
            }
            
            double offsetX = (Math.random() - 0.5) * 0.05;
            double offsetY = (Math.random() - 0.5) * 0.05;
            double offsetZ = (Math.random() - 0.5) * 0.05;
            
            clientLevel.addAlwaysVisibleParticle(
                MatrixParticles.BULLET_TRAIL.get(),
                true,
                particlePos.x + offsetX,
                particlePos.y + offsetY,
                particlePos.z + offsetZ,
                0, 0, 0
            );
        }
    }
    
    private static void spawnParticlesAtPosition(Entity bullet, Vec3 pos) {
        if (!(bullet.level() instanceof ClientLevel clientLevel)) {
            return;
        }
        
        int density = MatrixCraftConfig.TRAIL_DENSITY.get();
        
        for (int i = 0; i < density; i++) {
            double offsetX = (Math.random() - 0.5) * 0.05;
            double offsetY = (Math.random() - 0.5) * 0.05;
            double offsetZ = (Math.random() - 0.5) * 0.05;
            
            clientLevel.addAlwaysVisibleParticle(
                MatrixParticles.BULLET_TRAIL.get(),
                true,
                pos.x + offsetX,
                pos.y + offsetY,
                pos.z + offsetZ,
                0, 0, 0
            );
        }
    }
    
    private static void spawnTrailBetweenPositions(Entity bullet, Vec3 lastPos, Vec3 currentPos) {
        if (!(bullet.level() instanceof ClientLevel clientLevel)) {
            return;
        }
        
        int density = MatrixCraftConfig.TRAIL_DENSITY.get();
        Vec3 motion = currentPos.subtract(lastPos);
        double distance = motion.length();
        
        if (distance < 0.01) {
            return;
        }
        
        int particlesToSpawn = Math.max(density, (int)(density * (distance / 0.3)));
        particlesToSpawn = Math.min(particlesToSpawn, density * 10);
        
        for (int i = 0; i < particlesToSpawn; i++) {
            double t = i / (double) particlesToSpawn;
            Vec3 particlePos = lastPos.add(motion.scale(t));
            
            double offsetX = (Math.random() - 0.5) * 0.03;
            double offsetY = (Math.random() - 0.5) * 0.03;
            double offsetZ = (Math.random() - 0.5) * 0.03;
            
            clientLevel.addAlwaysVisibleParticle(
                MatrixParticles.BULLET_TRAIL.get(),
                true,
                particlePos.x + offsetX,
                particlePos.y + offsetY,
                particlePos.z + offsetZ,
                0, 0, 0
            );
        }
    }
    
    private static boolean isTaczBullet(Entity entity) {
        String className = entity.getClass().getName();
        return className.equals("com.tacz.guns.entity.EntityKineticBullet");
    }
}
