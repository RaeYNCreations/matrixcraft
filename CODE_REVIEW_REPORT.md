# MatrixCraft Comprehensive Code Review Report

**Review Date:** 2024
**Scope:** Full codebase security and bug analysis
**Focus Areas:** NPE, Memory Leaks, Threading, Client-Server Sync, Logic Bugs, Performance

---

## Executive Summary

**Total Issues Found:** 23
- **Critical:** 5
- **High:** 8
- **Medium:** 7
- **Low:** 3

---

## CRITICAL ISSUES

### 1. MatrixWallRunManager.java - Null Pointer Exception (NPE)
**File:** `MatrixWallRunManager.java`
**Lines:** 300-312
**Category:** NPE
**Severity:** Critical

**Problem:**
The `isWallAt()` method checks if chunks are loaded, but the subsequent `level.getBlockState()` calls can still throw NPE if the chunk unloads between the check and the call (race condition).

```java
if (!level.isLoaded(check1) || !level.isLoaded(check2)) {
    return false;
}
BlockState state1 = level.getBlockState(check1); // NPE if chunk unloads here
BlockState state2 = level.getBlockState(check2); // NPE if chunk unloads here
```

**Suggested Fix:**
Wrap the getBlockState calls in try-catch or use a safer API:
```java
private static boolean isWallAt(Level level, BlockPos playerPos, Direction dir) {
    BlockPos check1 = playerPos.relative(dir);
    BlockPos check2 = check1.above();
    
    if (!level.isLoaded(check1) || !level.isLoaded(check2)) {
        return false;
    }
    
    try {
        BlockState state1 = level.getBlockState(check1);
        BlockState state2 = level.getBlockState(check2);
        return state1.isSolid() || state2.isSolid();
    } catch (Exception e) {
        return false;
    }
}
```

---

### 2. DynamicLightManager.java - Memory Leak
**File:** `DynamicLightManager.java`
**Lines:** 23-64
**Category:** Memory Leak
**Severity:** Critical

**Problem:**
Multiple ConcurrentHashMaps grow indefinitely without cleanup:
- `dlsCache` - Never cleared except manually
- `entityDls` - Only cleaned when entities die
- `entityDlsChains` - Same issue
- `entityRefs` - WeakReferences help but map itself grows
- `lastSeenMs` - Grows indefinitely

The TTL-based cleanup in `onClientTick()` helps but may not catch all cases (e.g., if client disconnects from server, or world changes).

**Suggested Fix:**
Add size limits and aggressive cleanup:
```java
private static final int MAX_CACHE_SIZE = 1000;
private static final int MAX_ENTITY_LIGHTS = 500;

// In syncDynamicLights():
if (dlsCache.size() > MAX_CACHE_SIZE) {
    // Remove oldest 20%
    List<BlockPos> toRemove = new ArrayList<>(dlsCache.keySet());
    toRemove.subList(0, dlsCache.size() / 5).forEach(pos -> {
        Object dls = dlsCache.remove(pos);
        if (dls != null) invokeRemoveLightSource(dls);
    });
}

// Add world change detection:
private static WeakReference<Level> lastLevel = new WeakReference<>(null);

public static void clearIfWorldChanged(Level currentLevel) {
    Level last = lastLevel.get();
    if (last != currentLevel) {
        clearAllDynamicLights();
        lastLevel = new WeakReference<>(currentLevel);
    }
}
```

---

### 3. GlassRepairSystem.java - Performance Issue
**File:** `GlassRepairSystem.java`
**Lines:** 205-278
**Category:** Performance
**Severity:** Critical

**Problem:**
The `scanForGlassNearPlayers()` method performs cubic scanning which is O(n³) complexity. With `MAX_EFFECTIVE_RADIUS = 32`, this checks up to 32³ = 32,768 blocks per player, per tick. With multiple players, this becomes extremely expensive.

Even with the `MAX_CHECKS_PER_TICK = 1000` limit, incomplete scans can miss glass breaks.

