package com.raeyncraft.matrixcraft.registry;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.item.TheObeliskItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    
    public static final DeferredRegister.Items ITEMS = 
        DeferredRegister.createItems(MatrixCraftMod.MODID);
    
    public static final DeferredItem<Item> THE_OBELISK = ITEMS.register(
        "the_obelisk",
        () -> new TheObeliskItem(new Item.Properties())
    );
}