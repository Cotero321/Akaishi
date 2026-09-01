package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiEnergyAggregatorBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 赤石能量聚合器菜单：输入（下界合金锭 / 母岩）+ 输出（赤石锭 / 升级母岩）+ 能量/进度数据。
 */
public class AkaishiEnergyAggregatorMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public AkaishiEnergyAggregatorMenu(int id, Inventory inv, AkaishiEnergyAggregatorBlockEntity be) {
        this(id, inv, be.inventory(), be.data());
    }

    public AkaishiEnergyAggregatorMenu(int id, Inventory inv, Container container, ContainerData data) {
        super(ModMenus.CHISHI_ENERGY_AGGREGATOR.get(), id);
        this.container = container;
        this.data = data;

        // 方块槽：输入（下界合金锭）左侧 / 输出（赤石锭）右侧
        addSlot(new Slot(container, AkaishiEnergyAggregatorBlockEntity.INPUT_SLOT, 44, 30));
        addSlot(new Slot(container, AkaishiEnergyAggregatorBlockEntity.OUTPUT_SLOT, 116, 30));

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

    public int getEnergy() {
        return data.get(0);
    }

    public int getMaxEnergy() {
        return data.get(1);
    }

    /** 聚合进度（能量百分比 0-100） */
    public int getProgress() {
        return data.get(2);
    }

    /** 当前配方单次消耗（赤石锭聚合或母岩升级） */
    public int getCurrentCost() {
        return data.get(3);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < AkaishiEnergyAggregatorBlockEntity.SLOT_COUNT) {
                if (!this.moveItemStackTo(current, AkaishiEnergyAggregatorBlockEntity.SLOT_COUNT,
                        AkaishiEnergyAggregatorBlockEntity.SLOT_COUNT + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(current, 0, AkaishiEnergyAggregatorBlockEntity.SLOT_COUNT, false)) {
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