**Suggested Fix:**
Use event-based detection only, or implement incremental scanning:
```java
private static int scanOffset = 0; // Persist across ticks

private static void scanForGlassNearPlayers() {
    for (Map.Entry<ServerLevel, GlassTracker> entry : trackers.entrySet()) {
        ServerLevel level = entry.getKey();
        GlassTracker tracker = entry.getValue();
        
        if (level.players().isEmpty()) continue;
        
        int effectiveRadius = Math.min(scanRadius, MAX_EFFECTIVE_RADIUS);
        int blocksPerTick = 100; // Much smaller chunks
        int startOffset = scanOffset;
        int endOffset = startOffset + blocksPerTick;
        
        // Scan only a slice each tick, rotating through volume
        int totalBlocks = (effectiveRadius * 2) * (effectiveRadius * 2) * (effectiveRadius * 2);
        if (endOffset >= totalBlocks) {
            scanOffset = 0; // Reset
        } else {
            scanOffset = endOffset;
        }
        
        // Use linear indexing to scan a slice
        // ... implementation
    }
}
```

Better approach: **Remove active scanning entirely**, rely only on events + change detection:
```java
// Delete scanForGlassNearPlayers() entirely
// Only use checkForDirectBlockChanges() which is O(n) where n = tracked glass count
```

---

### 4. FocusModeEffects.java - Threading/State Corruption
**File:** `FocusModeEffects.java`
**Lines:** 82-104
**Category:** Threading/Logic Bug
**Severity:** Critical

**Problem:**
The cobweb and water bypass logic modifies global state (`MatrixSettings.setCobwebsEnabled()`) per-player, which causes issues in multiplayer:

1. **Thread safety:** Multiple player ticks can modify `cobwebsEnabled` simultaneously
2. **State corruption:** If Player A enters Focus (disables cobwebs) then Player B enters Focus, when Player A exits, cobwebs are re-enabled even though Player B is still in Focus
3. **Global state for per-player feature:** This should be per-player, not global

```java
if (inFocus) {
    if (MatrixSettings.areCobwebsEnabled()) {
        cobwebsWereEnabled.add(playerId); // What if another player already disabled it?
        MatrixSettings.setCobwebsEnabled(false); // Affects ALL players!
    }
}
```

**Suggested Fix:**
Make this per-player instead of global:
```java
// Track per-player bypass state instead of modifying global settings
private static final Set<UUID> playersWithCobwebBypass = new HashSet<>();

@SubscribeEvent
public static void onPlayerTick(PlayerTickEvent.Pre event) {
    Player player = event.getEntity();
    if (player.level().isClientSide) return;
    
    boolean inFocus = FocusManager.isInFocus(player);
    boolean cobwebBypassEnabled = isFocusCobwebBypassEnabled();
    
    UUID playerId = player.getUUID();
    
    // Update bypass set (per-player state)
    if (inFocus && cobwebBypassEnabled) {
        playersWithCobwebBypass.add(playerId);
    } else {
        playersWithCobwebBypass.remove(playerId);
    }
    
    // Apply effects directly to player, don't touch global state
}

// In CobwebBlockMixin, check per-player state:
public static boolean shouldBypassCobweb(Player player) {
    return playersWithCobwebBypass.contains(player.getUUID()) || 
           !MatrixSettings.areCobwebsEnabled();
}
```

---

### 5. MatrixWallRunManager.java - Client-Server Desync
**File:** `MatrixWallRunManager.java`
**Lines:** 280-297, 413-428
**Category:** Client-Server Sync
**Severity:** Critical

**Problem:**
The wall run system syncs velocity and position to client, but these are immediately overwritten by client prediction. The `syncPosition()` method sends a teleport packet, but Minecraft's client-side prediction will still interpolate/correct the position, causing rubber-banding.

Also, `clientTick()` increments `ticksActive` on client, but this should only happen server-side for authoritative state.

