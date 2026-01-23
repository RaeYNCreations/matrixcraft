package com.raeyncraft.matrixcraft.client;

import com.raeyncraft.matrixcraft.MatrixCraftConfig;
import com.raeyncraft.matrixcraft.particle.MatrixParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

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
    public static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        
        // Check if this is a TacZ bullet entity
        if (!isTaczBullet(entity)) {
            return;
        }
        
        // Check if trails are enabled
        if (!MatrixCraftConfig.TRAILS_ENABLED.get()) {
            return;
        }
        
        // Performance limiting
        if (processedThisTick >= MatrixCraftConfig.MAX_TRAILS_PER_TICK.get()) {
            return;
        }
        
        // Distance culling
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            double distance = mc.player.distanceToSqr(entity);
            double maxDist = MatrixCraftConfig.MAX_RENDER_DISTANCE.get();
            if (distance > maxDist * maxDist) {
                return;
            }
        }
        
        Vec3 currentPos = entity.position();
        int entityId = entity.getId();
        
        BulletTrailData data = trackedBullets.get(entityId);
        if (data == null) {
            data = new BulletTrailData(currentPos);
            trackedBullets.put(entityId, data);
        }
        
        // Spawn trail particles
        spawnTrailParticles(entity, data.lastPos, currentPos);
        
        data.lastPos = currentPos;
        data.ticksSinceSpawn++;
        processedThisTick++;
        
        // Clean up old entries
        if (data.ticksSinceSpawn > MatrixCraftConfig.TRAIL_LENGTH.get() || entity.isRemoved()) {
            trackedBullets.remove(entityId);
        }
    }
    
    private static void spawnTrailParticles(Entity bullet, Vec3 lastPos, Vec3 currentPos) {
        if (bullet.level() == null || !bullet.level().isClientSide) {
            return;
        }
        
        int density = MatrixCraftConfig.TRAIL_DENSITY.get();
        Vec3 motion = currentPos.subtract(lastPos);
        
        for (int i = 0; i < density; i++) {
            double t = i / (double) density;
            Vec3 particlePos = lastPos.add(motion.scale(t));
            
            // Add slight randomness for visual variety
            double offsetX = (Math.random() - 0.5) * 0.05;
            double offsetY = (Math.random() - 0.5) * 0.05;
            double offsetZ = (Math.random() - 0.5) * 0.05;
            
            // Get color from config
            float r = MatrixCraftConfig.TRAIL_RED.get() / 255f;
            float g = MatrixCraftConfig.TRAIL_GREEN.get() / 255f;
            float b = MatrixCraftConfig.TRAIL_BLUE.get() / 255f;
            
            bullet.level().addParticle(
                MatrixParticles.BULLET_TRAIL.get(),
                particlePos.x + offsetX,
                particlePos.y + offsetY,
                particlePos.z + offsetZ,
                motion.x, motion.y, motion.z
            );
        }
    }
    
    public static void spawnImpactEffect(Vec3 pos, Vec3 normal, Entity level) {
        if (!MatrixCraftConfig.IMPACTS_ENABLED.get()) {
            return;
        }
        
        int count = MatrixCraftConfig.IMPACT_PARTICLE_COUNT.get();
        double speed = MatrixCraftConfig.IMPACT_PARTICLE_SPEED.get();
        double radius = MatrixCraftConfig.IMPACT_RADIUS.get();
        
        for (int i = 0; i < count; i++) {
            double angle1 = Math.random() * Math.PI * 2;
            double angle2 = Math.random() * Math.PI;
            
            double vx = Math.sin(angle2) * Math.cos(angle1) * speed;
            double vy = Math.sin(angle2) * Math.sin(angle1) * speed;
            double vz = Math.cos(angle2) * speed;
            
            // Bias particles in the direction of the normal
            vx += normal.x * speed * 0.5;
            vy += normal.y * speed * 0.5;
            vz += normal.z * speed * 0.5;
            
            level.level().addParticle(
                MatrixParticles.BULLET_IMPACT.get(),
                pos.x + (Math.random() - 0.5) * radius,
                pos.y + (Math.random() - 0.5) * radius,
                pos.z + (Math.random() - 0.5) * radius,
                vx, vy, vz
            );
        }
    }
    
    // Reset tick counter at the end of each tick
    public static void resetTickCounter() {
        processedThisTick = 0;
    }
    
    // Helper method to identify TacZ bullets
    private static boolean isTaczBullet(Entity entity) {
        // Check if entity class name contains "EntityBullet" or is from TacZ
        String className = entity.getClass().getName();
        return className.contains("com.tacz") && 
               (className.contains("EntityBullet") || className.contains("AmmoEntity"));
    }
}