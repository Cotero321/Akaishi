package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * 赤石矿石块。
 * 掉落逻辑完全由战利品表驱动（固定数量，免疫时运/幸运），故此处无需额外逻辑。
 * 预留子类扩展点（如经验掉落、音效）。
 */
public class AkaishiOreBlock extends Block {

    public AkaishiOreBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .requiresCorrectToolForDrops()
                .strength(3.0F, 3.0F)
                .sound(SoundType.STONE));
    }
}
