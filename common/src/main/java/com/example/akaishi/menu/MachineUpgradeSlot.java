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
        // 必须用 getContainerSlot()（容器槽位号）而非 index：
        // Slot#index 是「菜单内序号」，由 AbstractContainerMenu#addSlot 按加入顺序赋值，
        // 两者只有在"升级槽按容器顺序且排在最前"时才恰好相等；一旦加入顺序或数量不同就会整体错位
        // （生命提纯器只加 1 个无线槽 → 菜单序号 0 → 被误判成速度格，只收速度升级）。
        return container.canPlaceItem(getContainerSlot(), stack);
    }
}
