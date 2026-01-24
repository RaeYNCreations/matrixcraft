package com.raeyncraft.matrixcraft.bullettime;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.bullettime.registry.BulletTimeRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Server-side event handlers for the Focus system
 */
@EventBusSubscriber(modid = MatrixCraftMod.MODID)
public class FocusServerEvents {
    
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Update focus manager
        FocusManager.serverTick();
    }
    
    /**
     * Apply damage resistance when in Focus mode
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        
        if (!(entity instanceof Player player)) {
            return;
        }
        
        if (FocusManager.isInFocus(player)) {
            // Apply damage resistance
            float multiplier = FocusManager.getDamageResistanceMultiplier(player);
            float newDamage = event.getOriginalDamage() * multiplier;
            event.setNewDamage(newDamage);
        }
    }
    
    /**
     * Handle when the Matrix Focus effect expires
     */
    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEffect() == null) return;
        
        // Check if it's our effect
        if (event.getEffect().value() instanceof com.raeyncraft.matrixcraft.bullettime.effect.MatrixFocusEffect) {
            LivingEntity entity = event.getEntity();
            if (entity instanceof ServerPlayer player) {
                FocusManager.deactivateFocus(player);
            }
        }
    }
    
    /**
     * Handle when the Matrix Focus effect expires naturally
     */
    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() == null) return;
        
        // Check if it's our effect
        if (event.getEffectInstance().getEffect().value() instanceof com.raeyncraft.matrixcraft.bullettime.effect.MatrixFocusEffect) {
            LivingEntity entity = event.getEntity();
            if (entity instanceof ServerPlayer player) {
                // Clean up
                com.raeyncraft.matrixcraft.bullettime.effect.MatrixFocusEffect.onEffectRemoved(player);
                MatrixCraftMod.LOGGER.info("[MatrixFocus] Focus expired for " + player.getName().getString());
            }
        }
    }
}
