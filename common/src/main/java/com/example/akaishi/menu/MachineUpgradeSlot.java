package com.example.akaishi.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 机械升级装配槽（普通型）：mayPlace 委托容器 canPlaceItem，
 * 仅允许对应类型的机械升级放入（速度格=速度升级、能量格=能量升级），
 * 从菜单层堵住“任意物品塞入升级槽”的漏洞（Slot 默认 mayPlace=true）。
 */
public class MachineUpgradeSlot extends Slot {

    public MachineUpgradeSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return container.canPlaceItem(index, stack);
    }
}
