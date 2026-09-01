package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 反应核心：置于反应堆内腔的燃烧结算核心，每个反应堆必须且只能有 1 个。
 * 无方块实体，由控制器统一执行燃烧/温度/熔毁结算；右键无交互。
 */
public class AkaishiReactorCoreBlock extends Block {

    public AkaishiReactorCoreBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .lightLevel(state -> 8)
                .requiresCorrectToolForDrops());
    }
}
