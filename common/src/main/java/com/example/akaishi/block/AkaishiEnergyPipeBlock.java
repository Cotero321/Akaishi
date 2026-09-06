package com.example.akaishi.block;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.block.entity.AkaishiEnergyPipeBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.energy.EnergyPipeTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
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

/**
 * 赤能源管道：能量网络中继方块，可将赤能源沿链式网络多跳传输到任意相连的存储/机器。
 * 本身不存储能量，仅作为链路节点参与网络传输。按等级（EnergyPipeTier）区分传输速率。
 * 模型按 6 方向连接状态变化：连接相邻管道或持有对应能量类型的设备时，对应方向延伸出管段。
 * 子类（如生命能量管道）可覆盖 {@link #getEnergyType()} 传输不同能量类型。
 */
public class AkaishiEnergyPipeBlock extends BaseEntityBlock {

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    /** 本管道的等级（决定传输速率） */
    private final EnergyPipeTier tier;

    public AkaishiEnergyPipeBlock(EnergyPipeTier tier) {
        super(Properties.of().strength(3.0F, 6.0F).noOcclusion());
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    public EnergyPipeTier getTier() {
        return tier;
    }

    /** 本管道传输的能量类型（默认赤能源；生命能量管道覆盖） */
    public IEnergyType getEnergyType() {
        return AkaishiEnergyType.INSTANCE;
    }

    /** 每 tick 传输速率（默认取等级值；特殊管道可覆盖） */
    public int getTransferRate() {
        return tier.transferRate;
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
        // 仅服务端重算并落盘，客户端经区块同步获得一致状态
        if (!level.isClientSide) {
            // 邻居方块变化（管道放置/拆除/设备增减）→ 网络拓扑可能变化，标记缓存失效
            if (level.getBlockEntity(pos) instanceof AkaishiEnergyPipeBlockEntity be) {
                be.markDirty();
            }
            BlockState computed = computeConnections(state, level, pos);
            if (computed != state) {
                level.setBlock(pos, computed, 3);
            }
        }
    }

    /** 依据 6 个邻居计算连接状态（被配置器断开的方向强制不连接） */
    private BlockState computeConnections(BlockState state, Level level, BlockPos pos) {
        AkaishiEnergyPipeBlockEntity pipe = level.getBlockEntity(pos) instanceof AkaishiEnergyPipeBlockEntity p ? p : null;
        return state
                .setValue(NORTH, !isDisconnected(pipe, Direction.NORTH) && connectsTo(level, pos.relative(Direction.NORTH)))
                .setValue(EAST, !isDisconnected(pipe, Direction.EAST) && connectsTo(level, pos.relative(Direction.EAST)))
                .setValue(SOUTH, !isDisconnected(pipe, Direction.SOUTH) && connectsTo(level, pos.relative(Direction.SOUTH)))
                .setValue(WEST, !isDisconnected(pipe, Direction.WEST) && connectsTo(level, pos.relative(Direction.WEST)))
                .setValue(UP, !isDisconnected(pipe, Direction.UP) && connectsTo(level, pos.relative(Direction.UP)))
                .setValue(DOWN, !isDisconnected(pipe, Direction.DOWN) && connectsTo(level, pos.relative(Direction.DOWN)));
    }

    /** 配置器断开/恢复连接后，重算并返回最新连接状态（由调用方决定是否落盘） */
    public BlockState refreshConnections(Level level, BlockPos pos) {
        return computeConnections(level.getBlockState(pos), level, pos);
    }

    /** 该方向是否被配置器断开 */
    private static boolean isDisconnected(AkaishiEnergyPipeBlockEntity pipe, Direction dir) {
        return pipe != null && pipe.isDisconnected(dir);
    }

    /** 判断某邻居是否可连接：同能量类型的管道，或持有本类型能量的设备 */
    private boolean connectsTo(Level level, BlockPos neighborPos) {
        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof AkaishiEnergyPipeBlock neighborPipe) {
            return neighborPipe.getEnergyType() == this.getEnergyType();
        }
        if (level.getBlockEntity(neighborPos) instanceof IEnergyProvider provider) {
            return provider.getEnergyStorage(getEnergyType()) != null;
        }
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_ENERGY_PIPE.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_ENERGY_PIPE.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_ENERGY_PIPE.get(), AkaishiEnergyPipeBlockEntity::serverTick);
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
