package com.example.template.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 液体储罐菜单：无机器槽位，仅玩家背包 + 液体量/容量数据。
 * 数据槽：0=液体量 1=容量。
 */
public class ChishiFluidTankMenu extends AbstractContainerMenu {

    private final ContainerData data;

    public ChishiFluidTankMenu(int id, Inventory playerInv, ContainerData data) {
        super(ModMenus.CHISHI_FLUID_TANK.get(), id);
        this.data = data;

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

    /** 当前液体储量（mb） */
    public long getFluidAmount() {
        return data.get(0);
    }

    /** 液体容量上限（mb） */
    public long getFluidMax() {
        return data.get(1);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    /** 供无方块实体兜底时使用的空菜单（数据全 0） */
    public static ChishiFluidTankMenu emptyMenu(int id, Inventory inv) {
        return new ChishiFluidTankMenu(id, inv, new SimpleContainerData(2));
    }
}
