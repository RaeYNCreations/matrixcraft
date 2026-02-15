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
        
        if (player.level().isClientSide) {
            if (MatrixWallRunManager.isWallRunning(player)) {
                MatrixWallRunManager.clientTick(player);
            }
            return;
        }
        
        boolean inFocus = FocusManager.isInFocus(player);
        
        if (!inFocus) {
            if (MatrixWallRunManager.isWallRunning(player)) {
                MatrixWallRunManager.stopWallRun(player);
            }
            return;
        }
        
        if (MatrixWallRunManager.isWallRunning(player)) {
            MatrixWallRunManager.updateWallRun(player);
        } else if (!player.onGround() && player.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
            MatrixWallRunManager.tryStartWallRun(player);
        }
    }
}