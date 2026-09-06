package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiFusionItemInputPortBlockEntity;
import com.example.akaishi.item.AkaishiFusionHeatSinkItem;
import com.example.akaishi.item.AkaishiPlasmaRodItem;
import com.example.akaishi.item.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 聚变物品口菜单：输入口（燃料棒/散热片缓冲）/ 输出口（生命灰烬缓冲）共用，27 格 9×3 缓冲槽 + 玩家背包。
 * 缓冲类型决定槽位可放置的物品：输入口仅燃料棒/散热片、输出口仅生命灰烬，与方块实体管道校验一致。
 * 缓冲槽中的物品由方块实体自动流转（输入口 → 控制器燃料/散热槽；控制器灰烬 → 输出口），无需玩家干预。
 */
public class AkaishiFusionItemPortMenu extends AbstractContainerMenu {

    /** 缓冲类型：输入口收燃料棒/散热片，输出口收生命灰烬 */
    public enum BufferKind { INPUT_RODS, OUTPUT_ASH }

    private final Container buffer;
    private final BufferKind kind;

    /** 缓冲类型（界面提示区分输入/输出口） */
    public BufferKind getKind() {
        return kind;
    }

    public AkaishiFusionItemPortMenu(int id, Inventory playerInv, Container buffer, BufferKind kind) {
        super(ModMenus.CHISHI_FUSION_ITEM_PORT.get(), id);
        this.buffer = buffer;
        this.kind = kind;

        // 缓冲槽 9×3（按类型过滤物品）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(buffer, col + row * 9, 8 + col * 18, 17 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return AkaishiFusionItemPortMenu.this.canPlace(stack);
                    }

                    @Override
                    public int getMaxStackSize() {
                        return 64;
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

    /** 槽位过滤：输入口仅燃料棒/散热片，输出口仅生命灰烬 */
    private boolean canPlace(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        return kind == BufferKind.INPUT_RODS
                ? stack.getItem() instanceof AkaishiPlasmaRodItem || stack.getItem() instanceof AkaishiFusionHeatSinkItem
                : stack.is(ModItems.lifeAsh.get());
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
            if (index < AkaishiFusionItemInputPortBlockEntity.BUFFER_SLOTS) {
                // 缓冲槽 → 玩家背包
                if (!this.moveItemStackTo(current, AkaishiFusionItemInputPortBlockEntity.BUFFER_SLOTS,
                        AkaishiFusionItemInputPortBlockEntity.BUFFER_SLOTS + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (canPlace(current)) {
                // 背包可接受物品 → 缓冲槽
                if (!this.moveItemStackTo(current, 0, AkaishiFusionItemInputPortBlockEntity.BUFFER_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 普通物品：背包行 ↔ 快捷栏
                if (index < AkaishiFusionItemInputPortBlockEntity.BUFFER_SLOTS + 27) {
                    if (!this.moveItemStackTo(current, AkaishiFusionItemInputPortBlockEntity.BUFFER_SLOTS + 27,
                            AkaishiFusionItemInputPortBlockEntity.BUFFER_SLOTS + 36, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(current, AkaishiFusionItemInputPortBlockEntity.BUFFER_SLOTS,
                        AkaishiFusionItemInputPortBlockEntity.BUFFER_SLOTS + 27, false)) {
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

    /** 供无方块实体兜底时使用的空菜单（缓冲全空，默认输入口过滤） */
    public static AkaishiFusionItemPortMenu emptyMenu(int id, Inventory inv) {
        return new AkaishiFusionItemPortMenu(id, inv,
                new SimpleContainer(AkaishiFusionItemInputPortBlockEntity.BUFFER_SLOTS), BufferKind.INPUT_RODS);
    }
}
