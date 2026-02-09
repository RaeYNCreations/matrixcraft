package com.raeyncraft.matrixcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SilentHillEffects {
    
    private static boolean active = false;
    private static double originalGamma = 0.5;
    
    /**
     * Apply Silent Hill visual/audio effects
     */
    public static void apply() {
        if (active) return;
        
        Minecraft mc = Minecraft.getInstance();
        
        // Store original gamma
        originalGamma = mc.options.gamma().get();
        
        // Darken the world
        mc.options.gamma().set(0.0);
        
        // Reduce render distance for fog effect
        int originalRenderDistance = mc.options.renderDistance().get();
        mc.options.renderDistance().set(Math.min(originalRenderDistance, 12));
        
        // Play ambient siren sound (looping)
        if (mc.player != null) {
            mc.level.playSound(
                mc.player,
                mc.player.blockPosition(),
                SoundEvents.WARDEN_AMBIENT,
                SoundSource.AMBIENT,
                0.3f,
                0.5f
            );
        }
        
        active = true;
    }
    
    /**
     * Remove Silent Hill effects
     */
    public static void remove() {
        if (!active) return;
        
        Minecraft mc = Minecraft.getInstance();
        
        // Restore original gamma
        mc.options.gamma().set(originalGamma);
        
        // Stop ambient sounds (handled by texture pack disable)
        
        active = false;
    }
    
    public static boolean isActive() {
        return active;
    }
}