package com.example.template.menu;

import com.example.template.block.entity.ChishiItemReconstructorBlockEntity;
import com.example.template.item.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 物品重构仪菜单：3 机器槽（0=原料，1=衰竭结晶代价，2=产物）+ 玩家背包 + 5 数据槽。
 * 结晶槽仅接纳衰竭结晶（mayPlace 限制）；原料/产物槽通用。
 */
public class ChishiItemReconstructorMenu extends AbstractContainerMenu {

    private final ContainerData data;
    private final Container inventory;

    public ChishiItemReconstructorMenu(int id, Inventory inv, ChishiItemReconstructorBlockEntity be) {
        this(id, inv, be.inventory(), be.data());
    }

    public ChishiItemReconstructorMenu(int id, Inventory inv, Container inventory, ContainerData data) {
        super(ModMenus.CHISHI_ITEM_RECONSTRUCTOR.get(), id);
        this.data = data;
        this.inventory = inventory;

        addSlot(new Slot(inventory, 0, 26, 40));
        addSlot(new Slot(inventory, 1, 62, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.exhaustedCrystal.get());
            }
        });
        addSlot(new Slot(inventory, 2, 98, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // 产物槽只读：防止放入杂物卡死机器（canFitOutput 永远 false）
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 124 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 180));
        }
        addDataSlots(data);
    }

    public long getEnergy() {
        return data.get(ChishiItemReconstructorBlockEntity.DATA_ENERGY);
    }

    public long getEnergyCapacity() {
        return data.get(ChishiItemReconstructorBlockEntity.DATA_ENERGY_CAPACITY);
    }

    public long getProgress() {
        return data.get(ChishiItemReconstructorBlockEntity.DATA_PROGRESS);
    }

    /** 当前配方所需结晶总数（无配方为 0） */
    public long getRequired() {
        return data.get(ChishiItemReconstructorBlockEntity.DATA_REQUIRED);
    }

    public long getCrystals() {
        return data.get(ChishiItemReconstructorBlockEntity.DATA_CRYSTALS);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            stack = current.copy();
            if (index < 3) {
                // 机器槽 → 玩家背包（3..29）→ 快捷栏（30..38）
                if (!this.moveItemStackTo(current, 3, 30, true) && !this.moveItemStackTo(current, 30, 39, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包/快捷栏：结晶 → 结晶槽（1），其余 → 原料槽（0），再背包内移动
                if (current.is(ModItems.exhaustedCrystal.get()) && !this.moveItemStackTo(current, 1, 2, false)
                        || !this.moveItemStackTo(current, 0, 1, false)
                        || !this.moveItemStackTo(current, 30, 39, false)
                        || !this.moveItemStackTo(current, 3, 30, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (current.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return stack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    public ContainerData data() {
        return data;
    }
}