**Suggested Fix:**
```java
// Remove clientTick() modification of state - read-only on client
public static void clientTick(Player player) {
    // Only for rendering/visual effects, don't modify state
    WallRunState state = activeWallRuns.get(player.getUUID());
    if (state != null) {
        // Visual effects only
    }
}

// For position sync, use relative teleport flags:
private static void syncPosition(Player player) {
    if (player instanceof ServerPlayer sp) {
        // Use relative flags to hint client to use exact position
        sp.connection.send(new ClientboundPlayerPositionPacket(
            player.getX(), player.getY(), player.getZ(),
            player.getYRot(), player.getXRot(),
            Set.of(), // Use empty set for absolute positioning
            0,
            true // Force accept teleport
        ));
    }
}
```

---

## HIGH SEVERITY ISSUES

### 6. DynamicLightManager.java - Reflection API Error Handling
**File:** `DynamicLightManager.java`
**Lines:** 98-126
**Category:** NPE/Logic Bug
**Severity:** High

**Problem:**
The `discoverDynamicLightsApi()` method sets `dynamicLightsAvailable = true` in `init()` even if API discovery partially fails. If `methodAddLightSource` is found but `methodRemoveLightSource` is null, lights can be added but never removed, causing memory leaks.

**Suggested Fix:**
```java
private static void discoverDynamicLightsApi() {
    if (dynamicLightsInstance == null) return;
    Class<?> cls = dynamicLightsInstance.getClass();

    for (Method m : cls.getMethods()) {
        // ... discovery logic
    }

    // Validate ALL required methods
    boolean allMethodsFound = methodAddLightSource != null && 
                              methodRemoveLightSource != null &&
                              dynamicLightSourceClass != null;
    
    if (!allMethodsFound) {
        MatrixCraftMod.LOGGER.error("[DynamicLightManager] Incomplete API discovery - disabling");
        methodAddLightSource = null;
        methodRemoveLightSource = null;
        methodUpdateTracking = null;
        methodClearLightSources = null;
        methodUpdateAll = null;
        dynamicLightSourceClass = null;
        dynamicLightsAvailable = false;
    }
}
```

---

### 7. FocusManager.java - Division by Zero
**File:** `FocusManager.java`
**Lines:** 55-57
**Category:** Logic Bug
**Severity:** High

**Problem:**
`getProgress()` can divide by zero if `durationTicks` is 0:
```java
public float getProgress() {
    return (float) ticksRemaining / (float) durationTicks; // Division by zero if durationTicks = 0
}
```

This can happen if config is set to 0 or negative value.

**Suggested Fix:**
```java
public float getProgress() {
    if (durationTicks <= 0) return 0.0f;
    return (float) ticksRemaining / (float) durationTicks;
}

// Also validate in getFocusDuration():
public static int getFocusDuration() {
    try {
        int duration = MatrixCraftConfig.getFocusDurationTicks();
        return Math.max(1, duration); // Ensure at least 1 tick
    } catch (Exception e) {
        return FOCUS_DURATION_TICKS;
    }
}
```

---

### 8. BulletTrailLighting.java - Race Condition
**File:** `BulletTrailLighting.java`
**Lines:** 199-220
**Category:** Threading
**Severity:** High

**Problem:**
The `tick()` method iterates over `activeLights` with an iterator and modifies it, while other threads (e.g., rendering thread) may be reading it via `getActiveLights()`. Despite using ConcurrentHashMap, iteration + removal during concurrent reads can cause ConcurrentModificationException or missed updates.

