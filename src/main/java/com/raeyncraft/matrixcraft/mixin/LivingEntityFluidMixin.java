package com.raeyncraft.matrixcraft.mixin;

import com.raeyncraft.matrixcraft.bullettime.FocusManager;
import com.raeyncraft.matrixcraft.command.MatrixSettings;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityFluidMixin {
    
    @Inject(method = "travel", at = @At("TAIL"))
    private void afterTravel(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (!(entity instanceof Player player)) return;
        
        boolean inFocus = FocusManager.isInFocus(player);
        boolean lavaBypassActive = !MatrixSettings.isLavaEnabled() || 
                                   (inFocus && com.raeyncraft.matrixcraft.bullettime.FocusModeEffects.isFocusLavaBypassEnabled());
        
        if (lavaBypassActive && player.isInLava()) {
            // Only boost if player is actually trying to move (travelVector has input)
            if (travelVector.lengthSqr() > 0.0001) {
                Vec3 motion = player.getDeltaMovement();
                player.setDeltaMovement(motion.x * 2.0, motion.y * 1.5, motion.z * 2.0);
            }
        }
    }
}