package com.raeyncraft.matrixcraft.wallrun;

import com.raeyncraft.matrixcraft.MatrixCraftConfig;
import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.bullettime.FocusManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MatrixWallRunManager {
    
    private static final Map<UUID, WallRunState> activeWallRuns = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    
    private static final double MIN_SPEED = 0.1;
    private static final int MAX_HORIZONTAL_TICKS = 120;
    private static final int MAX_VERTICAL_TICKS = 100;
    private static final long COOLDOWN_MS = 500;
    
    // ========== Config Getters ==========
    
    /**
     * Checks if horizontal wall running is enabled in config
     * @return True if players can run parallel along walls
     */
    public static boolean isHorizontalEnabled() {
        return MatrixCraftConfig.WALLRUN_HORIZONTAL_ENABLED.get();
    }
    
    /**
     * Checks if vertical wall running is enabled in config
     * @return True if players can climb straight up walls
     */
    public static boolean isVerticalEnabled() {
        return MatrixCraftConfig.WALLRUN_VERTICAL_ENABLED.get();
    }
    
    /**
     * Gets the maximum distance a player can run horizontally along a wall
     * @return Maximum horizontal wall run distance in blocks (default: 32.0)
     */
    public static double getHorizontalMaxDistance() {
        return MatrixCraftConfig.WALLRUN_HORIZONTAL_MAX_DISTANCE.get();
    }
    
    /**
     * Gets the maximum distance a player can climb vertically up a wall
     * @return Maximum vertical wall climb distance in blocks (default: 4.5)
     */
    public static double getVerticalMaxDistance() {
        return MatrixCraftConfig.WALLRUN_VERTICAL_MAX_DISTANCE.get();
    }
    
    /**
     * Gets the minimum angle from parallel for horizontal wall running
     * @return Minimum angle in degrees (default: 30.0)
     */
    public static double getHorizontalAngleMin() {
        return MatrixCraftConfig.WALLRUN_HORIZONTAL_ANGLE_MIN.get();
    }
    
    /**
     * Gets the maximum angle from parallel for horizontal wall running
     * @return Maximum angle in degrees (default: 60.0)
     */
    public static double getHorizontalAngleMax() {
        return MatrixCraftConfig.WALLRUN_HORIZONTAL_ANGLE_MAX.get();
    }
    
    /**
     * Gets the minimum angle from perpendicular for vertical wall running
     * @return Minimum angle in degrees (default: 0.0)
     */
    public static double getVerticalAngleMin() {
        return MatrixCraftConfig.WALLRUN_VERTICAL_ANGLE_MIN.get();
    }
    
    /**
     * Gets the maximum angle from perpendicular for vertical wall running
     * @return Maximum angle in degrees (default: 25.0)
     */
    public static double getVerticalAngleMax() {
        return MatrixCraftConfig.WALLRUN_VERTICAL_ANGLE_MAX.get();
    }
    
    public static class WallRunState {
        public final WallRunType type;
        public final Direction wallDirection;
        public final Vec3 wallNormal;
        public final Vec3 startPos;
        public final Vec3 runDirection;
        public final boolean wallIsOnRight;
        public final float playerYaw;
        public final double maxDistance;
        public int ticksActive;
        
        public WallRunState(WallRunType type, Direction wallDirection, Vec3 wallNormal, 
                           Vec3 startPos, Vec3 runDirection, boolean wallIsOnRight, float playerYaw) {
            this.type = type;
            this.wallDirection = wallDirection;
            this.wallNormal = wallNormal;
            this.startPos = startPos;
            this.runDirection = runDirection;
            this.wallIsOnRight = wallIsOnRight;
            this.playerYaw = playerYaw;
            this.ticksActive = 0;
            this.maxDistance = type == WallRunType.HORIZONTAL ? getHorizontalMaxDistance() : getVerticalMaxDistance();
        }
        
        public double getDistanceTraveled(Vec3 currentPos) {
            if (type == WallRunType.HORIZONTAL) {
                double dx = currentPos.x - startPos.x;
                double dz = currentPos.z - startPos.z;
                return Math.sqrt(dx * dx + dz * dz);
            } else {
                return currentPos.y - startPos.y;
            }
        }
        
        public int getMaxTicks() {
            return type == WallRunType.HORIZONTAL ? MAX_HORIZONTAL_TICKS : MAX_VERTICAL_TICKS;
        }
        
        public double getMaxDistance() {
            return maxDistance;
        }
    }
    
    public enum WallRunType {
        HORIZONTAL,
        VERTICAL
    }
    
    public static boolean isWallRunning(Player player) {
        return activeWallRuns.containsKey(player.getUUID());
    }
    
    public static WallRunState getWallRunState(Player player) {
        return activeWallRuns.get(player.getUUID());
    }
    
    private static boolean isOnCooldown(Player player) {
        Long cooldownEnd = cooldowns.get(player.getUUID());
        if (cooldownEnd == null) return false;
        long now = System.currentTimeMillis();
        if (now >= cooldownEnd) {
            cooldowns.remove(player.getUUID());
            return false;
        }
        return true;
    }
    
    private static void setCooldown(Player player) {
        cooldowns.put(player.getUUID(), System.currentTimeMillis() + COOLDOWN_MS);
    }
    
    public static boolean tryStartWallRun(Player player) {
        // Check if both types are disabled
        if (!isHorizontalEnabled() && !isVerticalEnabled()) {
            return false;
        }
        
        if (!FocusManager.isInFocus(player)) {
            return false;
        }
        
        if (isWallRunning(player)) {
            return false;
        }
        
        if (isOnCooldown(player)) {
            return false;
        }
        
        if (player.onGround()) {
            return false;
        }
        
        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        
        if (horizontalSpeed < MIN_SPEED) {
            return false;
        }
        
        Level level = player.level();
        BlockPos playerPos = player.blockPosition();
        Vec3 motionDir = new Vec3(velocity.x, 0, velocity.z).normalize();
        
        Direction foundWall = null;
        double closestDist = Double.MAX_VALUE;
        
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (isWallAt(level, playerPos, dir)) {
                Vec3 dirVec = new Vec3(dir.getStepX(), 0, dir.getStepZ());
                double dist = 1.0 - Math.abs(motionDir.dot(dirVec));
                if (dist < closestDist) {
                    closestDist = dist;
                    foundWall = dir;
                }
            }
        }
        
        if (foundWall == null) {
            return false;
        }
        
        Vec3 wallNormal = new Vec3(
            -foundWall.getStepX(),
            0,
            -foundWall.getStepZ()
        );
        
        double dot = motionDir.dot(wallNormal);
        double angleToNormal = Math.toDegrees(Math.acos(Math.clamp(dot, -1.0, 1.0)));
        
        double angleFromParallel = Math.abs(90.0 - angleToNormal);
        double angleFromInto = 180.0 - angleToNormal;
        
        WallRunType type;
        Vec3 runDirection;
        boolean wallIsOnRight;
        
        // Check horizontal first (only if enabled)
        if (isHorizontalEnabled() && angleFromParallel >= getHorizontalAngleMin() && angleFromParallel <= getHorizontalAngleMax()) {
            type = WallRunType.HORIZONTAL;
            
            runDirection = new Vec3(-wallNormal.z, 0, wallNormal.x).normalize();
            
            if (runDirection.dot(motionDir) < 0) {
                runDirection = runDirection.scale(-1);
            }
            
            Vec3 ourRight = new Vec3(runDirection.z, 0, -runDirection.x);
            wallIsOnRight = ourRight.dot(wallNormal) < 0;
            
        // Check vertical (only if enabled)
        } else if (isVerticalEnabled() && angleFromInto >= getVerticalAngleMin() && angleFromInto <= getVerticalAngleMax()) {
            type = WallRunType.VERTICAL;
            runDirection = new Vec3(0, 1, 0);
            wallIsOnRight = false;
            
        } else {
            return false;
        }
        
        WallRunState state = new WallRunState(
            type,
            foundWall,
            wallNormal,
            player.position(),
            runDirection,
            wallIsOnRight,
            player.getYRot()
        );
        
        activeWallRuns.put(player.getUUID(), state);
        
        if (type == WallRunType.HORIZONTAL) {
            Vec3 newVel = runDirection.scale(0.42);
            player.setDeltaMovement(newVel.x, 0, newVel.z);
            
            Vec3 offset = wallNormal.scale(0.05);
            player.setPos(player.getX() + offset.x, player.getY(), player.getZ() + offset.z);
            syncPosition(player); // Sync position to client to prevent desync
        } else {
            Vec3 intoWall = wallNormal.scale(-0.08);
            player.setDeltaMovement(intoWall.x, 0.32, intoWall.z);
        }
        
        player.fallDistance = 0;
        syncVelocity(player);
        
        return true;
    }
    
    private static boolean isWallAt(Level level, BlockPos playerPos, Direction dir) {
        BlockPos check1 = playerPos.relative(dir);
        BlockPos check2 = check1.above();
        
        // Check if chunks are loaded to prevent server crashes
        if (!level.isLoaded(check1) || !level.isLoaded(check2)) {
            return false;
        }
        
        BlockState state1 = level.getBlockState(check1);
        BlockState state2 = level.getBlockState(check2);
        
        return state1.isSolid() || state2.isSolid();
    }
    
    public static void updateWallRun(Player player) {
        WallRunState state = activeWallRuns.get(player.getUUID());
        if (state == null) {
            return;
        }
        
        state.ticksActive++;
        double distance = state.getDistanceTraveled(player.position());
        double maxDist = state.getMaxDistance();
        
        if (distance >= maxDist) {
            MatrixCraftMod.LOGGER.info("Wall run ended - max distance {} >= {}", distance, maxDist);
            endWallRun(player, state, true);
            return;
        }
        
        if (state.ticksActive >= state.getMaxTicks()) {
            MatrixCraftMod.LOGGER.info("Wall run ended - max ticks");
            endWallRun(player, state, true);
            return;
        }
        
        if (player.onGround()) {
            MatrixCraftMod.LOGGER.info("Wall run ended - hit ground");
            endWallRun(player, state, false);
            return;
        }
        
        Level level = player.level();
        BlockPos playerPos = player.blockPosition();
        
        if (!isWallAt(level, playerPos, state.wallDirection)) {
            MatrixCraftMod.LOGGER.info("Wall run ended - lost wall");
            endWallRun(player, state, true);
            return;
        }
        
        if (state.type == WallRunType.HORIZONTAL) {
            float speed = 0.40f - (state.ticksActive * 0.001f);
            speed = Math.max(speed, 0.30f);
            
            Vec3 newVel = state.runDirection.scale(speed);
            player.setDeltaMovement(newVel.x, 0, newVel.z);
            
        } else {
            double progress = distance / maxDist;
            
            float upSpeed = (float) (0.32 * (1.0 - progress * 0.8));
            upSpeed = Math.max(upSpeed, 0.06f);
            
            Vec3 intoWall = state.wallNormal.scale(-0.06);
            player.setDeltaMovement(intoWall.x, upSpeed, intoWall.z);
            
            if (state.ticksActive % 10 == 0) {
                MatrixCraftMod.LOGGER.info("Vertical: dist={}/{}, progress={}, speed={}", 
                    String.format("%.2f", distance), 
                    String.format("%.2f", maxDist),
                    String.format("%.2f", progress),
                    String.format("%.2f", upSpeed));
            }
        }
        
        player.fallDistance = 0;
        syncVelocity(player);
    }
    
    private static void endWallRun(Player player, WallRunState state, boolean doJump) {
        if (doJump) {
            Vec3 jumpVel;
            if (state.type == WallRunType.HORIZONTAL) {
                jumpVel = state.wallNormal.scale(0.45)
                    .add(state.runDirection.scale(0.2))
                    .add(0, 0.42, 0);
            } else {
                jumpVel = state.wallNormal.scale(0.55).add(0, 0.4, 0);
            }
            player.setDeltaMovement(jumpVel);
            syncVelocity(player);
            MatrixCraftMod.LOGGER.info("Wall jump!");
        }
        
        setCooldown(player);
        activeWallRuns.remove(player.getUUID());
        MatrixCraftMod.LOGGER.info("Wall run stopped after {} ticks", state.ticksActive);
    }
    
    public static void stopWallRun(Player player) {
        WallRunState state = activeWallRuns.remove(player.getUUID());
        if (state != null) {
            setCooldown(player);
        }
    }
    
    private static void syncVelocity(Player player) {
        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
        }
    }
    
    private static void syncPosition(Player player) {
        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundPlayerPositionPacket(
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(),
                Set.of(), // Empty set for absolute position
                0 // Teleport ID
            ));
        }
    }
    
    public static void stopAllWallRuns() {
        activeWallRuns.clear();
        cooldowns.clear();
    }
    
    public static void clientTick(Player player) {
        WallRunState state = activeWallRuns.get(player.getUUID());
        if (state != null) {
            state.ticksActive++;
        }
    }
}