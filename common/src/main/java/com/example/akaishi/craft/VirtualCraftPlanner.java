package com.example.akaishi.craft;

import com.example.akaishi.value.ItemPoints;
import com.example.akaishi.value.ItemTerminalFee;
import com.example.akaishi.value.RecipeIngredients;
import com.example.akaishi.value.RecipeIngredients.RecipeCost;
import com.example.akaishi.value.ValueCache;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 虚拟加工规划器：把「目标物品」递归展开成 <b>材料清单 / 耗时 / 赤能源</b> 三笔成本。
 * <p>
 * <b>口径（设计记忆 §15.5/§15.6）</b>：只处理<b>可解析配方</b>（原版工作台/熔炉等，以及任何能取到
 * 「产物 + 材料清单」的配方）；递归展开到<b>无配方的基础材料</b>为止。第三方机器配方不做虚拟执行
 * （其执行逻辑在机器内部），一律走派发路径（P4b）。
 * <p>
 * <b>关键澄清（勿再误读）</b>：终端的 <b>IP 不是货币</b>，而是"库占用量的度量"
 * （{@code ItemPoints} 给物品定价 → 存入库时记账）。所谓"消耗场域内 IP"就是
 * <b>消耗库里的实物材料</b>（账本随之下降）；真正的费用以<b>赤能源</b>支付，
 * 费率唯一来源是 {@link ItemTerminalFee}（取出 / 存入各一档，按减免件数递减）。
 * 因此本规划器<b>不自造任何汇率</b>：能量成本直接用 {@link ItemTerminalFee} 折算。
 * <p>
 * <b>三笔成本</b>：
 * <ul>
 *   <li>材料 = 配方树的叶子清单（同名合并，数量真实可扣）；</li>
 *   <li>IP = 叶子材料的 IP 之和（<b>仅用于界面显示</b>"这一单动用了多少库容量"）；</li>
 *   <li>时间 = 配方节点数 × {@link #TICKS_PER_STEP}（让耗时随产线深度增长，需求要求显示时间）；</li>
 *   <li>赤能源 = 取出材料的手续费 + 产物入库的手续费（按<b>无减免</b>估算，即上限）。</li>
 * </ul>
 * <p>
 * <b>性能闸门</b>（口径要求必须自补的四道之一：配方树结果缓存）：
 * 配方索引按 {@link ValueCache.Fingerprint} 缓存，配方热重载（数量或内容变化）才重建；
 * 单次求解另有 {@link #MAX_NODES} 与件数相关的批次闸门（{@link #STEP_BUDGET_BASE}）双闸门，超限即判定为"规划失败"，
 * 绝不做无界递归。所有缓存方法都是服务端专有（配方表只在服务端权威）。
 */
public final class VirtualCraftPlanner {

    /** 单次规划的配方节点数上限（跨整棵树累计） */
    private static final int MAX_NODES = 128;
    /**
     * 单次规划的配方<b>批次</b>闸门 = 固定预算 + 每件目标预算。
     * <p>
     * {@link #MAX_NODES} 只数节点，不限每节点要跑几批，而 9 进 1 的压缩链每下一层批次就翻 9 倍 ——
     * 没有这道闸门会出现"耗时数十小时、材料被长期锁死"的单子。
     * 但闸门必须是<b>件数相关</b>的：大批量订单（上限 {@link #MAX_TARGET_COUNT}）本来就该有更多批次，
     * 用固定值会把"999 把镐子"这类正常大单误判成不可加工。
     */
    private static final int STEP_BUDGET_BASE = 4096;
    private static final int STEP_BUDGET_PER_ITEM = 32;
    /**
     * 每个配方节点的耗时（tick）：<b>5 tick / 步</b>（用户口径）。
     * <p>
     * 只算虚拟加工这一侧；机器自身的加工时间不在此列（各机器的处理时长由机器自己定）。
     */
    public static final int TICKS_PER_STEP = 5;

    /**
     * 单笔虚拟加工单的目标件数上限（与界面数量框一致：4 位数）。
     * <p>
     * <b>不能用堆叠上限夹</b>：工具/装甲的 {@code maxStackSize = 1}，那样会把"8 把镐子"夹回 1 件
     * （现象就是"改了数量，需求与产物纹丝不动"）。虚拟加工单本身不是一个物品栈，件数由订单自己的闸门定。
     * <p>
     * 超 127 会牵动网络计数宽度：{@code writeFullStack} 的件数字段必须是 varint（byte 只到 127）。
     */
    public static final int MAX_TARGET_COUNT = 9999;

    /** 叶子材料：物品 + 真实需求量（可能超过单堆上限，故用 long） */
    public record Leaf(Item item, long count) {
    }

    /**
     * 一次规划结果（不可变）。
     *
     * @param target      目标物品（含数量）
     * @param leaves      需要现采的基础材料（同名已合并，按 IP 从高到低排序）
     * @param direct      最上层直接材料（界面显示用，数量 = 该层需求）
     * @param taken       直接从库存吃掉的材料（含中间产物；"下级材料够"就走这里）
     * @param outputs     整批执行的净产出（目标 + 富余中间产物）
     * @param materialIp  本单从库里取走的物品折算出的 IP（= 占用的库容量，界面显示用）
     * @param resultIp    产物的 IP 总量（产物入库的手续费基数）
     * @param totalTicks  总耗时（tick）
     * @param totalEnergy 赤能源消耗上限（按无减免件估算；实际由宿主减免后更少）
     * @param steps       配方节点总数
     */
    public record Plan(ItemStack target, List<Leaf> leaves, List<Leaf> direct, List<Leaf> taken,
                       List<ItemStack> outputs, long materialIp, long resultIp, long totalTicks,
                       long totalEnergy, int steps) {

        /**
         * 本单会从库里移除的全部物品：现采的基础材料 + 直接吃掉的库存材料。
         * <p>
         * 扣料与"够不够"判定都必须用它（只看 {@link #leaves} 会漏掉"库存里直接拿"的那部分）。
         */
        public List<Leaf> consumables() {
            if (taken.isEmpty()) {
                return leaves;
            }
            List<Leaf> all = new ArrayList<>(leaves.size() + taken.size());
            all.addAll(leaves);
            all.addAll(taken);
            return all;
        }
    }

    /**
     * 配方索引缓存：指纹一致即复用（配方热重载后指纹必变）。
     * <p>
     * {@code ordered} 是按注册名排好序的产物列表 —— 配方表本身是 Map，遍历顺序不稳定，
     * 若直接用它搜索，同样的查询两次会得到不同结果（界面列表跳动），故排序一次缓存起来。
     */
    private record CachedIndex(ValueCache.Fingerprint fingerprint, Map<Item, List<RecipeCost>> byResult,
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

    private VirtualCraftPlanner() {
    }

    /**
     * 玩家输入的规划入口：<b>件数按订单闸门夹、NBT 一律丢弃</b>（材料核对是物品级的，
     * 若原样采用客户端传来的栈，等于"基础材料即可造出任意 NBT 物品"，如权限位全开的身份卡）。
     *
     * @param stock 库快照（物品 → 总量）；空表 = 不看库存（纯可解性判定）
     */
    @Nullable
    public static Plan plan(RecipeManager manager, RegistryAccess access, ItemStack target,
            Map<Item, Long> stock) {
        if (target.isEmpty()) {
            return null;
        }
        return plan(manager, access, target.getItem(),
                Math.max(1, Math.min(target.getCount(), MAX_TARGET_COUNT)), stock);
    }

    /**
     * 规划一次虚拟加工（<b>带库存</b>）：<b>逐级先扣库存，缺的才现做</b>。
     * <p>
     * 库存里已有某级材料（如木板、木棍）就直接吃掉，不再从基础材料现做 ——
     * 这样"下级材料够"的物品也能算可制作，且与真实扣料一致（{@code Plan.taken} 就是被吃掉的库存）。
     * 目标物品本身<b>不从库存扣</b>：下单语义是"再做 N 个"，而不是"把已有的算进来"。
     * <p>
     * 件数由调用方给定，<b>不再按订单件数上限夹</b>（内部复核用的数量可能远大于单笔上限，
     * 夹小会得出偏松的"够"）。
     */
    @Nullable
    public static Plan plan(RecipeManager manager, RegistryAccess access, Item item, int amount,
            Map<Item, Long> stock) {
        if (item == null || item == Items.AIR || amount <= 0) {
            return null;
        }
        Map<Item, List<RecipeCost>> byResult = index(manager, access).byResult();
        // 目标本身就是基础材料时不做虚拟加工（没有配方可执行）
        List<RecipeCost> recipes = byResult.get(item);
        if (recipes == null || recipes.isEmpty()) {
            return null;
        }
        Solution solution = solve(byResult, item, amount, stock);
        if (solution == null) {
            return null;
        }
        List<Leaf> leaves = buildLeaves(solution.leafTotals);
        List<Leaf> taken = buildLeaves(solution.takenTotals);
        // 材料 IP = 本单会从库里取走的全部物品（现采的基础材料 + 直接吃掉的库存中间产物）
        long materialIp = 0L;
        for (Leaf leaf : leaves) {
            materialIp += ItemPoints.perItem(new ItemStack(leaf.item())) * leaf.count();
        }
        for (Leaf leaf : taken) {
            materialIp += ItemPoints.perItem(new ItemStack(leaf.item())) * leaf.count();
        }
        // 产物入库手续费按<b>全部净产出</b>算（目标 + 富余中间产物都会真的进库）
        long resultIp = 0L;
        for (ItemStack output : solution.outputs) {
            resultIp += ItemPoints.of(output);
        }
        // 手续费按"无减免件"估算（= 上限）：实际扣费由宿主按自己的减免件数算，只会更少
        long energy = ItemTerminalFee.withdrawCost(materialIp, 0) + ItemTerminalFee.depositCost(resultIp, 0);
        ItemStack head = solution.outputs.isEmpty()
                ? new ItemStack(item, 1) : solution.outputs.get(0);
        return new Plan(head, leaves, List.copyOf(solution.direct), taken, List.copyOf(solution.outputs),
                materialIp, resultIp, (long) solution.steps * TICKS_PER_STEP, energy, solution.steps);
    }

    /**
     * 该物品是否可被虚拟加工（有可解析配方，且配方树能完整展开）。
     * <p>
     * <b>会阻塞</b>建索引（见 {@link #index}）：界面路径请改用
     * {@link #isIndexReady} + {@link #startBuild} + {@link #stepBuild} 的分片流程。
     */
    public static boolean isCraftable(RecipeManager manager, RegistryAccess access, Item item) {
        return index(manager, access).craftableSet().contains(item);
    }

    /**
     * 全部<b>可解</b>的产物物品（按注册名排序，顺序稳定；调用方负责筛选与截断）。
     * <p>
     * 只列"能真正规划成功"的：列表里出现点进去必然失败的条目，等于给了用不了的入口。
     */
    public static List<Item> craftableItems(RecipeManager manager, RegistryAccess access) {
        return index(manager, access).craftable();
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

    /** 索引是否已就绪（就绪即可直接取目录，无需等待分片构建） */
    public static boolean isIndexReady(RecipeManager manager) {
        CachedIndex local = CACHE.get(manager);
        return local != null && local.fingerprint().equals(ValueCache.fingerprint(manager));
    }

    /**
     * 开始（或复用）一次分片构建。
     * <p>
     * <b>为什么要有它</b>：目录首次拉取原本在服务端线程上现场建索引 ——
     * 「收集全部配方 + 逐物品跑一遍可解性预筛」是秒级工作量（实测 1017 项 14.8 秒，且
     * {@code ItemPoints} 热路径重复扫全表是主因，另见 {@code ValueCache#fingerprint} 的复用窗口），
     * 直接把服务端卡住，玩家看到的就是"加工页刚打开没反应、等一会儿才能用"。
     * 改成分片推进后每 tick 只做 {@link #SLICE_NANOS} 一小段，服务端不再出现长阻塞。
     * <p>
     * 阶段 A 一次做完：只是遍历配方表建索引并排序，没有递归，代价与配方数同阶。
     * 阶段 B 交给 {@link #stepBuild}。
     */
    public static void startBuild(RecipeManager manager, RegistryAccess access) {
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
            // 原料格为空的配方不参与虚拟加工：留着它等于留了一条零成本造物的路
            if (cost.ingredients().isEmpty()) {
                continue;
            }
            byResult.computeIfAbsent(cost.resultItem(), key -> new ArrayList<>(2)).add(cost);
        }
        List<Item> ordered = new ArrayList<>(byResult.keySet());
        ordered.sort(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
        PENDING.put(manager, new Pending(fingerprint, Map.copyOf(byResult), List.copyOf(ordered), startedNanos));
    }

    /**
     * 推进一步分片构建（每 tick 调用一次）。
     *
     * @return true = 索引已就绪（本次刚好建完）；false = 仍需等待
     */
    public static boolean stepBuild(RecipeManager manager) {
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
            Solution solution = solve(pending.byResult, item, 1, Map.of());
            if (solution != null && !solution.leafTotals.isEmpty()) {
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

    // ===== 求解：选路 → 需求聚合 → 按层分批 =====

    /**
     * 求解一次虚拟加工。
     * <p>
     * <b>为什么不能逐槽递归展开</b>：那样兄弟槽之间的批次无法复用 —— 一把木镐（3 木板 + 2 木棍）
     * 会变成"3 个木板槽各扣 1 原木 + 2 个木棍槽各扣 2 原木 = 7 原木"，而正确答案是 2 原木
     * （5 木板 ÷ 每批 4 = 2 批）。多扣的材料还会凭空蒸发。
     * <p>
     * 本实现分三步：① 选路（每个物品固定一张配方 + 每格选中的候选物品，<b>多配方时优先"库存里有现货"的那张</b>，
     * 配方环退化为基础材料）；
     * ② 按依赖图<b>拓扑序</b>把需求聚合（同一物品的所有父需求先合并）；
     * ③ 每层按 {@code ceil(需求 ÷ 单批产出)} 分批，批次需求再压给下一层；
     * 叶子结算时，库里已有这个物品就直接吃库存，库里没有但<b>有它的等价形态</b>（铁块 ⇄ 铁锭）就现换一层。
     * <p>
     * <b>净产出按整批执行算</b>：每个被执行的批次都真实产出 {@code 批数 × 单批产出}，
     * 上层消耗一部分，剩下的（含顶层目标的整批）全部入库 —— 物料守恒，不蒸发。
     *
     * @return null = 不可虚拟加工（树里有"没有原料格"的配方，或超出节点/批次闸门）
     */
    @Nullable
    private static Solution solve(Map<Item, List<RecipeCost>> byResult, Item target, int amount,
            Map<Item, Long> stock) {
        Solution solution = new Solution();
        // 批次闸门随件数放宽（见 STEP_BUDGET_BASE 注释）
        int budget = STEP_BUDGET_BASE + amount * STEP_BUDGET_PER_ITEM;
        // ① 选路
        Map<Item, RecipeCost> chosen = new HashMap<>();
        Map<Item, List<Item>> children = new HashMap<>();
        Set<Item> blocked = new HashSet<>();
        if (!route(byResult, target, chosen, children, blocked, new LinkedHashSet<>(), new HashSet<>(), stock)) {
            return null;
        }
        for (Item item : blocked) {
            chosen.remove(item);
            children.remove(item);
        }
        // 目标自己落到环里 ⇒ 只能拿它当材料，等于没得做
        if (!chosen.containsKey(target)) {
            return null;
        }
        // ② 入度 = 有多少个父节点会喂它；父都处理完，需求才确定
        Map<Item, Integer> indegree = new HashMap<>();
        indegree.put(target, 0);
        for (List<Item> picks : children.values()) {
            for (Item pick : picks) {
                indegree.merge(pick, 1, Integer::sum);
            }
        }
        Map<Item, Long> demand = new HashMap<>();
        demand.put(target, (long) amount);
        Deque<Item> ready = new ArrayDeque<>();
        ready.add(target);
        int routed = 0;
        while (!ready.isEmpty()) {
            Item item = ready.poll();
            long need = demand.getOrDefault(item, 0L);
            RecipeCost recipe = chosen.get(item);
            List<Item> picks = children.get(item);
            if (recipe == null || picks == null) {
                continue; // 基础材料：需求已收齐，最后按叶子结算
            }
            if (++routed > MAX_NODES) {
                return null;
            }
            // 逐级先扣库存：库里已有的这一级材料直接吃掉，缺的才现做 ——
            // 于是"下级材料够（如库里有木板/木棍）"的物品也能算可制作，且与真实扣料同源。
            // 目标本身不扣库存（下单语义是"再做 N 个"）。
            long cover = item == target ? 0L
                    : Math.max(0L, Math.min(need, stock.getOrDefault(item, 0L)));
            long missing = need - cover;
            if (cover > 0L) {
                solution.takenTotals.merge(item, cover, Long::sum);
            }
            int per = Math.max(1, recipe.outputCount());
            long batches = ceilDiv(missing, per);
            solution.steps += (int) Math.min(batches, budget);
            if (solution.steps > budget) {
                return null;
            }
            long produced = batches * per;
            if (item == target) {
                // 顶层：整批产出全部入库（请求 1 个也要跑满一批，材料也只按整批扣）
                addOutput(solution, item, produced);
                // 直接材料（显示用）：只给最上面这一层，数量 = 每格 1 × 批数
                Map<Item, Long> direct = new LinkedHashMap<>();
                for (Item pick : picks) {
                    direct.merge(pick, batches, Long::sum);
                }
                direct.forEach((key, value) -> solution.direct.add(new Leaf(key, value)));
            } else {
                long surplus = produced - missing;
                if (surplus > 0L) {
                    addOutput(solution, item, surplus); // 富余的中间产物：一并入库
                }
            }
            for (Item pick : picks) {
                // 这一级被库存覆盖时 batches = 0：不再向下一层要料，但入度照样要还
                //（否则"整棵子树都不需要"的分支会卡在等待队列外，需求结算不全）
                if (batches > 0L) {
                    demand.merge(pick, batches, Long::sum);
                }
                if (indegree.merge(pick, -1, Integer::sum) == 0) {
                    ready.add(pick);
                }
            }
        }
        // ③ 叶子结算：有需求却没有配方的物品（基础材料，或配方环上被剪掉的物品）就是真实消耗。
        // 这里对"配方环"做一次补救：环上的物品（铁锭 ⇄ 铁块）被剪掉配方后本来只能当基础材料要，
        // 于是"库里有铁块"也做不出铁镐 —— 先看库存，不够再拿库存里的等价形态现换一层（见 exchangeFromStock）。
        Map<Item, Long> remaining = null; // 走到"等价兑换"时才复制一份可扣库存，避免多个叶子重复透支同一份料
        for (Map.Entry<Item, Long> entry : demand.entrySet()) {
            long need = entry.getValue();
            Item item = entry.getKey();
            if (need <= 0L || chosen.containsKey(item)) {
                continue;
            }
            Map<Item, Long> pool = remaining == null ? stock : remaining;
            long fromStock = Math.max(0L, Math.min(need, pool.getOrDefault(item, 0L)));
            if (fromStock > 0L) {
                solution.takenTotals.merge(item, fromStock, Long::sum); // 库里已有的：直接吃库存
                if (remaining != null) {
                    remaining.merge(item, -fromStock, Long::sum);
                }
                need -= fromStock;
            }
            if (need > 0L) {
                if (remaining == null) {
                    remaining = new HashMap<>(stock);
                }
                need = exchangeFromStock(byResult, item, need, remaining, solution, budget);
            }
            if (need > 0L) {
                solution.leafTotals.merge(item, need, Long::sum); // 真缺口：交给库存核对判"做不做得出"
            }
        }
        return solution;
    }

    /**
     * 用库存里已有的「等价形态」现换出还缺的 {@code item}（典型：库里有铁块 ⇒ 现换出铁锭）。
     * <p>
     * <b>为什么需要它</b>：铁锭 ⇄ 铁块 在配方图上互为原料，{@link #route} 必须剪掉这种环（否则无限递归），
     * 剪掉的代价是环上的物品只剩"基础材料"一条路 —— 库存里的铁块于是完全用不上（用户实测：
     * 库里有铁块却做不出铁镐）。
     * <p>
     * <b>只做一层</b>：原料<b>必须全部来自库存</b>，绝不递归展开。这是安全边界 ——
     * 环两端互为原料，一旦允许递归就是"用铁块造铁锭、再用铁锭造铁块"的死循环；
     * 只吃库存则天然终止，且与其它节点同一套物料守恒口径（吃掉的记 {@code taken}，整批多出的入库）。
     *
     * @param remaining 可扣库存（调用方的私有副本，会被本方法扣减）
     * @return 仍未解决的缺口（0 = 已全部解决）
     */
    private static long exchangeFromStock(Map<Item, List<RecipeCost>> byResult, Item item, long missing,
            Map<Item, Long> remaining, Solution solution, int budget) {
        List<RecipeCost> recipes = byResult.get(item);
        if (recipes == null || recipes.isEmpty()) {
            return missing;
        }
        RecipeCost best = null;
        List<Item> bestPicks = null;
        long bestBatches = 0L;
        long bestCost = Long.MAX_VALUE;
        for (RecipeCost recipe : recipes) {
            int slots = recipe.ingredients().size();
            if (slots == 0) {
                continue; // 无原料格的配方：放行即凭空造物
            }
            long batches = ceilDiv(missing, Math.max(1, recipe.outputCount()));
            List<Item> picks = new ArrayList<>(slots);
            long cost = 0L;
            boolean satisfiable = true;
            for (Item[] slot : recipe.ingredients()) {
                Item pick = pickStocked(slot, batches, remaining);
                if (pick == null) {
                    satisfiable = false;
                    break;
                }
                picks.add(pick);
                cost += batches; // 每格每批吃 1 个
            }
            // 多张配方都能换：挑"总吃料最少"的那张（同量产出吃得少的更划算）
            if (satisfiable && cost < bestCost) {
                best = recipe;
                bestPicks = picks;
                bestBatches = batches;
                bestCost = cost;
            }
        }
        // 换不出来（库存没有等价形态）或超出批次闸门：留作真缺口，由库存核对判"做不出"
        if (best == null || solution.steps + bestBatches > budget) {
            return missing;
        }
        solution.steps += (int) bestBatches;
        for (Item pick : bestPicks) {
            solution.takenTotals.merge(pick, bestBatches, Long::sum);
            remaining.merge(pick, -bestBatches, Long::sum);
        }
        long surplus = bestBatches * Math.max(1, best.outputCount()) - missing;
        if (surplus > 0L) {
            addOutput(solution, item, surplus); // 整批多出的部分一并入库（物料守恒）
        }
        return 0L;
    }

    /** 该材料格里挑一个「库存够 {@code batches} 个」的候选物品；够不着返回 null */
    @Nullable
    private static Item pickStocked(Item[] slot, long batches, Map<Item, Long> remaining) {
        for (Item candidate : slot) {
            if (remaining.getOrDefault(candidate, 0L) >= batches) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 选路：为每个物品固定一张配方与每格选中的候选物品（传播阶段必须与选路阶段口径一致）。
     * <p>
     * 配方环（A→B→A）上的物品退化为基础材料，避免无限递归；
     * 材料格无候选（空标签等）⇒ 直接判不可虚拟加工。
     */
    private static boolean route(Map<Item, List<RecipeCost>> byResult, Item item,
            Map<Item, RecipeCost> chosen, Map<Item, List<Item>> children, Set<Item> blocked,
            Set<Item> stack, Set<Item> done, Map<Item, Long> stock) {
        if (done.contains(item)) {
            return true;
        }
        List<RecipeCost> recipes = byResult.get(item);
        if (recipes == null || recipes.isEmpty()) {
            done.add(item);
            return true;
        }
        if (stack.contains(item)) {
            blocked.add(item);
            return true;
        }
        RecipeCost recipe = pickRecipe(recipes, stock);
        // 候选配方全都"没有原料格"（特殊/自定义配方）：判不可虚拟加工。
        // 放行就等于"产出不扣任何材料"，是凭空的复制漏洞，宁可不做也不能白送
        if (recipe == null) {
            return false;
        }
        List<Item> picks = new ArrayList<>(recipe.ingredients().size());
        for (Item[] slot : recipe.ingredients()) {
            Item pick = cheapestCandidate(slot);
            if (pick == null) {
                return false;
            }
            picks.add(pick);
        }
        stack.add(item);
        for (Item pick : picks) {
            if (!route(byResult, pick, chosen, children, blocked, stack, done, stock)) {
                return false;
            }
        }
        stack.remove(item);
        done.add(item);
        if (!blocked.contains(item)) {
            chosen.put(item, recipe);
            children.put(item, List.copyOf(picks));
        }
        return true;
    }

    /**
     * 记一笔净产出，并按堆叠上限切块。
     * <p>
     * 必须切块：目标件数上限是订单级的（{@link #MAX_TARGET_COUNT}），而工具等不可堆叠物
     * 单堆只有 1 —— 塞成"镐子 ×64"的一堆，入库与落地掉落都会出问题。
     */
    private static void addOutput(Solution solution, Item item, long count) {
        int per = Math.max(1, item.getMaxStackSize());
        long remaining = count;
        while (remaining > 0L) {
            solution.outputs.add(new ItemStack(item, (int) Math.min(remaining, per)));
            remaining -= per;
        }
    }

    /**
     * 多配方取一：优先<b>全材料格在库存里有现货</b>的那张，其次<b>材料格数最少</b>（展开面最小），
     * 并列时取<b>候选单价之和最低</b>。
     * <p>
     * <b>为什么要看库存</b>：同一物品常有多张配方（铁锭：铁块→9 铁锭 / 粗铁→铁锭 / 铁矿石→铁锭）。
     * 只按"格数 + 单价"选，会挑中单价最低的熔炼路线；玩家库里明明堆着铁块，却因为"没有粗铁"
     * 被判成做不出铁镐（用户实测）。先按现货挑，才能真正用上库存里的等价形态。
     * <p>
     * 刻意不做"递归比价"（要对每张配方都跑一遍树，代价随配方数放大）——
     * 虚拟加工给的是<b>可预期</b>成本，不追求全局最优。
     *
     * @return 选中的配方；<b>全部候选都没有原料格时返回 null</b>（由调用方判为不可加工）
     */
    @Nullable
    private static RecipeCost pickRecipe(List<RecipeCost> recipes, Map<Item, Long> stock) {
        RecipeCost best = null;
        boolean bestStocked = false;
        int bestSlots = Integer.MAX_VALUE;
        long bestPrice = Long.MAX_VALUE;
        for (RecipeCost recipe : recipes) {
            int slots = recipe.ingredients().size();
            if (slots == 0) {
                continue;
            }
            long price = 0L;
            for (Item[] slot : recipe.ingredients()) {
                Item pick = cheapestCandidate(slot);
                if (pick != null) {
                    price += ItemPoints.perItem(new ItemStack(pick));
                }
            }
            boolean stocked = isStocked(recipe, stock);
            boolean better;
            if (stocked != bestStocked) {
                better = stocked; // 现货优先
            } else if (slots != bestSlots) {
                better = slots < bestSlots;
            } else {
                better = price < bestPrice;
            }
            if (best == null || better) {
                best = recipe;
                bestStocked = stocked;
                bestSlots = slots;
                bestPrice = price;
            }
        }
        return best;
    }

    /** 该配方是否"每一格都有现货"（每格至少一个候选物品在库存里）；空库存恒 false（= 不看库存，行为不变） */
    private static boolean isStocked(RecipeCost recipe, Map<Item, Long> stock) {
        if (stock.isEmpty()) {
            return false;
        }
        for (Item[] slot : recipe.ingredients()) {
            boolean covered = false;
            for (Item candidate : slot) {
                if (stock.getOrDefault(candidate, 0L) > 0L) {
                    covered = true;
                    break;
                }
            }
            if (!covered) {
                return false;
            }
        }
        return true;
    }

    /** 标签候选里取 IP 单价最低者（标签常含多种可替代物品，取最省的作为估价口径） */
    @Nullable
    private static Item cheapestCandidate(Item[] candidates) {
        Item best = null;
        long bestPrice = Long.MAX_VALUE;
        for (Item item : candidates) {
            long price = ItemPoints.perItem(new ItemStack(item));
            if (price < bestPrice) {
                best = item;
                bestPrice = price;
            }
        }
        return best;
    }

    /** 叶子清单：同名合并后按 IP 从高到低排，界面第一眼看到最贵的材料；数量保留真实值供扣料 */
    private static List<Leaf> buildLeaves(Map<Item, Long> totals) {
        List<Map.Entry<Item, Long>> sorted = new ArrayList<>(totals.entrySet());
        sorted.sort((a, b) -> Long.compare(
                ItemPoints.perItem(new ItemStack(b.getKey())) * b.getValue(),
                ItemPoints.perItem(new ItemStack(a.getKey())) * a.getValue()));
        List<Leaf> leaves = new ArrayList<>(sorted.size());
        for (Map.Entry<Item, Long> entry : sorted) {
            leaves.add(new Leaf(entry.getKey(), Math.max(1L, entry.getValue())));
        }
        return List.copyOf(leaves);
    }

    // ===== 配方索引缓存 =====

    /**
     * 取"产物 → 配方"索引，指纹变化才重建。
     * <p>
     * 复用 {@link ValueCache#fingerprint(RecipeManager)}：它用"配方数 + 与顺序无关的内容哈希"，
     * 数据包热重载（数量不变但内容变了）同样能识别 —— 与估值缓存同一套失效判据，不另造轮子。
     */
    private static CachedIndex index(RecipeManager manager, RegistryAccess access) {
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
                // 原料格为空的配方不参与虚拟加工：留着它等于留了一条零成本造物的路
                if (cost.ingredients().isEmpty()) {
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
                Solution solution = solve(byResult, item, 1, Map.of());
                if (solution != null && !solution.leafTotals.isEmpty()) {
                    craftable.add(item);
                }
            }
            CachedIndex built = new CachedIndex(fingerprint, Map.copyOf(byResult),
                    List.copyOf(craftable), Set.copyOf(craftable));
            CACHE.put(manager, built);
            return built;
        }
    }

    /** 主动失效（调试命令/热重载钩子可用） */
    public static void invalidate() {
        synchronized (CACHE) {
            CACHE.clear();
        }
        PENDING.clear(); // 进行中的分片构建一并丢弃：它的指纹已经作废
    }

    private static long ceilDiv(long value, long divisor) {
        return (value + divisor - 1L) / divisor;
    }

    /** 单次求解的中间结果：叶子台账 / 直接材料（显示用）/ 净产出（入库用）/ 批数 */
    private static final class Solution {
        /** 需要现采的基础材料（各物品需求总量） */
        private final Map<Item, Long> leafTotals = new LinkedHashMap<>();
        /** 直接从库存吃掉的材料（各物品被吃掉的总量） */
        private final Map<Item, Long> takenTotals = new LinkedHashMap<>();
        private final List<Leaf> direct = new ArrayList<>();
        private final List<ItemStack> outputs = new ArrayList<>();
        private int steps;
    }
}
