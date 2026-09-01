package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/** 聚变隔热层：紧贴外壳内壁的一层，完整包裹才成型（结构校验） */
public class AkaishiFusionInsulationBlock extends Block {

    public AkaishiFusionInsulationBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(6.0F, 12.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
