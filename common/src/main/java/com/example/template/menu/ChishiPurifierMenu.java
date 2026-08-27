package com.example.template.menu;

import com.example.template.block.entity.ChishiPurifierBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 赤石提纯器菜单：3 个方块槽（燃料/输入/输出）+ 玩家背包槽。
 * 通过 ContainerData 将能量、燃烧与进度同步给客户端 GUI。
 */
public class ChishiPurifierMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public ChishiPurifierMenu(int id, Inventory inv, ChishiPurifierBlockEntity be) {
        this(id, inv, be.inventory(), be.data());
    }

    public ChishiPurifierMenu(int id, Inventory inv, Container container, ContainerData data) {
        super(ModMenus.CHISHI_PURIFIER.get(), id);
        this.container = container;
        this.data = data;

        // 方块槽：燃料 / 输入 / 输出
        addSlot(new Slot(container, ChishiPurifierBlockEntity.FUEL_SLOT, 56, 53));
        addSlot(new Slot(container, ChishiPurifierBlockEntity.INPUT_SLOT, 56, 17));
        addSlot(new Slot(container, ChishiPurifierBlockEntity.OUTPUT_SLOT, 116, 35));

        // 玩家背包 3×9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // 快捷栏 1×9
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }

        addDataSlots(data);
    }

    /** 当前赤石能量（GUI 能量条用） */
    public int getEnergy() {
        return data.get(0);
    }

    /** 当前燃料剩余时间（GUI 火焰动画用） */
    public int getBurnTime() {
        return data.get(1);
    }

    /** 当前提纯进度（GUI 进度条用） */
    public int getProgress() {
        return data.get(2);
    }

    /** 燃料总时间（GUI 火焰动画分母） */
    public int getBurnTimeTotal() {
        return data.get(3);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < ChishiPurifierBlockEntity.SLOT_COUNT) {
                // 方块槽 → 玩家背包
                if (!this.moveItemStackTo(current, ChishiPurifierBlockEntity.SLOT_COUNT, 39, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包 → 燃料槽（前 3 个槽位按顺序尝试，燃料槽优先由物品类型决定）
                if (!this.moveItemStackTo(current, 0, ChishiPurifierBlockEntity.SLOT_COUNT, false)) {
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
