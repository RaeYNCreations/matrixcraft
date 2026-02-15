package com.raeyncraft.matrixcraft.bullettime;

import com.raeyncraft.matrixcraft.MatrixCraftConfig;
import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.bullettime.effect.MatrixFocusEffect;
import com.raeyncraft.matrixcraft.bullettime.registry.BulletTimeRegistry;
import com.raeyncraft.matrixcraft.wallrun.MatrixWallRunManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
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
            int duration = MatrixCraftConfig.getFocusDurationTicks();
            // Validate duration is positive
            if (duration <= 0) {
                MatrixCraftMod.LOGGER.warn("[FocusManager] Invalid duration " + duration + ", using default");
                return FOCUS_DURATION_TICKS;
            }
            return duration;
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
            this.durationTicks = Math.max(1, durationTicks); // Ensure at least 1 tick
            this.ticksRemaining = this.durationTicks;
            this.isSinglePlayer = isSinglePlayer;
        }
        
        public float getProgress() {
            // Prevent division by zero
            if (durationTicks <= 0) return 0.0f;
            return (float) ticksRemaining / (float) durationTicks;
        }
    }
    
    /**
     * Activate Focus mode for a player (called from server)
     * Also activates TACZ Adrenaline Mode if available
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
        
        // Activate TACZ Adrenaline Mode if available
        tryActivateTaczAdrenaline(player, duration);
    }
    
    /**
     * Deactivate Focus mode for a player
     * Also deactivates TACZ Adrenaline Mode if it was activated by focus
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

        // STOP WALL RUNNING
        MatrixWallRunManager.stopWallRun(player);
        
        // Deactivate TACZ Adrenaline Mode if available
        tryDeactivateTaczAdrenaline(player);
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
        // Create a snapshot to avoid ConcurrentModificationException
        // if deactivateFocus() is called during iteration
        var entries = new ArrayList<>(activeFocusStates.entrySet());
        
        for (var entry : entries) {
            UUID playerId = entry.getKey();
            FocusState state = entry.getValue();
            
            state.ticksRemaining--;
            
            if (state.ticksRemaining <= 0) {
                activeFocusStates.remove(playerId);
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
    
    // ==================== TACZ ADRENALINE MODE INTEGRATION ====================
    
    // Cache TACZ availability to avoid repeated reflection lookups
    // Volatile ensures visibility across threads during lazy initialization
    private static volatile Boolean taczAvailable = null;
    
    /**
     * Check if TACZ is available (cached result)
     */
    private static boolean isTaczAvailable() {
        if (taczAvailable == null) {
            try {
                Class.forName("com.tacz.guns.adrenaline.AdrenalineManager");
                taczAvailable = true;
                MatrixCraftMod.LOGGER.info("[FocusManager] TACZ detected - adrenaline integration enabled");
            } catch (ClassNotFoundException e) {
                taczAvailable = false;
                MatrixCraftMod.LOGGER.debug("[FocusManager] TACZ not found - adrenaline integration disabled");
            }
        }
        return taczAvailable;
    }
    
    /**
     * Try to activate TACZ Adrenaline Mode using reflection
     * This allows integration without requiring TACZ as a dependency
     */
    private static void tryActivateTaczAdrenaline(ServerPlayer player, int durationTicks) {
        if (!isTaczAvailable()) {
            return;
        }
        
        try {
            // Access AdrenalineManager and related classes
            Class<?> adrenalineManagerClass = Class.forName("com.tacz.guns.adrenaline.AdrenalineManager");
            Class<?> playerDataClass = Class.forName("com.tacz.guns.adrenaline.AdrenalineManager$PlayerAdrenalineData");
            
            // Get the playerData map
            java.lang.reflect.Field playerDataField = adrenalineManagerClass.getDeclaredField("playerData");
            playerDataField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<UUID, Object> playerDataMap = (java.util.Map<UUID, Object>) playerDataField.get(null);
            
            // Get or create player data
            UUID playerId = player.getUUID();
            Object playerData = playerDataMap.computeIfAbsent(playerId, k -> {
                try {
                    return playerDataClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    MatrixCraftMod.LOGGER.warn("[FocusManager] Failed to create TACZ PlayerAdrenalineData: " + e.getMessage());
                    return null;
                }
            });
            
            if (playerData == null) {
                return;
            }
            
            // Call activate method with matching duration
            // Duration is in ticks, need to convert to seconds for TACZ
            long currentTime = System.currentTimeMillis();
            double currentMaxHealth = player.getMaxHealth();
            int durationSeconds = durationTicks / 20; // Convert ticks to seconds
            
            java.lang.reflect.Method activateMethod = playerDataClass.getDeclaredMethod("activate", long.class, double.class);
            activateMethod.setAccessible(true);
            activateMethod.invoke(playerData, currentTime, currentMaxHealth);
            
            // Override the active end time to match focus duration
            java.lang.reflect.Field activeEndTimeField = playerDataClass.getDeclaredField("activeEndTime");
            activeEndTimeField.setAccessible(true);
            activeEndTimeField.set(playerData, currentTime + (durationSeconds * 1000L));
            
            // Apply health modifier
            java.lang.reflect.Method applyHealthMethod = adrenalineManagerClass.getDeclaredMethod("applyHealthModifier", ServerPlayer.class, double.class);
            applyHealthMethod.setAccessible(true);
            
            // Get health multiplier from config (default 1.5)
            Class<?> configClass = Class.forName("com.tacz.guns.config.common.AdrenalineConfig");
            java.lang.reflect.Field healthMultiplierField = configClass.getDeclaredField("HEALTH_MULTIPLIER");
            healthMultiplierField.setAccessible(true);
            Object configValue = healthMultiplierField.get(null);
            java.lang.reflect.Method getMethod = configValue.getClass().getMethod("get");
            double healthMultiplier = (double) getMethod.invoke(configValue);
            
            applyHealthMethod.invoke(null, player, healthMultiplier);
            
            MatrixCraftMod.LOGGER.info("[FocusManager] Activated TACZ Adrenaline Mode for " + player.getName().getString() + 
                " (Duration: " + durationSeconds + "s, Health: " + healthMultiplier + "x)");
            
        } catch (Exception e) {
            MatrixCraftMod.LOGGER.warn("[FocusManager] Failed to activate TACZ adrenaline mode: " + e.getMessage());
            MatrixCraftMod.LOGGER.debug("[FocusManager] TACZ integration error details", e);
        }
    }
    
    /**
     * Try to deactivate TACZ Adrenaline Mode using reflection
     */
    private static void tryDeactivateTaczAdrenaline(ServerPlayer player) {
        if (!isTaczAvailable()) {
            return;
        }
        
        try {
            Class<?> adrenalineManagerClass = Class.forName("com.tacz.guns.adrenaline.AdrenalineManager");
            Class<?> playerDataClass = Class.forName("com.tacz.guns.adrenaline.AdrenalineManager$PlayerAdrenalineData");
            
            // Get the playerData map
            java.lang.reflect.Field playerDataField = adrenalineManagerClass.getDeclaredField("playerData");
            playerDataField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<UUID, Object> playerDataMap = (java.util.Map<UUID, Object>) playerDataField.get(null);
            
            UUID playerId = player.getUUID();
            Object playerData = playerDataMap.get(playerId);
            
            if (playerData == null) {
                return;
            }
            
            // Check if currently active
            java.lang.reflect.Method isActiveMethod = playerDataClass.getDeclaredMethod("isActive");
            isActiveMethod.setAccessible(true);
            boolean isActive = (boolean) isActiveMethod.invoke(playerData);
            
            if (!isActive) {
                return;
            }
            
            // Remove health modifier
            java.lang.reflect.Method removeHealthMethod = adrenalineManagerClass.getDeclaredMethod("removeHealthModifier", ServerPlayer.class);
            removeHealthMethod.setAccessible(true);
            removeHealthMethod.invoke(null, player);
            
            // Deactivate with matching cooldown
            // Get cooldown duration from config
            Class<?> configClass = Class.forName("com.tacz.guns.config.common.AdrenalineConfig");
            java.lang.reflect.Field cooldownField = configClass.getDeclaredField("COOLDOWN_DURATION");
            cooldownField.setAccessible(true);
            Object configValue = cooldownField.get(null);
            java.lang.reflect.Method getMethod = configValue.getClass().getMethod("get");
            int cooldownSeconds = (int) getMethod.invoke(configValue);
            
            // Match cooldown to focus duration for consistency
            int focusDurationSeconds = getFocusDuration() / 20;
            long currentTime = System.currentTimeMillis();
            
            // Call deactivate
            java.lang.reflect.Method deactivateMethod = playerDataClass.getDeclaredMethod("deactivate", long.class);
            deactivateMethod.setAccessible(true);
            deactivateMethod.invoke(playerData, currentTime);
            
            // Override cooldown to match focus cooldown
            java.lang.reflect.Field cooldownEndTimeField = playerDataClass.getDeclaredField("cooldownEndTime");
            cooldownEndTimeField.setAccessible(true);
            cooldownEndTimeField.set(playerData, currentTime + (focusDurationSeconds * 1000L));
            
            MatrixCraftMod.LOGGER.info("[FocusManager] Deactivated TACZ Adrenaline Mode for " + player.getName().getString() + 
                " (Cooldown: " + focusDurationSeconds + "s)");
            
        } catch (Exception e) {
            MatrixCraftMod.LOGGER.warn("[FocusManager] Failed to deactivate TACZ adrenaline mode: " + e.getMessage());
            MatrixCraftMod.LOGGER.debug("[FocusManager] TACZ integration error details", e);
        }
    }
}
