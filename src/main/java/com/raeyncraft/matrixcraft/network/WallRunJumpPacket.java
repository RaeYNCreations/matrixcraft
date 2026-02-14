package com.raeyncraft.matrixcraft.network;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.wallrun.MatrixWallRunManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet sent from client to server when player presses jump during wallrun
 */
public record WallRunJumpPacket() implements CustomPacketPayload {
    
    public static final Type<WallRunJumpPacket> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath(MatrixCraftMod.MODID, "wallrun_jump"));
    
    public static final StreamCodec<ByteBuf, WallRunJumpPacket> STREAM_CODEC = 
        StreamCodec.unit(new WallRunJumpPacket());
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    /**
     * Handle the packet on the server side
     */
    public static void handle(WallRunJumpPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                // Server-side: Execute the jump off wall
                MatrixCraftMod.LOGGER.debug("[WallRunJumpPacket] Server received jump request from " + 
                    serverPlayer.getName().getString());
                MatrixWallRunManager.jumpOffWall(serverPlayer);
            }
        });
    }
}
