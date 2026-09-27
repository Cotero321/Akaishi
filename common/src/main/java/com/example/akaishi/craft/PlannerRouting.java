package com.example.akaishi.craft;

import com.example.akaishi.value.ItemPoints;
import com.example.akaishi.value.RecipeIngredients.RecipeCost;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 虚拟加工规划的「配方匹配（选路）」协作类（自 {@link VirtualCraftPlanner} 拆出，包私有）：
 * 为每个物品固定一张配方与每格选中的候选物品 —— 配方环剪枝退化为基础材料、深度闸门防递归爆栈；
 * 多配方时按「现货优先 + 综合分（材料 / 时间 / 赤能源折算等价材料件数）」取一。
 * 公开入口仍在 {@link VirtualCraftPlanner}，本类只承接实现。
 */
final class PlannerRouting {

    private PlannerRouting() {
    }

    /** 选路评分：每格材料折算的等价件数（格数代表展开面，格多则递归树更易膨胀） */
    private static final double SLOT_WEIGHT = 1.0D;
    /** 选路评分：时间折算比例，20 tick（1 秒）≈ 1 件材料 */
    private static final double TICKS_PER_MATERIAL_UNIT = 20.0D;
    /** 选路评分：赤能源折算比例，1500 赤能源 ≈ 1 件材料（单槽族一批的默认能耗） */
    private static final double CHISHI_PER_MATERIAL_UNIT = 1500.0D;

    /**
     * 选一次路，结果写进入参三张表。
     *
     * <p><b>返回值语义</b>：true = 选出的路可用（{@code chosen} 里有目标）；false = 这条路不可用，
     * 三张表会被清空 —— 只走了一半就硬失败（某格没有候选、某张配方没有原料格）时，
     * 残留的半个树会被后续当成有效选路，必须清掉。
     *
     * <p>{@code stock} 只用于"现货优先"，传空表即"不看库存"的普通选路。
     */
    static boolean tryRoute(Map<Item, List<RecipeCost>> byResult, Item target, Map<Item, Long> stock,
            Map<Item, RecipeCost> chosen, Map<Item, List<Item>> children, Set<Item> blocked,
            ProcessCoverage coverage) {
        chosen.clear();
        children.clear();
        blocked.clear();
        if (!route(byResult, target, chosen, children, blocked, new LinkedHashSet<>(), new HashSet<>(), stock,
                coverage)) {
            chosen.clear();
            children.clear();
            blocked.clear();
            return false;
        }
        for (Item item : blocked) {
            chosen.remove(item);
            children.remove(item);
        }
        return chosen.containsKey(target);
    }

