package com.example.akaishi.craft;

import com.example.akaishi.craft.VirtualCraftPlanner.Leaf;
import com.example.akaishi.craft.VirtualCraftPlanner.Step;
import com.example.akaishi.value.RecipeIngredients.RecipeCost;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 虚拟加工规划的「求解调度」协作类（自 {@link VirtualCraftPlanner} 拆出，包私有）：
 * ① 选路（委托 {@link PlannerRouting}）→ ② 按依赖图拓扑序聚合需求、逐级先扣库存 →
 * ③ 按层分批并把批次需求压给下一层 → 叶子结算（含等价形态现换）→ ④ 关键路径计时。
 * 节点 / 批次双闸门在此执行；结果经 {@link Solution} 交给 {@link VirtualCraftPlanner} 结算成 {@link VirtualCraftPlanner.Plan}。
 */
final class PlannerSolve {

    /** 单次规划的配方节点数上限（跨整棵树累计） */
    static final int MAX_NODES = 128;
    /**
     * 单次规划的配方<b>批次</b>闸门 = 固定预算 + 每件目标预算。
     * <p>
     * {@link #MAX_NODES} 只数节点，不限每节点要跑几批，而 9 进 1 的压缩链每下一层批次就翻 9 倍 ——
     * 没有这道闸门会出现"耗时数十小时、材料被长期锁死"的单子。
     * 但闸门必须是<b>件数相关</b>的：大批量订单（上限 {@link VirtualCraftPlanner#MAX_TARGET_COUNT}）本来就该有更多批次，
     * 用固定值会把"999 把镐子"这类正常大单误判成不可加工。
     */
    private static final int STEP_BUDGET_BASE = 4096;
    private static final int STEP_BUDGET_PER_ITEM = 32;

