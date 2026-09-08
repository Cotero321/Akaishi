package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiEnergyGeneratorBlockEntity;
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
public class AkaishiEnergyGeneratorMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public AkaishiEnergyGeneratorMenu(int id, Inventory inv, AkaishiEnergyGeneratorBlockEntity be) {
        // 传入 BE 自身作为容器：成型（多方块外壳）时自动代理到中心主方块，未成型时操作自身槽位
        this(id, inv, be, be.data());
    }

    public AkaishiEnergyGeneratorMenu(int id, Inventory inv, Container container, ContainerData data) {
        super(ModMenus.CHISHI_ENERGY_GENERATOR.get(), id);
        this.container = container;
        this.data = data;

        // 燃料槽
        addSlot(new Slot(container, AkaishiEnergyGeneratorBlockEntity.FUEL_SLOT, 56, 17));

        // 能源产生升级组件装配槽 5×2（最多 10 个）：固定右上角 y=8 起两行，X 从面板右缘向左排列
        int[] cols = {80, 98, 116, 134, 152};
        int[] rows = {8, 26};
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < cols.length; c++) {
                addSlot(new SpeedUpgradeSlot(container, AkaishiEnergyGeneratorBlockEntity.UPGRADE_SLOT_START + r * cols.length + c,
                        cols[c], rows[r]));
            }
        }

        // 玩家背包 3×9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 96 + row * 18));
            }
        }
        // 快捷栏 1×9
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 152));
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

    /** 已装配的加速组件数量（0-10） */
    public int getUpgradeCount() {
        return data.get(3);
    }

    /** 当前加速倍率（供界面显示） */
    public double getBoostMultiplier() {
        return AkaishiEnergyGeneratorBlockEntity.getBoostMultiplier(getUpgradeCount());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < AkaishiEnergyGeneratorBlockEntity.TOTAL_SLOTS) {
                // 方块槽 → 玩家背包（总槽数 = 方块槽 + 36）
                if (!this.moveItemStackTo(current, AkaishiEnergyGeneratorBlockEntity.TOTAL_SLOTS, AkaishiEnergyGeneratorBlockEntity.TOTAL_SLOTS + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(current, 0, AkaishiEnergyGeneratorBlockEntity.TOTAL_SLOTS, false)) {
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
