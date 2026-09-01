package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * 生命转换矩阵外壳：类反应堆式矩阵的外壁方块。
 * 无方块实体，仅作为结构判定的一部分；可被能量输入/输出口替代。
 */
public class AkaishiLifeMatrixCasingBlock extends Block {

    public AkaishiLifeMatrixCasingBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(3.5F)
                .sound(SoundType.METAL));
    }
}
