package com.example.template.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 无线赤能源终端外壳：无线终端多方块墙面填充方块。
 * 纯结构判定方块，无方块实体。
 */
public class ChishiWirelessShellBlock extends Block {

    public ChishiWirelessShellBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
