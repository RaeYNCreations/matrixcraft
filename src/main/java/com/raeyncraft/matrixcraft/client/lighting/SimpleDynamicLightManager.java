package com.raeyncraft.matrixcraft.client.lighting;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.client.BulletTrailLighting;
import com.raeyncraft.matrixcraft.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simplified Dynamic Light Manager - Creates actual light marker entities
 * 
 * This manager creates invisible LightMarkerEntity instances that:
 * - Follow bullets and other entities
 * - Emit dynamic light that LambDynLights automatically detects
 * - Support RGB colored lighting
 * - Auto-cleanup when targets are removed
 * 
 * LambDynLights will automatically see these entities and create dynamic lights!
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT, modid = MatrixCraftMod.MODID)
public class SimpleDynamicLightManager {
    
    // Track entity light data (entity ID -> light info)
    private static final Map<Integer, EntityLightData> entityLights = new ConcurrentHashMap<>();
    
    // Track entity references for cleanup
    private static final Map<Integer, WeakReference<Entity>> entityRefs = new ConcurrentHashMap<>();
    
    // Track marker entities we've created (target entity ID -> list of markers)
    private static final Map<Integer, List<LightMarkerEntity>> markerEntities = new ConcurrentHashMap<>();
    
    // Last seen time for TTL cleanup
    private static final Map<Integer, Long> lastSeenMs = new ConcurrentHashMap<>();
    
    // TTL for entity lights
    private static final long ENTITY_TTL_MS = 3000L;
    
    // Max entities to track
    private static final int MAX_ENTITY_LIGHTS = 500;
    
    // Track current level for cleanup on world change
    private static WeakReference<net.minecraft.world.level.Level> lastLevel = new WeakReference<>(null);
    
    /**
     * Light data for an entity
     */
    public static class EntityLightData {
        public final int brightness;
        public final float red, green, blue;
        public final int chainCount;
        public final double chainSpacing;
        
        public EntityLightData(int brightness, float r, float g, float b, int chainCount, double chainSpacing) {
            this.brightness = brightness;
            this.red = r;
            this.green = g;
            this.blue = b;
            this.chainCount = chainCount;
            this.chainSpacing = chainSpacing;
        }
    }
    
    /**
     * Initialize - log that we're using marker entities
     */
    public static void init() {
        MatrixCraftMod.LOGGER.info("╔════════════════════════════════════════════════════════╗");
        MatrixCraftMod.LOGGER.info("║ SimpleDynamicLightManager initialized                 ║");
        MatrixCraftMod.LOGGER.info("║                                                        ║");
        MatrixCraftMod.LOGGER.info("║ Creates invisible marker entities that emit light     ║");
        MatrixCraftMod.LOGGER.info("║ LambDynLights will auto-detect these and create       ║");
        MatrixCraftMod.LOGGER.info("║ dynamic RGB colored lights!                           ║");
        MatrixCraftMod.LOGGER.info("║                                                        ║");
        MatrixCraftMod.LOGGER.info("║ Works with: LambDynLights, RyoamicLights, Shaders    ║");
        MatrixCraftMod.LOGGER.info("╚════════════════════════════════════════════════════════╝");
    }
    
    public static void ensureInit() {
        // Nothing to do
    }
    
    /**
     * Check if available - always true since we don't need external mods
     */
    public static boolean isDynamicLightsModAvailable() {
        return true; // We're always available
    }
    
    /**
     * Track a light on an entity (single light)
     * Creates an invisible marker entity that emits light
     */
    public static void trackEntityLight(Entity entity, int brightness, float r, float g, float b) {
        if (entity == null) return;
        
        // CLIENT-ONLY check - prevent server crashes
        if (!entity.level().isClientSide) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        
        int id = entity.getId();
        
        // Check if already tracking this entity - prevent duplicates
        if (markerEntities.containsKey(id)) {
            // Just update the timestamp
            lastSeenMs.put(id, System.currentTimeMillis());
            return;
        }
        
        entityLights.put(id, new EntityLightData(brightness, r, g, b, 1, 0.0));
        entityRefs.put(id, new WeakReference<>(entity));
        lastSeenMs.put(id, System.currentTimeMillis());
        
        // Create marker entity
        try {
            // Null check for entity type
            EntityType<LightMarkerEntity> entityType = ModEntities.LIGHT_MARKER.get();
            if (entityType == null) {
                MatrixCraftMod.LOGGER.warn("[SimpleDynamicLightManager] LIGHT_MARKER entity type not registered yet!");
                return;
            }
            
            // Double-check world is still valid before creating entity
            if (mc.level == null) {
                MatrixCraftMod.LOGGER.warn("[SimpleDynamicLightManager] World became null during entity creation!");
                return;
            }
            
            LightMarkerEntity marker = new LightMarkerEntity(entityType, mc.level);
            marker.setPos(entity.getX(), entity.getY(), entity.getZ());
            marker.setTarget(id, Vec3.ZERO);
            marker.setLightProperties(brightness, r, g, b);
            marker.setMaxTicks(100); // 5 seconds (increased from 3)
            
            // Final validation before adding to world
            if (mc.level != null && !marker.isRemoved()) {
                mc.level.addFreshEntity(marker);
                
                List<LightMarkerEntity> markers = new ArrayList<>();
                markers.add(marker);
                markerEntities.put(id, markers);
                
                MatrixCraftMod.LOGGER.debug("[SimpleDynamicLightManager] Created light marker for entity " + id + 
                    " RGB(" + (int)(r*255) + "," + (int)(g*255) + "," + (int)(b*255) + ")");
            } else {
                MatrixCraftMod.LOGGER.warn("[SimpleDynamicLightManager] Cannot add marker - world is null or marker is removed");
            }
        } catch (Exception e) {
            MatrixCraftMod.LOGGER.warn("[SimpleDynamicLightManager] Failed to create marker entity: " + e.getMessage());
        }
    }
    
