package com.example.akaishi.craft;

import com.example.akaishi.craft.PlannerSolve.Solution;
import com.example.akaishi.value.RecipeIngredients;
import com.example.akaishi.value.RecipeIngredients.RecipeCost;
import com.example.akaishi.value.ValueCache;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 虚拟加工规划的「配方索引与分片构建」协作类（自 {@link VirtualCraftPlanner} 拆出，包私有）：
 * 「产物 → 配方」索引按 {@link ValueCache.Fingerprint} 指纹缓存（指纹一致即复用）；
 * 首次建索引走分片构建（{@link #startBuild} / {@link #stepBuild}），按时间预算切片，避免服务端长阻塞。
 * 公开入口保持在 {@link VirtualCraftPlanner}（逐位委托，行为不变）。
 */
final class PlannerIndex {

    /**
     * 配方索引缓存：指纹一致即复用（配方热重载后指纹必变）。
     * <p>
     * {@code ordered} 是按注册名排好序的产物列表 —— 配方表本身是 Map，遍历顺序不稳定，
     * 若直接用它搜索，同样的查询两次会得到不同结果（界面列表跳动），故排序一次缓存起来。
     */
    record CachedIndex(ValueCache.Fingerprint fingerprint, Map<Item, List<RecipeCost>> byResult,
                       List<Item> craftable, Set<Item> craftableSet) {
    }

    /**
     * 索引缓存<b>按 {@link RecipeManager} 实例分表</b>（同族 {@link ValueCache} 的范式）。
     * <p>
     * 不能只用一个全局槽：集成服的客户端/服务端、开发环境各持一份配方表，
     * 指纹恰好相同时会互相命中，把 A 的索引当成 B 的用。
     * 用弱引用键，配方表被替换后旧表连同索引一起回收。
     */
    private static final Map<RecipeManager, CachedIndex> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * 每 tick 分片预算（纳秒，10 ms）。
     * <p>
     * 按<b>时间</b>而不是条目数分片：单个产物的展开代价差异很大，定条目数会在慢产物上卡帧、
     * 在快产物上白白拖长"准备中"；按时间自适应，总能既保住帧率又尽快建完。
     */
    private static final long SLICE_NANOS = 10_000_000L;

    /**
     * 单片超过这个耗时就打一条日志（ms）：用于定位"某一片里夹了一次性重活"
     * （最典型的是首次估值快照构建 {@code ValueKernel.buildSnapshot}）。
     * 只在真的卡了才打，不是噪声。
     */
    private static final long SLOW_SLICE_MS = 200L;

    private static final Logger LOGGER = LogManager.getLogger("akaishi.craft");

    /**
     * 进行中的分片构建（同 {@link #CACHE} 的弱引用分表范式）。
     * <p>
     * 构建状态放在静态表而不是菜单里：玩家关掉界面时构建进度不该丢，
     * 下次开界面接着跑完，避免"开了又关"反复从头预筛。
     */
    private static final Map<RecipeManager, Pending> PENDING =
            Collections.synchronizedMap(new WeakHashMap<>());

    private PlannerIndex() {
    }

    // ===== 分片构建（首次打开加工页不再卡服务端） =====

    /**
     * 分片构建进度：阶段 A（收集配方 + 排序）一次做完，阶段 B（可解性预筛）按 {@link #SLICE_NANOS} 推进。
     */
    private static final class Pending {
        private final ValueCache.Fingerprint fingerprint;
        private final Map<Item, List<RecipeCost>> byResult;
        private final List<Item> ordered;
        private final List<Item> craftable = new ArrayList<>();
        private final long startedNanos;
        private int cursor;

        private Pending(ValueCache.Fingerprint fingerprint, Map<Item, List<RecipeCost>> byResult,
                List<Item> ordered, long startedNanos) {
            this.fingerprint = fingerprint;
            this.byResult = byResult;
            this.ordered = ordered;
            this.startedNanos = startedNanos;
        }
    }

    /** 实现：契约与口径见 {@link VirtualCraftPlanner#isIndexReady} */
    static boolean isIndexReady(RecipeManager manager) {
        CachedIndex local = CACHE.get(manager);
        return local != null && local.fingerprint().equals(ValueCache.fingerprint(manager));
    }

    /**
     * 实现：契约与口径（为什么要分片、阶段 A/B 划分）见 {@link VirtualCraftPlanner#startBuild}；
     * 阶段 B 交给 {@link #stepBuild}。
     */
    static void startBuild(RecipeManager manager, RegistryAccess access) {
        if (isIndexReady(manager)) {
            return;
        }
        ValueCache.Fingerprint fingerprint = ValueCache.fingerprint(manager);
        Pending running = PENDING.get(manager);
        if (running != null && running.fingerprint.equals(fingerprint)) {
            return; // 已有同指纹的构建在进行中
        }
        long startedNanos = System.nanoTime();
        Map<Item, List<RecipeCost>> byResult = new HashMap<>();
        for (RecipeCost cost : RecipeIngredients.collect(manager, access)) {
            // 无原料格的配方默认不参与虚拟加工（判据与理由见 isUnsafeEmpty）
            if (PlannerSettlement.isUnsafeEmpty(cost)) {
                continue;
            }
            byResult.computeIfAbsent(cost.resultItem(), key -> new ArrayList<>(2)).add(cost);
        }
        List<Item> ordered = new ArrayList<>(byResult.keySet());
        ordered.sort(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
        PENDING.put(manager, new Pending(fingerprint, Map.copyOf(byResult), List.copyOf(ordered), startedNanos));
    }

    /**
     * 实现：契约见 {@link VirtualCraftPlanner#stepBuild}。
     *
     * @return true = 索引已就绪（本次刚好建完）；false = 仍需等待
     */
    static boolean stepBuild(RecipeManager manager) {
        Pending pending = PENDING.get(manager);
        if (pending == null) {
            return true; // 没有进行中的构建：可能已被别的路径建好，调用方重取一次即可
        }
        if (!pending.fingerprint.equals(ValueCache.fingerprint(manager))) {
            PENDING.remove(manager); // 配方表在建的过程中变了：丢弃重来
            return false;
        }
        int end = pending.ordered.size();
        long sliceStartNanos = System.nanoTime();
        long deadline = sliceStartNanos + SLICE_NANOS;
        while (pending.cursor < end) {
            Item item = pending.ordered.get(pending.cursor);
            pending.cursor++;
            // 与 index() 的预筛完全同一套求解与闸门：列得出来的必然做得成
            // （不带场域快照：预筛只判"可解性"，成本不是它的职责）
            Solution solution = PlannerSolve.solve(pending.byResult, item, 1, Map.of(), ProcessCoverage.EMPTY);
            // 纯能量配方（提纯机）没有叶子，靠 machineCost 放行
            if (solution != null && (!solution.leafTotals.isEmpty() || !solution.machineCost.isFree())) {
                pending.craftable.add(item);
            }
            if (System.nanoTime() >= deadline) {
                break; // 本 tick 预算用完：剩下的下 tick 继续
            }
        }
        long sliceMs = (System.nanoTime() - sliceStartNanos) / 1_000_000L;
        if (sliceMs > SLOW_SLICE_MS) {
            // 超预算多半是"这一片里夹了一次性重活"（通常是首次估值快照构建）：
            // 有了这条日志就不必猜，下一刀直接砍在对应层
            LOGGER.info("[akaishi] 配方索引分片偏慢：{} ms（进度 {}/{}）",
                    sliceMs, pending.cursor, pending.ordered.size());
        }
        if (pending.cursor < pending.ordered.size()) {
            return false;
        }
        CACHE.put(manager, new CachedIndex(pending.fingerprint, pending.byResult,
                List.copyOf(pending.craftable), Set.copyOf(pending.craftable)));
        PENDING.remove(manager);
        LOGGER.info("[akaishi] 配方索引就绪：{} 种产物 / {} 种可合成，用时 {} ms",
                pending.ordered.size(), pending.craftable.size(),
                (System.nanoTime() - pending.startedNanos) / 1_000_000L);
        return true;
    }

    // ===== 配方索引缓存 =====

    /**
     * 取"产物 → 配方"索引，指纹变化才重建。
     * <p>
     * 复用 {@link ValueCache#fingerprint(RecipeManager)}：它用"配方数 + 与顺序无关的内容哈希"，
     * 数据包热重载（数量不变但内容变了）同样能识别 —— 与估值缓存同一套失效判据，不另造轮子。
     */
    static CachedIndex index(RecipeManager manager, RegistryAccess access) {
        ValueCache.Fingerprint fingerprint = ValueCache.fingerprint(manager);
        CachedIndex local = CACHE.get(manager);
        if (local != null && local.fingerprint().equals(fingerprint)) {
            return local;
        }
        synchronized (CACHE) {
            local = CACHE.get(manager);
            if (local != null && local.fingerprint().equals(fingerprint)) {
                return local;
            }
            Map<Item, List<RecipeCost>> byResult = new HashMap<>();
            for (RecipeCost cost : RecipeIngredients.collect(manager, access)) {
                // 同 startBuild：无原料格且不合法的配方一律不参与（见 isUnsafeEmpty）
                if (PlannerSettlement.isUnsafeEmpty(cost)) {
                    continue;
                }
                byResult.computeIfAbsent(cost.resultItem(), key -> new ArrayList<>(2)).add(cost);
            }
            List<Item> ordered = new ArrayList<>(byResult.keySet());
            ordered.sort(java.util.Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
            // 预筛"可解性"：跑一遍与 plan() 完全相同的展开与闸门，
            // 于是"列得出来的必然做得成"（口径：可见即可用）。
            // 代价是 items × 节点预算，但只在配方表变化时算一次，有界
            List<Item> craftable = new ArrayList<>(ordered.size());
            for (Item item : ordered) {
                Solution solution = PlannerSolve.solve(byResult, item, 1, Map.of(), ProcessCoverage.EMPTY);
                // 有叶子（要现采材料）或要付机器能量，才算"做得成"；
                // 纯能量配方（提纯机）没有叶子，靠 machineCost 放行
                if (solution != null && (!solution.leafTotals.isEmpty() || !solution.machineCost.isFree())) {
                    craftable.add(item);
                }
            }
            CachedIndex built = new CachedIndex(fingerprint, Map.copyOf(byResult),
                    List.copyOf(craftable), Set.copyOf(craftable));
            CACHE.put(manager, built);
            return built;
        }
    }

    /** 实现：契约见 {@link VirtualCraftPlanner#invalidate} */
    static void invalidate() {
        synchronized (CACHE) {
            CACHE.clear();
        }
        PENDING.clear(); // 进行中的分片构建一并丢弃：它的指纹已经作废
    }
}
