package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 反应堆外壳：构成封闭长方体壳层的普通方块，右键无交互（仅控制器可打开界面）。
 */
public class AkaishiReactorShellBlock extends Block {

    public AkaishiReactorShellBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
