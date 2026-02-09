package com.raeyncraft.matrixcraft.silenthill;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Silent Hill Mode - Damage boost, health boost, and mob targeting system
 */
public class SilentHillMode {
    
    // Track players in Silent Hill mode
    private static final Set<UUID> silentHillPlayers = ConcurrentHashMap.newKeySet();
    
    // Attribute modifier ResourceLocation (unique for removal)
    private static final ResourceLocation HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(MatrixCraftMod.MODID, "silent_hill_health_boost");
    private static AttributeModifier healthModifier; // Store the modifier for removal
    
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
            return;
        }
        
        silentHillPlayers.add(playerId);
        
        // Apply health boost
        applyHealthBoost(player);
        
        // Client-side effects removed - you didn't ask for them
        
        MatrixCraftMod.LOGGER.info("[SilentHill] Enabled for player: " + player.getName().getString());
    }
    
    public static void disable(Player player) {
        UUID playerId = player.getUUID();
        
        if (!silentHillPlayers.contains(playerId)) {
            return;
        }
        
        silentHillPlayers.remove(playerId);
        
        // Remove health boost
        removeHealthBoost(player);
        
        // Client-side effects removed - you didn't ask for them
        
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
        if (healthModifier != null) {
            healthAttr.removeModifier(healthModifier);
        }
        
        // Calculate bonus health (current max health * 1.0 = +100%)
        double baseHealth = healthAttr.getBaseValue();
        double bonusHealth = baseHealth; // Double the base health
        
        // Add modifier (fixed constructor: ResourceLocation, double, Operation)
        healthModifier = new AttributeModifier(
            HEALTH_MODIFIER_ID,
            bonusHealth,
            AttributeModifier.Operation.ADD_VALUE
        );
        
        healthAttr.addPermanentModifier(healthModifier);
        
        // Heal player to new max
        player.setHealth(player.getMaxHealth());
    }
    
    /**
     * Remove health boost
     */
    private static void removeHealthBoost(Player player) {
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr == null) return;
        
        if (healthModifier != null) {
            healthAttr.removeModifier(healthModifier);
        }
        
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
            ((net.minecraft.world.entity.Mob) event.getEntity()).setTarget((LivingEntity) closestPlayer);
            MatrixCraftMod.LOGGER.debug("[SilentHill] Mob targeting: " + closestPlayer.getName().getString());
        }
    }
    
    /**
     * Clean up when player logs out
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerLoggedOutEvent event) {
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