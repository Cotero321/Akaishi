package com.example.template.menu;

import com.example.template.item.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 能源产生升级组件装配槽：只允许放入升级组件，发生器（单块/多方块中心）GUI 使用。
 */
public class SpeedUpgradeSlot extends Slot {

    public SpeedUpgradeSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.is(ModItems.chishiSpeedUpgrade.get());
    }
}
