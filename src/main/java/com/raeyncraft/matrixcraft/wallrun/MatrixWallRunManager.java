package com.raeyncraft.matrixcraft.wallrun;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.bullettime.FocusManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages Matrix-style wall running mechanics during Focus mode.
 */
public class MatrixWallRunManager {
    
    private static final Map<UUID, WallRunState> activeWallRuns = new HashMap<>();
    
    // Configuration
    private static final double HORIZONTAL_MIN_ANGLE = 30.0;
    private static final double HORIZONTAL_MAX_ANGLE = 60.0;
    private static final double VERTICAL_MIN_ANGLE = 80.0;
    private static final double VERTICAL_MAX_ANGLE = 100.0;
    
    private static final double MAX_HORIZONTAL_DISTANCE = 6.0;
    private static final double MAX_VERTICAL_DISTANCE = 6.0;
    private static final double WALL_DETECTION_RANGE = 1.5;
    private static final double MIN_SPEED_THRESHOLD = 0.15;
    
    // Movement speeds
    private static final double HORIZONTAL_SPEED = 0.45;
    private static final double VERTICAL_SPEED = 0.42;
    private static final double HORIZONTAL_LIFT = 0.15;
    
    public static class WallRunState {
        public final WallRunType type;
        public final Direction wallDirection;
        public final Vec3 wallNormal;
        public final Vec3 startPos;
        public final Vec3 runDirection;
        public final boolean wallIsRightSide;
        public double distanceTraveled;
        public int ticksActive;
        
        public WallRunState(WallRunType type, Direction wallDirection, Vec3 wallNormal, 
                           Vec3 startPos, Vec3 runDirection, boolean wallIsRightSide) {
            this.type = type;
            this.wallDirection = wallDirection;
            this.wallNormal = wallNormal;
            this.startPos = startPos;
            this.runDirection = runDirection;
            this.wallIsRightSide = wallIsRightSide;
            this.distanceTraveled = 0;
            this.ticksActive = 0;
        }
        
