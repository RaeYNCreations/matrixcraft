package com.raeyncraft.matrixcraft.bullettime.client;

import com.raeyncraft.matrixcraft.MatrixCraftConfig;
import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.bullettime.FocusManager;
import com.raeyncraft.matrixcraft.bullettime.registry.BulletTimeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

/**
 * Handles all client-side visual and audio effects for Focus mode.
 */
@EventBusSubscriber(modid = MatrixCraftMod.MODID, value = Dist.CLIENT)
public class FocusClientEffects {
    
    // Visual settings
    private static final float TARGET_FOV_MODIFIER = 0.75f; // Zoom in by 25%
    private static final float FOV_TRANSITION_SPEED = 0.1f;
    
    // State tracking
    private static float currentFovModifier = 1.0f;
    private static boolean wasInFocus = false;
    private static int focusTransitionTicks = 0;
    
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        
        // Check for Matrix Focus effect on the player
        boolean inFocus = hasMatrixFocusEffect(mc.player);
        
        // Sync client state based on mob effect
        if (inFocus) {
            MobEffectInstance effect = mc.player.getEffect(BulletTimeRegistry.MATRIX_FOCUS_EFFECT);
            if (effect != null) {
                // Use configured duration, not hardcoded
                int maxDuration = MatrixCraftConfig.getFocusDurationTicks();
                FocusManager.clientSetFocusState(true, effect.getDuration(), maxDuration);
            }
        } else {
            FocusManager.clientSetFocusState(false, 0, 0);
        }
        
        // Handle transition effects
        if (inFocus && !wasInFocus) {
            onFocusActivated(mc);
        } else if (!inFocus && wasInFocus) {
            onFocusDeactivated(mc);
        }
        
        wasInFocus = inFocus;
        
        // Update transition
        if (inFocus) {
            focusTransitionTicks = Math.min(focusTransitionTicks + 1, 20);
        } else {
            focusTransitionTicks = Math.max(focusTransitionTicks - 2, 0);
        }
        
        // Smooth FOV transition
        float targetFov = inFocus ? TARGET_FOV_MODIFIER : 1.0f;
        currentFovModifier = Mth.lerp(FOV_TRANSITION_SPEED, currentFovModifier, targetFov);
        
        // Update FocusManager client tick
        FocusManager.clientTick();
    }
    
    /**
     * Called when Focus mode activates
     */
    private static void onFocusActivated(Minecraft mc) {
        MatrixCraftMod.LOGGER.info("[MatrixFocus] Client: Focus activated!");
        
        // Play activation sound
        if (mc.player != null) {
            mc.level.playLocalSound(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS,
                0.5f, 0.5f, false
            );
        }
    }
    
    /**
     * Called when Focus mode deactivates
     */
    private static void onFocusDeactivated(Minecraft mc) {
        MatrixCraftMod.LOGGER.info("[MatrixFocus] Client: Focus deactivated!");
        
        // Play deactivation sound
        if (mc.player != null) {
            mc.level.playLocalSound(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS,
                0.5f, 1.5f, false
            );
        }
    }
    
    /**
     * Modify FOV for zoom effect
     */
    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        if (focusTransitionTicks > 0) {
            float transition = focusTransitionTicks / 20.0f;
            float fovMod = Mth.lerp(transition, 1.0f, TARGET_FOV_MODIFIER);
            event.setNewFovModifier(event.getNewFovModifier() * fovMod);
        }
    }
    
    /**
     * Check if the player has the Matrix Focus effect
     */
    private static boolean hasMatrixFocusEffect(LocalPlayer player) {
        try {
            return player.hasEffect(BulletTimeRegistry.MATRIX_FOCUS_EFFECT);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the current transition progress (0.0 to 1.0)
     */
    public static float getTransitionProgress() {
        return focusTransitionTicks / 20.0f;
    }
    
    /**
     * Check if currently transitioning
     */
    public static boolean isTransitioning() {
        return focusTransitionTicks > 0 && focusTransitionTicks < 20;
    }
}
