package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiActivatedFractionatorBlockEntity;
import com.example.akaishi.item.AkaishiMachineUpgradeItem;
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
 * 活化分馏器菜单：升级槽（速度/能量/无线接收）+ 1 输入槽（仅活化结晶）+ 2 只读输出槽 + 玩家背包 + 3 数据槽。
 */
public class AkaishiActivatedFractionatorMenu extends AbstractContainerMenu {

    /** 机器区槽数（升级槽 3 + 输入/输出槽 3），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = MachineUpgradeSlots.SLOT_COUNT + 3;
    /** 业务槽位索引（供 Screen tooltip 定位，槽位顺序与 addSlot 一致） */
    public static final int SLOT_INPUT = MachineUpgradeSlots.SLOT_COUNT;
    public static final int SLOT_OUT0 = MachineUpgradeSlots.SLOT_COUNT + 1;
    public static final int SLOT_OUT1 = MachineUpgradeSlots.SLOT_COUNT + 2;

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

        // 升级槽（速度/能量/无线接收各一格，mayPlace 由 MachineUpgradeSlots 按类型互斥过滤；固定面板右上角并排 y=8，规则3）
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_SPEED, 134, 8));
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_ENERGY, 152, 8));
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_WIRELESS, 116, 8));

        // 输入槽：只排除升级组件（保证 shift 点击时升级组件只进升级槽）；
        // 具体哪种输入有效由数据包配方决定，机器在 tick 内核对
        addSlot(new Slot(input, 0, 44, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !(stack.getItem() instanceof AkaishiMachineUpgradeItem);
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
        return LongDataSlots.read(data, AkaishiActivatedFractionatorBlockEntity.DATA_ENERGY,
                AkaishiActivatedFractionatorBlockEntity.DATA_ENERGY_HIGH);
    }

    public long getEnergyCapacity() {
        return LongDataSlots.read(data, AkaishiActivatedFractionatorBlockEntity.DATA_ENERGY_CAPACITY,
                AkaishiActivatedFractionatorBlockEntity.DATA_ENERGY_CAPACITY_HIGH);
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

    /** 无线接收升级是否已装（界面提示用） */
    public boolean hasWirelessReceiver() {
        return upgrades instanceof MachineUpgradeSlots slots && slots.hasWirelessReceiver();
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
                // 玩家背包/快捷栏：升级组件 → 升级槽，其余 → 输入槽，再背包内移动
                // （"哪种结晶能分馏"由数据包配方决定，机器在 tick 内按配方核对，菜单不预判）
                if (current.getItem() instanceof AkaishiMachineUpgradeItem) {
                    if (!this.moveItemStackTo(current, 0, MachineUpgradeSlots.SLOT_COUNT, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(current, SLOT_INPUT, SLOT_INPUT + 1, false)) {
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
        return this.input.stillValid(player) && this.output.stillValid(player);
    }

    public ContainerData data() {
        return data;
    }
}
