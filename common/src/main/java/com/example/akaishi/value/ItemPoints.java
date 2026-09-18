package com.example.akaishi.value;

import com.example.akaishi.api.value.IValueService;
import com.example.akaishi.api.value.ValueServices;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    /**
     * 单件 IP 缓存（键为物品，不含 NBT —— IP 本就按物品计价，见 {@link #perItem(ItemStack)}）。
     * <p>
     * <b>为什么必须有</b>：{@code ValueServices.get().craftingCost} → {@code ValueCache.get}
     * 每次都会重算一遍「全部配方的内容指纹」（O(配方数)）。而虚拟加工规划器在热路径上
     * 反复按<b>候选物</b>取单价（同一物品被取上千次），实测 1017 项预筛因此要 14.8 秒。
     * 按物品缓存后同一物品只算一次，数量级直接掉下来。
     * <p>
     * 失效判据取 {@code ValueServices.get().version()}（= 估值代次，配方 / 配置重载都会 +1）：
     * 代次一变整表清空，绝不会把旧价当新价用。
     */
    private static final Map<Item, Long> CACHE = new ConcurrentHashMap<>();
    private static volatile int cachedGeneration = -1;

    private ItemPoints() {
    }

    /** 单个物品的 IP 占用（同堆物品 NBT 相同，故按物品计价） */
    public static long perItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0L;
        }
        return perItem(stack.getItem());
    }

    /** 单个物品的 IP 占用（缓存入口；非物品传 null 视作 0） */
    public static long perItem(Item item) {
        if (item == null || item == Items.AIR) {
            return 0L;
        }
        IValueService service = ValueServices.get();
        int generation = service.version();
        if (generation != cachedGeneration) {
            CACHE.clear();
            cachedGeneration = generation;
        }
        return CACHE.computeIfAbsent(item, key -> Math.max(MIN_PER_ITEM, service.craftingCost(key)));
    }

    /** 整堆 IP 占用 = 单件 × 数量 */
    public static long of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0L;
        }
        return perItem(stack) * stack.getCount();
    }
}
