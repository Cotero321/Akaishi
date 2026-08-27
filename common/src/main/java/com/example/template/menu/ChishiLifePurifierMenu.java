package com.example.template.menu;

import com.example.template.block.entity.ChishiLifePurifierBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 生命能量提纯器菜单：1 个输出槽（生命能量固态物，只出不进）+ 双能量/进度数据。
 * 数据槽：0/1=赤能量/赤容量 2/3=生命能量/生命容量 4=固化进度百分比。
 */
public class ChishiLifePurifierMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public ChishiLifePurifierMenu(int id, Inventory inv, ChishiLifePurifierBlockEntity be) {
        this(id, inv, be.inventory(), be.data());
    }

    public ChishiLifePurifierMenu(int id, Inventory inv, Container container, ContainerData data) {
        super(ModMenus.CHISHI_LIFE_PURIFIER.get(), id);
        this.container = container;
        this.data = data;

        // 输出槽：只出不进
        addSlot(new Slot(container, ChishiLifePurifierBlockEntity.OUTPUT_SLOT, 116, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    /** 当前赤能源储量 */
    public long getChishiEnergy() {
        return data.get(0);
    }

    /** 赤能源容量 */
    public long getChishiMax() {
        return data.get(1);
    }

    /** 当前生命能量储量 */
    public long getLifeEnergy() {
        return data.get(2);
    }

    /** 生命能量容量 */
    public long getLifeMax() {
        return data.get(3);
    }

    /** 固化进度（0-100） */
    public int getProgress() {
        return data.get(ChishiLifePurifierBlockEntity.DATA_PROGRESS);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < ChishiLifePurifierBlockEntity.SLOT_COUNT) {
                if (!this.moveItemStackTo(current, ChishiLifePurifierBlockEntity.SLOT_COUNT,
                        ChishiLifePurifierBlockEntity.SLOT_COUNT + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包物品无法放入本机（仅输出槽，禁止放入）
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
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
