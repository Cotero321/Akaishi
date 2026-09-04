package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiEnergyProcessorBlockEntity;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 能量加工器菜单：1 个输入槽（生命固态物）+ 机器升级槽（速度/能量各一格）+ 赤能源/双输入罐/双输出罐/进度数据。
 * 槽位：0/1=升级槽 2=输入槽 3-38=玩家背包与快捷栏。
 * 数据槽：0/1=赤能量/赤容量 2/3=至纯能量入量/容量 4/5=复合能量入量/容量
 * 6/7=至纯燃料出量/容量 8/9=复合燃料出量/容量 10=加工进度百分比。
 */
public class AkaishiEnergyProcessorMenu extends AbstractContainerMenu {

    /** 机器区槽数（升级槽 2 + 输入槽 1），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = MachineUpgradeSlots.SLOT_COUNT + AkaishiEnergyProcessorBlockEntity.SLOT_COUNT;

    private final Container container;
    private final ContainerData data;
    private final Container upgrades;

    public AkaishiEnergyProcessorMenu(int id, Inventory inv, AkaishiEnergyProcessorBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getUpgradeSlots());
    }

    public AkaishiEnergyProcessorMenu(int id, Inventory inv, Container container, ContainerData data, Container upgrades) {
        super(ModMenus.CHISHI_ENERGY_PROCESSOR.get(), id);
        this.container = container;
        this.data = data;
        this.upgrades = upgrades;

        // 升级槽（速度/能量各一格，mayPlace 由 MachineUpgradeSlots 按类型互斥过滤）
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_SPEED, 152, 6));
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_ENERGY, 152, 24));

        // 输入槽：只收生命固态物
        addSlot(new Slot(container, AkaishiEnergyProcessorBlockEntity.INPUT_SLOT, 116, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.akaishiLifeEssenceSolid.get());
            }
        });

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
        return data.get(AkaishiEnergyProcessorBlockEntity.DATA_CHISHI_ENERGY);
    }

    public long getAkaishiMax() {
        return data.get(AkaishiEnergyProcessorBlockEntity.DATA_CHISHI_CAPACITY);
    }

    public long getPureInAmount() {
        return data.get(AkaishiEnergyProcessorBlockEntity.DATA_PURE_IN_AMOUNT);
    }

    public long getPureInMax() {
        return data.get(AkaishiEnergyProcessorBlockEntity.DATA_PURE_IN_CAPACITY);
    }

    public long getCompoundInAmount() {
        return data.get(AkaishiEnergyProcessorBlockEntity.DATA_COMPOUND_IN_AMOUNT);
    }

    public long getCompoundInMax() {
        return data.get(AkaishiEnergyProcessorBlockEntity.DATA_COMPOUND_IN_CAPACITY);
    }

    public long getPureOutAmount() {
        return data.get(AkaishiEnergyProcessorBlockEntity.DATA_PURE_OUT_AMOUNT);
    }

    public long getPureOutMax() {
        return data.get(AkaishiEnergyProcessorBlockEntity.DATA_PURE_OUT_CAPACITY);
    }

    public long getCompoundOutAmount() {
        return data.get(AkaishiEnergyProcessorBlockEntity.DATA_COMPOUND_OUT_AMOUNT);
    }

    public long getCompoundOutMax() {
        return data.get(AkaishiEnergyProcessorBlockEntity.DATA_COMPOUND_OUT_CAPACITY);
    }

    /** 加工进度（0-100） */
    public int getProgress() {
        return data.get(AkaishiEnergyProcessorBlockEntity.DATA_PROGRESS);
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
                if (!this.moveItemStackTo(current, MACHINE_SLOT_END,
                        MACHINE_SLOT_END + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包：升级组件自动按槽位类型过滤入升级槽，生命固态物入输入槽
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
        return container.stillValid(player);
    }
}
