package com.raeyncraft.matrixcraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
        MatrixCraftMod.LOGGER.info("[MatrixCraft] Registering /glassrepair command");
        
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("glassrepair")
            .requires(source -> source.hasPermission(2)) // Requires OP
            
            // /glassrepair enable
            .then(Commands.literal("enable")
                .executes(context -> {
                    GlassRepairSystem.setEnabled(true);
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§aGlass repair system enabled"), true);
                    return 1;
                })
            )
            
            // /glassrepair disable
            .then(Commands.literal("disable")
                .executes(context -> {
                    GlassRepairSystem.setEnabled(false);
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§cGlass repair system disabled"), true);
                    return 1;
                })
            )
            
            // /glassrepair delay <seconds>
            .then(Commands.literal("delay")
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                    .executes(context -> {
                        int seconds = IntegerArgumentType.getInteger(context, "seconds");
                        GlassRepairSystem.setRepairDelay(seconds);
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§eGlass repair delay set to " + seconds + " seconds"), true);
                        return 1;
                    })
                )
            )
            
            // /glassrepair status
            .then(Commands.literal("status")
                .executes(context -> {
                    ServerLevel level = context.getSource().getLevel();
                    boolean enabled = GlassRepairSystem.isEnabled();
                    int delay = GlassRepairSystem.getRepairDelaySeconds();
                    int pending = GlassRepairSystem.getPendingRepairCount(level);
                    
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6=== Glass Repair System ===\n" +
                            "§7Status: " + (enabled ? "§aEnabled" : "§cDisabled") + "\n" +
                            "§7Repair Delay: §e" + delay + " seconds\n" +
                            "§7Pending Repairs: §e" + pending), false);
                    return 1;
                })
            )
            
            // /glassrepair clear
            .then(Commands.literal("clear")
                .executes(context -> {
                    ServerLevel level = context.getSource().getLevel();
                    int count = GlassRepairSystem.getPendingRepairCount(level);
                    GlassRepairSystem.clearPendingRepairs(level);
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§eCleared " + count + " pending glass repairs"), true);
                    return 1;
                })
            )
            
            // /glassrepair now
            .then(Commands.literal("now")
                .executes(context -> {
                    ServerLevel level = context.getSource().getLevel();
                    GlassRepairSystem.repairAllNow(level);
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§aRepaired all broken glass immediately"), true);
                    return 1;
                })
            )
        );
    }
}
