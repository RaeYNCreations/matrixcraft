package com.raeyncraft.matrixcraft.item;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Safe Haven Obelisk - A mysterious artifact that creates a zone of protection.
 * 
 * Inspired by Silent Hill's save points - a glowing red circle that represents safety.
 * When placed, it prevents all non-player mobs from spawning within a 32-block radius
 * (2x2x2 chunks).
 * 
 * Visual: A lodestone-like block with a glowing red beacon effect.
 * Can only be obtained/used in Creative mode or by operators.
 */
public class SafeHavenObeliskItem extends Item {
    
    public static final int PROTECTION_RADIUS = 32; // 32 blocks = 2 chunks in each direction
    
    public SafeHavenObeliskItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Player player = context.getPlayer();
        
        if (player == null) return InteractionResult.FAIL;
        
        // Only allow creative/op players to use
        if (!player.isCreative() && !player.hasPermissions(2)) {
            if (level.isClientSide) {
                player.displayClientMessage(
                    Component.literal("Only operators can use this item.")
                        .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
        
        // Find the position above the clicked block
        BlockPos placePos = clickedPos.above();
        
        // Check if we can place here
        if (!level.getBlockState(placePos).canBeReplaced()) {
            if (level.isClientSide) {
                player.displayClientMessage(
                    Component.literal("Cannot place here - block is not replaceable.")
                        .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
        
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            // Place a lodestone as the base
            level.setBlock(placePos, Blocks.LODESTONE.defaultBlockState(), 3);
            
            // Register this position with the MobSuppressionSystem
            MobSuppressionSystem.addSuppressor(serverLevel, placePos, PROTECTION_RADIUS);
            
            // Play activation sound
            level.playSound(null, placePos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 0.5f);
            level.playSound(null, placePos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.5f, 0.8f);
            
            MatrixCraftMod.LOGGER.info("[SafeHaven] Obelisk placed at " + placePos + 
                " with " + PROTECTION_RADIUS + " block radius");
            
            // Consume item if not creative
            if (!player.isCreative()) {
                context.getItemInHand().shrink(1);
            }
            
            player.displayClientMessage(
                Component.literal("Safe Haven established. Radius: " + PROTECTION_RADIUS + " blocks.")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
        }
        
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("\"In this place, nothing can reach you...\"")
            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("Right-click on a block to place.")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("Effects:")
            .withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.literal("• Prevents mob spawns in " + PROTECTION_RADIUS + " block radius")
            .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("• Does not affect players")
            .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("• Break the lodestone to remove")
            .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("Creative/Operator only")
            .withStyle(ChatFormatting.DARK_PURPLE));
        
        super.appendHoverText(stack, context, tooltip, flag);
    }
    
    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Enchanted glint
    }
}
