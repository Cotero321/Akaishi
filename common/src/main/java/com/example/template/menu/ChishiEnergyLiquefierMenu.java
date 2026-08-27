package com.example.template.menu;

import com.example.template.block.entity.ChishiEnergyLiquefierBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 能量液化装置菜单：1 个输入槽（下界之星/凋零玫瑰/各混合物）+ 赤能源/输出液体/进度数据。
 * 数据槽：0/1=赤能量/赤容量 2/3=输出液体量/容量 4=液化进度百分比。
 */
public class ChishiEnergyLiquefierMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public ChishiEnergyLiquefierMenu(int id, Inventory inv, ChishiEnergyLiquefierBlockEntity be) {
        this(id, inv, be.inventory(), be.data());
    }

    public ChishiEnergyLiquefierMenu(int id, Inventory inv, Container container, ContainerData data) {
        super(ModMenus.CHISHI_ENERGY_LIQUEFIER.get(), id);
        this.container = container;
        this.data = data;

        // 材料输入槽：只进不出（位置对齐 GUI 纹理中的槽位图形）
        addSlot(new Slot(container, ChishiEnergyLiquefierBlockEntity.INPUT_SLOT, 116, 30));
        // 生命能量固态物输入槽（末地/幽匿/巨龙燃料液化消耗）
        addSlot(new Slot(container, ChishiEnergyLiquefierBlockEntity.SOLID_SLOT, 62, 35));

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

    public long getChishiEnergy() {
        return data.get(ChishiEnergyLiquefierBlockEntity.DATA_CHISHI_ENERGY);
    }

    public long getChishiMax() {
        return data.get(ChishiEnergyLiquefierBlockEntity.DATA_CHISHI_CAPACITY);
    }

    public long getFluidAmount() {
        return data.get(ChishiEnergyLiquefierBlockEntity.DATA_FLUID_AMOUNT);
    }

    public long getFluidMax() {
        return data.get(ChishiEnergyLiquefierBlockEntity.DATA_FLUID_CAPACITY);
    }

    /** 液化进度（0-100） */
    public int getProgress() {
        return data.get(ChishiEnergyLiquefierBlockEntity.DATA_PROGRESS);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < ChishiEnergyLiquefierBlockEntity.SLOT_COUNT) {
                if (!this.moveItemStackTo(current, ChishiEnergyLiquefierBlockEntity.SLOT_COUNT,
                        ChishiEnergyLiquefierBlockEntity.SLOT_COUNT + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包物品可放入输入槽（下界之星 / 凋零玫瑰）
                if (!this.moveItemStackTo(current, 0, ChishiEnergyLiquefierBlockEntity.SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
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
