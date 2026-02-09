package com.raeyncraft.matrixcraft.network;

import com.raeyncraft.matrixcraft.client.SilentHillEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SilentHillEffectPacket(boolean enable) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SilentHillEffectPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("matrixcraft", "silent_hill_effect"));

    public static final StreamCodec<FriendlyByteBuf, SilentHillEffectPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, SilentHillEffectPacket::enable,
        SilentHillEffectPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SilentHillEffectPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.enable) {
                SilentHillEffects.apply();
            } else {
                SilentHillEffects.remove();
            }
        });
    }
}