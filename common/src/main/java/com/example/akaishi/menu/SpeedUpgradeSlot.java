package com.example.akaishi.menu;

import com.example.akaishi.item.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 能源产生升级组件装配槽：只允许放入升级组件，发生器（单块/矩阵控制器）GUI 使用。
 * 单格位内可堆叠（上限 {@link #MAX_STACK}），堆叠数即装配等级。
 */
public class SpeedUpgradeSlot extends Slot {

    /** 单槽堆叠上限（与倍率公式的 n 上限一致） */
    public static final int MAX_STACK = 10;

    public SpeedUpgradeSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.is(ModItems.akaishiSpeedUpgrade.get());
    }

    @Override
    public int getMaxStackSize() {
        // 单格位可堆叠至 10 个，堆叠数即装配等级
        return MAX_STACK;
    }
}
