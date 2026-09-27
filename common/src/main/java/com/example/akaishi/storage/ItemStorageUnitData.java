package com.example.akaishi.storage;

import com.example.akaishi.api.storage.IItemStorageUnit;
import com.example.akaishi.block.ItemStorageUnitTier;
import com.example.akaishi.value.ItemPoints;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/**
 * 物品储存单元的数据本体：等阶 + 槽内容 + 同槽 IP 账本 + 同槽件数账本。
 * <p>
 * 不依赖方块实体 / 世界：宿主只负责生命周期与持久化转发，微缩件等其它宿主可直接复用本对象。
 * <p>
 * <b>双轨记账</b>（D3）：物品本体（含 NBT）落容器，占用 IP 落入同槽位的账本 {@link #slotIp}，
 * 二者在唯一写入口内同步变更，因此「物品 ↔ 账本」永不脱节（D10 无偏差）。
 * <p>
 * <b>一槽多堆（本轮改定）</b>：一个槽不再只装一堆，件数上限由 IP 容量决定；身份仍是
 * {@code (物品, NBT)} —— 不同 NBT 各占独立槽位、不得合并，同 NBT 必须并入同一条目（见 {@link #compact()}）。
 * <p>
 * <b>为什么件数必须另立账本 {@link #slotCount}</b>：1.20.1 的 {@code ItemStack} 序列化把数量写成 NBT
 * {@code Count}（<b>byte</b>，&gt;127 回绕），且原版槽同步、管道按 {@code getMaxStackSize()} 判余量都
 * 只认单堆语义 ⇒ 大数量若靠 {@code ItemStack.count} 承载，一存档 / 一同步 / 一过管道就损坏。
 * 因此容器只保存「身份 + 夹到单堆上限的展示数量」（视图堆，见 {@link IItemStorageUnit#getItem}），
 * 真实件数一律走 {@link #slotCount} 落盘（{@code Counts} long 数组）与 {@link #storedCount(int)} 读数。
 * <p>
 * <b>入账锁定</b>：折算只在存入瞬间执行一次，取出只扣账本值，价值表重载不漂移。
 * <p>
 * 容器内容与账本一律经 {@link #save} / {@link #load} 落盘，禁止调用
 * {@code Containers#dropContents}（否则双份复制）。
 */
public final class ItemStorageUnitData implements IItemStorageUnit {

    /** 槽位 IP 账本标签 */
    private static final String TAG_SLOT_IP = "SlotIp";
    /** 槽位件数账本标签（一槽多堆：真实件数，可超单堆上限） */
    private static final String TAG_SLOT_COUNT = "Counts";

    private ItemStorageUnitTier tier;

    /** 槽位数（各阶一致） */
    private final int slots;

    /** 槽位账本：{@code slotIp[i]} = 第 i 槽物品占用的 IP，与容器槽一一对应 */
    private final long[] slotIp;

    /** 槽位件数账本：{@code slotCount[i]} = 第 i 槽真实件数（可远超单堆上限），与容器槽一一对应 */
    private final long[] slotCount;

    private final SimpleContainer container;

    /** 变更回调（宿主 setChanged）：数据对象不直接依赖方块实体 */
    private final Runnable changeCallback;

    public ItemStorageUnitData(ItemStorageUnitTier tier, int slots, Runnable changeCallback) {
        this.tier = tier;
        this.slots = slots;
        this.slotIp = new long[slots];
        this.slotCount = new long[slots];
        this.changeCallback = changeCallback;
        this.container = new SimpleContainer(slots) {
            @Override
            public void setChanged() {
                super.setChanged();
                ItemStorageUnitData.this.setChanged();
            }
        };
    }

    /** 宿主侧变更通知（写入口内统一调用） */
    private void setChanged() {
        this.changeCallback.run();
    }

    public ItemStorageUnitTier tier() {
        return tier;
    }

    public void setTier(ItemStorageUnitTier tier) {
        this.tier = tier;
    }

    // ===== 契约（终端只读聚合） =====

    @Override
    public long getIpCapacity() {
        return tier.ipCapacity;
    }

