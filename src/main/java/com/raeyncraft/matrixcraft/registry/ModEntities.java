package com.raeyncraft.matrixcraft.registry;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Common entity registration
 * Note: Client-only entities (like LightMarkerEntity) are registered in ClientEntityRegistration
 * to prevent server-side crashes
 */
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = 
        DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MatrixCraftMod.MODID);
    
    // No entities registered here currently
    // Client-only entities are in ClientEntityRegistration to prevent dedicated server crashes
    
    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
