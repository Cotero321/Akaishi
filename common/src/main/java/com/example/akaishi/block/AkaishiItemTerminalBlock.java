package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiItemTerminalBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
 * 物品终端方块：物品终端多方块（5×5×5，与无线赤能源终端同族壳体）外墙主方块。
 * <p>
 * 复用 {@link com.example.akaishi.wireless.WirelessTerminalStructure} 做结构判定，内腔核心仍为
 * 共享的无线终端核心；本体不参与无线能量网络，仅作为 IP 物品库的 GUI 入口，
 * 外侧 1 格贴装各阶物品储存单元与赤能源接入口。
 */
public class AkaishiItemTerminalBlock extends AkaishiMachineBlock {

    /** 结构成型标记（成型后切换贴图） */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public AkaishiItemTerminalBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_ITEM_TERMINAL.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.CHISHI_ITEM_TERMINAL.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_ITEM_TERMINAL.get(),
                AkaishiItemTerminalBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** 右键打开物品终端界面（与其它机器同范式：仅服务端开菜单，客户端看结果） */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AkaishiItemTerminalBlockEntity terminal) {
                MenuRegistry.openExtendedMenu(serverPlayer, terminal);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * 拆机时释放弱加载票据（内腔区块加载构架带来的）。
     * <p>
     * 必须挂在这里而不是方块实体的 {@code setRemoved()}：区块卸载同样会触发 {@code setRemoved}，
     * 放在那里会导致「一卸载就自己把票放掉」，弱加载机制自废。
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof AkaishiItemTerminalBlockEntity terminal) {
            terminal.releaseChunkLoad();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
