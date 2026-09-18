package com.example.akaishi.block;

import com.example.akaishi.api.security.AkaishiSecurityPermission;
import com.example.akaishi.block.entity.AkaishiMiniMatrixTerminalBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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

import java.util.List;

/**
 * 微缩矩阵终端：微缩矩阵的控制器/主方块，贴在箱体墙面上。
 * <p>
 * 自身不持逻辑，成型判定与芯片盘点都在方块实体
 * （{@link AkaishiMiniMatrixTerminalBlockEntity}）里，本类只负责：
 * 方块状态里的成型标记 {@link #FORMED}、ticker 挂载、右键开界面、潜行右键手动重推安全设置。
 * <p>
 * 继承 {@link AkaishiMachineBlock} 以自动获得「破坏后数据随掉落物保留」。
 */
public class AkaishiMiniMatrixTerminalBlock extends AkaishiMachineBlock {

    /** 结构成型标记（由方块实体每次重扫后写入） */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public AkaishiMiniMatrixTerminalBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(4.0F)
                .sound(SoundType.METAL));
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_MINI_MATRIX_TERMINAL.get().create(pos, state);
    }

    /**
     * 双端都要 tick：服务端跑成型判定/场域/安全同步；客户端跑<b>只读</b>重扫，
     * 供场域屏障渲染器读取内腔升级数量（屏障是纯客户端表现，不新增同步包）。
     */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.CHISHI_MINI_MATRIX_TERMINAL.get()) {
            return null;
        }
        if (level.isClientSide) {
            return createTickerHelper(type, ModBlockEntities.CHISHI_MINI_MATRIX_TERMINAL.get(),
                    AkaishiMiniMatrixTerminalBlockEntity::clientTick);
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_MINI_MATRIX_TERMINAL.get(),
                AkaishiMiniMatrixTerminalBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** 放置时记录归属者（矩阵是主卡唯一编辑入口，归属者在此确立） */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(placer instanceof Player player)) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof AkaishiMiniMatrixTerminalBlockEntity controller) {
            controller.setOwner(player.getUUID(), player.getGameProfile().getName());
        }
    }

    /**
     * 右键：打开矩阵终端界面（三页：芯片列表 / 升级装配 / 安全认证）。
     * <b>潜行右键</b> = 手动把矩阵安全表重推给全部绑定芯片 ——
     * 正常路径下登记即自动下发（见菜单 broadcastChanges），此处是修复/兜底入口。
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof AkaishiMiniMatrixTerminalBlockEntity controller) {
            if (player.isShiftKeyDown()) {
                syncSecurity(serverPlayer, controller);
            } else {
                // 未成型必须给回执：否则界面是空白，玩家无从判断是尺寸搭错、用错方块还是内腔放了东西
                if (!controller.isFormed()) {
                    List<Component> problem = controller.describeStructureProblem();
                    if (problem == null) {
                        player.displayClientMessage(
                                Component.translatable("message.akaishi.mini_matrix.unformed_generic"), false);
                    } else {
                        problem.forEach(line -> player.displayClientMessage(line, false));
                    }
                }
                MenuRegistry.openExtendedMenu(serverPlayer, controller);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 潜行右键：安全表同步（需安全权限；未登记任何条目时按 AE2 口径全放行） */
    private static void syncSecurity(ServerPlayer player, AkaishiMiniMatrixTerminalBlockEntity controller) {
        if (!controller.security().check(player.getUUID(), AkaishiSecurityPermission.SECURITY)) {
            player.displayClientMessage(Component.translatable("message.akaishi.mini_matrix.sync_denied"), true);
            return;
        }
        player.displayClientMessage(Component.translatable("message.akaishi.mini_matrix.sync_done",
                controller.applySecurityToChips()), true);
    }

    /**
     * 拆机时释放本终端登记的无线场域（含弱加载票据）。
     * <p>
     * 与终端族的弱加载同纪律：必须挂在这里而不是方块实体的 {@code setRemoved()} ——
     * 区块卸载同样会触发 {@code setRemoved}，放在那里会「一卸载就自己把场域放掉」。
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof AkaishiMiniMatrixTerminalBlockEntity controller) {
            controller.releaseField();
            // 拆机 = 加工终止：不终止的话任务会继续倒计时并尝试往已消失的宿主里塞产物
            if (level instanceof ServerLevel serverLevel) {
                controller.abortCraft(serverLevel);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
