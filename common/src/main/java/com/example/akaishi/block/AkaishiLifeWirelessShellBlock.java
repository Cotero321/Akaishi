package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 生命无线终端外壳：生命无线终端多方块（5×5×5）墙面填充方块。
 * 纯结构判定方块，无方块实体（镜像赤能源版 {@link AkaishiWirelessShellBlock}，生命黄调）。
 */
public class AkaishiLifeWirelessShellBlock extends Block {

    public AkaishiLifeWirelessShellBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_YELLOW)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
