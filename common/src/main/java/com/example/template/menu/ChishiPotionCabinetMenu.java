package com.example.template.menu;

import com.example.template.block.entity.ChishiPotionCabinetBlockEntity;
import com.example.template.life.potion.ChishiPotionItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 药剂库菜单：54 格药剂槽 + 背包。
 * 模板筛选为客户端本地状态（CabinetSlot.isActive 控制渲染/交互），无需网络包。
 */
public class ChishiPotionCabinetMenu extends AbstractContainerMenu {

    /** 当前筛选模板 id（"" = 全部，客户端本地状态） */
    private String filterTemplate = "";
    private final Container container;

    public ChishiPotionCabinetMenu(int id, Inventory inv, ChishiPotionCabinetBlockEntity cabinet) {
        this(id, inv, cabinet.inventory());
    }

    /** 完整构造（网络工厂共用） */
    public ChishiPotionCabinetMenu(int id, Inventory inv, Container container) {
        super(ModMenus.CHISHI_POTION_CABINET.get(), id);
        this.container = container;

        // 药剂槽 6 行 × 9 列
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new CabinetSlot(this, container, col + row * 9, 8 + col * 18, 16 + row * 18));
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

    /** 客户端设置筛选模板（纯本地，槽位 isActive 即时生效） */
    public void setFilter(String templateId) {
        this.filterTemplate = templateId != null ? templateId : "";
    }

    public String getFilterTemplate() {
        return filterTemplate;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < ChishiPotionCabinetBlockEntity.CABINET_SLOTS) {
                // 库内 → 背包
                if (!this.moveItemStackTo(current, ChishiPotionCabinetBlockEntity.CABINET_SLOTS,
                        ChishiPotionCabinetBlockEntity.CABINET_SLOTS + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (current.getItem() instanceof ChishiPotionItem) {
                // 背包 → 库内（同 NBT 自动合并，BE tick 再兜底）
                if (!this.moveItemStackTo(current, 0, ChishiPotionCabinetBlockEntity.CABINET_SLOTS, false)) {
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

    /** 药剂槽：按筛选模板控制渲染与交互（isActive false 时隐藏且不可点击） */
    static class CabinetSlot extends Slot {
        private final ChishiPotionCabinetMenu menu;

        CabinetSlot(ChishiPotionCabinetMenu menu, Container container, int index, int x, int y) {
            super(container, index, x, y);
            this.menu = menu;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof ChishiPotionItem;
        }

        @Override
        public boolean isActive() {
            String filter = menu.getFilterTemplate();
            if (filter.isEmpty()) {
                return true;
            }
            ItemStack stack = getItem();
            return stack.getItem() instanceof ChishiPotionItem
                    && filter.equals(ChishiPotionItem.getTemplateId(stack));
        }
    }
}
