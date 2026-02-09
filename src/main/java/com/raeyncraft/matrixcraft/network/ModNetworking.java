package com.raeyncraft.matrixcraft.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = "matrixcraft", bus = EventBusSubscriber.Bus.MOD)
public class ModNetworking {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("matrixcraft");
        registrar.playToClient(SilentHillEffectPacket.TYPE, SilentHillEffectPacket.STREAM_CODEC, SilentHillEffectPacket::handle);
    }
}