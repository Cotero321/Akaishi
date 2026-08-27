package com.example.template.menu;

import com.example.template.block.entity.ChishiEnergyAssemblyBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 小型赤能源组合结构菜单：1 个燃料槽 + 玩家背包槽。
 * 数据槽：0=能量，1=燃烧时间，2=燃烧总时间，3=结构状态（0/1）。
 */
public class ChishiEnergyAssemblyMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public ChishiEnergyAssemblyMenu(int id, Inventory inv, ChishiEnergyAssemblyBlockEntity be) {
        this(id, inv, be, be.data());
    }

    public ChishiEnergyAssemblyMenu(int id, Inventory inv, Container container, ContainerData data) {
        super(ModMenus.CHISHI_ENERGY_ASSEMBLY.get(), id);
        this.container = container;
        this.data = data;

        // 燃料槽
        addSlot(new Slot(container, ChishiEnergyAssemblyBlockEntity.FUEL_SLOT, 25, 42));

        // 能源产生升级组件装配槽 5×2（最多 10 个）
        int[] cols = {8, 26, 44, 62, 80};
        int[] rows = {58, 68};
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < cols.length; c++) {
                addSlot(new SpeedUpgradeSlot(container, ChishiEnergyAssemblyBlockEntity.UPGRADE_SLOT_START + r * cols.length + c,
                        cols[c], rows[r]));
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

    /** 已装配的加速组件数量（0-10） */
    public int getUpgradeCount() {
        return data.get(4);
    }

    /** 当前加速倍率（供界面显示） */
    public double getBoostMultiplier() {
        return ChishiEnergyAssemblyBlockEntity.getBoostMultiplier(getUpgradeCount());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < ChishiEnergyAssemblyBlockEntity.TOTAL_SLOTS) {
                // 方块槽 → 玩家背包（总槽数 = 方块槽 + 36）
                if (!this.moveItemStackTo(current, ChishiEnergyAssemblyBlockEntity.TOTAL_SLOTS, ChishiEnergyAssemblyBlockEntity.TOTAL_SLOTS + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(current, 0, ChishiEnergyAssemblyBlockEntity.TOTAL_SLOTS, false)) {
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
