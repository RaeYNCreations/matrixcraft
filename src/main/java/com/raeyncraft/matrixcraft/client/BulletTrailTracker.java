package com.raeyncraft.matrixcraft.client;

import com.raeyncraft.matrixcraft.MatrixCraftConfig;
import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.particle.MatrixParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.*;

@EventBusSubscriber(value = Dist.CLIENT)
public class BulletTrailTracker {
    private static final Map<Integer, BulletTrailData> trackedBullets = new HashMap<>();
    private static int processedThisTick = 0;
    
    private static class BulletTrailData {
        Vec3 lastPos;
        Vec3 lastVelocity;
        int ticksSinceSpawn;
        boolean wasAlive;
        
        BulletTrailData(Vec3 pos, Vec3 velocity) {
            this.lastPos = pos;
            this.lastVelocity = velocity;
            this.ticksSinceSpawn = 0;
            this.wasAlive = true;
        }
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
        
        // Track which bullets we've seen this tick
        Set<Integer> bulletsSeenThisTick = new HashSet<>();
        
        // Scan all entities for bullets
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!isTaczBullet(entity)) {
                continue;
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
            int entityId = entity.getId();
            
            bulletsSeenThisTick.add(entityId);
            
            BulletTrailData data = trackedBullets.get(entityId);
            if (data == null) {
                // New bullet detected - spawn trail BACKWARD toward shooter!
                data = new BulletTrailData(currentPos, currentVelocity);
                trackedBullets.put(entityId, data);
                
                if (currentVelocity.length() > 0.01) {
                    spawnInitialBackwardTrail(entity, currentPos, currentVelocity);
                }
                
                spawnParticlesAtPosition(entity, currentPos);
            } else {
                // Spawn trail between last and current position
                if (!currentPos.equals(data.lastPos)) {
                    spawnTrailBetweenPositions(entity, data.lastPos, currentPos);
                }
                
                spawnParticlesAtPosition(entity, currentPos);
                
                // Update stored data
                data.lastPos = currentPos;
                data.lastVelocity = currentVelocity;
            }
            
            data.ticksSinceSpawn++;
            processedThisTick++;
            
            if (data.ticksSinceSpawn > MatrixCraftConfig.TRAIL_LENGTH.get()) {
                trackedBullets.remove(entityId);
            }
        }
        
        // Check for bullets that disappeared (hit something!)
        Iterator<Map.Entry<Integer, BulletTrailData>> iterator = trackedBullets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, BulletTrailData> entry = iterator.next();
            int bulletId = entry.getKey();
            BulletTrailData data = entry.getValue();
            
            // If bullet was alive last tick but not seen this tick = impact!
            if (data.wasAlive && !bulletsSeenThisTick.contains(bulletId)) {
                Vec3 impactPos = data.lastPos;
                
                // Check if there's a solid block at or near the impact position
                BlockPos blockPos = BlockPos.containing(impactPos);
                BlockState blockState = mc.level.getBlockState(blockPos);
                
                // Only spawn impact if bullet hit a solid block (not air/water)
                boolean hitSolidBlock = !blockState.isAir() && 
                                       blockState.isSolid() && 
                                       !blockState.liquid();
                
                if (hitSolidBlock) {
                    // Calculate impact normal (direction away from block)
                    Vec3 impactNormal = data.lastVelocity.normalize().reverse();
                    
                    spawnImpactEffect(impactPos, impactNormal, mc.level);
                    
                    MatrixCraftMod.LOGGER.info("IMPACT on " + blockState.getBlock().getName().getString() + " at " + impactPos);
                } else {
                    MatrixCraftMod.LOGGER.info("Bullet despawned (no solid block): " + blockState.getBlock().getName().getString());
                }
                
                iterator.remove();
            } else if (!bulletsSeenThisTick.contains(bulletId)) {
                iterator.remove();
            } else {
                data.wasAlive = true;
            }
        }
    }
    
    private static void spawnInitialBackwardTrail(Entity bullet, Vec3 currentPos, Vec3 velocity) {
        if (!(bullet.level() instanceof ClientLevel clientLevel)) {
            return;
        }
        
        Vec3 direction = velocity.normalize();
        int backwardSteps = 20;
        double stepSize = 0.5;
        
        for (int i = 0; i < backwardSteps; i++) {
            Vec3 particlePos = currentPos.subtract(direction.scale(i * stepSize));
            
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
    
    private static void spawnImpactEffect(Vec3 pos, Vec3 normal, ClientLevel level) {
        if (!MatrixCraftConfig.IMPACTS_ENABLED.get()) {
            MatrixCraftMod.LOGGER.warn("Impact particles disabled in config!");
            return;
        }
        
        MatrixCraftMod.LOGGER.info("Spawning " + MatrixCraftConfig.IMPACT_PARTICLE_COUNT.get() + " impact particles at " + pos);
        
        int count = MatrixCraftConfig.IMPACT_PARTICLE_COUNT.get();
        double speed = MatrixCraftConfig.IMPACT_PARTICLE_SPEED.get();
        double radius = MatrixCraftConfig.IMPACT_RADIUS.get();
        
        for (int i = 0; i < count; i++) {
            // Random direction for spark burst
            double angle1 = Math.random() * Math.PI * 2;
            double angle2 = Math.random() * Math.PI * 0.5;
            
            double vx = Math.sin(angle2) * Math.cos(angle1) * speed;
            double vy = Math.sin(angle2) * Math.sin(angle1) * speed;
            double vz = Math.cos(angle2) * speed;
            
            // Add some velocity in the impact normal direction
            vx += normal.x * speed * 0.5;
            vy += normal.y * speed * 0.5;
            vz += normal.z * speed * 0.5;
            
            // Spread particles around impact point
            double offsetX = (Math.random() - 0.5) * radius;
            double offsetY = (Math.random() - 0.5) * radius;
            double offsetZ = (Math.random() - 0.5) * radius;
            
            try {
                level.addAlwaysVisibleParticle(
                    MatrixParticles.BULLET_IMPACT.get(),
                    true,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    vx, vy, vz
                );
                
                if (i == 0) {
                    MatrixCraftMod.LOGGER.info("First impact particle spawned successfully!");
                }
            } catch (Exception e) {
                MatrixCraftMod.LOGGER.error("Failed to spawn impact particle: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        MatrixCraftMod.LOGGER.info("Finished spawning impact particles");
    }
    
    private static boolean isTaczBullet(Entity entity) {
        String className = entity.getClass().getName();
        return className.equals("com.tacz.guns.entity.EntityKineticBullet");
    }
}
