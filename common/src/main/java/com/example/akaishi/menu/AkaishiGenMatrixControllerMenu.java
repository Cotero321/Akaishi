package com.example.akaishi.menu;

import com.example.akaishi.block.AkaishiGenMatrixTier;
import com.example.akaishi.block.entity.AkaishiGenMatrixControllerBlockEntity;
import com.example.akaishi.util.LongDataSlots;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 发生器矩阵控制器菜单：1 个燃料槽 + 10 个升级组件槽 + 玩家背包槽。
 * 数据槽：0/1=能量低/高，2/3=燃烧能量低/高，4/5=燃料总能量低/高，6=结构状态，7=升级组件数。
 */
public class AkaishiGenMatrixControllerMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;
    /** 矩阵等级（低级/高级，供界面显示倍率与文案） */
    private final AkaishiGenMatrixTier tier;

    public AkaishiGenMatrixControllerMenu(int id, Inventory inv, AkaishiGenMatrixControllerBlockEntity be) {
        this(id, inv, be, be.data(), be.tier());
    }

    public AkaishiGenMatrixControllerMenu(int id, Inventory inv, Container container, ContainerData data, AkaishiGenMatrixTier tier) {
        super(ModMenus.CHISHI_GEN_MATRIX_CONTROLLER.get(), id);
        this.container = container;
        this.data = data;
        this.tier = tier;

        // 燃料槽
        addSlot(new Slot(container, AkaishiGenMatrixControllerBlockEntity.FUEL_SLOT, 25, 42));

        // 能源产生升级组件装配槽 5×2（最多 10 个）：固定面板右上角 y=8，5 列整齐（规则 3）
        int[] cols = {80, 98, 116, 134, 152};
        int[] rows = {8, 26};
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < cols.length; c++) {
                addSlot(new SpeedUpgradeSlot(container,
                        AkaishiGenMatrixControllerBlockEntity.UPGRADE_SLOT_START + r * cols.length + c,
                        cols[c], rows[r]));
            }
        }

        // 玩家背包 3×9（升级槽扩为两行 18 间距后整体下移）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 96 + row * 18));
            }
        }
        // 快捷栏 1×9
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 152));
        }

        addDataSlots(data);
    }

    /** 已存能量（long，容量可超 int） */
    public long getEnergy() {
        return LongDataSlots.read(data,
                AkaishiGenMatrixControllerBlockEntity.DATA_ENERGY,
                AkaishiGenMatrixControllerBlockEntity.DATA_ENERGY_HIGH);
    }

    public int getBurnTime() {
        return (int) LongDataSlots.read(data, AkaishiGenMatrixControllerBlockEntity.DATA_BURN,
                AkaishiGenMatrixControllerBlockEntity.DATA_BURN_HIGH);
    }

    public int getBurnTimeTotal() {
        return (int) LongDataSlots.read(data, AkaishiGenMatrixControllerBlockEntity.DATA_BURN_TOTAL,
                AkaishiGenMatrixControllerBlockEntity.DATA_BURN_TOTAL_HIGH);
    }

    /** 结构是否完整激活 */
    public boolean isFormed() {
        return data.get(AkaishiGenMatrixControllerBlockEntity.DATA_FORMED) == 1;
    }

    /** 已装配的加速组件数量（0-10） */
    public int getUpgradeCount() {
        return data.get(AkaishiGenMatrixControllerBlockEntity.DATA_UPGRADES);
    }

    /** 当前加速倍率（供界面显示） */
    public double getBoostMultiplier() {
        return AkaishiGenMatrixControllerBlockEntity.getBoostMultiplier(getUpgradeCount());
    }

    /** 矩阵等级 */
    public AkaishiGenMatrixTier tier() {
        return tier;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < AkaishiGenMatrixControllerBlockEntity.TOTAL_SLOTS) {
                if (!this.moveItemStackTo(current, AkaishiGenMatrixControllerBlockEntity.TOTAL_SLOTS,
                        AkaishiGenMatrixControllerBlockEntity.TOTAL_SLOTS + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(current, 0, AkaishiGenMatrixControllerBlockEntity.TOTAL_SLOTS, false)) {
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