        public boolean isComplete() {
            return (type == WallRunType.HORIZONTAL && distanceTraveled >= MAX_HORIZONTAL_DISTANCE) ||
                   (type == WallRunType.VERTICAL && distanceTraveled >= MAX_VERTICAL_DISTANCE);
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
    
    /**
     * Attempt to start a wall run - called every tick for airborne players
     */
    public static boolean tryStartWallRun(Player player) {
        // Only works in Focus mode
        if (!FocusManager.isInFocus(player)) {
            return false;
        }
        
        // Don't start if already wall running
        if (isWallRunning(player)) {
            return false;
        }
        
        // Must be airborne (not on ground)
        if (player.onGround()) {
            return false;
        }
        
        // Must not be in water
        if (player.isInWaterOrBubble()) {
            return false;
        }
        
        // Check if player has enough horizontal speed
        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        
        MatrixCraftMod.LOGGER.info("Wall run check - Speed: {}, Velocity: {}", horizontalSpeed, velocity);
        
        if (horizontalSpeed < MIN_SPEED_THRESHOLD) {
            MatrixCraftMod.LOGGER.info("Wall speed too low: {} < {}", horizontalSpeed, MIN_SPEED_THRESHOLD);
            return false;
        }
        
        // Detect nearby wall
        WallDetectionResult wall = detectWall(player);
        if (wall == null) {
            MatrixCraftMod.LOGGER.info("No wall detected");
            return false;
        }
        
        MatrixCraftMod.LOGGER.info("Wall detected: {} at {}", wall.direction, wall.blockPos);
        
        // Calculate approach angle
        Vec3 movementDir = new Vec3(velocity.x, 0, velocity.z).normalize();
        double approachAngle = calculateApproachAngle(movementDir, wall.normal);
        
        MatrixCraftMod.LOGGER.info("Wall approach angle: {}", approachAngle);
        
        // Determine wall run type based on angle
        WallRunType type = null;
        
        if (approachAngle >= HORIZONTAL_MIN_ANGLE && approachAngle <= HORIZONTAL_MAX_ANGLE) {
            type = WallRunType.HORIZONTAL;
            MatrixCraftMod.LOGGER.info("HORIZONTAL wall run triggered!");
        } else if (approachAngle >= VERTICAL_MIN_ANGLE && approachAngle <= VERTICAL_MAX_ANGLE) {
            type = WallRunType.VERTICAL;
            MatrixCraftMod.LOGGER.info("VERTICAL wall run triggered!");
        } else {
            MatrixCraftMod.LOGGER.info("Wall angle not in range - H:{}-{}, V:{}-{}", 
                HORIZONTAL_MIN_ANGLE, HORIZONTAL_MAX_ANGLE, VERTICAL_MIN_ANGLE, VERTICAL_MAX_ANGLE);
        }
        
        if (type == null) {
            return false;
        }
        
        // Calculate run direction and wall side
        Vec3 runDirection;
        boolean wallIsRightSide = false;
        
        if (type == WallRunType.HORIZONTAL) {
            runDirection = wall.normal.cross(new Vec3(0, 1, 0)).normalize();
            
            if (runDirection.dot(movementDir) < 0) {
                runDirection = runDirection.scale(-1);
            }
            
            Vec3 cross = movementDir.cross(wall.normal);
            wallIsRightSide = cross.y > 0;
            
        } else { // VERTICAL
            runDirection = new Vec3(0, 1, 0);
        }
        
        // Create and store wall run state
        WallRunState state = new WallRunState(
            type,
            wall.direction,
            wall.normal,
            player.position(),
            runDirection,
            wallIsRightSide
        );
        
        activeWallRuns.put(player.getUUID(), state);
        
        // Apply initial boost to start the wall run
        applyInitialBoost(player, state);
        
        MatrixCraftMod.LOGGER.info("Wall run STARTED! Type: {}", type);
        
        return true;
    }
    
    /**
     * Apply initial velocity boost when starting wall run
     */
    private static void applyInitialBoost(Player player, WallRunState state) {
        Vec3 currentVelocity = player.getDeltaMovement();
        
        if (state.type == WallRunType.HORIZONTAL) {
            Vec3 horizontalVel = state.runDirection.scale(HORIZONTAL_SPEED);
            player.setDeltaMovement(horizontalVel.x, 0.2, horizontalVel.z);
        } else {
            Vec3 verticalVel = new Vec3(currentVelocity.x * 0.5, VERTICAL_SPEED, currentVelocity.z * 0.5);
            player.setDeltaMovement(verticalVel);
        }
    }
    
    /**
     * Update wall run for a player (called each tick)
     */
    public static void updateWallRun(Player player) {
        WallRunState state = activeWallRuns.get(player.getUUID());
        if (state == null) {
            return;
        }
        
        state.ticksActive++;
        
        if (state.isComplete()) {
            if (state.type == WallRunType.VERTICAL) {
                performAutomaticWallJump(player, state);
            }
            stopWallRun(player);
            return;
        }
        
        WallDetectionResult wall = detectWall(player);
        if (wall == null) {
            MatrixCraftMod.LOGGER.info("Wall run ended - no wall");
            stopWallRun(player);
            return;
        }
        
        if (!wall.direction.equals(state.wallDirection)) {
            MatrixCraftMod.LOGGER.info("Wall run ended - direction changed");
            stopWallRun(player);
            return;
        }
        
        Vec3 movement;
        double speed;
        
        if (state.type == WallRunType.HORIZONTAL) {
            speed = HORIZONTAL_SPEED;
            movement = state.runDirection.scale(speed).add(0, HORIZONTAL_LIFT, 0);
            
            double liftReduction = Math.max(0, 1.0 - (state.ticksActive / 30.0));
            movement = new Vec3(movement.x, movement.y * liftReduction, movement.z);
            
        } else {
            speed = VERTICAL_SPEED;
            double speedReduction = Math.max(0.2, 1.0 - (state.ticksActive / 20.0));
            movement = state.runDirection.scale(speed * speedReduction);
        }
        
        player.setDeltaMovement(movement);
        player.fallDistance = 0;
        
        state.distanceTraveled += speed;
    }
    
    private static void performAutomaticWallJump(Player player, WallRunState state) {
        Vec3 jumpDirection = state.wallNormal.scale(-0.6).add(0, 0.8, 0).normalize();
        Vec3 jumpVelocity = jumpDirection.scale(0.65);
        
        player.setDeltaMovement(jumpVelocity);
        player.fallDistance = 0;
        
        MatrixCraftMod.LOGGER.info("Auto wall jump executed!");
    }
    
    public static void stopWallRun(Player player) {
        WallRunState state = activeWallRuns.remove(player.getUUID());
        
        if (state != null) {
            MatrixCraftMod.LOGGER.info("Wall run stopped. Distance: {}", state.distanceTraveled);
            
            if (state.type == WallRunType.HORIZONTAL) {
                Vec3 exitVelocity = state.runDirection.scale(0.25);
                Vec3 currentVel = player.getDeltaMovement();
                player.setDeltaMovement(
                    currentVel.x + exitVelocity.x, 
                    currentVel.y, 
                    currentVel.z + exitVelocity.z
                );
            }
        }
    }
    
    public static void stopAllWallRuns() {
        activeWallRuns.clear();
    }
    
    /**
     * Detect a wall near the player
     */
    private static WallDetectionResult detectWall(Player player) {
        Level level = player.level();
        Vec3 playerPos = player.position();
        Vec3 velocity = player.getDeltaMovement();
        
        Vec3 horizontalVel = new Vec3(velocity.x, 0, velocity.z);
        if (horizontalVel.lengthSqr() > 0) {
            horizontalVel = horizontalVel.normalize();
        }
        
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        
        for (Direction dir : directions) {
            Vec3 dirVec = new Vec3(dir.getStepX(), 0, dir.getStepZ());
            
            // Check at player's position and slightly ahead
            for (double dist = 0.5; dist <= WALL_DETECTION_RANGE; dist += 0.5) {
                Vec3 checkPos = playerPos.add(
                    dir.getStepX() * dist,
                    0,
                    dir.getStepZ() * dist
                );
                
                BlockPos blockPos = BlockPos.containing(checkPos.x, playerPos.y, checkPos.z);
                
                // Check from feet to above head
                for (int yOffset = 0; yOffset <= 2; yOffset++) {
                    BlockPos checkBlock = blockPos.above(yOffset);
                    
                    if (!level.isLoaded(checkBlock)) {
                        continue;
                    }
                    
                    BlockState state = level.getBlockState(checkBlock);
                    
                    if (!state.isAir() && state.isSolid()) {
                        Vec3 normal = new Vec3(-dir.getStepX(), 0, -dir.getStepZ()).normalize();
                        return new WallDetectionResult(dir, normal, checkBlock);
                    }
                }
            }
        }
        
        return null;
    }
    
    private static double calculateApproachAngle(Vec3 movementDir, Vec3 wallNormal) {
        double dot = movementDir.dot(wallNormal);
        double angle = Math.toDegrees(Math.acos(Math.abs(dot)));
        return angle;
    }
    
    private static class WallDetectionResult {
        public final Direction direction;
        public final Vec3 normal;
        public final BlockPos blockPos;
        
        public WallDetectionResult(Direction direction, Vec3 normal, BlockPos blockPos) {
            this.direction = direction;
            this.normal = normal;
            this.blockPos = blockPos;
        }
    }
}