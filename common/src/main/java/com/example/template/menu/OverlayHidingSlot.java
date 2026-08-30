package com.example.template.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

import java.util.function.BooleanSupplier;

/**
 * 机器槽位：存储联动浮层打开时自动失活（物品隐藏、交互让位浮层槽位），
 * 与 {@link LinkedVaultSlot} 的启闭互为镜像，从机制上避免双 UI 叠加。
 * hidden 供应商延迟求值（菜单 linkState 在槽位注入之后才赋值）。
 */
public class OverlayHidingSlot extends Slot {

    private final BooleanSupplier hidden;

    public OverlayHidingSlot(Container container, int slot, int x, int y, BooleanSupplier hidden) {
        super(container, slot, x, y);
        this.hidden = hidden;
    }

    @Override
    public boolean isActive() {
        return !hidden.getAsBoolean() && super.isActive();
    }
}