    private PlannerSolve() {
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
    static Solution solve(Map<Item, List<RecipeCost>> byResult, Item target, int amount,
            Map<Item, Long> stock, ProcessCoverage coverage) {
        Solution solution = new Solution();
        // 批次闸门随件数放宽（见 STEP_BUDGET_BASE 注释）。
        // 用 long 计算后再夹：amount 上限 9999 且会被外部传接近 int 上限的值，int 相乘会溢出成负数，
        // 表现成"预算为负 ⇒ 一律判不可解"的保守错判
        long budgetWide = (long) STEP_BUDGET_BASE + (long) amount * STEP_BUDGET_PER_ITEM;
        int budget = (int) Math.min(Integer.MAX_VALUE, Math.max(STEP_BUDGET_BASE, budgetWide));
        // ① 选路
        Map<Item, RecipeCost> chosen = new HashMap<>();
        Map<Item, List<Item>> children = new HashMap<>();
        Set<Item> blocked = new HashSet<>();
        // 选路带"现货优先"（看库存决定用哪张配方/哪一格用谁），而列表预筛用的是<b>空库存</b> ——
        // 两者一旦选出不同的路，就可能出现"列表点得进来、详情却说做不出"（实测：骨粉 ⇄ 骨块是配方环，
        // 库里有骨块时现货优先把目标引上环，环被剪掉后目标就没了）。
        // 补救：按真实库存选路失败时，退回"不看库存"的普通选路再选一次 —— 那正是列表预筛跑的同一条路，
        // 于是"列得出来的必然给得出账"成立。换路只影响"用哪张配方、哪一格用谁"，
        // 叶子结算与扣料仍按真实库存算，成本不会失真。
        if (!PlannerRouting.tryRoute(byResult, target, stock, chosen, children, blocked, coverage)
                && !stock.isEmpty()) {
            PlannerRouting.tryRoute(byResult, target, Map.of(), chosen, children, blocked, coverage);
        }
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
            // 闸门必须是<b>真上限</b>：先判"这一个节点的批数放不放得下"，放不下直接判不可解。
            // 用 min(batches, budget) 记账是错的 —— 记账被夹到预算、真实 batches 却照旧往下推，
            // 于是单节点就能把 demand / outputs 放大到十万级（产出按堆叠上限切块 ⇒ 十万个 ItemStack
            // 随任务常驻，完成时还要逐堆入库）。旧写法只保证"累计计数不超"，拦不住这一次的爆炸。
            if (batches > budget - solution.steps) {
                return null;
            }
            solution.steps += (int) batches;
            // 机器成本按<b>批次</b>累加（每批真跑一次机器）；被库存覆盖的批次 batches = 0，不计成本。
            // 同时把「本节点自身耗时（步时 + 机器墙钟）」记进依赖图，供最后算关键路径
            PlannerSettlement.recordNode(solution, item, recipe, batches, coverage);
            PlannerSettlement.addByproduct(solution, recipe, batches);
            long produced = batches * per;
            // 每格消耗数量（如压缩机 9 粉 → 1 块）：Ingredient 表达不了，须由配方声明，
            // 否则虚拟加工会把"9 粉压 1 块"算成"1 粉压 1 块"，凭空放大产物
            // 输入保留型配方（培养机 consume_input=false）：机器只要求"有料在场"且从不扣除，
            // 故整单只需 inputCount 件，按批数乘会把同一份种子重复要 N 遍（玩家白丢材料）
            boolean inputKept = PlannerSettlement.isInputKept(recipe);
            int[] perSlotCounts = PlannerSettlement.slotCounts(recipe);
            List<Long> slotUnits = new ArrayList<>(picks.size());
            for (int slot = 0; slot < picks.size(); slot++) {
                int perSlot = slot < perSlotCounts.length ? perSlotCounts[slot] : 1;
                slotUnits.add(batches <= 0L ? 0L : inputKept ? perSlot : batches * perSlot);
            }
            // 真机加工的执行节点就记在这一处：上面的 batches/units/picks 是唯一权威，
            // 执行器不再自己推账（只按它投料与对账）。batches = 0（被库存覆盖）的节点没有机器动作，不记。
            if (batches > 0L) {
                solution.order.add(new Step(item, recipe, batches, List.copyOf(slotUnits), List.copyOf(picks),
                        inputKept, solution.nodeWall.getOrDefault(item, 0L)));
            }
            if (item == target) {
                // 顶层：整批产出全部入库（请求 1 个也要跑满一批，材料也只按整批扣）
                PlannerSettlement.addOutput(solution, item, produced);
                // 直接材料（显示用）：只给最上面这一层，数量 = 每格消耗 × 批数
                Map<Item, Long> direct = new LinkedHashMap<>();
                for (int slot = 0; slot < picks.size(); slot++) {
                    direct.merge(picks.get(slot), slotUnits.get(slot), Long::sum);
                }
                direct.forEach((key, value) -> solution.direct.add(new Leaf(key, value)));
            } else {
                long surplus = produced - missing;
                if (surplus > 0L) {
                    PlannerSettlement.addOutput(solution, item, surplus); // 富余的中间产物：一并入库
                }
            }
            for (int slot = 0; slot < picks.size(); slot++) {
                // 这一级被库存覆盖时 batches = 0：不再向下一层要料，但入度照样要还
                //（否则"整棵子树都不需要"的分支会卡在等待队列外，需求结算不全）
                long units = slotUnits.get(slot);
                if (units > 0L) {
                    demand.merge(picks.get(slot), units, Long::sum);
                }
                if (indegree.merge(picks.get(slot), -1, Integer::sum) == 0) {
                    ready.add(picks.get(slot));
                }
            }
        }
        // ③ 叶子结算：有需求却没有配方的物品（基础材料，或配方环上被剪掉的物品）就是真实消耗。
        // 这里对"配方环"做一次补救：环上的物品（铁锭 ⇄ 铁块）被剪掉配方后本来只能当基础材料要，
        // 于是"库里有铁块"也做不出铁镐 —— 先看库存，不够再拿库存里的等价形态现换一层（见 exchangeFromStock）。
        // remaining 从一开始就是 stock 的一份"可扣副本"：叶子按顺序吃库存，必须统一在这份副本上扣。
        // 原先懒创建会导致"先吃库存的叶子"扣了 takenTotals 却没扣副本，
        // 之后 new HashMap<>(stock) 复制出来的仍是原量 ⇒ 同一批库存被后续叶子再计一遍（把能做判成缺料）。
        Map<Item, Long> remaining = new HashMap<>(stock);
        for (Map.Entry<Item, Long> entry : demand.entrySet()) {
            long need = entry.getValue();
            Item item = entry.getKey();
            if (need <= 0L || chosen.containsKey(item)) {
                continue;
            }
            long fromStock = Math.max(0L, Math.min(need, remaining.getOrDefault(item, 0L)));
            if (fromStock > 0L) {
                solution.takenTotals.merge(item, fromStock, Long::sum); // 库里已有的：直接吃库存
                remaining.merge(item, -fromStock, Long::sum);
                need -= fromStock;
            }
            if (need > 0L) {
                need = exchangeFromStock(byResult, item, need, remaining, solution, budget, coverage);
            }
            if (need > 0L) {
                solution.leafTotals.merge(item, need, Long::sum); // 真缺口：交给库存核对判"做不做得出"
            }
        }
        // ④ 总耗时 = 依赖图上的<b>关键路径</b>：不同族的节点本可并行（熔炼与工作台互不等待），
        // 简单求和会把多分支的配方算成"串行"，属于保守高估
        solution.criticalTicks = criticalPath(solution.nodeWall, children);
        return solution;
    }

