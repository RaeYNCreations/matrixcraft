package com.raeyncraft.matrixcraft.registry;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.client.lighting.LightMarkerEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = 
        DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MatrixCraftMod.MODID);
    
    public static final DeferredHolder<EntityType<?>, EntityType<LightMarkerEntity>> LIGHT_MARKER =
        ENTITIES.register("light_marker", () -> EntityType.Builder.<LightMarkerEntity>of(
            LightMarkerEntity::new,
            MobCategory.MISC
        )
        .sized(0.1F, 0.1F)
        .clientTrackingRange(64)
        .updateInterval(1)
        .build("light_marker"));
    
    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
