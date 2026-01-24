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
        Vec3 lastVelocity;
        int ticksSinceSpawn;
        
        BulletTrailData(Vec3 pos, Vec3 velocity) {
            this.lastPos = pos;
            this.lastVelocity = velocity;
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
            Vec3 currentVelocity = entity.getDeltaMovement();
            int entityId = entity.getId();
            
            BulletTrailData data = trackedBullets.get(entityId);
            if (data == null) {
                // New bullet detected - spawn trail BACKWARD toward shooter
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
                
                data.lastPos = currentPos;
                data.lastVelocity = currentVelocity;
            }
            
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
    
    private static boolean isTaczBullet(Entity entity) {
        String className = entity.getClass().getName();
        return className.equals("com.tacz.guns.entity.EntityKineticBullet");
    }
}
