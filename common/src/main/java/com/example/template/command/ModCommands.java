package com.example.template.command;

import com.example.template.block.ModBlocks;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 测试指令：/chishi_geode [radius]
 * 扫描玩家周围已加载区块中的赤石水晶母岩，传送到最近的赤石晶洞。
 * 用途：开发阶段快速验证晶洞世界生成，无需手动挖矿寻找。
 * 由 Forge RegisterCommandsEvent 触发 build() 注册。
 */
public final class ModCommands {

    /** 赤石水晶母岩方块（4 个等级） */
    private static final List<Block> GEODES = List.of(
            ModBlocks.CHISHI_GEODE_FLAWED.get(),
            ModBlocks.CHISHI_GEODE_NORMAL.get(),
            ModBlocks.CHISHI_GEODE_PRISTINE.get(),
            ModBlocks.CHISHI_GEODE_PERFECT.get());

    /** 扫描步长：跳过部分方块换取速度（晶洞体积远超步长，不会漏掉） */
    private static final int SCAN_STEP = 4;

    private ModCommands() {
    }

    /** 注册 /chishi_geode 指令（由 Forge RegisterCommandsEvent 调用） */
    public static void build(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("chishi_geode")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> teleportToNearestGeode(ctx, 128)));
    }

    private static int teleportToNearestGeode(CommandContext<CommandSourceStack> ctx, int radius)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();

        // 扫描以玩家为中心的正方体区域（未加载区块 getBlockState 返回空气，安全）
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        for (int x = -radius; x <= radius; x += SCAN_STEP) {
            for (int z = -radius; z <= radius; z += SCAN_STEP) {
                for (int y = minY; y <= maxY; y += SCAN_STEP) {
                    BlockPos pos = center.offset(x, y, z);
                    if (isGeode(level.getBlockState(pos))) {
                        double dist = center.distSqr(pos);
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearest = pos;
                        }
                    }
                }
            }
        }

        if (nearest == null) {
            source.sendSuccess(() -> Component.literal("附近未找到赤石晶洞，请移动或扩大范围"), false);
            return 0;
        }

        // 从母岩向上寻找安全落脚点（连续 2 格空气，避免传送到方块内部）
        BlockPos land = nearest.above();
        for (int y = nearest.getY() + 1; y <= Math.min(nearest.getY() + 32, maxY); y++) {
            BlockPos feet = new BlockPos(nearest.getX(), y, nearest.getZ());
            if (level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()) {
                land = feet;
                break;
            }
        }

        player.teleportTo(land.getX() + 0.5, land.getY(), land.getZ() + 0.5);
        BlockPos finalNearest = nearest;
        source.sendSuccess(() -> Component.literal("已传送至赤石晶洞: "
                + finalNearest.getX() + ", " + finalNearest.getY() + ", " + finalNearest.getZ()), false);
        return 1;
    }

    private static boolean isGeode(BlockState state) {
        for (Block block : GEODES) {
            if (state.is(block)) {
                return true;
            }
        }
        return false;
    }
}
