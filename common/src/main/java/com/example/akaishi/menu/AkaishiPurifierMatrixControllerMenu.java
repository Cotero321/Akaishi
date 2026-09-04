package com.example.akaishi.menu;

import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.block.entity.AkaishiPurifierMatrixControllerBlockEntity;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 提纯矩阵控制器菜单：升级槽（速度/能量）+ 2 个方块槽（输入/输出）+ 玩家背包槽。
 * 数据槽：0=能量，1=提纯进度，2=结构状态。
 */
public class AkaishiPurifierMatrixControllerMenu extends AbstractContainerMenu {

    /** 机器区槽数（升级槽 2 + 方块槽 2），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = MachineUpgradeSlots.SLOT_COUNT
            + AkaishiPurifierMatrixControllerBlockEntity.SLOT_COUNT;

    private final Container container;
    private final ContainerData data;
    private final Container upgrades;

    public AkaishiPurifierMatrixControllerMenu(int id, Inventory inv, AkaishiPurifierMatrixControllerBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getUpgradeSlots());
    }

    public AkaishiPurifierMatrixControllerMenu(int id, Inventory inv, Container container, ContainerData data) {
        this(id, inv, container, data, new MachineUpgradeSlots());
    }

    AkaishiPurifierMatrixControllerMenu(int id, Inventory inv, Container container, ContainerData data, Container upgrades) {
        super(ModMenus.CHISHI_PURIFIER_MATRIX_CONTROLLER.get(), id);
        this.container = container;
        this.data = data;
        this.upgrades = upgrades;

        // 升级槽（速度/能量各一格，mayPlace 由 MachineUpgradeSlots 按类型互斥过滤）。
        // 置于输入槽(56,17)正下方空地，避开右下成型状态文字区与输出槽(116,35)
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_SPEED, 56, 53));
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_ENERGY, 74, 53));

        // 输入槽：仅接受提纯原料（粗制赤石块 / 赤石水晶块）
        addSlot(new Slot(container, AkaishiPurifierMatrixControllerBlockEntity.INPUT_SLOT, 56, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModBlocks.RAW_CHISHI_BLOCK.get().asItem())
                        || stack.is(ModBlocks.CHISHI_CRYSTAL_BLOCK.get().asItem());
            }
        });
        // 输出槽：只出不进
        addSlot(new Slot(container, AkaishiPurifierMatrixControllerBlockEntity.OUTPUT_SLOT, 116, 35) {
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

    public int getEnergy() {
        return data.get(0);
    }

    public int getProgress() {
        return data.get(1);
    }

    /** 结构是否完整激活 */
    public boolean isFormed() {
        return data.get(2) == 1;
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
                if (!this.moveItemStackTo(current, MACHINE_SLOT_END, MACHINE_SLOT_END + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包：升级组件进升级槽，原料按 mayPlace 过滤进输入槽，其余回收
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
