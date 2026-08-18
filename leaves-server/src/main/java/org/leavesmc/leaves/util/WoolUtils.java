package org.leavesmc.leaves.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

import static java.util.Map.entry;

public class WoolUtils {
    private static final Map<Block, DyeColor> WOOL_BLOCK_TO_DYE = Map.ofEntries(
        entry(Blocks.WOOL.pick(DyeColor.WHITE), DyeColor.WHITE),
        entry(Blocks.WOOL.pick(DyeColor.ORANGE), DyeColor.ORANGE),
        entry(Blocks.WOOL.pick(DyeColor.MAGENTA), DyeColor.MAGENTA),
        entry(Blocks.WOOL.pick(DyeColor.LIGHT_BLUE), DyeColor.LIGHT_BLUE),
        entry(Blocks.WOOL.pick(DyeColor.YELLOW), DyeColor.YELLOW),
        entry(Blocks.WOOL.pick(DyeColor.LIME), DyeColor.LIME),
        entry(Blocks.WOOL.pick(DyeColor.PINK), DyeColor.PINK),
        entry(Blocks.WOOL.pick(DyeColor.GRAY), DyeColor.GRAY),
        entry(Blocks.WOOL.pick(DyeColor.LIGHT_GRAY), DyeColor.LIGHT_GRAY),
        entry(Blocks.WOOL.pick(DyeColor.CYAN), DyeColor.CYAN),
        entry(Blocks.WOOL.pick(DyeColor.PURPLE), DyeColor.PURPLE),
        entry(Blocks.WOOL.pick(DyeColor.BLUE), DyeColor.BLUE),
        entry(Blocks.WOOL.pick(DyeColor.BROWN), DyeColor.BROWN),
        entry(Blocks.WOOL.pick(DyeColor.GREEN), DyeColor.GREEN),
        entry(Blocks.WOOL.pick(DyeColor.RED), DyeColor.RED),
        entry(Blocks.WOOL.pick(DyeColor.BLACK), DyeColor.BLACK)
    );

    public static DyeColor getWoolColorAtPosition(Level worldIn, BlockPos pos) {
        BlockState state = worldIn.getBlockState(pos);
        return WOOL_BLOCK_TO_DYE.get(state.getBlock());
    }
}
