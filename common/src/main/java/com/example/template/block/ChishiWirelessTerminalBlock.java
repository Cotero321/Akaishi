package com.example.template.block;

import com.example.template.block.entity.ChishiWirelessTerminalBlockEntity;
import com.example.template.block.entity.ModBlockEntities;
import com.example.template.wireless.WirelessNetworkManager;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
 * 无线赤能源终端方块：无线终端多方块（5×5×5）外墙主方块。
 * 必须作为外壳墙面的一部分放置；成型后成为该网络的能量中枢与 GUI 入口
 * （终端运行情况/能量储存/安全卡认证/能量传输 四个页面）。被拆时解除网络在线绑定并释放弱加载区块。
 */
public class ChishiWirelessTerminalBlock extends ChishiMachineBlock {

    /** 结构成型标记（成型后切换贴图） */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public ChishiWirelessTerminalBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_CYAN)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_WIRELESS_TERMINAL.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_WIRELESS_TERMINAL.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_WIRELESS_TERMINAL.get(),
                ChishiWirelessTerminalBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ChishiWirelessTerminalBlockEntity terminal
                && player instanceof ServerPlayer serverPlayer) {
            MenuRegistry.openExtendedMenu(serverPlayer, terminal);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ChishiWirelessTerminalBlockEntity t) {
            // 解除网络在线绑定（已认证输入口/输出口将因找不到授权终端而停止传输）
            WirelessNetworkManager.unregisterTerminal(t.terminalId());
            // 释放弱加载区块（网络区块 ticket，防泄漏）
            t.releaseChunkLoad();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