    /**
     * 关键路径（最长路）：{@code 完工 = 本节点耗时 + max(各子节点完工)}。
     * <p>
     * <b>为什么不是求和</b>：兄弟分支之间没有先后关系（木板与木棍可以各做各的），
     * 只有"父要等子喂料"这一条约束，因此一次订单的最短总耗时是<b>最长的那条依赖链</b>。
     * <p>
     * 依赖图在选路阶段已剪掉配方环（环上物品被 {@link PlannerRouting#route} 记为 blocked 并移出 {@code children}），
     * 故是 DAG；{@code visiting} 只是兜底的防环护栏，正常不会命中。
     */
    private static long criticalPath(Map<Item, Long> nodeWall, Map<Item, List<Item>> children) {
        Map<Item, Long> finish = new HashMap<>();
        Set<Item> visiting = new HashSet<>();
        long longest = 0L;
        for (Item item : nodeWall.keySet()) {
            longest = Math.max(longest, finishOf(item, nodeWall, children, finish, visiting));
        }
        for (Item item : children.keySet()) {
            longest = Math.max(longest, finishOf(item, nodeWall, children, finish, visiting));
        }
        return longest;
    }

    private static long finishOf(Item item, Map<Item, Long> nodeWall, Map<Item, List<Item>> children,
            Map<Item, Long> finish, Set<Item> visiting) {
        Long cached = finish.get(item);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(item)) {
            return 0L; // 护栏：真图上不该有环
        }
        long tail = 0L;
        List<Item> picks = children.get(item);
        if (picks != null) {
            for (Item child : picks) {
                tail = Math.max(tail, finishOf(child, nodeWall, children, finish, visiting));
            }
        }
        visiting.remove(item);
        // 被库存完全覆盖的节点没有 nodeWall 记录：它只是"过路节点"，自身耗时 0
        long total = MachineProcessEnergy.saturatingAdd(nodeWall.getOrDefault(item, 0L), tail);
        finish.put(item, total);
        return total;
    }

    /**
     * 用库存里已有的「等价形态」现换出还缺的 {@code item}（典型：库里有铁块 ⇒ 现换出铁锭）。
     * <p>
     * <b>为什么需要它</b>：铁锭 ⇄ 铁块 在配方图上互为原料，{@link PlannerRouting#route} 必须剪掉这种环（否则无限递归），
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
            Map<Item, Long> remaining, Solution solution, int budget, ProcessCoverage coverage) {
        List<RecipeCost> recipes = byResult.get(item);
        if (recipes == null || recipes.isEmpty()) {
            return missing;
        }
        RecipeCost best = null;
        List<Item> bestPicks = null;
        long bestBatches = 0L;
        long[] bestUnits = null;
        long bestCost = Long.MAX_VALUE;
        for (RecipeCost recipe : recipes) {
            int slots = recipe.ingredients().size();
            if (PlannerSettlement.isUnsafeEmpty(recipe)) {
                continue; // 无原料格且不合法：放行即凭空造物（见 isUnsafeEmpty）
            }
            long batches = ceilDiv(missing, Math.max(1, recipe.outputCount()));
            // 每格消耗数量由配方逐格声明（IProcessSlotCounts），Ingredient 本身表达不了；
            // 输入保留型配方：只按在场量要一份，不随批数翻倍
            boolean inputKept = PlannerSettlement.isInputKept(recipe);
            int[] perSlotCounts = PlannerSettlement.slotCounts(recipe);
            long[] units = new long[slots];
            List<Item> picks = new ArrayList<>(slots);
            long cost = 0L;
            boolean satisfiable = true;
            for (int slot = 0; slot < slots; slot++) {
                int perSlot = slot < perSlotCounts.length ? perSlotCounts[slot] : 1;
                long unit = inputKept ? perSlot : batches * perSlot;
                Item pick = pickStocked(recipe.ingredients().get(slot), unit, remaining);
                if (pick == null) {
                    satisfiable = false;
                    break;
                }
                units[slot] = unit;
                picks.add(pick);
                cost += unit;
            }
            // 多张配方都能换：挑"总吃料最少"的那张（同量产出吃得少的更划算）
            if (satisfiable && cost < bestCost) {
                best = recipe;
                bestPicks = picks;
                bestBatches = batches;
                bestUnits = units;
                bestCost = cost;
            }
        }
        // 换不出来（库存没有等价形态）或超出批次闸门：留作真缺口，由库存核对判"做不出"
        if (best == null || solution.steps + bestBatches > budget) {
            return missing;
        }
        solution.steps += (int) bestBatches;
        // 等价兑换也要跑机器（如 1 块 → 9 锭），成本与耗时按同一口径计入
        PlannerSettlement.recordNode(solution, item, best, bestBatches, coverage);
        PlannerSettlement.addByproduct(solution, best, bestBatches);
        for (int i = 0; i < bestPicks.size(); i++) {
            long unit = bestUnits[i];
            solution.takenTotals.merge(bestPicks.get(i), unit, Long::sum);
            remaining.merge(bestPicks.get(i), -unit, Long::sum);
        }
        long surplus = bestBatches * Math.max(1, best.outputCount()) - missing;
        if (surplus > 0L) {
            PlannerSettlement.addOutput(solution, item, surplus); // 整批多出的部分一并入库（物料守恒）
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

    private static long ceilDiv(long value, long divisor) {
        return (value + divisor - 1L) / divisor;
    }

    /** 单次求解的中间结果：叶子台账 / 直接材料（显示用）/ 净产出（入库用）/ 批数与机器成本 */
    static final class Solution {
        /** 需要现采的基础材料（各物品需求总量） */
        final Map<Item, Long> leafTotals = new LinkedHashMap<>();
        /** 直接从库存吃掉的材料（各物品被吃掉的总量） */
        final Map<Item, Long> takenTotals = new LinkedHashMap<>();
        final List<Leaf> direct = new ArrayList<>();
        final List<ItemStack> outputs = new ArrayList<>();
        int steps;
        /** 机器加工成本（赤能源 + 生命能量）：Σ 批数 × 单批成本 */
        MachineProcessEnergy.Cost machineCost = MachineProcessEnergy.Cost.ZERO;
        /** 本单用到的机械工序族（去重；只有本模组的机器配方才算） */
        final Set<RecipeType<?>> machineKinds = new HashSet<>();
        /** 每个配方节点<b>自身</b>的耗时（tick）：批数 × 步时 + 该节点机器墙钟（被库存覆盖的节点不记） */
        final Map<Item, Long> nodeWall = new HashMap<>();
        /** 执行序（<b>自顶向下</b>的拓扑序：父在前）：真机加工在 buildPlan 时反转成"依赖优先" */
        final List<Step> order = new ArrayList<>();
        /** 总耗时（tick）：依赖图上的关键路径（最长路），在 {@link #criticalPath} 里算 */
        long criticalTicks;
    }
}
