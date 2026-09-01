package com.example.akaishi.menu;

import com.example.akaishi.block.AkaishiGenMatrixTier;
import com.example.akaishi.block.entity.AkaishiGenMatrixControllerBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 发生器矩阵控制器菜单：1 个燃料槽 + 10 个升级组件槽 + 玩家背包槽。
 * 数据槽：0=能量，1=燃烧能量，2=燃料总能量，3=结构状态，4=升级组件数。
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

        // 能源产生升级组件装配槽 5×2（最多 10 个，行距 18 避免槽位叠压）
        int[] cols = {8, 26, 44, 62, 80};
        int[] rows = {58, 76};
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

    public int getEnergy() {
        return data.get(0);
    }

    public int getBurnTime() {
        return data.get(1);
    }

    public int getBurnTimeTotal() {
        return data.get(2);
    }

    /** 结构是否完整激活 */
    public boolean isFormed() {
        return data.get(3) == 1;
    }

    /** 已装配的加速组件数量（0-10） */
    public int getUpgradeCount() {
        return data.get(4);
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
