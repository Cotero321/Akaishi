package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/** 生命无线终端跨维组件。 */
public class AkaishiLifeWirelessDimBridgeBlock extends Block {

    public AkaishiLifeWirelessDimBridgeBlock() {
        super(Properties.of()
                .mapColor(MapColor.GOLD)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
