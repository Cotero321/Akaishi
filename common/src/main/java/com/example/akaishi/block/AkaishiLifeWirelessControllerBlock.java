package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 生命无线终端控制器：生命无线终端多方块外墙结构件（纯结构判定，无方块实体、无界面）。
 * 保留作为外墙的经典构型（终端方块 + 控制器 + 外壳），使结构外观与赤能源版兼容。
 * 镜像赤能源版 {@link AkaishiWirelessControllerBlock}，生命白调。
 */
public class AkaishiLifeWirelessControllerBlock extends Block {

    public AkaishiLifeWirelessControllerBlock() {
        super(Properties.of()
                .mapColor(MapColor.SNOW)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
