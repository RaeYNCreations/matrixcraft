package com.raeyncraft.matrixcraft.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.raeyncraft.matrixcraft.bullettime.FocusManager;
import com.raeyncraft.matrixcraft.command.MatrixSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class FireOverlayMixin {
    
    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private static void cancelFireOverlay(Minecraft minecraft, PoseStack poseStack, CallbackInfo ci) {
        Player player = minecraft.player;
        if (player == null) return;
        
        boolean inFocus = FocusManager.isInFocus(player);
        boolean lavaBypassActive = !MatrixSettings.isLavaEnabled() || 
                                   (inFocus && com.raeyncraft.matrixcraft.bullettime.FocusModeEffects.isFocusLavaBypassEnabled());
        
        if (lavaBypassActive) {
            // Cancel fire overlay rendering
            ci.cancel();
        }
    }
}