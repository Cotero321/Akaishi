package com.example.akaishi.value;

import com.example.akaishi.api.value.ValueServices;

import net.minecraft.world.item.ItemStack;

/**
 * IP（Item Points）折算：物品 → 占用量的唯一换算入口。
 * <p>
 * 基准口径取价值体系的造价分（{@code craftingCost}）：钻石 410 IP/个、一整箱 708,480 IP，
 * 与设计稿一致。折算<b>只在入账瞬间</b>执行一次，取出时只扣账本中记录的数值，
 * 因此价值表重载 / 配方变动都不会让已存物品的占用量漂移（D10 无偏差）。
 */
public final class ItemPoints {

    /** 单件占用下限：任何可存物品至少占 1 IP，避免零价物品白占容量 */
    public static final long MIN_PER_ITEM = 1L;

    private ItemPoints() {
    }

    /** 单个物品的 IP 占用（同堆物品 NBT 相同，故按物品计价） */
    public static long perItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0L;
        }
        return Math.max(MIN_PER_ITEM, ValueServices.get().craftingCost(stack.getItem()));
    }

    /** 整堆 IP 占用 = 单件 × 数量 */
    public static long of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0L;
        }
        return perItem(stack) * stack.getCount();
    }
}
