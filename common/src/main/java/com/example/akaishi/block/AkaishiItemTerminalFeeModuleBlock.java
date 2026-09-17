package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 物品终端费率减免组件：物品终端多方块内腔功能件（放在核心附近，内腔任意非核心格）。
 * <p>
 * 每个组件把一次性存取费率降低 10%，最多 5 个（下限 −50%）；费率口径与上限见
 * {@link com.example.akaishi.value.ItemTerminalFee}（存入 0.5% / 取出 0.25% 为基准）。
 * 超出上限的部分<b>忽略</b>（不破坏结构）。
 * <p>
 * 纯结构判定方块，无方块实体。必须放在成型结构内腔才生效；墙面放置不参与成型。
 */
public class AkaishiItemTerminalFeeModuleBlock extends Block {

    public AkaishiItemTerminalFeeModuleBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_YELLOW)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
