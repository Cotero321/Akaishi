package com.example.akaishi.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 微缩矩阵终端升级组件方块（构造注入类型）：装在 <b>5×5×5 箱体的内腔</b>，
 * 由矩阵控制器扫描内腔计数后生效（同无线终端族的内腔组件范式，见 {@code AkaishiWirelessChunkRangeBlock}）。
 * <p>
 * 纯结构件：无方块实体、无界面、无自身逻辑；读数一律由控制器汇总。
 * 继承 {@link AkaishiMachineBlock} 以获得统一的机器方块属性（掉落保留、破坏行为）。
 */
public class AkaishiMiniMatrixUpgradeBlock extends AkaishiMachineBlock {

    private final AkaishiMiniMatrixUpgradeType type;

    public AkaishiMiniMatrixUpgradeBlock(AkaishiMiniMatrixUpgradeType type) {
        super(Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(3.0F)
                .sound(SoundType.METAL));
        this.type = type;
    }

    public AkaishiMiniMatrixUpgradeType type() {
        return type;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null; // 纯结构件，无方块实体
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** 悬浮说明：装入位置 + 该项作用（内腔组件无界面，提示只能挂在这里） */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.translatable("gui.akaishi.matrix.upgrade.block_hint"));
        tooltip.add(Component.translatable(type.hintKey()));
        tooltip.add(Component.translatable("gui.akaishi.matrix.upgrade.max_hint", type.maxCount()));
    }
}
