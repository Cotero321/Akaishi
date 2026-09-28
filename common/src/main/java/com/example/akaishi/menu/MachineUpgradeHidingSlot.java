package com.example.akaishi.menu;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;

/**
 * 机械升级装配槽（存储联动浮层型）：在 {@link OverlayHidingSlot} 隐藏能力之上，
 * 追加仅允许对应机械升级放入的 mayPlace 过滤（与 MachineUpgradeSlot 同源，委托容器 canPlaceItem）。
 */
public class MachineUpgradeHidingSlot extends OverlayHidingSlot {

    public MachineUpgradeHidingSlot(Container container, int slot, int x, int y, BooleanSupplier hidden) {
        super(container, slot, x, y, hidden);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // 与 MachineUpgradeSlot 同源修正：用容器槽位号 getContainerSlot()，
        // 不能用 Slot#index（那是 addSlot 赋的"菜单内序号"，顺序一变就会整体错位）
        return container.canPlaceItem(getContainerSlot(), stack);
    }
}
