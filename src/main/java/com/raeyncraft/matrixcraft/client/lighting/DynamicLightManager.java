package com.raeyncraft.matrixcraft.client.lighting;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.client.BulletTrailLighting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic Light Manager - updated.
 *
 * - supports entity-backed single proxies and chains of proxies trailing an entity
 * - provides clearAllDynamicLights()
 * - TTL-based sweep and pinging
 * - forceUpdateAll() (throttled) to force LambDynLights to refresh
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT, modid = MatrixCraftMod.MODID)
public class DynamicLightManager {

    private static boolean initialized = false;
    private static boolean dynamicLightsAvailable = false;
    private static boolean permanentlyDisabled = false; // Kill switch - disable on ANY error

    private static Object dynamicLightsInstance = null;
    private static Method methodAddLightSource = null;
    private static Method methodRemoveLightSource = null;
    private static Method methodUpdateTracking = null;
    private static Method methodClearLightSources = null;
    private static Method methodUpdateAll = null;

    private static Class<?> dynamicLightSourceClass = null;

    // BlockPos -> proxy/instance map (existing behavior)
    private static final Map<BlockPos, Object> dlsCache = new ConcurrentHashMap<>();

    // Single-proxy: Entity id -> proxy instance
    private static final Map<Integer, Object> entityDls = new ConcurrentHashMap<>();

    // Chained proxies: Entity id -> list of proxies
    private static final Map<Integer, List<Object>> entityDlsChains = new ConcurrentHashMap<>();

    // Entity id -> weak reference to entity
    private static final Map<Integer, WeakReference<Entity>> entityRefs = new ConcurrentHashMap<>();

    // Entity id -> last-seen timestamp (ms)
    private static final Map<Integer, Long> lastSeenMs = new ConcurrentHashMap<>();

    // TTL (ms) for unseen entities before forced untrack
    private static final long ENTITY_TTL_MS = 3000L;

    // Force-update throttle (ms)
    private static long lastForceUpdateMs = 0L;
    private static final long FORCE_UPDATE_MIN_INTERVAL_MS = 200L;
    
    // Size limits to prevent memory leaks
    private static final int MAX_CACHE_SIZE = 1000;
    private static final int MAX_ENTITY_LIGHTS = 500;
    
    // Track current level to detect world changes
    private static WeakReference<Level> lastLevel = new WeakReference<>(null);

    public static void init() {
        if (initialized) return;
        initialized = true;
        
        // Check if already permanently disabled
        if (permanentlyDisabled) {
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] LambDynLights integration permanently disabled due to previous errors");
            return;
        }

        // Discover LambDynLights implementation
        try {
            Class<?> lambClass = Class.forName("dev.lambdaurora.lambdynlights.LambDynLights");
            try {
                Method get = lambClass.getMethod("get");
                dynamicLightsInstance = get.invoke(null);
                MatrixCraftMod.LOGGER.info("[DynamicLightManager] LambDynLights detected and singleton obtained.");
                discoverDynamicLightsApi();
                // Only set available if we successfully discovered the API
                if (dynamicLightSourceClass != null && methodAddLightSource != null) {
                    dynamicLightsAvailable = true;
                    MatrixCraftMod.LOGGER.info("[DynamicLightManager] LambDynLights API successfully initialized.");
                } else {
                    MatrixCraftMod.LOGGER.warn("[DynamicLightManager] LambDynLights detected but API discovery failed!");
                    permanentlyDisableDynamicLights("API discovery failed");
                }
                return;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                MatrixCraftMod.LOGGER.debug("[DynamicLightManager] LambDynLights reflection failed: " + ex.getMessage());
                permanentlyDisableDynamicLights("Reflection failed: " + ex.getMessage());
            }
        } catch (ClassNotFoundException e) {
            // Expected when LambDynLights is not installed - not an error
            MatrixCraftMod.LOGGER.debug("[DynamicLightManager] LambDynLights not found (expected if not installed)");
        }