**Suggested Fix:**
```java
public static void tick() {
    if (activeLights.isEmpty()) {
        return;
    }
    
    // Create snapshot to avoid concurrent modification
    List<BlockPos> toRemove = new ArrayList<>();
    
    for (Map.Entry<BlockPos, LightSource> entry : activeLights.entrySet()) {
        LightSource light = entry.getValue();
        light.ticksRemaining--;
        
        if (light.ticksRemaining <= 0) {
            toRemove.add(entry.getKey());
        }
    }
    
    // Remove after iteration
    for (BlockPos pos : toRemove) {
        activeLights.remove(pos);
    }
    
    // Update texture
    try {
        DynamicLightTextureManager.ensureInit();
        DynamicLightTextureManager.updateTexture();
    } catch (Throwable e) {
        MatrixCraftMod.LOGGER.debug("[BulletTrailLighting] Texture update failed: " + e.getMessage());
    }
}
```

---

### 9. GlassRepairSystem.java - Concurrent Modification
**File:** `GlassRepairSystem.java`
**Lines:** 156-198
**Category:** Threading
**Severity:** High

**Problem:**
`checkForDirectBlockChanges()` iterates over `tracker.knownGlass` and modifies it (via `iterator.remove()`), but this happens every tick. If the `scanForGlassNearPlayers()` method adds to `knownGlass` on the same tick, we could get a ConcurrentModificationException (even though it's unlikely, the maps aren't thread-safe for this pattern).

**Suggested Fix:**
Use proper synchronization or switch to ConcurrentHashMap:
```java
private static class GlassTracker {
    // Use concurrent collections
    Map<BlockPos, BlockState> knownGlass = new ConcurrentHashMap<>();
    List<BrokenGlass> brokenGlass = Collections.synchronizedList(new ArrayList<>());
    Map<BlockPos, BlockState> lastTickSnapshot = new ConcurrentHashMap<>();
}
```

---

### 10. MatrixWallRunManager.java - Infinite Recursion Risk
**File:** `MatrixWallRunManager.java`
**Lines:** 406-411
**Category:** Logic Bug
**Severity:** High

**Problem:**
`stopWallRun()` calls `setCooldown()` which doesn't check if player is already on cooldown. If called repeatedly (e.g., from multiple event handlers), it keeps extending the cooldown unnecessarily.

Also, there's a potential for infinite recursion if `endWallRun()` triggers events that call `stopWallRun()` again.

**Suggested Fix:**
```java
public static void stopWallRun(Player player) {
    WallRunState state = activeWallRuns.remove(player.getUUID());
    if (state != null) {
        // Only set cooldown if not already on cooldown
        if (!isOnCooldown(player)) {
            setCooldown(player);
        }
    }
}

// Add guard in endWallRun to prevent re-entry:
private static void endWallRun(Player player, WallRunState state, boolean doJump) {
    // Check if already ended
    if (!activeWallRuns.containsKey(player.getUUID())) {
        return; // Already ended
    }
    
    // ... rest of logic
}
```

---

### 11. DynamicLightManager.java - Proxy Memory Leak
**File:** `DynamicLightManager.java`
**Lines:** 230-262, 358-426
**Category:** Memory Leak
**Severity:** High

**Problem:**
The dynamic proxy objects created via `Proxy.newProxyInstance()` capture references to the outer class and variables. If LambDynLights holds references to these proxies after they're removed from our caches, they can leak memory because the `InvocationHandler` closures capture the maps (`entityRefs`, `lastSeenMs`, etc.).

**Suggested Fix:**
Ensure proxies are weakly referenced and don't capture strong references:
```java
// Make InvocationHandler not capture entire manager
private static InvocationHandler createEntityHandler(final int id, final int chainIndex, 
                                                     final double spacing, final float r, 
                                                     final float g, final float b) {
    return new InvocationHandler() {
        private boolean enabled = true;
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // ... implementation
            
            // Use method references instead of lambda closures where possible
            WeakReference<Entity> wr = entityRefs.get(id);
            if (wr == null) {
                return getDefaultReturn(method.getReturnType());
            }
            
            // ... rest
        }
    };
}
```

---

### 12. BulletTrailTracker.java - Entity Memory Leak
**File:** `BulletTrailTracker.java`
**Lines:** 51-52, 220-246
**Category:** Memory Leak
**Severity:** High

