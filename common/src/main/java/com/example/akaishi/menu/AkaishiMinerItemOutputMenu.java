package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiMinerItemOutputBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 矿机物品输出口菜单：产物缓冲 27 格（9×3，只读供管道/漏斗抽取）+ 玩家背包。
 * 数据槽：0/1=能量(占位恒 0) 2=成型状态。
 */
public class AkaishiMinerItemOutputMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public AkaishiMinerItemOutputMenu(int id, Inventory inv, AkaishiMinerItemOutputBlockEntity be) {
        this(id, inv, be.buffer(), be.data());
    }

    public AkaishiMinerItemOutputMenu(int id, Inventory inv, Container container, ContainerData data) {
        super(ModMenus.CHISHI_MINER_ITEM_OUTPUT.get(), id);
        this.container = container;
        this.data = data;

        // 产物缓冲 9×3（只读）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new OutputSlot(container, row * 9 + col, 8 + col * 18, 40 + row * 18));
            }
        }
        // 玩家背包 3×9 + 快捷栏 1×9（适配 176×198 界面）
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

    /** 是否已与成型矿机连接 */
    public boolean isLinked() {
        return data.get(2) == 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            int machineEnd = AkaishiMinerItemOutputBlockEntity.BUFFER_SLOTS;
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
}
