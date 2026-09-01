package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/** 聚变核心：聚变堆正中心唯一约束点，恰好 1 个才成型 */
public class AkaishiFusionCoreBlock extends Block {

    public AkaishiFusionCoreBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(6.0F, 12.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
