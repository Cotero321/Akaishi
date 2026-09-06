package com.example.akaishi.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 矿机能量输入口菜单：无机器槽（纯能量缓冲），仅玩家背包 + 能量/容量/成型数据展示。
 * 数据槽：0=能量 1=容量 2=成型状态。
 */
public class AkaishiMinerEnergyInputMenu extends AbstractContainerMenu {

    private final ContainerData data;

    public AkaishiMinerEnergyInputMenu(int id, Inventory playerInv, ContainerData data) {
        super(ModMenus.CHISHI_MINER_ENERGY_INPUT.get(), id);
        this.data = data;

        // 玩家背包 3×9（y=84 起，适配 176×166 界面）
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

    public int getEnergy() {
        return data.get(0);
    }

    public int getCapacity() {
        return data.get(1);
    }

    /** 是否已与成型矿机连接 */
    public boolean isLinked() {
        return data.get(2) == 1;
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
    public static AkaishiMinerEnergyInputMenu emptyMenu(int id, Inventory inv) {
        return new AkaishiMinerEnergyInputMenu(id, inv, new SimpleContainerData(3));
    }
}
