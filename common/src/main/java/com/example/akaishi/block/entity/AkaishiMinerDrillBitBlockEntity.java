package com.example.akaishi.block.entity;

import com.example.akaishi.block.AkaishiMinerControllerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 钻机钻头方块实体：仅承载成型状态查询（结构最底层中心，固定在控制器下方两格），
 * 服务端无逻辑；客户端渲染器据此在成型时绘制向下的信标光束。
 */
public class AkaishiMinerDrillBitBlockEntity extends BlockEntity {

    public AkaishiMinerDrillBitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_MINER_DRILL_BIT.get(), pos, state);
    }

    /** 所属结构是否成型：钻头固定在控制器正下方两格（dy0 对 dy2），直接读控制器 FORMED 状态 */
    public boolean isPartOfFormedStructure() {
        if (level == null) {
            return false;
        }
        BlockState controller = level.getBlockState(worldPosition.offset(0, 2, 0));
        return controller.getBlock() instanceof AkaishiMinerControllerBlock
                && controller.getValue(AkaishiMinerControllerBlock.FORMED);
    }
}
