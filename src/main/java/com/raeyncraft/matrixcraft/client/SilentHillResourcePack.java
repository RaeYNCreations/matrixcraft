package com.raeyncraft.matrixcraft.client;

import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.PackLocationInfo;

import java.nio.file.Path;

@EventBusSubscriber(modid = "matrixcraft", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SilentHillResourcePack {
    
    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            // Get the path to the built-in resource pack
            Path resourcePath = event.getModFile().findResource("resourcepacks/silent_hill");
            
            // Register it as a selectable pack (but don't enable yet)
            event.addRepositorySource((consumer) -> {
                PackLocationInfo info = new PackLocationInfo(
                    "matrixcraft:silent_hill",
                    net.minecraft.network.chat.Component.literal("Silent Hill Mode"),
                    PackSource.BUILT_IN,
                    java.util.Optional.empty()
                );
                
                Pack pack = Pack.readMetaAndCreate(
                    info,
                    new PathPackResources.PathResourcesSupplier(resourcePath),
                    PackType.CLIENT_RESOURCES,
                    new Pack.Position(false, Pack.Position.INSERT_PRIORITY, false)
                );
                
                if (pack != null) {
                    consumer.accept(pack);
                }
            });
        }
    }
}