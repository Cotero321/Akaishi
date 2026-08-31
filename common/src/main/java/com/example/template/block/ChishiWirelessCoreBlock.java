package com.example.template.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 无线赤能源终端核心：无线终端多方块内腔中心方块（恰好 1 个）。
 * 纯结构判定方块，无方块实体；拆掉它结构即失效（终端停止运转）。
 */
public class ChishiWirelessCoreBlock extends Block {

    public ChishiWirelessCoreBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_CYAN)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
