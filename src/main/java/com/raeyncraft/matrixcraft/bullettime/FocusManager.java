package com.raeyncraft.matrixcraft.bullettime;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.bullettime.effect.MatrixFocusEffect;
import com.raeyncraft.matrixcraft.bullettime.registry.BulletTimeRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for the Focus (bullet time) system.
 * Handles both single-player time dilation and multiplayer buff system.
 */
public class FocusManager {
    
    // Duration in ticks (10 seconds = 200 ticks)
    public static final int FOCUS_DURATION_TICKS = 200;
    
    // Track active focus states (server-side)
    private static final Map<UUID, FocusState> activeFocusStates = new ConcurrentHashMap<>();
    
    public static class FocusState {
        public final long startTime;
        public final int durationTicks;
        public int ticksRemaining;
        public final boolean isSinglePlayer;
        
        public FocusState(int durationTicks, boolean isSinglePlayer) {
            this.startTime = System.currentTimeMillis();
            this.durationTicks = durationTicks;
            this.ticksRemaining = durationTicks;
            this.isSinglePlayer = isSinglePlayer;
        }
        
        public float getProgress() {
            return (float) ticksRemaining / (float) durationTicks;
        }
    }
    
    /**
     * Activate Focus mode for a player (called from server)
     */
    public static void activateFocus(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        // Check if single player
        boolean isSinglePlayer = player.getServer() != null && 
                                 player.getServer().isSingleplayer();
        
        MatrixCraftMod.LOGGER.info("[MatrixFocus] Activating focus for " + player.getName().getString() + 
            " (SinglePlayer: " + isSinglePlayer + ")");
        
        // Create focus state
        FocusState state = new FocusState(FOCUS_DURATION_TICKS, isSinglePlayer);
        activeFocusStates.put(playerId, state);
        
        // Apply mob effect (for multiplayer buffs)
        MobEffectInstance effect = new MobEffectInstance(
            BulletTimeRegistry.MATRIX_FOCUS_EFFECT,
            FOCUS_DURATION_TICKS,
            0, // Amplifier
            false, // Ambient
            true, // Visible particles
            true  // Show icon
        );
        player.addEffect(effect);
        
        // Sync to client
        syncFocusToClient(player, true, FOCUS_DURATION_TICKS);
    }
    
    /**
     * Deactivate Focus mode for a player
     */
    public static void deactivateFocus(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        FocusState state = activeFocusStates.remove(playerId);
        if (state != null) {
            MatrixCraftMod.LOGGER.info("[MatrixFocus] Deactivating focus for " + player.getName().getString());
        }
        
        // Remove mob effect
        player.removeEffect(BulletTimeRegistry.MATRIX_FOCUS_EFFECT);
        
        // Clean up attribute modifiers
        MatrixFocusEffect.onEffectRemoved(player);
        
        // Sync to client
        syncFocusToClient(player, false, 0);
    }
    
    /**
     * Check if a player is currently in Focus mode
     */
    public static boolean isInFocus(Player player) {
        return activeFocusStates.containsKey(player.getUUID());
    }
    
    /**
     * Get the focus state for a player
     */
    public static FocusState getFocusState(Player player) {
        return activeFocusStates.get(player.getUUID());
    }
    
    /**
     * Called every server tick to update focus states
     */
    public static void serverTick() {
        var iterator = activeFocusStates.entrySet().iterator();
        
        while (iterator.hasNext()) {
            var entry = iterator.next();
            FocusState state = entry.getValue();
            
            state.ticksRemaining--;
            
            if (state.ticksRemaining <= 0) {
                iterator.remove();
                // Note: The mob effect will auto-expire, which triggers cleanup
            }
        }
    }
    
    /**
     * Sync focus state to client (via packet or direct call in SP)
     */
    private static void syncFocusToClient(ServerPlayer player, boolean active, int ticksRemaining) {
        // In integrated server (single player / LAN host), we can update client state directly
        // For dedicated servers, you'd need a network packet
        
        // For now, the client will detect the mob effect and sync that way
        // A proper implementation would use a custom packet
    }
    
    /**
     * Check if we should use actual time dilation (single player only)
     */
    public static boolean shouldUseTimeDilation(Player player) {
        FocusState state = activeFocusStates.get(player.getUUID());
        return state != null && state.isSinglePlayer;
    }
    
    // ==================== COMBAT MODIFIERS ====================
    
    /**
     * Get the accuracy multiplier for a focused player
     * Returns 1.0 for normal, lower values = better accuracy (less spread)
     */
    public static float getAccuracyMultiplier(Player player) {
        if (isInFocus(player)) {
            return 0.5f; // 50% spread reduction
        }
        return 1.0f;
    }
    
    /**
     * Get the recoil multiplier for a focused player
     * Returns 1.0 for normal, lower values = less recoil
     */
    public static float getRecoilMultiplier(Player player) {
        if (isInFocus(player)) {
            return 0.6f; // 40% recoil reduction
        }
        return 1.0f;
    }
    
    /**
     * Get the damage resistance multiplier for a focused player
     * Returns 1.0 for normal, lower values = less damage taken
     */
    public static float getDamageResistanceMultiplier(Player player) {
        if (isInFocus(player)) {
            return 0.85f; // 15% damage reduction
        }
        return 1.0f;
    }
}
