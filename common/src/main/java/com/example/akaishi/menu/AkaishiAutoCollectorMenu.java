package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiAutoCollectorBlockEntity;
import com.example.akaishi.item.AkaishiMachineUpgradeItem;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 自动收集器菜单：升级槽（速度/能量）+ 27 槽存储（9×3）+ 玩家背包。
 * 通过 ContainerData 将能量与收集进度同步给客户端 GUI。
 */
public class AkaishiAutoCollectorMenu extends AbstractContainerMenu {

    /** 机器区槽数（升级槽 2 + 存储槽 27），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = MachineUpgradeSlots.SLOT_COUNT + AkaishiAutoCollectorBlockEntity.STORAGE_SIZE;

    private final Container container;
    private final ContainerData data;
    private final Container upgrades;

    public AkaishiAutoCollectorMenu(int id, Inventory inv, AkaishiAutoCollectorBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getUpgradeSlots());
    }

    public AkaishiAutoCollectorMenu(int id, Inventory inv, Container container, ContainerData data) {
        this(id, inv, container, data, new MachineUpgradeSlots());
    }

    AkaishiAutoCollectorMenu(int id, Inventory inv, Container container, ContainerData data, Container upgrades) {
        super(ModMenus.CHISHI_AUTO_COLLECTOR.get(), id);
        this.container = container;
        this.data = data;
        this.upgrades = upgrades;

        // 升级槽（速度/能量各一格，mayPlace 由 MachineUpgradeSlots 按类型互斥过滤；顶部右侧避开状态行/能量条）
        addSlot(new Slot(upgrades, MachineUpgradeSlots.SLOT_SPEED, 134, 8));
        addSlot(new Slot(upgrades, MachineUpgradeSlots.SLOT_ENERGY, 152, 8));

        // 存储槽 9×3
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(container, col + row * 9, 8 + col * 18, 46 + row * 18));
            }
        }
        // 玩家背包 3×9（198 高 GUI：y=124 起，与 akaishi_auto_collector.png 槽位图案对齐）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 124 + row * 18));
            }
        }
        // 快捷栏 1×9
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 180));
        }

        addDataSlots(data);
    }

    /** 当前赤石能量（GUI 能量条用） */
    public int getEnergy() {
        return data.get(0);
    }

    /** 能量容量（GUI 能量条分母） */
    public int getEnergyCapacity() {
        return data.get(1);
    }

    /** 当前收集进度百分比（GUI 进度条用） */
    public int getProgress() {
        return data.get(2);
    }

    /** 工作状态：0=待机 1=能量不足 2=工作中 */
    public int getStatus() {
        return data.get(3);
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
                // 机器区（升级槽 + 存储槽）→ 玩家背包
                if (!this.moveItemStackTo(current, MACHINE_SLOT_END,
                        MACHINE_SLOT_END + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包：升级组件 → 升级槽，其余 → 存储槽
                if (current.getItem() instanceof AkaishiMachineUpgradeItem) {
                    if (!this.moveItemStackTo(current, 0, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(current, 2, MACHINE_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(current, MACHINE_SLOT_END + 27,
                        MACHINE_SLOT_END + 36, false)
                        && !this.moveItemStackTo(current, MACHINE_SLOT_END,
                        MACHINE_SLOT_END + 27, false)) {
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
