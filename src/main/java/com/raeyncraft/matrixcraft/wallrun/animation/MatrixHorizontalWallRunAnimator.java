package com.raeyncraft.matrixcraft.wallrun.animation;

import com.raeyncraft.matrixcraft.wallrun.MatrixWallRunManager;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

/**
 * Animates the player model during horizontal wall running.
 * Adapted from ParCool's HorizontalWallRunAnimator.
 */
public class MatrixHorizontalWallRunAnimator {
    
    private int ticksActive = 0;
    private float limbSwing = 0;
    private final boolean wallIsRightSide;
    
    public MatrixHorizontalWallRunAnimator(boolean wallIsRightSide) {
        this.wallIsRightSide = wallIsRightSide;
    }
    
    public void tick() {
        ticksActive++;
    }
    
    private float getFactor(float tick) {
        return tick < 5 ? 1 - (float)Math.pow((5 - tick) / 5, 2) : 1;
    }
    
    public void animate(PlayerModel<?> model, AbstractClientPlayer player, float partialTick) {
        limbSwing = model.attackTime; // Use existing animation time
        float factor = getFactor(ticksActive + partialTick);
        float angle = factor * 15f * (wallIsRightSide ? -1f : 1f);
        float armSwingPhase = limbSwing * 0.6662f;
        
        // Head rotation
        model.head.xRot += Math.toRadians(-15 * factor);
        model.head.zRot += Math.toRadians(angle);
        
        if (wallIsRightSide) {
            // Right side wall run
            model.leftArm.zRot += Math.toRadians(-30);
            model.rightArm.xRot = (float) Math.toRadians(20 - 8d * Math.cos(armSwingPhase));
            model.rightArm.zRot = (float) Math.toRadians(110);
            
            model.leftArm.xRot += (float) Math.toRadians(-10);
            model.leftArm.zRot += (float) -Math.toRadians(35 + 5 * Math.sin(armSwingPhase)) * factor;
            
            model.rightLeg.zRot += (float) Math.toRadians(17);
            model.leftLeg.zRot += (float) Math.toRadians(25);
            model.head.yRot += Math.toRadians(-5f + 8f * Mth.cos(armSwingPhase));
        } else {
            // Left side wall run
            model.rightArm.zRot += Math.toRadians(30);
            model.leftArm.xRot = (float) Math.toRadians(20 - 8d * Math.cos(armSwingPhase));
            model.leftArm.zRot = (float) Math.toRadians(-110);
            
            model.rightArm.xRot += (float) Math.toRadians(-10);
            model.rightArm.zRot += (float) Math.toRadians(35 + 5 * Math.sin(armSwingPhase)) * factor;
            
            model.rightLeg.zRot += (float) Math.toRadians(-25);
            model.leftLeg.zRot += (float) Math.toRadians(-17);
            model.head.yRot += Math.toRadians(5f - 8f * Mth.cos(armSwingPhase));
        }
        
        // Body roll
        float sign = wallIsRightSide ? -1 : 1;
        float bodyAngle = factor * 30f * sign;
        float yOffset = 0.145f * (float) Math.pow(Math.cos(limbSwing * 0.6662), 2.);
        
        // Apply body rotation (this would need custom rendering hooks)
        model.body.zRot += Math.toRadians(bodyAngle);
        model.body.xRot += Math.toRadians(20 * factor);
        model.body.yRot += Math.toRadians(sign * (-5f + 8f * Mth.cos(limbSwing * 0.66662f)));
    }
    
    public boolean shouldRemove(AbstractClientPlayer player) {
        return !MatrixWallRunManager.isWallRunning(player);
    }
}