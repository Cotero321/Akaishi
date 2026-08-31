package com.example.template.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 终端跨维组件：无线终端多方块内腔功能件（放置在终端核心附近，内腔任意非中心格）。
 * 纯结构判定方块，无方块实体。内腔含 ≥1 个本组件时，控制器向频道解锁跨维度传输
 * （输入口/输出口可与异维度终端互连，损耗按跨维固定值计算）。
 * 必须放在成型结构内腔才生效；墙面放置不参与成型。
 */
public class ChishiWirelessDimBridgeBlock extends Block {

    public ChishiWirelessDimBridgeBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
