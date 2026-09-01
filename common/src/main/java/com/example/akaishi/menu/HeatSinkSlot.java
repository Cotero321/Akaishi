package com.example.akaishi.menu;

import com.example.akaishi.item.AkaishiHeatSinkItem;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;

/**
 * 散热片槽位：仅允许放入 {@link AkaishiHeatSinkItem}，且通过 hidden 供应商实现
 * 温度页专属激活（切到其它页签时隐藏、不可交互）。
 * 温度页用于审视 / 更换反应堆各散热组件中的散热片。
 */
public class HeatSinkSlot extends Slot {

    private final BooleanSupplier hidden;

    public HeatSinkSlot(Container container, int slot, int x, int y, BooleanSupplier hidden) {
        super(container, slot, x, y);
        this.hidden = hidden;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.getItem() instanceof AkaishiHeatSinkItem;
    }

    @Override
    public boolean isActive() {
        return !hidden.getAsBoolean() && super.isActive();
    }
}
