package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * 粗制赤石块：由 9 个赤石晶压制而成，是赤石提纯器的原料。
 * 需铁镐以上挖掘（由 block tag 指定），掉落自身。
 */
public class AkaishiBlock extends Block {

    public AkaishiBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .requiresCorrectToolForDrops()
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL));
    }
}
