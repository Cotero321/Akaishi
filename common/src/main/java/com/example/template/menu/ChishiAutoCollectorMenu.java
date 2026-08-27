package com.example.template.menu;

import com.example.template.block.entity.ChishiAutoCollectorBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 自动收集器菜单：27 槽存储（9×3）+ 玩家背包。
 * 通过 ContainerData 将能量与收集进度同步给客户端 GUI。
 */
public class ChishiAutoCollectorMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public ChishiAutoCollectorMenu(int id, Inventory inv, ChishiAutoCollectorBlockEntity be) {
        this(id, inv, be.inventory(), be.data());
    }

    public ChishiAutoCollectorMenu(int id, Inventory inv, Container container, ContainerData data) {
        super(ModMenus.CHISHI_AUTO_COLLECTOR.get(), id);
        this.container = container;
        this.data = data;

        // 存储槽 9×3
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(container, col + row * 9, 8 + col * 18, 17 + row * 18));
            }
        }
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

    /** 当前赤石能量（GUI 能量条用） */
    public int getEnergy() {
        return data.get(0);
    }

    /** 能量容量（GUI 能量条分母） */
    public int getEnergyCapacity() {
        return data.get(1);
    }

    /** 当前收集进度百分比（GUI 进度条用） */
    public int getProgress() {
        return data.get(2);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            int machineSlots = ChishiAutoCollectorBlockEntity.STORAGE_SIZE;
            if (index < machineSlots) {
                // 存储槽 → 玩家背包
                if (!this.moveItemStackTo(current, machineSlots, machineSlots + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包 → 存储槽
                if (!this.moveItemStackTo(current, 0, machineSlots, false)) {
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
