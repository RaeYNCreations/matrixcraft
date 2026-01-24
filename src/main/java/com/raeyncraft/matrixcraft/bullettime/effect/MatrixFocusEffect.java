package com.raeyncraft.matrixcraft.bullettime.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.resources.ResourceLocation;

/**
 * The Matrix Focus effect - grants combat bonuses in exchange for slower movement.
 * This is the server-side component for multiplayer balance.
 */
public class MatrixFocusEffect extends MobEffect {
    
    // Attribute modifier IDs
    private static final ResourceLocation MOVEMENT_MODIFIER_ID =
        ResourceLocation.fromNamespaceAndPath("matrixcraft", "matrix_focus_speed");
    
    public MatrixFocusEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x00FF00); // Green color
    }
    
    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // Effect tick logic if needed
        // Most effects are handled via attributes and the FocusManager
        return true;
    }
    
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Apply every tick for smooth effect
        return true;
    }
    
    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        // Apply movement speed reduction
        if (!entity.level().isClientSide) {
            var movementAttribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movementAttribute != null) {
                // Remove existing modifier if present
                movementAttribute.removeModifier(MOVEMENT_MODIFIER_ID);
                
                // Add 30% movement speed reduction
                movementAttribute.addTransientModifier(new AttributeModifier(
                    MOVEMENT_MODIFIER_ID,
                    -0.30, // -30% speed
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                ));
            }
        }
        
        super.onEffectStarted(entity, amplifier);
    }
    
    @Override
    public void onEffectAdded(LivingEntity entity, int amplifier) {
        super.onEffectAdded(entity, amplifier);
    }
    
    /**
     * Called when effect is removed - clean up attribute modifiers
     */
    public static void onEffectRemoved(LivingEntity entity) {
        var movementAttribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementAttribute != null) {
            movementAttribute.removeModifier(MOVEMENT_MODIFIER_ID);
        }
    }
}
