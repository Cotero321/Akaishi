package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AbstractMechanicalMachineBlockEntity;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import com.example.akaishi.util.LongDataSlots;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 机械改造三机菜单基类：升级槽（速度/能量/无线接收，右上角 y=8）+ 机器槽 + 双能源/进度数据 + 通用快捷移动。
 * 机器槽由子类 addSlot 配置；升级槽与玩家背包布局三机完全一致。
 */
public abstract class AbstractMechanicalMachineMenu extends AbstractContainerMenu {

    /** 升级槽数（速度/能量/无线接收三层，机器区槽 = 本值 + 子类机器槽） */
    public static final int UPGRADE_SLOT_COUNT = MachineUpgradeSlots.SLOT_COUNT;

    protected final Container container;
    protected final ContainerData data;
    protected final Container upgrades;
    private final int machineSlotEnd;

    protected AbstractMechanicalMachineMenu(net.minecraft.world.inventory.MenuType<?> type, int id,
                                            Container container, ContainerData data, Container upgrades,
                                            int machineSlots, Inventory inv) {
        super(type, id);
        this.container = container;
        this.data = data;
        this.upgrades = upgrades;
        this.machineSlotEnd = UPGRADE_SLOT_COUNT + machineSlots;

        // 升级槽（右上角固定并排 y=8，与全模组一致）
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_SPEED, 134, 8));
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_ENERGY, 152, 8));
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_WIRELESS, 116, 8));

        addMachineSlots();

        // 玩家背包 3×9 + 快捷栏 1×9（背包起始 y=104，快捷栏 y=168，与布局草图一致）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 168));
        }
        addDataSlots(data);
    }

    /** 子类在此添加机器槽 */
    protected abstract void addMachineSlots();

    protected Container container() {
        return container;
    }

    /** 无线接收升级是否已装（界面提示用） */
    public boolean hasWirelessReceiver() {
        return upgrades instanceof MachineUpgradeSlots slots && slots.hasWirelessReceiver();
    }

    // ===== 双能源 + 进度数据（能量为 long，各占低/高 32 位两槽） =====

    public long getAkaishiEnergy() {
        return LongDataSlots.read(data, AbstractMechanicalMachineBlockEntity.DATA_AKAISHI,
                AbstractMechanicalMachineBlockEntity.DATA_AKAISHI + 1);
    }

    public long getAkaishiMax() {
        return LongDataSlots.read(data, AbstractMechanicalMachineBlockEntity.DATA_AKAISHI_MAX,
                AbstractMechanicalMachineBlockEntity.DATA_AKAISHI_MAX + 1);
    }

    public long getLifeEnergy() {
        return LongDataSlots.read(data, AbstractMechanicalMachineBlockEntity.DATA_LIFE,
                AbstractMechanicalMachineBlockEntity.DATA_LIFE + 1);
    }

    public long getLifeMax() {
        return LongDataSlots.read(data, AbstractMechanicalMachineBlockEntity.DATA_LIFE_MAX,
                AbstractMechanicalMachineBlockEntity.DATA_LIFE_MAX + 1);
    }

    public int getProgressPercent() {
        return data.get(AbstractMechanicalMachineBlockEntity.DATA_PROGRESS);
    }

    // ===== 制作状态（bit0 请求中/制作中，bit1 当前条件可制作） =====

    public int getCraftState() {
        return data.get(AbstractMechanicalMachineBlockEntity.DATA_CRAFT);
    }

    /** 正在制作（已请求且尚未完成） */
    public boolean isCrafting() {
        return (getCraftState() & 1) != 0;
    }

    /** 当前原料/输出满足制作条件（按钮可用） */
    public boolean canCraft() {
        return (getCraftState() & 2) != 0;
    }

    // ===== 快捷移动 =====

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < machineSlotEnd) {
                // 机器区（升级槽 + 机器槽）→ 玩家背包
                if (!this.moveItemStackTo(current, machineSlotEnd, machineSlotEnd + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包 → 机器区（mayPlace 过滤）
                if (!this.moveItemStackTo(current, 0, machineSlotEnd, false)) {
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