package com.example.akaishi.block;

import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.block.entity.AkaishiItemPipeBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
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

/**
 * 物品管道：物流网络中继方块，将物品沿链式网络多跳传输到相连的容器/机器。
 * 本身不存储物品，仅作为链路节点参与网络传输。按等级（ItemPipeTier）区分每 tick 传输个数。
 * 模型按 6 方向连接状态变化：连接相邻物品管道或可访问的容器设备时，对应方向延伸出管段。
 * 连接判定：相邻物品管道，或实现 {@link IItemPipeDevice} / {@link Container} 的设备（箱子、机器等）。
 */
public class AkaishiItemPipeBlock extends BaseEntityBlock {

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    /** 物品管道等级：基础/高级/精英/终极，每级 4 倍传输速率（个/tick），终极 64 个/tick */
    public enum ItemPipeTier {
        /** 基础：1 个/tick，初期少量自动化 */
        BASIC(1),
        /** 高级：4 个/tick */
        ADVANCED(4),
        /** 精英：16 个/tick */
        ELITE(16),
        /** 终极：64 个/tick，海量物品瞬间输送 */
        ULTIMATE(64);

        public final int transferRate;

        ItemPipeTier(int transferRate) {
            this.transferRate = transferRate;
        }
    }

    private final ItemPipeTier tier;

    public AkaishiItemPipeBlock(ItemPipeTier tier) {
        super(Properties.of().strength(3.0F, 6.0F).noOcclusion());
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    public ItemPipeTier getTier() {
        return tier;
    }

    /** 每 tick 可传输的物品个数 */
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
            if (level.getBlockEntity(pos) instanceof AkaishiItemPipeBlockEntity be) {
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
        AkaishiItemPipeBlockEntity pipe = level.getBlockEntity(pos) instanceof AkaishiItemPipeBlockEntity p ? p : null;
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

    private static boolean isDisconnected(AkaishiItemPipeBlockEntity pipe, Direction dir) {
        return pipe != null && pipe.isDisconnected(dir);
    }

    /** 判断某邻居是否可连接：同物品管道，或可访问的容器设备 */
    private boolean connectsTo(Level level, BlockPos neighborPos) {
        if (level.getBlockState(neighborPos).getBlock() instanceof AkaishiItemPipeBlock) {
            return true;
        }
        return isPipeAccessible(level.getBlockEntity(neighborPos));
    }

    /** 设备是否可被物品管道访问（本模组物流设备或任意容器） */
    public static boolean isPipeAccessible(BlockEntity be) {
        return be instanceof IItemPipeDevice || be instanceof Container;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_ITEM_PIPE.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_ITEM_PIPE.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_ITEM_PIPE.get(), AkaishiItemPipeBlockEntity::serverTick);
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
