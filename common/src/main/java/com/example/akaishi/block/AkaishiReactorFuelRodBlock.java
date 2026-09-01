package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 燃料棒组件：置于反应堆内腔，每根解锁 1 个燃料槽（满配 10 根）。
 * 无方块实体，结构检测由控制器扫描计数；右键无交互。
 */
public class AkaishiReactorFuelRodBlock extends Block {

    public AkaishiReactorFuelRodBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_GRAY)
                .strength(4.0F, 5.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
