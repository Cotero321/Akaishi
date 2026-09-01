package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiDecayPurifierBlockEntity;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 衰变净化塔菜单：仅升级槽（速度/能量）+ 玩家背包 + 数据槽（能量/容量/净化中/区域数）。
 * 无机器物品槽位；198 高 GUI（与催化剂一致，玩家背包 y=124、快捷栏 y=180）。
 */
public class AkaishiDecayPurifierMenu extends AbstractContainerMenu {

    /** 机器区槽数（仅升级槽 2 格），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = MachineUpgradeSlots.SLOT_COUNT;

    private final ContainerData data;
    private final Container upgrades;

    public AkaishiDecayPurifierMenu(int id, Inventory inv, AkaishiDecayPurifierBlockEntity be) {
        this(id, inv, be.data(), be.getUpgradeSlots());
    }

    /** 网络工厂兜底构造（无方块实体时传空升级槽） */
    public AkaishiDecayPurifierMenu(int id, Inventory inv, ContainerData data) {
        this(id, inv, data, new MachineUpgradeSlots());
    }

    AkaishiDecayPurifierMenu(int id, Inventory inv, ContainerData data, Container upgrades) {
        super(ModMenus.CHISHI_DECAY_PURIFIER.get(), id);
        this.data = data;
        this.upgrades = upgrades;

        // 升级槽（速度/能量各一格，mayPlace 由 MachineUpgradeSlots 按类型互斥过滤）
        addSlot(new Slot(upgrades, MachineUpgradeSlots.SLOT_SPEED, 134, 30));
        addSlot(new Slot(upgrades, MachineUpgradeSlots.SLOT_ENERGY, 152, 30));

        // 玩家背包 3 行 y=124 起，快捷栏 y=180（198 高 GUI，与 akaishi_wireless_terminal.png 槽位图案对齐）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 124 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 180));
        }
        this.addDataSlots(data);
    }

    public long getEnergy() {
        return data.get(AkaishiDecayPurifierBlockEntity.DATA_ENERGY);
    }

    public long getEnergyCapacity() {
        return data.get(AkaishiDecayPurifierBlockEntity.DATA_CAPACITY);
    }

    /** 是否正在净化（有区域且能量充足） */
    public boolean isWorking() {
        return data.get(AkaishiDecayPurifierBlockEntity.DATA_WORKING) == 1;
    }

    /** 范围内衰竭区域数量 */
    public int getZoneCount() {
        return data.get(AkaishiDecayPurifierBlockEntity.DATA_ZONE_COUNT);
    }

    /** 速度升级组件数量（0~8） */
    public int getSpeedUpgradeCount() {
        return upgrades.getItem(MachineUpgradeSlots.SLOT_SPEED).getCount();
    }

    /** 能量升级组件数量（0~8） */
    public int getEnergyUpgradeCount() {
        return upgrades.getItem(MachineUpgradeSlots.SLOT_ENERGY).getCount();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < MACHINE_SLOT_END) {
                // 升级槽 → 玩家背包
                if (!this.moveItemStackTo(current, MACHINE_SLOT_END, MACHINE_SLOT_END + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包 → 升级槽（mayPlace 过滤）
                if (!this.moveItemStackTo(current, 0, MACHINE_SLOT_END, false)) {
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
        return true;
    }
}
