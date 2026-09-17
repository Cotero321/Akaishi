package com.example.akaishi.item;

import com.example.akaishi.block.AkaishiEnergyPipeBlock;
import com.example.akaishi.block.AkaishiFluidPipeBlock;
import com.example.akaishi.block.AkaishiItemPipeBlock;
import com.example.akaishi.block.entity.AkaishiPipeControl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

/**
 * 赤能源配置器（原调试工具）：参考 Mekanism 配置器。
 * 右键管道（赤能源管道 / 物品管道 / 液体管道）只旋转「被点击那一面」的方向类型，
 * 在"正常 / 输出 / 输入"间循环；潜行（shift）+ 右键则断开或恢复被点击那一侧的连接。
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
            Direction face = resolveFace(level, pos, ctx);
            // 潜行 + 右键：断开/恢复被点击那一侧的连接
            if (player != null && player.isShiftKeyDown()) {
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
                // 断开位会影响邻接管道同侧管臂的显示；若本方块状态未变则原版不会通知邻居，故显式唤醒邻居重算
                level.updateNeighborsAt(pos, state.getBlock());
                syncToClients(level, pos, be, state);
                player.displayClientMessage(Component.translatable(
                        disconnected ? "message.akaishi.pipe.disconnected"
                                : "message.akaishi.pipe.connected"), true);
                return InteractionResult.sidedSuccess(false);
            }
            // 右键：只旋转被点击那一面 正常 → 输出 → 输入
            BlockState before = level.getBlockState(pos);
            int next = (pipe.getSideMode(face) + 1) % 3;
            pipe.setSideMode(face, next);
            // 方向类型存在方块实体里，方块状态未变时原版不会下发方块实体数据，必须手动广播
            syncToClients(level, pos, be, before);
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.akaishi.pipe_mode." + next)
                        .append(Component.literal(" "))
                        .append(Component.translatable("message.akaishi.pipe_side." + face.getName())), true);
            }
            return InteractionResult.sidedSuccess(false);
        }
        return InteractionResult.PASS;
    }

    /**
     * 管臂横向半宽（6px 管臂 = 3/16），与 PipeShapes 保持一致。
     * 注意：命中点坐标已减去方块中心 0.5，故「离中心的距离」才是判定基准——
     * 中心核占 ±3/16，管臂自核表面接出，故沿轴内边界同样为 3/16（而非绝对坐标的 5/16）。
     */
    private static final double ARM_HALF = 3.0D / 16.0D;
    /** 判定容差：命中点可能正好落在包围盒表面上 */
    private static final double HIT_EPS = 1.0E-4D;

    /**
     * 判定配置器作用的「面」。
     * 管臂仅 6px 宽，斜视时射线常越过臂身落到中心核上，直接取命中面会出现「点侧面却提示上面」；
     * 且从侧向看某条臂时，命中面是臂的侧向面（如从西侧看北臂得到 WEST）而非臂的轴向，
     * 取命中面会切到错误方向。
     * 故先以命中点与各已连管臂包围盒做包含判定：落在某条臂内就操作该臂方向；
     * 仅命中中心核时才用命中面（此时射线穿过哪一面就正对哪一面，必然正确）。
     */
    private static Direction resolveFace(Level level, BlockPos pos, UseOnContext ctx) {
        Vec3 hit = ctx.getClickLocation();
        Direction arm = armAt(level.getBlockState(pos),
                hit.x - pos.getX() - 0.5D, hit.y - pos.getY() - 0.5D, hit.z - pos.getZ() - 0.5D);
        return arm != null ? arm : ctx.getClickedFace();
    }

    /** 命中点落在哪条已连接管臂内（取沿轴偏移最大者，处理共角歧义），未命中返回 null */
    private static Direction armAt(BlockState state, double dx, double dy, double dz) {
        Direction best = null;
        double bestAlong = Double.NEGATIVE_INFINITY;
        for (Direction dir : Direction.values()) {
            if (!isConnectedOn(state, dir)) {
                continue;
            }
            double along = dx * dir.getStepX() + dy * dir.getStepY() + dz * dir.getStepZ();
            if (along < ARM_HALF - HIT_EPS) {
                continue;
            }
            double a1;
            double a2;
            switch (dir.getAxis()) {
                case X -> {
                    a1 = dy;
                    a2 = dz;
                }
                case Y -> {
                    a1 = dx;
                    a2 = dz;
                }
                default -> {
                    a1 = dx;
                    a2 = dy;
                }
            }
            if (Math.abs(a1) <= ARM_HALF + HIT_EPS && Math.abs(a2) <= ARM_HALF + HIT_EPS && along > bestAlong) {
                best = dir;
                bestAlong = along;
            }
        }
        return best;
    }

    /** 该方向是否已连出管臂（属性名与 Direction#getName 一致） */
    private static boolean isConnectedOn(BlockState state, Direction dir) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof BooleanProperty bool && bool.getName().equals(dir.getName())) {
                return state.getValue(bool);
            }
        }
        return false;
    }

    /**
     * 把方块实体数据（方向类型/断开位）下发给附近玩家，使标识渲染即时生效。
     * 同时以「变更前状态 → 当前状态」广播方块更新：方块实体数据包只刷新 BE 数据，
     * 不会让客户端重渲染，必须补一次方块更新（旧状态需传真实旧状态，传成同一个状态会被当成无变化）。
     */
    private static void syncToClients(Level level, BlockPos pos, BlockEntity be, BlockState oldState) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(be);
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(x, y, z) <= 64.0 * 64.0) {
                player.connection.send(packet);
            }
        }
        serverLevel.sendBlockUpdated(pos, oldState, level.getBlockState(pos), Block.UPDATE_CLIENTS);
    }
}
