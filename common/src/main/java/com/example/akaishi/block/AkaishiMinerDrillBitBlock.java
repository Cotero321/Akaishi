package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiMinerDrillBitBlockEntity;
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
 * 钻机钻头：赤石矿机结构最底层的中心柱方块（9×9×5 结构 dy0 中心）。
 * 结构成型时客户端渲染器在钻头下方绘制向下的信标光束；
 * 方块实体仅承载成型状态查询，服务端无逻辑。
 */
public class AkaishiMinerDrillBitBlock extends AkaishiMachineBlock {

    public AkaishiMinerDrillBitBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(6.0F)
                .sound(SoundType.METAL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_MINER_DRILL_BIT.get().create(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
