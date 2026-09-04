package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiFuelMixerBlockEntity;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 燃料混合器菜单：机器升级槽（速度/能量各一格，顶部并排）+ 玩家背包 + 9 个数据槽同步。
 * 槽位：0/1=升级槽 2-37=玩家背包与快捷栏。
 * 数据槽：0/1=赤能量/容量 2/3=输入1 4/5=输入2 6/7=输出 8=混合进度。
 */
public class AkaishiFuelMixerMenu extends AbstractContainerMenu {

    /** 机器区槽数（升级槽 2，无物品输入输出槽），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = MachineUpgradeSlots.SLOT_COUNT;

    private final ContainerData data;
    private final Container upgrades;

    public AkaishiFuelMixerMenu(int id, Inventory inv, AkaishiFuelMixerBlockEntity be) {
        this(id, inv, be.data(), be.getUpgradeSlots());
    }

    public AkaishiFuelMixerMenu(int id, Inventory inv, ContainerData data, Container upgrades) {
        super(ModMenus.CHISHI_FUEL_MIXER.get(), id);
        this.data = data;
        this.upgrades = upgrades;

        // 升级槽（速度/能量各一格，mayPlace 由 MachineUpgradeSlots 按类型互斥过滤）
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_SPEED, 134, 6));
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_ENERGY, 152, 6));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    public long getAkaishiEnergy() {
        return data.get(AkaishiFuelMixerBlockEntity.DATA_CHISHI_ENERGY);
    }

    public long getAkaishiMax() {
        return data.get(AkaishiFuelMixerBlockEntity.DATA_CHISHI_CAPACITY);
    }

    public long getIn1Amount() {
        return data.get(AkaishiFuelMixerBlockEntity.DATA_IN1_AMOUNT);
    }

    public long getIn1Max() {
        return data.get(AkaishiFuelMixerBlockEntity.DATA_IN1_CAPACITY);
    }

    public long getIn2Amount() {
        return data.get(AkaishiFuelMixerBlockEntity.DATA_IN2_AMOUNT);
    }

    public long getIn2Max() {
        return data.get(AkaishiFuelMixerBlockEntity.DATA_IN2_CAPACITY);
    }

    public long getOutAmount() {
        return data.get(AkaishiFuelMixerBlockEntity.DATA_OUT_AMOUNT);
    }

    public long getOutMax() {
        return data.get(AkaishiFuelMixerBlockEntity.DATA_OUT_CAPACITY);
    }

    /** 混合进度（0-100） */
    public int getProgress() {
        return data.get(AkaishiFuelMixerBlockEntity.DATA_PROGRESS);
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
                // 玩家背包 → 升级槽（mayPlace 按组件类型自动过滤）
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
