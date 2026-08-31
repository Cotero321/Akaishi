package com.example.template.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 无线赤能源控制器：无线终端多方块外墙结构件（纯结构判定，无方块实体、无界面）。
 * 保留作为外墙的经典构型（终端方块 + 安全方块 + 控制器 + 外壳），使结构外观与旧版兼容。
 */
public class ChishiWirelessControllerBlock extends Block {

    public ChishiWirelessControllerBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_BLUE)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
