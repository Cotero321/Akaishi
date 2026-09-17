package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 物品终端区块加载构架：物品终端多方块内腔功能件（放在核心附近，内腔任意非核心格）。
 * <p>
 * 内腔含 ≥1 个本组件时，终端自身会对其所在区块<b>以及全部贴装单元/赤能源接入口所在区块</b>
 * 施加弱加载票据（{@code TicketType.PORTAL}，免费、不收税，与无线终端族同口径），
 * 使玩家远离后终端仍持续运转、跨区块的贴装件仍可被读到（否则容量会凭空少一截）。
 * <p>
 * 与无线族不同的取舍：票据由<b>终端统一管理</b>（不另设方块实体），因此本组件是纯结构判定方块。
 * 纯结构判定方块，无方块实体；超出 1 个的部分忽略。
 * 必须放在成型结构内腔才生效；墙面放置不参与成型。
 */
public class AkaishiItemTerminalChunkLoaderBlock extends Block {

    public AkaishiItemTerminalChunkLoaderBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_GREEN)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
