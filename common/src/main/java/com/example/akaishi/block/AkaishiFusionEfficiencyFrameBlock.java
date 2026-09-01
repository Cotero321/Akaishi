package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/** 聚变效率框架：框架层结构件，加速燃料消耗与产热（1.15^N 叠乘） */
public class AkaishiFusionEfficiencyFrameBlock extends Block {

    public AkaishiFusionEfficiencyFrameBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(6.0F, 12.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
