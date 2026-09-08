package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/** 生命无线终端区块加载范围组件。 */
public class AkaishiLifeWirelessChunkRangeBlock extends Block {

    public AkaishiLifeWirelessChunkRangeBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_GREEN)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
