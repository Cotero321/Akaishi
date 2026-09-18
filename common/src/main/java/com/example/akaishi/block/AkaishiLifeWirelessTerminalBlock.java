package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiLifeWirelessTerminalBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import com.example.akaishi.miniature.MiniatureCollapse;
import com.example.akaishi.wireless.WirelessNetworkManager;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 生命无线终端方块：生命无线终端多方块（5×5×5）外墙主方块。
 * 必须作为外壳墙面的一部分放置；成型后成为该生命网络的能量中枢与 GUI 入口
 * （终端运行情况/能量储存/安全卡认证/能量传输 四个页面）。被拆时解除网络在线绑定并释放弱加载 ticket。
 * 内腔可放置跨维、区块加载、范围与输入/输出损耗抑制组件。
 */
public class AkaishiLifeWirelessTerminalBlock extends AkaishiMachineBlock {

    /** 结构成型标记（成型后切换贴图） */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public AkaishiLifeWirelessTerminalBlock() {
        super(Properties.of()
                .mapColor(MapColor.GOLD)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FORMED);
    }

    /**
     * 记录归属者：终端「归属者恒全权限」是安全系统的根（AE2 口径：谁放安全终端谁是主人）。
     * 结构主方块被放置时写入，之后不随拆装结构改变。
     */
    @Override
    public void setPlacedBy(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
            BlockState state, net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(placer instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof com.example.akaishi.block.entity.AkaishiLifeWirelessTerminalBlockEntity be) {
            be.setOwner(player.getUUID(), player.getGameProfile().getName());
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_LIFE_WIRELESS_TERMINAL.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_LIFE_WIRELESS_TERMINAL.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_LIFE_WIRELESS_TERMINAL.get(),
                AkaishiLifeWirelessTerminalBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // 潜行 + 可微缩终端 ⇒ 坍缩为单方块（命中即拦截，不再开界面）
        if (MiniatureCollapse.handleSneakUse(level, pos, player)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AkaishiLifeWirelessTerminalBlockEntity terminal
                && player instanceof ServerPlayer serverPlayer) {
            MenuRegistry.openExtendedMenu(serverPlayer, terminal);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AkaishiLifeWirelessTerminalBlockEntity t) {
            // 解除网络在线绑定（已认证输入口/输出口将因找不到授权终端而停止传输）
            WirelessNetworkManager.unregisterTerminal(t.terminalId());
            t.releaseChunkLoad();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
