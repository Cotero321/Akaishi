package com.example.template.menu;

import com.example.template.block.entity.ChishiReactorControllerBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 反应堆控制器菜单：10 个燃料槽（每根燃料棒解锁 1 槽）+ 玩家背包。
 * 数据槽（12 个）：温度/成型/棒数/有效散热/散热%/废品量/废品容量/停机/活跃槽/产率/耗速/废品满。
 * 界面通过 {@link ChishiReactorControllerScreen} 分"燃料/散热"两页展示。
 */
public class ChishiReactorControllerMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public ChishiReactorControllerMenu(int id, Inventory inv, ChishiReactorControllerBlockEntity be) {
        this(id, inv, be.fuelSlots(), be.data());
    }

    public ChishiReactorControllerMenu(int id, Inventory inv, Container container, ContainerData data) {
        super(ModMenus.CHISHI_REACTOR_CONTROLLER.get(), id);
        this.container = container;
        this.data = data;

        // 燃料槽 5×2（GUI 高 186，槽位避开上方信息区）
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 5; col++) {
                addSlot(new Slot(container, row * 5 + col, 8 + col * 18, 34 + row * 18));
            }
        }

        // 玩家背包 3×9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            }
        }
        // 快捷栏 1×9
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 162));
        }

        addDataSlots(data);
    }

    // ===== 数据读取（供界面显示） =====

    public int getTemp() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_TEMP);
    }

    public boolean isFormed() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_FORMED) == 1;
    }

    public int getRodCount() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_ROD_COUNT);
    }

    public int getEffectiveCoolers() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_EFFECTIVE_COOLERS);
    }

    public int getCoolingPercent() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_COOLING_PERCENT);
    }

    public long getWasteAmount() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_WASTE_AMOUNT) & 0xFFFFFFFFL;
    }

    public long getWasteMax() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_WASTE_CAPACITY) & 0xFFFFFFFFL;
    }

    public boolean isShutdown() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_SHUTDOWN) == 1;
    }

    public int getActiveSlots() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_ACTIVE_SLOTS);
    }

    public long getEnergyPerTick() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_ENERGY_PER_TICK) & 0xFFFFFFFFL;
    }

    public double getFuelDrainPerTick() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_FUEL_DRAIN) / 1000.0;
    }

    public boolean isWasteFull() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_WASTE_FULL) == 1;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < ChishiReactorControllerBlockEntity.MAX_FUEL_SLOTS) {
                // 机器槽 → 玩家背包
                if (!this.moveItemStackTo(current, ChishiReactorControllerBlockEntity.MAX_FUEL_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(current, 0, ChishiReactorControllerBlockEntity.MAX_FUEL_SLOTS, false)) {
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
}