    @Override
    public long getStoredIp() {
        // 实时求和：总量不另存缓存，从结构上排除「总数与明细不一致」的可能
        long total = 0L;
        for (long v : slotIp) {
            total += v;
        }
        return total;
    }

    /** 剩余可用 IP（界面展示「容量还剩多少」，用户口径） */
    public long getRemainingIp() {
        return Math.max(0L, getIpCapacity() - getStoredIp());
    }

    /** 槽位数（只读视图用，不暴露容器本体，避免外部直写绕过账本） */
    @Override
    public int slots() {
        return slots;
    }

    /** 只读取视图堆（界面 / 终端浏览用）：数量恒夹到单堆上限，真实件数见 {@link #storedCount(int)} */
    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < slots ? container.getItem(slot) : ItemStack.EMPTY;
    }

    /** 单槽真实件数（一槽多堆：可远超单堆上限） */
    @Override
    public long storedCount(int slot) {
        return slot >= 0 && slot < slots ? slotCount[slot] : 0L;
    }

    /** 单槽已占用 IP（只读，供物品库按占用排序，无需重查价值表） */
    @Override
    public long getSlotIp(int slot) {
        return slot >= 0 && slot < slots ? slotIp[slot] : 0L;
    }

    // ===== 唯一写入口（三法：insert / extract / setItemAt） =====

    /**
     * 当前还能再存入多少件：槽位侧走 {@link #slotRoom}（有空槽 / 已有条目即不限），
     * 再与「剩余 IP 能装几件」取小 ⇒ 一槽多堆后实际上限由 IP 容量决定。
     * 返回值即 {@link #insert} 的 atomic 边界。
     */
    @Override
    public int acceptable(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        long byCapacity = getRemainingIp() / ItemPoints.perItem(stack);
        long room = slotRoom(stack);
        return (int) Math.min(Math.min(room, byCapacity), Integer.MAX_VALUE);
    }

    /**
     * 存入整堆（唯一写入口之一）：容量或槽位不足则<b>整笔拒绝</b>，不做部分存入。
     * <p>
     * 一槽多堆：同 {@code (物品, NBT)} 一律并入已有条目（件数不限），否则占用一个空槽；
     * 单槽件数不再受单堆上限约束。
     *
     * @return 实际存入数量（0 表示整笔未改动）
     */
    @Override
    public int insert(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        int want = stack.getCount();
        if (want <= 0 || acceptable(stack) < want) {
            return 0;
        }
        long unitIp = ItemPoints.perItem(stack);
        // 1) 先并入同物品同 NBT 的条目（一槽多堆：不再受单堆上限约束；compact 后每种至多一条）
        for (int i = 0; i < slots; i++) {
            ItemStack cur = container.getItem(i);
            if (cur.isEmpty() || !ItemStack.isSameItemSameTags(cur, stack)) {
                continue;
            }
            slotCount[i] += want;
            slotIp[i] += unitIp * want;
            syncViewCount(i);
            setChanged();
            return want;
        }
        // 2) 余量放入空槽
        for (int i = 0; i < slots; i++) {
            if (!container.getItem(i).isEmpty()) {
                continue;
            }
            container.setItem(i, stack.copyWithCount(viewCount(want, stack)));
            slotCount[i] = want;
            slotIp[i] = unitIp * want;
            setChanged();
            return want;
        }
        return 0;
    }

    /**
     * 取出（唯一写入口之一）：原物取回（含 NBT，D9），账本按比例扣减，整槽取空时一次性归零。
     * <p>
     * 件数以 {@link #slotCount} 为准（一槽多堆：槽里可能还剩很多），扣账公式与
     * {@code TerminalActions.withdrawIp} 严格一致（部分取出按比例向下取整，整槽取空精确归零）。
     *
     * @return 取出的堆（空表示无货）
     */
    @Override
    public ItemStack extract(int slot, int amount) {
        if (slot < 0 || slot >= slots || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack cur = container.getItem(slot);
        if (cur.isEmpty() || slotCount[slot] <= 0L) {
            if (slotIp[slot] != 0L || slotCount[slot] != 0L) {
                // 空槽不应有账目，顺手修偏
                container.setItem(slot, ItemStack.EMPTY);
                slotCount[slot] = 0L;
                slotIp[slot] = 0L;
                setChanged();
            }
            return ItemStack.EMPTY;
        }
        long stored = slotCount[slot];
        int take = (int) Math.min(amount, stored);
        boolean whole = take >= stored;
        ItemStack out = cur.copyWithCount(take);
        if (whole) {
            container.setItem(slot, ItemStack.EMPTY);
            slotCount[slot] = 0L;
            slotIp[slot] = 0L;
        } else {
            // 部分取出：按比例扣减（向下取整，误差 < 1 IP），整槽取空时上面已精确归零
            slotIp[slot] -= slotIp[slot] * take / stored;
            slotCount[slot] = stored - take;
            syncViewCount(slot);
            setChanged();
        }
        return out;
    }

    /**
     * 直接置槽（唯一写入口之一）：账本按新内容折算一次并锁定。
     * <p>
     * 语义与历史一致（数量取传入堆的件数）；一槽多堆下若传入超过单堆上限的堆，件数照记、
     * 视图堆数量夹到单堆上限。
     *
     * @return 被替换下来的原内容（容量不足时原样返回，表示未改动）
     */
    public ItemStack setItemAt(int slot, ItemStack stack) {
        if (slot < 0 || slot >= slots) {
            return ItemStack.EMPTY;
        }
        ItemStack old = container.getItem(slot);
        long newCount = stack.isEmpty() ? 0L : stack.getCount();
        long newIp = stack.isEmpty() ? 0L : ItemPoints.perItem(stack) * newCount;
        if (getStoredIp() - slotIp[slot] + newIp > getIpCapacity()) {
            return old;
        }
        if (stack.isEmpty()) {
            container.setItem(slot, ItemStack.EMPTY);
            slotCount[slot] = 0L;
            slotIp[slot] = 0L;
        } else {
            container.setItem(slot, stack.copyWithCount(viewCount(newCount, stack)));
            slotCount[slot] = newCount;
            slotIp[slot] = newIp;
        }
        return old;
    }

    // ===== 视图堆数量与槽账本的一致性 =====

    /** 视图堆数量 = min(真实件数, 单堆上限)：只为展示/搬运，绝非真实件数 */
    private static int viewCount(long count, ItemStack proto) {
        return (int) Math.min(count, proto.getMaxStackSize());
    }

    /** 让第 i 槽的视图堆数量与件数账本对齐（真实件数只落在 {@link #slotCount}） */
    private void syncViewCount(int i) {
        ItemStack cur = container.getItem(i);
        if (cur.isEmpty()) {
            return;
        }
        int want = viewCount(slotCount[i], cur);
        if (cur.getCount() != want) {
            cur.setCount(want);
        }
    }

    // ===== NBT 持久化（容器 + 双账本同批落盘） =====

    public void save(CompoundTag tag) {
        NonNullList<ItemStack> items = NonNullList.withSize(slots, ItemStack.EMPTY);
        for (int i = 0; i < slots; i++) {
            // 只落「身份 + 夹到单堆上限的展示数量」（容器不变量），大数量走 Counts（见类注释）
            items.set(i, container.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items);
        tag.putLongArray(TAG_SLOT_COUNT, slotCount);
        tag.putLongArray(TAG_SLOT_IP, slotIp);
    }

    /**
     * 把<b>任意形态</b>的储存单元（含方块实体形态）按本类格式写入一条条目
     * （{@code {Tier, Unit{Items, Counts, SlotIp}}}）。
     * <p>
     * 微缩（坍缩）时单元还在方块实体里，必须能从接口面导出 —— 与 {@link #save} 共用同一套键，
     * 因此导出的数据可直接被微缩状态读回，两边格式不会漂移。
     */
    public static CompoundTag writeEntry(IItemStorageUnit unit, ItemStorageUnitTier tier) {
        CompoundTag entry = new CompoundTag();
        entry.putString(TAG_TIER, tier.name());
        CompoundTag inner = new CompoundTag();
        int slots = unit.slots();
        NonNullList<ItemStack> items = NonNullList.withSize(slots, ItemStack.EMPTY);
        long[] ips = new long[slots];
        long[] counts = new long[slots];
        for (int i = 0; i < slots; i++) {
            items.set(i, unit.getItem(i));
            ips[i] = unit.getSlotIp(i);
            counts[i] = unit.storedCount(i);
        }
        ContainerHelper.saveAllItems(inner, items);
        inner.putLongArray(TAG_SLOT_COUNT, counts);
        inner.putLongArray(TAG_SLOT_IP, ips);
        entry.put(TAG_UNIT, inner);
        return entry;
    }

    /** 条目内的等阶键 */
    public static final String TAG_TIER = "Tier";
    /** 条目内的单元数据键（内含 Items / Counts / SlotIp） */
    public static final String TAG_UNIT = "Unit";

    public void load(CompoundTag tag) {
        NonNullList<ItemStack> items = NonNullList.withSize(slots, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        long[] loadedCounts = tag.getLongArray(TAG_SLOT_COUNT);
        boolean hasCounts = loadedCounts.length > 0;
        for (int i = 0; i < slots; i++) {
            ItemStack s = items.get(i);
            container.setItem(i, s);
            // 旧档（无 Counts）件数即容器里那一堆（≤ 单堆上限）；新档以件数账本为准
            slotCount[i] = hasCounts
                    ? (i < loadedCounts.length ? loadedCounts[i] : 0L)
                    : (s.isEmpty() ? 0L : s.getCount());
        }
        long[] loadedIp = tag.getLongArray(TAG_SLOT_IP);
        Arrays.fill(slotIp, 0L);
        System.arraycopy(loadedIp, 0, slotIp, 0, Math.min(loadedIp.length, slots));
        // 账本长度不符（伪造 / 旧档）时余位留 0，交由自愈按内容重算
        healLedger();
    }

    /**
     * 加载自愈（§2.5 防线 4）：双账本必须与容器内容一一对应 ——
     * 空槽清账；缺件数按容器补算；账目高于内容价值（伪造 NBT）则夹回内容价值。末尾做一次
     * {@link #compact()} 把旧档的多槽碎片合并成「一条目一槽」。
     */
    private void healLedger() {
        for (int i = 0; i < slots; i++) {
            ItemStack s = container.getItem(i);
            if (s.isEmpty()) {
                container.setItem(i, ItemStack.EMPTY);
                slotCount[i] = 0L;
                slotIp[i] = 0L;
                continue;
            }
            if (slotCount[i] <= 0L) {
                slotCount[i] = s.getCount(); // 缺账补算（旧档 / 脏数据）
            }
            syncViewCount(i);
            long value = ItemPoints.perItem(s) * slotCount[i];
            if (slotIp[i] <= 0L || slotIp[i] > value) {
                slotIp[i] = value;
            }
        }
        compact();
        setChanged();
    }

    /**
     * 同种合并（compaction）：把同 {@code (物品, NBT)} 分散在多槽的条目并成一条，
     * 释放多余槽位。加载时执行一次（含旧档迁移），此后 {@link #insert} 恒定并入已有条目，
     * 不再产生碎片。
     * <p>
     * <b>守恒</b>：合并只做「件数相加、IP 相加、清空被并入的那一槽」，源与目的都在同一账本内，
     * 故 {@code Σ slotCount}、{@code Σ slotIp} 逐项不变（物品总数与 IP 账本守恒）；
     * 释放的槽位 = 被清空的槽，其 {@code slotCount/slotIp} 一并归零，不会留下孤儿账目。
     */
    private void compact() {
        for (int i = 0; i < slots; i++) {
            if (slotCount[i] <= 0L || container.getItem(i).isEmpty()) {
                continue;
            }
            for (int j = i + 1; j < slots; j++) {
                if (slotCount[j] <= 0L || container.getItem(j).isEmpty()
                        || !ItemStack.isSameItemSameTags(container.getItem(i), container.getItem(j))) {
                    continue;
                }
                slotCount[i] += slotCount[j];
                slotIp[i] += slotIp[j];
                container.setItem(j, ItemStack.EMPTY);
                slotCount[j] = 0L;
                slotIp[j] = 0L;
            }
            syncViewCount(i);
        }
    }
}
