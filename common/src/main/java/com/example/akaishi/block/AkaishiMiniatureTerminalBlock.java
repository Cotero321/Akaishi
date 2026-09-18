package com.example.akaishi.block;

import com.example.akaishi.block.entity.MiniatureTerminalBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import com.example.akaishi.wireless.ItemTerminalRegistry;
import com.example.akaishi.wireless.WirelessNetworkManager;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 微缩终端方块：把一台完整终端（多方块）连同数据与接口能力浓缩成的<b>单方块</b>。
 * <p>
 * 方块本体是通用壳：数据、能力、界面全部由方块实体里的「终端族状态」决定
 * （见 {@code api/miniature} 的适配器与状态契约），因此新增终端族不需要动本类。
 * <p>
 * 获取路径只有一条：<b>对已成型终端使用微缩</b>（坍缩动作会把整套结构消耗掉并原地留下本方块）。
 * 被拆时数据随掉落物保留（{@code AkaishiMachineBlock} + {@code IDataCarrier}）。
 */
public class AkaishiMiniatureTerminalBlock extends AkaishiMachineBlock {

    public AkaishiMiniatureTerminalBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(4.0F)
                .sound(SoundType.METAL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_MINIATURE_TERMINAL.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.CHISHI_MINIATURE_TERMINAL.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_MINIATURE_TERMINAL.get(),
                MiniatureTerminalBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** 右键打开该终端族的界面（无界面族不响应；未装载数据的空壳也不响应） */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof MiniatureTerminalBlockEntity terminal
                && terminal.hasMenu()) {
            MenuRegistry.openExtendedMenu(serverPlayer, terminal);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * 方块被拆：立即从终端注册表摘除（不等心跳超时，否则拆后 40 tick 内仍会被储存口列为可绑定终端）。
     * <p>
     * 注意与物品终端同口径：必须挂在 {@code onRemove} 而不是方块实体的 {@code setRemoved()}
     * —— 区块卸载同样会触发 {@code setRemoved}，挂那里会导致"一卸载就注销"。
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof MiniatureTerminalBlockEntity terminal) {
            ItemTerminalRegistry.unregister(terminal.terminalId());
            // 能量族微缩件同时解除无线网络在线绑定（端口/便携终端将因找不到终端而停止传输，口径同原终端方块）
            if (terminal.terminalId() != null) {
                WirelessNetworkManager.unregisterTerminal(terminal.terminalId());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * 放置完成后把方块实体数据推给客户端。
     * <p>
     * 芯片是"物品 → 方块"形态互转的：物品里的 {@code BlockEntityTag} 由原版在放置时还原到方块实体，
     * 但原版不会因此自动发方块实体数据包，客户端拿不到数据就开不出界面（菜单按坐标取本地实体）。
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof MiniatureTerminalBlockEntity terminal) {
            terminal.syncToClient();
        }
    }
}
