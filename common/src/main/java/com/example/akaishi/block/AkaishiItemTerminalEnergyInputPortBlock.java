package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiItemTerminalEnergyInputPortBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/**
 * 物品终端赤能源接入口：贴装于终端结构外侧 1 格的纯汇口（仅管道注入，无手动界面）。
 * <p>
 * 口内只维护自身缓冲，<b>不做每 tick 向终端转发</b>（D13 单一能量入口路径）：
 * 终端在存入 / 取出结算时主动扫描外围 1 格并抽干各口，因此本方块无需 ticker，零 tick 开销。
 */
public class AkaishiItemTerminalEnergyInputPortBlock extends AkaishiMachineBlock {

    public AkaishiItemTerminalEnergyInputPortBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(4.0F)
                .sound(SoundType.METAL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_ITEM_TERMINAL_ENERGY_INPUT.get().create(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
