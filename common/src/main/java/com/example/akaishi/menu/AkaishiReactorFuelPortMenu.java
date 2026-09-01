package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiReactorFuelPortBlockEntity;
import com.example.akaishi.item.AkaishiFuelCellItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 燃料投放口菜单：27 格燃料罐缓冲槽（9×3）+ 玩家背包。
 * 槽位仅允许燃料罐（空/满），每格限 1 罐，与方块实体的投料/取料逻辑保持一致。
 * 缓冲槽中的燃料罐由方块实体自动分配至控制器，无需玩家干预。
 */
public class AkaishiReactorFuelPortMenu extends AbstractContainerMenu {

    private final Container buffer;

    public AkaishiReactorFuelPortMenu(int id, Inventory playerInv, Container buffer) {
        super(ModMenus.CHISHI_REACTOR_FUEL_PORT.get(), id);
        this.buffer = buffer;

        // 缓冲槽 9×3（仅燃料罐，每格 1 罐）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(buffer, col + row * 9, 8 + col * 18, 17 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.getItem() instanceof AkaishiFuelCellItem;
                    }

                    @Override
                    public int getMaxStackSize() {
                        return 1;
                    }
                });
            }
        }
        // 玩家背包 3×9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // 快捷栏 1×9
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return buffer.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < AkaishiReactorFuelPortBlockEntity.BUFFER_SLOTS) {
                // 缓冲槽 → 玩家背包
                if (!this.moveItemStackTo(current, AkaishiReactorFuelPortBlockEntity.BUFFER_SLOTS,
                        AkaishiReactorFuelPortBlockEntity.BUFFER_SLOTS + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (current.getItem() instanceof AkaishiFuelCellItem) {
                // 背包燃料罐 → 缓冲槽
                if (!this.moveItemStackTo(current, 0, AkaishiReactorFuelPortBlockEntity.BUFFER_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 普通物品：背包行 ↔ 快捷栏
                if (index < AkaishiReactorFuelPortBlockEntity.BUFFER_SLOTS + 27) {
                    if (!this.moveItemStackTo(current, AkaishiReactorFuelPortBlockEntity.BUFFER_SLOTS + 27,
                            AkaishiReactorFuelPortBlockEntity.BUFFER_SLOTS + 36, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(current, AkaishiReactorFuelPortBlockEntity.BUFFER_SLOTS,
                        AkaishiReactorFuelPortBlockEntity.BUFFER_SLOTS + 27, false)) {
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

    /** 供无方块实体兜底时使用的空菜单（缓冲全空） */
    public static AkaishiReactorFuelPortMenu emptyMenu(int id, Inventory inv) {
        return new AkaishiReactorFuelPortMenu(id, inv,
                new SimpleContainer(AkaishiReactorFuelPortBlockEntity.BUFFER_SLOTS));
    }
}
