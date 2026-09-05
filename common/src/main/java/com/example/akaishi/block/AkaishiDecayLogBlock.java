package com.example.akaishi.block;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;

/**
 * 衰竭木：衰竭区域内一切原木（minecraft:logs 标签）被污染后的终态柱状方块。
 * <p>
 * 保留原木的轴向（axis）属性，使被污染树木维持原有朝向；属性沿袭原版橡木
 * （硬度 2.0、木头音效），破坏后掉落自身。
 */
public class AkaishiDecayLogBlock extends RotatedPillarBlock {

    public AkaishiDecayLogBlock() {
        super(Properties.copy(Blocks.OAK_LOG));
    }
}
