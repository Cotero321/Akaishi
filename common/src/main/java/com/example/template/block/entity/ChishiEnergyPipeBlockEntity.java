package com.example.template.block.entity;

import com.example.template.api.energy.IEnergyProvider;
import com.example.template.api.energy.IEnergyStorage;
import com.example.template.api.energy.IEnergyType;
import com.example.template.block.ChishiEnergyPipeBlock;
import com.example.template.energy.ChishiEnergyType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * 赤能源管道方块实体：能量网络传输核心。
 * 每 tick 由网络"代表节点"（网络中坐标最小的管道）执行 BFS 收集全部连通管道与
 * 相邻设备（实现 IEnergyProvider），再从可输出节点抽能注入可接收节点。
 * 管道可被配置器设置为方向模式：正常 / 推 / 拉，并可断开单侧连接以精细控制流向。
 * 传输按能量类型隔离：BFS 只连通同类型管道，设备按 {@link IEnergyProvider#getEnergyStorage(IEnergyType)} 按类型匹配。
 */
public class ChishiEnergyPipeBlockEntity extends BlockEntity implements ChishiPipeControl {

    /** 方向模式：正常（默认，按设备能力双向判定） */
    public static final int MODE_NORMAL = 0;
    /** 方向模式：推（相连设备只作能量汇，管道主动向设备推能） */
    public static final int MODE_PUSH = 1;
    /** 方向模式：拉（相连设备只作能量源，管道主动从设备拉能） */
    public static final int MODE_PULL = 2;

    /** 网络规模上限：防止超大网络 BFS 遍历过多节点拖慢主线程（超出则截断，远端设备可能无法接入） */
    private static final int MAX_NETWORK = 1024;

    /** 本段管道方向模式，默认正常 */
    private int mode = MODE_NORMAL;

    /** 被配置器断开的连接面（bit 0-5 对应 Direction.ordinal()），断开后不参与连接与传输 */
    private int disconnectedMask;

    // ===== 网络拓扑缓存：仅在结构变化时重扫，避免每 tick 全网络 BFS =====
    /** 网络结构是否可能已变化（放置/拆除/断开管道时置位，经邻居管道逐 tick 传播至代表） */
    private boolean networkDirty = true;
    /** 代表节点缓存的本网络管道列表（BFS 结果）；null 表示尚无缓存 */
    private List<BlockPos> cachedPipes;
    /** 代表节点缓存的本网络总传输速率 */
    private long cachedNetworkRate;

    public ChishiEnergyPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ChishiEnergyPipeBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CHISHI_ENERGY_PIPE.get(), pos, state);
    }

    /** 本段管道传输的能量类型（子类可覆盖，如生命能量管道） */
    public IEnergyType getEnergyType() {
        return ChishiEnergyType.INSTANCE;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiEnergyPipeBlockEntity be) {
        be.tickServer();
    }

    public int getMode() {
        return mode;
    }

    /** 切换方向模式（正常→推→拉循环）并标记保存 */
    public void setMode(int mode) {
        if (mode >= MODE_NORMAL && mode <= MODE_PULL) {
            this.mode = mode;
            setChanged();
        }
    }

    /** 该方向是否被配置器断开连接 */
    public boolean isDisconnected(Direction dir) {
        return (disconnectedMask & (1 << dir.ordinal())) != 0;
    }

    /** 网络拓扑可能已变化，通知本管道缓存失效（方块放置/拆除/断开连接时调用） */
    public void markDirty() {
        networkDirty = true;
    }

    /** 切换某方向的连接（断开↔恢复），返回切换后是否处于断开状态 */
    public boolean toggleDisconnected(Direction dir) {
        disconnectedMask ^= (1 << dir.ordinal());
        networkDirty = true; // 连接拓扑变化 → 缓存失效
        setChanged();
        return isDisconnected(dir);
    }

    private void tickServer() {
        IEnergyType pipeType = getEnergyType();
        // 快速裁剪：若存在坐标更小的同类型相邻管道，则本节点非网络代表，交由代表统一传输
        if (!isNetworkRepresentative(pipeType)) {
            // 非代表：若自身缓存标记脏，把标记传播给相邻管道（逐 tick 泛洪至代表），并清除自身标记
            if (networkDirty) {
                networkDirty = false;
                for (Direction dir : Direction.values()) {
                    if (isDisconnected(dir)) {
                        continue;
                    }
                    BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
                    if (neighbor instanceof ChishiEnergyPipeBlockEntity np && np.getEnergyType() == pipeType) {
                        np.markDirty();
                    }
                }
            }
            return;
        }
        // 代表：无缓存时先做轻量邻居检查，孤立管道（无管道/设备邻居）无需处理
        if (cachedPipes == null && !hasNetworkNeighbor(pipeType)) {
            return;
        }
        // 拓扑变化或无缓存 → 重扫网络并刷新缓存
        if (networkDirty || cachedPipes == null) {
            refreshNetwork(pipeType);
        }
        if (cachedPipes.isEmpty()) {
            return;
        }
        transferNetwork(pipeType);
    }

    /** 是否本网络代表：不存在坐标更小的同类型相邻管道（局部最小唯一，等于网络坐标最小节点） */
    private boolean isNetworkRepresentative(IEnergyType pipeType) {
        for (Direction dir : Direction.values()) {
            if (isDisconnected(dir)) {
                continue;
            }
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor instanceof ChishiEnergyPipeBlockEntity np
                    && np.getEnergyType() == pipeType && neighbor.getBlockPos().compareTo(worldPosition) < 0) {
                return false;
            }
        }
        return true;
    }

    /** 是否存在相邻的同类管道或可接入设备 */
    private boolean hasNetworkNeighbor(IEnergyType pipeType) {
        for (Direction dir : Direction.values()) {
            if (isDisconnected(dir)) {
                continue;
            }
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor instanceof ChishiEnergyPipeBlockEntity np && np.getEnergyType() == pipeType) {
                return true;
            }
            if (neighbor instanceof IEnergyProvider provider && provider.getEnergyStorage(pipeType) != null) {
                return true;
            }
        }
        return false;
    }

    /** 沿管道 BFS 收集全部连通管道并刷新缓存（仅代表在拓扑变化时调用）；同时清除网络内所有管道的脏标记 */
    private void refreshNetwork(IEnergyType pipeType) {
        List<BlockPos> pipes = new ArrayList<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visitedPipes = new HashSet<>();
        queue.add(worldPosition);
        visitedPipes.add(worldPosition);
        while (!queue.isEmpty() && pipes.size() < MAX_NETWORK) {
            BlockPos cur = queue.poll();
            pipes.add(cur);
            ChishiEnergyPipeBlockEntity curPipe = level.getBlockEntity(cur) instanceof ChishiEnergyPipeBlockEntity p ? p : null;
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
                BlockEntity nb = level.getBlockEntity(next);
                // 只连通同能量类型的管道（赤管道与生命管道物理相邻也不混网）
                if (nb instanceof ChishiEnergyPipeBlockEntity np && np.getEnergyType() == pipeType) {
                    queue.add(next);
                }
            }
        }
        // 缓存网络总传输速率：用 long 累加（终极管道 5000 万/段，1024 段上限可达 512 亿，int 会溢出为负）
        long rate = 0;
        for (BlockPos pipe : pipes) {
            if (level.getBlockEntity(pipe) instanceof ChishiEnergyPipeBlockEntity pb
                    && level.getBlockState(pipe).getBlock() instanceof ChishiEnergyPipeBlock pbBlock) {
                rate += pbBlock.getTransferRate();
            }
        }
        this.cachedPipes = pipes;
        this.cachedNetworkRate = rate;
        this.networkDirty = false;
    }

    /** 基于缓存的网络成员执行一次能量传输（代表每 tick 调用） */
    private void transferNetwork(IEnergyType pipeType) {
        // 使用代表缓存：正常 tick 零 BFS；缓存过期（拓扑变化后）由 tickServer 先调用 refreshNetwork 再进入
        List<BlockPos> pipes = cachedPipes;

        // 收集源与汇（独立收集：储存单元这类双向缓冲可同时充当源和汇）。
        // 推模式：相连设备只作汇；拉模式：相连设备只作源；正常模式：按设备能力双向判定。
        // 纯源（不可输入，如发生机）与双向缓冲分开记录，传输时优先抽取纯源，避免先抽干缓冲。
        List<IEnergyStorage> pureSources = new ArrayList<>();
        List<IEnergyStorage> bufferSources = new ArrayList<>();
        List<IEnergyStorage> sinks = new ArrayList<>();
        Set<BlockPos> visitedDevices = new HashSet<>();
        for (BlockPos pipe : pipes) {
            ChishiEnergyPipeBlockEntity pb = level.getBlockEntity(pipe) instanceof ChishiEnergyPipeBlockEntity p ? p : null;
            int pipeMode = pb != null ? pb.getMode() : MODE_NORMAL;
            for (Direction dir : Direction.values()) {
                if (pb != null && pb.isDisconnected(dir)) {
                    continue;
                }
                BlockPos nb = pipe.relative(dir);
                if (!visitedDevices.add(nb)) {
                    continue;
                }
                BlockEntity be = level.getBlockEntity(nb);
                if (!(be instanceof IEnergyProvider provider)) {
                    continue;
                }
                // 按管道类型取设备对应存储；设备不持有该类型时返回 null，自然跳过
                IEnergyStorage storage = provider.getEnergyStorage(pipeType);
                if (storage == null) {
                    continue;
                }
                boolean asSource = switch (pipeMode) {
                    case MODE_PUSH -> false;   // 推：只作汇，不作源
                    case MODE_PULL -> true;    // 拉：强制作源
                    default -> provider.canOutputEnergy(pipeType);
                };
                boolean asSink = switch (pipeMode) {
                    case MODE_PUSH -> true;    // 推：强制作汇
                    case MODE_PULL -> false;   // 拉：只作源，不作汇
                    default -> provider.canInputEnergy(pipeType);
                };
                if (asSource && storage.getEnergyStored() > 0) {
                    // 可输入的设备是双向缓冲（如储存单元），后抽；纯源优先
                    if (provider.canInputEnergy(pipeType)) {
                        bufferSources.add(storage);
                    } else {
                        pureSources.add(storage);
                    }
                }
                if (asSink && storage.getEnergyStored() < storage.getMaxEnergy()) {
                    sinks.add(storage);
                }
            }
        }
        if ((pureSources.isEmpty() && bufferSources.isEmpty()) || sinks.isEmpty()) {
            return;
        }

        // 网络每 tick 总传输上限 = 缓存的全网络管道传输速率之和（等级越高、管道越多，输送越快）
        long networkRate = cachedNetworkRate;
        if (networkRate <= 0) {
            return;
        }

        // 需求驱动：抽取量不超过所有汇的总空缺，避免网络需求小于管道速率时过量抽取缓冲
        long totalDemand = 0;
        for (IEnergyStorage sink : sinks) {
            totalDemand += Math.max(0, sink.getMaxEnergy() - sink.getEnergyStored());
        }
        if (totalDemand <= 0) {
            return;
        }

        // Mekanism 式缓冲中转：先把能量从源抽入网络缓冲，再统一推给汇。
        // 避免"源→汇"直连时双向缓冲（储存单元）既被抽又被灌造成的回流与能量搬运。
        long buffer = 0;
        long toExtract = Math.min(networkRate, totalDemand);
        for (IEnergyStorage source : pureSources) {
            if (toExtract <= 0) {
                break;
            }
            long got = source.extractEnergy(toExtract, false);
            buffer += got;
            toExtract -= got;
        }
        for (IEnergyStorage source : bufferSources) {
            if (toExtract <= 0) {
                break;
            }
            long got = source.extractEnergy(toExtract, false);
            buffer += got;
            toExtract -= got;
        }
        if (buffer <= 0) {
            return;
        }
        long remaining = buffer;
        for (IEnergyStorage sink : sinks) {
            if (remaining <= 0) {
                break;
            }
            long need = sink.getMaxEnergy() - sink.getEnergyStored();
            if (need <= 0) {
                continue;
            }
            remaining -= sink.addEnergy(Math.min(remaining, need), false);
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Mode", mode);
        tag.putInt("Disconnected", disconnectedMask);
    }

    @Override
    public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
        mode = tag.getInt("Mode");
        if (mode < MODE_NORMAL || mode > MODE_PULL) {
            mode = MODE_NORMAL;
        }
        disconnectedMask = tag.getInt("Disconnected");
    }
}
