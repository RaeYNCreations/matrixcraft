package com.raeyncraft.matrixcraft.item;

import com.raeyncraft.matrixcraft.MatrixCraftConfig;
import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.silenthill.SilentHillMode;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TheObeliskItem extends Item {

    private static final Map<UUID, Long> cooldowns = new HashMap<>();

    public TheObeliskItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        UUID playerId = player.getUUID();
        long currentTime = level.getGameTime();

        if (SilentHillMode.isInSilentHillMode(player)) {
            player.displayClientMessage(
                Component.literal("Silent Hill Mode is already active!")
                    .withStyle(ChatFormatting.DARK_RED),
                true
            );
            return InteractionResultHolder.fail(stack);
        }

        if (cooldowns.containsKey(playerId)) {
            long lastUse = cooldowns.get(playerId);
            long cooldownTicks = MatrixCraftConfig.OBELISK_COOLDOWN.get() * 20L;
            long timeRemaining = (lastUse + cooldownTicks) - currentTime;

            if (timeRemaining > 0) {
                int secondsRemaining = (int) (timeRemaining / 20);
                player.displayClientMessage(
                    Component.literal("The Obelisk is recharging... (" + secondsRemaining + "s)")
                        .withStyle(ChatFormatting.RED),
                    true
                );
                return InteractionResultHolder.fail(stack);
            }
        }

        SilentHillMode.enable(player);
        cooldowns.put(playerId, currentTime);

        int durationTicks = MatrixCraftConfig.OBELISK_DURATION.get() * 20;

        level.getServer().tell(new net.minecraft.server.TickTask(
            level.getServer().getTickCount() + durationTicks,
            () -> {
                if (player.isAlive() && SilentHillMode.isInSilentHillMode(player)) {
                    SilentHillMode.disable(player);
                    player.displayClientMessage(
                        Component.literal("Silent Hill Mode has ended...")
                            .withStyle(ChatFormatting.GRAY),
                        true
                    );
                }
            }
        ));

        player.displayClientMessage(
            Component.literal("Reality warps around you...")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
            true
        );

        MatrixCraftMod.LOGGER.info("[Obelisk] Activated by: " + player.getName().getString());

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("\"The fog is coming...\"")
            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("Right-click to activate Silent Hill Mode")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("Effects:")
            .withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.literal("• 3x Damage")
            .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("• 2x Health")
            .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("• All monsters hunt you")
            .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("Duration: " + MatrixCraftConfig.OBELISK_DURATION.get() + "s")
            .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("Cooldown: " + MatrixCraftConfig.OBELISK_COOLDOWN.get() + "s")
            .withStyle(ChatFormatting.AQUA));

        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Enchanted glint
    }

    public static void clearCooldown(UUID playerId) {
        cooldowns.remove(playerId);
    }

    public static void clearAllCooldowns() {
        cooldowns.clear();
    }
}