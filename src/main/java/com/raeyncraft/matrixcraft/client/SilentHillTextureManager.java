package com.raeyncraft.matrixcraft.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class SilentHillTextureManager {
    
    private static boolean silentHillActive = false;
    
    /**
     * Enable Silent Hill textures WITHOUT reloading
     * This just reorders the pack priority - already loaded!
     */
    public static void enable() {
        if (silentHillActive) return;
        
        // Silent Hill textures disabled to prevent green screen
        // Minecraft mc = Minecraft.getInstance();
        // PackRepository repo = mc.getResourcePackRepository();
        // List<String> enabled = new ArrayList<>(repo.getSelectedIds());
        // if (!enabled.contains("matrixcraft:silent_hill")) {
        //     enabled.add(0, "matrixcraft:silent_hill");
        //     repo.setSelected(enabled);
        //     mc.execute(() -> mc.reloadResourcePacks());
        // }
        
        silentHillActive = true;
    }
    
    /**
     * Disable Silent Hill textures
     */
    public static void disable() {
        if (!silentHillActive) return;
        
        // Minecraft mc = Minecraft.getInstance();
        // PackRepository repo = mc.getResourcePackRepository();
        // List<String> enabled = new ArrayList<>(repo.getSelectedIds());
        // enabled.remove("matrixcraft:silent_hill");
        // repo.setSelected(enabled);
        // mc.execute(() -> mc.reloadResourcePacks());
        
        silentHillActive = false;
    }
    
    public static boolean isActive() {
        return silentHillActive;
    }
}