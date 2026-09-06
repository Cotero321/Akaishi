package com.example.akaishi.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/**
 * 矿机架构【外接】：赤石矿机立柱专用结构块（边立柱位可替换件之一），纯结构件，无界面。
 * 可作为外接管道/线缆的替换接入位，结构判定上与矿机架构等价。
 */
public class AkaishiMinerExternalFrameBlock extends AkaishiMachineBlock {

    public AkaishiMinerExternalFrameBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(4.0F)
                .sound(SoundType.METAL));
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
}
