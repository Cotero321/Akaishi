package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 物品终端核心：物品终端多方块内腔核心方块（恰好 1 个）。
 * <p>
 * 与无线终端核心分开成独立方块，使物品终端自成一套工艺与识别色，不与无线体系混用。
 * 纯结构判定方块，无方块实体；拆掉它结构即失效（终端停止运转）。
 */
public class AkaishiItemTerminalCoreBlock extends Block {

    public AkaishiItemTerminalCoreBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
