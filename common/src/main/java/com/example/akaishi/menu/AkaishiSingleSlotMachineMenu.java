package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiSingleSlotMachineBlockEntity;
import com.example.akaishi.item.AkaishiMachineUpgradeItem;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 单输入单输出处理机器菜单抽象基类（升级槽 2 + 输入 + 输出 + 玩家背包 + 数据槽）。
 * 槽位布局四台机器统一：输入(26,40)、输出(98,40)、速度升级(134,40)、能量升级(152,40)、
 * 背包 (8,124) 起（窗口高 198）。
 */
public abstract class AkaishiSingleSlotMachineMenu extends AbstractContainerMenu {

    /** 机器区槽数（升级槽 2 + 输入 1 + 输出 1），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = MachineUpgradeSlots.SLOT_COUNT + 2;

    private final ContainerData data;
    private final Container inventory;
    private final Container upgrades;

    protected AkaishiSingleSlotMachineMenu(@Nullable MenuType<?> type, int id, Container inventory,
                                          ContainerData data, Container upgrades, Inventory inv) {
        super(type, id);
        this.data = data;
        this.inventory = inventory;
        this.upgrades = upgrades;

        // 升级槽（速度/能量各一格，互斥过滤由 MachineUpgradeSlots.canPlaceItem 完成）
        addSlot(new Slot(upgrades, MachineUpgradeSlots.SLOT_SPEED, 134, 40));
        addSlot(new Slot(upgrades, MachineUpgradeSlots.SLOT_ENERGY, 152, 40));
        // 输入槽：排除升级组件（保证 shift 点击时升级组件只进升级槽）
        addSlot(new Slot(inventory, 0, 26, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !(stack.getItem() instanceof AkaishiMachineUpgradeItem);
            }
        });
        // 输出槽只读：防止放入杂物卡死机器
        addSlot(new Slot(inventory, 1, 98, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        // 玩家背包 + 快捷栏
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

    public long getEnergy() {
        return data.get(AkaishiSingleSlotMachineBlockEntity.DATA_ENERGY);
    }

    public long getEnergyCapacity() {
        return data.get(AkaishiSingleSlotMachineBlockEntity.DATA_CAPACITY);
    }

    public long getProgress() {
        return data.get(AkaishiSingleSlotMachineBlockEntity.DATA_PROGRESS);
    }

    /** 当前配方总耗时（tick，无配方为 0） */
    public long getRequired() {
        return data.get(AkaishiSingleSlotMachineBlockEntity.DATA_REQUIRED);
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
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            stack = current.copy();
            if (index < MACHINE_SLOT_END) {
                // 机器区（升级槽 + 输入 + 输出）→ 玩家背包
                if (!this.moveItemStackTo(current, MACHINE_SLOT_END, MACHINE_SLOT_END + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包/快捷栏：升级组件 → 升级槽，其余 → 输入槽，再背包内移动
                if (current.getItem() instanceof AkaishiMachineUpgradeItem) {
                    if (!this.moveItemStackTo(current, 0, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(current, 2, 3, false)) {
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(current, MACHINE_SLOT_END + 27, MACHINE_SLOT_END + 36, false)
                        && !this.moveItemStackTo(current, MACHINE_SLOT_END, MACHINE_SLOT_END + 27, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (current.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return stack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    public ContainerData data() {
        return data;
    }
}
