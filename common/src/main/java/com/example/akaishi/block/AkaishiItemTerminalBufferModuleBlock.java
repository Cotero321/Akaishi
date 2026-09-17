package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 物品终端缓冲扩展组件：物品终端多方块内腔功能件（放在核心附近，内腔任意非核心格）。
 * <p>
 * 每个组件把终端<b>自身的赤能源缓冲容量</b>提高 {@link #BONUS_ENERGY}，从而抬高单笔可结算上限
 * （单笔上限 = 缓冲能承担的最大费用对应的 IP，见 {@code ItemTerminalFee.maxIp}）。
 * 注意它不改「供能」能力 —— 缓冲填得满仍取决于能源接入口的数量与储量。
 * <p>
 * 纯结构判定方块，无方块实体；超出 {@link #MAX_EFFECTIVE} 的部分<b>忽略</b>（不破坏结构）。
 * 必须放在成型结构内腔才生效；墙面放置不参与成型。
 */
public class AkaishiItemTerminalBufferModuleBlock extends Block {

    /** 每个组件增加的缓冲容量（赤能源） */
    public static final long BONUS_ENERGY = 1_000_000L;
    /** 生效上限：超出部分忽略 */
    public static final int MAX_EFFECTIVE = 8;

    public AkaishiItemTerminalBufferModuleBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
