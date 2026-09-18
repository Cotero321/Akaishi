package com.example.akaishi.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

/**
 * 过滤网真槽位：完全走原版槽渲染（物品图标 + 堆数由原版绘制，界面只补画槽框）。
 * <p>
 * 槽位是原版真槽：光标拿物品点入 = 原版放置 1 个（{@code maxStackSize=1}），点已配置的槽 = 取回过滤物，
 * 取放与同步全部走原版点击/槽同步管线，无自定义包。悬停时原版给出物品名，
 * 操作口径由界面自绘提示补齐（见 {@code AkaishiItemPortScreen#renderFilterTooltip}）。
 */
final class ItemPortFilterSlot extends Slot {

    private final AkaishiItemPortMenu menu;
    /** 过滤网内下标（0..8；容器下标同值，单独存一份免得依赖原版字段） */
    private final int filterIndex;

    ItemPortFilterSlot(AkaishiItemPortMenu menu, Container container, int filterIndex, int x, int y) {
        super(container, filterIndex, x, y);
        this.menu = menu;
        this.filterIndex = filterIndex;
    }

    /** 过滤网内下标（界面悬停提示据此取过滤物名） */
    int filterIndex() {
        return this.filterIndex;
    }

    @Override
    public boolean isActive() {
        // 只在运行页激活：绑定页若仍可命中，会点进看不见的格子里去
        return this.menu.filterSlotsActive();
    }
}
