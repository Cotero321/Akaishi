package com.example.akaishi.menu;

import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.block.entity.AkaishiPurifierBlockEntity;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 赤石提纯器菜单：升级槽（速度/能量）+ 3 个方块槽（燃料/输入/输出）+ 玩家背包槽。
 * 通过 ContainerData 将能量、燃烧与进度同步给客户端 GUI。
 */
public class AkaishiPurifierMenu extends AbstractContainerMenu {

    /** 机器区槽数（升级槽 2 + 机器物品槽 3），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = MachineUpgradeSlots.SLOT_COUNT + AkaishiPurifierBlockEntity.SLOT_COUNT;

    private final Container container;
    private final ContainerData data;
    private final Container upgrades;

    public AkaishiPurifierMenu(int id, Inventory inv, AkaishiPurifierBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getUpgradeSlots());
    }

    public AkaishiPurifierMenu(int id, Inventory inv, Container container, ContainerData data) {
        this(id, inv, container, data, new MachineUpgradeSlots());
    }

    AkaishiPurifierMenu(int id, Inventory inv, Container container, ContainerData data, Container upgrades) {
        super(ModMenus.CHISHI_PURIFIER.get(), id);
        this.container = container;
        this.data = data;
        this.upgrades = upgrades;

        // 升级槽（速度/能量各一格，mayPlace 由 MachineUpgradeSlots 按类型互斥过滤；燃料槽行右侧避开能量条）
        addSlot(new Slot(upgrades, MachineUpgradeSlots.SLOT_SPEED, 116, 53));
        addSlot(new Slot(upgrades, MachineUpgradeSlots.SLOT_ENERGY, 134, 53));

        // 方块槽：燃料 / 输入 / 输出
        // 燃料槽：仅燃料可放入；提纯矩阵成型后禁用（矩阵由外部赤能源驱动，不再烧燃料）
        addSlot(new Slot(container, AkaishiPurifierBlockEntity.FUEL_SLOT, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return data.get(AkaishiPurifierBlockEntity.DATA_FORMED) == 0
                        && AkaishiPurifierBlockEntity.getFuelEnergy(stack) > 0;
            }
        });
        // 输入槽：仅接受提纯原料（粗制赤石块 / 赤石水晶块）
        addSlot(new Slot(container, AkaishiPurifierBlockEntity.INPUT_SLOT, 56, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModBlocks.RAW_CHISHI_BLOCK.get().asItem())
                        || stack.is(ModBlocks.CHISHI_CRYSTAL_BLOCK.get().asItem());
            }
        });
        // 输出槽：只出不进
        addSlot(new Slot(container, AkaishiPurifierBlockEntity.OUTPUT_SLOT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // 玩家背包 3×9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // 快捷栏 1×9
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }

        addDataSlots(data);
    }

    /** 当前赤石能量（GUI 能量条用） */
    public int getEnergy() {
        return data.get(0);
    }

    /** 当前燃料剩余时间（GUI 火焰动画用） */
    public int getBurnTime() {
        return data.get(1);
    }

    /** 当前提纯进度（GUI 进度条用） */
    public int getProgress() {
        return data.get(2);
    }

    /** 燃料总时间（GUI 火焰动画分母） */
    public int getBurnTimeTotal() {
        return data.get(3);
    }

    /** 是否提纯矩阵成型（GUI 据此切换无燃料版贴图并隐藏燃料槽/火焰） */
    public boolean isFormed() {
        return data.get(AkaishiPurifierBlockEntity.DATA_FORMED) == 1;
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
                // 机器区（升级槽 + 方块槽）→ 玩家背包
                if (!this.moveItemStackTo(current, MACHINE_SLOT_END,
                        MACHINE_SLOT_END + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包：升级组件进升级槽，燃料/原料按 mayPlace 过滤进对应槽，其余回收
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
