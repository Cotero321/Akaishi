package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 聚变散热框架：框架层纯结构件，不再持有散热片/方块实体。
 * 每个框架为控制器解锁 1 个散热片槽（上限 10）；散热片统一存放在控制器散热页。
 */
public class AkaishiFusionCoolerFrameBlock extends Block {

    public AkaishiFusionCoolerFrameBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