    /**
     * Track a chain of lights on an entity
     * Creates multiple marker entities trailing behind the entity
     */
    public static void trackEntityLightChain(Entity entity, int chainCount, double chainSpacing, 
                                             int brightness, float r, float g, float b) {
        if (entity == null) return;
        
        // CLIENT-ONLY check - prevent server crashes
        if (!entity.level().isClientSide) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        
        int id = entity.getId();
        
        // Check if already tracking this entity - prevent duplicates
        if (markerEntities.containsKey(id)) {
            // Just update the timestamp
            lastSeenMs.put(id, System.currentTimeMillis());
            return;
        }
        
        entityLights.put(id, new EntityLightData(brightness, r, g, b, chainCount, chainSpacing));
        entityRefs.put(id, new WeakReference<>(entity));
        lastSeenMs.put(id, System.currentTimeMillis());
        
        // Create chain of marker entities
        try {
            // Null check for entity type
            EntityType<LightMarkerEntity> entityType = ModEntities.LIGHT_MARKER.get();
            if (entityType == null) {
                MatrixCraftMod.LOGGER.warn("[SimpleDynamicLightManager] LIGHT_MARKER entity type not registered yet!");
                return;
            }
            
            // Double-check world is still valid before creating entities
            if (mc.level == null) {
                MatrixCraftMod.LOGGER.warn("[SimpleDynamicLightManager] World became null during chain creation!");
                return;
            }
            
            // Null check for velocity
            Vec3 velocity = entity.getDeltaMovement();
            if (velocity == null) {
                velocity = Vec3.ZERO;
            }
            
            double vLen = Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y + velocity.z * velocity.z);
            Vec3 direction = vLen > 0.001 ? new Vec3(velocity.x / vLen, velocity.y / vLen, velocity.z / vLen) : Vec3.ZERO;
            
            List<LightMarkerEntity> markers = new ArrayList<>();
            
            for (int i = 0; i < chainCount; i++) {
                // Calculate offset for this marker in the chain
                Vec3 offset = direction.scale(-i * chainSpacing); // Behind the entity
                
                LightMarkerEntity marker = new LightMarkerEntity(entityType, mc.level);
                marker.setPos(entity.getX() + offset.x, entity.getY() + offset.y, entity.getZ() + offset.z);
                marker.setTarget(id, offset);
                marker.setLightProperties(brightness, r, g, b);
                marker.setMaxTicks(100); // 5 seconds (increased from 3)
                
                // Final validation before adding to world
                if (mc.level != null && !marker.isRemoved()) {
                    mc.level.addFreshEntity(marker);
                    markers.add(marker);
                } else {
                    MatrixCraftMod.LOGGER.warn("[SimpleDynamicLightManager] Cannot add chain marker #" + i + " - world is null or marker is removed");
                    break; // Stop creating more markers if world becomes invalid
                }
            }
            
            // Only add to tracking if we successfully created markers
            if (!markers.isEmpty()) {
                markerEntities.put(id, markers);
                
                MatrixCraftMod.LOGGER.debug("[SimpleDynamicLightManager] Created " + markers.size() + 
                    " light markers for entity " + id + " RGB(" + (int)(r*255) + "," + (int)(g*255) + "," + (int)(b*255) + ")");
            }
        } catch (Exception e) {
            MatrixCraftMod.LOGGER.warn("[SimpleDynamicLightManager] Failed to create marker chain: " + e.getMessage());
        }
    }
    
    /**
     * Ping an entity to mark it as active
     */
    public static void pingEntity(int id) {
        lastSeenMs.put(id, System.currentTimeMillis());
    }
    
    /**
     * Stop tracking a light by entity ID
     * Removes all marker entities for this target
     * Thread-safe to prevent concurrent modification issues
     */
    public static void untrackEntityLightById(int id) {
        entityLights.remove(id);
        entityRefs.remove(id);
        lastSeenMs.remove(id);
        
        // Remove marker entities (synchronized to prevent concurrent modification)
        List<LightMarkerEntity> markers = markerEntities.remove(id);
        if (markers != null) {
            synchronized (markers) {
                for (LightMarkerEntity marker : markers) {
                    if (marker != null && !marker.isRemoved()) {
                        try {
                            marker.discard();
                        } catch (Exception e) {
                            // Ignore - entity might already be discarded
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Get light data for an entity
     */
    public static EntityLightData getEntityLight(int entityId) {
        return entityLights.get(entityId);
    }
    
    /**
     * Get light positions for an entity (including chain)
     */
    public static List<Vec3> getEntityLightPositions(Entity entity) {
        if (entity == null) return Collections.emptyList();
        
        EntityLightData data = entityLights.get(entity.getId());
        if (data == null) return Collections.emptyList();
        
        List<Vec3> positions = new ArrayList<>();
        Vec3 basePos = entity.position();
        Vec3 velocity = entity.getDeltaMovement();
        
        // Normalize velocity for direction
        double vLen = Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y + velocity.z * velocity.z);
        Vec3 direction = vLen > 0.001 ? new Vec3(velocity.x / vLen, velocity.y / vLen, velocity.z / vLen) : Vec3.ZERO;
        
        // Add positions for chain
        for (int i = 0; i < data.chainCount; i++) {
            Vec3 offset = direction.scale(i * data.chainSpacing);
            positions.add(basePos.subtract(offset));
        }
        
        return positions;
    }
    
    /**
     * Get all tracked entity IDs
     */
    public static Set<Integer> getTrackedEntityIds() {
        return new HashSet<>(entityLights.keySet());
    }
    
    /**
     * Clear all tracked lights and marker entities
     */
    public static void clearAllDynamicLights() {
        // Remove all marker entities
        for (List<LightMarkerEntity> markers : markerEntities.values()) {
            if (markers != null) {
                for (LightMarkerEntity marker : markers) {
                    if (marker != null && !marker.isRemoved()) {
                        marker.discard();
                    }
                }
            }
        }
        
        entityLights.clear();
        entityRefs.clear();
        lastSeenMs.clear();
        markerEntities.clear();
        MatrixCraftMod.LOGGER.info("[SimpleDynamicLightManager] Cleared all tracked lights and markers");
    }
    
    /**
     * Tick to clean up old lights
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        
        // Check for world change
        net.minecraft.world.level.Level currentLevel = mc.level;
        net.minecraft.world.level.Level lastLevelRef = lastLevel.get();
        if (lastLevelRef != currentLevel) {
            clearAllDynamicLights();
            lastLevel = new WeakReference<>(currentLevel);
            MatrixCraftMod.LOGGER.info("[SimpleDynamicLightManager] World changed, cleared all lights");
        }
        
        // Tick bullet trail lighting
        BulletTrailLighting.tick();
        
        // Clean up old entities - use safe iteration to prevent concurrent modification
        long now = System.currentTimeMillis();
        List<Integer> toRemove = new ArrayList<>();
        
        // First pass: identify entities to remove
        for (Map.Entry<Integer, WeakReference<Entity>> entry : entityRefs.entrySet()) {
            int id = entry.getKey();
            WeakReference<Entity> ref = entry.getValue();
            Entity e = ref == null ? null : ref.get();
            
            // Use getOrDefault to avoid race condition where lastSeen is removed between check and access
            Long lastSeen = lastSeenMs.getOrDefault(id, now);
            boolean tooOld = (now - lastSeen > ENTITY_TTL_MS);
            
            if (e == null || e.isRemoved() || !e.isAlive() || tooOld) {
                toRemove.add(id);
            }
        }
        
        // Second pass: remove identified entities
        for (Integer id : toRemove) {
            untrackEntityLightById(id);
            entityRefs.remove(id);
        }
        
        // Enforce size limits
        if (entityLights.size() > MAX_ENTITY_LIGHTS) {
            // Remove oldest
            List<Integer> ids = new ArrayList<>(lastSeenMs.keySet());
            ids.sort((a, b) -> {
                Long timeA = lastSeenMs.getOrDefault(a, now);
                Long timeB = lastSeenMs.getOrDefault(b, now);
                return timeA.compareTo(timeB);
            });
            
            int toRemoveCount = entityLights.size() - (MAX_ENTITY_LIGHTS * 4 / 5);
            for (int i = 0; i < toRemoveCount && i < ids.size(); i++) {
                untrackEntityLightById(ids.get(i));
            }
            MatrixCraftMod.LOGGER.info("[SimpleDynamicLightManager] Cleaned up " + toRemoveCount + " old lights");
        }
    }
    
    /**
     * Force update all - for compatibility, does nothing
     */
    public static void forceUpdateAll() {
        // Nothing to do - we're passive
    }
}
