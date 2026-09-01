package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiLifeCentrifugeBlockEntity;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
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
 * 生命离心机菜单：升级槽（速度/能量）+ 2 机器输出槽（0=活化结晶主产物，1=衰竭结晶副产物）+ 玩家背包 + 5 数据槽。
 * 输入为液体（经管道注入），无物品输入槽；输出物由玩家从 GUI 取出。
 */
public class AkaishiLifeCentrifugeMenu extends AbstractContainerMenu {

    /** 机器区槽数（升级槽 2 + 输出槽 2），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = MachineUpgradeSlots.SLOT_COUNT + 2;

    private final ContainerData data;
    private final Container output;
    private final Container upgrades;

    public AkaishiLifeCentrifugeMenu(int id, Inventory inv, AkaishiLifeCentrifugeBlockEntity be) {
        this(id, inv, be.outputContainer(), be.data(), be.getUpgradeSlots());
    }

    public AkaishiLifeCentrifugeMenu(int id, Inventory inv, Container output, ContainerData data) {
        this(id, inv, output, data, new MachineUpgradeSlots());
    }

    AkaishiLifeCentrifugeMenu(int id, Inventory inv, Container output, ContainerData data, Container upgrades) {
        super(ModMenus.CHISHI_LIFE_CENTRIFUGE.get(), id);
        this.data = data;
        this.output = output;
        this.upgrades = upgrades;

        // 升级槽（速度/能量各一格，mayPlace 由 MachineUpgradeSlots 按类型互斥过滤；产物槽右侧）
        addSlot(new Slot(upgrades, MachineUpgradeSlots.SLOT_SPEED, 134, 56));
        addSlot(new Slot(upgrades, MachineUpgradeSlots.SLOT_ENERGY, 152, 56));

        addSlot(new Slot(output, 0, 62, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // 输出槽只读：防止放入杂物卡死机器
            }
        });
        addSlot(new Slot(output, 1, 98, 56) {
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

    public long getEnergy() {
        return data.get(AkaishiLifeCentrifugeBlockEntity.DATA_ENERGY);
    }

    public long getEnergyCapacity() {
        return data.get(AkaishiLifeCentrifugeBlockEntity.DATA_ENERGY_CAPACITY);
    }

    public long getInAmount() {
        return data.get(AkaishiLifeCentrifugeBlockEntity.DATA_IN_AMOUNT);
    }

    public long getInMax() {
        return data.get(AkaishiLifeCentrifugeBlockEntity.DATA_IN_CAPACITY);
    }

    /** 当前批次进度（mb，满 {@link AkaishiLifeCentrifugeBlockEntity#BATCH_MB} 结算） */
    public long getProgress() {
        return data.get(AkaishiLifeCentrifugeBlockEntity.DATA_PROGRESS);
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
                // 机器区（升级槽 + 输出槽）→ 玩家背包
                if (!this.moveItemStackTo(current, MACHINE_SLOT_END, MACHINE_SLOT_END + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包：升级组件进升级槽（mayPlace 过滤），输出槽只读自动跳过
                if (!this.moveItemStackTo(current, 0, MACHINE_SLOT_END, false)) {
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
        return this.output.stillValid(player);
    }

    public ContainerData data() {
        return data;
    }
}
