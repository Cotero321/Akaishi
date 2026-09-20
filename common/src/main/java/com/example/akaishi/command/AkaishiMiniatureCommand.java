package com.example.akaishi.command;

import com.example.akaishi.block.AkaishiItemTerminalBlocks;
import com.example.akaishi.block.AkaishiMiniMatrixBlocks;
import com.example.akaishi.block.AkaishiMiniMatrixUpgradeType;
import com.example.akaishi.block.entity.AkaishiItemTerminalBlockEntity;
import com.example.akaishi.block.entity.AkaishiMiniMatrixTerminalBlockEntity;
import com.example.akaishi.block.entity.MiniatureTerminalBlockEntity;
import com.example.akaishi.craft.VirtualCraftTask;
import com.example.akaishi.multiblock.ItemTerminalStructure;
import com.example.akaishi.multiblock.MatrixStructure;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * 微缩终端的联调指令：<b>一条命令生成一台已成型的物品终端</b>。
 * <p>
 * 存在的理由：物品终端是 5×5×5 多方块，手工搭一套（外壳 + 结构玻璃 + 内腔核心 + 储存单元）才能测微缩，
 * 联调成本极高且极易漏一块而"看起来没反应"。本指令按 {@code ItemTerminalStructure} 的校验规则
 * 精确生成一套成型结构，并顺手放好联调需要的贴装件：
 * <ul>
 *   <li>墙内嵌 1 个储存单元（在箱体内 ⇒ 坍缩时随结构消耗）；</li>
 *   <li>结构<b>外侧 1 格</b>再贴 1 个储存单元（在箱体外 ⇒ 检验"贴装件必须一并消耗"这条防复制不变量）；</li>
 *   <li>外侧贴 1 个赤能源接入口，并给终端缓冲预充电，便于验证存取计费。</li>
 * </ul>
 * 仅开发联调用（权限等级 2）。
 * <p>
 * 另含 {@code /akaishi_miniature matrix}：生成一套成型的<b>微缩矩阵</b>（5×5×5 空腔箱体，
 * 控制器贴玩家侧墙面），供芯片识别（P1a）联调。
 */
public final class AkaishiMiniatureCommand {

    /** 预充的赤能源（够跑很多笔存取，方便测费用与"赤能源不足"红字） */
    private static final long PRELOAD_ENERGY = 2_000_000L;

