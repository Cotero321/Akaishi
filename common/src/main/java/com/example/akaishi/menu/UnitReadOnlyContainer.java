package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiItemStorageUnitBlockEntity;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 储存单元只读视图容器：把单元的 54 槽映射成菜单槽位，借原版槽同步把内容下发到客户端。
 * <p>
 * <b>为什么需要它</b>：单元方块实体不覆写 {@code getUpdateTag}，客户端那一份是空容器，
 * 因此只读界面必须由服务端下发槽内容 —— 走原版槽同步比再加一条 S2C 通道更省。
 * <p>
 * <b>为什么不是直接拿单元容器的引用</b>：单元内容只能由物品终端经 {@code insert/extract/setItemAt}
 * 三写入口改动（§2.5 防线），若把容器本体交给菜单，任何 {@code Slot#set} 都会绕过 {@code slotIp} 账本。
 * 本类在<b>服务端丢弃一切写入</b>，只在客户端写本地镜像，从接口面排除旁路。
 * <p>
 * <b>客户端为什么返回投影而不是镜像</b>：54 个物理槽、每槽上限 64，1728 件钻石会摊成 27 格，
 * 原样显示等于同一种物品刷一屏。故客户端把镜像投影成「同物品同 NBT 归一格 + 累计件数」的合并视图，
 * 直接由本类交原版槽渲染 —— 不再由界面自绘，避免两层绘制叠在一起。
 * 服务端始终返回真实内容，槽同步路径完全不受投影影响。
 */
public class UnitReadOnlyContainer implements Container {

    /** 单元槽位数（只读视图与单元账本严格同长） */
    public static final int SIZE = AkaishiItemStorageUnitBlockEntity.SLOTS;

    private final AkaishiItemStorageUnitBlockEntity unit;
    /** 是否读穿单元本体：仅服务端成立；客户端与方块实体缺失时退化为本地镜像 */
    private final boolean readThrough;
    /** 客户端镜像（承接 {@code initializeContents} 下发的槽内容，是投影的数据源） */
    private final NonNullList<ItemStack> mirror;

    /** 客户端投影：合并后的可视条目（恒 count=1，保留 NBT），服务端不使用 */
    private final NonNullList<ItemStack> projection = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final long[] projectionTotals = new long[SIZE];
    private int projectionSize;
    private long projectionItemTotal;
    /** 镜像是否已变化：变化才重算投影（同步每 tick 都会写槽，不做无谓重算） */
    private boolean projectionDirty = true;

    public UnitReadOnlyContainer(AkaishiItemStorageUnitBlockEntity unit, boolean serverSide) {
        this.unit = unit;
        this.readThrough = serverSide && unit != null;
        this.mirror = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < SIZE; i++) {
            if (!getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= SIZE) {
            return ItemStack.EMPTY;
        }
        if (this.readThrough) {
            return this.unit.getItem(slot);
        }
        ensureProjection();
        return this.projection.get(slot);
    }

    /** 只读：服务端丢弃写入；客户端写入镜像并标记投影待重算 */
    @Override
    public void setItem(int slot, ItemStack stack) {
        if (this.readThrough || slot < 0 || slot >= SIZE) {
            return;
        }
        this.mirror.set(slot, stack);
        this.projectionDirty = true;
    }

    /** 只读视图不产生脏数据，不触发方块实体存档 */
    @Override
    public void setChanged() {
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void clearContent() {
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /** 合并后的可视条目数（仅客户端界面调用） */
    public int viewSize() {
        ensureProjection();
        return this.projectionSize;
    }

    /** 第 cell 格的合并总件数（仅客户端界面调用） */
    public long viewTotal(int cell) {
        ensureProjection();
        return cell >= 0 && cell < SIZE ? this.projectionTotals[cell] : 0L;
    }

    /** 合并后的总件数（仅客户端界面调用） */
    public long viewItemTotal() {
        ensureProjection();
        return this.projectionItemTotal;
    }

    /**
     * 按类合并：同物品且同 NBT 归为一格并累计件数（与物品终端聚合口径一致）。
     * <p>
     * 54 槽规模下 O(n²) 比较（{@code isSameItemSameTags} 只比 item 与 tag、无对象分配）代价可忽略，
     * 换来的好处是不必维护「内容指纹 → 缓存」这层可能失效的状态。
     */
    private void ensureProjection() {
        if (!this.projectionDirty) {
            return;
        }
        this.projectionDirty = false;
        int size = 0;
        long itemTotal = 0L;
        for (int i = 0; i < SIZE; i++) {
            ItemStack stack = this.mirror.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            itemTotal += stack.getCount();
            int hit = -1;
            for (int k = 0; k < size; k++) {
                if (ItemStack.isSameItemSameTags(this.projection.get(k), stack)) {
                    hit = k;
                    break;
                }
            }
            if (hit < 0) {
                this.projection.set(size, stack.copyWithCount(1));
                this.projectionTotals[size] = stack.getCount();
                size++;
            } else {
                this.projectionTotals[hit] += stack.getCount();
            }
        }
        // 清掉上一次投影的残留格，避免条目变少后旧图标仍被渲染
        for (int i = size; i < SIZE; i++) {
            this.projection.set(i, ItemStack.EMPTY);
            this.projectionTotals[i] = 0L;
        }
        this.projectionSize = size;
        this.projectionItemTotal = itemTotal;
    }
}
