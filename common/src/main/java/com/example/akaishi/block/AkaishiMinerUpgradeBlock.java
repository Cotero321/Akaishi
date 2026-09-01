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
 * 矿机升级模块方块（速度/时运/储能三种，构造注入类型）：以方块形式直接安装
 * 在矿机结构的升级框架位置上（替换矿机架构【矿机升级】），控制器扫描统计生效。
 * 纯结构件，无方块实体、无界面。
 */
public class AkaishiMinerUpgradeBlock extends AkaishiMachineBlock {

    private final AkaishiMinerUpgradeType type;

    public AkaishiMinerUpgradeBlock(AkaishiMinerUpgradeType type) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(4.0F)
                .sound(SoundType.METAL));
        this.type = type;
    }

    public AkaishiMinerUpgradeType type() {
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
}
