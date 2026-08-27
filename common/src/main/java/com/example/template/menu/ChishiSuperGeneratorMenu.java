package com.example.template.menu;

import com.example.template.block.entity.ChishiSuperGeneratorCoreBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 超级发生器架构核心菜单：1 个燃料槽 + 玩家背包槽。
 * 数据槽：0=能量，1=燃烧能量，2=燃料总能量，3=结构状态（0/1）。
 */
public class ChishiSuperGeneratorMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public ChishiSuperGeneratorMenu(int id, Inventory inv, ChishiSuperGeneratorCoreBlockEntity be) {
        this(id, inv, be.inventory(), be.data());
    }

    public ChishiSuperGeneratorMenu(int id, Inventory inv, Container container, ContainerData data) {
        super(ModMenus.CHISHI_SUPER_GENERATOR_CORE.get(), id);
        this.container = container;
        this.data = data;

        // 燃料槽
        addSlot(new Slot(container, ChishiSuperGeneratorCoreBlockEntity.FUEL_SLOT, 25, 42));

        // 玩家背包 3×9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // 快捷栏 1×9
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }

        addDataSlots(data);
    }

    public int getEnergy() {
        return data.get(0);
    }

    public int getBurnTime() {
        return data.get(1);
    }

    public int getBurnTimeTotal() {
        return data.get(2);
    }

    /** 结构是否完整激活 */
    public boolean isFormed() {
        return data.get(3) == 1;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < ChishiSuperGeneratorCoreBlockEntity.SLOT_COUNT) {
                if (!this.moveItemStackTo(current, ChishiSuperGeneratorCoreBlockEntity.SLOT_COUNT,
                        ChishiSuperGeneratorCoreBlockEntity.SLOT_COUNT + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(current, 0, ChishiSuperGeneratorCoreBlockEntity.SLOT_COUNT, false)) {
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
