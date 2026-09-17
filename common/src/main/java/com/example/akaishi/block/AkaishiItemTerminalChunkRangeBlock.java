package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 物品终端区块加载扩展组件：物品终端多方块内腔功能件（放在核心附近，内腔任意非核心格）。
 * <p>
 * 内腔含 ≥1 个本组件时，各弱加载目标区块的范围从「单区块」扩为「向外各 1 区块」（即 3×3），
 * 使与结构相邻区块的机器在玩家远离时也能照常运转。需要同时存在
 * {@link AkaishiItemTerminalChunkLoaderBlock} 才有意义。
 * <p>
 * 纯结构判定方块，无方块实体；超出 1 个的部分忽略。
 * 必须放在成型结构内腔才生效；墙面放置不参与成型。
 */
public class AkaishiItemTerminalChunkRangeBlock extends Block {

    public AkaishiItemTerminalChunkRangeBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_GREEN)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
