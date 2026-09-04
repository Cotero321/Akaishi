package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiItemReconstructorBlockEntity;
import com.example.akaishi.item.AkaishiMachineUpgradeItem;
import com.example.akaishi.item.ModItems;
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
 * 物品重构仪菜单：升级槽（速度/能量）+ 3 机器槽（2=原料，3=衰竭结晶代价，4=产物）+ 玩家背包 + 5 数据槽。
 * 结晶槽仅接纳衰竭结晶（mayPlace 限制）；原料槽排除升级组件（保证 shift 点击时升级组件只进升级槽）。
 */
public class AkaishiItemReconstructorMenu extends AbstractContainerMenu {

    /** 机器区槽数（升级槽 2 + 机器物品槽 3），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = MachineUpgradeSlots.SLOT_COUNT + 3;

    private final ContainerData data;
    private final Container inventory;
    private final Container upgrades;

    public AkaishiItemReconstructorMenu(int id, Inventory inv, AkaishiItemReconstructorBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getUpgradeSlots());
    }

    public AkaishiItemReconstructorMenu(int id, Inventory inv, Container inventory, ContainerData data) {
        this(id, inv, inventory, data, new MachineUpgradeSlots());
    }

    AkaishiItemReconstructorMenu(int id, Inventory inv, Container inventory, ContainerData data, Container upgrades) {
        super(ModMenus.CHISHI_ITEM_RECONSTRUCTOR.get(), id);
        this.data = data;
        this.inventory = inventory;
        this.upgrades = upgrades;

        // 升级槽（速度/能量各一格，mayPlace 由 MachineUpgradeSlots 按类型互斥过滤）
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_SPEED, 134, 40));
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_ENERGY, 152, 40));

        addSlot(new Slot(inventory, 0, 26, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // 原料槽排除升级组件：升级组件必须进升级槽
                return !(stack.getItem() instanceof AkaishiMachineUpgradeItem);
            }
        });
        addSlot(new Slot(inventory, 1, 62, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.exhaustedCrystal.get());
            }
        });
        addSlot(new Slot(inventory, 2, 98, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // 产物槽只读：防止放入杂物卡死机器（canFitOutput 永远 false）
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
        return data.get(AkaishiItemReconstructorBlockEntity.DATA_ENERGY);
    }

    public long getEnergyCapacity() {
        return data.get(AkaishiItemReconstructorBlockEntity.DATA_ENERGY_CAPACITY);
    }

    public long getProgress() {
        return data.get(AkaishiItemReconstructorBlockEntity.DATA_PROGRESS);
    }

    /** 当前配方所需结晶总数（无配方为 0） */
    public long getRequired() {
        return data.get(AkaishiItemReconstructorBlockEntity.DATA_REQUIRED);
    }

    public long getCrystals() {
        return data.get(AkaishiItemReconstructorBlockEntity.DATA_CRYSTALS);
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
                // 机器区（升级槽 + 原料/结晶/产物槽）→ 玩家背包（含快捷栏）
                if (!this.moveItemStackTo(current, MACHINE_SLOT_END,
                        MACHINE_SLOT_END + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包/快捷栏：结晶 → 结晶槽，升级组件 → 升级槽，其余 → 原料槽，再背包内移动
                if (current.is(ModItems.exhaustedCrystal.get())) {
                    if (!this.moveItemStackTo(current, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (current.getItem() instanceof AkaishiMachineUpgradeItem) {
                    if (!this.moveItemStackTo(current, 0, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(current, 2, 3, false)) {
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
