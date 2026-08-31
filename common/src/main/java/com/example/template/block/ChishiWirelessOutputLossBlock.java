package com.example.template.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 输出损耗抑制组件：无线终端多方块内腔功能件（放置在终端核心附近，内腔任意非中心格）。
 * 纯结构判定方块，无方块实体。内腔每含 1 个本组件，输出口方向（频道储能 → 缓冲）的
 * 传输损耗按配置值（默认 5%）比例降低，可叠加（多个组件线性累加，最高削减 90%）。
 * 必须放在成型结构内腔才生效；墙面放置不参与成型。
 */
public class ChishiWirelessOutputLossBlock extends Block {

    public ChishiWirelessOutputLossBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
