package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.storage.IItemStorageUnit;
import com.example.akaishi.block.AkaishiItemStorageUnitBlock;
import com.example.akaishi.block.ItemStorageUnitTier;
import com.example.akaishi.menu.AkaishiItemStorageUnitMenu;
import com.example.akaishi.value.ItemPoints;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/**
 * 物品储存单元方块实体：以 IP 计量「占用量」的贴装式仓库（D5）。
 * <p>
 * <b>双轨记账</b>（D3）：物品本体（含 NBT）落容器，占用 IP 落入同槽位的账本 {@link #slotIp}，
 * 二者在唯一写入口内同步变更，因此「物品 ↔ 账本」永不脱节（D10 无偏差）。
 * <p>
 * <b>入账锁定</b>：折算只在存入瞬间执行一次，取出只扣账本值，价值表重载不漂移。
 * <p>
 * <b>被动存储</b>：不实现 {@code IItemPipeDevice} / 不暴露 {@code ITEM_HANDLER}，
 * 外部物流无法直写容器（否则会绕过账本）。物品进出仅由物品终端调用
 * {@link #insert} / {@link #extract} / {@link #setItemAt} 完成。
 * <p>
 * 数据经 {@link AkaishiItemStorageUnitBlock} 继承的打包链路随掉落物保留；容器内容与账本
 * 一律走 {@code saveAdditional}，禁止调用 {@code Containers#dropContents}（否则双份复制）。
 */
public class AkaishiItemStorageUnitBlockEntity extends BlockEntity
        implements IDataCarrier, IItemStorageUnit, ExtendedMenuProvider {

    /** 槽位数：单页大箱 54（6×9），与项目存储库单页口径一致 */
    public static final int SLOTS = 54;

    /** 槽位账本标签 */
    private static final String TAG_SLOT_IP = "SlotIp";

    /** 槽位账本：{@code slotIp[i]} = 第 i 槽物品占用的 IP，与容器槽一一对应 */
    private final long[] slotIp = new long[SLOTS];

    private final SimpleContainer container;

    public AkaishiItemStorageUnitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_ITEM_STORAGE_UNIT.get(), pos, state);
        this.container = new SimpleContainer(SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiItemStorageUnitBlockEntity.this.setChanged();
            }
        };
    }

    /** 本机等阶（由方块决定；异常方块退回基础档，避免空指针） */
    private ItemStorageUnitTier tier() {
        Block block = getBlockState().getBlock();
        return block instanceof AkaishiItemStorageUnitBlock unit ? unit.getTier() : ItemStorageUnitTier.BASIC;
    }

    // ===== 契约（终端只读聚合） =====

    @Override
    public long getIpCapacity() {
        return tier().ipCapacity;
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
        return SLOTS;
    }

    /** 只读取槽（界面 / 终端浏览用） */
    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < SLOTS ? container.getItem(slot) : ItemStack.EMPTY;
    }

    /** 单槽已占用 IP（只读，供物品库按占用排序，无需重查价值表） */
    @Override
    public long getSlotIp(int slot) {
        return slot >= 0 && slot < SLOTS ? slotIp[slot] : 0L;
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
        for (int i = 0; i < SLOTS; i++) {
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
        for (int i = 0; i < SLOTS && remaining > 0; i++) {
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
        for (int i = 0; i < SLOTS && remaining > 0; i++) {
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
        if (slot < 0 || slot >= SLOTS || amount <= 0) {
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
        if (slot < 0 || slot >= SLOTS) {
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

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < SLOTS; i++) {
            items.set(i, container.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items);
        tag.putLongArray(TAG_SLOT_IP, slotIp);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < SLOTS; i++) {
            container.setItem(i, items.get(i));
        }
        long[] loaded = tag.getLongArray(TAG_SLOT_IP);
        Arrays.fill(slotIp, 0L);
        System.arraycopy(loaded, 0, slotIp, 0, Math.min(loaded.length, SLOTS));
        // 账本长度不符（伪造 / 旧档）时余位留 0，交由自愈按内容重算
        healLedger();
    }

    /**
     * 加载自愈（§2.5 防线 4）：账本必须与容器内容一一对应 ——
     * 空槽清账；缺账按当前内容折算补一次；账目高于内容价值（伪造 NBT）则夹回内容价值。
     */
    private void healLedger() {
        for (int i = 0; i < SLOTS; i++) {
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

    // ===== 菜单入口（ExtendedMenuProvider：右键打开只读视图，按钮位置经 saveExtraData 下发） =====

    @Override
    public Component getDisplayName() {
        // 直接取方块名：三阶单元的双语名随方块解析，无需在此分支
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiItemStorageUnitMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }
}
