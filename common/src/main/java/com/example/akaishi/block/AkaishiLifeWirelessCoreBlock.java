package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 生命无线终端核心：生命无线终端多方块内腔中心方块（恰好 1 个）。
 * 纯结构判定方块，无方块实体；拆掉它结构即失效（终端停止运转）。
 * 镜像赤能源版 {@link AkaishiWirelessCoreBlock}，生命金色调（与终端主方块同族色）。
 */
public class AkaishiLifeWirelessCoreBlock extends Block {

    public AkaishiLifeWirelessCoreBlock() {
        super(Properties.of()
                .mapColor(MapColor.GOLD)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
