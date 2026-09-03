package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiLifeFusionAnvilBlockEntity;
import com.example.akaishi.item.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 生命的融合砧菜单：输入（赤石护甲）+ 输入（生命的融合锭）+ 输出（生命融合护甲）。
 * 输出槽只读，点击融合按钮经 clickMenuButton 通知服务端执行融合（保留升级数据）。
 */
public class AkaishiLifeFusionAnvilMenu extends AbstractContainerMenu {

    /** 执行融合的按钮 id */
    public static final int BUTTON_FUSE = 100;

    private final Container container;
    private final AkaishiLifeFusionAnvilBlockEntity be;

    public AkaishiLifeFusionAnvilMenu(int id, Inventory inv, AkaishiLifeFusionAnvilBlockEntity be) {
        super(ModMenus.CHISHI_LIFE_FUSION_ANVIL.get(), id);
        this.container = be != null ? be.inventory() : new SimpleContainer(AkaishiLifeFusionAnvilBlockEntity.SLOT_COUNT);
        this.be = be;

        // 机器槽横排（y=34 单行）：护甲 / 融合锭 / 产物输出，标签在槽位上方不压叠
        addSlot(new Slot(container, AkaishiLifeFusionAnvilBlockEntity.INPUT_GEAR_SLOT, 26, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AkaishiLifeFusionAnvilBlockEntity.isFusionGear(stack);
            }
        });
        // 输入槽：仅生命的融合锭可放入
        addSlot(new Slot(container, AkaishiLifeFusionAnvilBlockEntity.INPUT_INGOT_SLOT, 80, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.lifeFusionIngot.get());
            }
        });
        // 输出槽只读：防止放入杂物卡死融合
        addSlot(new Slot(container, AkaishiLifeFusionAnvilBlockEntity.OUTPUT_SLOT, 134, 34) {
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
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be != null && id == BUTTON_FUSE) {
            be.tryFuse();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < AkaishiLifeFusionAnvilBlockEntity.SLOT_COUNT) {
                if (!this.moveItemStackTo(current, AkaishiLifeFusionAnvilBlockEntity.SLOT_COUNT,
                        AkaishiLifeFusionAnvilBlockEntity.SLOT_COUNT + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包 → 按 mayPlace 规则落到对应输入槽（输出槽只读自动跳过）
                if (!this.moveItemStackTo(current, 0, AkaishiLifeFusionAnvilBlockEntity.SLOT_COUNT, false)) {
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
