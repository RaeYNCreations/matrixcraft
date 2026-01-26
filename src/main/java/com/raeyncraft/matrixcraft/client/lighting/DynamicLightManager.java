package com.raeyncraft.matrixcraft.client.lighting;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.client.BulletTrailLighting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.lang.reflect.*;
import java.util.*;

import sun.misc.Unsafe;
import sun.reflect.ReflectionFactory;

/**
 * Dynamic Light Manager (proxy-based DynamicLightSource)
 *
 * Uses a java.lang.reflect.Proxy to implement org.thinkingstudio.ryoamiclights.DynamicLightSource
 * at runtime when Ryoamic's concrete implementations are not available.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT, modid = MatrixCraftMod.MODID)
public class DynamicLightManager {

    private static boolean initialized = false;
    private static boolean dynamicLightsAvailable = false;

    private static Object dynamicLightsInstance = null;
    private static Method methodAddLightSource = null;
    private static Method methodRemoveLightSource = null;
    private static Method methodUpdateTracking = null;

    private static Class<?> dynamicLightSourceClass = null;

    private static final Map<BlockPos, Object> dlsCache = new HashMap<>();

    public static void init() {
        if (initialized) return;
        initialized = true;

        try {
            Class<?> ryoClass = Class.forName("org.thinkingstudio.ryoamiclights.RyoamicLights");
            Method get = ryoClass.getMethod("get");
            dynamicLightsInstance = get.invoke(null);
            dynamicLightsAvailable = true;
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] RyoamicLights detected and singleton obtained.");
            discoverRyoamicApi();
            return;
        } catch (ClassNotFoundException cnfe) {
        } catch (Throwable t) {
            MatrixCraftMod.LOGGER.debug("[DynamicLightManager] Error obtaining RyoamicLights singleton: " + t.getMessage());
        }

        try {
            Class<?> lambClass = Class.forName("dev.lambdaurora.lambdynlights.LambDynLights");
            Method get = lambClass.getMethod("get");
            dynamicLightsInstance = get.invoke(null);
            dynamicLightsAvailable = true;
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] LambDynLights detected and singleton obtained.");
            discoverRyoamicApi();
            return;
        } catch (ClassNotFoundException cnfe) {
        } catch (Throwable t) {
            MatrixCraftMod.LOGGER.debug("[DynamicLightManager] Error obtaining LambDynLights singleton: " + t.getMessage());
        }

        dynamicLightsAvailable = false;
        MatrixCraftMod.LOGGER.info("[DynamicLightManager] No dynamic-lights mod found; using particle glow only.");
    }

    private static void discoverRyoamicApi() {
        if (dynamicLightsInstance == null) return;
        Class<?> cls = dynamicLightsInstance.getClass();
        MatrixCraftMod.LOGGER.info("[DynamicLightManager] Dumping methods on detected singleton: " + cls.getName());
        for (Method m : cls.getMethods()) {
            StringBuilder sig = new StringBuilder();
            sig.append(m.getReturnType().getSimpleName()).append(" ").append(m.getName()).append("(");
            Class<?>[] pts = m.getParameterTypes();
            for (int i = 0; i < pts.length; i++) {
                sig.append(pts[i].getCanonicalName());
                if (i < pts.length - 1) sig.append(", ");
            }
            sig.append(")");
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] API Method: " + sig.toString());
        }

        for (Method m : cls.getMethods()) {
            String n = m.getName().toLowerCase();
            if ((n.equals("addlightsource") || n.equals("addlight")) && m.getParameterCount() == 1) {
                methodAddLightSource = m;
            } else if ((n.equals("removelightsource") || n.equals("removelight")) && m.getParameterCount() == 1) {
                methodRemoveLightSource = m;
            } else if ((n.equals("updatetracking") || n.equals("updatelight") || n.equals("update")) && m.getParameterCount() == 1) {
                methodUpdateTracking = m;
            }
        }

        if (methodAddLightSource != null) {
            Class<?> p = methodAddLightSource.getParameterTypes()[0];
            dynamicLightSourceClass = p;
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] Identified DynamicLightSource class: " + p.getName());
            dumpDynamicLightSourceShape(p);
        } else {
            try {
                dynamicLightSourceClass = Class.forName("org.thinkingstudio.ryoamiclights.DynamicLightSource");
                dumpDynamicLightSourceShape(dynamicLightSourceClass);
            } catch (ClassNotFoundException ignored) {
            }
        }

        MatrixCraftMod.LOGGER.info("[DynamicLightManager] Methods -> add:" + (methodAddLightSource != null) +
                " remove:" + (methodRemoveLightSource != null) + " update:" + (methodUpdateTracking != null));
    }

    private static void dumpDynamicLightSourceShape(Class<?> dlsClass) {
        if (dlsClass == null) return;
        MatrixCraftMod.LOGGER.info("[DynamicLightManager] DynamicLightSource class: " + dlsClass.getName());
        try {
            for (Constructor<?> c : dlsClass.getDeclaredConstructors()) {
                StringBuilder sb = new StringBuilder();
                sb.append("ctor ").append(c.getName()).append("(");
                Class<?>[] pts = c.getParameterTypes();
                for (int i = 0; i < pts.length; i++) {
                    sb.append(pts[i].getCanonicalName());
                    if (i < pts.length - 1) sb.append(", ");
                }
                sb.append(")");
                MatrixCraftMod.LOGGER.info("[DynamicLightManager] DLS Constructor: " + sb.toString());
            }
            for (Field f : dlsClass.getDeclaredFields()) {
                MatrixCraftMod.LOGGER.info("[DynamicLightManager] DLS Field: " + f.getType().getCanonicalName() + " " + f.getName());
            }
            for (Method m : dlsClass.getDeclaredMethods()) {
                StringBuilder sb = new StringBuilder();
                sb.append(m.getReturnType().getSimpleName()).append(" ").append(m.getName()).append("(");
                Class<?>[] pts = m.getParameterTypes();
                for (int i = 0; i < pts.length; i++) {
                    sb.append(pts[i].getCanonicalName());
                    if (i < pts.length - 1) sb.append(", ");
                }
                sb.append(")");
                MatrixCraftMod.LOGGER.info("[DynamicLightManager] DLS Method: " + sb.toString());
            }
        } catch (Throwable t) {
            MatrixCraftMod.LOGGER.debug("[DynamicLightManager] Failed to dump DynamicLightSource shape: " + t.getMessage());
        }
    }

    public static boolean isDynamicLightsModAvailable() {
        if (!initialized) init();
        return dynamicLightsAvailable;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;

        BulletTrailLighting.tick();

        if (isDynamicLightsModAvailable()) {
            syncDynamicLights(mc.level);
        }
    }

    private static void syncDynamicLights(Level level) {
        Map<BlockPos, BulletTrailLighting.LightSource> sources = BulletTrailLighting.getActiveLights();

        MatrixCraftMod.LOGGER.info("[DynamicLightManager] Active lights count: " + (sources == null ? 0 : sources.size()));
        if (sources != null && !sources.isEmpty()) {
            for (Map.Entry<BlockPos, BulletTrailLighting.LightSource> e : sources.entrySet()) {
                MatrixCraftMod.LOGGER.info("[DynamicLightManager] ActiveLight: pos=" + e.getKey() + " brightness=" + e.getValue().getCurrentBrightness());
            }
        }

        Set<BlockPos> toRemove = new HashSet<>();
        for (BlockPos pos : dlsCache.keySet()) {
            if (sources == null || !sources.containsKey(pos)) toRemove.add(pos);
        }
        for (BlockPos pos : toRemove) {
            Object dls = dlsCache.remove(pos);
            if (dls != null) {
                MatrixCraftMod.LOGGER.info("[DynamicLightManager] Removing DLS for pos " + pos);
                invokeRemoveLightSource(dls);
            }
        }

        if (sources == null) return;

        for (Map.Entry<BlockPos, BulletTrailLighting.LightSource> e : sources.entrySet()) {
            BlockPos pos = e.getKey();
            BulletTrailLighting.LightSource light = e.getValue();

            Object dls = dlsCache.get(pos);
            if (dls == null || !containsInstance(dls)) {
                MatrixCraftMod.LOGGER.info("[DynamicLightManager] Attempting to construct DLS for pos " + pos + " brightness=" + light.getCurrentBrightness());
                dls = createDynamicLightSource(level, pos, light);
                if (dls != null) {
                    dlsCache.put(pos, dls);
                    MatrixCraftMod.LOGGER.info("[DynamicLightManager] addLightSource invoked for pos " + pos);
                    invokeAddLightSource(dls);
                } else {
                    MatrixCraftMod.LOGGER.info("[DynamicLightManager] Could not construct DynamicLightSource for pos " + pos);
                }
            } else {
                if (methodUpdateTracking != null) {
                    try {
                        methodUpdateTracking.invoke(dynamicLightsInstance, dls);
                        MatrixCraftMod.LOGGER.info("[DynamicLightManager] updateTracking invoked for pos " + pos);
                    } catch (IllegalAccessException | InvocationTargetException ex) {
                        MatrixCraftMod.LOGGER.info("[DynamicLightManager] updateTracking invocation failed: " + ex.getMessage());
                    }
                } else {
                    MatrixCraftMod.LOGGER.info("[DynamicLightManager] Re-invoking addLightSource (fallback) for pos " + pos);
                    invokeAddLightSource(dls);
                }
            }
        }
    }

    private static boolean containsInstance(Object dls) {
        return true;
    }

    private static void invokeAddLightSource(Object dls) {
        if (dynamicLightsInstance == null || methodAddLightSource == null) return;
        try {
            methodAddLightSource.invoke(dynamicLightsInstance, dls);
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] addLightSource invocation succeeded.");
        } catch (IllegalAccessException | InvocationTargetException ex) {
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] addLightSource invocation failed: " + ex.getMessage());
        }
    }

    private static void invokeRemoveLightSource(Object dls) {
        if (dynamicLightsInstance == null) return;
        try {
            if (methodRemoveLightSource != null) {
                methodRemoveLightSource.invoke(dynamicLightsInstance, dls);
                MatrixCraftMod.LOGGER.info("[DynamicLightManager] removeLightSource invocation succeeded.");
            } else {
                MatrixCraftMod.LOGGER.info("[DynamicLightManager] removeLightSource not available; no fallback implemented.");
            }
        } catch (IllegalAccessException | InvocationTargetException ex) {
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] removeLightSource invocation failed: " + ex.getMessage());
        }
    }

    private static Method findMethodByName(Class<?> cls, String name, int paramCount) {
        for (Method m : cls.getMethods()) {
            if (m.getName().equalsIgnoreCase(name) && m.getParameterCount() == paramCount) return m;
        }
        return null;
    }

    /**
     * Create a DynamicLightSource instance.
     * First attempt: create a java.lang.reflect.Proxy implementing the DynamicLightSource interface.
     * If proxy creation fails, fall back to constructor attempts and allocation fallbacks (kept for safety).
     */
    private static Object createDynamicLightSource(Level level, BlockPos pos, BulletTrailLighting.LightSource light) {
        if (dynamicLightSourceClass == null) {
            try {
                dynamicLightSourceClass = Class.forName("org.thinkingstudio.ryoamiclights.DynamicLightSource");
            } catch (ClassNotFoundException e) {
                MatrixCraftMod.LOGGER.info("[DynamicLightManager] DynamicLightSource class not available: " + e.getMessage());
                return null;
            }
        }

        // Common values
        final double dx = pos.getX() + 0.5;
        final double dy = pos.getY() + 0.5;
        final double dz = pos.getZ() + 0.5;
        final int luminance = light.getCurrentBrightness();
        final Level world = level;

        // 1) Try Proxy implementation (preferred)
        try {
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] Creating DynamicLightSource proxy for pos " + pos + " lum=" + luminance);
            InvocationHandler handler = new InvocationHandler() {
                private boolean enabled = luminance > 0;

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    String name = method.getName();
                    // Object methods
                    if ("equals".equals(name) && args != null && args.length == 1) {
                        return proxy == args[0];
                    }
                    if ("hashCode".equals(name) && (args == null || args.length == 0)) {
                        return System.identityHashCode(proxy);
                    }
                    if ("toString".equals(name) && (args == null || args.length == 0)) {
                        return "DynamicLightProxy[pos=" + pos + ",lum=" + luminance + "]";
                    }

                    // Ryoamic interface methods (support both prefixed and unprefixed names)
                    if (name.equals("ryoamicLights$getDynamicLightX") || name.equals("getDynamicLightX")) {
                        return dx;
                    }
                    if (name.equals("ryoamicLights$getDynamicLightY") || name.equals("getDynamicLightY")) {
                        return dy;
                    }
                    if (name.equals("ryoamicLights$getDynamicLightZ") || name.equals("getDynamicLightZ")) {
                        return dz;
                    }
                    if (name.equals("ryoamicLights$getDynamicLightWorld") || name.equals("getDynamicLightWorld")) {
                        return world;
                    }
                    if (name.equals("ryoamicLights$getLuminance") || name.equals("getLuminance")) {
                        return luminance;
                    }
                    if (name.equals("ryoamicLights$isDynamicLightEnabled") || name.equals("isDynamicLightEnabled")) {
                        return enabled;
                    }
                    if (name.equals("ryoamicLights$setDynamicLightEnabled") || name.equals("setDynamicLightEnabled")) {
                        boolean e = (boolean) args[0];
                        enabled = e;
                        // keep Ryoamic's tracked sources consistent if the proxy toggles enabled
                        try {
                            if (dynamicLightsInstance != null) {
                                if (e && methodAddLightSource != null) {
                                    methodAddLightSource.invoke(dynamicLightsInstance, proxy);
                                } else if (!e && methodRemoveLightSource != null) {
                                    methodRemoveLightSource.invoke(dynamicLightsInstance, proxy);
                                }
                            }
                        } catch (Throwable ignored) {}
                        return null;
                    }
                    if (name.equals("ryoamicLights$resetDynamicLight") || name.equals("resetDynamicLight")) {
                        // noop
                        return null;
                    }
                    if (name.equals("ryoamicLights$dynamicLightTick") || name.equals("dynamicLightTick")) {
                        // noop: Ryoamic expects implementers to update luminance/state over time.
                        return null;
                    }
                    // updateDynamicLight(WorldRenderer) -> boolean
                    if (name.equals("ryoamiclights$updateDynamicLight") || name.equals("updateDynamicLight")) {
                        // Ryoamic uses the boolean result for update count; returning true is safe.
                        return true;
                    }
                    // scheduleTrackedChunksRebuild(WorldRenderer)
                    if (name.equals("ryoamiclights$scheduleTrackedChunksRebuild") || name.equals("scheduleTrackedChunksRebuild")) {
                        // No-op here; Ryoamic will handle chunk rebuild when needed. Return void.
                        return null;
                    }

                    // If method is default or unknown, return a sensible default rather than attempting MethodHandles.
                    Class<?> ret = method.getReturnType();
                    if (ret == boolean.class) return false;
                    if (ret == int.class) return 0;
                    if (ret == long.class) return 0L;
                    if (ret == double.class) return 0.0;
                    return null;
                }
            };

            Object proxy = Proxy.newProxyInstance(dynamicLightSourceClass.getClassLoader(),
                    new Class[]{dynamicLightSourceClass}, handler);

            MatrixCraftMod.LOGGER.info("[DynamicLightManager] Constructed DLS proxy for pos " + pos);
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] DLS Proxy Summary: x=" + dx + " y=" + dy + " z=" + dz + " lum=" + luminance + " world=" + world);
            return proxy;
        } catch (Throwable t) {
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] Proxy creation failed: " + t.getMessage());
        }

        // --- previous constructor/alloc attempts (kept as fallback) ---
        Constructor<?>[] ctors = dynamicLightSourceClass.getDeclaredConstructors();
        Arrays.sort(ctors, new Comparator<Constructor<?>>() {
            @Override
            public int compare(Constructor<?> a, Constructor<?> b) {
                return Integer.compare(b.getParameterCount(), a.getParameterCount());
            }
        });

        int xInt = pos.getX();
        int yInt = pos.getY();
        int zInt = pos.getZ();

        float rFloat = light.red;
        float gFloat = light.green;
        float bFloat = light.blue;

        for (Constructor<?> ctor : ctors) {
            try {
                ctor.setAccessible(true);
                Class<?>[] pts = ctor.getParameterTypes();
                Object[] args = buildArgumentsForConstructor(pts, level, pos, xInt, yInt, zInt, dx, dy, dz, luminance, rFloat, gFloat, bFloat);
                if (args == null) continue;
                Object instance = ctor.newInstance(args);
                MatrixCraftMod.LOGGER.info("[DynamicLightManager] Constructed DLS with ctor: " + ctor);
                tryPopulateFields(instance, level, pos, dx, dy, dz, luminance, rFloat, gFloat, bFloat);
                logDlsState(instance);
                return instance;
            } catch (InvocationTargetException ite) {
                MatrixCraftMod.LOGGER.info("[DynamicLightManager] ctor invocation threw for ctor " + ctor + ": " + ite.getTargetException());
            } catch (Throwable t) {
                MatrixCraftMod.LOGGER.info("[DynamicLightManager] ctor try failed: " + t.getMessage());
            }
        }

        // ReflectionFactory allocation
        try {
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] Attempting allocation via ReflectionFactory (serialization) for " + dynamicLightSourceClass.getName());
            ReflectionFactory reflectionFactory = ReflectionFactory.getReflectionFactory();
            Constructor<?> objCtor = Object.class.getDeclaredConstructor();
            Constructor<?> serCtor = reflectionFactory.newConstructorForSerialization(dynamicLightSourceClass, objCtor);
            serCtor.setAccessible(true);
            Object inst = serCtor.newInstance();
            tryPopulateFields(inst, level, pos, dx, dy, dz, luminance, rFloat, gFloat, bFloat);
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] Constructed DLS via serialization allocation.");
            logDlsState(inst);
            return inst;
        } catch (Throwable t) {
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] ReflectionFactory allocation failed: " + t.getMessage());
        }

        // Unsafe allocation
        try {
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] Attempting allocation via Unsafe.allocateInstance for " + dynamicLightSourceClass.getName());
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Unsafe unsafe = (Unsafe) f.get(null);
            Object inst = unsafe.allocateInstance(dynamicLightSourceClass);
            tryPopulateFields(inst, level, pos, dx, dy, dz, luminance, rFloat, gFloat, bFloat);
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] Constructed DLS via Unsafe allocation.");
            logDlsState(inst);
            return inst;
        } catch (Throwable t) {
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] Unsafe allocation failed: " + t.getMessage());
        }

        MatrixCraftMod.LOGGER.info("[DynamicLightManager] No suitable DLS constructor succeeded and allocation fallbacks failed.");
        return null;
    }

    private static Object[] buildArgumentsForConstructor(Class<?>[] pts,
                                                        Level level, BlockPos pos,
                                                        int xi, int yi, int zi,
                                                        double dx, double dy, double dz,
                                                        int luminance,
                                                        float rFloat, float gFloat, float bFloat) {
        Object[] args = new Object[pts.length];
        int doubleCoordIndex = 0;
        int colorIndex = 0;

        for (int i = 0; i < pts.length; i++) {
            Class<?> p = pts[i];
            if (p.isAssignableFrom(Level.class)) {
                args[i] = level;
            } else if (p.isAssignableFrom(BlockPos.class)) {
                args[i] = pos;
            } else if (p == double.class || p == Double.class) {
                if (doubleCoordIndex == 0) { args[i] = dx; doubleCoordIndex++; }
                else if (doubleCoordIndex == 1) { args[i] = dy; doubleCoordIndex++; }
                else if (doubleCoordIndex == 2) { args[i] = dz; doubleCoordIndex++; }
                else { args[i] = (double) luminance; }
            } else if (p == int.class || p == Integer.class) {
                if (pts.length == 4 && i == 0 && pts[0] == int.class && pts[1] == int.class && pts[2] == int.class && pts[3] == int.class) {
                    return new Object[] { xi, yi, zi, luminance };
                }
                args[i] = luminance;
            } else if (p == float.class || p == Float.class) {
                if (colorIndex == 0) { args[i] = rFloat; colorIndex++; }
                else if (colorIndex == 1) { args[i] = gFloat; colorIndex++; }
                else if (colorIndex == 2) { args[i] = bFloat; colorIndex++; }
                else { args[i] = (float)(luminance / 15f); }
            } else {
                String name = p.getSimpleName().toLowerCase();
                if (name.contains("pos") || name.contains("blockpos")) args[i] = pos;
                else if (name.contains("level") || name.contains("world")) args[i] = level;
                else return null;
            }
        }
        return args;
    }

    private static void tryPopulateFields(Object inst,
                                          Level level, BlockPos pos,
                                          double dx, double dy, double dz,
                                          int luminance,
                                          float rFloat, float gFloat, float bFloat) {
        if (inst == null) return;
        Class<?> cls = inst.getClass();
        Field[] fields = cls.getDeclaredFields();
        for (Field f : fields) {
            try {
                f.setAccessible(true);
                String name = f.getName().toLowerCase();
                Class<?> t = f.getType();
                if ((name.contains("x") || name.contains("posx") || name.contains("dynamiclightx")) && (t == double.class || t == Double.class)) {
                    f.set(inst, dx); continue;
                }
                if ((name.contains("y") || name.contains("posy") || name.contains("dynamiclighty")) && (t == double.class || t == Double.class)) {
                    f.set(inst, dy); continue;
                }
                if ((name.contains("z") || name.contains("posz") || name.contains("dynamiclightz")) && (t == double.class || t == Double.class)) {
                    f.set(inst, dz); continue;
                }
                if ((name.contains("x") || name.contains("posx")) && (t == int.class || t == Integer.class)) {
                    f.set(inst, pos.getX()); continue;
                }
                if ((name.contains("y") || name.contains("posy")) && (t == int.class || t == Integer.class)) {
                    f.set(inst, pos.getY()); continue;
                }
                if ((name.contains("z") || name.contains("posz")) && (t == int.class || t == Integer.class)) {
                    f.set(inst, pos.getZ()); continue;
                }
                if ((name.contains("luminance") || name.contains("light") || name.contains("lumen") || name.contains("level")) && (t == int.class || t == Integer.class)) {
                    f.set(inst, luminance); continue;
                }
                if ((name.contains("world") || name.contains("level") || name.contains("dimension")) && Level.class.isAssignableFrom(t)) {
                    f.set(inst, level); continue;
                }
                if ((name.contains("r") || name.contains("red")) && (t == float.class || t == double.class)) {
                    f.set(inst, (t == double.class) ? (double) rFloat : rFloat); continue;
                }
                if ((name.contains("g") || name.contains("green")) && (t == float.class || t == double.class)) {
                    f.set(inst, (t == double.class) ? (double) gFloat : gFloat); continue;
                }
                if ((name.contains("b") || name.contains("blue")) && (t == float.class || t == double.class)) {
                    f.set(inst, (t == double.class) ? (double) bFloat : bFloat); continue;
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static void logDlsState(Object dls) {
        if (dls == null) return;
        try {
            Class<?> cls = dls.getClass();
            String[] methodNames = {
                    "ryoamicLights$getDynamicLightX",
                    "ryoamicLights$getDynamicLightY",
                    "ryoamicLights$getDynamicLightZ",
                    "ryoamicLights$getLuminance",
                    "ryoamicLights$isDynamicLightEnabled"
            };
            for (String name : methodNames) {
                try {
                    Method m = cls.getDeclaredMethod(name);
                    m.setAccessible(true);
                    Object val = m.invoke(dls);
                    MatrixCraftMod.LOGGER.info("[DynamicLightManager] DLS State: " + name + " = " + String.valueOf(val));
                } catch (NoSuchMethodException nsme) {
                    String alt = name.replace("ryoamicLights$", "");
                    try {
                        Method m2 = cls.getDeclaredMethod(alt);
                        m2.setAccessible(true);
                        Object val = m2.invoke(dls);
                        MatrixCraftMod.LOGGER.info("[DynamicLightManager] DLS State: " + alt + " = " + String.valueOf(val));
                    } catch (NoSuchMethodException ignored) {
                    } catch (Throwable ignored) {
                    }
                } catch (Throwable t) {
                    MatrixCraftMod.LOGGER.info("[DynamicLightManager] DLS state read failed for " + name + ": " + t.getMessage());
                }
            }
        } catch (Throwable t) {
            MatrixCraftMod.LOGGER.info("[DynamicLightManager] logDlsState failed: " + t.getMessage());
        }
    }

    public static void requestChunkRebuild(LevelRenderer renderer, BlockPos pos) {
        if (dynamicLightsAvailable && dynamicLightsInstance != null) {
            Method m = findMethodByName(dynamicLightsInstance.getClass(), "scheduleChunkRebuild", 2);
            if (m != null) {
                try {
                    m.invoke(dynamicLightsInstance, renderer, pos);
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    MatrixCraftMod.LOGGER.info("[DynamicLightManager] scheduleChunkRebuild failed: " + ex.getMessage());
                }
            }
        }
    }

    public static void ensureInit() {
        if (!initialized) init();
    }
}