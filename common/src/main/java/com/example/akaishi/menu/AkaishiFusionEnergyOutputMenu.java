package com.example.akaishi.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 聚变能量输出口菜单：无机器槽位，仅玩家背包 + 能量/容量数据展示。
 * 能量与容量为 long，拆 4 个 int 数据槽同步（0/1=能量低/高位，2/3=容量低/高位）。
 */
public class AkaishiFusionEnergyOutputMenu extends AbstractContainerMenu {

    private final ContainerData data;

    public AkaishiFusionEnergyOutputMenu(int id, Inventory playerInv, ContainerData data) {
        super(ModMenus.CHISHI_FUSION_ENERGY_OUTPUT.get(), id);
        this.data = data;

        // 玩家背包 3×9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // 快捷栏 1×9
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
        this.addDataSlots(data);
    }

    /** 缓冲能量（long 由 0/1 低位/高位重组） */
    public long getEnergy() {
        return ((long) data.get(1) << 32) | (data.get(0) & 0xFFFFFFFFL);
    }

    /** 缓冲容量（long 由 2/3 低位/高位重组） */
    public long getMaxEnergy() {
        return ((long) data.get(3) << 32) | (data.get(2) & 0xFFFFFFFFL);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // 无机器槽：仅处理背包行 ↔ 快捷栏
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < 27) {
                if (!this.moveItemStackTo(current, 27, 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(current, 0, 27, false)) {
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

    /** 供无方块实体兜底时使用的空菜单（数据全 0） */
    public static AkaishiFusionEnergyOutputMenu emptyMenu(int id, Inventory inv) {
        return new AkaishiFusionEnergyOutputMenu(id, inv, new SimpleContainerData(4));
    }
}
