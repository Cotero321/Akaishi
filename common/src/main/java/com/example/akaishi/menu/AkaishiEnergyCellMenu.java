package com.example.akaishi.menu;

import com.example.akaishi.item.AkaishiPortableEnergyCell;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 赤能源储存单元菜单：0 号槽为便携单元充能槽，仅展示能量。
 * 能量与容量为 long，拆成 4 个 int 数据槽同步（0/1=能量低/高位，2/3=容量低/高位），
 * 便携单元同样拆 4 个（4/5=能量低/高位，6/7=容量低/高位）。
 * 玩家背包槽位（3 行 + 快捷栏）供常规交互。
 */
public class AkaishiEnergyCellMenu extends AbstractContainerMenu {

    private final ContainerData data;
    /** 便携单元充能槽容器 */
    private final Container cellSlot;

    public AkaishiEnergyCellMenu(int id, Inventory playerInv, Container cellSlot, ContainerData data) {
        super(ModMenus.CHISHI_ENERGY_CELL.get(), id);
        this.data = data;
        this.cellSlot = cellSlot;

        // 便携单元充能槽（只允许放入便捷赤能源储存单元）
        this.addSlot(new Slot(cellSlot, 0, 150, 44) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AkaishiPortableEnergyCell.isPortableCell(stack);
            }
        });

        // 玩家背包 3 行 × 9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // 快捷栏
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
        this.addDataSlots(data);
    }

    /** 当前赤能源储量（long 由 4 个 int 槽重组：0/1 低位/高位） */
    public long getEnergy() {
        return ((long) data.get(1) << 32) | (data.get(0) & 0xFFFFFFFFL);
    }

    /** 容量上限（long 由 4 个 int 槽重组：2/3 低位/高位） */
    public long getMaxEnergy() {
        return ((long) data.get(3) << 32) | (data.get(2) & 0xFFFFFFFFL);
    }

    /** 便携单元当前能量（4/5 低位/高位） */
    public long getCellEnergy() {
        return ((long) data.get(5) << 32) | (data.get(4) & 0xFFFFFFFFL);
    }

    /** 便携单元容量（6/7 低位/高位） */
    public long getCellMaxEnergy() {
        return ((long) data.get(7) << 32) | (data.get(6) & 0xFFFFFFFFL);
    }

    /** 充能槽是否有便携单元 */
    public boolean hasCell() {
        return !cellSlot.getItem(0).isEmpty();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if (index == 0) {
                // 充能槽 → 背包
                if (!this.moveItemStackTo(stack, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (AkaishiPortableEnergyCell.isPortableCell(stack)) {
                // 背包中的便携单元 → 充能槽
                if (!this.moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 普通物品：背包行 ↔ 快捷栏
                if (index < 28) {
                    if (!this.moveItemStackTo(stack, 28, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(stack, 1, 28, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return itemstack;
    }

    /** 供无方块实体兜底时使用的空菜单（数据全 0） */
    public static AkaishiEnergyCellMenu emptyMenu(int id, Inventory inv) {
        return new AkaishiEnergyCellMenu(id, inv, new SimpleContainer(1), new net.minecraft.world.inventory.SimpleContainerData(8));
    }
}