    /**
     * 选路：为每个物品固定一张配方与每格选中的候选物品（传播阶段必须与选路阶段口径一致）。
     * <p>
     * 配方环（A→B→A）上的物品退化为基础材料，避免无限递归；
     * 材料格无候选（空标签等）⇒ 直接判不可虚拟加工。
     */
    private static boolean route(Map<Item, List<RecipeCost>> byResult, Item item,
            Map<Item, RecipeCost> chosen, Map<Item, List<Item>> children, Set<Item> blocked,
            Set<Item> stack, Set<Item> done, Map<Item, Long> stock, ProcessCoverage coverage) {
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
        // 深度闸门：route 是深递归，而 MAX_NODES 只在传播阶段计数（选路跑在它之前）。
        // 没有这一层，超长的配方链会在服务端线程上递归爆栈 —— StackOverflowError 是 Error，
        // 现有的 try/catch 兜不住整帧。超深者按"环上物品"同口径退化为需要现货的基础材料。
        if (stack.size() >= PlannerSolve.MAX_NODES) {
            blocked.add(item);
            return true;
        }
        RecipeCost recipe = pickRecipe(recipes, stock, coverage);
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
            if (!route(byResult, pick, chosen, children, blocked, stack, done, stock, coverage)) {
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
     * 多配方取一（<b>最佳线路</b>）：先取<b>全材料格在库存里有现货</b>的那张，看库存相同则取
     * {@link #routeScore} <b>综合分最低</b>的那张。
     * <p>
     * <b>为什么要看库存</b>：同一物品常有多张配方（铁锭：铁块→9 铁锭 / 粗铁→铁锭 / 铁矿石→铁锭）。
     * 不看库存会挑中"单价更低但库里没料"的路线；玩家库里明明堆着铁块，却因为"没有粗铁"被判成
     * 做不出铁镐（用户实测）。现货优先保留为第一关键字。
     * <p>
     * <b>综合分口径</b>（用户拍板"时间 + 能量 + 材料"）见 {@link #routeScore}：
     * 材料用 {@link ItemPoints} 折算（它本身是<b>递归传播</b>出来的价，所以材料这一项是全局最优的），
     * 时间与能量只算<b>该配方自身</b>一层（子树的时间/能量不做递归 —— 那要对每张候选跑一遍展开，
     * 目录构建会被放大到上千倍）。
     *
     * @return 选中的配方；<b>全部候选都是"无原料格且不耗能"时返回 null</b>（由调用方判为不可加工）
     */
    @Nullable
    private static RecipeCost pickRecipe(List<RecipeCost> recipes, Map<Item, Long> stock,
            ProcessCoverage coverage) {
        RecipeCost best = null;
        boolean bestStocked = false;
        double bestScore = Double.MAX_VALUE;
        for (RecipeCost recipe : recipes) {
            if (PlannerSettlement.isUnsafeEmpty(recipe)) {
                continue;
            }
            boolean stocked = isStocked(recipe, stock);
            double score = routeScore(recipe, coverage);
            boolean better;
            if (stocked != bestStocked) {
                better = stocked; // 现货优先
            } else {
                better = score < bestScore;
            }
            if (best == null || better) {
                best = recipe;
                bestStocked = stocked;
                bestScore = score;
            }
        }
        return best;
    }

    /**
     * 候选配方的综合分（越小越好）：把<b>材料 / 时间 / 赤能源</b>都折成"等价材料件数"再相加，
     * 折算是为了让三者可比 —— {@code 1 秒 ≈ 1 件材料}、{@code 1500 赤能源 ≈ 1 件材料}
     * （1500 恰是单槽族一批的默认能耗，口径直观）。
     * <p>
     * <b>用 double 而不是 long</b>：整数除法会把 {@code 20 tick} 以下的时间、{@code 1500 赤能源}
     * 以下的能耗一律截成 0，快机器/低能耗配方之间的差别被抹平，选路退化成"只看材料"。
     * <p>
     * <b>格数惩罚</b>：每格材料另计 {@link #SLOT_WEIGHT}。"格"本身不是成本，但它代表展开面 ——
     * 格数多的配方会让递归树的分支成倍膨胀，极端情况会撞 {@link PlannerSolve#MAX_NODES} / 批次闸门，
     * 把本来做得出的物品判成做不了。给一点权重即可让同分的路线偏向紧凑的那张。
     * <p>
     * 机器时间/能耗取该族<b>最快的那台机台</b>做代表（"这条路最少要花多少"），
     * 实际结算仍按 {@link MachineScheduler} 分轮调度，两者口径不冲突：这里只用于<b>选路比较</b>。
     */
    private static double routeScore(RecipeCost recipe, ProcessCoverage coverage) {
        double score = 0.0D;
        int slots = 0;
        for (Item[] slot : recipe.ingredients()) {
            slots++;
            Item pick = cheapestCandidate(slot);
            if (pick != null) {
                score += ItemPoints.perItem(new ItemStack(pick));
            }
        }
        score += slots * SLOT_WEIGHT;
        Recipe<?> source = recipe.recipe();
        RecipeType<?> kind = MachineProcessEnergy.machineKind(source);
        if (kind == null) {
            return score; // 原版配方：不占机械工时与能耗
        }
        MachineSpec fastest = MachineSpec.BASE;
        for (MachineSpec spec : coverage.machinesOf(kind)) {
            if (spec.speedCount() > fastest.speedCount()) {
                fastest = spec;
            }
        }
        score += MachineProcessEnergy.ticksPerRun(source, fastest) / TICKS_PER_MATERIAL_UNIT;
        score += MachineProcessEnergy.costPerRun(source, fastest).chishi() / CHISHI_PER_MATERIAL_UNIT;
        return score;
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
}
