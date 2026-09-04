package com.example.akaishi.block.entity;

import com.example.akaishi.api.fluid.IExternalFluidAccess;
import com.example.akaishi.api.fluid.IFluidPipeDevice;
import com.example.akaishi.block.AkaishiFluidPipeBlock;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.fluid.FluidTank;
import com.example.akaishi.fluid.ModFluids;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 液体管道方块实体：液体网络传输核心。
 * 每 tick 由网络"代表节点"（网络中坐标最小的管道）执行 BFS 收集全部连通管道与
 * 相邻液体罐（模组设备 {@link IFluidPipeDevice} 或外部液体能力），把液体从源罐
 * 送到汇罐。每段管道内置缓冲罐：网络内没有设备源/汇时承接外部管道（MEK）注入。
 * 支持配置器方向模式与单侧断开。
 */
public class AkaishiFluidPipeBlockEntity extends BlockEntity implements AkaishiPipeControl {

    /** 方向模式：正常 / 推（只作汇）/ 拉（只作源） */
    public static final int MODE_NORMAL = 0;
    public static final int MODE_PUSH = 1;
    public static final int MODE_PULL = 2;

    /** 本段管道缓冲罐容量（承接外部注入的落点），由 {@link ModConfig#fluidPipeBufferCapacity} 提供 */

    private final FluidTank buffer;
    private int mode = MODE_NORMAL;
    /** 被配置器断开的连接面（bit 0-5 对应 Direction.ordinal()） */
    private int disconnectedMask;

    // ===== 网络拓扑缓存：仅在结构变化时重扫，避免每 tick 全网络 BFS（与能量管道一致）=====
    /** 网络结构是否可能已变化（放置/拆除/断开时置位，markDirty 同步传播至代表） */
    private boolean networkDirty = true;
    /** 代表节点缓存的本网络管道列表（BFS 结果）；null 表示尚无缓存 */
    private List<BlockPos> cachedPipes;

    // ===== 传输期临时集合：仅代表节点每 tick 复用，避免频繁 GC =====
    private final Map<BlockPos, DeviceObs> deviceObs = new HashMap<>();
    private final List<TankHandle> sources = new ArrayList<>();
    private final List<TankHandle> sinks = new ArrayList<>();
    private final Set<Object> seenTanks = new HashSet<>();

