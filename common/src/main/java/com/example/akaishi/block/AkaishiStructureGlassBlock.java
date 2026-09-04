package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * 结构玻璃：半透明观察窗墙块，可替代对应多方块结构的外壳
 * （反应堆 / 聚变堆 / 发生器矩阵 / 提纯矩阵 / 生命转换矩阵 / 无线终端）。
 * <p>
 * 各结构变体在 {@link ModBlocks} 中注册为独立实例，仅被对应结构的墙块判定接受；
 * 属性沿用原版玻璃（无遮挡、玻璃音效、空手可快速破坏）。
 */
public class AkaishiStructureGlassBlock extends Block {

    public AkaishiStructureGlassBlock() {
        super(Properties.copy(Blocks.GLASS));
    }
}
