package com.raeyncraft.matrixcraft.network;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Network handler for MatrixCraft packets
 */
@EventBusSubscriber(modid = MatrixCraftMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
    
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MatrixCraftMod.MODID);
        
        // Register wallrun jump packet (client -> server)
        registrar.playToServer(
            WallRunJumpPacket.TYPE,
            WallRunJumpPacket.STREAM_CODEC,
            WallRunJumpPacket::handle
        );
        
        MatrixCraftMod.LOGGER.info("[NetworkHandler] Registered network packets");
    }
}