    public AkaishiFluidPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.buffer = createBuffer();
    }

    public AkaishiFluidPipeBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CHISHI_FLUID_PIPE.get(), pos, state);
    }

    /** 缓冲罐工厂：子类（废料管道）可覆写为废料专用罐/多液体罐。
     *  基类（普通管道）缓冲拒收衰竭燃料，防止外部罐混入废料时被普通管道缓存（绕过泄漏机制） */
    protected FluidTank createBuffer() {
        return new FluidTank(ModConfig.fluidPipeBufferCapacity) {
            @Override
            public long fill(FluidStack resource, boolean simulate) {
                if (resource != null && ModFluids.isExhaustedFuel(resource.getFluid())) {
                    return 0; // 废料仅限废料管道家族运输
                }
                return super.fill(resource, simulate);
            }

            @Override
            protected void onChanged() {
                setChanged();
            }
        };
    }

    /** 是否废料管道家族：废料管道仅与废料管道互连，仅对接废料专用设备 */
    public boolean isWasteFamily() {
        return false;
    }

    /** 是否等离子体管道家族：等离子体管道仅与等离子体管道互连，仅对接等离子体专用罐（第三传输家族） */
    public boolean isPlasmaFamily() {
        return false;
    }

    /** 邻居是否为同家族管道（普通/废料/等离子体三族网络物理隔离） */
    private boolean sameFamilyPipe(BlockEntity be) {
        return be instanceof AkaishiFluidPipeBlockEntity p
                && p.isWasteFamily() == isWasteFamily()
                && p.isPlasmaFamily() == isPlasmaFamily();
    }

    public FluidTank buffer() {
        return buffer;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiFluidPipeBlockEntity be) {
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

    /** 网络拓扑可能已变化：置脏并同步传播至整个网络（放置/拆除/断开时调用，消除逐 tick 泛洪延迟）。
     *  迭代栈防环防递归溢出；仅主线程调用（neighborChanged/右键断开） */
    public void markDirty() {
        Deque<AkaishiFluidPipeBlockEntity> pending = new ArrayDeque<>();
        pending.push(this);
        while (!pending.isEmpty()) {
            AkaishiFluidPipeBlockEntity cur = pending.pop();
            if (cur.networkDirty) {
                continue; // 已标记过，防环
            }
            cur.networkDirty = true;
            for (Direction dir : Direction.values()) {
                if (cur.isDisconnected(dir)) {
                    continue;
                }
                BlockEntity n = cur.level.getBlockEntity(cur.worldPosition.relative(dir));
                if (cur.sameFamilyPipe(n) && n instanceof AkaishiFluidPipeBlockEntity np && !np.networkDirty) {
                    pending.push(np);
                }
            }
        }
    }

    private void tickServer() {
        // 快速裁剪：存在坐标更小的相邻液体管道时，本节点非网络代表，交由代表统一传输。
        // 非代表脏标记已由 markDirty 同步传播至代表，此处无需逐 tick 泛洪
        if (!isNetworkRepresentative()) {
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

    /** 是否本网络代表：不存在坐标更小的相邻同家族管道（局部最小唯一，等于网络坐标最小节点） */
    private boolean isNetworkRepresentative() {
        for (Direction dir : Direction.values()) {
            if (isDisconnected(dir)) {
                continue;
            }
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (sameFamilyPipe(neighbor) && neighbor.getBlockPos().compareTo(worldPosition) < 0) {
                return false;
            }
        }
        return true;
    }

    /** 是否存在相邻的同家族管道或可接入液体罐 */
    private boolean hasNetworkNeighbor() {
        for (Direction dir : Direction.values()) {
            if (isDisconnected(dir)) {
                continue;
            }
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (sameFamilyPipe(neighbor)) {
                return true;
            }
            if (hasFluidTank(neighbor, dir)) {
                return true;
            }
        }
        return false;
    }

    /** 沿管道 BFS 收集全部连通管道并刷新缓存（仅代表在拓扑变化时调用）；同时清除网络内所有管道的脏标记。
     *  不设网络规模上限：blockstate 物理连接必须与传输网络一致，避免"连上了但不走液" */
    private void refreshNetwork() {
        List<BlockPos> pipes = new ArrayList<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visitedPipes = new HashSet<>();
        queue.add(worldPosition);
        visitedPipes.add(worldPosition);
        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            pipes.add(cur);
            AkaishiFluidPipeBlockEntity curPipe = level.getBlockEntity(cur) instanceof AkaishiFluidPipeBlockEntity p ? p : null;
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
                if (sameFamilyPipe(level.getBlockEntity(next))) {
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

        // 第一遍：统计每个邻居设备的观察方向与相邻管道模式集合。
        // 设备源/汇角色由"全部相邻管道模式"聚合判定（全推只作汇/全拉只作源/其余双向），
        // 避免遍历顺序决定行为（多管道共享设备时单段模式被静默覆盖）
        deviceObs.clear();
        for (BlockPos pipe : pipes) {
            AkaishiFluidPipeBlockEntity pb = pipeAt(pipe);
            int pipeMode = pb != null ? pb.getMode() : MODE_NORMAL;
            for (Direction dir : Direction.values()) {
                if (pb != null && pb.isDisconnected(dir)) {
                    continue;
                }
                BlockPos nb = pipe.relative(dir);
                if (!hasFluidTank(level.getBlockEntity(nb), dir)) {
                    continue;
                }
                DeviceObs obs = deviceObs.computeIfAbsent(nb, k -> new DeviceObs(nb));
                obs.dirs.add(dir);
                obs.sides++;
                obs.allPush &= pipeMode == MODE_PUSH;
                obs.allPull &= pipeMode == MODE_PULL;
            }
        }

        // 第二遍：按设备聚合角色收集源/汇罐（外部液体按观察方向收集，罐按底层身份去重）。
        // 角色：全推 → 设备只作汇；全拉 → 设备只作源；其余（含混合）→ 双向，按罐级 canExtract/canInsert 收敛
        sources.clear();
        sinks.clear();
        seenTanks.clear();
        for (DeviceObs obs : deviceObs.values()) {
            for (Direction dir : obs.dirs) {
                List<TankHandle> tanks = collectTanks(obs.pos, dir);
                for (TankHandle tank : tanks) {
                    if (!seenTanks.add(tank.identity())) {
                        continue;
                    }
                    boolean asSource = obs.allPush ? false : (obs.allPull ? true : tank.canExtract());
                    boolean asSink = obs.allPull ? false : (obs.allPush ? true : tank.canInsert());
                    if (asSource && tank.getAmount() > 0) {
                        sources.add(tank);
                    }
                    if (asSink && tank.getAmount() < tank.getCapacity()) {
                        sinks.add(tank);
                    }
                }
            }
        }
        // 网络内无设备源/汇时，全网各段管道缓冲作为兜底源/汇（外部注入后参与网络分发）
        boolean noDeviceSource = sources.isEmpty();
        boolean noDeviceSink = sinks.isEmpty();
        if (noDeviceSource || noDeviceSink) {
            for (BlockPos pipe : pipes) {
                AkaishiFluidPipeBlockEntity pb = pipeAt(pipe);
                if (pb == null) {
                    continue;
                }
                FluidTank b = pb.buffer();
                if (noDeviceSource && !b.isEmpty()) {
                    sources.add(TankHandle.of(b));
                }
                if (noDeviceSink && !b.isFull()) {
                    sinks.add(TankHandle.of(b));
                }
            }
        }
        if (sources.isEmpty() || sinks.isEmpty()) {
            return;
        }

        // 网络每 tick 总传输上限 = 管道段数 × 单段速率
        long networkRate = pipes.size() * AkaishiFluidPipeBlock.getTransferRate();
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
            long take = Math.min(Math.min(quota, source.getAmount()), AkaishiFluidPipeBlock.getTransferRate());
            if (take <= 0) {
                continue;
            }
            FluidStack taken = source.drain(take);
            // 家族液体过滤：普通管道不传输废料（外部罐若混入废料，立即放回，防止绕过本 mod 泄漏机制）；
            // 等离子体管道不传输非等离子体，普通/废料管道也不承接等离子体（三族彻底隔离）
            if ((!isWasteFamily() && ModFluids.isExhaustedFuel(taken.getFluid()))
                    || (!isPlasmaFamily() && ModFluids.isPlasma(taken.getFluid()))
                    || (isPlasmaFamily() && !ModFluids.isPlasma(taken.getFluid()))) {
                source.fill(taken);
                continue;
            }
            for (TankHandle sink : sinks) {
                if (taken.isEmpty()) {
                    break;
                }
                // 跳过与源同底层罐的汇：自循环只消耗配额，且无效标记方块
                if (sink.identity() == source.identity()) {
                    continue;
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

    /** 收集邻居方块暴露的全部液体罐（模组设备或外部能力），并标记其输入/输出权限。
     *  家族隔离：普通管道只取普通罐、废料管道只取废料罐、等离子体管道只取等离子体罐
     *  （按罐级 {@link IFluidPipeDevice#isWasteTank}/{@link IFluidPipeDevice#isPlasmaTank}），
     *  混合接入设备（如生命活化器）因此可同时接多族管道；废料/等离子体管道不接外部液体能力 */
    private List<TankHandle> collectTanks(BlockPos pos, Direction side) {
        List<TankHandle> result = new ArrayList<>();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IFluidPipeDevice device) {
            List<FluidTank> tanks = device.getFluidTanks();
            if (tanks == null) {
                return result; // 附属模组实现违约（接口已声明 @NotNull），防御性跳过
            }
            for (FluidTank tank : tanks) {
                if (device.isWasteTank(tank) != isWasteFamily() || device.isPlasmaTank(tank) != isPlasmaFamily()) {
                    continue; // 罐级家族过滤：本管道只与家族匹配的罐对接
                }
                result.add(TankHandle.of(tank, device.canPipeExtract(tank), device.canPipeInsert(tank)));
            }
            return result;
        }
        if (isWasteFamily() || isPlasmaFamily()) {
            return result; // 废料/等离子体管道只传本家族液体，不接外部液体能力（MEK 等）
        }
        if (IExternalFluidAccess.FluidBridge.INSTANCE != null) {
            IExternalFluidAccess.ExternalFluidTank tank = IExternalFluidAccess.FluidBridge.INSTANCE.getTank(level, pos, side);
            if (tank != null) {
                result.add(TankHandle.of(tank));
            }
        }
        return result;
    }

    /** 邻居方块是否存在可对接的液体罐（模组设备或外部液体能力），按罐级家族过滤 */
    private boolean hasFluidTank(BlockEntity be, Direction side) {
        if (be == null) {
            return false; // 邻居未加载/已拆除：视作无罐，避免 NPE 拖垮 tick
        }
        if (be instanceof IFluidPipeDevice device) {
            List<FluidTank> tanks = device.getFluidTanks();
            if (tanks == null) {
                return false;
            }
            for (FluidTank tank : tanks) {
                if (device.isWasteTank(tank) == isWasteFamily() && device.isPlasmaTank(tank) == isPlasmaFamily()) {
                    return true;
                }
            }
            return false;
        }
        return !isWasteFamily() && !isPlasmaFamily() && IExternalFluidAccess.FluidBridge.INSTANCE != null
                && IExternalFluidAccess.FluidBridge.INSTANCE.getTank(level, be.getBlockPos(), side) != null;
    }

    /** 按坐标取管道 BE（已卸载/类型不符时返回 null） */
    private AkaishiFluidPipeBlockEntity pipeAt(BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AkaishiFluidPipeBlockEntity p ? p : null;
    }

    /** 设备观察聚合：观察方向 + 相邻管道模式统计。
     *  角色判定：全部相邻管道为推 → 设备只作汇；全部为拉 → 设备只作源；其余（含混合）→ 双向 */
    private static final class DeviceObs {
        final BlockPos pos;
        final List<Direction> dirs = new ArrayList<>(2);
        int sides;
        boolean allPush = true;
        boolean allPull = true;

        DeviceObs(BlockPos pos) {
            this.pos = pos;
        }
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

        /** 底层罐身份：同一罐同时被标记为源与汇时跳过，防止自循环挤占配额饿死真实传输 */
        Object identity();

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

                @Override
                public Object identity() {
                    return tank;
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

                @Override
                public Object identity() {
                    return tank;
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
