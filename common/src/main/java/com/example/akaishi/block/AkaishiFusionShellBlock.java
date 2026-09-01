package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/** 耐高温聚变外壳：聚变堆结构最外层墙面，结构扫描的封闭壳体 */
public class AkaishiFusionShellBlock extends Block {

    public AkaishiFusionShellBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(6.0F, 12.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
