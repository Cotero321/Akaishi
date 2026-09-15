package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 便捷传输构架：无线终端多方块内腔功能件（放置在终端核心附近，内腔任意非中心格）。
 * 纯结构判定方块，无方块实体。内腔含 ≥1 个本组件时，解锁便携终端（无线能源便捷终端）
 * 的「随身供能」功能：开启后终端持续把储能输入玩家携带的赤能源单元 / 饰品 / 手持赤石装备。
 * 必须放在成型结构内腔才生效；墙面放置不参与成型。
 */
public class AkaishiWirelessTransmitFrameBlock extends Block {

    public AkaishiWirelessTransmitFrameBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_CYAN)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
