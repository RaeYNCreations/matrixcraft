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
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(value = Dist.CLIENT, modid = MatrixCraftMod.MODID)
public class BulletImpactTracker {
    
    private static final Map<Integer, Vec3> trackedBullets = new HashMap<>();
    private static final Set<Integer> processedImpacts = new HashSet<>();
    
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        
        if (mc.player.tickCount % 100 == 0) {
            MatrixCraftMod.LOGGER.info("[BulletImpact] Tracker is running. Impact lighting enabled: " + MatrixCraftConfig.IMPACT_LIGHTING_ENABLED.get());
            MatrixCraftMod.LOGGER.info("[BulletImpact] Currently tracking " + trackedBullets.size() + " bullets");
        }
        
        if (!MatrixCraftConfig.IMPACT_LIGHTING_ENABLED.get()) return;
        
        Set<Integer> currentBullets = new HashSet<>();
        
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!isTaczBullet(entity)) continue;
            
            int id = entity.getId();
            currentBullets.add(id);
            
            if (!trackedBullets.containsKey(id)) {
                trackedBullets.put(id, entity.position());
                MatrixCraftMod.LOGGER.info("[BulletImpact] Started tracking bullet " + id + " at " + entity.position());
            } else {
                trackedBullets.put(id, entity.position());
            }
        }
        
        Set<Integer> removedBullets = new HashSet<>();
        for (Integer id : trackedBullets.keySet()) {
            if (!currentBullets.contains(id) && !processedImpacts.contains(id)) {
                removedBullets.add(id);
            }
        }
        
        for (Integer id : removedBullets) {
            Vec3 impactPos = trackedBullets.get(id);
            if (impactPos != null) {
                MatrixCraftMod.LOGGER.info("[BulletImpact] *** IMPACT DETECTED *** for bullet " + id + " at " + impactPos);
                createImpactEffect(impactPos, (ClientLevel) mc.level);
                processedImpacts.add(id);
            }
        }
        
        trackedBullets.keySet().removeIf(id -> !currentBullets.contains(id));
        
        if (mc.player.tickCount % 100 == 0) {
            processedImpacts.clear();
        }
    }
    
    private static void createImpactEffect(Vec3 pos, ClientLevel level) {
        // Remove the first log line
        
        // Create impact light
        if (BulletTrailLighting.isDynamicLightingEnabled()) {
            int brightness = MatrixCraftConfig.IMPACT_LIGHT_LEVEL.get();
            int duration = MatrixCraftConfig.IMPACT_LIGHT_DURATION.get();
            float[] color = BulletTrailLighting.getTrailColor();
            
            BulletTrailLighting.addImpactLight(pos.x, pos.y, pos.z, brightness, duration, color[0], color[1], color[2]);
            // Remove the detailed log
        }
        
        // Spawn impact particles
        int particleCount = MatrixCraftConfig.IMPACT_PARTICLE_COUNT.get();
        double spread = MatrixCraftConfig.IMPACT_PARTICLE_SPREAD.get();
        
        // Remove the particle count log
        
        for (int i = 0; i < particleCount; i++) {
            double ox = (Math.random() - 0.5) * spread;
            double oy = (Math.random() - 0.5) * spread;
            double oz = (Math.random() - 0.5) * spread;
            
            double vx = (Math.random() - 0.5) * 0.1;
            double vy = (Math.random() - 0.5) * 0.1;
            double vz = (Math.random() - 0.5) * 0.1;
            
            level.addAlwaysVisibleParticle(
                MatrixParticles.BULLET_IMPACT.get(),
                true,
                pos.x + ox, pos.y + oy, pos.z + oz,
                vx, vy, vz
            );
        }
    }
    
    private static boolean isTaczBullet(Entity entity) {
        return entity.getClass().getName().equals("com.tacz.guns.entity.EntityKineticBullet")
                || String.valueOf(entity.getType()).toLowerCase().contains("tacz");
    }
}