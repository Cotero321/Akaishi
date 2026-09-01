package com.example.akaishi.block.entity;

import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.block.AkaishiItemPipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
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
 * 物品管道方块实体：物流网络传输核心。
 * 每 tick 由网络"代表节点"（网络中坐标最小的管道）执行 BFS 收集全部连通管道与
 * 相邻容器设备，再从可输出设备的输出槽抽取物品，注入可接收设备的输入槽。
 * 管道可被配置器设置为方向模式：正常 / 推 / 拉，并可断开单侧连接以精细控制流向。
 * <p>
 * 设备槽位规则：实现 {@link IItemPipeDevice} 的机器按声明的输入/输出槽精准对接；
 * 普通容器（箱子、漏斗等）全部槽位既作输入又作输出。
 */
public class AkaishiItemPipeBlockEntity extends BlockEntity implements AkaishiPipeControl {

    /** 方向模式：正常（默认，按设备能力双向判定） */
    public static final int MODE_NORMAL = 0;
    /** 方向模式：推（相连设备只作物品汇，管道主动向设备推物品） */
    public static final int MODE_PUSH = 1;
    /** 方向模式：拉（相连设备只作物品源，管道主动从设备拉物品） */
    public static final int MODE_PULL = 2;

    /** 网络规模上限：防止超大网络 BFS 遍历过多节点拖慢主线程（超出则截断，远端设备可能无法接入） */
    private static final int MAX_NETWORK = 1024;

    /** 本段管道方向模式，默认正常 */
    private int mode = MODE_NORMAL;

    /** 被配置器断开的连接面（bit 0-5 对应 Direction.ordinal()），断开后不参与连接与传输 */
    private int disconnectedMask;

    // ===== 网络拓扑缓存：仅在结构变化时重扫，避免每 tick 全网络 BFS（与能量管道一致）=====
    /** 网络结构是否可能已变化（放置/拆除/断开时置位，经邻居管道逐 tick 传播至代表） */
    private boolean networkDirty = true;
    /** 代表节点缓存的本网络管道列表（BFS 结果）；null 表示尚无缓存 */
    private List<BlockPos> cachedPipes;
    /** 代表节点缓存的本网络总传输速率（各段管道速率之和） */
    private long cachedNetworkRate;

