package com.raeyncraft.matrixcraft.mixin;

import com.raeyncraft.matrixcraft.command.MatrixSettings;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.CobwebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to disable cobweb slowdown when MatrixSettings.cobwebsEnabled is false
 */
@Mixin(CobwebBlock.class)
public class CobwebBlockMixin {
    
    /**
     * Cancel the entityInside method when cobwebs are disabled
     */
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (!MatrixSettings.areCobwebsEnabled()) {
            // Cancel the slowdown effect
            ci.cancel();
        }
    }
}
