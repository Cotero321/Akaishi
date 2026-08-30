package com.example.template.block;

import com.example.template.block.entity.ChishiAdvancedPurifierBlockEntity;
import com.example.template.block.entity.ChishiPurifierBlockEntity;
import com.example.template.block.entity.ModBlockEntities;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 高级提纯构建方块：直接消耗赤能源将粗制赤石块/赤石水晶块提纯为赤石精华（无燃料槽）。
 * 作为"提纯矩阵"（3×3×3）外壳时（formed=true）休眠，由中心提纯器集中提纯。
 */
public class ChishiAdvancedPurifierBlock extends ChishiMachineBlock {

    /** 是否为提纯矩阵外壳（休眠标记，由中心提纯器写入） */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public ChishiAdvancedPurifierBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(3.5F)
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
        return ModBlockEntities.CHISHI_ADVANCED_PURIFIER.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_ADVANCED_PURIFIER.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_ADVANCED_PURIFIER.get(), ChishiAdvancedPurifierBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // 仅作为提纯矩阵外壳时可用：右键代理打开中心提纯器界面；单放无界面
        if (state.getValue(FORMED)) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                BlockPos center = findMatrixCenter(level, pos);
                if (center != null && level.getBlockEntity(center) instanceof ChishiPurifierBlockEntity purifier) {
                    MenuRegistry.openExtendedMenu(serverPlayer, purifier);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /** 在自身为中心的 3×3×3 范围内查找成型的提纯矩阵中心（普通提纯器） */
    private static BlockPos findMatrixCenter(Level level, BlockPos shellPos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos p = shellPos.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof ChishiPurifierBlock
                            && s.getValue(ChishiPurifierBlock.FORMED)) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        // 外壳方块放置：可能使周围 3×3×3 内提纯器中心成型，通知其重扫结构缓存
        if (!level.isClientSide) {
            ChishiPurifierBlockEntity.notifyNearbyCenters(level, pos);
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiAdvancedPurifierBlockEntity purifier) {
                Containers.dropContents(level, pos, purifier.inventory());
            }
            // 外壳方块被移除：周围中心的结构缓存失效，通知其重扫
            if (!level.isClientSide) {
                ChishiPurifierBlockEntity.notifyNearbyCenters(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
