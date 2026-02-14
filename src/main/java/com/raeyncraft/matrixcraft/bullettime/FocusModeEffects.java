package com.raeyncraft.matrixcraft.bullettime;

import com.raeyncraft.matrixcraft.MatrixCraftConfig;
import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.command.MatrixSettings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles Focus mode effects that integrate with other game systems:
 * - Lava/fire immunity during Focus (configurable)
 * - Cobweb bypass during Focus (handled in CobwebBlockMixin)
 * - Water bypass during Focus (configurable)
 * - Manual lava immunity toggle (separate from Focus)
 * 
 * These effects can be toggled via commands and are saved to config.
 */
@EventBusSubscriber(modid = MatrixCraftMod.MODID)
public class FocusModeEffects {
    
    /**
     * Handle lava/fire damage immunity
     * Blocks damage if:
     * 1. Manual lava toggle is OFF (/matrix utilities lava off), OR
     * 2. Player is in Focus mode AND lava bypass is enabled in config
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        // Check damage type
        DamageSource source = event.getSource();
        
        boolean isFireDamage = source.is(DamageTypes.IN_FIRE) ||
                               source.is(DamageTypes.ON_FIRE) ||
                               source.is(DamageTypes.LAVA) ||
                               source.is(DamageTypes.HOT_FLOOR);
        
        if (!isFireDamage) return;
        
        // Check if manual lava immunity is active
        boolean manualLavaImmunity = !MatrixSettings.isLavaEnabled();
        
        // Check if Focus mode lava bypass is active
        boolean focusLavaBypass = FocusManager.isInFocus(player) && isFocusLavaBypassEnabled();
        
        if (manualLavaImmunity || focusLavaBypass) {
            // Cancel the damage
            event.setNewDamage(0);
            
            // Also extinguish the player
            if (!player.level().isClientSide) {
                player.clearFire();
            }
        }
    }
    
    /**
     * Handle water bypass and lava effects during Focus mode
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        
        boolean inFocus = FocusManager.isInFocus(player);
        
        // Handle water bypass
        boolean waterBypassActive = !MatrixSettings.isWaterEnabled() || (inFocus && isFocusWaterBypassEnabled());
        if (waterBypassActive) {
            // Force player out of swimming animation
            if (player.isInWater()) {
                player.setSwimming(false);
                player.setOnGround(true); // Trick the game into thinking we're on ground
                
                // Apply high water movement speed
                AttributeInstance waterSpeed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.WATER_MOVEMENT_EFFICIENCY);
                if (waterSpeed != null && waterSpeed.getValue() < 1.0) {
                    waterSpeed.setBaseValue(1.75);
                }
            }
        }
        
        // Handle lava bypass - fire effects
        boolean lavaBypassActive = !MatrixSettings.isLavaEnabled() || (inFocus && isFocusLavaBypassEnabled());
        if (lavaBypassActive && player.isInLava()) {
            // Remove fire effects
            player.clearFire();
            player.setRemainingFireTicks(0);
        }
    }
    
    /**
     * Check if Focus mode lava bypass is enabled in config
     */
    public static boolean isFocusLavaBypassEnabled() {
        try {
            return MatrixCraftConfig.FOCUS_LAVA_IMMUNITY.get();
        } catch (Exception e) {
            return true; // Default enabled
        }
    }
    
    /**
     * Check if Focus mode cobweb bypass is enabled in config
     */
    public static boolean isFocusCobwebBypassEnabled() {
        try {
            return MatrixCraftConfig.FOCUS_COBWEB_BYPASS.get();
        } catch (Exception e) {
            return true; // Default enabled
        }
    }
    
    /**
     * Set Focus mode lava bypass enabled state
     */
    public static void setFocusLavaBypass(boolean enabled) {
        try {
            MatrixCraftConfig.FOCUS_LAVA_IMMUNITY.set(enabled);
            MatrixCraftConfig.saveCommonConfig();
        } catch (Exception e) {
            MatrixCraftMod.LOGGER.error("Failed to set lava bypass: " + e.getMessage());
        }
    }
    
    /**
     * Set Focus mode cobweb bypass enabled state
     */
    public static void setFocusCobwebBypass(boolean enabled) {
        try {
            MatrixCraftConfig.FOCUS_COBWEB_BYPASS.set(enabled);
            MatrixCraftConfig.saveCommonConfig();
        } catch (Exception e) {
            MatrixCraftMod.LOGGER.error("Failed to set cobweb bypass: " + e.getMessage());
        }
    }
    
    /**
     * Check if Focus mode water bypass is enabled in config
     */
    public static boolean isFocusWaterBypassEnabled() {
        try {
            return MatrixCraftConfig.FOCUS_WATER_BYPASS.get();
        } catch (Exception e) {
            return true; // Default enabled
        }
    }
    
    /**
     * Set Focus mode water bypass enabled state
     */
    public static void setFocusWaterBypass(boolean enabled) {
        try {
            MatrixCraftConfig.FOCUS_WATER_BYPASS.set(enabled);
            MatrixCraftConfig.saveCommonConfig();
        } catch (Exception e) {
            MatrixCraftMod.LOGGER.error("Failed to set water bypass: " + e.getMessage());
        }
    }
}
