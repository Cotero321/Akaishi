package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * 提纯矩阵外壳：类反应堆式矩阵的外壁方块。
 * 无方块实体，仅作为结构判定的一部分；可被三种端口（物品输入/物品输出/能量输入）替代。
 */
public class AkaishiPurifierMatrixCasingBlock extends Block {

    public AkaishiPurifierMatrixCasingBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(3.5F)
                .sound(SoundType.METAL));
    }
}
