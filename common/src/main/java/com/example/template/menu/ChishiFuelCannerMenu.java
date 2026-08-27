package com.example.template.menu;

import com.example.template.block.entity.ChishiFuelCannerBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 燃料装罐机菜单：1 个空罐输入槽 + 1 个满罐输出槽 + 输入液体量数据。
 * 数据槽：0/1=输入液体量/容量。
 */
public class ChishiFuelCannerMenu extends AbstractContainerMenu {

    private final Container container;
    private final net.minecraft.world.inventory.ContainerData data;

    public ChishiFuelCannerMenu(int id, Inventory inv, ChishiFuelCannerBlockEntity be) {
        this(id, inv, be.inventory(), be.data());
    }

    public ChishiFuelCannerMenu(int id, Inventory inv, Container container, net.minecraft.world.inventory.ContainerData data) {
        super(ModMenus.CHISHI_FUEL_CANNER.get(), id);
        this.container = container;
        this.data = data;

        // 空/半满燃料罐输入槽（位置对齐 GUI 纹理槽位图形）
        addSlot(new Slot(container, ChishiFuelCannerBlockEntity.INPUT_SLOT, 62, 35));
        // 满燃料罐输出槽
        addSlot(new Slot(container, ChishiFuelCannerBlockEntity.OUTPUT_SLOT, 116, 35));

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

    public long getFluidAmount() {
        return data.get(ChishiFuelCannerBlockEntity.DATA_FLUID_AMOUNT);
    }

    public long getFluidMax() {
        return data.get(ChishiFuelCannerBlockEntity.DATA_FLUID_CAPACITY);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            stack = current.copy();
            if (index < ChishiFuelCannerBlockEntity.SLOT_COUNT) {
                // 机器槽 → 玩家背包
                if (!this.moveItemStackTo(current, ChishiFuelCannerBlockEntity.SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(current, 0, ChishiFuelCannerBlockEntity.SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
            if (current.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (current.getCount() == stack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, current);
        }
        return stack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}
