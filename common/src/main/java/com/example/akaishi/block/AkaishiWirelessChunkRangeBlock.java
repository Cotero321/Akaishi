package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 区块加载扩展组件：无线终端多方块内腔功能件（放置在终端核心附近，内腔任意非中心格）。
 * 纯结构判定方块，无方块实体。内腔含 ≥1 个本组件时，控制器对频道内每个输入口/输出口
 * 的弱加载范围从「口所在单区块」扩展为「以口区块为中心的 3×3 区块」——
 * 使与口相邻区块的机器（发生器/用电机等）在玩家远离时也能照常运转。
 * 必须放在成型结构内腔才生效；墙面放置不参与成型。
 */
public class AkaishiWirelessChunkRangeBlock extends Block {

    public AkaishiWirelessChunkRangeBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_GREEN)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
