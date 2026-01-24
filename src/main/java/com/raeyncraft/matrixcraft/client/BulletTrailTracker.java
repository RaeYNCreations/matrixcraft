package com.raeyncraft.matrixcraft.client;

import com.raeyncraft.matrixcraft.MatrixCraftConfig;
import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.particle.MatrixParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

import java.util.*;

/**
 * Bullet Trail Tracker v4 - Uses multiple detection methods:
 * 1. Attack key held detection (for local player)
 * 2. Sound-based detection (backup)  
 * 3. Entity scanning (for continuous trails)
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class BulletTrailTracker {
    
    // Track bullets we've seen
    private static final Map<Integer, BulletTrailData> trackedBullets = new HashMap<>();
    
    // Track firing state
    private static boolean wasAttackKeyDown = false;
    private static long lastTrailSpawnTime = 0;
    private static final long TRAIL_COOLDOWN_MS = 80; // Minimum ms between trails (matches fire rate)
    
    // Debug flag - set to true to see what's happening
    private static final boolean DEBUG = true;
    
    private static class BulletTrailData {
        Vec3 lastPos;
        int ticksTracked;
        boolean hadInitialTrail;
        
        BulletTrailData(Vec3 pos) {
            this.lastPos = pos;
            this.ticksTracked = 0;
            this.hadInitialTrail = false;
        }
    }
    
    /**
     * Client tick - check for attack key being held while holding TacZ gun
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!MatrixCraftConfig.TRAILS_ENABLED.get()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return; // In a GUI
        
        // Check if attack key is currently pressed
        boolean attackKeyDown = mc.options.keyAttack.isDown();
        
        // Check if holding a TacZ gun
        ItemStack mainHand = mc.player.getMainHandItem();
        boolean holdingGun = isTaczGun(mainHand);
        
        if (holdingGun && attackKeyDown) {
            long now = System.currentTimeMillis();
            
            // Spawn trail if enough time has passed (respects fire rate)
            if (now - lastTrailSpawnTime >= TRAIL_COOLDOWN_MS) {
                lastTrailSpawnTime = now;
                
                if (DEBUG && !wasAttackKeyDown) {
                    MatrixCraftMod.LOGGER.info("[BulletTrail] Attack key pressed while holding gun - spawning trail");
                }
                
                spawnInstantTrailFromPlayer(mc.player);
            }
        }
        
        wasAttackKeyDown = attackKeyDown;
        
        // Also scan for bullet entities
        scanForBullets(mc);
    }
    
    /**
     * Sound detection - backup method
     */
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (!MatrixCraftConfig.TRAILS_ENABLED.get()) return;
        if (event.getSound() == null) return;
        
        String soundName = event.getSound().getLocation().toString().toLowerCase();
        
        if (DEBUG && soundName.contains("tacz")) {
            MatrixCraftMod.LOGGER.info("[BulletTrail] TacZ sound: " + soundName);
        }
        
        // Check for gun fire sounds
        if (isTaczGunSound(soundName)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            
            double soundX = event.getSound().getX();
            double soundY = event.getSound().getY();
            double soundZ = event.getSound().getZ();
            Vec3 soundPos = new Vec3(soundX, soundY, soundZ);
            
            // Find who fired
            Player shooter = findShooterNear(soundPos);
            if (shooter != null) {
                long now = System.currentTimeMillis();
                if (now - lastTrailSpawnTime >= TRAIL_COOLDOWN_MS) {
                    lastTrailSpawnTime = now;
                    
                    if (DEBUG) {
                        MatrixCraftMod.LOGGER.info("[BulletTrail] Sound trigger - spawning trail for " + shooter.getName().getString());
                    }
                    
                    spawnInstantTrailFromPlayer(shooter);
                }
            }
        }
    }
    
    /**
     * Scan for bullet entities and maintain continuous trails
     */
    private static void scanForBullets(Minecraft mc) {
        int bulletsFound = 0;
        
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!isTaczBullet(entity)) continue;
            
            bulletsFound++;
            
            // Distance culling
            double distance = mc.player.distanceToSqr(entity);
            double maxDist = MatrixCraftConfig.MAX_RENDER_DISTANCE.get();
            if (distance > maxDist * maxDist) continue;
            
            int entityId = entity.getId();
            Vec3 currentPos = entity.position();
            Vec3 velocity = entity.getDeltaMovement();
            
            BulletTrailData data = trackedBullets.get(entityId);
            if (data == null) {
                // NEW bullet detected
                data = new BulletTrailData(currentPos);
                trackedBullets.put(entityId, data);
                
                if (DEBUG) {
                    MatrixCraftMod.LOGGER.info("[BulletTrail] New bullet entity detected at " + currentPos + " vel=" + velocity.length());
                }
                
                // Spawn backward trail if we haven't spawned one recently via other methods
                if (!data.hadInitialTrail && velocity.lengthSqr() > 0.1) {
                    spawnBackwardTrail(currentPos, velocity);
                    data.hadInitialTrail = true;
                }
            } else {
                // Existing bullet - continuous trail
                if (currentPos.distanceToSqr(data.lastPos) > 0.01) {
                    spawnTrailBetween(data.lastPos, currentPos);
                }
            }
            
            data.lastPos = currentPos;
            data.ticksTracked++;
            
            if (data.ticksTracked > MatrixCraftConfig.TRAIL_LENGTH.get() * 2) {
                trackedBullets.remove(entityId);
            }
        }
        
        // Cleanup removed bullets
        trackedBullets.entrySet().removeIf(entry -> {
            Entity e = mc.level.getEntity(entry.getKey());
            return e == null || e.isRemoved();
        });
    }
    
    /**
     * Spawn trail FORWARD from player's gun
     */
    private static void spawnInstantTrailFromPlayer(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel clientLevel)) return;
        
        Vec3 gunPos = getGunPosition(player);
        Vec3 lookDir = player.getLookAngle();
        
        // Trail length - how far a bullet travels in ~2 seconds
        double trailLength = 50.0;
        int particleCount = 75;
        
        if (DEBUG) {
            MatrixCraftMod.LOGGER.info("[BulletTrail] Spawning forward trail from " + gunPos + " dir=" + lookDir);
        }
        
        for (int i = 0; i < particleCount; i++) {
            double t = i / (double) particleCount;
            Vec3 pos = gunPos.add(lookDir.scale(t * trailLength));
            
            double ox = (Math.random() - 0.5) * 0.04;
            double oy = (Math.random() - 0.5) * 0.04;
            double oz = (Math.random() - 0.5) * 0.04;
            
            clientLevel.addAlwaysVisibleParticle(
                MatrixParticles.BULLET_TRAIL.get(),
                true,
                pos.x + ox, pos.y + oy, pos.z + oz,
                0, 0, 0
            );
        }
    }
    
    /**
     * Spawn trail BACKWARD from bullet's current position
     */
    private static void spawnBackwardTrail(Vec3 bulletPos, Vec3 velocity) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel clientLevel)) return;
        
        Vec3 direction = velocity.normalize();
        double trailLength = 40.0;
        int particleCount = 60;
        
        if (DEBUG) {
            MatrixCraftMod.LOGGER.info("[BulletTrail] Spawning backward trail from " + bulletPos);
        }
        
        for (int i = 0; i < particleCount; i++) {
            double t = i / (double) particleCount;
            Vec3 pos = bulletPos.subtract(direction.scale(t * trailLength));
            
            double ox = (Math.random() - 0.5) * 0.04;
            double oy = (Math.random() - 0.5) * 0.04;
            double oz = (Math.random() - 0.5) * 0.04;
            
            clientLevel.addAlwaysVisibleParticle(
                MatrixParticles.BULLET_TRAIL.get(),
                true,
                pos.x + ox, pos.y + oy, pos.z + oz,
                0, 0, 0
            );
        }
    }
    
    /**
     * Spawn trail between two positions
     */
    private static void spawnTrailBetween(Vec3 from, Vec3 to) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel clientLevel)) return;
        
        double distance = from.distanceTo(to);
        if (distance < 0.1) return;
        
        int density = MatrixCraftConfig.TRAIL_DENSITY.get();
        int count = Math.max(density, (int)(distance * density * 2));
        count = Math.min(count, 30);
        
        for (int i = 0; i < count; i++) {
            double t = i / (double) count;
            Vec3 pos = from.lerp(to, t);
            
            double ox = (Math.random() - 0.5) * 0.03;
            double oy = (Math.random() - 0.5) * 0.03;
            double oz = (Math.random() - 0.5) * 0.03;
            
            clientLevel.addAlwaysVisibleParticle(
                MatrixParticles.BULLET_TRAIL.get(),
                true,
                pos.x + ox, pos.y + oy, pos.z + oz,
                0, 0, 0
            );
        }
    }
    
    /**
     * Get gun barrel position
     */
    private static Vec3 getGunPosition(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        
        return eye
            .add(look.scale(0.5))
            .add(right.scale(0.3))
            .subtract(0, 0.1, 0);
    }
    
    /**
     * Find player near a position
     */
    private static Player findShooterNear(Vec3 pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        
        for (Player player : mc.level.players()) {
            if (player.getEyePosition().distanceTo(pos) < 3.0) {
                return player;
            }
        }
        return null;
    }
    
    /**
     * Check if sound is a TacZ gun sound
     */
    private static boolean isTaczGunSound(String name) {
        if (!name.contains("tacz")) return false;
        
        // Common patterns in TacZ gun sounds
        return name.contains("shoot") || 
               name.contains("fire") ||
               name.contains("_s") ||    // Many TacZ sounds use _s suffix
               name.contains("gun") ||
               name.contains("rifle") ||
               name.contains("pistol") ||
               name.contains("smg") ||
               name.contains("sniper") ||
               name.contains("shotgun") ||
               name.contains("silencer") ||
               name.contains("suppressor");
    }
    
    /**
     * Check if item is a TacZ gun
     */
    private static boolean isTaczGun(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        // Check class name
        String className = stack.getItem().getClass().getName();
        if (className.contains("tacz") || className.contains("guns")) {
            if (DEBUG) {
                MatrixCraftMod.LOGGER.debug("[BulletTrail] Holding TacZ gun: " + className);
            }
            return true;
        }
        
        // Check item ID
        String itemId = stack.getItem().toString();
        return itemId.contains("tacz:");
    }
    
    /**
     * Check if entity is a TacZ bullet
     */
    private static boolean isTaczBullet(Entity entity) {
        return entity.getClass().getName().equals("com.tacz.guns.entity.EntityKineticBullet");
    }
}
