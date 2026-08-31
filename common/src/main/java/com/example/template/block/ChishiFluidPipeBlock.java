package com.example.template.block;

import com.example.template.api.fluid.IExternalFluidAccess;
import com.example.template.api.fluid.IFluidPipeDevice;
import com.example.template.block.entity.ChishiFluidPipeBlockEntity;
import com.example.template.block.entity.ModBlockEntities;
import com.example.template.config.ModConfig;
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

    public ChishiFluidPipeBlock() {
        super(Properties.of().strength(3.0F, 6.0F).noOcclusion());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    public static int getTransferRate() {
        return ModConfig.fluidPipeRate;
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
            // 邻居方块变化（管道放置/拆除/设备增减）→ 网络拓扑可能变化，标记缓存失效
            if (level.getBlockEntity(pos) instanceof ChishiFluidPipeBlockEntity be) {
                be.markDirty();
            }
            BlockState computed = computeConnections(state, level, pos);
            if (computed != state) {
                level.setBlock(pos, computed, 3);
            }
        }
    }

    private BlockState computeConnections(BlockState state, Level level, BlockPos pos) {
        ChishiFluidPipeBlockEntity pipe = level.getBlockEntity(pos) instanceof ChishiFluidPipeBlockEntity p ? p : null;
        return state
                .setValue(NORTH, !isDisconnected(pipe, Direction.NORTH) && connectsTo(level, pos.relative(Direction.NORTH), pipe))
                .setValue(EAST, !isDisconnected(pipe, Direction.EAST) && connectsTo(level, pos.relative(Direction.EAST), pipe))
                .setValue(SOUTH, !isDisconnected(pipe, Direction.SOUTH) && connectsTo(level, pos.relative(Direction.SOUTH), pipe))
                .setValue(WEST, !isDisconnected(pipe, Direction.WEST) && connectsTo(level, pos.relative(Direction.WEST), pipe))
                .setValue(UP, !isDisconnected(pipe, Direction.UP) && connectsTo(level, pos.relative(Direction.UP), pipe))
                .setValue(DOWN, !isDisconnected(pipe, Direction.DOWN) && connectsTo(level, pos.relative(Direction.DOWN), pipe));
    }

    /** 配置器断开/恢复连接后重算连接状态（由调用方决定是否落盘） */
    public BlockState refreshConnections(Level level, BlockPos pos) {
        return computeConnections(level.getBlockState(pos), level, pos);
    }

    private static boolean isDisconnected(ChishiFluidPipeBlockEntity pipe, Direction dir) {
        return pipe != null && pipe.isDisconnected(dir);
    }

    /** 本方块是否废料管道（放置瞬间 BE 尚未生成时用于家族判定） */
    protected boolean isWastePipeBlock() {
        return false;
    }

    /**
     * 邻居可连接（按管道家族隔离）：
     * 管道仅与同家族管道相连；废料专用设备（废品口/保存桶）仅废料管道可接；
     * 普通设备仅普通管道可接；外部液体能力仅普通管道可接。
     */
    private boolean connectsTo(Level level, BlockPos neighborPos, ChishiFluidPipeBlockEntity pipe) {
        boolean wasteFamily = pipe != null ? pipe.isWasteFamily() : isWastePipeBlock();
        BlockEntity neighbor = level.getBlockEntity(neighborPos);
        if (neighbor instanceof ChishiFluidPipeBlockEntity np) {
            return np.isWasteFamily() == wasteFamily; // 两族管道并排放置互不连接（网络物理隔离）
        }
        if (neighbor instanceof IFluidPipeDevice device) {
            // 混合接入设备（如生命活化器）两族管道均可连接；其余按整体家族匹配
            return device.acceptsBothFluidFamilies() || device.isWasteOnlyDevice() == wasteFamily;
        }
        // 跨模组：Forge capability 液体能力（仅普通管道）
        if (!wasteFamily && IExternalFluidAccess.FluidBridge.INSTANCE != null) {
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
