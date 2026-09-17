package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiItemStorageUnitBlockEntity;
import com.example.akaishi.util.LongDataSlots;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 物品储存单元菜单（D18 只读视图）：54 槽全展示 + 占用 / 容量统计，无任何写入路径。
 * <p>
 * 单元是<b>被动存储</b>（不实现物流接口、不暴露容器本体），物品进出只能由物品终端驱动，
 * 因此本界面刻意只读：槽位由 {@link UnitViewSlot} + {@link UnitReadOnlyContainer} 双重挡写，
 * 玩家在这里既不能放也不能取，杜绝绕过 {@code slotIp} 账本的旁路（§2.5 防线）。
 * <p>
 * 统计值走数据槽同步：单元没有 ticker，故由打开中的菜单每 tick 刷新一次再交原版下发。
 */
public class AkaishiItemStorageUnitMenu extends AbstractContainerMenu {

    // ===== 版式几何（Menu 与 Screen 共用） =====

    public static final int COLUMNS = 9;
    /** 行数由槽位总数推导（54 槽 = 6 行，与单页大箱口径一致） */
    public static final int ROWS = UnitReadOnlyContainer.SIZE / COLUMNS;
    public static final int PANEL_W = 176;
    public static final int PANEL_H = 230;
    public static final int SLOT_X = 8;
    public static final int SLOT_Y = 29;
    public static final int SLOT_STEP = 18;
    public static final int INV_TOP = 148;
    public static final int HOTBAR_Y = 206;

    private static final int PLAYER_SLOTS = 36;
    private static final int MAIN_INV_SLOTS = 27;
    /** 槽位区间：单元槽 [0, UNIT_END)，主背包 [UNIT_END, MAIN_INV_END)，快捷栏 [MAIN_INV_END, HOTBAR_END) */
    private static final int UNIT_END = UnitReadOnlyContainer.SIZE;
    private static final int MAIN_INV_END = UNIT_END + MAIN_INV_SLOTS;
    private static final int HOTBAR_END = UNIT_END + PLAYER_SLOTS;
    private static final double MAX_DISTANCE_SQR = 64.0D;

    // ===== 数据槽（long 拆 4 槽） =====

    public static final int DATA_USED_LOW = 0;
    public static final int DATA_USED_HIGH = 1;
    public static final int DATA_USED_HIGH2 = 2;
    public static final int DATA_USED_HIGH3 = 3;
    public static final int DATA_CAPACITY_LOW = 4;
    public static final int DATA_CAPACITY_HIGH = 5;
    public static final int DATA_CAPACITY_HIGH2 = 6;
    public static final int DATA_CAPACITY_HIGH3 = 7;
    public static final int DATA_SLOTS = 8;

    private final AkaishiItemStorageUnitBlockEntity unit;
    private final ContainerData data = new SimpleContainerData(DATA_SLOTS);
    private final Player player;
    /** 只读视图容器：客户端界面按类合并视图的数据源（{@code viewSize/viewTotal} 只在客户端有意义） */
    private final UnitReadOnlyContainer view;
    /**
     * 「原版槽渲染这一趟」标记。
     * <p>
     * 1.20.1 的 {@code AbstractContainerScreen.renderSlot} 只在<b>空槽位</b>才走 {@code getNoItemIcon}
     * 分支，槽里有物品时照样 {@code renderItem}，且这批几何到帧末最后一次 flush 才落屏 ——
     * 会把界面自绘的合并总量盖住。故该趟让视图槽对原版伪装成空槽，图标与总量全部自绘。
     */
    private boolean vanillaRenderPass;