**Problem:**
`processedBullets` and `bulletLastPos` are HashSet/HashMap that track bullets by entity ID. The cleanup happens every 40 ticks, which means fast-moving bullets that despawn between cleanup cycles can accumulate. With high fire rate guns, this adds up.

Also, the cleanup checks `mc.level.getEntity(id)` which is O(1) but iterates all entries - with thousands of bullets, this becomes expensive.

**Suggested Fix:**
```java
// Clean up more frequently for bullets
if (tickCounter % 10 == 0) { // Every 0.5 seconds instead of 2 seconds
    cleanupOldEntries(mc);
}

// Also add size limit:
private static final int MAX_TRACKED_BULLETS = 500;

private static void scanBulletEntities(Minecraft mc) {
    // Check size limit
    if (processedBullets.size() > MAX_TRACKED_BULLETS) {
        // Clear oldest 20%
        List<Integer> toRemove = new ArrayList<>(processedBullets);
        for (int i = 0; i < toRemove.size() / 5; i++) {
            int id = toRemove.get(i);
            processedBullets.remove(id);
            bulletLastPos.remove(id);
            try {
                DynamicLightManager.untrackEntityLightById(id);
            } catch (Throwable e) {
                // ignore
            }
        }
    }
    
    // ... rest of logic
}
```

---

### 13. FocusServerEvents.java - Null Effect Check
**File:** `FocusServerEvents.java`
**Lines:** 60-70, 76-88
**Category:** NPE
**Severity:** High

**Problem:**
Event handlers check `event.getEffect() == null` and `event.getEffectInstance() == null`, but then use `instanceof` which can throw NPE if the `.value()` call returns null:

```java
if (event.getEffect().value() instanceof MatrixFocusEffect) // NPE if value() is null
```

**Suggested Fix:**
```java
@SubscribeEvent
public static void onEffectRemoved(MobEffectEvent.Remove event) {
    if (event.getEffect() == null) return;
    
    try {
        var effectValue = event.getEffect().value();
        if (effectValue instanceof MatrixFocusEffect) {
            LivingEntity entity = event.getEntity();
            if (entity instanceof ServerPlayer player) {
                FocusManager.deactivateFocus(player);
            }
        }
    } catch (Exception e) {
        MatrixCraftMod.LOGGER.debug("Effect removal check failed", e);
    }
}
```

---

## MEDIUM SEVERITY ISSUES

### 14. MatrixWallRunManager.java - Edge Case: Zero Distance
**File:** `MatrixWallRunManager.java`
**Lines:** 123-131
**Category:** Logic Bug
**Severity:** Medium

**Problem:**
`getDistanceTraveled()` can return negative values for vertical wall runs if player Y position decreases (falls). This breaks the distance check in `updateWallRun()` line 325.

**Suggested Fix:**
```java
public double getDistanceTraveled(Vec3 currentPos) {
    if (type == WallRunType.HORIZONTAL) {
        double dx = currentPos.x - startPos.x;
        double dz = currentPos.z - startPos.z;
        return Math.sqrt(dx * dx + dz * dz);
    } else {
        // Use absolute difference to handle falling
        return Math.abs(currentPos.y - startPos.y);
    }
}
```

---

### 15. DynamicLightTextureManager.java - Reflection Warning Spam
**File:** `DynamicLightTextureManager.java`
**Lines:** 39-68
**Category:** Performance/Logging
**Severity:** Medium

**Problem:**
The `createResourceLocation()` method uses reflection every time it's called. While it's static and called once, the error logging at line 66 is ERROR level which is too severe for a recoverable issue.