        dynamicLightsAvailable = false;
        MatrixCraftMod.LOGGER.info("[DynamicLightManager] No dynamic-lights mod found; using particle glow only.");
    }
    
    /**
     * Permanently disable LambDynLights integration on any error to prevent crashes
     */
    private static void permanentlyDisableDynamicLights(String reason) {
        permanentlyDisabled = true;
        dynamicLightsAvailable = false;
        dynamicLightsInstance = null;
        methodAddLightSource = null;
        methodRemoveLightSource = null;
        methodUpdateTracking = null;
        methodClearLightSources = null;
        methodUpdateAll = null;
        dynamicLightSourceClass = null;
        
        MatrixCraftMod.LOGGER.error("╔════════════════════════════════════════════════════════════════╗");
        MatrixCraftMod.LOGGER.error("║ LambDynLights integration PERMANENTLY DISABLED                ║");
        MatrixCraftMod.LOGGER.error("║ Reason: " + String.format("%-54s", reason) + " ║");
        MatrixCraftMod.LOGGER.error("║                                                                ║");
        MatrixCraftMod.LOGGER.error("║ This is NOT a bug - it's a safety feature to prevent crashes. ║");
        MatrixCraftMod.LOGGER.error("║ Bullet trail lighting will still work via shaders/particles.   ║");
        MatrixCraftMod.LOGGER.error("╚════════════════════════════════════════════════════════════════╝");
    }

    private static void discoverDynamicLightsApi() {
        if (dynamicLightsInstance == null) return;
        Class<?> cls = dynamicLightsInstance.getClass();

        for (Method m : cls.getMethods()) {
            String n = m.getName().toLowerCase();
            if ((n.equals("addlightsource") || n.equals("addlight")) && m.getParameterCount() == 1) {
                methodAddLightSource = m;
            } else if ((n.equals("removelightsource") || n.equals("removelight")) && m.getParameterCount() == 1) {
                methodRemoveLightSource = m;
            } else if ((n.equals("updatetracking") || n.equals("updatelight") || n.equals("update")) && m.getParameterCount() == 1) {
                methodUpdateTracking = m;
            } else if ((n.equals("clearlightsources") || n.equals("clearlights")) && m.getParameterCount() == 0) {
                methodClearLightSources = m;
            } else if ((n.equals("updateall") || n.equals("update_all") || n.equals("updatealltracked")) && m.getParameterCount() == 1) {
                methodUpdateAll = m;
            }
        }

        if (methodAddLightSource != null) {
            Class<?> p = methodAddLightSource.getParameterTypes()[0];
            dynamicLightSourceClass = p;
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] Identified DynamicLightSource class: " + p.getName());
        }

        MatrixCraftMod.LOGGER.info("[DynamicLightManager] Methods -> add:" + (methodAddLightSource != null) +
                " remove:" + (methodRemoveLightSource != null) + " update:" + (methodUpdateTracking != null) +
                " clear:" + (methodClearLightSources != null) + " updateAll:" + (methodUpdateAll != null));
    }

    public static boolean isDynamicLightsModAvailable() {
        if (permanentlyDisabled) return false;
        if (!initialized) init();
        return dynamicLightsAvailable;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;

        // Check for world change and clear if changed
        Level currentLevel = mc.level;
        Level lastLevelRef = lastLevel.get();
        if (lastLevelRef != currentLevel) {
            clearAllDynamicLights();
            lastLevel = new WeakReference<>(currentLevel);
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] World changed, cleared all lights");
        }

        BulletTrailLighting.tick();

        if (isDynamicLightsModAvailable()) {
            syncDynamicLights(mc.level);
        }

        // Sweep tracked entity references and remove lights whose entities are gone or unseen for TTL
        try {
            long now = System.currentTimeMillis();
            for (Iterator<Map.Entry<Integer, WeakReference<Entity>>> it = entityRefs.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Integer, WeakReference<Entity>> entry = it.next();
                int id = entry.getKey();
                WeakReference<Entity> ref = entry.getValue();
                Entity e = ref == null ? null : ref.get();
                boolean unseenTooLong = false;
                Long last = lastSeenMs.get(id);
                if (last == null || now - last > ENTITY_TTL_MS) unseenTooLong = true;

                if (e == null || e.isRemoved() || !e.isAlive() || unseenTooLong) {
                    untrackEntityLightById(id);
                    it.remove();
                    lastSeenMs.remove(id);
                }
            }
        } catch (Throwable e) {
            MatrixCraftMod.LOGGER.debug("[DynamicLightManager] Entity light sweep error: " + e.getMessage());
        }
        
        // Check size limits and cleanup if needed
        enforceMemoryLimits();
    }

    private static void syncDynamicLights(Level level) {
        Map<BlockPos, BulletTrailLighting.LightSource> sources = BulletTrailLighting.getActiveLights();

        MatrixCraftMod.LOGGER.debug("[DynamicLightManager] Active lights count: " + (sources == null ? 0 : sources.size()));

        Set<BlockPos> toRemove = new HashSet<>();
        for (BlockPos pos : dlsCache.keySet()) {
            if (sources == null || !sources.containsKey(pos)) toRemove.add(pos);
        }
        for (BlockPos pos : toRemove) {
            Object dls = dlsCache.remove(pos);
            if (dls != null) {
                invokeRemoveLightSource(dls);
            }
        }

        if (sources == null) return;

        for (Map.Entry<BlockPos, BulletTrailLighting.LightSource> e : sources.entrySet()) {
            BlockPos pos = e.getKey();
            BulletTrailLighting.LightSource light = e.getValue();

            Object dls = dlsCache.get(pos);
            if (dls == null) {
                dls = createDynamicLightSource(level, pos, light);
                if (dls != null) {
                    dlsCache.put(pos, dls);
                    invokeAddLightSource(dls);
                }
            } else {
                if (methodUpdateTracking != null) {
                    try {
                        methodUpdateTracking.invoke(dynamicLightsInstance, dls);
                    } catch (IllegalAccessException | InvocationTargetException ex) {
                        MatrixCraftMod.LOGGER.info("[DynamicLightManager] updateTracking invocation failed: " + ex.getMessage());
                    }
                } else {
                    invokeAddLightSource(dls);
                }
            }
        }
    }

    private static void invokeAddLightSource(Object dls) {
        if (permanentlyDisabled || dynamicLightsInstance == null || methodAddLightSource == null) return;
        try {
            methodAddLightSource.invoke(dynamicLightsInstance, dls);
        } catch (Throwable ex) {
            permanentlyDisableDynamicLights("addLightSource invocation failed: " + ex.getMessage());
        }
    }

    private static void invokeRemoveLightSource(Object dls) {
        if (permanentlyDisabled || dynamicLightsInstance == null) return;
        try {
            if (methodRemoveLightSource != null) {
                methodRemoveLightSource.invoke(dynamicLightsInstance, dls);
            }
        } catch (Throwable ex) {
            permanentlyDisableDynamicLights("removeLightSource invocation failed: " + ex.getMessage());
        }
    }

    private static Object createDynamicLightSource(Level level, BlockPos pos, BulletTrailLighting.LightSource light) {
        if (permanentlyDisabled || dynamicLightSourceClass == null) return null;

        final double dx = pos.getX() + 0.5;
        final double dy = pos.getY() + 0.5;
        final double dz = pos.getZ() + 0.5;

        try {
            InvocationHandler handler = new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    String name = method.getName();
                    if ("equals".equals(name) && args != null && args.length == 1) return proxy == args[0];
                    if ("hashCode".equals(name)) return System.identityHashCode(proxy);
                    if ("toString".equals(name)) return "DynamicLightProxy[pos=" + pos + "]";
                    if (name.equals("ryoamicLights$getDynamicLightX") || name.equals("getDynamicLightX")) return dx;
                    if (name.equals("ryoamicLights$getDynamicLightY") || name.equals("getDynamicLightY")) return dy;
                    if (name.equals("ryoamicLights$getDynamicLightZ") || name.equals("getDynamicLightZ")) return dz;
                    if (name.equals("ryoamicLights$getDynamicLightWorld") || name.equals("getDynamicLightWorld")) return level;
                    if (name.equals("ryoamicLights$getLuminance") || name.equals("getLuminance")) return light.getCurrentBrightness();
                    if (name.equals("ryoamicLights$isDynamicLightEnabled") || name.equals("isDynamicLightEnabled")) return true;
                    if (name.equals("ryoamiclights$updateDynamicLight") || name.equals("updateDynamicLight")) return true;
                    return getDefaultReturn(method.getReturnType());
                }
            };
            Object proxy = Proxy.newProxyInstance(dynamicLightSourceClass.getClassLoader(),
                    new Class[]{dynamicLightSourceClass}, handler);
            return proxy;
        } catch (Throwable t) {
            permanentlyDisableDynamicLights("Proxy creation failed: " + t.getMessage());
            return null;
        }
    }

    // -----------------------
    // ENTITY-TRACKING + CHAINED PROXIES
    // -----------------------

    /**
     * Ping an entity id to mark it as recently seen.
     */
    public static void pingEntity(int id) {
        lastSeenMs.put(id, System.currentTimeMillis());
    }

    /**
     * Track an entity with a single proxy (existing behavior)
     */
    public static void trackEntityLight(Entity entity, int luminance, float r, float g, float b) {
        if (entity == null) return;
        if (permanentlyDisabled) return; // Kill switch check
        if (!isDynamicLightsModAvailable()) return;
        ensureInit();
        
        // Additional safety check - verify API is properly initialized
        if (dynamicLightSourceClass == null || methodAddLightSource == null) {
            MatrixCraftMod.LOGGER.debug("[DynamicLightManager] API not initialized, skipping entity light tracking");
            return;
        }
        
        int id = entity.getId();

        try {
            if (entityDls.containsKey(id)) {
                // refresh ref and ping
                entityRefs.put(id, new WeakReference<>(entity));
                lastSeenMs.put(id, System.currentTimeMillis());
                Object existing = entityDls.get(id);
                if (existing != null && methodUpdateTracking != null) {
                    try { 
                        methodUpdateTracking.invoke(dynamicLightsInstance, existing); 
                    } catch (Throwable e) {
                        permanentlyDisableDynamicLights("Update tracking failed: " + e.getMessage());
                    }
                }
                return;
            }

            entityRefs.put(id, new WeakReference<>(entity));
            lastSeenMs.put(id, System.currentTimeMillis());

            InvocationHandler handler = createEntityHandler(id, -1, 0.0, r, g, b);
            Object proxy = Proxy.newProxyInstance(dynamicLightSourceClass.getClassLoader(),
                    new Class[]{dynamicLightSourceClass}, handler);
            entityDls.put(id, proxy);
            invokeAddLightSource(proxy);
        } catch (Throwable t) {
            permanentlyDisableDynamicLights("trackEntityLight proxy creation failed: " + t.getMessage());
        }
    }

    /**
     * Track an entity with a chain of N proxies offset backwards along the bullet velocity.
     * count >=1, spacing in blocks (double).
     */
    public static void trackEntityLightChain(Entity entity, int count, double spacing, int luminance, float r, float g, float b) {
        if (entity == null) return;
        if (permanentlyDisabled) return; // Kill switch check
        if (!isDynamicLightsModAvailable()) return;
        ensureInit();
        
        // Additional safety check - verify API is properly initialized
        if (dynamicLightSourceClass == null || methodAddLightSource == null) {
            MatrixCraftMod.LOGGER.debug("[DynamicLightManager] API not initialized, skipping entity light chain tracking");
            return;
        }
        
        int id = entity.getId();

        // don't double-register chain if exists
        if (entityDlsChains.containsKey(id)) {
            entityRefs.put(id, new WeakReference<>(entity));
            lastSeenMs.put(id, System.currentTimeMillis());
            return;
        }

        List<Object> proxies = new ArrayList<>();
        try {
            entityRefs.put(id, new WeakReference<>(entity));
            lastSeenMs.put(id, System.currentTimeMillis());

            for (int i = 0; i < count; i++) {
                InvocationHandler handler = createEntityHandler(id, i, spacing, r, g, b);
                Object proxy = Proxy.newProxyInstance(dynamicLightSourceClass.getClassLoader(),
                        new Class[]{dynamicLightSourceClass}, handler);
                proxies.add(proxy);
                invokeAddLightSource(proxy);
                
                // Check if we got disabled during invocation
                if (permanentlyDisabled) {
                    // Clean up what we created
                    for (Object p : proxies) {
                        try { 
                            invokeRemoveLightSource(p); 
                        } catch (Throwable e) {
                            MatrixCraftMod.LOGGER.debug("[DynamicLightManager] Failed to remove light during cleanup: " + e.getMessage());
                        }
                    }
                    return;
                }
            }
            entityDlsChains.put(id, proxies);
        } catch (Throwable t) {
            permanentlyDisableDynamicLights("trackEntityLightChain proxy creation failed: " + t.getMessage());
            for (Object p : proxies) {
                try { 
                    invokeRemoveLightSource(p); 
                } catch (Throwable e) {
                    MatrixCraftMod.LOGGER.debug("[DynamicLightManager] Failed to remove light during error cleanup: " + e.getMessage());
                }
            }
        }
    }

    private static InvocationHandler createEntityHandler(final int id, final int chainIndex, final double spacing, final float r, final float g, final float b) {
        return new InvocationHandler() {
            private boolean enabled = true;
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (permanentlyDisabled) {
                    // Return safe defaults if permanently disabled
                    return getDefaultReturn(method.getReturnType());
                }
                
                String name = method.getName();

                if ("equals".equals(name) && args != null && args.length == 1) return proxy == args[0];
                if ("hashCode".equals(name)) return System.identityHashCode(proxy);
                if ("toString".equals(name)) return "EntityDLS[id=" + id + ",idx=" + chainIndex + "]";

                WeakReference<Entity> wr = entityRefs.get(id);
                Entity ent = wr == null ? null : wr.get();

                if (name.equals("ryoamicLights$getDynamicLightX") || name.equals("getDynamicLightX")) {
                    Vec3 p = computeProxyPosition(ent, chainIndex, spacing);
                    return p == null ? 0.0 : p.x;
                }
                if (name.equals("ryoamicLights$getDynamicLightY") || name.equals("getDynamicLightY")) {
                    Vec3 p = computeProxyPosition(ent, chainIndex, spacing);
                    return p == null ? 0.0 : p.y;
                }
                if (name.equals("ryoamicLights$getDynamicLightZ") || name.equals("getDynamicLightZ")) {
                    Vec3 p = computeProxyPosition(ent, chainIndex, spacing);
                    return p == null ? 0.0 : p.z;
                }
                if (name.equals("ryoamicLights$getDynamicLightWorld") || name.equals("getDynamicLightWorld")) {
                    return ent == null ? null : ent.level();
                }

                if (name.equals("ryoamicLights$getLuminance") || name.equals("getLuminance")) {
                    try {
                        if (ent != null) {
                            BlockPos p = BlockPos.containing(ent.getX(), ent.getY(), ent.getZ());
                            BulletTrailLighting.LightSource near = BulletTrailLighting.getNearestLight(p, 3.0);
                            if (near != null) return near.getCurrentBrightness();
                        }
                    } catch (Throwable e) {
                        MatrixCraftMod.LOGGER.debug("[DynamicLightManager] Luminance calculation error: " + e.getMessage());
                    }
                    return BulletTrailLighting.getConfiguredLightLevel();
                }

                if (name.equals("ryoamicLights$isDynamicLightEnabled") || name.equals("isDynamicLightEnabled")) return enabled;
                if (name.equals("ryoamicLights$setDynamicLightEnabled") || name.equals("setDynamicLightEnabled")) {
                    boolean e = (boolean) args[0];
                    enabled = e;
                    try {
                        if (dynamicLightsInstance != null && !permanentlyDisabled) {
                            if (e && methodAddLightSource != null) methodAddLightSource.invoke(dynamicLightsInstance, proxy);
                            else if (!e && methodRemoveLightSource != null) methodRemoveLightSource.invoke(dynamicLightsInstance, proxy);
                        }
                    } catch (Throwable ex) {
                        permanentlyDisableDynamicLights("setDynamicLightEnabled failed: " + ex.getMessage());
                    }
                    return null;
                }
                if (name.equals("ryoamiclights$updateDynamicLight") || name.equals("updateDynamicLight")) {
                    try {
                        if (ent == null || ent.isRemoved() || !ent.isAlive()) return false;
                    } catch (Throwable e) {
                        MatrixCraftMod.LOGGER.debug("[DynamicLightManager] Update light check failed: " + e.getMessage());
                    }
                    return true;
                }
                return getDefaultReturn(method.getReturnType());
            }
        };
    }

    private static Vec3 computeProxyPosition(Entity ent, int chainIndex, double spacing) {
        if (ent == null) return null;
        Vec3 base = ent.position();
        if (chainIndex < 0) return base;
        Vec3 vel = ent.getDeltaMovement();
        double vx = vel.x, vy = vel.y, vz = vel.z;
        double len = Math.sqrt(vx*vx + vy*vy + vz*vz);
        if (len <= 1e-6) return base;
        Vec3 dir = new Vec3(vx/len, vy/len, vz/len);
        double offset = chainIndex * spacing;
        return base.subtract(dir.scale(offset));
    }

    /**
     * Unregister a tracked entity light by id (removes single proxy and any chain proxies).
     */
    public static void untrackEntityLightById(int id) {
        Object single = entityDls.remove(id);
        if (single != null) {
            try { 
                invokeRemoveLightSource(single); 
            } catch (Throwable e) {
                MatrixCraftMod.LOGGER.debug("[DynamicLightManager] Remove single light failed for id=" + id + ": " + e.getMessage());
            }
        }
        List<Object> chain = entityDlsChains.remove(id);
        if (chain != null) {
            for (Object p : chain) {
                try { 
                    invokeRemoveLightSource(p); 
                } catch (Throwable e) {
                    MatrixCraftMod.LOGGER.debug("[DynamicLightManager] Remove chain light failed for id=" + id + ": " + e.getMessage());
                }
            }
        }
        entityRefs.remove(id);
        lastSeenMs.remove(id);
    }

    /**
     * Clear all dynamic lights from the dynamic-lights implementation and internal caches.
     */
    public static void clearAllDynamicLights() {
        try {
            if (methodClearLightSources != null && dynamicLightsInstance != null) {
                try {
                    methodClearLightSources.invoke(dynamicLightsInstance);
                    MatrixCraftMod.LOGGER.info("[DynamicLightManager] Invoked clearLightSources on dynamic-lights mod.");
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    MatrixCraftMod.LOGGER.info("[DynamicLightManager] clearLightSources invocation failed: " + ex.getMessage());
                }
            } else {
                for (Object p : dlsCache.values()) invokeRemoveLightSource(p);
                dlsCache.clear();
                for (Object p : entityDls.values()) invokeRemoveLightSource(p);
                entityDls.clear();
                for (List<Object> list : entityDlsChains.values()) for (Object p : list) invokeRemoveLightSource(p);
                entityDlsChains.clear();
                entityRefs.clear();
                lastSeenMs.clear();
                MatrixCraftMod.LOGGER.info("[DynamicLightManager] Cleared all cached dynamic-lights (fallback).");
            }
        } catch (Throwable t) {
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] clearAllDynamicLights failed: " + t.getMessage());
        }
    }

    /**
     * Force the dynamic-lights implementation to update all tracked sources (throttled).
     */
    public static void forceUpdateAll() {
        if (!isDynamicLightsModAvailable()) return;
        ensureInit();
        long now = System.currentTimeMillis();
        if (now - lastForceUpdateMs < FORCE_UPDATE_MIN_INTERVAL_MS) return;
        lastForceUpdateMs = now;

        if (methodUpdateAll != null && dynamicLightsInstance != null) {
            try {
                methodUpdateAll.invoke(dynamicLightsInstance, Minecraft.getInstance().levelRenderer);
                MatrixCraftMod.LOGGER.info("[DynamicLightManager] forceUpdateAll invoked on dynamic-lights mod.");
            } catch (IllegalAccessException | InvocationTargetException ex) {
                MatrixCraftMod.LOGGER.info("[DynamicLightManager] forceUpdateAll invocation failed: " + ex.getMessage());
            }
        }
    }

    private static Object getDefaultReturn(Class<?> ret) {
        if (ret == boolean.class) return false;
        if (ret == int.class) return 0;
        if (ret == long.class) return 0L;
        if (ret == double.class) return 0.0;
        return null;
    }

    public static void ensureInit() {
        if (!initialized) init();
    }
    
    /**
     * Enforce memory limits to prevent unbounded growth
     */
    private static void enforceMemoryLimits() {
        // Limit dlsCache size
        if (dlsCache.size() > MAX_CACHE_SIZE) {
            // Remove entries to bring size down to 80% of limit
            int targetSize = (MAX_CACHE_SIZE * 4) / 5; 
            int toRemove = dlsCache.size() - targetSize;
            
            List<BlockPos> positions = new ArrayList<>(dlsCache.keySet());
            for (int i = 0; i < toRemove && i < positions.size(); i++) {
                BlockPos pos = positions.get(i);
                Object dls = dlsCache.remove(pos);
                if (dls != null) {
                    invokeRemoveLightSource(dls);
                }
            }
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] Cleaned up " + toRemove + " lights from cache (limit exceeded)");
        }
        
        // Limit entity lights size
        int totalEntityLights = entityDls.size() + entityDlsChains.values().stream().mapToInt(List::size).sum();
        if (totalEntityLights > MAX_ENTITY_LIGHTS) {
            int targetSize = (MAX_ENTITY_LIGHTS * 4) / 5;
            int toRemove = Math.max(1, totalEntityLights - targetSize); // Ensure at least 1 removed
            
            // Remove oldest entity lights first
            List<Integer> entityIds = new ArrayList<>(lastSeenMs.keySet());
            entityIds.sort((a, b) -> {
                Long timeA = lastSeenMs.get(a);
                Long timeB = lastSeenMs.get(b);
                if (timeA == null) return -1;
                if (timeB == null) return 1;
                return timeA.compareTo(timeB);
            });
            
            for (int i = 0; i < toRemove && i < entityIds.size(); i++) {
                untrackEntityLightById(entityIds.get(i));
            }
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] Cleaned up " + toRemove + " entity lights (limit exceeded)");
        }
    }
}