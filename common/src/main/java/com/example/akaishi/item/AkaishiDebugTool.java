package com.example.akaishi.item;

import com.example.akaishi.block.AkaishiEnergyPipeBlock;
import com.example.akaishi.block.AkaishiFluidPipeBlock;
import com.example.akaishi.block.AkaishiItemPipeBlock;
import com.example.akaishi.block.entity.AkaishiPipeControl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 赤能源配置器（原调试工具）：参考 Mekanism 配置器。
 * 右键管道（赤能源管道 / 物品管道）在"正常 / 推 / 拉"三种方向模式间循环切换；
 * 潜行（shift）+ 右键则断开或恢复被点击那一侧的连接，用于精细控制网络流向。
 */
public class AkaishiDebugTool extends Item {

    public AkaishiDebugTool() {
        super(new Item.Properties());
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        BlockPos pos = ctx.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof AkaishiPipeControl pipe) {
            Player player = ctx.getPlayer();
            // 潜行 + 右键：断开/恢复被点击那一侧的连接
            if (player != null && player.isShiftKeyDown()) {
                Direction face = ctx.getClickedFace();
                boolean disconnected = pipe.toggleDisconnected(face);
                // 重算连接并落盘，使断开面的管段外观同步消失/恢复
                BlockState state = level.getBlockState(pos);
                BlockState computed = null;
                if (state.getBlock() instanceof AkaishiEnergyPipeBlock energyPipe) {
                    computed = energyPipe.refreshConnections(level, pos);
                } else if (state.getBlock() instanceof AkaishiItemPipeBlock itemPipe) {
                    computed = itemPipe.refreshConnections(level, pos);
                } else if (state.getBlock() instanceof AkaishiFluidPipeBlock fluidPipe) {
                    computed = fluidPipe.refreshConnections(level, pos);
                }
                if (computed != null && computed != state) {
                    level.setBlock(pos, computed, 3);
                }
                player.displayClientMessage(Component.translatable(
                        disconnected ? "message.akaishi.pipe.disconnected"
                                : "message.akaishi.pipe.connected"), true);
                return InteractionResult.sidedSuccess(false);
            }
            // 右键：循环切换 正常 → 推 → 拉
            int next = (pipe.getMode() + 1) % 3;
            pipe.setMode(next);
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable("message.akaishi.pipe_mode." + next), true);
            }
            return InteractionResult.sidedSuccess(false);
        }
        return InteractionResult.PASS;
    }
}