    private AkaishiMiniatureCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("akaishi_miniature")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("build").executes(AkaishiMiniatureCommand::buildItemTerminal))
                .then(Commands.literal("matrix").executes(AkaishiMiniatureCommand::buildMiniMatrix))
                // 加工链路的状态回执（没有界面时靠它确认"真的在跑"）
                .then(Commands.literal("status").executes(AkaishiMiniatureCommand::status)));
    }

    /** 状态回执：矩阵是否成型、场域与联动状态、当前加工任务 */
    private static int status(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        AkaishiMiniMatrixTerminalBlockEntity matrix = findNearbyMatrix(player.serverLevel(),
                player.blockPosition(), 12);
        if (matrix == null) {
            ctx.getSource().sendFailure(Component.literal("12 格内没有微缩矩阵控制器"));
            return 0;
        }
        VirtualCraftTask craft = matrix.craftTask();
        String craftLine = craft == null
                ? (matrix.lastCraftFail() == null ? "无" : "无（上次失败：" + matrix.lastCraftFail() + "）")
                : craft.plan().target().getHoverName().getString()
                + "（节点 " + craft.completedSteps() + "/" + craft.totalSteps()
                + " · 预计剩余 " + craft.remainingTicks() + " tick）";
        // 未成型时附上诊断（译名由客户端渲染，服务端只带 key 与参数）
        List<Component> problem = matrix.describeStructureProblem();
        ctx.getSource().sendSuccess(() -> {
            Component text = Component.literal("矩阵成型：" + matrix.isFormed()
                    + " | 芯片 " + matrix.chipCount() + " 枚"
                    + "\n场域半径：" + matrix.fieldRadiusChunks() + " 区块 | 已申领节点："
                    + matrix.claimedNodeCount() + "/" + AkaishiMiniMatrixUpgradeType.EXTEND.maxCount()
                    + " | 联动升级："
                    + (matrix.hasUpgrade(AkaishiMiniMatrixUpgradeType.LINK) ? "已装" : "无")
                    + " | 上轮能源直供：" + matrix.lastPushedEnergy()
                    + " | 芯片间搬运：" + matrix.lastChipTransfer()
                    + "\n加工任务：" + craftLine);
            if (problem == null) {
                return text;
            }
            Component full = text.copy();
            for (Component line : problem) {
                full = full.copy().append("\n").append(line);
            }
            return full;
        }, false);
        return 1;
    }

    /** 附近最近的微缩矩阵控制器（命令联调用；半径不宜过大，避免遍历成本） */
    private static AkaishiMiniMatrixTerminalBlockEntity findNearbyMatrix(ServerLevel level, BlockPos center, int radius) {
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (level.getBlockEntity(p) instanceof AkaishiMiniMatrixTerminalBlockEntity matrix) {
                return matrix;
            }
        }
        return null;
    }

    /** 在玩家前方生成一台成型物品终端（箱体最小角 = 前方 4 格处） */
    private static int buildItemTerminal(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        Direction facing = player.getDirection();
        BlockPos min = player.blockPosition().relative(facing, 4).offset(-2, 0, -2);
        BlockPos max = min.offset(4, 4, 4);

        Block shell = AkaishiItemTerminalBlocks.CHISHI_ITEM_TERMINAL_SHELL.get();
        Block core = AkaishiItemTerminalBlocks.CHISHI_ITEM_TERMINAL_CORE.get();
        Block terminal = AkaishiItemTerminalBlocks.CHISHI_ITEM_TERMINAL.get();
        Block storage = AkaishiItemTerminalBlocks.CHISHI_ITEM_STORAGE_UNIT_BASIC.get();
        Block energyPort = AkaishiItemTerminalBlocks.CHISHI_ITEM_TERMINAL_ENERGY_INPUT.get();

        // 0) 清场前先拦一道：范围内已有「已装载的微缩终端」时拒绝生成 ——
        //    否则清场会把它连数据一起抹掉（它自己就是一份终端数据，不能当空气清掉）
        BlockPos occupied = loadedMiniature(level, min, max);
        if (occupied != null) {
            ctx.getSource().sendFailure(Component.literal(
                    "前方范围内已有一枚已装载的微缩终端（" + pos(occupied) + "），请先移走或换个方向再执行"));
            return 0;
        }
        // 1) 清场：箱体 + 外圈 1 格（避免残留方块导致墙面判定失败）
        for (BlockPos p : BlockPos.betweenClosed(min.offset(-1, -1, -1), max.offset(1, 1, 1))) {
            level.setBlock(p.immutable(), Blocks.AIR.defaultBlockState(), 2);
        }
        // 2) 墙面（厚度 1）全部铺外壳；内腔留空
        for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
            BlockPos p = cursor.immutable();
            if (isOnWall(p, min, max)) {
                level.setBlock(p, shell.defaultBlockState(), 2);
            }
        }
        // 3) 内腔中心放本体内核（校验要求内腔恰 1 个核心）
        level.setBlock(min.offset(2, 2, 2), core.defaultBlockState(), 2);
        // 4) 主方块落在「靠近玩家那一面墙」的正中间：玩家一眼就能看到并右键到，
        //    放到背面/侧面容易点了外壳（外壳没有交互，表现为"右键毫无反应"）
        BlockPos terminalPos = nearFaceCenter(facing, min, max);
        level.setBlock(terminalPos, terminal.defaultBlockState(), 2);
        // 5) 贴装件：墙内嵌 1 个（随结构消耗）+ 外侧 1 格再贴 1 个（检验防复制不变量）
        //    嵌的那个放在**顶面**中心：侧面的中心位可能已被主方块占用（朝东时两者曾算出同一坐标，
        //    结果储存单元把主方块覆盖掉 → 结构不成型、owner 也没写上，表现为"右键毫无反应"）
        BlockPos embedded = new BlockPos(min.getX() + 2, max.getY(), min.getZ() + 2);
        level.setBlock(embedded, storage.defaultBlockState(), 2);
        BlockPos outside = new BlockPos(min.getX() - 1, min.getY() + 2, min.getZ() + 2);
        level.setBlock(outside, storage.defaultBlockState(), 2);
        // 6) 赤能源接入口贴在另一侧外侧，并给终端缓冲预充电
        level.setBlock(new BlockPos(max.getX() + 1, min.getY() + 2, min.getZ() + 2),
                energyPort.defaultBlockState(), 2);
        // 7) 自检：生成结果必须真的通过成型校验，否则明确报错（不要谎报"已生成成型终端"）
        if (ItemTerminalStructure.scan(level, terminalPos) == null) {
            ctx.getSource().sendFailure(Component.literal(
                    "结构自检未通过：生成结果不成型（方块位置 " + pos(terminalPos) + "），请反馈此坐标"));
            return 0;
        }
        if (level.getBlockEntity(terminalPos) instanceof AkaishiItemTerminalBlockEntity be) {
            be.setOwner(player.getUUID(), player.getGameProfile().getName());
            be.getEnergyStorage().addEnergy(PRELOAD_ENERGY, false);
        }

        ctx.getSource().sendSuccess(() -> Component.literal(
                "已生成成型物品终端：本体 " + pos(terminalPos) + "，箱体 " + pos(min) + " → " + pos(max)
                        + "；手持一枚【空的】微缩终端潜行右键本体即可装载（墙内与外圈贴装件会被一并消耗）"), false);
        return 1;
    }

    /**
     * 在玩家前方生成一套成型的<b>微缩矩阵</b>（5×5×5 空腔箱体）。
     * <p>
     * 墙面铺外壳、内腔留空（矩阵要求内腔全为空气）；控制器落玩家侧墙面正中，
     * 其左右各留 1 格结构玻璃做观察窗。生成后用<b>与方块实体同一套墙块白名单</b>自检，
     * 不通过则明确报错，不谎报成功。
     */
    private static int buildMiniMatrix(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        Direction facing = player.getDirection();
        BlockPos min = player.blockPosition().relative(facing, 4).offset(-2, 0, -2);
        BlockPos max = min.offset(4, 4, 4);

        Block casing = AkaishiMiniMatrixBlocks.CHISHI_MINI_MATRIX_CASING.get();
        Block glass = AkaishiMiniMatrixBlocks.CHISHI_MINI_MATRIX_STRUCTURE_GLASS.get();
        Block terminal = AkaishiMiniMatrixBlocks.CHISHI_MINI_MATRIX_TERMINAL.get();

        BlockPos occupied = loadedMiniature(level, min, max);
        if (occupied != null) {
            ctx.getSource().sendFailure(Component.literal(
                    "前方范围内已有一枚已装载的微缩终端（" + pos(occupied) + "），请先移走或换个方向再执行"));
            return 0;
        }
        // 1) 清场：箱体 + 外圈 1 格（避免残留方块导致墙面判定失败）
        for (BlockPos p : BlockPos.betweenClosed(min.offset(-1, -1, -1), max.offset(1, 1, 1))) {
            level.setBlock(p.immutable(), Blocks.AIR.defaultBlockState(), 2);
        }
        // 2) 墙面全部铺外壳（内腔不填：矩阵要求内腔全为空气）
        for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
            BlockPos p = cursor.immutable();
            if (isOnWall(p, min, max)) {
                level.setBlock(p, casing.defaultBlockState(), 2);
            }
        }
        // 3) 控制器落玩家侧墙面正中，其左右各留 1 格结构玻璃（同面墙面格，仍算有效墙块）
        BlockPos terminalPos = nearFaceCenter(facing, min, max);
        level.setBlock(terminalPos, terminal.defaultBlockState(), 2);
        boolean alongX = facing.getAxis() == Direction.Axis.Z;
        level.setBlock(alongX ? terminalPos.offset(1, 0, 0) : terminalPos.offset(0, 0, 1),
                glass.defaultBlockState(), 2);
        level.setBlock(alongX ? terminalPos.offset(-1, 0, 0) : terminalPos.offset(0, 0, -1),
                glass.defaultBlockState(), 2);
        // 4) 自检：必须真成型才报成功
        if (MatrixStructure.scan(level, terminalPos, AkaishiMiniMatrixTerminalBlockEntity.SIZE,
                AkaishiMiniMatrixTerminalBlockEntity::isWallBlock) == null) {
            ctx.getSource().sendFailure(Component.literal(
                    "结构自检未通过：生成结果不成型（控制器位置 " + pos(terminalPos) + "），请反馈此坐标"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("已生成成型微缩矩阵：控制器 " + pos(terminalPos)
                + "，箱体 " + pos(min) + " → " + pos(max)
                + "；右键控制器可看芯片盘点，把已装载的微缩终端放到墙面上即会被识别"), false);
        return 1;
    }

    /** 范围（外扩 1 格）内是否存在「已装载的微缩终端」，返回其坐标（无则 null） */
    private static BlockPos loadedMiniature(ServerLevel level, BlockPos min, BlockPos max) {
        for (BlockPos p : BlockPos.betweenClosed(min.offset(-1, -1, -1), max.offset(1, 1, 1))) {
            if (level.getBlockEntity(p) instanceof MiniatureTerminalBlockEntity chip && chip.isLoadedTerminal()) {
                return p.immutable();
            }
        }
        return null;
    }

    /** 靠近玩家那一面墙的正中间（主方块落点：玩家一眼能看到并右键到） */
    private static BlockPos nearFaceCenter(Direction facing, BlockPos min, BlockPos max) {
        return switch (facing) {
            case NORTH -> new BlockPos(min.getX() + 2, min.getY() + 2, max.getZ());
            case SOUTH -> new BlockPos(min.getX() + 2, min.getY() + 2, min.getZ());
            case WEST -> new BlockPos(max.getX(), min.getY() + 2, min.getZ() + 2);
            default -> new BlockPos(min.getX(), min.getY() + 2, min.getZ() + 2);
        };
    }

    /** 是否落在箱体表面（壁厚 1） */
    private static boolean isOnWall(BlockPos p, BlockPos min, BlockPos max) {
        return p.getX() == min.getX() || p.getX() == max.getX()
                || p.getY() == min.getY() || p.getY() == max.getY()
                || p.getZ() == min.getZ() || p.getZ() == max.getZ();
    }

    private static String pos(BlockPos p) {
        return p.getX() + "," + p.getY() + "," + p.getZ();
    }
}
