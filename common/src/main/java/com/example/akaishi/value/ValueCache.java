package com.example.akaishi.value;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluid;

/**
 * 估值快照缓存。
 *
 * <p>包含两项关键修正：
 * <ul>
 *   <li>P0-1：失效判定从「仅比配方数量」升级为「数量 + 与顺序无关的内容指纹」，
 *       配方被数据包热重载替换（数量不变）时也能识别并重建。</li>
 *   <li>P0-2：键改为 {@link WeakHashMap}，配方的 {@link RecipeManager} 卸载后
 *       缓存条目可被 GC 回收，不再强引用整个服务端配方表。</li>
 * </ul>
 */
public final class ValueCache {

    /** 最多保留的配方管理器数量，超出即整体丢弃（防止异常场景堆积） */
    private static final int MAX_MANAGERS = 4;

    private static final Object LOCK = new Object();

    private static final Map<RecipeManager, Snapshot> SNAPSHOTS =
            Collections.synchronizedMap(new WeakHashMap<>());

    /** 估值代次：快照重建或主动失效时自增，供外部判断「价格是否需要刷新」 */
    private static final AtomicInteger GENERATION = new AtomicInteger();

    /** 配方内容指纹：数量 + 元素级聚合哈希（加法聚合，与遍历顺序无关） */
    public record Fingerprint(int recipeCount, long hash) {
    }

    /** 一次完整估值结果，构建后只读 */
    public record Snapshot(Fingerprint fingerprint,
                           Map<Item, Double> itemValues,
                           Map<Fluid, Double> fluidPerMb,
                           Map<Item, Double> ingredientTerms,
                           Map<Item, Double> functionTerms,
                           Map<Item, Double> lootTerms) {

        /** 物品价值分（未收录物品为 0） */
        public double itemValue(Item item) {
            return itemValues.getOrDefault(item, 0.0);
        }

        /** 流体价值分：每 mB 分 × 总量 */
        public double fluidValue(Fluid fluid, long mb) {
            if (mb <= 0) {
                return 0.0;
            }
            Double perMb = fluidPerMb.get(fluid);
            return perMb == null ? 0.0 : perMb * mb;
        }
    }

    private ValueCache() {
    }

    /**
     * 取缓存快照；指纹不一致时用 {@code builder} 重建。
     *
     * @param manager 当前配方管理器
     * @param builder 重建逻辑（由内核提供，缓存层不关心算法）
     */
    public static Snapshot get(RecipeManager manager, Supplier<Snapshot> builder) {
        Snapshot cached = SNAPSHOTS.get(manager);
        if (cached != null && cached.fingerprint().equals(fingerprint(manager))) {
            return cached;
        }
        synchronized (LOCK) {
            cached = SNAPSHOTS.get(manager);
            if (cached != null && cached.fingerprint().equals(fingerprint(manager))) {
                return cached;
            }
            Snapshot built = builder.get();
            if (SNAPSHOTS.size() >= MAX_MANAGERS) {
                SNAPSHOTS.clear();
            }
            SNAPSHOTS.put(manager, built);
            GENERATION.incrementAndGet();
            return built;
        }
    }

    /** 配方 / 配置变动后主动失效 */
    public static void invalidate() {
        synchronized (LOCK) {
            SNAPSHOTS.clear();
            GENERATION.incrementAndGet();
        }
    }

    /** 当前估值代次 */
    public static int generation() {
        return GENERATION.get();
    }

    /** 计算内容指纹：配方 id 哈希 + 配方实例哈希，加法聚合保证顺序无关 */
    public static Fingerprint fingerprint(RecipeManager manager) {
        int count = 0;
        long hash = 0L;
        for (Recipe<?> recipe : manager.getRecipes()) {
            count++;
            long entry = ((long) recipe.getId().hashCode() << 32) ^ recipe.hashCode();
            hash += entry;
        }
        return new Fingerprint(count, hash * 31L + count);
    }
}
