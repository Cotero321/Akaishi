package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiMinerPortBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 矿机转口菜单：产物缓冲 27 格（9×3，只读供管道/漏斗抽取）+ 玩家背包。
 * 数据槽：0=能量 1=容量 2=成型状态。
 */
public class AkaishiMinerPortMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public AkaishiMinerPortMenu(int id, Inventory inv, AkaishiMinerPortBlockEntity be) {
        this(id, inv, be.buffer(), be.data());
    }

    public AkaishiMinerPortMenu(int id, Inventory inv, Container container, ContainerData data) {
        super(ModMenus.CHISHI_MINER_PORT.get(), id);
        this.container = container;
        this.data = data;

        // 产物缓冲 9×3
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new OutputSlot(container, row * 9 + col, 8 + col * 18, 40 + row * 18));
            }
        }

        // 玩家背包 3×9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 124 + row * 18));
            }
        }
        // 快捷栏 1×9
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 180));
        }

        addDataSlots(data);
    }

    /** 只读产物槽（仅控制器可写入） */
    private static class OutputSlot extends Slot {
        OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    public int getEnergy() {
        return data.get(AkaishiMinerPortBlockEntity.DATA_ENERGY);
    }

    public int getCapacity() {
        return data.get(AkaishiMinerPortBlockEntity.DATA_CAPACITY);
    }

    /** 是否已与成型矿机连接 */
    public boolean isLinked() {
        return data.get(AkaishiMinerPortBlockEntity.DATA_FORMED) == 1;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            int machineEnd = AkaishiMinerPortBlockEntity.BUFFER_SLOTS;
            if (index < machineEnd) {
                if (!this.moveItemStackTo(current, machineEnd, machineEnd + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(current, 0, machineEnd, false)) {
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
