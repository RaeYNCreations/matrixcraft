package com.raeyncraft.matrixcraft.wallrun;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.bullettime.FocusManager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Event handler for Matrix-style wall running mechanics
 */
@EventBusSubscriber(modid = MatrixCraftMod.MODID)
public class MatrixWallRunEventHandler {
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        
        // Only process on server side
        if (player.level().isClientSide) {
            return;
        }
        
        // Check if player is in Focus mode
        boolean inFocus = FocusManager.isInFocus(player);
        
        if (!inFocus) {
            // Stop wall running if focus mode ended
            if (MatrixWallRunManager.isWallRunning(player)) {
                MatrixWallRunManager.stopWallRun(player);
            }
            return;
        }
        
        // If already wall running, update it
        if (MatrixWallRunManager.isWallRunning(player)) {
            MatrixWallRunManager.updateWallRun(player);
        } else {
            // Try to start wall run every tick when:
            // - Player is airborne (not on ground)
            // - Player is moving horizontally
            if (!player.onGround() && player.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
                MatrixWallRunManager.tryStartWallRun(player);
            }
        }
    }
}