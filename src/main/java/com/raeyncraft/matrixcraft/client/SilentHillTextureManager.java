package com.raeyncraft.matrixcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
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
        
        Minecraft mc = Minecraft.getInstance();
        PackRepository repo = mc.getResourcePackRepository();
        
        // Get current enabled packs
        List<String> enabled = new ArrayList<>(repo.getSelectedIds());
        
        // If Silent Hill pack isn't enabled, add it to TOP priority
        if (!enabled.contains("matrixcraft:silent_hill")) {
            enabled.add(0, "matrixcraft:silent_hill"); // Top priority = override others
            repo.setSelected(enabled);
            
            // This is the key: ASYNC reload to avoid stutter
            mc.execute(() -> mc.reloadResourcePacks());
        }
        
        silentHillActive = true;
    }
    
    /**
     * Disable Silent Hill textures
     */
    public static void disable() {
        if (!silentHillActive) return;
        
        Minecraft mc = Minecraft.getInstance();
        PackRepository repo = mc.getResourcePackRepository();
        
        List<String> enabled = new ArrayList<>(repo.getSelectedIds());
        enabled.remove("matrixcraft:silent_hill");
        repo.setSelected(enabled);
        
        mc.execute(() -> mc.reloadResourcePacks());
        
        silentHillActive = false;
    }
    
    public static boolean isActive() {
        return silentHillActive;
    }
}