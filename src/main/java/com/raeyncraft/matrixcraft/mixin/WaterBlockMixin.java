package com.raeyncraft.matrixcraft.mixin;

import com.raeyncraft.matrixcraft.command.MatrixSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to disable water slowdown when MatrixSettings.waterEnabled is false
 * This allows players to move through water like air when water bypass is enabled
 * Only affects water blocks, not lava or other fluids
 */
@Mixin(LiquidBlock.class)
public class WaterBlockMixin {
    
    /**
     * Cancel the entityInside method when water is disabled
     * This prevents water from slowing the player down
     * Only affects water blocks, not lava
     */
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        // Only cancel if water is disabled AND this is actually a water block (not lava)
        if (!MatrixSettings.isWaterEnabled()) {
            LiquidBlock liquidBlock = (LiquidBlock) (Object) this;
            if (liquidBlock.getFluid() == Fluids.WATER || liquidBlock.getFluid() == Fluids.FLOWING_WATER) {
                // Cancel the slowdown effect for water only
                ci.cancel();
            }
        }
    }
}
