package com.raeyncraft.matrixcraft.bullettime.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The Matrix Focus effect - grants combat bonuses in exchange for slower movement.
 * This is the server-side component for multiplayer balance.
 * 
 * Applies:
 * - Movement speed reduction (30%)
 * - Jump strength reduction (40%) - for slower, more floaty jumps
 * - Gravity reduction (50%) - makes jumps feel slow-motion
 */
public class MatrixFocusEffect extends MobEffect {
    
    // Attribute modifier IDs
    private static final ResourceLocation MOVEMENT_MODIFIER_ID = 
        ResourceLocation.fromNamespaceAndPath("matrixcraft", "focus.movement");
    private static final ResourceLocation JUMP_MODIFIER_ID = 
        ResourceLocation.fromNamespaceAndPath("matrixcraft", "focus.jump");
    private static final ResourceLocation GRAVITY_MODIFIER_ID = 
        ResourceLocation.fromNamespaceAndPath("matrixcraft", "focus.gravity");
    
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
        if (!entity.level().isClientSide) {
            applyAttributeModifiers(entity);
        }
        super.onEffectStarted(entity, amplifier);
    }
    
    @Override
    public void onEffectAdded(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            applyAttributeModifiers(entity);
        }
        super.onEffectAdded(entity, amplifier);
    }
    
    /**
     * Apply all attribute modifiers for bullet time effect
     */
    private static void applyAttributeModifiers(LivingEntity entity) {
        // Movement speed reduction (30%)
        var movementAttribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementAttribute != null) {
            movementAttribute.removeModifier(MOVEMENT_MODIFIER_ID);
            movementAttribute.addTransientModifier(new AttributeModifier(
                MOVEMENT_MODIFIER_ID,
                -0.20, // -20% speed
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
        
        // Jump strength reduction (40%) - lower jumps
        var jumpAttribute = entity.getAttribute(Attributes.JUMP_STRENGTH);
        if (jumpAttribute != null) {
            jumpAttribute.removeModifier(JUMP_MODIFIER_ID);
            jumpAttribute.addTransientModifier(new AttributeModifier(
                JUMP_MODIFIER_ID,
                -0.10, // -10% jump strength
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
        
        // Gravity reduction (50%) - slower falling, floaty jumps
        var gravityAttribute = entity.getAttribute(Attributes.GRAVITY);
        if (gravityAttribute != null) {
            gravityAttribute.removeModifier(GRAVITY_MODIFIER_ID);
            gravityAttribute.addTransientModifier(new AttributeModifier(
                GRAVITY_MODIFIER_ID,
                -0.60, // -50% gravity (half gravity = slow-mo falling)
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
    }
    
    /**
     * Called when effect is removed - clean up all attribute modifiers
     */
    public static void onEffectRemoved(LivingEntity entity) {
        var movementAttribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementAttribute != null) {
            movementAttribute.removeModifier(MOVEMENT_MODIFIER_ID);
        }
        
        var jumpAttribute = entity.getAttribute(Attributes.JUMP_STRENGTH);
        if (jumpAttribute != null) {
            jumpAttribute.removeModifier(JUMP_MODIFIER_ID);
        }
        
        var gravityAttribute = entity.getAttribute(Attributes.GRAVITY);
        if (gravityAttribute != null) {
            gravityAttribute.removeModifier(GRAVITY_MODIFIER_ID);
        }
    }
}
