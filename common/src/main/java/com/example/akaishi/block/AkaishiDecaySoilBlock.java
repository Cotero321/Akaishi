package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * 衰竭土壤：衰竭区域内泥土被污染腐化后的终态方块。
 * <p>
 * 死亡土壤——不参与草皮蔓延、不可被锄头耕耘，仅作装饰建材与净化体系的原料产出。
 * 属性沿用泥土的轻硬度，空手/任意工具可挖，掉落自身（由 loot table 控制）。
 */
public class AkaishiDecaySoilBlock extends Block {

    public AkaishiDecaySoilBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.DIRT)
                .strength(0.5F)
                .sound(SoundType.GRAVEL));
    }
}
