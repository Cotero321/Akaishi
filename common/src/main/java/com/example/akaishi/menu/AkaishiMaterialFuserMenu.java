package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiMaterialFuserBlockEntity;
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
 * 材料融合器菜单：两输入槽（基底 A / 辅料 B）+ 一只读输出槽 + 三升级槽 + 能量/进度数据。
 * <p>
 * 槽位注册顺序 = {@code slots} 下标：0 速 / 1 能 / 2 无线接收（**规则3：固定面板右上角并排 y=8**）
 * → 3 输入A / 4 输入B / 5 输出（机器行 y=40），随后玩家背包 3×9 + 快捷栏。
 * 自绘槽框与悬停命中区一律读 {@code menu.slots.get(i).x/y}（三套坐标同源）。
 */
public class AkaishiMaterialFuserMenu extends AbstractContainerMenu {

    /** 输入A 在 {@code slots} 中的下标（升级槽占 0..2） */
    public static final int SLOT_INPUT_A_INDEX = MachineUpgradeSlots.SLOT_COUNT;
    /** 输入B 在 {@code slots} 中的下标 */
    public static final int SLOT_INPUT_B_INDEX = MachineUpgradeSlots.SLOT_COUNT + 1;
    /** 输出槽在 {@code slots} 中的下标 */
    public static final int SLOT_OUTPUT_INDEX = MachineUpgradeSlots.SLOT_COUNT + 2;
    /** 机器区槽数（升级槽 3 + 输入 2 + 输出 1），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = MachineUpgradeSlots.SLOT_COUNT + 3;

    private final Container inventory;
    private final ContainerData data;
    private final Container upgrades;

    public AkaishiMaterialFuserMenu(int id, Inventory inv, AkaishiMaterialFuserBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getUpgradeSlots());
    }

    public AkaishiMaterialFuserMenu(int id, Inventory inv, Container inventory, ContainerData data) {
        this(id, inv, inventory, data, new MachineUpgradeSlots());
    }

    AkaishiMaterialFuserMenu(int id, Inventory inv, Container inventory, ContainerData data, Container upgrades) {
        super(ModMenus.CHISHI_MATERIAL_FUSER.get(), id);
        this.inventory = inventory;
        this.data = data;
        this.upgrades = upgrades;

        // 升级槽（速度/能量/无线接收各一格；规则3：固定面板右上角并排 y=8）
        // 注册顺序必须与 MachineUpgradeSlots.SLOT_* 一致：0=速度、1=能量、2=无线接收
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_SPEED, 134, 8));
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_ENERGY, 152, 8));
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_WIRELESS, 116, 8));
        // 输入A / 输入B：排除升级组件（保证 shift 点击时升级组件只进升级槽）
        addSlot(new Slot(inventory, AkaishiMaterialFuserBlockEntity.SLOT_INPUT_A, 26, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !(stack.getItem() instanceof AkaishiMachineUpgradeItem);
            }
        });
        addSlot(new Slot(inventory, AkaishiMaterialFuserBlockEntity.SLOT_INPUT_B, 62, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !(stack.getItem() instanceof AkaishiMachineUpgradeItem);
            }
        });
        // 输出槽只读：防止放入杂物卡死机器
        addSlot(new Slot(inventory, AkaishiMaterialFuserBlockEntity.SLOT_OUTPUT, 98, 40) {
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
        return LongDataSlots.read(data, AkaishiMaterialFuserBlockEntity.DATA_ENERGY,
                AkaishiMaterialFuserBlockEntity.DATA_ENERGY_HIGH);
    }

    public long getEnergyCapacity() {
        return LongDataSlots.read(data, AkaishiMaterialFuserBlockEntity.DATA_CAPACITY,
                AkaishiMaterialFuserBlockEntity.DATA_CAPACITY_HIGH);
    }

    public long getProgress() {
        return data.get(AkaishiMaterialFuserBlockEntity.DATA_PROGRESS);
    }

    /** 当前配方基础总耗时（tick，无配方为 0） */
    public long getRequired() {
        return data.get(AkaishiMaterialFuserBlockEntity.DATA_REQUIRED);
    }

    /** 当前配方单次加工总耗（赤能源，基础值） */
    public long getCost() {
        return LongDataSlots.read(data, AkaishiMaterialFuserBlockEntity.DATA_COST,
                AkaishiMaterialFuserBlockEntity.DATA_COST_HIGH);
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
                // 机器区（升级槽 + 两输入 + 输出）→ 玩家背包
                if (!this.moveItemStackTo(current, MACHINE_SLOT_END, MACHINE_SLOT_END + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包：升级组件 → 升级槽，其余 → 两个输入槽，再在背包内移动
                if (current.getItem() instanceof AkaishiMachineUpgradeItem) {
                    if (!this.moveItemStackTo(current, 0, MachineUpgradeSlots.SLOT_COUNT, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(current, SLOT_INPUT_A_INDEX, SLOT_OUTPUT_INDEX, false)) {
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
        return this.inventory.stillValid(player);
    }

    public ContainerData data() {
        return data;
    }
}
