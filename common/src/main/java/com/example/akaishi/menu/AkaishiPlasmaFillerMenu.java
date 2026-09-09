package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiPlasmaFillerBlockEntity;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import com.example.akaishi.util.LongDataSlots;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 离子体填装器菜单：升级槽（速度/能量）+ 1 反应棒槽 + 3 只读燃料棒输出槽 + 玩家背包 + 7 数据槽（3 等离子体罐量/进度）。
 */
public class AkaishiPlasmaFillerMenu extends AbstractContainerMenu {

    /** 机器区槽数（升级槽 2 + 反应棒 1 + 输出槽 3），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = MachineUpgradeSlots.SLOT_COUNT + 4;
    /** 业务槽位索引（供 Screen tooltip 定位，槽位顺序与 addSlot 一致） */
    public static final int SLOT_ROD = MachineUpgradeSlots.SLOT_COUNT;
    public static final int SLOT_OUT0 = MachineUpgradeSlots.SLOT_COUNT + 1;
    public static final int SLOT_OUT1 = MachineUpgradeSlots.SLOT_COUNT + 2;
    public static final int SLOT_OUT2 = MachineUpgradeSlots.SLOT_COUNT + 3;

    private final ContainerData data;
    private final Container rods;
    private final Container output;
    private final Container upgrades;

    public AkaishiPlasmaFillerMenu(int id, Inventory inv, AkaishiPlasmaFillerBlockEntity be) {
        this(id, inv, be.rodsContainer(), be.outputContainer(), be.data(), be.getUpgradeSlots());
    }

    public AkaishiPlasmaFillerMenu(int id, Inventory inv, Container rods, Container output, ContainerData data) {
        this(id, inv, rods, output, data, new MachineUpgradeSlots());
    }

    AkaishiPlasmaFillerMenu(int id, Inventory inv, Container rods, Container output, ContainerData data, Container upgrades) {
        super(ModMenus.CHISHI_PLASMA_FILLER.get(), id);
        this.data = data;
        this.rods = rods;
        this.output = output;
        this.upgrades = upgrades;

        // 升级槽（速度/能量各一格，mayPlace 由 MachineUpgradeSlots 按类型互斥过滤；固定面板右上角并排 y=8，规则3）
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_SPEED, 134, 8));
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_ENERGY, 152, 8));

        // 反应棒槽：仅聚变反应棒可放入
        addSlot(new Slot(rods, 0, 44, 66) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.fusionRod.get());
            }
        });
        // 燃料棒输出槽只读
        addSlot(new Slot(output, 0, 80, 66) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new Slot(output, 1, 116, 66) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new Slot(output, 2, 152, 66) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

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

    public long getPlasmaAmount(int index) {
        int offset = index * AkaishiPlasmaFillerBlockEntity.DATA_PLASMA_STRIDE;
        return LongDataSlots.read(data, AkaishiPlasmaFillerBlockEntity.DATA_PLASMA0_AMOUNT + offset,
                AkaishiPlasmaFillerBlockEntity.DATA_PLASMA0_AMOUNT_HIGH + offset);
    }

    public long getPlasmaCapacity(int index) {
        int offset = index * AkaishiPlasmaFillerBlockEntity.DATA_PLASMA_STRIDE;
        return LongDataSlots.read(data, AkaishiPlasmaFillerBlockEntity.DATA_PLASMA0_CAPACITY + offset,
                AkaishiPlasmaFillerBlockEntity.DATA_PLASMA0_CAPACITY_HIGH + offset);
    }

    public int getProgress() {
        return data.get(AkaishiPlasmaFillerBlockEntity.DATA_PROGRESS);
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
                // 机器区（升级槽 + 反应棒 + 输出槽）→ 玩家背包
                if (!this.moveItemStackTo(current, MACHINE_SLOT_END, MACHINE_SLOT_END + 27, true)
                        && !this.moveItemStackTo(current, MACHINE_SLOT_END + 27, MACHINE_SLOT_END + 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包：升级组件/反应棒按 mayPlace 过滤进机器区，余量背包内移动
                if (!this.moveItemStackTo(current, 0, MACHINE_SLOT_END, false)
                        && !this.moveItemStackTo(current, MACHINE_SLOT_END + 27, MACHINE_SLOT_END + 36, false)
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
        return this.rods.stillValid(player) && this.output.stillValid(player);
    }

    public ContainerData data() {
        return data;
    }
}