**Suggested Fix:**
```java
// Cache the result
private static final ResourceLocation TEX_LOC;

static {
    TEX_LOC = createResourceLocationInternal("matrixcraft", "trail_lights");
}

private static ResourceLocation createResourceLocationInternal(String namespace, String path) {
    try {
        // ... reflection logic
        return (ResourceLocation) ctor2.newInstance(namespace, path);
    } catch (Throwable t) {
        MatrixCraftMod.LOGGER.warn("[DynamicLightTextureManager] Using fallback ResourceLocation construction");
        // Try fallback
        try {
            return ResourceLocation.parse(namespace + ":" + path);
        } catch (Throwable t2) {
            MatrixCraftMod.LOGGER.error("[DynamicLightTextureManager] ResourceLocation construction completely failed");
            return null;
        }
    }
}
```

---

### 16. GlassRepairSystem.java - Integer Overflow
**File:** `GlassRepairSystem.java`
**Lines:** 272-277
**Category:** Logic Bug
**Severity:** Medium

**Problem:**
The log message happens every 100 ticks regardless of whether anything changed. This spams logs unnecessarily.

Also, `tickCounter` is an `int` that increments every tick - it will overflow after ~24 days of server runtime at 20 TPS (2^31 / 20 / 60 / 60 / 24 ≈ 24.8 days), causing modulo operations to behave unexpectedly.

**Suggested Fix:**
```java
// Use long for tick counter
private static long tickCounter = 0;

// Only log when there's something to report
if (tickCounter % LOG_FREQUENCY_TICKS == 0 && (tracker.knownGlass.size() > 0 || tracker.brokenGlass.size() > 0)) {
    MatrixCraftMod.LOGGER.info("[GlassRepair] Status: " + 
        tracker.knownGlass.size() + " glass tracked, " + 
        tracker.brokenGlass.size() + " pending repair" +
        (newGlassFound > 0 ? ", +" + newGlassFound + " new" : ""));
}
```

---

### 17. FocusModeEffects.java - Attribute Leak
**File:** `FocusModeEffects.java`
**Lines:** 110-119
**Category:** Memory Leak
**Severity:** Medium

**Problem:**
When water bypass is active, the code sets `waterSpeed.setBaseValue(1.75)` but never restores the original base value. This permanently modifies the player's water movement attribute even after Focus ends.

**Suggested Fix:**
```java
// Track original values
private static final Map<UUID, Double> originalWaterSpeed = new HashMap<>();

// In onPlayerTick:
if (waterBypassActive) {
    if (player.isInWater()) {
        AttributeInstance waterSpeed = player.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
        if (waterSpeed != null) {
            UUID playerId = player.getUUID();
            if (!originalWaterSpeed.containsKey(playerId)) {
                originalWaterSpeed.put(playerId, waterSpeed.getBaseValue());
            }
            waterSpeed.setBaseValue(1.75);
        }
    }
} else {
    // Restore original value
    UUID playerId = player.getUUID();
    if (originalWaterSpeed.containsKey(playerId)) {
        AttributeInstance waterSpeed = player.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
        if (waterSpeed != null) {
            waterSpeed.setBaseValue(originalWaterSpeed.get(playerId));
        }
        originalWaterSpeed.remove(playerId);
    }
}
```

---

### 18. BulletTrailLighting.java - Stream Performance
**File:** `BulletTrailLighting.java`
**Lines:** 295-300
**Category:** Performance
**Severity:** Medium

**Problem:**
The `pruneOldestLights()` method uses a stream with `.sorted()` which is O(n log n). This is called when lights reach capacity (300 lights), making it expensive. Also, the comment says "O(n)" but it's actually O(n log n).

**Suggested Fix:**
```java
private static void pruneOldestLights(int count) {
    if (activeLights.size() <= count) {
        activeLights.clear();
        return;
    }
    
    // Use min-heap for O(n log k) where k = count
    PriorityQueue<Map.Entry<BlockPos, LightSource>> oldest = new PriorityQueue<>(
        count,
        Comparator.comparingInt(e -> e.getValue().ticksRemaining)
    );
    
    for (Map.Entry<BlockPos, LightSource> entry : activeLights.entrySet()) {
        oldest.offer(entry);
        if (oldest.size() > count) {
            oldest.poll();
        }
    }
    
    // Remove the oldest
    while (!oldest.isEmpty()) {
        activeLights.remove(oldest.poll().getKey());
    }
}
```

