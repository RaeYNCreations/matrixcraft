package com.raeyncraft.matrixcraft.client.lighting;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Light Marker Entity - An invisible entity that emits dynamic light
 * 
 * This entity:
 * - Is invisible
 * - Has no collision
 * - Follows a target entity (bullet)
 * - Implements dynamic light emission for LambDynLights
 * - Auto-removes when target is gone
 * 
 * LambDynLights will automatically detect this entity and create dynamic lights!
 * 
 * NOTE: This entity is CLIENT-ONLY. It's only ever spawned on the client side
 * in SimpleDynamicLightManager.
 */
@OnlyIn(Dist.CLIENT)
public class LightMarkerEntity extends Entity {
    
    private int targetEntityId = -1;
    private int lightLevel = 15;
    private float red = 1.0f;
    private float green = 1.0f;
    private float blue = 1.0f;
    private int ticksAlive = 0;
    private int maxTicks = 20;
    private Vec3 offset = Vec3.ZERO;
    
    public LightMarkerEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.noCulling = true;
    }
    
    /**
     * Set the target entity to follow
     */
    public void setTarget(int entityId, Vec3 offset) {
        this.targetEntityId = entityId;
        this.offset = offset;
    }
    
    /**
     * Set light properties
     */
    public void setLightProperties(int level, float r, float g, float b) {
        this.lightLevel = Math.max(0, Math.min(15, level));
        this.red = Math.max(0f, Math.min(1f, r));
        this.green = Math.max(0f, Math.min(1f, g));
        this.blue = Math.max(0f, Math.min(1f, b));
    }
    
    /**
     * Set lifetime
     */
    public void setMaxTicks(int ticks) {
        this.maxTicks = ticks;
    }
    
    /**
     * Get light level - called by LambDynLights
     * Fades out as the entity ages for realistic decay
     */
    public int getLuminance() {
        if (maxTicks <= 0) return lightLevel;
        
        // Calculate fade: full brightness at start, fade to 0 at end
        float progress = (float) ticksAlive / (float) maxTicks;
        float fade = 1.0f - progress;
        
        // Apply fade to light level
        int fadedLevel = Math.max(0, (int)(lightLevel * fade));
        return fadedLevel;
    }
    
    /**
     * Check if this should emit light - called by LambDynLights
     */
    public boolean isDynamicLightEnabled() {
        return lightLevel > 0;
    }
    
    /**
     * Get light color - for colored dynamic lights
     */
    public int getLightColor() {
        int r = (int)(red * 255);
        int g = (int)(green * 255);
        int b = (int)(blue * 255);
        return (r << 16) | (g << 8) | b;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        ticksAlive++;
        
        // Remove if expired
        if (ticksAlive >= maxTicks) {
            this.discard();
            return;
        }
        
        // Follow target entity
        if (targetEntityId >= 0) {
            Entity target = level().getEntity(targetEntityId);
            if (target == null || target.isRemoved()) {
                this.discard();
                return;
            }
            
            // Update position to follow target with offset
            Vec3 targetPos = target.position().add(offset);
            this.setPos(targetPos.x, targetPos.y, targetPos.z);
        }
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // No synced data needed - client-only entity
    }
    
    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        // Don't save - client-only entity
    }
    
    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        // Don't save - client-only entity
    }
    
    @Override
    public boolean isPickable() {
        return false;
    }
    
    @Override
    public boolean isPushable() {
        return false;
    }
    
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return false; // Invisible
    }
    
    @Override
    public boolean isAttackable() {
        return false;
    }
}
