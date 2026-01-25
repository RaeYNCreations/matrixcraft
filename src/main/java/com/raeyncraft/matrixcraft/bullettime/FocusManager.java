package com.raeyncraft.matrixcraft.bullettime;

import com.raeyncraft.matrixcraft.MatrixCraftConfig;
import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.bullettime.effect.MatrixFocusEffect;
import com.raeyncraft.matrixcraft.bullettime.registry.BulletTimeRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for the Focus (bullet time) system.
 * Handles both single-player time dilation and multiplayer buff system.
 * Duration is configurable via /matrix bullettime duration
 */
public class FocusManager {
    
    // Default duration in ticks (10 seconds = 200 ticks) - now configurable
    public static final int FOCUS_DURATION_TICKS = 200;
    
    // Track active focus states (server-side)
    private static final Map<UUID, FocusState> activeFocusStates = new ConcurrentHashMap<>();
    
    /**
     * Get the configured focus duration in ticks
     */
    public static int getFocusDuration() {
        try {
            return MatrixCraftConfig.getFocusDurationTicks();
        } catch (Exception e) {
            // Config not loaded yet, use default
            return FOCUS_DURATION_TICKS;
        }
    }
    
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
        
        // Get configured duration
        int duration = getFocusDuration();
        
        MatrixCraftMod.LOGGER.info("[MatrixFocus] Activating focus for " + player.getName().getString() + 
            " (SinglePlayer: " + isSinglePlayer + ", Duration: " + (duration / 20) + "s)");
        
        // Create focus state
        FocusState state = new FocusState(duration, isSinglePlayer);
        activeFocusStates.put(playerId, state);
        
        // Apply mob effect (for multiplayer buffs)
        MobEffectInstance effect = new MobEffectInstance(
            BulletTimeRegistry.MATRIX_FOCUS_EFFECT,
            duration,
            0, // Amplifier
            false, // Ambient
            true, // Visible particles
            true  // Show icon
        );
        player.addEffect(effect);
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
    }
    
    /**
     * Check if a player is currently in Focus mode (server-side check)
     */
    public static boolean isInFocus(Player player) {
        if (player.level().isClientSide) {
            // On client, delegate to client state holder
            return FocusClientState.isClientInFocus();
        }
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
    
    // ==================== CLIENT STATE ACCESS (safe for server) ====================
    
    /**
     * Set client focus state - only call from client code
     */
    public static void clientSetFocusState(boolean active, int ticksRemaining, int maxTicks) {
        FocusClientState.setFocusState(active, ticksRemaining, maxTicks);
    }
    
    /**
     * Client tick - only call from client code
     */
    public static void clientTick() {
        FocusClientState.tick();
    }
    
    /**
     * Check if client is in focus
     */
    public static boolean isClientInFocus() {
        return FocusClientState.isClientInFocus();
    }
    
    /**
     * Get client focus progress (0.0 to 1.0)
     */
    public static float getClientFocusProgress() {
        return FocusClientState.getProgress();
    }
    
    /**
     * Get client focus ticks remaining
     */
    public static int getClientFocusTicksRemaining() {
        return FocusClientState.getTicksRemaining();
    }
}
