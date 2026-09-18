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
 * 物品储存单元的数据本体：等阶 + 槽内容 + 同槽 IP 账本。
 * <p>
 * 不依赖方块实体 / 世界：宿主只负责生命周期与持久化转发，微缩件等其它宿主可直接复用本对象。
 * <p>
 * <b>双轨记账</b>（D3）：物品本体（含 NBT）落容器，占用 IP 落入同槽位的账本 {@link #slotIp}，
 * 二者在唯一写入口内同步变更，因此「物品 ↔ 账本」永不脱节（D10 无偏差）。
 * <p>
 * <b>入账锁定</b>：折算只在存入瞬间执行一次，取出只扣账本值，价值表重载不漂移。
 * <p>
 * 容器内容与账本一律经 {@link #save} / {@link #load} 落盘，禁止调用
 * {@code Containers#dropContents}（否则双份复制）。
 */
public final class ItemStorageUnitData implements IItemStorageUnit {

    /** 槽位账本标签 */
    private static final String TAG_SLOT_IP = "SlotIp";

    private ItemStorageUnitTier tier;

    /** 槽位数（各阶一致） */
    private final int slots;

    /** 槽位账本：{@code slotIp[i]} = 第 i 槽物品占用的 IP，与容器槽一一对应 */
    private final long[] slotIp;

    private final SimpleContainer container;

    /** 变更回调（宿主 setChanged）：数据对象不直接依赖方块实体 */
    private final Runnable changeCallback;

    public ItemStorageUnitData(ItemStorageUnitTier tier, int slots, Runnable changeCallback) {
        this.tier = tier;
        this.slots = slots;
        this.slotIp = new long[slots];
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

    /** 只读取槽（界面 / 终端浏览用） */
    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < slots ? container.getItem(slot) : ItemStack.EMPTY;
    }

    /** 单槽已占用 IP（只读，供物品库按占用排序，无需重查价值表） */
    @Override
    public long getSlotIp(int slot) {
        return slot >= 0 && slot < slots ? slotIp[slot] : 0L;
    }

    // ===== 唯一写入口（三法：insert / extract / setItemAt） =====

    /**
     * 当前还能再存入多少件（容量 + 空位双约束），供终端算单笔上限与预检。
     * 返回值即 {@link #insert} 的 atomic 边界。
     */
    @Override
    public int acceptable(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        int maxStack = stack.getMaxStackSize();
        long room = 0L;
        for (int i = 0; i < slots; i++) {
            ItemStack cur = container.getItem(i);
            if (cur.isEmpty()) {
                room += maxStack;
            } else if (ItemStack.isSameItemSameTags(cur, stack)) {
                room += maxStack - cur.getCount();
            }
        }
        long byCapacity = getRemainingIp() / ItemPoints.perItem(stack);
        return (int) Math.min(Math.min(room, byCapacity), Integer.MAX_VALUE);
    }

    /**
     * 存入整堆（唯一写入口之一）：容量或空位不足则<b>整笔拒绝</b>，不做部分存入。
     *
     * @return 实际存入数量（0 表示整笔未改动）
     */
    @Override
    public int insert(ItemStack stack) {
        if (stack.isEmpty() || acceptable(stack) < stack.getCount()) {
            return 0;
        }
        long unitIp = ItemPoints.perItem(stack);
        int remaining = stack.getCount();
        // 1) 先并入同物品同 NBT 的未满堆，避免无谓占格
        for (int i = 0; i < slots && remaining > 0; i++) {
            ItemStack cur = container.getItem(i);
            if (cur.isEmpty() || !ItemStack.isSameItemSameTags(cur, stack)) {
                continue;
            }
            int move = Math.min(stack.getMaxStackSize() - cur.getCount(), remaining);
            if (move <= 0) {
                continue;
            }
            cur.grow(move);
            slotIp[i] += unitIp * move;
            remaining -= move;
            setChanged();
        }
        // 2) 余量放入空槽
        for (int i = 0; i < slots && remaining > 0; i++) {
            if (!container.getItem(i).isEmpty()) {
                continue;
            }
            int move = Math.min(stack.getMaxStackSize(), remaining);
            container.setItem(i, stack.copyWithCount(move));
            slotIp[i] = unitIp * move;
            remaining -= move;
        }
        return stack.getCount() - remaining;
    }

    /**
     * 取出（唯一写入口之一）：原物取回（含 NBT，D9），账本按比例扣减，整槽取空时一次性归零。
     *
     * @return 取出的堆（空表示无货）
     */
    @Override
    public ItemStack extract(int slot, int amount) {
        if (slot < 0 || slot >= slots || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack cur = container.getItem(slot);
        if (cur.isEmpty()) {
            if (slotIp[slot] != 0L) {
                slotIp[slot] = 0L; // 空槽不应有账目，顺手修偏
                setChanged();
            }
            return ItemStack.EMPTY;
        }
        int take = Math.min(amount, cur.getCount());
        boolean whole = take >= cur.getCount();
        ItemStack out = cur.copyWithCount(take);
        if (whole) {
            container.setItem(slot, ItemStack.EMPTY);
            slotIp[slot] = 0L;
        } else {
            // 部分取出：按比例扣减（向下取整，误差 < 1 IP），整槽取空时上面已精确归零
            slotIp[slot] -= slotIp[slot] * take / cur.getCount();
            cur.shrink(take);
            setChanged();
        }
        return out;
    }

    /**
     * 直接置槽（唯一写入口之一）：账本按新内容折算一次并锁定。
     *
     * @return 被替换下来的原内容（容量不足时原样返回，表示未改动）
     */
    public ItemStack setItemAt(int slot, ItemStack stack) {
        if (slot < 0 || slot >= slots) {
            return ItemStack.EMPTY;
        }
        ItemStack old = container.getItem(slot);
        long newIp = ItemPoints.of(stack);
        if (getStoredIp() - slotIp[slot] + newIp > getIpCapacity()) {
            return old;
        }
        container.setItem(slot, stack);
        slotIp[slot] = stack.isEmpty() ? 0L : newIp;
        return old;
    }

    // ===== NBT 持久化（容器 + 账本同批落盘） =====

    public void save(CompoundTag tag) {
        NonNullList<ItemStack> items = NonNullList.withSize(slots, ItemStack.EMPTY);
        for (int i = 0; i < slots; i++) {
            items.set(i, container.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items);
        tag.putLongArray(TAG_SLOT_IP, slotIp);
    }

    /**
     * 把<b>任意形态</b>的储存单元（含方块实体形态）按本类格式写入一条条目
     * （{@code {Tier, Unit{Items, SlotIp}}}）。
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
        for (int i = 0; i < slots; i++) {
            items.set(i, unit.getItem(i));
            ips[i] = unit.getSlotIp(i);
        }
        ContainerHelper.saveAllItems(inner, items);
        inner.putLongArray(TAG_SLOT_IP, ips);
        entry.put(TAG_UNIT, inner);
        return entry;
    }

    /** 条目内的等阶键 */
    public static final String TAG_TIER = "Tier";
    /** 条目内的单元数据键（内含 Items / SlotIp） */
    public static final String TAG_UNIT = "Unit";

    public void load(CompoundTag tag) {
        NonNullList<ItemStack> items = NonNullList.withSize(slots, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < slots; i++) {
            container.setItem(i, items.get(i));
        }
        long[] loaded = tag.getLongArray(TAG_SLOT_IP);
        Arrays.fill(slotIp, 0L);
        System.arraycopy(loaded, 0, slotIp, 0, Math.min(loaded.length, slots));
        // 账本长度不符（伪造 / 旧档）时余位留 0，交由自愈按内容重算
        healLedger();
    }

    /**
     * 加载自愈（§2.5 防线 4）：账本必须与容器内容一一对应 ——
     * 空槽清账；缺账按当前内容折算补一次；账目高于内容价值（伪造 NBT）则夹回内容价值。
     */
    private void healLedger() {
        for (int i = 0; i < slots; i++) {
            ItemStack s = container.getItem(i);
            if (s.isEmpty()) {
                slotIp[i] = 0L;
                continue;
            }
            long value = ItemPoints.of(s);
            if (slotIp[i] <= 0L || slotIp[i] > value) {
                slotIp[i] = value;
            }
        }
        setChanged();
    }
}
