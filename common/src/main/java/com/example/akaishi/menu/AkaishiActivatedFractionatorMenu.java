package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiActivatedFractionatorBlockEntity;
import com.example.akaishi.item.AkaishiMachineUpgradeItem;
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
 * 活化分馏器菜单：升级槽（速度/能量）+ 1 输入槽（仅活化结晶）+ 2 只读输出槽 + 玩家背包 + 3 数据槽。
 */
public class AkaishiActivatedFractionatorMenu extends AbstractContainerMenu {

    /** 机器区槽数（升级槽 2 + 输入/输出槽 3），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = MachineUpgradeSlots.SLOT_COUNT + 3;

    private final ContainerData data;
    private final Container input;
    private final Container output;
    private final Container upgrades;

    public AkaishiActivatedFractionatorMenu(int id, Inventory inv, AkaishiActivatedFractionatorBlockEntity be) {
        this(id, inv, be.inputContainer(), be.outputContainer(), be.data(), be.getUpgradeSlots());
    }

    public AkaishiActivatedFractionatorMenu(int id, Inventory inv, Container input, Container output, ContainerData data) {
        this(id, inv, input, output, data, new MachineUpgradeSlots());
    }

    AkaishiActivatedFractionatorMenu(int id, Inventory inv, Container input, Container output, ContainerData data, Container upgrades) {
        super(ModMenus.CHISHI_ACTIVATED_FRACTIONATOR.get(), id);
        this.data = data;
        this.input = input;
        this.output = output;
        this.upgrades = upgrades;

        // 升级槽（速度/能量各一格，mayPlace 由 MachineUpgradeSlots 按类型互斥过滤）
        addSlot(new Slot(upgrades, MachineUpgradeSlots.SLOT_SPEED, 134, 52));
        addSlot(new Slot(upgrades, MachineUpgradeSlots.SLOT_ENERGY, 152, 52));

        // 输入槽：仅 7 种活化结晶可放入
        addSlot(new Slot(input, 0, 44, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AkaishiActivatedFractionatorBlockEntity.isActivatedCrystal(stack);
            }
        });
        // 输出槽只读：防止放入杂物卡死机器
        addSlot(new Slot(output, 0, 80, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new Slot(output, 1, 116, 52) {
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
        return data.get(AkaishiActivatedFractionatorBlockEntity.DATA_ENERGY);
    }

    public long getEnergyCapacity() {
        return data.get(AkaishiActivatedFractionatorBlockEntity.DATA_ENERGY_CAPACITY);
    }

    /** 当前加工进度（tick，满 {@link com.example.akaishi.config.ModConfig#fractionatorProcessTicks} 结算） */
    public int getProgress() {
        return data.get(AkaishiActivatedFractionatorBlockEntity.DATA_PROGRESS);
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
                // 机器区（升级槽 + 输入/输出槽）→ 玩家背包（含快捷栏）
                if (!this.moveItemStackTo(current, MACHINE_SLOT_END,
                        MACHINE_SLOT_END + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包/快捷栏：活化结晶 → 输入槽，升级组件 → 升级槽，其余仅在玩家栏内移动
                if (AkaishiActivatedFractionatorBlockEntity.isActivatedCrystal(current)) {
                    if (!this.moveItemStackTo(current, 2, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (current.getItem() instanceof AkaishiMachineUpgradeItem) {
                    if (!this.moveItemStackTo(current, 0, 2, false)) {
                        return ItemStack.EMPTY;
                    }
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
        return this.input.stillValid(player) && this.output.stillValid(player);
    }

    public ContainerData data() {
        return data;
    }
}
