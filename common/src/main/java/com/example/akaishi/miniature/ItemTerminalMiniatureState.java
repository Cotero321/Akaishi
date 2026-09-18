package com.example.akaishi.miniature;

import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.miniature.MiniatureTerminalState;
import com.example.akaishi.api.storage.IItemStorageUnit;
import com.example.akaishi.api.storage.IItemTerminalHost;
import com.example.akaishi.block.AkaishiItemTerminalBufferModuleBlock;
import com.example.akaishi.block.ItemStorageUnitTier;
import com.example.akaishi.block.entity.AkaishiItemStorageUnitBlockEntity;
import com.example.akaishi.block.entity.MiniatureTerminalBlockEntity;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.menu.TerminalActions;
import com.example.akaishi.storage.ItemStorageUnitData;
import com.example.akaishi.util.LongDataSlots;
import com.example.akaishi.value.ItemPoints;
import com.example.akaishi.value.ItemTerminalFee;
import com.example.akaishi.wireless.ItemPortTransfer;
import com.example.akaishi.wireless.TerminalSecurity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 微缩后的「物品终端」状态：原终端的全部数据 + 全部对外能力。
 * <p>
 * 为什么能零成本继承原终端的语义：储存单元被抽成了纯数据对象
 * （{@link ItemStorageUnitData}），库页菜单只认 {@link IItemTerminalHost}，
 * 搬运引擎与计费（{@link ItemPortTransfer} / {@link ItemTerminalFee}）也都是按契约取数。
 * 因此这里只做三件事：<b>持有数据</b>、<b>实现宿主契约</b>、<b>按同一套口径落账</b>。
 * <p>
 * 对外物品能力用两个"虚拟槽"（不存物、纯转发），语义与储存无线输入/输出口完全一致：
 * <ul>
 *   <li>槽 {@link #SLOT_IN}：只允许外部塞入，物品直接进库并结算<b>存入费</b>；</li>
 *   <li>槽 {@link #SLOT_OUT}：只允许外部抽取，从库中取出并结算<b>取出费</b>；</li>
 *   <li>费用不足一律整笔拒绝（不吞不吐），与终端 GUI 存取同一套算法（4 IP = 1 赤能源）。</li>
 * </ul>
 */
public final class ItemTerminalMiniatureState implements MiniatureTerminalState, IItemTerminalHost {

    /** 虚拟输入槽（外部塞入） */
    public static final int SLOT_IN = 0;
    /** 虚拟输出槽（外部抽取） */
    public static final int SLOT_OUT = 1;

    /** payload 键：储存单元列表（坍缩导出与读回共用，勿单独改一处） */
    public static final String TAG_UNITS = "Units";
    /** payload 键：安全表 */
    public static final String TAG_SECURITY = "Security";
    /** payload 键：赤能源缓冲余额 */
    public static final String TAG_ENERGY = "Energy";
    /** payload 键：原结构缓冲扩展组件份数（决定微缩后的缓冲容量） */
    public static final String TAG_BUFFER_MODULES = "BufferModules";
    /** payload 键：原结构费率减免份数 */
    public static final String TAG_FEE_MODULES = "FeeModules";

    private final MiniatureTerminalBlockEntity be;

    /** 原结构里各储存单元的数据（保序；容量与占用随之一并继承） */
    private final List<ItemStorageUnitData> units = new ArrayList<>();
    /** 安全表（归属者 + 权限表）：整体继承，微缩不改授权 */
    private final TerminalSecurity security;
    /** 赤能源缓冲：容量 = 配置基准 + 原结构缓冲扩展组件加成 */
    private final AkaishiEnergyStorage energy;
    private final UUID terminalId;
    /** 原结构生效的费率减免份数（0 ~ 上限） */
    private final int feeModules;

    private final SimpleContainerData data = new SimpleContainerData(DATA_SLOTS);
    private int contentRevision;
    private long contentHash;

    public ItemTerminalMiniatureState(MiniatureTerminalBlockEntity be, CompoundTag payload) {
        this.be = be;
        // 终端 ID 由方块自己持有（与微缩前同一个）：这里只借用，不重复落一份，避免两处 ID 漂移
        this.terminalId = be.terminalId() == null ? UUID.randomUUID() : be.terminalId();
        this.security = new TerminalSecurity(be::setChanged);
        if (payload.contains(TAG_SECURITY)) {
            this.security.load(payload.getCompound(TAG_SECURITY));
        }
        int bufferModules = payload.getInt(TAG_BUFFER_MODULES);
        this.feeModules = ItemTerminalFee.effectiveModules(payload.getInt(TAG_FEE_MODULES));
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE,
                ModConfig.itemTerminalEnergyBuffer
                        + (long) bufferModules * AkaishiItemTerminalBufferModuleBlock.BONUS_ENERGY);
        this.energy.setEnergy(payload.getLong(TAG_ENERGY));
        loadUnits(payload);
        this.contentHash = computeContentHash();
    }

    private void loadUnits(CompoundTag payload) {
        ListTag list = payload.getList(TAG_UNITS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            // 等阶名脏数据一律退回基础档：单个单元不合法不该拖垮整个方块加载
            ItemStorageUnitTier tier;
            try {
                tier = ItemStorageUnitTier.valueOf(entry.getString(ItemStorageUnitData.TAG_TIER));
            } catch (IllegalArgumentException e) {
                tier = ItemStorageUnitTier.BASIC;
            }
            ItemStorageUnitData unit = new ItemStorageUnitData(tier,
                    AkaishiItemStorageUnitBlockEntity.SLOTS, be::setChanged);
            unit.load(entry.getCompound(ItemStorageUnitData.TAG_UNIT));
            units.add(unit);
        }
    }

    // ===== 存盘 =====

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        CompoundTag sec = new CompoundTag();
        security.save(sec);
        tag.put(TAG_SECURITY, sec);
        tag.putLong(TAG_ENERGY, energy.getEnergyStored());
        tag.putInt(TAG_FEE_MODULES, feeModules);
        // 缓冲扩展份数由容量反推会失真（配置可能被改），故按"容量 - 基准"还原份数
        tag.putInt(TAG_BUFFER_MODULES, bufferModuleCount());
        ListTag list = new ListTag();
        for (ItemStorageUnitData unit : units) {
            CompoundTag entry = new CompoundTag();
            entry.putString(ItemStorageUnitData.TAG_TIER, unit.tier().name());
            // ItemStorageUnitData#save 为就地写入（无返回值）
            CompoundTag unitTag = new CompoundTag();
            unit.save(unitTag);
            entry.put(ItemStorageUnitData.TAG_UNIT, unitTag);
            list.add(entry);
        }
        tag.put(TAG_UNITS, list);
        return tag;
    }

    private int bufferModuleCount() {
        long bonus = energy.getMaxEnergy() - ModConfig.itemTerminalEnergyBuffer;
        return (int) Math.max(0L, bonus / AkaishiItemTerminalBufferModuleBlock.BONUS_ENERGY);
    }

    // ===== 运行 =====

    @Override
    public void tick() {
        long hash = computeContentHash();
        if (hash != contentHash) {
            contentHash = hash;
            contentRevision++;
            be.setChanged();
        }
        pushData();
    }

    private void pushData() {
        data.set(DATA_FORMED, 1);
        LongDataSlots.write(data, DATA_USED_LOW, DATA_USED_HIGH, DATA_USED_HIGH2, DATA_USED_HIGH3, totalStoredIp());
        LongDataSlots.write(data, DATA_CAPACITY_LOW, DATA_CAPACITY_HIGH, DATA_CAPACITY_HIGH2,
                DATA_CAPACITY_HIGH3, totalIpCapacity());
        LongDataSlots.write(data, DATA_BUFFER_LOW, DATA_BUFFER_HIGH, DATA_BUFFER_HIGH2, DATA_BUFFER_HIGH3,
                energy.getEnergyStored());
        data.set(DATA_UNIT_COUNT, units.size());
        LongDataSlots.write(data, DATA_EFFECTIVE_BUFFER_LOW, DATA_EFFECTIVE_BUFFER_HIGH,
                DATA_EFFECTIVE_BUFFER_HIGH2, DATA_EFFECTIVE_BUFFER_HIGH3, energy.getMaxEnergy());
        data.set(DATA_FEE_MODULES, feeModules);
    }

    /** 库内容指纹（口径同物品终端：显式混入堆量，NBT 一并参与） */
    private long computeContentHash() {
        long hash = 1L;
        for (ItemStorageUnitData unit : units) {
            int slots = unit.slots();
            for (int slot = 0; slot < slots; slot++) {
                ItemStack stack = unit.getItem(slot);
                hash = 31L * hash + (stack.isEmpty() ? 0L
                        : 31L * (31L * stack.getItem().hashCode() + stack.getCount())
                                + Objects.hashCode(stack.getTag()));
            }
        }
        return hash;
    }

    private long totalIpCapacity() {
        long total = 0L;
        for (ItemStorageUnitData unit : units) {
            total += unit.getIpCapacity();
        }
        return total;
    }

    private long totalStoredIp() {
        long total = 0L;
        for (ItemStorageUnitData unit : units) {
            total += unit.getStoredIp();
        }
        return total;
    }

    // ===== IItemTerminalHost（库页菜单 / 落账口径） =====

    @Override
    public ContainerData data() {
        return data;
    }

    @Override
    public UUID terminalId() {
        return terminalId;
    }

    @Override
    public TerminalSecurity security() {
        return security;
    }

    /** 微缩件即成型件：没有结构可失效 */
    @Override
    public boolean isFormed() {
        return true;
    }

    @Override
    public int contentRevision() {
        return contentRevision;
    }

    @Override
    public List<IItemStorageUnit> storageUnits() {
        return Collections.unmodifiableList(units);
    }

    @Override
    public long maxBatchIp(boolean deposit) {
        return ItemTerminalFee.maxIp(energy.getMaxEnergy(), deposit, feeModules);
    }

    @Override
    public boolean tryChargeFee(long ip, boolean deposit) {
        if (ip <= 0L) {
            return true;
        }
        long fee = feeCost(ip, deposit);
        if (fee <= 0L) {
            return true;
        }
        if (energy.getEnergyStored() < fee) {
            return false;
        }
        energy.extractEnergy(fee, false);
        be.setChanged();
        return true;
    }

    @Override
    public boolean canAffordFee(long ip, boolean deposit) {
        return ip <= 0L || energy.getEnergyStored() >= feeCost(ip, deposit);
    }

    private long feeCost(long ip, boolean deposit) {
        return deposit ? ItemTerminalFee.depositCost(ip, feeModules) : ItemTerminalFee.withdrawCost(ip, feeModules);
    }

    @Override
    public BlockPos getBlockPos() {
        return be.getBlockPos();
    }

    @Override
    public boolean isRemoved() {
        return be.isRemoved();
    }

    // ===== 物品能力（虚拟槽：输入只收、输出只给，费用闸门与 GUI 存取同口径） =====

    @Override
    public int containerSize() {
        return 2;
    }

    @Override
    public int[] pipeInputSlots() {
        return new int[] {SLOT_IN};
    }

    @Override
    public int[] pipeOutputSlots() {
        return new int[] {SLOT_OUT};
    }

    @Override
    public ItemStack previewSlot(int slot) {
        if (slot != SLOT_OUT) {
            return ItemStack.EMPTY; // 输入槽恒空：本块不存"待收物"，塞入即入库
        }
        for (ItemStorageUnitData unit : units) {
            int slots = unit.slots();
            for (int i = 0; i < slots; i++) {
                ItemStack stored = unit.getItem(i);
                if (!stored.isEmpty()) {
                    return stored.copy();
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canAcceptSlot(int slot, ItemStack stack) {
        if (slot != SLOT_IN || stack.isEmpty()) {
            return false;
        }
        int batch = Math.min(stack.getCount(), ItemPortTransfer.BATCH_LIMIT);
        ItemStack probe = stack.copyWithCount(batch);
        if (ItemPortTransfer.acceptLimit(this, probe, null) < batch) {
            return false; // 空间不足：不能承诺整堆收下
        }
        return canAffordFee(ItemPoints.of(probe), true);
    }

    @Override
    public ItemStack insertSlot(int slot, ItemStack stack) {
        if (slot != SLOT_IN || stack.isEmpty()) {
            return stack;
        }
        return ItemPortTransfer.insertIntoTerminal(this, stack, null, this::chargeDeposit);
    }

    @Override
    public ItemStack removeSlot(int slot, int amount) {
        if (slot != SLOT_OUT || amount <= 0) {
            return ItemStack.EMPTY;
        }
        int want = Math.min(amount, ItemPortTransfer.BATCH_LIMIT);
        for (ItemStorageUnitData unit : units) {
            int slots = unit.slots();
            for (int i = 0; i < slots; i++) {
                ItemStack stored = unit.getItem(i);
                if (stored.isEmpty()) {
                    continue;
                }
                int take = Math.min(want, stored.getCount());
                // 先扣费再取物：不足则整笔拒绝，物品与账本零改动
                if (!chargeWithdraw(unit, i, stored, take)) {
                    return ItemStack.EMPTY;
                }
                return unit.extract(i, take);
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean chargeDeposit(ItemStack batch) {
        long ip = ItemPoints.of(batch);
        return ip <= 0L || tryChargeFee(ip, true);
    }

    private boolean chargeWithdraw(IItemStorageUnit unit, int slot, ItemStack stored, int amount) {
        long ip = TerminalActions.withdrawIp(unit, slot, stored.getCount(), amount);
        return ip <= 0L || tryChargeFee(ip, false);
    }

    // ===== 自身视图 / 能量 =====

    @Override
    public IItemTerminalHost itemHost() {
        return this;
    }

    @Override
    public IEnergyStorage energyStorage() {
        return energy;
    }

    /** 微缩件没有贴装接入口，只能靠能量管道回充，故允许注入 */
    @Override
    public boolean canInputEnergy() {
        return true;
    }
}
