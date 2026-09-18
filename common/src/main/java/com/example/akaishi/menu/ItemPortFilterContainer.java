package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiItemPortBlockEntity;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/**
 * 过滤网 9 格 {@link Container} 适配（AE2 总线配置槽口径）。
 * <p>
 * <b>服务端</b>：直读写 {@link AkaishiItemPortBlockEntity} 的过滤数组 —— 过滤只有这一份数据源，
 * 槽里放的物品就是 {@code matches()} 判定用的那份物品（放进去能取回，不凭空消耗/复制）。
 * <b>客户端</b>：方块实体缺失时用同长镜像数组承接原版槽同步下发的副本（不再有自定义过滤快照包）。
 * <p>
 * 槽位恒单件（{@link #getMaxStackSize()} = 1）：一堆物品进不了配置槽，也不会自动摊到其它格子。
 */
final class ItemPortFilterContainer implements Container {

    /** 方块实体（服务端 / 客户端同区块也有实例）；null = 无实体兜底，内容落在 {@link #mirror} */
    private final AkaishiItemPortBlockEntity host;
    /** 客户端镜像（host == null 时使用） */
    private final ItemStack[] mirror = new ItemStack[AkaishiItemPortBlockEntity.FILTER_SLOTS];

    ItemPortFilterContainer(AkaishiItemPortBlockEntity host) {
        this.host = host;
        Arrays.fill(mirror, ItemStack.EMPTY);
    }

    @Override
    public int getContainerSize() {
        return AkaishiItemPortBlockEntity.FILTER_SLOTS;
    }

    /** 配置槽恒单件：原版放置/交换都按 1 个走（避免一堆叠进过滤槽） */
    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            if (!getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return host != null ? host.filterSlot(slot) : mirror[slot];
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack current = getItem(slot);
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        // 恒单件：一次只取回 1 个（原版按 getMaxStackSize() 取，这里再夹一次，杜绝越取）
        ItemStack taken = current.copyWithCount(1);
        setItem(slot, ItemStack.EMPTY);
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeItem(slot, 1);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= getContainerSize()) {
            return;
        }
        if (host != null) {
            host.setFilterSlot(slot, stack);
            return;
        }
        mirror[slot] = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    @Override
    public void setChanged() {
        if (host != null) {
            host.setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            setItem(slot, ItemStack.EMPTY);
        }
    }
}
