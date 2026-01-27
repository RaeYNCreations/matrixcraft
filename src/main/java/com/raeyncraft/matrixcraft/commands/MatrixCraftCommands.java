package com.raeyncraft.matrixcraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.raeyncraft.matrixcraft.MatrixCraftConfig;
import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.command.MatrixSettings;
import com.raeyncraft.matrixcraft.glass.GlassRepairSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * MatrixCraft Commands - Reorganized Structure
 * 
 * /matrix bullettime - Bullet time settings (colors, effects)
 * /matrix bullettrails - Bullet trail settings
 * /matrix utilities - Glass repair, cobwebs, etc.
 */
@EventBusSubscriber(modid = MatrixCraftMod.MODID)
public class MatrixCraftCommands {
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MatrixCraftMod.LOGGER.info("[MatrixCraft] Registering /matrix commands");
        
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("matrix")
            .requires(source -> source.hasPermission(2))
            .then(buildBulletTimeCommands())
            .then(buildBulletTrailCommands())
            .then(buildUtilitiesCommands())
            .executes(context -> {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§6=== MatrixCraft Commands ===\n" +
                        "§e/matrix bullettime §7- Bullet time / Focus mode settings\n" +
                        "§e/matrix bullettrails §7- Bullet trail effects\n" +
                        "§e/matrix utilities §7- Glass repair, cobwebs, etc.\n" +
                        "§7\nUse each subcommand for more options."), false);
                return 1;
            })
        );
        
        MatrixCraftMod.LOGGER.info("[MatrixCraft] Commands registered successfully");
    }
    
    // ==================== BULLET TIME COMMANDS ====================
    
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildBulletTimeCommands() {
        return Commands.literal("bullettime")
            
            // /matrix bullettime focusbar color <r> <g> <b>
            .then(Commands.literal("focusbar")
                .then(Commands.literal("color")
                    .then(Commands.argument("r", IntegerArgumentType.integer(0, 255))
                        .then(Commands.argument("g", IntegerArgumentType.integer(0, 255))
                            .then(Commands.argument("b", IntegerArgumentType.integer(0, 255))
                                .executes(context -> {
                                    int r = IntegerArgumentType.getInteger(context, "r");
                                    int g = IntegerArgumentType.getInteger(context, "g");
                                    int b = IntegerArgumentType.getInteger(context, "b");
                                    MatrixCraftConfig.FOCUS_BAR_COLOR_R.set(r);
                                    MatrixCraftConfig.FOCUS_BAR_COLOR_G.set(g);
                                    MatrixCraftConfig.FOCUS_BAR_COLOR_B.set(b);
                                    MatrixCraftConfig.saveClientConfig();
                                    context.getSource().sendSuccess(() -> 
                                        Component.literal("§6[Bullet Time] §7Focus bar color: §cR:" + r + " §aG:" + g + " §9B:" + b), true);
                                    return 1;
                                })
                            )
                        )
                    )
                    .executes(context -> {
                        int r = MatrixCraftConfig.FOCUS_BAR_COLOR_R.get();
                        int g = MatrixCraftConfig.FOCUS_BAR_COLOR_G.get();
                        int b = MatrixCraftConfig.FOCUS_BAR_COLOR_B.get();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Time] §7Focus bar color: §cR:" + r + " §aG:" + g + " §9B:" + b), false);
                        return 1;
                    })
                )
            )
            
            // /matrix bullettime screentint
            .then(Commands.literal("screentint")
                .then(Commands.literal("color")
                    .then(Commands.argument("r", IntegerArgumentType.integer(0, 255))
                        .then(Commands.argument("g", IntegerArgumentType.integer(0, 255))
                            .then(Commands.argument("b", IntegerArgumentType.integer(0, 255))
                                .executes(context -> {
                                    int r = IntegerArgumentType.getInteger(context, "r");
                                    int g = IntegerArgumentType.getInteger(context, "g");
                                    int b = IntegerArgumentType.getInteger(context, "b");
                                    MatrixCraftConfig.FOCUS_TINT_COLOR_R.set(r);
                                    MatrixCraftConfig.FOCUS_TINT_COLOR_G.set(g);
                                    MatrixCraftConfig.FOCUS_TINT_COLOR_B.set(b);
                                    MatrixCraftConfig.saveClientConfig();
                                    context.getSource().sendSuccess(() -> 
                                        Component.literal("§6[Bullet Time] §7Screen tint color: §cR:" + r + " §aG:" + g + " §9B:" + b), true);
                                    return 1;
                                })
                            )
                        )
                    )
                    .executes(context -> {
                        int r = MatrixCraftConfig.FOCUS_TINT_COLOR_R.get();
                        int g = MatrixCraftConfig.FOCUS_TINT_COLOR_G.get();
                        int b = MatrixCraftConfig.FOCUS_TINT_COLOR_B.get();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Time] §7Screen tint color: §cR:" + r + " §aG:" + g + " §9B:" + b), false);
                        return 1;
                    })
                )
                .then(Commands.literal("intensity")
                    .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 1.0))
                        .executes(context -> {
                            double intensity = DoubleArgumentType.getDouble(context, "value");
                            MatrixCraftConfig.FOCUS_TINT_INTENSITY.set(intensity);
                            MatrixCraftConfig.saveClientConfig();
                            context.getSource().sendSuccess(() -> 
                                Component.literal("§6[Bullet Time] §7Screen tint intensity: §e" + String.format("%.2f", intensity)), true);
                            return 1;
                        })
                    )
                    .executes(context -> {
                        double intensity = MatrixCraftConfig.FOCUS_TINT_INTENSITY.get();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Time] §7Screen tint intensity: §e" + String.format("%.2f", intensity)), false);
                        return 1;
                    })
                )
            )
            
            // /matrix bullettime vignette
            .then(Commands.literal("vignette")
                .then(Commands.argument("intensity", DoubleArgumentType.doubleArg(0.0, 1.0))
                    .executes(context -> {
                        double intensity = DoubleArgumentType.getDouble(context, "intensity");
                        MatrixCraftConfig.FOCUS_VIGNETTE_INTENSITY.set(intensity);
                        MatrixCraftConfig.saveClientConfig();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Time] §7Vignette intensity: §e" + String.format("%.2f", intensity)), true);
                        return 1;
                    })
                )
                .executes(context -> {
                    double intensity = MatrixCraftConfig.FOCUS_VIGNETTE_INTENSITY.get();
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6[Bullet Time] §7Vignette intensity: §e" + String.format("%.2f", intensity)), false);
                    return 1;
                })
            )
            
            // /matrix bullettime duration
            .then(Commands.literal("duration")
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 120))
                    .executes(context -> {
                        int seconds = IntegerArgumentType.getInteger(context, "seconds");
                        MatrixCraftConfig.FOCUS_DURATION_SECONDS.set(seconds);
                        MatrixCraftConfig.saveCommonConfig();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Time] §7Duration: §e" + seconds + " seconds"), true);
                        return 1;
                    })
                )
                .executes(context -> {
                    int seconds = MatrixCraftConfig.FOCUS_DURATION_SECONDS.get();
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6[Bullet Time] §7Duration: §e" + seconds + " seconds"), false);
                    return 1;
                })
            )
            
            // /matrix bullettime cooldown
            .then(Commands.literal("cooldown")
                .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 600))
                    .executes(context -> {
                        int seconds = IntegerArgumentType.getInteger(context, "seconds");
                        MatrixCraftConfig.FOCUS_COOLDOWN_SECONDS.set(seconds);
                        MatrixCraftConfig.saveCommonConfig();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Time] §7Cooldown: §e" + seconds + " seconds"), true);
                        return 1;
                    })
                )
                .executes(context -> {
                    int seconds = MatrixCraftConfig.FOCUS_COOLDOWN_SECONDS.get();
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6[Bullet Time] §7Cooldown: §e" + seconds + " seconds"), false);
                    return 1;
                })
            )
            
            // /matrix bullettime preset
            .then(Commands.literal("preset")
                .then(Commands.literal("matrix")
                    .executes(context -> {
                        MatrixCraftConfig.FOCUS_BAR_COLOR_R.set(0);
                        MatrixCraftConfig.FOCUS_BAR_COLOR_G.set(255);
                        MatrixCraftConfig.FOCUS_BAR_COLOR_B.set(0);
                        MatrixCraftConfig.FOCUS_TINT_COLOR_R.set(0);
                        MatrixCraftConfig.FOCUS_TINT_COLOR_G.set(255);
                        MatrixCraftConfig.FOCUS_TINT_COLOR_B.set(0);
                        MatrixCraftConfig.FOCUS_TINT_INTENSITY.set(0.15);
                        MatrixCraftConfig.FOCUS_VIGNETTE_INTENSITY.set(0.4);
                        MatrixCraftConfig.saveClientConfig();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Time] §aApplied Matrix preset (green)"), true);
                        return 1;
                    })
                )
                .then(Commands.literal("redpill")
                    .executes(context -> {
                        MatrixCraftConfig.FOCUS_BAR_COLOR_R.set(255);
                        MatrixCraftConfig.FOCUS_BAR_COLOR_G.set(50);
                        MatrixCraftConfig.FOCUS_BAR_COLOR_B.set(50);
                        MatrixCraftConfig.FOCUS_TINT_COLOR_R.set(255);
                        MatrixCraftConfig.FOCUS_TINT_COLOR_G.set(0);
                        MatrixCraftConfig.FOCUS_TINT_COLOR_B.set(0);
                        MatrixCraftConfig.FOCUS_TINT_INTENSITY.set(0.1);
                        MatrixCraftConfig.FOCUS_VIGNETTE_INTENSITY.set(0.5);
                        MatrixCraftConfig.saveClientConfig();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Time] §cApplied Red Pill preset"), true);
                        return 1;
                    })
                )
                .then(Commands.literal("bluepill")
                    .executes(context -> {
                        MatrixCraftConfig.FOCUS_BAR_COLOR_R.set(50);
                        MatrixCraftConfig.FOCUS_BAR_COLOR_G.set(100);
                        MatrixCraftConfig.FOCUS_BAR_COLOR_B.set(255);
                        MatrixCraftConfig.FOCUS_TINT_COLOR_R.set(0);
                        MatrixCraftConfig.FOCUS_TINT_COLOR_G.set(50);
                        MatrixCraftConfig.FOCUS_TINT_COLOR_B.set(255);
                        MatrixCraftConfig.FOCUS_TINT_INTENSITY.set(0.12);
                        MatrixCraftConfig.FOCUS_VIGNETTE_INTENSITY.set(0.35);
                        MatrixCraftConfig.saveClientConfig();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Time] §9Applied Blue Pill preset"), true);
                        return 1;
                    })
                )
                .then(Commands.literal("gold")
                    .executes(context -> {
                        MatrixCraftConfig.FOCUS_BAR_COLOR_R.set(255);
                        MatrixCraftConfig.FOCUS_BAR_COLOR_G.set(200);
                        MatrixCraftConfig.FOCUS_BAR_COLOR_B.set(0);
                        MatrixCraftConfig.FOCUS_TINT_COLOR_R.set(255);
                        MatrixCraftConfig.FOCUS_TINT_COLOR_G.set(180);
                        MatrixCraftConfig.FOCUS_TINT_COLOR_B.set(0);
                        MatrixCraftConfig.FOCUS_TINT_INTENSITY.set(0.1);
                        MatrixCraftConfig.FOCUS_VIGNETTE_INTENSITY.set(0.3);
                        MatrixCraftConfig.saveClientConfig();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Time] §eApplied Gold preset"), true);
                        return 1;
                    })
                )
                .then(Commands.literal("purple")
                    .executes(context -> {
                        MatrixCraftConfig.FOCUS_BAR_COLOR_R.set(180);
                        MatrixCraftConfig.FOCUS_BAR_COLOR_G.set(0);
                        MatrixCraftConfig.FOCUS_BAR_COLOR_B.set(255);
                        MatrixCraftConfig.FOCUS_TINT_COLOR_R.set(150);
                        MatrixCraftConfig.FOCUS_TINT_COLOR_G.set(0);
                        MatrixCraftConfig.FOCUS_TINT_COLOR_B.set(200);
                        MatrixCraftConfig.FOCUS_TINT_INTENSITY.set(0.12);
                        MatrixCraftConfig.FOCUS_VIGNETTE_INTENSITY.set(0.45);
                        MatrixCraftConfig.saveClientConfig();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Time] §5Applied Purple preset"), true);
                        return 1;
                    })
                )
            )
            
            // /matrix bullettime status
            .then(Commands.literal("status")
                .executes(context -> {
                    int barR = MatrixCraftConfig.FOCUS_BAR_COLOR_R.get();
                    int barG = MatrixCraftConfig.FOCUS_BAR_COLOR_G.get();
                    int barB = MatrixCraftConfig.FOCUS_BAR_COLOR_B.get();
                    int tintR = MatrixCraftConfig.FOCUS_TINT_COLOR_R.get();
                    int tintG = MatrixCraftConfig.FOCUS_TINT_COLOR_G.get();
                    int tintB = MatrixCraftConfig.FOCUS_TINT_COLOR_B.get();
                    double tintInt = MatrixCraftConfig.FOCUS_TINT_INTENSITY.get();
                    double vigInt = MatrixCraftConfig.FOCUS_VIGNETTE_INTENSITY.get();
                    int duration = MatrixCraftConfig.FOCUS_DURATION_SECONDS.get();
                    int cooldown = MatrixCraftConfig.FOCUS_COOLDOWN_SECONDS.get();
                    
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6=== Bullet Time Settings ===\n" +
                            "§7Duration: §e" + duration + " seconds\n" +
                            "§7Cooldown: §e" + cooldown + " seconds\n" +
                            "§7Focus Bar Color: §cR:" + barR + " §aG:" + barG + " §9B:" + barB + "\n" +
                            "§7Screen Tint Color: §cR:" + tintR + " §aG:" + tintG + " §9B:" + tintB + "\n" +
                            "§7Tint Intensity: §e" + String.format("%.2f", tintInt) + "\n" +
                            "§7Vignette Intensity: §e" + String.format("%.2f", vigInt)), false);
                    return 1;
                })
            )
            
            // /matrix bullettime (help)
            .executes(context -> {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§6=== Bullet Time Commands ===\n" +
                        "§e/matrix bullettime duration <1-120>\n" +
                        "§e/matrix bullettime cooldown <0-600>\n" +
                        "§e/matrix bullettime focusbar color <r> <g> <b>\n" +
                        "§e/matrix bullettime screentint color <r> <g> <b>\n" +
                        "§e/matrix bullettime screentint intensity <0.0-1.0>\n" +
                        "§e/matrix bullettime vignette <0.0-1.0>\n" +
                        "§e/matrix bullettime preset <matrix|redpill|bluepill|gold|purple>\n" +
                        "§e/matrix bullettime status"), false);
                return 1;
            });
    }
    
    // ==================== BULLET TRAILS COMMANDS ====================
    
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildBulletTrailCommands() {
        return Commands.literal("bullettrails")
            
            .then(Commands.literal("enable")
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean enabled = BoolArgumentType.getBool(context, "enabled");
                        MatrixCraftConfig.TRAILS_ENABLED.set(enabled);
                        MatrixCraftConfig.saveClientConfig();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Trails] §7Enabled: " + (enabled ? "§atrue" : "§cfalse")), true);
                        return 1;
                    })
                )
                .executes(context -> {
                    boolean current = MatrixCraftConfig.TRAILS_ENABLED.get();
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6[Bullet Trails] §7Enabled: " + (current ? "§atrue" : "§cfalse")), false);
                    return 1;
                })
            )
            
            .then(Commands.literal("length")
                .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 100))
                    .executes(context -> {
                        int length = IntegerArgumentType.getInteger(context, "ticks");
                        MatrixCraftConfig.TRAIL_LENGTH.set(length);
                        MatrixCraftConfig.saveClientConfig();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Trails] §7Length: §e" + length + " ticks"), true);
                        return 1;
                    })
                )
                .executes(context -> {
                    int current = MatrixCraftConfig.TRAIL_LENGTH.get();
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6[Bullet Trails] §7Length: §e" + current + " ticks"), false);
                    return 1;
                })
            )
            
            .then(Commands.literal("density")
                .then(Commands.argument("particles", IntegerArgumentType.integer(1, 10))
                    .executes(context -> {
                        int density = IntegerArgumentType.getInteger(context, "particles");
                        MatrixCraftConfig.TRAIL_DENSITY.set(density);
                        MatrixCraftConfig.saveClientConfig();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Trails] §7Density: §e" + density + " particles/tick"), true);
                        return 1;
                    })
                )
                .executes(context -> {
                    int current = MatrixCraftConfig.TRAIL_DENSITY.get();
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6[Bullet Trails] §7Density: §e" + current + " particles/tick"), false);
                    return 1;
                })
            )
            
            .then(Commands.literal("width")
                .then(Commands.argument("size", DoubleArgumentType.doubleArg(0.01, 1.0))
                    .executes(context -> {
                        double width = DoubleArgumentType.getDouble(context, "size");
                        MatrixCraftConfig.TRAIL_WIDTH.set(width);
                        MatrixCraftConfig.saveClientConfig();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Trails] §7Width: §e" + String.format("%.2f", width)), true);
                        return 1;
                    })
                )
                .executes(context -> {
                    double current = MatrixCraftConfig.TRAIL_WIDTH.get();
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6[Bullet Trails] §7Width: §e" + String.format("%.2f", current)), false);
                    return 1;
                })
            )
            
            .then(Commands.literal("color")
                .then(Commands.argument("r", IntegerArgumentType.integer(0, 255))
                    .then(Commands.argument("g", IntegerArgumentType.integer(0, 255))
                        .then(Commands.argument("b", IntegerArgumentType.integer(0, 255))
                            .executes(context -> {
                                int r = IntegerArgumentType.getInteger(context, "r");
                                int g = IntegerArgumentType.getInteger(context, "g");
                                int b = IntegerArgumentType.getInteger(context, "b");
                                MatrixCraftConfig.TRAIL_COLOR_R.set(r);
                                MatrixCraftConfig.TRAIL_COLOR_G.set(g);
                                MatrixCraftConfig.TRAIL_COLOR_B.set(b);
                                MatrixCraftConfig.saveClientConfig();
                                // Force dynamic lights to refresh colors (throttled inside manager)
                                try {
                                    com.raeyncraft.matrixcraft.client.lighting.DynamicLightManager.ensureInit();
                                    com.raeyncraft.matrixcraft.client.lighting.DynamicLightManager.forceUpdateAll();
                                    MatrixCraftMod.LOGGER.info("[MatrixCraftCommands] Requested dynamic-lights updateAll after color change.");
                                } catch (Throwable ignored) {}
                                context.getSource().sendSuccess(() -> 
                                    Component.literal("§6[Bullet Trails] §7Color: §cR:" + r + " §aG:" + g + " §9B:" + b), true);
                                return 1;
                            })
                        )
                    )
                )
                .executes(context -> {
                    int r = MatrixCraftConfig.TRAIL_COLOR_R.get();
                    int g = MatrixCraftConfig.TRAIL_COLOR_G.get();
                    int b = MatrixCraftConfig.TRAIL_COLOR_B.get();
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6[Bullet Trails] §7Color: §cR:" + r + " §aG:" + g + " §9B:" + b), false);
                    return 1;
                })
            )
            
            .then(Commands.literal("alpha")
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 1.0))
                    .executes(context -> {
                        double alpha = DoubleArgumentType.getDouble(context, "value");
                        MatrixCraftConfig.TRAIL_ALPHA.set(alpha);
                        MatrixCraftConfig.saveClientConfig();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Trails] §7Alpha: §e" + String.format("%.2f", alpha)), true);
                        return 1;
                    })
                )
                .executes(context -> {
                    double current = MatrixCraftConfig.TRAIL_ALPHA.get();
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6[Bullet Trails] §7Alpha: §e" + String.format("%.2f", current)), false);
                    return 1;
                })
            )
            
            .then(Commands.literal("glow")
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean glow = BoolArgumentType.getBool(context, "enabled");
                        MatrixCraftConfig.TRAIL_GLOW.set(glow);
                        MatrixCraftConfig.saveClientConfig();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Trails] §7Glow: " + (glow ? "§atrue" : "§cfalse")), true);
                        return 1;
                    })
                )
                .executes(context -> {
                    boolean current = MatrixCraftConfig.TRAIL_GLOW.get();
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6[Bullet Trails] §7Glow: " + (current ? "§atrue" : "§cfalse")), false);
                    return 1;
                })
            )
            
            .then(Commands.literal("maxdistance")
                .then(Commands.argument("blocks", DoubleArgumentType.doubleArg(16, 256))
                    .executes(context -> {
                        double maxDist = DoubleArgumentType.getDouble(context, "blocks");
                        MatrixCraftConfig.MAX_RENDER_DISTANCE.set(maxDist);
                        MatrixCraftConfig.saveClientConfig();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Trails] §7Max Distance: §e" + String.format("%.0f", maxDist) + " blocks"), true);
                        return 1;
                    })
                )
                .executes(context -> {
                    double current = MatrixCraftConfig.MAX_RENDER_DISTANCE.get();
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6[Bullet Trails] §7Max Distance: §e" + String.format("%.0f", current) + " blocks"), false);
                    return 1;
                })
            )
            
            .then(Commands.literal("maxpertick")
                .then(Commands.argument("count", IntegerArgumentType.integer(10, 500))
                    .executes(context -> {
                        int maxPerTick = IntegerArgumentType.getInteger(context, "count");
                        MatrixCraftConfig.MAX_TRAILS_PER_TICK.set(maxPerTick);
                        MatrixCraftConfig.saveClientConfig();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Trails] §7Max Trails/Tick: §e" + maxPerTick), true);
                        return 1;
                    })
                )
                .executes(context -> {
                    int current = MatrixCraftConfig.MAX_TRAILS_PER_TICK.get();
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6[Bullet Trails] §7Max Trails/Tick: §e" + current), false);
                    return 1;
                })
            )
            
            .then(Commands.literal("status")
                .executes(context -> {
                    boolean enabled = MatrixCraftConfig.TRAILS_ENABLED.get();
                    int length = MatrixCraftConfig.TRAIL_LENGTH.get();
                    int density = MatrixCraftConfig.TRAIL_DENSITY.get();
                    double width = MatrixCraftConfig.TRAIL_WIDTH.get();
                    int r = MatrixCraftConfig.TRAIL_COLOR_R.get();
                    int g = MatrixCraftConfig.TRAIL_COLOR_G.get();
                    int b = MatrixCraftConfig.TRAIL_COLOR_B.get();
                    double alpha = MatrixCraftConfig.TRAIL_ALPHA.get();
                    boolean glow = MatrixCraftConfig.TRAIL_GLOW.get();
                    double maxDist = MatrixCraftConfig.MAX_RENDER_DISTANCE.get();
                    int maxPerTick = MatrixCraftConfig.MAX_TRAILS_PER_TICK.get();
                    boolean dynLight = MatrixCraftConfig.TRAIL_DYNAMIC_LIGHTING.get();
                    int lightLevel = MatrixCraftConfig.TRAIL_LIGHT_LEVEL.get();
                    
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6=== Bullet Trail Settings ===\n" +
                            "§7Enabled: " + (enabled ? "§atrue" : "§cfalse") + "\n" +
                            "§7Length: §e" + length + " ticks\n" +
                            "§7Density: §e" + density + " particles/tick\n" +
                            "§7Width: §e" + String.format("%.2f", width) + "\n" +
                            "§7Color: §cR:" + r + " §aG:" + g + " §9B:" + b + "\n" +
                            "§7Alpha: §e" + String.format("%.2f", alpha) + "\n" +
                            "§7Glow: " + (glow ? "§atrue" : "§cfalse") + "\n" +
                            "§7Max Distance: §e" + String.format("%.0f", maxDist) + " blocks\n" +
                            "§7Max Trails/Tick: §e" + maxPerTick + "\n" +
                            "§7Dynamic Lighting: " + (dynLight ? "§atrue" : "§cfalse") + "\n" +
                            "§7Light Level: §e" + lightLevel), false);
                    return 1;
                })
            )
            
            .then(Commands.literal("lighting")
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean enabled = BoolArgumentType.getBool(context, "enabled");
                        MatrixCraftConfig.TRAIL_DYNAMIC_LIGHTING.set(enabled);
                        MatrixCraftConfig.saveClientConfig();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Trails] §7Dynamic lighting: " + (enabled ? "§aENABLED" : "§cDISABLED")), true);
                        return 1;
                    })
                )
                .executes(context -> {
                    boolean current = MatrixCraftConfig.TRAIL_DYNAMIC_LIGHTING.get();
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6[Bullet Trails] §7Dynamic lighting: " + (current ? "§atrue" : "§cfalse")), false);
                    return 1;
                })
            )
            
            .then(Commands.literal("lightlevel")
                .then(Commands.argument("level", IntegerArgumentType.integer(1, 15))
                    .executes(context -> {
                        int level = IntegerArgumentType.getInteger(context, "level");
                        MatrixCraftConfig.TRAIL_LIGHT_LEVEL.set(level);
                        MatrixCraftConfig.saveClientConfig();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Trails] §7Light level: §e" + level), true);
                        return 1;
                    })
                )
                .executes(context -> {
                    int current = MatrixCraftConfig.TRAIL_LIGHT_LEVEL.get();
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6[Bullet Trails] §7Light level: §e" + current), false);
                    return 1;
                })
            )

            .then(Commands.literal("light_spacing")
                .then(Commands.argument("value", IntegerArgumentType.integer(1, 50))
                    .executes(context -> {
                        int spacing = IntegerArgumentType.getInteger(context, "value");
                        MatrixCraftConfig.TRAIL_LIGHT_SPACING.set(spacing);
                        MatrixCraftConfig.saveClientConfig();
                        context.getSource().sendSuccess(() ->
                            Component.literal("§6[Bullet Trails] §7Light spacing set to §e" + spacing), true);
                        return 1;
                    })
                )
            )

            .then(Commands.literal("light_duration")
                .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 1200))
                    .executes(context -> {
                        int ticks = IntegerArgumentType.getInteger(context, "ticks");
                        MatrixCraftConfig.TRAIL_LIGHT_DURATION_TICKS.set(ticks);
                        MatrixCraftConfig.saveClientConfig();
                        context.getSource().sendSuccess(() ->
                            Component.literal("§6[Bullet Trails] §7Light duration set to §e" + ticks + " ticks"), true);
                        return 1;
                    })
                )
            )

            .then(Commands.literal("light_chain")
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean enabled = BoolArgumentType.getBool(context, "enabled");
                        MatrixCraftConfig.TRAIL_CHAIN_ENABLED.set(enabled);
                        MatrixCraftConfig.saveClientConfig();
                        context.getSource().sendSuccess(() ->
                            Component.literal("§6[Bullet Trails] §7Chained lights enabled: §e" + enabled), true);
                        return 1;
                    })
                )
            )

            .then(Commands.literal("chain_count")
                .then(Commands.argument("count", IntegerArgumentType.integer(1, 8))
                    .executes(context -> {
                        int count = IntegerArgumentType.getInteger(context, "count");
                        MatrixCraftConfig.TRAIL_CHAIN_COUNT.set(count);
                        MatrixCraftConfig.saveClientConfig();
                        context.getSource().sendSuccess(() ->
                            Component.literal("§6[Bullet Trails] §7Chain count set to §e" + count), true);
                        return 1;
                    })
                )
            )

            .then(Commands.literal("chain_spacing")
                .then(Commands.argument("spacing", DoubleArgumentType.doubleArg(0.0, 5.0))
                    .executes(context -> {
                        double spacing = DoubleArgumentType.getDouble(context, "spacing");
                        MatrixCraftConfig.TRAIL_CHAIN_SPACING.set(spacing);
                        MatrixCraftConfig.saveClientConfig();
                        context.getSource().sendSuccess(() ->
                            Component.literal("§6[Bullet Trails] §7Chain spacing set to §e" + String.format("%.2f", spacing)), true);
                        return 1;
                    })
                )
            )
            
            .executes(context -> {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§6=== Bullet Trail Commands ===\n" +
                        "§e/matrix bullettrails enable <true/false>\n" +
                        "§e/matrix bullettrails length <1-100>\n" +
                        "§e/matrix bullettrails density <1-10>\n" +
                        "§e/matrix bullettrails width <0.01-1.0>\n" +
                        "§e/matrix bullettrails color <r> <g> <b>\n" +
                        "§e/matrix bullettrails alpha <0.0-1.0>\n" +
                        "§e/matrix bullettrails glow <true/false>\n" +
                        "§e/matrix bullettrails maxdistance <16-256>\n" +
                        "§e/matrix bullettrails maxpertick <10-500>\n" +
                        "§e/matrix bullettrails lighting <true/false>\n" +
                        "§e/matrix bullettrails lightlevel <1-15>\n" +
                        "§e/matrix bullettrails light_spacing <1-50>\n" +
                        "§e/matrix bullettrails light_duration <1-1200>\n" +
                        "§e/matrix bullettrails light_chain <true/false>\n" +
                        "§e/matrix bullettrails chain_count <1-8>\n" +
                        "§e/matrix bullettrails chain_spacing <0.0-5.0>\n" +
                        "§e/matrix bullettrails status"), false);
                return 1;
            });
    }
    
    // ==================== UTILITIES COMMANDS ====================
    
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildUtilitiesCommands() {
        return Commands.literal("utilities")
            
            .then(Commands.literal("cobwebs")
                .then(Commands.literal("on")
                    .executes(context -> {
                        MatrixSettings.setCobwebsEnabled(true);
                        context.getSource().sendSuccess(() -> 
                            Component.literal("Cobwebs ")
                                .append(Component.literal("ENABLED").withStyle(ChatFormatting.GREEN))
                                .append(" - You will be slowed by cobwebs"), true);
                        return 1;
                    })
                )
                .then(Commands.literal("off")
                    .executes(context -> {
                        MatrixSettings.setCobwebsEnabled(false);
                        context.getSource().sendSuccess(() -> 
                            Component.literal("Cobwebs ")
                                .append(Component.literal("DISABLED").withStyle(ChatFormatting.RED))
                                .append(" - You can walk through cobwebs freely"), true);
                        return 1;
                    })
                )
                .executes(context -> {
                    boolean enabled = MatrixSettings.areCobwebsEnabled();
                    context.getSource().sendSuccess(() -> 
                        Component.literal("Cobwebs are currently ")
                            .append(Component.literal(enabled ? "ENABLED" : "DISABLED")
                                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
                    return 1;
                })
            )
            
            // Manual lava toggle (like cobwebs - instant on/off)
            .then(Commands.literal("lava")
                .then(Commands.literal("on")
                    .executes(context -> {
                        MatrixSettings.setLavaEnabled(true);
                        context.getSource().sendSuccess(() -> 
                            Component.literal("Lava damage ")
                                .append(Component.literal("ENABLED").withStyle(ChatFormatting.GREEN))
                                .append(" - You will take damage from lava/fire"), true);
                        return 1;
                    })
                )
                .then(Commands.literal("off")
                    .executes(context -> {
                        MatrixSettings.setLavaEnabled(false);
                        context.getSource().sendSuccess(() -> 
                            Component.literal("Lava damage ")
                                .append(Component.literal("DISABLED").withStyle(ChatFormatting.RED))
                                .append(" - You are immune to lava/fire damage"), true);
                        return 1;
                    })
                )
                .executes(context -> {
                    boolean enabled = MatrixSettings.isLavaEnabled();
                    context.getSource().sendSuccess(() -> 
                        Component.literal("Lava damage is currently ")
                            .append(Component.literal(enabled ? "ENABLED" : "DISABLED")
                                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
                    return 1;
                })
            )
            
            // Focus mode lava bypass (config setting for bullet time)
            .then(Commands.literal("lavabypass")
                .then(Commands.literal("on")
                    .executes(context -> {
                        MatrixCraftConfig.FOCUS_LAVA_IMMUNITY.set(true);
                        MatrixCraftConfig.saveCommonConfig();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Time] §7Lava bypass: ")
                                .append(Component.literal("ENABLED").withStyle(ChatFormatting.GREEN))
                                .append(Component.literal(" - Lava immunity during Focus mode").withStyle(ChatFormatting.GRAY)), true);
                        return 1;
                    })
                )
                .then(Commands.literal("off")
                    .executes(context -> {
                        MatrixCraftConfig.FOCUS_LAVA_IMMUNITY.set(false);
                        MatrixCraftConfig.saveCommonConfig();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Time] §7Lava bypass: ")
                                .append(Component.literal("DISABLED").withStyle(ChatFormatting.RED))
                                .append(Component.literal(" - Normal lava damage during Focus").withStyle(ChatFormatting.GRAY)), true);
                        return 1;
                    })
                )
                .executes(context -> {
                    boolean enabled = MatrixCraftConfig.FOCUS_LAVA_IMMUNITY.get();
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6[Bullet Time] §7Lava bypass is currently ")
                            .append(Component.literal(enabled ? "ENABLED" : "DISABLED")
                                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
                    return 1;
                })
            )
            
            .then(Commands.literal("cobwebbypass")
                .then(Commands.literal("on")
                    .executes(context -> {
                        MatrixCraftConfig.FOCUS_COBWEB_BYPASS.set(true);
                        MatrixCraftConfig.saveCommonConfig();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Time] §7Cobweb bypass: ")
                                .append(Component.literal("ENABLED").withStyle(ChatFormatting.GREEN))
                                .append(Component.literal(" - Cobwebs auto-disabled during Focus").withStyle(ChatFormatting.GRAY)), true);
                        return 1;
                    })
                )
                .then(Commands.literal("off")
                    .executes(context -> {
                        MatrixCraftConfig.FOCUS_COBWEB_BYPASS.set(false);
                        MatrixCraftConfig.saveCommonConfig();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Time] §7Cobweb bypass: ")
                                .append(Component.literal("DISABLED").withStyle(ChatFormatting.RED))
                                .append(Component.literal(" - Cobwebs work normally during Focus").withStyle(ChatFormatting.GRAY)), true);
                        return 1;
                    })
                )
                .executes(context -> {
                    boolean enabled = MatrixCraftConfig.FOCUS_COBWEB_BYPASS.get();
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6[Bullet Time] §7Cobweb bypass is currently ")
                            .append(Component.literal(enabled ? "ENABLED" : "DISABLED")
                                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
                    return 1;
                })
            )
            
            .then(Commands.literal("glassrepair")
                .then(Commands.literal("enable")
                    .executes(context -> {
                        GlassRepairSystem.setEnabled(true);
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Glass Repair] §aSystem enabled"), true);
                        return 1;
                    })
                )
                .then(Commands.literal("disable")
                    .executes(context -> {
                        GlassRepairSystem.setEnabled(false);
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Glass Repair] §cSystem disabled"), true);
                        return 1;
                    })
                )
                .then(Commands.literal("delay")
                    .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                        .executes(context -> {
                            int seconds = IntegerArgumentType.getInteger(context, "seconds");
                            GlassRepairSystem.setRepairDelay(seconds);
                            context.getSource().sendSuccess(() -> 
                                Component.literal("§6[Glass Repair] §7Delay set to §e" + seconds + " seconds"), true);
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("status")
                    .executes(context -> {
                        ServerLevel level = context.getSource().getLevel();
                        boolean enabled = GlassRepairSystem.isEnabled();
                        int delay = GlassRepairSystem.getRepairDelaySeconds();
                        int pending = GlassRepairSystem.getPendingRepairCount(level);
                        int tracked = GlassRepairSystem.getTrackedGlassCount(level);
                        
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6=== Glass Repair System ===\n" +
                                "§7Status: " + (enabled ? "§aEnabled" : "§cDisabled") + "\n" +
                                "§7Repair Delay: §e" + delay + " seconds\n" +
                                "§7Tracked Glass: §e" + tracked + "\n" +
                                "§7Pending Repairs: §e" + pending), false);
                        return 1;
                    })
                )
                .then(Commands.literal("clear")
                    .executes(context -> {
                        ServerLevel level = context.getSource().getLevel();
                        int count = GlassRepairSystem.getPendingRepairCount(level);
                        GlassRepairSystem.clearPendingRepairs(level);
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Glass Repair] §eCleared " + count + " pending repairs"), true);
                        return 1;
                    })
                )
                .then(Commands.literal("now")
                    .executes(context -> {
                        ServerLevel level = context.getSource().getLevel();
                        GlassRepairSystem.repairAllNow(level);
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Glass Repair] §aRepaired all broken glass immediately"), true);
                        return 1;
                    })
                )
                .then(Commands.literal("rescan")
                    .executes(context -> {
                        ServerLevel level = context.getSource().getLevel();
                        GlassRepairSystem.rescan(level);
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Glass Repair] §eRescanning for glass blocks..."), true);
                        return 1;
                    })
                )
                .executes(context -> {
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6=== Glass Repair Commands ===\n" +
                            "§e/matrix utilities glassrepair enable\n" +
                            "§e/matrix utilities glassrepair disable\n" +
                            "§e/matrix utilities glassrepair delay <seconds>\n" +
                            "§e/matrix utilities glassrepair status\n" +
                            "§e/matrix utilities glassrepair clear\n" +
                            "§e/matrix utilities glassrepair now\n" +
                            "§e/matrix utilities glassrepair rescan"), false);
                    return 1;
                })
            )
            
            .executes(context -> {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§6=== Utilities Commands ===\n" +
                        "§e/matrix utilities cobwebs [on|off] §7- Toggle cobweb slowdown\n" +
                        "§e/matrix utilities lava [on|off] §7- Toggle lava/fire damage\n" +
                        "§e/matrix utilities lavabypass [on|off] §7- Lava immunity during Focus\n" +
                        "§e/matrix utilities cobwebbypass [on|off] §7- Cobweb bypass during Focus\n" +
                        "§e/matrix utilities glassrepair §7- Glass repair system"), false);
                return 1;
            });
    }
}
