package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiFusionCoolerFrameBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/** 聚变散热框架：框架层结构件，内嵌 1 个散热片槽（插聚变散热片），成型后参与散热结算 */
public class AkaishiFusionCoolerFrameBlock extends AkaishiMachineBlock {

    public AkaishiFusionCoolerFrameBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_FUSION_COOLER_FRAME.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_FUSION_COOLER_FRAME.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_FUSION_COOLER_FRAME.get(),
                AkaishiFusionCoolerFrameBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
