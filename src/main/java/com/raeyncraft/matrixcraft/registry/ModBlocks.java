package com.raeyncraft.matrixcraft.registry;

import com.raeyncraft.matrixcraft.MatrixCraftMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MatrixCraftMod.MODID);

    public static final DeferredBlock<Block> SAFE_HAVEN_OBELISK =
            BLOCKS.register("safe_haven_obelisk",
                () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.LODESTONE)));
}
