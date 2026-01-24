package com.raeyncraft.matrixcraft.command;

/**
 * Stores runtime settings that can be toggled via commands.
 * These settings are per-session and reset when the game restarts.
 */
public class MatrixSettings {
    
    // Cobweb settings
    private static boolean cobwebsEnabled = true; // Default: cobwebs work normally
    
    /**
     * Check if cobwebs should slow the player
     */
    public static boolean areCobwebsEnabled() {
        return cobwebsEnabled;
    }
    
    /**
     * Set whether cobwebs should slow the player
     */
    public static void setCobwebsEnabled(boolean enabled) {
        cobwebsEnabled = enabled;
    }
    
    /**
     * Toggle cobwebs on/off
     */
    public static boolean toggleCobwebs() {
        cobwebsEnabled = !cobwebsEnabled;
        return cobwebsEnabled;
    }
}
