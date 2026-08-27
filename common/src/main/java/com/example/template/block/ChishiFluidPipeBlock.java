package com.example.template.block;

import com.example.template.api.fluid.IExternalFluidAccess;
import com.example.template.api.fluid.IFluidPipeDevice;
import com.example.template.block.entity.ChishiFluidPipeBlockEntity;
import com.example.template.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * 液体管道：液体网络中继方块，可将液体沿链式网络多跳传输到相连的机器/储罐。
 * 每段内置小容量缓冲罐（承接 MEK 等外部管道注入），连接状态按 6 方向邻居变化。
 * 支持配置器方向模式与单侧断开（ChishiPipeControl），复用能量管道的连接模型。
 */
public class ChishiFluidPipeBlock extends BaseEntityBlock {

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    /** 每段管道每 tick 传输速率（mb） */
    private static final int FLUID_PIPE_RATE = 4000;

    public ChishiFluidPipeBlock() {
        super(Properties.of().strength(3.0F, 6.0F).noOcclusion());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    public static int getTransferRate() {
        return FLUID_PIPE_RATE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return computeConnections(this.defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, isMoving);
        if (!level.isClientSide) {
            BlockState computed = computeConnections(state, level, pos);
            if (computed != state) {
                level.setBlock(pos, computed, 3);
            }
        }
    }

    private BlockState computeConnections(BlockState state, Level level, BlockPos pos) {
        ChishiFluidPipeBlockEntity pipe = level.getBlockEntity(pos) instanceof ChishiFluidPipeBlockEntity p ? p : null;
        return state
                .setValue(NORTH, !isDisconnected(pipe, Direction.NORTH) && connectsTo(level, pos.relative(Direction.NORTH)))
                .setValue(EAST, !isDisconnected(pipe, Direction.EAST) && connectsTo(level, pos.relative(Direction.EAST)))
                .setValue(SOUTH, !isDisconnected(pipe, Direction.SOUTH) && connectsTo(level, pos.relative(Direction.SOUTH)))
                .setValue(WEST, !isDisconnected(pipe, Direction.WEST) && connectsTo(level, pos.relative(Direction.WEST)))
                .setValue(UP, !isDisconnected(pipe, Direction.UP) && connectsTo(level, pos.relative(Direction.UP)))
                .setValue(DOWN, !isDisconnected(pipe, Direction.DOWN) && connectsTo(level, pos.relative(Direction.DOWN)));
    }

    /** 配置器断开/恢复连接后重算连接状态（由调用方决定是否落盘） */
    public BlockState refreshConnections(Level level, BlockPos pos) {
        return computeConnections(level.getBlockState(pos), level, pos);
    }

    private static boolean isDisconnected(ChishiFluidPipeBlockEntity pipe, Direction dir) {
        return pipe != null && pipe.isDisconnected(dir);
    }

    /** 邻居可连接：液体管道、持有液体罐的模组设备，或外部液体能力（MEK 等） */
    private boolean connectsTo(Level level, BlockPos neighborPos) {
        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof ChishiFluidPipeBlock) {
            return true;
        }
        if (level.getBlockEntity(neighborPos) instanceof IFluidPipeDevice) {
            return true;
        }
        // 跨模组：Forge capability 液体能力
        if (IExternalFluidAccess.FluidBridge.INSTANCE != null) {
            for (Direction side : Direction.values()) {
                if (IExternalFluidAccess.FluidBridge.INSTANCE.getTank(level, neighborPos, side) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_FLUID_PIPE.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_FLUID_PIPE.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_FLUID_PIPE.get(), ChishiFluidPipeBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
