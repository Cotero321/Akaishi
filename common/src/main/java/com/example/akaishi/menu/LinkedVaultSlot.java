package com.example.akaishi.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 联动存储槽：映射"联动存储容器"的分页槽位（page × PAGE_SLOTS + indexInPage）。
 * 仅当机器界面存储面板打开时激活（isActive），否则隐藏且不可交互。
 * 放置/取用直连存储容器，由存储库自身的 canPlaceItem/tick 逻辑负责归类与合并。
 */
public class LinkedVaultSlot extends Slot {

    private final StorageLinkState state;
    private final int indexInPage;

    public LinkedVaultSlot(StorageLinkState state, Container storage, int indexInPage, int x, int y) {
        super(storage, indexInPage, x, y);
        this.state = state;
        this.indexInPage = indexInPage;
    }

    private int actualIndex() {
        return state.page * StorageLink.PAGE_SLOTS + indexInPage;
    }

    @Override
    public ItemStack getItem() {
        Container storage = state.storage;
        if (storage == null || actualIndex() >= storage.getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return storage.getItem(actualIndex());
    }

    @Override
    public void set(ItemStack stack) {
        Container storage = state.storage;
        if (storage != null && actualIndex() < storage.getContainerSize()) {
            storage.setItem(actualIndex(), stack);
        }
        setChanged();
    }

    @Override
    public ItemStack remove(int amount) {
        ItemStack current = getItem();
        ItemStack split = current.split(amount);
        set(current);
        return split;
    }

    @Override
    public boolean isActive() {
        return state.open && state.storage != null;
    }

    /** 联动区自由放置（归类交给存储库自身机制） */
    @Override
    public boolean mayPlace(ItemStack stack) {
        return true;
    }
}
