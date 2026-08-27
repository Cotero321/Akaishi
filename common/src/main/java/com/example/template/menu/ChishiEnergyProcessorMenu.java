package com.example.template.menu;

import com.example.template.block.entity.ChishiEnergyProcessorBlockEntity;
import com.example.template.item.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 能量加工器菜单：1 个输入槽（生命固态物）+ 赤能源/双输入罐/双输出罐/进度数据。
 * 数据槽：0/1=赤能量/赤容量 2/3=至纯能量入量/容量 4/5=复合能量入量/容量
 * 6/7=至纯燃料出量/容量 8/9=复合燃料出量/容量 10=加工进度百分比。
 */
public class ChishiEnergyProcessorMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public ChishiEnergyProcessorMenu(int id, Inventory inv, ChishiEnergyProcessorBlockEntity be) {
        this(id, inv, be.inventory(), be.data());
    }

    public ChishiEnergyProcessorMenu(int id, Inventory inv, Container container, ContainerData data) {
        super(ModMenus.CHISHI_ENERGY_PROCESSOR.get(), id);
        this.container = container;
        this.data = data;

        // 输入槽：只收生命固态物
        addSlot(new Slot(container, ChishiEnergyProcessorBlockEntity.INPUT_SLOT, 116, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.chishiLifeEssenceSolid.get());
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

    public long getChishiEnergy() {
        return data.get(ChishiEnergyProcessorBlockEntity.DATA_CHISHI_ENERGY);
    }

    public long getChishiMax() {
        return data.get(ChishiEnergyProcessorBlockEntity.DATA_CHISHI_CAPACITY);
    }

    public long getPureInAmount() {
        return data.get(ChishiEnergyProcessorBlockEntity.DATA_PURE_IN_AMOUNT);
    }

    public long getPureInMax() {
        return data.get(ChishiEnergyProcessorBlockEntity.DATA_PURE_IN_CAPACITY);
    }

    public long getCompoundInAmount() {
        return data.get(ChishiEnergyProcessorBlockEntity.DATA_COMPOUND_IN_AMOUNT);
    }

    public long getCompoundInMax() {
        return data.get(ChishiEnergyProcessorBlockEntity.DATA_COMPOUND_IN_CAPACITY);
    }

    public long getPureOutAmount() {
        return data.get(ChishiEnergyProcessorBlockEntity.DATA_PURE_OUT_AMOUNT);
    }

    public long getPureOutMax() {
        return data.get(ChishiEnergyProcessorBlockEntity.DATA_PURE_OUT_CAPACITY);
    }

    public long getCompoundOutAmount() {
        return data.get(ChishiEnergyProcessorBlockEntity.DATA_COMPOUND_OUT_AMOUNT);
    }

    public long getCompoundOutMax() {
        return data.get(ChishiEnergyProcessorBlockEntity.DATA_COMPOUND_OUT_CAPACITY);
    }

    /** 加工进度（0-100） */
    public int getProgress() {
        return data.get(ChishiEnergyProcessorBlockEntity.DATA_PROGRESS);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < ChishiEnergyProcessorBlockEntity.SLOT_COUNT) {
                if (!this.moveItemStackTo(current, ChishiEnergyProcessorBlockEntity.SLOT_COUNT,
                        ChishiEnergyProcessorBlockEntity.SLOT_COUNT + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包中仅生命固态物可移入输入槽
                if (!this.moveItemStackTo(current, 0, ChishiEnergyProcessorBlockEntity.SLOT_COUNT, false)) {
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