    public AkaishiItemPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public AkaishiItemPipeBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CHISHI_ITEM_PIPE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiItemPipeBlockEntity be) {
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

    /** 切换某方向的连接（断开↔恢复），返回切换后是否处于断开状态 */
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
        // 快速裁剪：若存在坐标更小的相邻同类管道，则本节点非网络代表，交由代表统一传输
        if (!isNetworkRepresentative()) {
            // 非代表：若自身缓存标记脏，把标记传播给相邻管道（逐 tick 泛洪至代表），并清除自身标记
            if (networkDirty) {
                networkDirty = false;
                for (Direction dir : Direction.values()) {
                    if (isDisconnected(dir)) {
                        continue;
                    }
                    BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
                    if (neighbor instanceof AkaishiItemPipeBlockEntity np) {
                        np.markDirty();
                    }
                }
            }
            return;
        }
        // 代表：无缓存时先做轻量邻居检查，孤立管道（无管道/容器邻居）无需处理
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

    /** 是否本网络代表：不存在坐标更小的相邻同类管道（局部最小唯一，等于网络坐标最小节点） */
    private boolean isNetworkRepresentative() {
        for (Direction dir : Direction.values()) {
            if (isDisconnected(dir)) {
                continue;
            }
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor instanceof AkaishiItemPipeBlockEntity
                    && neighbor.getBlockPos().compareTo(worldPosition) < 0) {
                return false;
            }
        }
        return true;
    }

    /** 是否存在相邻的同类管道或可访问容器设备 */
    private boolean hasNetworkNeighbor() {
        for (Direction dir : Direction.values()) {
            if (isDisconnected(dir)) {
                continue;
            }
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor instanceof AkaishiItemPipeBlockEntity) {
                return true;
            }
            if (AkaishiItemPipeBlock.isPipeAccessible(neighbor)) {
                return true;
            }
        }
        return false;
    }

    /** 网络设备条目：持有容器引用及输入/输出槽位 */
    private record DeviceEntry(Container device, int[] inputSlots, int[] outputSlots) {
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
            AkaishiItemPipeBlockEntity curPipe = level.getBlockEntity(cur) instanceof AkaishiItemPipeBlockEntity p ? p : null;
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
                if (level.getBlockEntity(next) instanceof AkaishiItemPipeBlockEntity) {
                    queue.add(next);
                }
            }
        }
        // 缓存网络总传输速率（各段管道速率之和），避免每 tick 重算
        long rate = 0;
        for (BlockPos pipe : pipes) {
            if (level.getBlockEntity(pipe) instanceof AkaishiItemPipeBlockEntity
                    && level.getBlockState(pipe).getBlock() instanceof AkaishiItemPipeBlock pb) {
                rate += pb.getTransferRate();
            }
        }
        this.cachedPipes = pipes;
        this.cachedNetworkRate = rate;
        this.networkDirty = false;
    }

    /** 基于缓存的网络成员执行一次物品传输（代表每 tick 调用，正常 tick 零 BFS） */
    private void transferNetwork() {
        List<BlockPos> pipes = cachedPipes;

        // 收集源与汇。推模式：相连设备只作汇；拉模式：只作源；正常模式：按设备能力双向判定。
        // 纯源（仅输出）与双向缓冲分开记录，传输时优先抽取纯源，避免先抽干缓冲。
        List<DeviceEntry> pureSources = new ArrayList<>();
        List<DeviceEntry> bufferSources = new ArrayList<>();
        List<DeviceEntry> sinks = new ArrayList<>();
        Set<BlockPos> visitedDevices = new HashSet<>();
        for (BlockPos pipe : pipes) {
            AkaishiItemPipeBlockEntity pb = level.getBlockEntity(pipe) instanceof AkaishiItemPipeBlockEntity p ? p : null;
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
                if (!AkaishiItemPipeBlock.isPipeAccessible(be)) {
                    continue;
                }
                Container device = (Container) be;
                int[] inputSlots = inputSlotsOf(device);
                int[] outputSlots = outputSlotsOf(device);
                boolean asSource = switch (pipeMode) {
                    case MODE_PUSH -> false;   // 推：只作汇，不作源
                    case MODE_PULL -> true;    // 拉：强制作源
                    default -> outputSlots.length > 0;
                };
                boolean asSink = switch (pipeMode) {
                    case MODE_PUSH -> true;    // 推：强制作汇
                    case MODE_PULL -> false;   // 拉：只作源，不作汇
                    default -> inputSlots.length > 0;
                };
                DeviceEntry entry = new DeviceEntry(device, inputSlots, outputSlots);
                if (asSource && outputSlots.length > 0) {
                    // 可输入的设备是双向缓冲（如箱子），后抽；纯源优先
                    if (inputSlots.length > 0) {
                        bufferSources.add(entry);
                    } else {
                        pureSources.add(entry);
                    }
                }
                if (asSink && inputSlots.length > 0) {
                    sinks.add(entry);
                }
            }
        }
        if ((pureSources.isEmpty() && bufferSources.isEmpty()) || sinks.isEmpty()) {
            return;
        }
        // 汇没有任何空余时直接跳过，避免"抽了又退回"的无谓抖动
        if (!hasSinkSpace(sinks)) {
            return;
        }

        // 网络每 tick 总传输上限 = 缓存的全网络管道传输速率之和（个/tick）
        long networkRate = cachedNetworkRate;
        if (networkRate <= 0) {
            return;
        }

        // 传输：从源输出槽抽取（最多 networkRate 个），立即插入汇输入槽；插不进的退回源。
        // 优先抽取纯源，再抽双向缓冲；插入时跳过与源相同的设备，避免物品原地搬运回流。
        long remainingRate = networkRate;
        for (DeviceEntry source : pureSources) {
            remainingRate = transferFromSource(source, sinks, remainingRate);
            if (remainingRate <= 0) {
                return;
            }
        }
        for (DeviceEntry source : bufferSources) {
            remainingRate = transferFromSource(source, sinks, remainingRate);
            if (remainingRate <= 0) {
                return;
            }
        }
    }

    /** 从单个源设备抽取并配送物品，返回剩余传输额度 */
    private long transferFromSource(DeviceEntry source, List<DeviceEntry> sinks, long remainingRate) {
        for (int slot : source.outputSlots) {
            if (remainingRate <= 0) {
                break;
            }
            int amount = (int) Math.min(remainingRate, 64);
            ItemStack stack = extractFromSlots(source.device, new int[]{slot}, amount, false);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack left = insertToSinks(source, stack, sinks);
            if (!left.isEmpty()) {
                // 汇都放不下时退回源（放回输出槽，保持物品不丢失）
                insertIntoSlots(source.device, source.outputSlots, left, false);
            }
            remainingRate -= (stack.getCount() - left.getCount());
        }
        return remainingRate;
    }

    /** 将物品依次插入各汇的输入槽；跳过与源相同的设备；返回无法插入的剩余部分 */
    private ItemStack insertToSinks(DeviceEntry source, ItemStack stack, List<DeviceEntry> sinks) {
        ItemStack remaining = stack.copy();
        for (DeviceEntry sink : sinks) {
            if (sink.device() == source.device()) {
                continue;
            }
            remaining = insertIntoSlots(sink.device(), sink.inputSlots(), remaining, false);
            if (remaining.isEmpty()) {
                break;
            }
        }
        return remaining;
    }

    /** 是否存在任意汇输入槽有空余 */
    private boolean hasSinkSpace(List<DeviceEntry> sinks) {
        for (DeviceEntry sink : sinks) {
            for (int slot : sink.inputSlots()) {
                ItemStack stack = sink.device().getItem(slot);
                if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                    return true;
                }
            }
        }
        return false;
    }

    // ===== 设备槽位适配与物品搬运工具 =====

    /** 设备的输入槽：IItemPipeDevice 按声明，普通容器全槽 */
    private static int[] inputSlotsOf(Container device) {
        if (device instanceof IItemPipeDevice pipeDevice) {
            return pipeDevice.getPipeInputSlots();
        }
        return allSlots(device);
    }

    /** 设备的输出槽：IItemPipeDevice 按声明，普通容器全槽 */
    private static int[] outputSlotsOf(Container device) {
        if (device instanceof IItemPipeDevice pipeDevice) {
            return pipeDevice.getPipeOutputSlots();
        }
        return allSlots(device);
    }

    private static int[] allSlots(Container device) {
        int[] slots = new int[device.getContainerSize()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
    }

    /** 从指定槽位中抽取最多 amount 个物品（只取第一个非空槽，避免跨槽合并） */
    private static ItemStack extractFromSlots(Container container, int[] slots, int amount, boolean simulate) {
        for (int slot : slots) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int take = Math.min(amount, stack.getCount());
            if (simulate) {
                ItemStack copy = stack.copy();
                copy.setCount(take);
                return copy;
            }
            return container.removeItem(slot, take);
        }
        return ItemStack.EMPTY;
    }

    /** 将物品插入指定槽位（先合并同物品堆叠，再放入空槽），返回无法插入的剩余部分 */
    private static ItemStack insertIntoSlots(Container container, int[] slots, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        // 第一遍：合并进同物品且未满的堆叠
        for (int slot : slots) {
            if (remaining.isEmpty()) {
                break;
            }
            ItemStack target = container.getItem(slot);
            if (target.isEmpty() || !ItemStack.isSameItemSameTags(target, remaining)
                    || target.getCount() >= target.getMaxStackSize()) {
                continue;
            }
            int place = Math.min(target.getMaxStackSize() - target.getCount(), remaining.getCount());
            if (!simulate) {
                target.grow(place);
                container.setChanged();
            }
            remaining.shrink(place);
        }
        // 第二遍：整体放入空槽
        for (int slot : slots) {
            if (remaining.isEmpty()) {
                break;
            }
            if (container.getItem(slot).isEmpty() && container.canPlaceItem(slot, remaining)) {
                if (!simulate) {
                    container.setItem(slot, remaining.copy());
                    container.setChanged();
                }
                remaining = ItemStack.EMPTY;
            }
        }
        return remaining;
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
