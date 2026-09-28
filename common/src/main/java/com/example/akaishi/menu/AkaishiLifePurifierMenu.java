package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiLifePurifierBlockEntity;
import com.example.akaishi.item.ModItems;
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
 * 生命能量提纯器菜单：升级槽（速度/能量/无线）+ 1 个输出槽（生命能量固态物，只出不进）+ 双能量/进度数据。
 * 数据槽：0/1=赤能量低/高 2/3=赤容量低/高 4/5=生命能量低/高 6/7=生命容量低/高 8=固化进度百分比。
 */
public class AkaishiLifePurifierMenu extends AbstractContainerMenu {

    /** 机器区槽数（无线接收槽 1 + 输入槽 1 + 输出槽 1），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = 1 + AkaishiLifePurifierBlockEntity.SLOT_COUNT;

    private final Container container;
    private final ContainerData data;
    private final Container upgrades;

    public AkaishiLifePurifierMenu(int id, Inventory inv, AkaishiLifePurifierBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getUpgradeSlots());
    }

    public AkaishiLifePurifierMenu(int id, Inventory inv, Container container, ContainerData data) {
        this(id, inv, container, data, new MachineUpgradeSlots());
    }

    AkaishiLifePurifierMenu(int id, Inventory inv, Container container, ContainerData data, Container upgrades) {
        super(ModMenus.CHISHI_LIFE_PURIFIER.get(), id);
        this.container = container;
        this.data = data;
        this.upgrades = upgrades;

        // 升级槽：本机只保留无线接收槽（固定面板右上角，规则3）
        // 装入后本机器加入所在无线场域，可由微缩矩阵终端直供赤能源 / 生命能量
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_WIRELESS, 152, 8));

        // 输入槽：只收固化原料（浓缩赤石精华），口径与配方 ingredient 一致
        addSlot(new Slot(container, AkaishiLifePurifierBlockEntity.INPUT_SLOT, 116, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.akaishiEssenceCompressed.get());
            }
        });

        // 输出槽：只出不进
        addSlot(new Slot(container, AkaishiLifePurifierBlockEntity.OUTPUT_SLOT, 116, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
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

    /** 当前赤能源储量 */
    public long getAkaishiEnergy() {
        return LongDataSlots.read(data, AkaishiLifePurifierBlockEntity.DATA_AKAISHI_ENERGY,
                AkaishiLifePurifierBlockEntity.DATA_AKAISHI_ENERGY_HIGH);
    }

    /** 赤能源容量 */
    public long getAkaishiMax() {
        return LongDataSlots.read(data, AkaishiLifePurifierBlockEntity.DATA_AKAISHI_CAPACITY,
                AkaishiLifePurifierBlockEntity.DATA_AKAISHI_CAPACITY_HIGH);
    }

    /** 当前生命能量储量 */
    public long getLifeEnergy() {
        return LongDataSlots.read(data, AkaishiLifePurifierBlockEntity.DATA_LIFE_ENERGY,
                AkaishiLifePurifierBlockEntity.DATA_LIFE_ENERGY_HIGH);
    }

    /** 生命能量容量 */
    public long getLifeMax() {
        return LongDataSlots.read(data, AkaishiLifePurifierBlockEntity.DATA_LIFE_CAPACITY,
                AkaishiLifePurifierBlockEntity.DATA_LIFE_CAPACITY_HIGH);
    }

    /** 固化进度（0-100） */
    public int getProgress() {
        return data.get(AkaishiLifePurifierBlockEntity.DATA_PROGRESS);
    }

    /** 无线接收升级是否已装（界面提示用） */
    public boolean hasWirelessReceiver() {
        return upgrades.getItem(MachineUpgradeSlots.SLOT_WIRELESS).getCount() > 0;
    }

    /**
     * 无线接收槽是否为空（界面提示用）。
     * <p>有物品时原版已画物品提示，自定义槽位提示必须让位，否则两个 tooltip 叠在同一坐标上。
     */
    public boolean isWirelessSlotEmpty() {
        return upgrades.getItem(MachineUpgradeSlots.SLOT_WIRELESS).isEmpty();
    }

    /** 输入槽是否为空（同上：仅空槽时提示用途） */
    public boolean isInputSlotEmpty() {
        return container.getItem(AkaishiLifePurifierBlockEntity.INPUT_SLOT).isEmpty();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
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
