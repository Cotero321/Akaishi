package com.example.template.block;

import com.example.template.block.entity.ChishiWirelessTerminalBlockEntity;
import com.example.template.menu.ChishiWirelessTerminalMenu;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 终端安全方块：无线终端多方块外墙方块（纯结构件），同时是「安全卡认证」页的直达入口。
 * 右键在附近（±5）定位成型终端方块并直接打开其 GUI 的安全卡认证页面，便于玩家快速登记身份卡。
 */
public class ChishiWirelessSecurityBlock extends Block {

    /** 终端搜索半径：结构固定边长 5，外围留余量 */
    private static final int SEARCH_RANGE = 5;

    public ChishiWirelessSecurityBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ChishiWirelessTerminalBlockEntity terminal = findTerminal(level, pos);
            if (terminal == null) {
                serverPlayer.displayClientMessage(
                        Component.translatable("message.template_mod.security.no_terminal"), false);
                return InteractionResult.sidedSuccess(false);
            }
            openSecurityPage(terminal, serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 在附近查找成型终端方块实体（任一即可，GUI 内显示成型状态） */
    private static ChishiWirelessTerminalBlockEntity findTerminal(Level level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-SEARCH_RANGE, -SEARCH_RANGE, -SEARCH_RANGE),
                pos.offset(SEARCH_RANGE, SEARCH_RANGE, SEARCH_RANGE))) {
            if (level.getBlockState(p).getBlock() instanceof ChishiWirelessTerminalBlock) {
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof ChishiWirelessTerminalBlockEntity t) {
                    return t;
                }
            }
        }
        return null;
    }

    /** 打开终端 GUI 并定位到「安全卡认证」页（复用终端方块菜单，仅初始页不同） */
    private static void openSecurityPage(ChishiWirelessTerminalBlockEntity terminal, ServerPlayer player) {
        MenuRegistry.openExtendedMenu(player, new dev.architectury.registry.menu.ExtendedMenuProvider() {
            @Override
            public Component getDisplayName() {
                return terminal.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new ChishiWirelessTerminalMenu(id, inv, terminal);
            }

            @Override
            public void saveExtraData(FriendlyByteBuf buf) {
                buf.writeBlockPos(terminal.getBlockPos());
                buf.writeInt(ChishiWirelessTerminalMenu.PAGE_SECURITY); // 直达安全卡认证页
            }
        });
    }
}
