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
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Bullet Trail Tracker - Uses TacZ's GunFireEvent for INSTANT detection!
 * 
 * TacZ fires GunFireEvent on the client side when a gun fires.
 * This gives us instant detection with zero delay.
 * No mixins required!
 * 
 * Now includes dynamic lighting integration with LambDynamicLights (if available)
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class BulletTrailTracker {
    
    // Cooldown to prevent duplicate trails
    private static long lastTrailTime = 0;
    private static final long TRAIL_COOLDOWN_MS = 30;
    
    // Track bullets for continuous trails (other players' bullets)
    private static final Set<Integer> processedBullets = new HashSet<>();
    private static final Map<Integer, Vec3> bulletLastPos = new HashMap<>();
    
    // Cleanup counter
    private static int tickCounter = 0;
    
    // Dynamic lighting - add lights every N particles
    private static final int LIGHT_SPACING = 10; // Add a light every 10 particles
    
    // ==================== TACZ EVENT HANDLER ====================
    
    /**
     * Listen for TacZ GunFireEvent - this fires INSTANTLY when a gun shoots!
     * This is a NeoForge event posted by TacZ, no mixin needed.
     */
    @SubscribeEvent
    public static void onGunFire(com.tacz.guns.api.event.common.GunFireEvent event) {
        // Only process client-side events
        if (event.getLogicalSide() != LogicalSide.CLIENT) return;
        if (!MatrixCraftConfig.TRAILS_ENABLED.get()) return;
        
        // Get the shooter
        if (!(event.getShooter() instanceof Player player)) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        // Rate limit
        long now = System.currentTimeMillis();
        if (now - lastTrailTime < TRAIL_COOLDOWN_MS) return;
        lastTrailTime = now;
        
        MatrixCraftMod.LOGGER.info("[BulletTrail] GunFireEvent! Shooter: " + player.getName().getString());
        
        // Spawn trail from the shooter
        spawnTrailFromPlayer(player, (ClientLevel) mc.level);
    }
    
    // ==================== TICK-BASED FOR OTHER PLAYERS ====================
    
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!MatrixCraftConfig.TRAILS_ENABLED.get()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return;
        
        tickCounter++;
        
        // Update dynamic lighting system
        BulletTrailLighting.tick();
        
        // Scan for bullet entities (for other players' bullets + continuous trails)
        scanBulletEntities(mc);
        
        // Cleanup every 2 seconds
        if (tickCounter % 40 == 0) {
            cleanupOldEntries(mc);
        }
    }
    
    /**
     * Scan for bullet entities - for other players and continuous trails
     */
    private static void scanBulletEntities(Minecraft mc) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!isTaczBullet(entity)) continue;
            
            int entityId = entity.getId();
            Vec3 currentPos = entity.position();
            Vec3 velocity = entity.getDeltaMovement();
            
            // Distance check
            double distSq = mc.player.distanceToSqr(entity);
            double maxDist = MatrixCraftConfig.MAX_RENDER_DISTANCE.get();
            if (distSq > maxDist * maxDist) continue;
            
            // For bullets we haven't seen (other players' shots)
            if (!processedBullets.contains(entityId)) {
                processedBullets.add(entityId);
                
                // Only spawn if this is NOT the local player's bullet
                // (local player trails are handled by GunFireEvent)
                // We can tell by checking if the trail was just spawned
                long now = System.currentTimeMillis();
                if (now - lastTrailTime > 100 && velocity.lengthSqr() > 1.0) {
                    // This is another player's bullet - spawn trail
                    spawnTrailFromBullet(currentPos, velocity, (ClientLevel) mc.level);
                }
            }
            
            // Continuous trail - draw from last position to current
            Vec3 lastPos = bulletLastPos.get(entityId);
            if (lastPos != null && currentPos.distanceToSqr(lastPos) > 0.01) {
                spawnTrailSegment(lastPos, currentPos, (ClientLevel) mc.level);
            }
            
            bulletLastPos.put(entityId, currentPos);
        }
    }
    
    // ==================== TRAIL SPAWNING ====================
    
    /**
     * Spawn trail forward from player's gun position - INSTANT
     */
    private static void spawnTrailFromPlayer(Player player, ClientLevel level) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookDir = player.getLookAngle();
        
        // Start trail from just in front of the player's eyes
        // This creates the effect of bullets coming from center screen
        Vec3 muzzle = eyePos.add(lookDir.scale(0.5));
        
        // Spawn trail forward from muzzle
        double trailLength = 100.0;
        int particleCount = 150;
        
        // Check if glow/lighting is enabled
        boolean addLights = isGlowEnabled();
        
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
            
            // Add dynamic light source every N particles
            if (addLights && i % LIGHT_SPACING == 0) {
                BulletTrailLighting.addLightSource(pos.x, pos.y, pos.z);
            }
        }
    }
    
    /**
     * Spawn trail backward from bullet position (for other players)
     */
    private static void spawnTrailFromBullet(Vec3 bulletPos, Vec3 velocity, ClientLevel level) {
        Vec3 direction = velocity.normalize();
        
        double trailLength = 80.0;
        int particleCount = 120;
        
        boolean addLights = isGlowEnabled();
        
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
            
            // Add dynamic light source every N particles
            if (addLights && i % LIGHT_SPACING == 0) {
                BulletTrailLighting.addLightSource(pos.x, pos.y, pos.z);
            }
        }
    }
    
    /**
     * Spawn continuous trail segment between positions
     */
    private static void spawnTrailSegment(Vec3 from, Vec3 to, ClientLevel level) {
        double distance = from.distanceTo(to);
        if (distance < 0.1) return;
        
        int count = Math.max(3, (int)(distance * 3));
        count = Math.min(count, 20);
        
        boolean addLights = isGlowEnabled();
        
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
            
            // Add dynamic light at segment endpoints
            if (addLights && (i == 0 || i == count - 1)) {
                BulletTrailLighting.addLightSource(pos.x, pos.y, pos.z);
            }
        }
    }
    
    // ==================== UTILITIES ====================
    
    private static void cleanupOldEntries(Minecraft mc) {
        processedBullets.removeIf(id -> {
            Entity e = mc.level.getEntity(id);
            return e == null || e.isRemoved();
        });
        
        bulletLastPos.entrySet().removeIf(entry -> {
            Entity e = mc.level.getEntity(entry.getKey());
            return e == null || e.isRemoved();
        });
    }
    
    private static boolean isTaczBullet(Entity entity) {
        return entity.getClass().getName().equals("com.tacz.guns.entity.EntityKineticBullet");
    }
    
    private static boolean isGlowEnabled() {
        try {
            return MatrixCraftConfig.TRAIL_GLOW.get();
        } catch (Exception e) {
            return true;
        }
    }
}
