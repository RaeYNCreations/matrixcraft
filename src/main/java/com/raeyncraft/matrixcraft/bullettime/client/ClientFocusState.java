package com.raeyncraft.matrixcraft.bullettime.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientFocusState {

    private static boolean inFocus;
    private static int ticksRemaining;
    private static int maxTicks;

    public static void set(boolean active, int remaining, int max) {
        inFocus = active;
        ticksRemaining = remaining;
        maxTicks = max;
    }

    public static void clientTick() {
        if (inFocus && ticksRemaining > 0) {
            ticksRemaining--;
            if (ticksRemaining <= 0) {
                inFocus = false;
            }
        }
    }

    public static boolean isInFocus() {
        return inFocus;
    }

    public static float getProgress() {
        return maxTicks <= 0 ? 0f : (float) ticksRemaining / maxTicks;
    }
}
