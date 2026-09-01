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
 * 矿机架构【矿机升级】：赤石矿机升级框架方块（布局编号 2），纯结构件，无界面。
 * 升级模块（速度/时运/储能方块）直接放置在其位置上替换它即生效，由控制器扫描统计。
 */
public class AkaishiMinerUpgradeFrameBlock extends AkaishiMachineBlock {

    public AkaishiMinerUpgradeFrameBlock() {
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
