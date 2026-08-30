package com.example.template.block.entity;

import com.example.template.api.fluid.IExternalFluidAccess;
import com.example.template.api.fluid.IFluidPipeDevice;
import com.example.template.block.ChishiFluidPipeBlock;
import com.example.template.fluid.FluidTank;
import dev.architectury.fluid.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 液体管道方块实体：液体网络传输核心。
 * 每 tick 由网络"代表节点"（网络中坐标最小的管道）执行 BFS 收集全部连通管道与
 * 相邻液体罐（模组设备 {@link IFluidPipeDevice} 或外部液体能力），把液体从源罐
 * 送到汇罐。每段管道内置缓冲罐：网络内没有设备源/汇时承接外部管道（MEK）注入。
 * 支持配置器方向模式与单侧断开。
 */
public class ChishiFluidPipeBlockEntity extends BlockEntity implements ChishiPipeControl {

    /** 方向模式：正常 / 推（只作汇）/ 拉（只作源） */
    public static final int MODE_NORMAL = 0;
    public static final int MODE_PUSH = 1;
    public static final int MODE_PULL = 2;

    /** 网络规模上限：防止超大网络 BFS 遍历过多节点拖慢主线程（超出则截断，远端设备可能无法接入） */
    private static final int MAX_NETWORK = 1024;

    /** 本段管道缓冲罐容量（承接外部注入的落点） */
    public static final long BUFFER_CAPACITY = 8000;

    private final FluidTank buffer;
    private int mode = MODE_NORMAL;
    /** 被配置器断开的连接面（bit 0-5 对应 Direction.ordinal()） */
    private int disconnectedMask;

    // ===== 网络拓扑缓存：仅在结构变化时重扫，避免每 tick 全网络 BFS（与能量管道一致）=====
    /** 网络结构是否可能已变化（放置/拆除/断开时置位，经邻居管道逐 tick 传播至代表） */
    private boolean networkDirty = true;
    /** 代表节点缓存的本网络管道列表（BFS 结果）；null 表示尚无缓存 */
    private List<BlockPos> cachedPipes;

