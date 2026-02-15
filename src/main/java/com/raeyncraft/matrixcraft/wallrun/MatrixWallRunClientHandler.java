package com.raeyncraft.matrixcraft.wallrun.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.network.WallRunJumpPacket;
import com.raeyncraft.matrixcraft.wallrun.MatrixWallRunManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = MatrixCraftMod.MODID, value = Dist.CLIENT)
public class MatrixWallRunClientHandler {
    
    private static final Set<UUID> activePoses = new HashSet<>();
    // Thread-safe boolean for jump key state tracking
    private static volatile boolean wasJumpKeyDown = false;
    
    /**
     * Client-side tick to detect jump key press during wallrun
     * This handles the player's ability to jump off the wall at any time
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        
        // Check if player is wallrunning
        if (!MatrixWallRunManager.isWallRunning(mc.player)) {
            wasJumpKeyDown = false;
            return;
        }
        
        // Check if jump key is pressed (client-side detection)
        boolean isJumpKeyDown = mc.options.keyJump.isDown();
        
        // Detect rising edge (key just pressed, not held)
        if (isJumpKeyDown && !wasJumpKeyDown) {
            // Player just pressed jump while wallrunning
            MatrixCraftMod.LOGGER.info("[WallRunClient] Jump key pressed, sending jump request to server");
            
            // Send packet to server to execute the jump
            // The server is authoritative for wallrun state in multiplayer
            PacketDistributor.sendToServer(new WallRunJumpPacket());
            
            // Also execute locally for immediate client-side response (works in single-player)
            // In multiplayer, the server will sync the actual state back
            MatrixWallRunManager.jumpOffWall(mc.player);
        }
        
        wasJumpKeyDown = isJumpKeyDown;
    }
    
    @SubscribeEvent
    public static void onRenderPre(RenderLivingEvent.Pre<?, ?> event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        UUID playerId = player.getUUID();
        activePoses.remove(playerId);
        
        if (!MatrixWallRunManager.isWallRunning(player)) {
            return;
        }
        
        MatrixWallRunManager.WallRunState state = MatrixWallRunManager.getWallRunState(player);
        if (state == null) {
            return;
        }
        
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        activePoses.add(playerId);
        
        float ticks = state.ticksActive + event.getPartialTick();
        float playerHeight = 1.8f;
        
        if (state.type == MatrixWallRunManager.WallRunType.HORIZONTAL) {
            // HORIZONTAL: Lean toward wall
            float factor = Math.min(1.0f, ticks / 6.0f);
            factor = easeOutQuad(factor);
            
            Vec3 runDir = state.runDirection;
            
            float maxLean = 75.0f;
            float leanAngle = maxLean * factor;
            
            if (!state.wallIsOnRight) {
                leanAngle = -leanAngle;
            }
            
            float leanRad = (float) Math.toRadians(leanAngle);
            Vector3f axis = new Vector3f((float) runDir.x, 0, (float) runDir.z).normalize();
            
            poseStack.translate(0, playerHeight / 2.0, 0);
            Quaternionf rotation = new Quaternionf(new AxisAngle4f(leanRad, axis));
            poseStack.mulPose(rotation);
            poseStack.translate(0, -playerHeight / 2.0, 0);
            
        } else {
            // VERTICAL: Lean AWAY from wall (back faces wall, front faces outward)
            float factor = Math.min(1.0f, ticks / 5.0f);
            factor = easeOutBack(factor);
            
            Vec3 wallNormal = state.wallNormal;
            
            // Axis is perpendicular to wall normal, horizontal (along wall surface)
            Vector3f pitchAxis = new Vector3f((float) -wallNormal.z, 0, (float) wallNormal.x).normalize();
            
            // NEGATED: Lean away from wall instead of into it
            float maxPitch = 65.0f;
            float pitchAngle = -maxPitch * factor; // <-- NEGATED HERE
            float pitchRad = (float) Math.toRadians(pitchAngle);
            
            poseStack.translate(0, playerHeight / 2.0, 0);
            
            Quaternionf pitchRotation = new Quaternionf(new AxisAngle4f(pitchRad, pitchAxis));
            poseStack.mulPose(pitchRotation);
            
            poseStack.translate(0, -playerHeight / 2.0, 0);
        }
    }
    
    @SubscribeEvent
    public static void onRenderPost(RenderLivingEvent.Post<?, ?> event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        if (activePoses.remove(player.getUUID())) {
            event.getPoseStack().popPose();
        }
    }
    
    private static float easeOutQuad(float x) {
        return 1.0f - (1.0f - x) * (1.0f - x);
    }
    
    private static float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
    }
}