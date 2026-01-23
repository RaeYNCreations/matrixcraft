package com.raeyncraft.matrixcraft.client;

import com.raeyncraft.matrixcraft.MatrixCraftConfig;
import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.particle.MatrixParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
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
        int ticksSinceSpawn;
        
        BulletTrailData(Vec3 pos) {
            this.lastPos = pos;
            this.ticksSinceSpawn = 0;
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
            int entityId = entity.getId();
            
            BulletTrailData data = trackedBullets.get(entityId);
            if (data == null) {
                // New bullet detected - spawn trail BACKWARD toward shooter!
                data = new BulletTrailData(currentPos);
                trackedBullets.put(entityId, data);
                
                // Get bullet's velocity to trace backward
                Vec3 velocity = entity.getDeltaMovement();
                if (velocity.length() > 0.01) {
                    // Spawn trail backward from current position toward where it came from
                    spawnInitialBackwardTrail(entity, currentPos, velocity);
                }
                
                // Also spawn at current position
                spawnParticlesAtPosition(entity, currentPos);
            } else {
                // Spawn trail between last and current position
                if (!currentPos.equals(data.lastPos)) {
                    spawnTrailBetweenPositions(entity, data.lastPos, currentPos);
                }
                
                // Also spawn at current position
                spawnParticlesAtPosition(entity, currentPos);
            }
            
            data.lastPos = currentPos;
            data.ticksSinceSpawn++;
            processedThisTick++;
            
            if (data.ticksSinceSpawn > MatrixCraftConfig.TRAIL_LENGTH.get() || entity.isRemoved()) {
                trackedBullets.remove(entityId);
            }
        }
        
        // Clean up removed bullets
        trackedBullets.entrySet().removeIf(entry -> {
            Entity entity = mc.level.getEntity(entry.getKey());
            return entity == null || entity.isRemoved();
        });
    }
    
    // Spawn initial trail BACKWARD from where we first detect the bullet
    private static void spawnInitialBackwardTrail(Entity bullet, Vec3 currentPos, Vec3 velocity) {
        if (!(bullet.level() instanceof ClientLevel clientLevel)) {
            return;
        }
        
        // Normalize velocity direction
        Vec3 direction = velocity.normalize();
        
        // Spawn particles backward along the velocity vector
        int backwardSteps = 20; // Spawn ~20 particles going backward
        double stepSize = 0.5; // 0.5 blocks per step
        
        for (int i = 0; i < backwardSteps; i++) {
            // Calculate position going backward
            Vec3 particlePos = currentPos.subtract(direction.scale(i * stepSize));
            
            // Small random offset
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
        
        MatrixCraftMod.LOGGER.info("Spawned backward trail from " + currentPos + " toward shooter");
    }
    
    // Spawn particles directly at the bullet's current position
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
    
    // Fill in the trail between two positions
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
    
    public static void spawnImpactEffect(Vec3 pos, Vec3 normal, Entity entity) {
        if (!MatrixCraftConfig.IMPACTS_ENABLED.get()) {
            return;
        }
        
        if (!(entity.level() instanceof ClientLevel clientLevel)) {
            return;
        }
        
        int count = MatrixCraftConfig.IMPACT_PARTICLE_COUNT.get();
        double speed = MatrixCraftConfig.IMPACT_PARTICLE_SPEED.get();
        double radius = MatrixCraftConfig.IMPACT_RADIUS.get();
        
        for (int i = 0; i < count; i++) {
            double angle1 = Math.random() * Math.PI * 2;
            double angle2 = Math.random() * Math.PI * 0.5;
            
            double vx = Math.sin(angle2) * Math.cos(angle1) * speed;
            double vy = Math.sin(angle2) * Math.sin(angle1) * speed;
            double vz = Math.cos(angle2) * speed;
            
            vx += normal.x * speed * 0.5;
            vy += normal.y * speed * 0.5;
            vz += normal.z * speed * 0.5;
            
            double offsetX = (Math.random() - 0.5) * radius;
            double offsetY = (Math.random() - 0.5) * radius;
            double offsetZ = (Math.random() - 0.5) * radius;
            
            clientLevel.addAlwaysVisibleParticle(
                MatrixParticles.BULLET_IMPACT.get(),
                true,
                pos.x + offsetX,
                pos.y + offsetY,
                pos.z + offsetZ,
                vx, vy, vz
            );
        }
    }
    
    private static boolean isTaczBullet(Entity entity) {
        String className = entity.getClass().getName();
        return className.equals("com.tacz.guns.entity.EntityKineticBullet");
    }
}
