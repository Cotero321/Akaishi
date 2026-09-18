package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiItemPortBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 储存无线输入口：把面朝方向容器的物品抓进绑定的物品终端（照 AE2 输入总线）。
 * <p>
 * 方向语义唯一真源是本方块的 {@link #FACING}——{@link AkaishiItemPortBlockEntity} 只对
 * {@code portPos.relative(FACING)} 处的容器操作，因此放置时口朝点击面（贴着被点击的容器）。
 * 右键打开界面（运行 / 远程绑定）；搬运由方块实体的 serverTick 驱动（输出=false 的抓取侧）。
 */
public class AkaishiItemInputPortBlock extends AkaishiMachineBlock {

    /** 口朝向（指向对面被操作的容器） */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public AkaishiItemInputPortBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(3.5F)
                .sound(SoundType.METAL));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** 放置时口朝点击面：FACING 指向被点击的方块（即要操作的容器） */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_ITEM_INPUT_PORT.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_ITEM_INPUT_PORT.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_ITEM_INPUT_PORT.get(),
                AkaishiItemPortBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** 右键打开界面（运行 / 远程绑定，走 ExtendedMenuProvider 携带方块坐标） */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof AkaishiItemPortBlockEntity port) {
            MenuRegistry.openExtendedMenu(serverPlayer, port);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