---

### 19. LivingEntityFluidMixin.java - Excessive Velocity Modification
**File:** `LivingEntityFluidMixin.java`
**Lines:** 24-31
**Category:** Logic Bug/Balance
**Severity:** Medium

**Problem:**
The lava bypass multiplies velocity by 2.0 (horizontal) and 1.5 (vertical) every tick while in lava and moving. This is exponential growth: after 10 ticks, velocity would be 2^10 = 1024x original speed, launching players at extreme speeds.

**Suggested Fix:**
```java
if (lavaBypassActive && player.isInLava()) {
    if (travelVector.lengthSqr() > 0.0001) {
        Vec3 motion = player.getDeltaMovement();
        // Set to a fixed speed instead of multiplying
        double horizontalSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontalSpeed < 0.3) { // Only boost if below target speed
            double factor = 0.3 / Math.max(horizontalSpeed, 0.01);
            player.setDeltaMovement(motion.x * factor, motion.y * 1.2, motion.z * factor);
        }
    }
}
```

---

### 20. MatrixWallRunManager.java - Cooldown Map Leak
**File:** `MatrixWallRunManager.java`
**Lines:** 155-164
**Category:** Memory Leak
**Severity:** Medium

**Problem:**
The `isOnCooldown()` method removes expired cooldowns, but if a player disconnects while on cooldown, their UUID stays in the map forever. Same issue with `consecutiveWallJumps`.

**Suggested Fix:**
```java
// Add periodic cleanup
private static long lastCleanupTime = 0;
private static final long CLEANUP_INTERVAL_MS = 60000; // 1 minute

public static void serverTick() {
    long now = System.currentTimeMillis();
    if (now - lastCleanupTime > CLEANUP_INTERVAL_MS) {
        lastCleanupTime = now;
        
        // Remove all expired cooldowns
        cooldowns.entrySet().removeIf(entry -> now >= entry.getValue());
    }
}

// Also add to stopAllWallRuns() to be called on world unload
```

---

## LOW SEVERITY ISSUES

### 21. MatrixWallRunEventHandler.java - Redundant Client Check
**File:** `MatrixWallRunEventHandler.java`
**Lines:** 17-23
**Category:** Logic Bug
**Severity:** Low

**Problem:**
The code has a comment "Don't return early - let client-side logic continue below" but then all the logic below line 25 is server-side (checks `inFocus`, modifies wall run state). This creates confusion and the client branch does nothing useful.

**Suggested Fix:**
```java
@SubscribeEvent
public static void onPlayerTick(PlayerTickEvent.Pre event) {
    Player player = event.getEntity();
    
    // Handle client-side rendering separately
    if (player.level().isClientSide) {
        if (MatrixWallRunManager.isWallRunning(player)) {
            MatrixWallRunManager.clientTick(player);
        }
        return; // Early return for client - no server logic needed
    }
    
    // Server-side logic only below this point
    boolean inFocus = FocusManager.isInFocus(player);
    // ... rest
}
```

---

### 22. DynamicLightManager.java - Debug Logging in Production
**File:** `DynamicLightManager.java`
**Lines:** Multiple (all LOGGER.debug() calls)
**Category:** Performance
**Severity:** Low

**Problem:**
Many `LOGGER.debug()` calls construct strings even when debug logging is disabled. While minor, in hot paths (like entity update every tick), this adds overhead.

**Suggested Fix:**
```java
// Use lambda logging for debug
if (MatrixCraftMod.LOGGER.isDebugEnabled()) {
    MatrixCraftMod.LOGGER.debug("[DynamicLightManager] Active lights count: " + sources.size());
}

// Or use parameterized logging
MatrixCraftMod.LOGGER.debug("[DynamicLightManager] Active lights count: {}", sources == null ? 0 : sources.size());
```