    public AkaishiItemStorageUnitMenu(int id, Inventory inv, AkaishiItemStorageUnitBlockEntity unit) {
        super(ModMenus.CHISHI_ITEM_STORAGE_UNIT.get(), id);
        this.unit = unit;
        this.player = inv.player;
        // 服务端读穿单元本体，客户端用本地镜像承接下发的槽内容（并在客户端投影成按类合并视图）
        UnitReadOnlyContainer view = new UnitReadOnlyContainer(unit, !inv.player.level().isClientSide());
        this.view = view;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int index = row * COLUMNS + col;
                addSlot(new UnitViewSlot(this, view, index, SLOT_X + col * SLOT_STEP, SLOT_Y + row * SLOT_STEP));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                addSlot(new Slot(inv, col + row * COLUMNS + 9, SLOT_X + col * SLOT_STEP, INV_TOP + row * SLOT_STEP));
            }
        }
        for (int col = 0; col < COLUMNS; col++) {
            addSlot(new Slot(inv, col, SLOT_X + col * SLOT_STEP, HOTBAR_Y));
        }
        addDataSlots(this.data);
    }

    @Override
    public void broadcastChanges() {
        // 单元无 ticker，故由打开中的菜单刷新统计值；必须先写再交原版比对，否则本 tick 不会下发
        if (this.player instanceof ServerPlayer && this.unit != null) {
            LongDataSlots.write(this.data, DATA_USED_LOW, DATA_USED_HIGH, DATA_USED_HIGH2, DATA_USED_HIGH3,
                    this.unit.getStoredIp());
            LongDataSlots.write(this.data, DATA_CAPACITY_LOW, DATA_CAPACITY_HIGH, DATA_CAPACITY_HIGH2,
                    DATA_CAPACITY_HIGH3, this.unit.getIpCapacity());
        }
        super.broadcastChanges();
    }

    /** 已占用 IP（服务端权威值） */
    public long usedIp() {
        return LongDataSlots.read(this.data, DATA_USED_LOW, DATA_USED_HIGH, DATA_USED_HIGH2, DATA_USED_HIGH3);
    }

    /** 总容量 IP */
    public long capacityIp() {
        return LongDataSlots.read(this.data, DATA_CAPACITY_LOW, DATA_CAPACITY_HIGH, DATA_CAPACITY_HIGH2,
                DATA_CAPACITY_HIGH3);
    }

    /** 剩余可用 IP（用户口径：容量还剩多少） */
    public long remainingIp() {
        return Math.max(0L, capacityIp() - usedIp());
    }

    /** 按类合并后的可视条目数（客户端）：一种物品占一格 */
    public int viewSize() {
        return this.view.viewSize();
    }

    /** 是否处于「原版槽渲染这一趟」（仅该趟内视图槽返回空堆，让原版不画物品） */
    public boolean vanillaRenderPass() {
        return this.vanillaRenderPass;
    }

    /** 由界面在 {@code super.render} 前后包夹设置（务必 try/finally） */
    public void setVanillaRenderPass(boolean value) {
        this.vanillaRenderPass = value;
    }

    /** 第 cell 格的合并总件数（客户端） */
    public long viewTotal(int cell) {
        return this.view.viewTotal(cell);
    }

    /** 合并后的总件数（客户端） */
    public long viewItemTotal() {
        return this.view.viewItemTotal();
    }

    /**
     * 快速移动：<b>只允许玩家背包内部互转</b>。
     * <p>
     * 单元槽必须直接拒绝 —— 只读槽取不走内容，而原版 {@code moveItemStackTo} 会先把物品放进背包
     * 再把源堆清空，源槽是只读的话清空无效，结果就是<b>凭空复制</b>。同理客户端不执行本方法，
     * 一切以服务端为准。
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (player.level().isClientSide() || index < UNIT_END || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack current = slot.getItem();
        ItemStack result = current.copy();
        boolean moved = index < MAIN_INV_END
                ? this.moveItemStackTo(current, MAIN_INV_END, HOTBAR_END, false)
                : this.moveItemStackTo(current, UNIT_END, MAIN_INV_END, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (current.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (current.getCount() == result.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, current);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.unit != null && !this.unit.isRemoved()
                && player.distanceToSqr(this.unit.getBlockPos().getCenter()) <= MAX_DISTANCE_SQR;
    }
}