    public ChishiFluidPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.buffer = new FluidTank(BUFFER_CAPACITY) {
            @Override
            protected void onChanged() {
                setChanged();
            }
        };
    }

    public ChishiFluidPipeBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CHISHI_FLUID_PIPE.get(), pos, state);
    }

    public FluidTank buffer() {
        return buffer;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiFluidPipeBlockEntity be) {
        be.tickServer();
    }

    @Override
    public int getMode() {
        return mode;
    }

    @Override
    public void setMode(int mode) {
        if (mode >= MODE_NORMAL && mode <= MODE_PULL) {
            this.mode = mode;
            setChanged();
        }
    }

    @Override
    public boolean isDisconnected(Direction dir) {
        return (disconnectedMask & (1 << dir.ordinal())) != 0;
    }

    @Override
    public boolean toggleDisconnected(Direction dir) {
        disconnectedMask ^= (1 << dir.ordinal());
        networkDirty = true; // 连接拓扑变化 → 缓存失效
        setChanged();
        return isDisconnected(dir);
    }

    /** 网络拓扑可能已变化，通知本管道缓存失效（方块放置/拆除/断开连接时调用） */
    public void markDirty() {
        networkDirty = true;
    }

    private void tickServer() {
        // 快速裁剪：存在坐标更小的相邻液体管道时，本节点非网络代表，交由代表统一传输
        if (!isNetworkRepresentative()) {
            // 非代表：若自身缓存标记脏，把标记传播给相邻管道（逐 tick 泛洪至代表），并清除自身标记
            if (networkDirty) {
                networkDirty = false;
                for (Direction dir : Direction.values()) {
                    if (isDisconnected(dir)) {
                        continue;
                    }
                    BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
                    if (neighbor instanceof ChishiFluidPipeBlockEntity np) {
                        np.markDirty();
                    }
                }
            }
            return;
        }
        // 代表：无缓存时先做轻量邻居检查，孤立管道无需处理
        if (cachedPipes == null && !hasNetworkNeighbor()) {
            return;
        }
        // 拓扑变化或无缓存 → 重扫网络并刷新缓存
        if (networkDirty || cachedPipes == null) {
            refreshNetwork();
        }
        if (cachedPipes.isEmpty()) {
            return;
        }
        transferNetwork();
    }

    /** 是否本网络代表：不存在坐标更小的相邻管道（局部最小唯一，等于网络坐标最小节点） */
    private boolean isNetworkRepresentative() {
        for (Direction dir : Direction.values()) {
            if (isDisconnected(dir)) {
                continue;
            }
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor instanceof ChishiFluidPipeBlockEntity
                    && neighbor.getBlockPos().compareTo(worldPosition) < 0) {
                return false;
            }
        }
        return true;
    }

    /** 是否存在相邻的液体管道或可接入液体罐 */
    private boolean hasNetworkNeighbor() {
        for (Direction dir : Direction.values()) {
            if (isDisconnected(dir)) {
                continue;
            }
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor instanceof ChishiFluidPipeBlockEntity) {
                return true;
            }
            if (hasFluidTank(neighbor, dir)) {
                return true;
            }
        }
        return false;
    }

    /** 沿管道 BFS 收集全部连通管道并刷新缓存（仅代表在拓扑变化时调用）；同时清除网络内所有管道的脏标记 */
    private void refreshNetwork() {
        List<BlockPos> pipes = new ArrayList<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visitedPipes = new HashSet<>();
        queue.add(worldPosition);
        visitedPipes.add(worldPosition);
        while (!queue.isEmpty() && pipes.size() < MAX_NETWORK) {
            BlockPos cur = queue.poll();
            pipes.add(cur);
            ChishiFluidPipeBlockEntity curPipe = level.getBlockEntity(cur) instanceof ChishiFluidPipeBlockEntity p ? p : null;
            if (curPipe != null) {
                curPipe.networkDirty = false; // 缓存已刷新，清除脏标记
            }
            for (Direction dir : Direction.values()) {
                if (curPipe != null && curPipe.isDisconnected(dir)) {
                    continue;
                }
                BlockPos next = cur.relative(dir);
                if (!visitedPipes.add(next)) {
                    continue;
                }
                if (level.getBlockEntity(next) instanceof ChishiFluidPipeBlockEntity) {
                    queue.add(next);
                }
            }
        }
        this.cachedPipes = pipes;
        this.networkDirty = false;
    }

    /** 基于缓存的网络成员执行一次液体传输（代表每 tick 调用，正常 tick 零 BFS） */
    private void transferNetwork() {
        List<BlockPos> pipes = cachedPipes;

        // 收集源与汇罐。推模式：相连设备只作汇；拉模式：只作源；正常模式双向。
        List<TankHandle> sources = new ArrayList<>();
        List<TankHandle> sinks = new ArrayList<>();
        Set<BlockPos> visitedDevices = new HashSet<>();
        for (BlockPos pipe : pipes) {
            ChishiFluidPipeBlockEntity pb = level.getBlockEntity(pipe) instanceof ChishiFluidPipeBlockEntity p ? p : null;
            int pipeMode = pb != null ? pb.getMode() : MODE_NORMAL;
            for (Direction dir : Direction.values()) {
                if (pb != null && pb.isDisconnected(dir)) {
                    continue;
                }
                BlockPos nb = pipe.relative(dir);
                if (!visitedDevices.add(nb)) {
                    continue;
                }
                List<TankHandle> tanks = collectTanks(nb, dir);
                for (TankHandle tank : tanks) {
                    boolean asSource = switch (pipeMode) {
                        case MODE_PUSH -> false;
                        case MODE_PULL -> true;
                        default -> tank.canExtract();
                    };
                    boolean asSink = switch (pipeMode) {
                        case MODE_PUSH -> true;
                        case MODE_PULL -> false;
                        default -> tank.canInsert();
                    };
                    if (asSource && tank.getAmount() > 0) {
                        sources.add(tank);
                    }
                    if (asSink && tank.getAmount() < tank.getCapacity()) {
                        sinks.add(tank);
                    }
                }
            }
        }
        // 网络内无设备源/汇时，管道缓冲罐作为兜底源/汇（承接 MEK 外部注入）
        if (sources.isEmpty() && !buffer.isEmpty()) {
            sources.add(TankHandle.of(buffer));
        }
        if (sinks.isEmpty() && !buffer.isFull()) {
            sinks.add(TankHandle.of(buffer));
        }
        if (sources.isEmpty() || sinks.isEmpty()) {
            return;
        }

        // 网络每 tick 总传输上限 = 管道段数 × 单段速率
        long networkRate = pipes.size() * ChishiFluidPipeBlock.getTransferRate();
        long movedTotal = 0;
        // 需求驱动：总注入量不超过所有汇的空缺
        long demand = 0;
        for (TankHandle sink : sinks) {
            demand += Math.max(0, sink.getCapacity() - sink.getAmount());
        }
        if (demand <= 0) {
            return;
        }
        long quota = Math.min(networkRate, demand);
        for (TankHandle source : sources) {
            if (quota <= 0) {
                break;
            }
            long take = Math.min(Math.min(quota, source.getAmount()), ChishiFluidPipeBlock.getTransferRate());
            if (take <= 0) {
                continue;
            }
            FluidStack taken = source.drain(take);
            for (TankHandle sink : sinks) {
                if (taken.isEmpty()) {
                    break;
                }
                long moved = sink.fill(taken);
                if (moved > 0) {
                    taken.shrink(moved);
                    quota -= moved;
                    movedTotal += moved;
                }
            }
            // 未全部注入的部分放回源罐，避免液体凭空消失
            if (!taken.isEmpty()) {
                source.fill(taken);
            }
        }
        if (movedTotal > 0) {
            setChanged();
        }
    }

    /** 收集邻居方块暴露的全部液体罐（模组设备或外部能力），并标记其输入/输出权限 */
    private List<TankHandle> collectTanks(BlockPos pos, Direction side) {
        List<TankHandle> result = new ArrayList<>();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IFluidPipeDevice device) {
            for (FluidTank tank : device.getFluidTanks()) {
                result.add(TankHandle.of(tank, device.canPipeExtract(tank), device.canPipeInsert(tank)));
            }
            return result;
        }
        if (IExternalFluidAccess.FluidBridge.INSTANCE != null) {
            IExternalFluidAccess.ExternalFluidTank tank = IExternalFluidAccess.FluidBridge.INSTANCE.getTank(level, pos, side);
            if (tank != null) {
                result.add(TankHandle.of(tank));
            }
        }
        return result;
    }

    /** 邻居方块是否存在可对接的液体罐（模组设备或外部液体能力） */
    private boolean hasFluidTank(BlockEntity be, Direction side) {
        if (be instanceof IFluidPipeDevice device) {
            return !device.getFluidTanks().isEmpty();
        }
        return IExternalFluidAccess.FluidBridge.INSTANCE != null
                && IExternalFluidAccess.FluidBridge.INSTANCE.getTank(level, be.getBlockPos(), side) != null;
    }

    /** 统一罐句柄：屏蔽模组 FluidTank 与外部罐的差异 */
    private interface TankHandle {
        long getAmount();

        long getCapacity();

        boolean canExtract();

        boolean canInsert();

        /** 抽取指定量，返回实际抽出的液体 */
        FluidStack drain(long amount);

        /** 注入液体，返回实际注入量 */
        long fill(FluidStack stack);

        static TankHandle of(FluidTank tank) {
            return of(tank, true, true);
        }

        static TankHandle of(FluidTank tank, boolean extract, boolean insert) {
            return new TankHandle() {
                @Override
                public long getAmount() {
                    return tank.getAmount();
                }

                @Override
                public long getCapacity() {
                    return tank.getCapacity();
                }

                @Override
                public boolean canExtract() {
                    return extract;
                }

                @Override
                public boolean canInsert() {
                    return insert;
                }

                @Override
                public FluidStack drain(long amount) {
                    return tank.drain(amount, false);
                }

                @Override
                public long fill(FluidStack stack) {
                    return tank.fill(stack, false);
                }
            };
        }

        static TankHandle of(IExternalFluidAccess.ExternalFluidTank tank) {
            return new TankHandle() {
                @Override
                public long getAmount() {
                    return tank.getAmount();
                }

                @Override
                public long getCapacity() {
                    return tank.getCapacity();
                }

                @Override
                public boolean canExtract() {
                    return true;
                }

                @Override
                public boolean canInsert() {
                    return true;
                }

                @Override
                public FluidStack drain(long amount) {
                    return tank.drain(amount, false);
                }

                @Override
                public long fill(FluidStack stack) {
                    return tank.fill(stack, false);
                }
            };
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Buffer", buffer.writeToNbt());
        tag.putInt("Mode", mode);
        tag.putInt("Disconnected", disconnectedMask);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        buffer.readFromNbt(tag.getCompound("Buffer"));
        mode = tag.getInt("Mode");
        if (mode < MODE_NORMAL || mode > MODE_PULL) {
            mode = MODE_NORMAL;
        }
        disconnectedMask = tag.getInt("Disconnected");
    }
}
