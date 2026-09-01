package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/** 聚变燃料框架：框架层结构件，每个解锁控制器 1 个燃料槽（上限 4） */
public class AkaishiFusionFuelFrameBlock extends Block {

    public AkaishiFusionFuelFrameBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(6.0F, 12.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
