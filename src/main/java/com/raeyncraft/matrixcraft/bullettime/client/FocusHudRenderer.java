package com.raeyncraft.matrixcraft.bullettime.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.bullettime.FocusManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Renders the Focus bar HUD element and Matrix visual overlay
 */
@EventBusSubscriber(modid = MatrixCraftMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class FocusHudRenderer {
    
    private static final ResourceLocation FOCUS_HUD_LAYER = 
        ResourceLocation.fromNamespaceAndPath(MatrixCraftMod.MODID, "focus_hud");
    
    // Bar dimensions
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 5;
    private static final int BAR_PADDING = 10;
    
    // Colors
    private static final int BAR_BG_COLOR = 0x80000000;      // Semi-transparent black
    private static final int BAR_FILL_COLOR = 0xFF00FF00;    // Bright green
    private static final int BAR_BORDER_COLOR = 0xFF004400;  // Dark green
    private static final int VIGNETTE_COLOR = 0x00FF00;      // Green for vignette
    
    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, FOCUS_HUD_LAYER, FocusHudRenderer::renderFocusHud);
        MatrixCraftMod.LOGGER.info("[MatrixCraft] Focus HUD layer registered");
    }
    
    /**
     * Main render method for the Focus HUD
     */
    private static void renderFocusHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        
        float transition = FocusClientEffects.getTransitionProgress();
        
        // Only render if in focus or transitioning
        if (transition <= 0) {
            return;
        }
        
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        
        // Render vignette overlay
        renderVignette(graphics, screenWidth, screenHeight, transition);
        
        // Render green tint overlay
        renderGreenTint(graphics, screenWidth, screenHeight, transition);
        
        // Render focus bar
        if (FocusManager.isClientInFocus()) {
            renderFocusBar(graphics, screenWidth, screenHeight);
        }
        
        // Render "FOCUS" text
        renderFocusText(graphics, screenWidth, screenHeight, transition);
    }
    
    /**
     * Render dark vignette around screen edges
     */
    private static void renderVignette(GuiGraphics graphics, int width, int height, float intensity) {
        if (intensity <= 0) return;
        
        int alpha = (int)(80 * intensity);
        int vignetteColor = (alpha << 24);
        
        // Top
        graphics.fill(0, 0, width, 30, vignetteColor);
        // Bottom
        graphics.fill(0, height - 30, width, height, vignetteColor);
        // Left
        graphics.fill(0, 0, 30, height, vignetteColor);
        // Right
        graphics.fill(width - 30, 0, width, height, vignetteColor);
    }
    
    /**
     * Render subtle green tint over the screen
     */
    private static void renderGreenTint(GuiGraphics graphics, int width, int height, float intensity) {
        if (intensity <= 0) return;
        
        int alpha = (int)(25 * intensity); // Very subtle
        int greenTint = (alpha << 24) | (0x00 << 16) | (0xFF << 8) | 0x00; // ARGB
        
        graphics.fill(0, 0, width, height, greenTint);
    }
    
    /**
     * Render the focus bar showing remaining time
     */
    private static void renderFocusBar(GuiGraphics graphics, int screenWidth, int screenHeight) {
        float progress = FocusManager.getClientFocusProgress();
        
        // Position: bottom center of screen, above hotbar
        int barX = (screenWidth - BAR_WIDTH) / 2;
        int barY = screenHeight - 50;
        
        // Background
        graphics.fill(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, BAR_BORDER_COLOR);
        graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, BAR_BG_COLOR);
        
        // Fill based on progress
        int fillWidth = (int)(BAR_WIDTH * progress);
        if (fillWidth > 0) {
            // Gradient effect - brighter in center
            graphics.fill(barX, barY, barX + fillWidth, barY + BAR_HEIGHT, BAR_FILL_COLOR);
            
            // Highlight on top edge
            int highlightColor = 0xFF44FF44;
            graphics.fill(barX, barY, barX + fillWidth, barY + 1, highlightColor);
        }
        
        // Pulsing glow effect when low
        if (progress < 0.3f) {
            float pulse = (float)(Math.sin(System.currentTimeMillis() / 100.0) * 0.5 + 0.5);
            int pulseAlpha = (int)(100 * pulse);
            int pulseColor = (pulseAlpha << 24) | 0xFF0000; // Red pulse
            graphics.fill(barX - 2, barY - 2, barX + BAR_WIDTH + 2, barY + BAR_HEIGHT + 2, pulseColor);
        }
    }
    
    /**
     * Render "FOCUS" text indicator
     */
    private static void renderFocusText(GuiGraphics graphics, int screenWidth, int screenHeight, float intensity) {
        if (intensity < 0.5f) return;
        
        Minecraft mc = Minecraft.getInstance();
        String text = "FOCUS";
        
        int textWidth = mc.font.width(text);
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight - 65;
        
        // Calculate alpha based on intensity and time for subtle pulse
        float pulse = (float)(Math.sin(System.currentTimeMillis() / 500.0) * 0.2 + 0.8);
        int alpha = (int)(255 * intensity * pulse);
        int color = (alpha << 24) | 0x00FF00; // Green with alpha
        
        // Draw with shadow
        graphics.drawString(mc.font, text, x + 1, y + 1, 0x004400, false);
        graphics.drawString(mc.font, text, x, y, color, false);
    }
    
    /**
     * Render scan lines effect (optional, for extra Matrix feel)
     */
    private static void renderScanLines(GuiGraphics graphics, int width, int height, float intensity) {
        if (intensity <= 0) return;
        
        int alpha = (int)(20 * intensity);
        int lineColor = (alpha << 24);
        
        // Draw horizontal lines every 4 pixels
        for (int y = 0; y < height; y += 4) {
            graphics.fill(0, y, width, y + 1, lineColor);
        }
    }
}
