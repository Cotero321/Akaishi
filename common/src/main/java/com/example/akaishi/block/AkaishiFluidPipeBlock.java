package com.example.akaishi.block;

import com.example.akaishi.api.fluid.IExternalFluidAccess;
import com.example.akaishi.api.fluid.IFluidPipeDevice;
import com.example.akaishi.block.entity.AkaishiFluidPipeBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.fluid.FluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 液体管道：液体网络中继方块，可将液体沿链式网络多跳传输到相连的机器/储罐。
 * 每段内置小容量缓冲罐（承接 MEK 等外部管道注入），连接状态按 6 方向邻居变化。
 * 支持配置器方向模式与单侧断开（AkaishiPipeControl），复用能量管道的连接模型。
 */
public class AkaishiFluidPipeBlock extends BaseEntityBlock {

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    public AkaishiFluidPipeBlock() {
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
            if (level.getBlockEntity(pos) instanceof AkaishiFluidPipeBlockEntity be) {
                be.markDirty();
            }
            BlockState computed = computeConnections(state, level, pos);
            if (computed != state) {
                level.setBlock(pos, computed, 3);
            }
        }
    }

    private BlockState computeConnections(BlockState state, Level level, BlockPos pos) {
        AkaishiFluidPipeBlockEntity pipe = level.getBlockEntity(pos) instanceof AkaishiFluidPipeBlockEntity p ? p : null;
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

    private static boolean isDisconnected(AkaishiFluidPipeBlockEntity pipe, Direction dir) {
        return pipe != null && pipe.isDisconnected(dir);
    }

    /** 本方块是否废料管道（放置瞬间 BE 尚未生成时用于家族判定） */
    protected boolean isWastePipeBlock() {
        return false;
    }

    /** 本方块是否等离子体管道（放置瞬间 BE 尚未生成时用于家族判定） */
    protected boolean isPlasmaPipeBlock() {
        return false;
    }

    /** 设备是否暴露了等离子体罐（仅等离子体管道可对接此类设备） */
    private static boolean hasPlasmaTank(IFluidPipeDevice device) {
        List<FluidTank> tanks = device.getFluidTanks();
        if (tanks == null) {
            return false; // 附属模组实现违约，防御性跳过
        }
        for (FluidTank tank : tanks) {
            if (device.isPlasmaTank(tank)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 邻居可连接（按管道家族隔离）：
     * 管道仅与同家族管道相连（普通/废料/等离子体三族物理隔离）；
     * 废料专用设备（废品口/保存桶）仅废料管道可接；等离子体罐设备仅等离子体管道可接；
     * 普通设备仅普通管道可接；外部液体能力仅普通管道可接。
     */
    private boolean connectsTo(Level level, BlockPos neighborPos, AkaishiFluidPipeBlockEntity pipe) {
        boolean wasteFamily = pipe != null ? pipe.isWasteFamily() : isWastePipeBlock();
        boolean plasmaFamily = pipe != null ? pipe.isPlasmaFamily() : isPlasmaPipeBlock();
        BlockEntity neighbor = level.getBlockEntity(neighborPos);
        if (neighbor instanceof AkaishiFluidPipeBlockEntity np) {
            return np.isWasteFamily() == wasteFamily && np.isPlasmaFamily() == plasmaFamily;
        }
        if (neighbor instanceof IFluidPipeDevice device) {
            // 混合接入设备（如生命活化器）普通/废料两族均可连接；仅当设备暴露等离子体罐时才允许等离子体管道
            if (device.acceptsBothFluidFamilies()) {
                return !plasmaFamily || hasPlasmaTank(device);
            }
            // 等离子体罐设备 → 仅等离子体管道可接；其余设备按废料家族匹配
            if (hasPlasmaTank(device)) {
                return plasmaFamily;
            }
            return device.isWasteOnlyDevice() == wasteFamily && !plasmaFamily;
        }
        // 跨模组：Forge capability 液体能力（仅普通管道）
        if (!wasteFamily && !plasmaFamily && IExternalFluidAccess.FluidBridge.INSTANCE != null) {
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
        return createTickerHelper(type, ModBlockEntities.CHISHI_FLUID_PIPE.get(), AkaishiFluidPipeBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // 碰撞/点选形状与细管模型一致（中心核 + 已连接方向延伸段）
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PipeShapes.build(state, NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PipeShapes.build(state, NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return PipeShapes.build(state, NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }
}
