package com.raeyncraft.matrixcraft.bullettime.item;

import com.raeyncraft.matrixcraft.bullettime.FocusManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class RedPillItem extends Item {
    
    // Cooldown in ticks (60 seconds = 1200 ticks)
    private static final int COOLDOWN_TICKS = 1200;
    
    public RedPillItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Check if on cooldown
        if (player.getCooldowns().isOnCooldown(this)) {
            if (level.isClientSide) {
                player.displayClientMessage(
                    Component.literal("You must wait before taking another pill...")
                        .withStyle(ChatFormatting.RED), 
                    true
                );
            }
            return InteractionResultHolder.fail(stack);
        }
        
        // Check if already in focus mode
        if (FocusManager.isInFocus(player)) {
            if (level.isClientSide) {
                player.displayClientMessage(
                    Component.literal("Already experiencing the Matrix...")
                        .withStyle(ChatFormatting.GREEN), 
                    true
                );
            }
            return InteractionResultHolder.fail(stack);
        }
        
        // Consume the pill
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        
        // Play consumption sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 0.5F);
        
        // Apply the effect
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            FocusManager.activateFocus(serverPlayer);
        }
        
        // Start cooldown
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        
        // Client-side message
        if (level.isClientSide) {
            player.displayClientMessage(
                Component.literal("Welcome to the real world...")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), 
                true
            );
        }
        
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("\"This is your last chance.\"")
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltipComponents.add(Component.literal(""));
        tooltipComponents.add(Component.literal("Right-click to enter Focus Mode")
            .withStyle(ChatFormatting.GREEN));
        tooltipComponents.add(Component.literal("Duration: 10 seconds")
            .withStyle(ChatFormatting.DARK_GREEN));
        tooltipComponents.add(Component.literal("Cooldown: 60 seconds")
            .withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.literal(""));
        tooltipComponents.add(Component.literal("Effects:")
            .withStyle(ChatFormatting.WHITE));
        tooltipComponents.add(Component.literal("• Enhanced reflexes")
            .withStyle(ChatFormatting.GREEN));
        tooltipComponents.add(Component.literal("• Improved accuracy")
            .withStyle(ChatFormatting.GREEN));
        tooltipComponents.add(Component.literal("• Damage resistance")
            .withStyle(ChatFormatting.GREEN));
        
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
    
    @Override
    public boolean isFoil(ItemStack stack) {
        // Give it an enchanted glint
        return true;
    }
}
