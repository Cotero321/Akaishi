package com.example.template.menu;

import com.example.template.block.entity.ChishiSampleVaultBlockEntity;
import com.example.template.life.sample.ChishiLifeSampleItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 样本库菜单：54 格样本槽 + 背包。
 * 槽位仅接受生命样本（纯度任意），同 NBT 样本由 BE tick 自动合并。
 */
public class ChishiSampleVaultMenu extends AbstractContainerMenu {

    private final Container container;

    public ChishiSampleVaultMenu(int id, Inventory inv, ChishiSampleVaultBlockEntity vault) {
        this(id, inv, vault.inventory());
    }

    /** 完整构造（网络工厂共用） */
    public ChishiSampleVaultMenu(int id, Inventory inv, Container container) {
        super(ModMenus.CHISHI_SAMPLE_VAULT.get(), id);
        this.container = container;

        // 样本槽 6 行 × 9 列
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new SampleSlot(container, col + row * 9, 8 + col * 18, 16 + row * 18));
            }
        }
        // 背包 3 行 + 快捷栏
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 138 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 196));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < ChishiSampleVaultBlockEntity.SAMPLE_SLOTS) {
                // 库内 → 背包
                if (!this.moveItemStackTo(current, ChishiSampleVaultBlockEntity.SAMPLE_SLOTS,
                        ChishiSampleVaultBlockEntity.SAMPLE_SLOTS + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (current.getItem() instanceof ChishiLifeSampleItem) {
                // 背包 → 库内（同 NBT 自动合并，BE tick 再兜底）
                if (!this.moveItemStackTo(current, 0, ChishiSampleVaultBlockEntity.SAMPLE_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
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

    /** 样本槽：仅接受生命样本 */
    static class SampleSlot extends Slot {
        SampleSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof ChishiLifeSampleItem;
        }
    }
}
