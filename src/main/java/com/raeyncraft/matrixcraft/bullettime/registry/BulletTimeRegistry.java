package com.raeyncraft.matrixcraft.bullettime.registry;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.bullettime.effect.MatrixFocusEffect;
import com.raeyncraft.matrixcraft.bullettime.item.RedPillItem;
import com.raeyncraft.matrixcraft.item.SafeHavenObeliskItem;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BulletTimeRegistry {
    
    // Deferred registers
    public static final DeferredRegister<Item> ITEMS = 
        DeferredRegister.create(BuiltInRegistries.ITEM, MatrixCraftMod.MODID);
    
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = 
        DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, MatrixCraftMod.MODID);
    
    // ==================== ITEMS ====================
    
    public static final DeferredHolder<Item, RedPillItem> RED_PILL = ITEMS.register("red_pill",
        () -> new RedPillItem(new Item.Properties()
            .stacksTo(16)
        ));
    
    public static final DeferredHolder<Item, SafeHavenObeliskItem> SAFE_HAVEN_OBELISK = ITEMS.register("safe_haven_obelisk",
        () -> new SafeHavenObeliskItem(new Item.Properties()
            .stacksTo(1)
        ));
    
    // ==================== MOB EFFECTS ====================
    
    public static final Holder<MobEffect> MATRIX_FOCUS_EFFECT = MOB_EFFECTS.register("matrix_focus",
        MatrixFocusEffect::new);
    
    // ==================== REGISTRATION ====================
    
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);
        
        // Register creative tab listener
        modEventBus.addListener(BulletTimeRegistry::addToCreativeTabs);
        
        MatrixCraftMod.LOGGER.info("[MatrixCraft] Bullet Time registry initialized");
    }
    
    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        // Add to Combat tab
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(RED_PILL.get());
        }
        
        // Also add to Food & Drinks since it's consumable
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(RED_PILL.get());
        }
        
        // Add Safe Haven Obelisk to Operator Utilities (or Redstone tab as alternative)
        if (event.getTabKey() == CreativeModeTabs.OP_BLOCKS) {
            event.accept(SAFE_HAVEN_OBELISK.get());
        }
        
        // Also add to Functional Blocks
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(SAFE_HAVEN_OBELISK.get());
        }
    }
}