---

### 23. BulletTrailTracker.java - Magic Numbers
**File:** `BulletTrailTracker.java`
**Lines:** 37-49
**Category:** Code Quality
**Severity:** Low

**Problem:**
Many magic numbers lack documentation:
- `TRAIL_COOLDOWN_MS = 30` - Why 30ms?
- `TRAIL_PARTICLE_OFFSET_LARGE = 0.04` - Why this value?
- Cleanup frequency `tickCounter % 40 == 0` - Why 40 ticks?

**Suggested Fix:**
```java
// Add comments explaining the values
private static final long TRAIL_COOLDOWN_MS = 30; // 30ms = ~1.5 frames at 60fps, prevents duplicate trails
private static final double TRAIL_PARTICLE_OFFSET_LARGE = 0.04; // Particle spread radius in blocks
private static final double TRAIL_PARTICLE_OFFSET_SMALL = 0.03; // Tighter spread for segment particles
private static final int CLEANUP_FREQUENCY_TICKS = 40; // Clean up every 2 seconds (40 ticks)

// Use named constant
if (tickCounter % CLEANUP_FREQUENCY_TICKS == 0) {
    cleanupOldEntries(mc);
}
```

---

## SUMMARY BY CATEGORY

### Null Pointer Exceptions (4)
1. ✅ MatrixWallRunManager.isWallAt() - chunk race condition
2. ✅ DynamicLightManager - reflection null checks
3. ✅ FocusServerEvents - effect value null
4. ✅ FocusManager.getProgress() - division by zero

### Memory Leaks (6)
1. ✅ DynamicLightManager - unbounded maps
2. ✅ DynamicLightManager - proxy closures
3. ✅ BulletTrailTracker - bullet tracking
4. ✅ MatrixWallRunManager - cooldown maps
5. ✅ FocusModeEffects - attribute modifications
6. ✅ GlassRepairSystem - knownGlass unbounded growth

### Threading Issues (3)
1. ✅ FocusModeEffects - global state race condition
2. ✅ BulletTrailLighting - concurrent modification
3. ✅ GlassRepairSystem - non-thread-safe collections

### Client-Server Sync (1)
1. ✅ MatrixWallRunManager - position/velocity desync

### Logic Bugs (6)
1. ✅ MatrixWallRunManager - negative distance
2. ✅ MatrixWallRunManager - infinite recursion risk
3. ✅ GlassRepairSystem - integer overflow
4. ✅ LivingEntityFluidMixin - exponential velocity
5. ✅ MatrixWallRunEventHandler - redundant check
6. ✅ BulletTrailLighting - incorrect algorithm complexity

### Performance Issues (3)
1. ✅ GlassRepairSystem - O(n³) scanning
2. ✅ BulletTrailLighting - stream sorting
3. ✅ DynamicLightManager - debug string construction

---

## RECOMMENDATIONS

### Immediate Actions (Critical + High)
1. **Fix GlassRepairSystem scanning** - Remove cubic scan or make incremental
2. **Fix FocusModeEffects global state** - Make per-player
3. **Add memory leak protections** - Size limits on all growing maps
4. **Add null safety** - Wrap risky operations in try-catch
5. **Fix velocity exploit** - Cap lava movement speed

### Short-term (Medium)
1. Add periodic cleanup for all tracking maps
2. Use concurrent collections where needed
3. Validate config values before use
4. Add attribute value restoration

### Long-term (Low + Code Quality)
1. Add comprehensive unit tests for edge cases
2. Document magic numbers and constants
3. Add performance monitoring/metrics
4. Consider using a proper entity tracking system

---

## SECURITY SUMMARY

**Critical Security Issues:** None
**Exploitable Bugs:** 1 (velocity exploit in LivingEntityFluidMixin)
**Resource Exhaustion Risks:** 3 (memory leaks in tracking systems)

All issues are related to stability and gameplay bugs rather than security vulnerabilities.

---

**Review Complete**
