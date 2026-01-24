package com.raeyncraft.matrixcraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.raeyncraft.matrixcraft.MatrixCraftConfig;
import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.glass.GlassRepairSystem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = MatrixCraftMod.MODID)
public class MatrixCraftCommands {
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MatrixCraftMod.LOGGER.info("[MatrixCraft] Registering /matrix commands");
        
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("matrix")
            .requires(source -> source.hasPermission(2)) // Requires OP
            
            // ==================== BULLET TRAILS ====================
            .then(Commands.literal("bullettrails")
                
                // /matrix bullettrails enable <true/false>
                .then(Commands.literal("enable")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            boolean enabled = BoolArgumentType.getBool(context, "enabled");
                            MatrixCraftConfig.TRAILS_ENABLED.set(enabled);
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
                
                // /matrix bullettrails length <1-100>
                .then(Commands.literal("length")
                    .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 100))
                        .executes(context -> {
                            int length = IntegerArgumentType.getInteger(context, "ticks");
                            MatrixCraftConfig.TRAIL_LENGTH.set(length);
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
                
                // /matrix bullettrails density <1-10>
                .then(Commands.literal("density")
                    .then(Commands.argument("particles", IntegerArgumentType.integer(1, 10))
                        .executes(context -> {
                            int density = IntegerArgumentType.getInteger(context, "particles");
                            MatrixCraftConfig.TRAIL_DENSITY.set(density);
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
                
                // /matrix bullettrails width <0.01-1.0>
                .then(Commands.literal("width")
                    .then(Commands.argument("size", DoubleArgumentType.doubleArg(0.01, 1.0))
                        .executes(context -> {
                            double width = DoubleArgumentType.getDouble(context, "size");
                            MatrixCraftConfig.TRAIL_WIDTH.set(width);
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
                
                // /matrix bullettrails color <red> <green> <blue>
                .then(Commands.literal("color")
                    .then(Commands.argument("red", IntegerArgumentType.integer(0, 255))
                        .then(Commands.argument("green", IntegerArgumentType.integer(0, 255))
                            .then(Commands.argument("blue", IntegerArgumentType.integer(0, 255))
                                .executes(context -> {
                                    int red = IntegerArgumentType.getInteger(context, "red");
                                    int green = IntegerArgumentType.getInteger(context, "green");
                                    int blue = IntegerArgumentType.getInteger(context, "blue");
                                    MatrixCraftConfig.TRAIL_RED.set(red);
                                    MatrixCraftConfig.TRAIL_GREEN.set(green);
                                    MatrixCraftConfig.TRAIL_BLUE.set(blue);
                                    context.getSource().sendSuccess(() -> 
                                        Component.literal("§6[Bullet Trails] §7Color: §cR:" + red + " §aG:" + green + " §9B:" + blue), true);
                                    return 1;
                                })
                            )
                        )
                    )
                    .executes(context -> {
                        int r = MatrixCraftConfig.TRAIL_RED.get();
                        int g = MatrixCraftConfig.TRAIL_GREEN.get();
                        int b = MatrixCraftConfig.TRAIL_BLUE.get();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Trails] §7Color: §cR:" + r + " §aG:" + g + " §9B:" + b), false);
                        return 1;
                    })
                )
                
                // /matrix bullettrails alpha <0.0-1.0>
                .then(Commands.literal("alpha")
                    .then(Commands.argument("transparency", DoubleArgumentType.doubleArg(0.0, 1.0))
                        .executes(context -> {
                            double alpha = DoubleArgumentType.getDouble(context, "transparency");
                            MatrixCraftConfig.TRAIL_ALPHA.set(alpha);
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
                
                // /matrix bullettrails glow <true/false>
                .then(Commands.literal("glow")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            boolean glow = BoolArgumentType.getBool(context, "enabled");
                            MatrixCraftConfig.TRAIL_GLOW.set(glow);
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
                
                // /matrix bullettrails maxdistance <16-256>
                .then(Commands.literal("maxdistance")
                    .then(Commands.argument("blocks", DoubleArgumentType.doubleArg(16.0, 256.0))
                        .executes(context -> {
                            double distance = DoubleArgumentType.getDouble(context, "blocks");
                            MatrixCraftConfig.MAX_RENDER_DISTANCE.set(distance);
                            context.getSource().sendSuccess(() -> 
                                Component.literal("§6[Bullet Trails] §7Max Render Distance: §e" + String.format("%.0f", distance) + " blocks"), true);
                            return 1;
                        })
                    )
                    .executes(context -> {
                        double current = MatrixCraftConfig.MAX_RENDER_DISTANCE.get();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Bullet Trails] §7Max Render Distance: §e" + String.format("%.0f", current) + " blocks"), false);
                        return 1;
                    })
                )
                
                // /matrix bullettrails maxpertick <10-500>
                .then(Commands.literal("maxpertick")
                    .then(Commands.argument("count", IntegerArgumentType.integer(10, 500))
                        .executes(context -> {
                            int count = IntegerArgumentType.getInteger(context, "count");
                            MatrixCraftConfig.MAX_TRAILS_PER_TICK.set(count);
                            context.getSource().sendSuccess(() -> 
                                Component.literal("§6[Bullet Trails] §7Max Trails/Tick: §e" + count), true);
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
                
                // /matrix bullettrails status
                .then(Commands.literal("status")
                    .executes(context -> {
                        boolean enabled = MatrixCraftConfig.TRAILS_ENABLED.get();
                        int length = MatrixCraftConfig.TRAIL_LENGTH.get();
                        int density = MatrixCraftConfig.TRAIL_DENSITY.get();
                        double width = MatrixCraftConfig.TRAIL_WIDTH.get();
                        int r = MatrixCraftConfig.TRAIL_RED.get();
                        int g = MatrixCraftConfig.TRAIL_GREEN.get();
                        int b = MatrixCraftConfig.TRAIL_BLUE.get();
                        double alpha = MatrixCraftConfig.TRAIL_ALPHA.get();
                        boolean glow = MatrixCraftConfig.TRAIL_GLOW.get();
                        double maxDist = MatrixCraftConfig.MAX_RENDER_DISTANCE.get();
                        int maxPerTick = MatrixCraftConfig.MAX_TRAILS_PER_TICK.get();
                        
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
                                "§7Max Trails/Tick: §e" + maxPerTick), false);
                        return 1;
                    })
                )
                
                // /matrix bullettrails (no args - show help)
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
                            "§e/matrix bullettrails status"), false);
                    return 1;
                })
            )
            
            // ==================== GLASS REPAIR ====================
            .then(Commands.literal("glassrepair")
                
                // /matrix glassrepair enable
                .then(Commands.literal("enable")
                    .executes(context -> {
                        GlassRepairSystem.setEnabled(true);
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Glass Repair] §aSystem enabled"), true);
                        return 1;
                    })
                )
                
                // /matrix glassrepair disable
                .then(Commands.literal("disable")
                    .executes(context -> {
                        GlassRepairSystem.setEnabled(false);
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Glass Repair] §cSystem disabled"), true);
                        return 1;
                    })
                )
                
                // /matrix glassrepair delay <seconds>
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
                
                // /matrix glassrepair status
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
                
                // /matrix glassrepair clear
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
                
                // /matrix glassrepair now
                .then(Commands.literal("now")
                    .executes(context -> {
                        ServerLevel level = context.getSource().getLevel();
                        GlassRepairSystem.repairAllNow(level);
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Glass Repair] §aRepaired all broken glass immediately"), true);
                        return 1;
                    })
                )
                
                // /matrix glassrepair rescan
                .then(Commands.literal("rescan")
                    .executes(context -> {
                        ServerLevel level = context.getSource().getLevel();
                        GlassRepairSystem.rescan(level);
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6[Glass Repair] §eRescanning for glass blocks..."), true);
                        return 1;
                    })
                )
                
                // /matrix glassrepair (no args - show help)
                .executes(context -> {
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6=== Glass Repair Commands ===\n" +
                            "§e/matrix glassrepair enable\n" +
                            "§e/matrix glassrepair disable\n" +
                            "§e/matrix glassrepair delay <seconds>\n" +
                            "§e/matrix glassrepair status\n" +
                            "§e/matrix glassrepair clear\n" +
                            "§e/matrix glassrepair now\n" +
                            "§e/matrix glassrepair rescan"), false);
                    return 1;
                })
            )
            
            // /matrix cobwebs
            .then(Commands.literal("cobwebs")
                // /matrix cobwebs on
                .then(Commands.literal("on")
                    .executes(context -> setCobwebs(context.getSource(), true))
                )
                // /matrix cobwebs off
                .then(Commands.literal("off")
                    .executes(context -> setCobwebs(context.getSource(), false))
                )
                // /matrix cobwebs (no argument - show status)
                .executes(context -> showCobwebStatus(context.getSource()))
            )

            // ==================== MAIN /matrix COMMAND ====================
            .executes(context -> {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§6=== MatrixCraft Commands ===\n" +
                        "§e/matrix bullettrails §7- Configure bullet trail effects\n" +
                        "§e/matrix glassrepair §7- Configure glass repair system\n" +
                        "§e/matrix cobwebs §7- Enable or disable cobwebs\n" +
                        "§7\nUse each subcommand for more options."), false);
                return 1;
            })
        );
        
        MatrixCraftMod.LOGGER.info("[MatrixCraft] Commands registered successfully");
        
    }
}
