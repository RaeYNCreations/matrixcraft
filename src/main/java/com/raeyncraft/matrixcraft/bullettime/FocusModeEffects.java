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
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles Focus mode effects that integrate with other game systems:
 * - Lava/fire immunity during Focus (configurable)
 * - Automatic cobweb bypass during Focus (configurable)
 * - Manual lava immunity toggle (separate from Focus)
 * 
 * These effects can be toggled via commands and are saved to config.
 */
@EventBusSubscriber(modid = MatrixCraftMod.MODID)
public class FocusModeEffects {
    
    // Track players who had cobwebs disabled before Focus (to restore their state)
    private static final Set<UUID> cobwebsWereEnabled = new HashSet<>();
    
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
     * Handle cobweb bypass during Focus mode
     * This is checked every tick to enable/disable cobwebs based on Focus state
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        
        // Check if cobweb bypass is enabled
        if (!isFocusCobwebBypassEnabled()) return;
        
        UUID playerId = player.getUUID();
        boolean inFocus = FocusManager.isInFocus(player);
        
        if (inFocus) {
            // Entering Focus - disable cobwebs if they were enabled
            if (MatrixSettings.areCobwebsEnabled()) {
                cobwebsWereEnabled.add(playerId);
                MatrixSettings.setCobwebsEnabled(false);
            }
        } else {
            // Not in Focus - restore cobwebs if we disabled them
            if (cobwebsWereEnabled.contains(playerId)) {
                cobwebsWereEnabled.remove(playerId);
                MatrixSettings.setCobwebsEnabled(true);
            }
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
}
