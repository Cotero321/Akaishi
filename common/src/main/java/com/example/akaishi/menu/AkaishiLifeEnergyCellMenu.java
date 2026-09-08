package com.example.akaishi.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 生命能量储存器菜单（单方块电池独立界面）：
 * 无机器槽位，仅展示生命能量/容量。能量与容量为 long，拆 4 个 int 数据槽同步
 * （0/1 = 能量低/高位，2/3 = 容量低/高位），避免容量超 int 截断。
 */
public class AkaishiLifeEnergyCellMenu extends AbstractContainerMenu {

    private final ContainerData data;

    public AkaishiLifeEnergyCellMenu(int id, Inventory playerInv, ContainerData data) {
        super(ModMenus.CHISHI_LIFE_ENERGY_CELL.get(), id);
        this.data = data;

        // 玩家背包 3 行 × 9 + 快捷栏
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
        this.addDataSlots(data);
    }

    /** 当前生命能量（0/1 低位/高位重组） */
    public long getLifeEnergy() {
        return ((long) data.get(1) << 32) | (data.get(0) & 0xFFFFFFFFL);
    }

    /** 容量上限（2/3 低位/高位重组） */
    public long getLifeMax() {
        return ((long) data.get(3) << 32) | (data.get(2) & 0xFFFFFFFFL);
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
    public static AkaishiLifeEnergyCellMenu emptyMenu(int id, Inventory inv) {
        return new AkaishiLifeEnergyCellMenu(id, inv, new SimpleContainerData(4));
    }
}
