package com.example.template.menu;

import com.example.template.block.entity.ChishiEnergyGeneratorBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 赤能源发生机菜单：1 个燃料槽 + 玩家背包槽。
 * 通过 ContainerData 将能量与燃烧状态同步给客户端 GUI。
 */
public class ChishiEnergyGeneratorMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public ChishiEnergyGeneratorMenu(int id, Inventory inv, ChishiEnergyGeneratorBlockEntity be) {
        this(id, inv, be.inventory(), be.data());
    }

    public ChishiEnergyGeneratorMenu(int id, Inventory inv, Container container, ContainerData data) {
        super(ModMenus.CHISHI_ENERGY_GENERATOR.get(), id);
        this.container = container;
        this.data = data;

        // 燃料槽
        addSlot(new Slot(container, ChishiEnergyGeneratorBlockEntity.FUEL_SLOT, 56, 17));

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

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < ChishiEnergyGeneratorBlockEntity.SLOT_COUNT) {
                // 方块槽 → 玩家背包（总槽数 = 方块槽 + 36）
                if (!this.moveItemStackTo(current, ChishiEnergyGeneratorBlockEntity.SLOT_COUNT, ChishiEnergyGeneratorBlockEntity.SLOT_COUNT + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(current, 0, ChishiEnergyGeneratorBlockEntity.SLOT_COUNT, false)) {
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
