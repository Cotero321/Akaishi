package com.example.template.menu;

import com.example.template.block.entity.ChishiLifeCentrifugeBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 生命离心机菜单：2 机器输出槽（0=活化结晶主产物，1=衰竭结晶副产物）+ 玩家背包 + 5 数据槽。
 * 输入为液体（经管道注入），无物品输入槽；输出物由玩家从 GUI 取出。
 */
public class ChishiLifeCentrifugeMenu extends AbstractContainerMenu {

    private final ContainerData data;
    private final Container output;

    public ChishiLifeCentrifugeMenu(int id, Inventory inv, ChishiLifeCentrifugeBlockEntity be) {
        this(id, inv, be.outputContainer(), be.data());
    }

    public ChishiLifeCentrifugeMenu(int id, Inventory inv, Container output, ContainerData data) {
        super(ModMenus.CHISHI_LIFE_CENTRIFUGE.get(), id);
        this.data = data;
        this.output = output;

        addSlot(new Slot(output, 0, 62, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // 输出槽只读：防止放入杂物卡死机器
            }
        });
        addSlot(new Slot(output, 1, 98, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 124 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 180));
        }
        addDataSlots(data);
    }

    public long getEnergy() {
        return data.get(ChishiLifeCentrifugeBlockEntity.DATA_ENERGY);
    }

    public long getEnergyCapacity() {
        return data.get(ChishiLifeCentrifugeBlockEntity.DATA_ENERGY_CAPACITY);
    }

    public long getInAmount() {
        return data.get(ChishiLifeCentrifugeBlockEntity.DATA_IN_AMOUNT);
    }

    public long getInMax() {
        return data.get(ChishiLifeCentrifugeBlockEntity.DATA_IN_CAPACITY);
    }

    /** 当前批次进度（mb，满 {@link ChishiLifeCentrifugeBlockEntity#BATCH_MB} 结算） */
    public long getProgress() {
        return data.get(ChishiLifeCentrifugeBlockEntity.DATA_PROGRESS);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            stack = current.copy();
            if (index < 2) {
                // 机器输出槽 → 玩家背包（2..28）→ 快捷栏（29..37）
                if (!this.moveItemStackTo(current, 2, 29, true) && !this.moveItemStackTo(current, 29, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包/快捷栏：无物品输入槽，仅在两者间移动
                if (!this.moveItemStackTo(current, 29, 38, false) && !this.moveItemStackTo(current, 2, 29, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (current.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return stack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.output.stillValid(player);
    }

    public ContainerData data() {
        return data;
    }
}
