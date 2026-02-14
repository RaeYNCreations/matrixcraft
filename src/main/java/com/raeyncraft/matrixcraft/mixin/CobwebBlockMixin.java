package com.raeyncraft.matrixcraft.mixin;

import com.raeyncraft.matrixcraft.bullettime.FocusManager;
import com.raeyncraft.matrixcraft.bullettime.FocusModeEffects;
import com.raeyncraft.matrixcraft.command.MatrixSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to disable cobweb slowdown when MatrixSettings.cobwebsEnabled is false
 * or when a player is in Focus mode with cobweb bypass enabled
 * Note: In Minecraft 1.21+, CobwebBlock was renamed to WebBlock
 */
@Mixin(WebBlock.class)
public class CobwebBlockMixin {
    
    /**
     * Cancel the entityInside method when cobwebs are disabled
     * or when player is in Focus mode with bypass enabled
     */
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        // Check global cobweb setting
        if (!MatrixSettings.areCobwebsEnabled()) {
            ci.cancel();
            return;
        }
        
        // Check per-player Focus mode bypass
        if (entity instanceof Player player) {
            if (FocusManager.isInFocus(player) && FocusModeEffects.isFocusCobwebBypassEnabled()) {
                ci.cancel();
            }
        }
    }
}
