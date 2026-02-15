package com.raeyncraft.matrixcraft.wallrun;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.bullettime.FocusManager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = MatrixCraftMod.MODID)
public class MatrixWallRunEventHandler {
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        
        // Separate client and server logic completely
        if (player.level().isClientSide) {
            // Client-side: Only handle rendering and animations
            if (MatrixWallRunManager.isWallRunning(player)) {
                MatrixWallRunManager.clientTick(player);
            }
            return; // Don't run server logic on client
        }
        
        // Server-side only from here on
        // Check Focus first - if not in focus, stop any wallrun immediately
        boolean inFocus = FocusManager.isInFocus(player);
        
        if (!inFocus) {
            if (MatrixWallRunManager.isWallRunning(player)) {
                MatrixWallRunManager.stopWallRun(player);
            }
            return;
        }
        
        // Player is in focus - allow wallrun mechanics
        if (MatrixWallRunManager.isWallRunning(player)) {
            // Jump detection is now handled inside updateWallRun via velocity changes
            MatrixWallRunManager.updateWallRun(player);
        } else if (!player.onGround() && player.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
            MatrixWallRunManager.tryStartWallRun(player);
        }
    }
}