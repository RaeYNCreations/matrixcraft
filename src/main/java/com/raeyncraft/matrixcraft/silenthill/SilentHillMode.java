package com.raeyncraft.matrixcraft.silenthill;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Silent Hill Mode - Damage boost, health boost, and mob targeting system
 */
@EventBusSubscriber(modid = MatrixCraftMod.MODID)
public class SilentHillMode {
    
    // Track players in Silent Hill mode
    private static final Set<UUID> silentHillPlayers = ConcurrentHashMap.newKeySet();
    
    // Attribute modifier UUIDs (unique for removal)
    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("a3c8f9e2-1d4b-4c5a-9f2e-8b7a6c5d4e3f");
    
    // Constants
    private static final double DAMAGE_MULTIPLIER = 3.0; // 3x damage
    private static final double HEALTH_MULTIPLIER = 2.0; // 2x health (20 -> 40 hearts)
    private static final double MOB_TARGET_RANGE = 64.0; // 64 block range
    
    /**
     * Enable Silent Hill mode for a player
     */
    public static void enable(Player player) {
        UUID playerId = player.getUUID();
        
        if (silentHillPlayers.contains(playerId)) {
            return; // Already enabled
        }
        
        silentHillPlayers.add(playerId);
        
        // Apply health boost
        applyHealthBoost(player);
        
        // Client-side effects (textures, fog, sound)
        if (player.level().isClientSide) {
            com.raeyncraft.matrixcraft.client.SilentHillTextureManager.enable();
            com.raeyncraft.matrixcraft.client.SilentHillEffects.apply();
        }
        
        MatrixCraftMod.LOGGER.info("[SilentHill] Enabled for player: " + player.getName().getString());
    }
    
    /**
     * Disable Silent Hill mode for a player
     */
    public static void disable(Player player) {
        UUID playerId = player.getUUID();
        
        if (!silentHillPlayers.contains(playerId)) {
            return; // Not enabled
        }
        
        silentHillPlayers.remove(playerId);
        
        // Remove health boost
        removeHealthBoost(player);
        
        // Client-side cleanup
        if (player.level().isClientSide) {
            com.raeyncraft.matrixcraft.client.SilentHillTextureManager.disable();
            com.raeyncraft.matrixcraft.client.SilentHillEffects.remove();
        }
        
        MatrixCraftMod.LOGGER.info("[SilentHill] Disabled for player: " + player.getName().getString());
    }
    
    /**
     * Toggle Silent Hill mode
     */
    public static void toggle(Player player) {
        if (isInSilentHillMode(player)) {
            disable(player);
        } else {
            enable(player);
        }
    }
    
    /**
     * Check if a player is in Silent Hill mode
     */
    public static boolean isInSilentHillMode(Player player) {
        return silentHillPlayers.contains(player.getUUID());
    }
    
    /**
     * Get all players in Silent Hill mode
     */
    public static Set<UUID> getSilentHillPlayers() {
        return new HashSet<>(silentHillPlayers);
    }
    
    /**
     * Apply 2x health boost
     */
    private static void applyHealthBoost(Player player) {
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr == null) return;
        
        // Remove existing modifier if present
        healthAttr.removeModifier(HEALTH_MODIFIER_UUID);
        
        // Calculate bonus health (current max health * 1.0 = +100%)
        double baseHealth = healthAttr.getBaseValue();
        double bonusHealth = baseHealth; // Double the base health
        
        // Add modifier
        AttributeModifier modifier = new AttributeModifier(
            HEALTH_MODIFIER_UUID,
            "Silent Hill Health Boost",
            bonusHealth,
            AttributeModifier.Operation.ADD_VALUE
        );
        
        healthAttr.addPermanentModifier(modifier);
        
        // Heal player to new max
        player.setHealth(player.getMaxHealth());
    }
    
    /**
     * Remove health boost
     */
    private static void removeHealthBoost(Player player) {
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr == null) return;
        
        healthAttr.removeModifier(HEALTH_MODIFIER_UUID);
        
        // Cap current health to new max
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }
    
    // ==================== EVENT HANDLERS ====================
    
    /**
     * Apply 3x damage boost to players in Silent Hill mode
     */
    @SubscribeEvent
    public static void onPlayerDamage(LivingDamageEvent.Pre event) {
        // Only boost damage dealt BY players, not TO players
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        
        if (!isInSilentHillMode(attacker)) return;
        
        // Apply 3x damage multiplier
        float originalDamage = event.getOriginalDamage();
        float boostedDamage = originalDamage * (float) DAMAGE_MULTIPLIER;
        
        event.setNewDamage(boostedDamage);
        
        MatrixCraftMod.LOGGER.debug("[SilentHill] Damage boost: " + originalDamage + " -> " + boostedDamage);
    }
    
    /**
     * Force hostile mobs to target players in Silent Hill mode
     * If multiple Silent Hill players, target the closest one
     */
    @SubscribeEvent
    public static void onMobTargeting(LivingChangeTargetEvent event) {
        // Only affect hostile mobs
        if (!(event.getEntity() instanceof net.minecraft.world.entity.monster.Monster mob)) return;
        
        // Find all Silent Hill players within range
        List<Player> silentHillPlayersInRange = new ArrayList<>();
        
        for (UUID playerId : silentHillPlayers) {
            Player player = mob.level().getPlayerByUUID(playerId);
            
            if (player != null && !player.isSpectator() && !player.isCreative()) {
                double distance = mob.distanceTo(player);
                
                if (distance <= MOB_TARGET_RANGE) {
                    silentHillPlayersInRange.add(player);
                }
            }
        }
        
        // If no Silent Hill players in range, let vanilla AI work
        if (silentHillPlayersInRange.isEmpty()) return;
        
        // Find closest Silent Hill player
        Player closestPlayer = null;
        double closestDistance = Double.MAX_VALUE;
        
        for (Player player : silentHillPlayersInRange) {
            double distance = mob.distanceTo(player);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestPlayer = player;
            }
        }
        
        // Force mob to target the closest Silent Hill player
        if (closestPlayer != null) {
            event.setNewTarget(closestPlayer);
            MatrixCraftMod.LOGGER.debug("[SilentHill] Mob targeting: " + closestPlayer.getName().getString());
        }
    }

    @SubscribeEvent
    public static void onTACZDamage(TACZDamageEvent event) { // Replace with actual TACZ event
        if (event.getAttacker() instanceof Player player && isInSilentHillMode(player)) {
            event.setDamage(event.getDamage() * DAMAGE_MULTIPLIER);
        }
    }

    /**
     * Clean up when player logs out
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        silentHillPlayers.remove(event.getEntity().getUUID());
    }

    /**
     * Clean up on server stop
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        silentHillPlayers.clear();
    }
}