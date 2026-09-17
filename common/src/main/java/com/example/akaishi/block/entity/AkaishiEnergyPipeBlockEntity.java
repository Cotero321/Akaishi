package com.example.akaishi.block.entity;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.block.AkaishiEnergyPipeBlock;
import com.example.akaishi.energy.AkaishiEnergyType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
public class AkaishiEnergyPipeBlockEntity extends BlockEntity implements AkaishiPipeControl {

    /** 每面 2 bit 打包的方向模式（bit0-1=DOWN ... bit10-11=EAST），默认全 0 即全正常；
     *  取值见 {@link AkaishiPipeControl#MODE_NORMAL} / {@code MODE_OUTPUT} / {@code MODE_INPUT} */
    private int sideModes;

    /** 网络规模上限：防止超大网络 BFS 遍历过多节点拖慢主线程（超出则截断，远端设备可能无法接入） */
    private static final int MAX_NETWORK = 1024;

    private static final Logger LOGGER = LogManager.getLogger();

    /** 被配置器断开的连接面（bit 0-5 对应 Direction.ordinal()），断开后不参与连接与传输 */
    private int disconnectedMask;

    // ===== 网络拓扑缓存：仅在结构变化时重扫，避免每 tick 全网络 BFS =====
    /** 网络结构是否可能已变化（放置/拆除/断开管道时置位，经邻居管道逐 tick 传播至代表） */
    private boolean networkDirty = true;
    /** 代表节点缓存的本网络管道列表（BFS 结果）；null 表示尚无缓存 */
    private List<BlockPos> cachedPipes;
    /** 代表节点缓存的本网络总传输速率 */
    private long cachedNetworkRate;

