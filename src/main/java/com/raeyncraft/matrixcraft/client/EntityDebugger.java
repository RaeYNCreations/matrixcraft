package com.raeyncraft.matrixcraft.client;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(value = Dist.CLIENT)
public class EntityDebugger {
    private static final Set<String> loggedClasses = new HashSet<>();
    
    @SubscribeEvent
    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        
        // Only log on client side
        if (!entity.level().isClientSide) {
            return;
        }
        
        String className = entity.getClass().getName();
        
        // Log any entity from TacZ
        if (className.contains("tacz") || className.contains("TACZ")) {
            if (!loggedClasses.contains(className)) {
                MatrixCraftMod.LOGGER.info("==> FOUND TACZ ENTITY: " + className);
                loggedClasses.add(className);
            }
        }
        
        // Also log projectiles
        if (className.contains("Projectile") || className.contains("projectile")) {
            if (!loggedClasses.contains(className)) {
                MatrixCraftMod.LOGGER.info("==> Found projectile: " + className);
                loggedClasses.add(className);
            }
        }
    }
}
