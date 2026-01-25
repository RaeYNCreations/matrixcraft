package com.raeyncraft.matrixcraft.bullettime;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client-only state holder for Focus mode.
 * This class is completely stripped on dedicated servers,
 * preventing NoSuchFieldError crashes.
 */
@OnlyIn(Dist.CLIENT)
public class FocusClientState {
    
    private static boolean clientInFocus = false;
    private static int clientFocusTicksRemaining = 0;
    private static int clientFocusMaxTicks = 0;
    
    public static void setFocusState(boolean active, int ticksRemaining, int maxTicks) {
        clientInFocus = active;
        clientFocusTicksRemaining = ticksRemaining;
        clientFocusMaxTicks = maxTicks;
    }
    
    public static void tick() {
        if (clientInFocus && clientFocusTicksRemaining > 0) {
            clientFocusTicksRemaining--;
            
            if (clientFocusTicksRemaining <= 0) {
                clientInFocus = false;
            }
        }
    }
    
    public static boolean isClientInFocus() {
        return clientInFocus;
    }
    
    public static float getProgress() {
        if (clientFocusMaxTicks <= 0) return 0f;
        return (float) clientFocusTicksRemaining / (float) clientFocusMaxTicks;
    }
    
    public static int getTicksRemaining() {
        return clientFocusTicksRemaining;
    }
    
    public static void reset() {
        clientInFocus = false;
        clientFocusTicksRemaining = 0;
        clientFocusMaxTicks = 0;
    }
}
