package com.example.template.block;

import com.example.template.block.entity.ChishiLifeAggregationConverterBlockEntity;
import com.example.template.block.entity.ChishiLifeConversionArchitectureBlockEntity;
import com.example.template.block.entity.ModBlockEntities;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 生命聚合转换器：单方块即可独立工作（消耗赤能源聚合出生命能量），
 * 也可作为"生命转换架构"（3×3×3）的 26 台外壳。成型后外壳休眠，
 * 能量访问与右键交互均代理到中心主方块。
 */
public class ChishiLifeAggregationConverterBlock extends BaseEntityBlock {

    /** 结构是否成型（作为生命转换架构外壳） */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public ChishiLifeAggregationConverterBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_GREEN)
                .strength(5.0F)
                .sound(SoundType.METAL));
        this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_LIFE_AGGREGATION_CONVERTER.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_LIFE_AGGREGATION_CONVERTER.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_LIFE_AGGREGATION_CONVERTER.get(),
                ChishiLifeAggregationConverterBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof ChishiLifeAggregationConverterBlockEntity converter) {
                // 成型后为外壳：代理打开中心"生命转换架构"界面；未成型则打开自身界面
                ChishiLifeConversionArchitectureBlockEntity arch = converter.findArchitecture();
                if (arch != null) {
                    MenuRegistry.openExtendedMenu(serverPlayer, arch);
                } else {
                    MenuRegistry.openExtendedMenu(serverPlayer, converter);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
