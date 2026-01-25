package com.raeyncraft.matrixcraft.bullettime.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.raeyncraft.matrixcraft.MatrixCraftConfig;
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
 * Renders the Focus bar HUD element and Matrix visual overlay.
 * Colors are configurable via /matrix bullettime commands.
 */
@EventBusSubscriber(modid = MatrixCraftMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class FocusHudRenderer {
    
    private static final ResourceLocation FOCUS_HUD_LAYER = 
        ResourceLocation.fromNamespaceAndPath(MatrixCraftMod.MODID, "focus_hud");
    
    // Bar dimensions
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 5;
    
    // Background colors (not configurable)
    private static final int BAR_BG_COLOR = 0x80000000;  // Semi-transparent black
    
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
        
        // Render screen tint overlay
        renderScreenTint(graphics, screenWidth, screenHeight, transition);
        
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
        
        int alpha = MatrixCraftConfig.getVignetteAlpha(intensity);
        int vignetteColor = (alpha << 24);
        
        int edgeSize = 30;
        
        // Top
        graphics.fill(0, 0, width, edgeSize, vignetteColor);
        // Bottom
        graphics.fill(0, height - edgeSize, width, height, vignetteColor);
        // Left
        graphics.fill(0, 0, edgeSize, height, vignetteColor);
        // Right
        graphics.fill(width - edgeSize, 0, width, height, vignetteColor);
    }
    
    /**
     * Render configurable screen tint
     */
    private static void renderScreenTint(GuiGraphics graphics, int width, int height, float intensity) {
        if (intensity <= 0) return;
        
        int tintColor = MatrixCraftConfig.getFocusTintColor(intensity);
        graphics.fill(0, 0, width, height, tintColor);
    }
    
    /**
     * Render the focus bar showing remaining time - uses config colors
     */
    private static void renderFocusBar(GuiGraphics graphics, int screenWidth, int screenHeight) {
        float progress = FocusManager.getClientFocusProgress();
        
        // Position: center of screen, above the hotbar/health/xp area
        // screenHeight - 90 puts it well above the standard HUD elements
        int barX = (screenWidth - BAR_WIDTH) / 2;
        int barY = screenHeight - 90;
        
        // Get colors from config
        int borderColor = MatrixCraftConfig.getFocusBarBorderColor();
        int fillColor = MatrixCraftConfig.getFocusBarColor();
        int highlightColor = MatrixCraftConfig.getFocusBarHighlightColor();
        
        // Background/Border
        graphics.fill(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, borderColor);
        graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, BAR_BG_COLOR);
        
        // Fill based on progress
        int fillWidth = (int)(BAR_WIDTH * progress);
        if (fillWidth > 0) {
            graphics.fill(barX, barY, barX + fillWidth, barY + BAR_HEIGHT, fillColor);
            
            // Highlight on top edge
            graphics.fill(barX, barY, barX + fillWidth, barY + 1, highlightColor);
        }
        
        // Pulsing warning effect when low
        if (progress < 0.3f) {
            float pulse = (float)(Math.sin(System.currentTimeMillis() / 100.0) * 0.5 + 0.5);
            int pulseAlpha = (int)(100 * pulse);
            int pulseColor = (pulseAlpha << 24) | 0xFF0000; // Red pulse warning
            graphics.fill(barX - 2, barY - 2, barX + BAR_WIDTH + 2, barY + BAR_HEIGHT + 2, pulseColor);
        }
    }
    
    /**
     * Render "FOCUS" text indicator - uses config colors
     */
    private static void renderFocusText(GuiGraphics graphics, int screenWidth, int screenHeight, float intensity) {
        if (intensity < 0.5f) return;
        
        Minecraft mc = Minecraft.getInstance();
        String text = "FOCUS";
        
        int textWidth = mc.font.width(text);
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight - 105; // Above the focus bar
        
        // Calculate alpha based on intensity and time for subtle pulse
        float pulse = (float)(Math.sin(System.currentTimeMillis() / 500.0) * 0.2 + 0.8);
        float alpha = intensity * pulse;
        
        int textColor = MatrixCraftConfig.getFocusTextColor(alpha);
        int shadowColor = MatrixCraftConfig.getFocusTextShadowColor();
        
        // Draw with shadow
        graphics.drawString(mc.font, text, x + 1, y + 1, shadowColor, false);
        graphics.drawString(mc.font, text, x, y, textColor, false);
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