    public AkaishiEnergyPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public AkaishiEnergyPipeBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CHISHI_ENERGY_PIPE.get(), pos, state);
    }

    /** 本段管道传输的能量类型（子类可覆盖，如生命能量管道） */
    public IEnergyType getEnergyType() {
        return AkaishiEnergyType.INSTANCE;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiEnergyPipeBlockEntity be) {
        be.tickServer();
    }

    @Override
    public int getSideMode(Direction dir) {
        return (sideModes >> (dir.ordinal() * 2)) & 3;
    }

    /** 设置某面的方向模式（正常/输出/输入），越界回退为正常 */
    @Override
    public void setSideMode(Direction dir, int mode) {
        if (mode < MODE_NORMAL || mode > MODE_INPUT) {
            mode = MODE_NORMAL;
        }
        int shift = dir.ordinal() * 2;
        sideModes = (sideModes & ~(3 << shift)) | (mode << shift);
        setChanged();
    }

    /** 该方向是否被配置器断开连接 */
    @Override
    public boolean isDisconnected(Direction dir) {
        return (disconnectedMask & (1 << dir.ordinal())) != 0;
    }

    /** 网络拓扑可能已变化，通知本管道缓存失效（方块放置/拆除/断开连接时调用） */
    public void markDirty() {
        networkDirty = true;
    }

    /** 切换某方向的连接（断开↔恢复），返回切换后是否处于断开状态 */
    @Override
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
                    if (neighbor instanceof AkaishiEnergyPipeBlockEntity np && np.getEnergyType() == pipeType) {
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
            if (neighbor instanceof AkaishiEnergyPipeBlockEntity np
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
            if (neighbor instanceof AkaishiEnergyPipeBlockEntity np && np.getEnergyType() == pipeType) {
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
            AkaishiEnergyPipeBlockEntity curPipe = level.getBlockEntity(cur) instanceof AkaishiEnergyPipeBlockEntity p ? p : null;
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
                if (nb instanceof AkaishiEnergyPipeBlockEntity np && np.getEnergyType() == pipeType) {
                    queue.add(next);
                }
            }
        }
        // 缓存网络总传输速率：用 long 累加（终极管道 5000 万/段，1024 段上限可达 512 亿，int 会溢出为负）。
        // 网络含无限管道时直接置为 Long.MAX_VALUE 哨兵（不参与求和，避免溢出），传输端据此跳过速率截断
        long rate = 0;
        boolean infinite = false;
        for (BlockPos pipe : pipes) {
            if (level.getBlockEntity(pipe) instanceof AkaishiEnergyPipeBlockEntity pb
                    && level.getBlockState(pipe).getBlock() instanceof AkaishiEnergyPipeBlock pbBlock) {
                if (pbBlock.isInfinite()) {
                    infinite = true;
                    break;
                }
                rate += pbBlock.getTransferRate();
            }
        }
        this.cachedPipes = pipes;
        this.cachedNetworkRate = infinite ? Long.MAX_VALUE : rate;
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
            AkaishiEnergyPipeBlockEntity pb = level.getBlockEntity(pipe) instanceof AkaishiEnergyPipeBlockEntity p ? p : null;
            for (Direction dir : Direction.values()) {
                if (pb != null && pb.isDisconnected(dir)) {
                    continue;
                }
                // 方向类型取自「离开本段朝设备」那一面，逐面独立
                int sideMode = pb != null ? pb.getSideMode(dir) : MODE_NORMAL;
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
                boolean asSource = switch (sideMode) {
                    case MODE_OUTPUT -> false;   // 输出：设备只作汇，不作源
                    case MODE_INPUT -> true;     // 输入：设备强制作源
                    default -> provider.canOutputEnergy(pipeType);
                };
                boolean asSink = switch (sideMode) {
                    case MODE_OUTPUT -> true;    // 输出：设备强制作汇
                    case MODE_INPUT -> false;    // 输入：设备只作源，不作汇
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
        long budget = cachedNetworkRate;
        if (budget <= 0) {
            return;
        }

        // 直连传输：逐汇探测空缺 → 从源真实抽取 → 直接插入汇，全程不经管道自身存储。
        // sinks 已按 BlockPos 去重，故同一目标每 tick 只模拟/处理一次，不存在重复计算。
        // 目标满或拒收一律 continue（不动任何源），保证能量不丢失、不销毁。
        List<Draw> draws = new ArrayList<>();
        for (IEnergyStorage sink : sinks) {
            if (budget <= 0) {
                break;
            }
            long need = sink.getMaxEnergy() - sink.getEnergyStored();
            if (need <= 0) {
                continue; // 目标已满：跳过
            }
            long canAccept = sink.addEnergy(Math.min(budget, need), true); // simulate 探测，不改状态
            if (canAccept <= 0) {
                continue; // 目标拒收：跳过
            }
            draws.clear();
            long got = extractFrom(sink, pureSources, bufferSources, canAccept, draws);
            if (got <= 0) {
                continue; // 所有源都无能量：跳过
            }
            long accepted = sink.addEnergy(got, false); // 真实插入
            long leftover = got - accepted;
            if (leftover > 0) {
                giveBack(draws, leftover); // 目标少收的那部分按明细退回源，杜绝凭空损失
            }
            budget -= accepted;
        }
    }

    /**
     * 依次从纯源、缓冲源真实抽取能量（每次抽取前先 simulate 探测可抽量），总量不超过 want。
     * 抽取明细记入 draws，供目标插入不足时等量退回。
     * 注意：每 tick 都对邻居重新探测，未接入能量的方向不会被缓存，邻居更新后下一 tick 即可重新识别。
     */
    private long extractFrom(IEnergyStorage sink, List<IEnergyStorage> pureSources,
                             List<IEnergyStorage> bufferSources, long want, List<Draw> draws) {
        long remaining = want;
        for (IEnergyStorage source : pureSources) {
            remaining -= draw(sink, source, remaining, draws);
            if (remaining <= 0) {
                return want;
            }
        }
        for (IEnergyStorage source : bufferSources) {
            remaining -= draw(sink, source, remaining, draws);
            if (remaining <= 0) {
                return want;
            }
        }
        return want - remaining;
    }

    /** 从单个源抽取：跳过汇自身（避免自抽自灌的空转搬运），先 simulate 探测再真实抽取 */
    private static long draw(IEnergyStorage sink, IEnergyStorage source, long want, List<Draw> draws) {
        if (want <= 0 || source == sink) {
            return 0;
        }
        long available = source.extractEnergy(want, true);
        if (available <= 0) {
            return 0;
        }
        long got = Math.min(available, source.extractEnergy(available, false));
        if (got <= 0) {
            return 0;
        }
        draws.add(new Draw(source, got));
        return got;
    }

    /** 按抽取明细逆序退回差额：源刚被抽走能量，通常必有空间；退不完说明设备实现异常，需告警 */
    private static void giveBack(List<Draw> draws, long leftover) {
        for (int i = draws.size() - 1; i >= 0 && leftover > 0; i--) {
            leftover -= draws.get(i).giveBack(leftover);
        }
        if (leftover > 0) {
            LOGGER.warn("Pipe energy return failed: {} FE lost at {}", leftover, draws);
        }
    }

    /** 单次抽取明细（源 + 数量），用于目标插入不足时等量退回 */
    private static final class Draw {
        private final IEnergyStorage storage;
        private long amount;

        Draw(IEnergyStorage storage, long amount) {
            this.storage = storage;
            this.amount = amount;
        }

        /** 退回最多 want 点能量，返回实际退回量并从明细中扣减 */
        long giveBack(long want) {
            long done = storage.addEnergy(Math.min(want, amount), false);
            if (done > 0) {
                amount -= done;
            }
            return done;
        }

        @Override
        public String toString() {
            return amount + "FE@" + storage;
        }
    }

    /**
     * 方向类型/断开位只存于方块实体，服务端切换后必须随方块更新下发；
     * 默认实现返回 null / 空 tag，会导致客户端 getSideMode 恒为「正常」、标识渲染不触发。
     */
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("SideModes", sideModes);
        tag.putInt("Disconnected", disconnectedMask);
    }

    @Override
    public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
        sideModes = tag.getInt("SideModes");
        disconnectedMask = tag.getInt("Disconnected");
    }
}
