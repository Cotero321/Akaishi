package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 物品终端外壳：物品终端多方块墙面填充方块。
 * <p>
 * 与无线赤能源终端族<b>完全隔离</b>：只被
 * {@link com.example.akaishi.multiblock.ItemTerminalStructure} 接受，无线终端结构不认它；
 * 反之无线外壳也不被物品终端结构接受，两个体系不可能共用一个箱体。
 * 纯结构判定方块，无方块实体。
 */
public class AkaishiItemTerminalShellBlock extends Block {

    public AkaishiItemTerminalShellBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_CYAN)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
