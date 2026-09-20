package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiEnergyAggregatorBlockEntity;
import com.example.akaishi.item.AkaishiMachineUpgradeItem;
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
 * 赤石能量聚合器菜单：输入（下界合金锭 / 母岩）+ 输出（赤石锭 / 升级母岩）+ 升级槽 3 + 能量/进度数据。
 * <p>
 * 升级槽（无线接收 / 速度 / 能量）固定面板右上角 Y=8，与其余机器同布局；槽位排在方块槽之后，
 * 故 {@link #MACHINE_SLOT_END} = 5（前 2 格是输入输出，界面贴图只画这两格的框）。
 */
public class AkaishiEnergyAggregatorMenu extends AbstractContainerMenu {

    /** 机器区槽数（输入 + 输出 + 升级槽 3），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = AkaishiEnergyAggregatorBlockEntity.SLOT_COUNT
            + MachineUpgradeSlots.SLOT_COUNT;

    private final Container container;
    private final ContainerData data;
    private final Container upgrades;

    public AkaishiEnergyAggregatorMenu(int id, Inventory inv, AkaishiEnergyAggregatorBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getUpgradeSlots());
    }

    public AkaishiEnergyAggregatorMenu(int id, Inventory inv, Container container, ContainerData data) {
        this(id, inv, container, data, new MachineUpgradeSlots());
    }

    AkaishiEnergyAggregatorMenu(int id, Inventory inv, Container container, ContainerData data, Container upgrades) {
        super(ModMenus.CHISHI_ENERGY_AGGREGATOR.get(), id);
        this.container = container;
        this.data = data;
        this.upgrades = upgrades;

        // 方块槽：输入（下界合金锭）左侧 / 输出（赤石锭）右侧
        addSlot(new Slot(container, AkaishiEnergyAggregatorBlockEntity.INPUT_SLOT, 44, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // 排除升级组件，保证 shift 点击时升级组件只进升级槽
                return !(stack.getItem() instanceof AkaishiMachineUpgradeItem);
            }
        });
        // 输出槽只读：防止放入杂物卡死机器（与单槽族机器同口径）
        addSlot(new Slot(container, AkaishiEnergyAggregatorBlockEntity.OUTPUT_SLOT, 116, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        // 升级槽（速度/能量/无线接收各一格，固定面板右上角 Y=8）
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_SPEED, 134, 8));
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_ENERGY, 152, 8));
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_WIRELESS,
                MachineUpgradeSlots.WIRELESS_SLOT_X, MachineUpgradeSlots.WIRELESS_SLOT_Y));

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

    public long getEnergy() {
        return LongDataSlots.read(data, AkaishiEnergyAggregatorBlockEntity.DATA_ENERGY,
                AkaishiEnergyAggregatorBlockEntity.DATA_ENERGY_HIGH);
    }

    public long getMaxEnergy() {
        return LongDataSlots.read(data, AkaishiEnergyAggregatorBlockEntity.DATA_CAPACITY,
                AkaishiEnergyAggregatorBlockEntity.DATA_CAPACITY_HIGH);
    }

    /** 聚合进度（能量百分比 0-100） */
    public int getProgress() {
        return data.get(AkaishiEnergyAggregatorBlockEntity.DATA_PROGRESS);
    }

    /** 当前配方单次消耗（赤石锭聚合或母岩升级） */
    public long getCurrentCost() {
        return LongDataSlots.read(data, AkaishiEnergyAggregatorBlockEntity.DATA_COST,
                AkaishiEnergyAggregatorBlockEntity.DATA_COST_HIGH);
    }

    /** 速度升级组件数量（0~8） */
    public int getSpeedUpgradeCount() {
        return upgrades.getItem(MachineUpgradeSlots.SLOT_SPEED).getCount();
    }

    /** 能量升级组件数量（0~8） */
    public int getEnergyUpgradeCount() {
        return upgrades.getItem(MachineUpgradeSlots.SLOT_ENERGY).getCount();
    }

    /** 无线接收升级是否已装（界面提示用） */
    public boolean hasWirelessReceiver() {
        return upgrades instanceof MachineUpgradeSlots slots && slots.hasWirelessReceiver();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < MACHINE_SLOT_END) {
                // 机器区（输入/输出 + 升级槽）→ 玩家背包
                if (!this.moveItemStackTo(current, MACHINE_SLOT_END, MACHINE_SLOT_END + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包：升级组件 → 升级槽，其余 → 输入槽，再在背包内移动
                if (current.getItem() instanceof AkaishiMachineUpgradeItem) {
                    if (!this.moveItemStackTo(current, AkaishiEnergyAggregatorBlockEntity.SLOT_COUNT,
                            MACHINE_SLOT_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(current, AkaishiEnergyAggregatorBlockEntity.INPUT_SLOT,
                        AkaishiEnergyAggregatorBlockEntity.INPUT_SLOT + 1, false)) {
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
