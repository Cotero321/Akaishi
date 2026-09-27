package com.example.akaishi.craft;

import com.example.akaishi.api.recipe.IProcessSlotCounts;
import com.example.akaishi.craft.PlannerSolve.Solution;
import com.example.akaishi.craft.VirtualCraftPlanner.Leaf;
import com.example.akaishi.craft.recipe.AkaishiItemProcessRecipe;
import com.example.akaishi.craft.recipe.IAkaishiMachineRecipe;
import com.example.akaishi.value.ItemPoints;
import com.example.akaishi.value.RecipeIngredients;
import com.example.akaishi.value.RecipeIngredients.RecipeCost;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 虚拟加工规划的「结算与产出」协作类（自 {@link VirtualCraftPlanner} 拆出，包私有）：
 * 叶子清单排序（{@link #buildLeaves}）、净产出记账与堆叠切块（{@link #addOutput} / {@link #addByproduct}）、
 * 节点成本与墙钟（{@link #recordNode}）、输入保留判据（{@link #isInputKept}）、
 * 「无原料格」准入闸门（{@link #isUnsafeEmpty}）。
 * 被求解（{@link PlannerSolve}）、索引（PlannerIndex）与公开入口（{@link VirtualCraftPlanner}）共用。
 */
final class PlannerSettlement {

    private PlannerSettlement() {
    }

    /** 叶子清单：同名合并后按 IP 从高到低排，界面第一眼看到最贵的材料；数量保留真实值供扣料 */
    static List<Leaf> buildLeaves(Map<Item, Long> totals) {
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

    /**
     * 记一笔净产出，并按堆叠上限切块。
     * <p>
     * 必须切块：目标件数上限是订单级的（{@link VirtualCraftPlanner#MAX_TARGET_COUNT}），而工具等不可堆叠物
     * 单堆只有 1 —— 塞成"镐子 ×64"的一堆，入库与落地掉落都会出问题。
     */
    static void addOutput(Solution solution, Item item, long count) {
        int per = Math.max(1, item.getMaxStackSize());
        long remaining = count;
        while (remaining > 0L) {
            solution.outputs.add(new ItemStack(item, (int) Math.min(remaining, per)));
            remaining -= per;
        }
    }

    /**
     * 记一批的<b>副产物</b>（如活化分馏器的衰竭结晶）：它不占原料格、也没有上层会消耗，
     * 因此每批产出的全部进库 —— 必须计入 {@code outputs}，否则"配方说会副产、虚拟加工却不产"。
     * <p>批数 0（被库存覆盖）不产副产，与机器一致。
     */
    static void addByproduct(Solution solution, RecipeCost recipe, long batches) {
        if (batches <= 0L || !(recipe.recipe() instanceof AkaishiItemProcessRecipe process)) {
            return;
        }
        ItemStack byproduct = process.byproduct();
        if (byproduct.isEmpty()) {
            return;
        }
        addOutput(solution, byproduct.getItem(),
                MachineProcessEnergy.saturatingMultiply(batches, byproduct.getCount()));
    }

    /**
     * 记一个配方节点的成本与自身耗时（能量 + 工序族 + 墙钟），供最后算关键路径。
     * <p>单批值实时从 {@link MachineProcessEnergy} 求取（配置热重载即时生效，故不能烘进配方索引缓存）；
     * 累加走饱和运算，防配置填到上限时溢出成负数。
     * <p>节点墙钟 = 步时（{@code 批数 × }{@link VirtualCraftPlanner#TICKS_PER_STEP}）+ 该节点机器墙钟；
     * 批数 0（被库存完全覆盖）记 0 即可 —— 它在关键路径里只是"过路节点"。
     */
    static void recordNode(Solution solution, Item item, RecipeCost recipe, long batches,
            ProcessCoverage coverage) {
        if (batches <= 0L) {
            return;
        }
        long wall = MachineProcessEnergy.saturatingMultiply(batches, VirtualCraftPlanner.TICKS_PER_STEP);
        Recipe<?> source = recipe.recipe();
        RecipeType<?> kind = MachineProcessEnergy.machineKind(source);
        if (kind != null) {
            solution.machineKinds.add(kind);
            // 成本/耗时按该族机台的<b>真实升级与台数</b>核算：多台并行跑，
            // 墙钟取最晚完工的那台，能耗取各机之和（升级不同时，多分活给快机会同时省时间与能耗）
            MachineScheduler.Workout workout = MachineScheduler.schedule(source, coverage.machinesOf(kind), batches);
            solution.machineCost = solution.machineCost.plus(workout.cost());
            wall = MachineProcessEnergy.saturatingAdd(wall, workout.wallTicks());
        }
        solution.nodeWall.put(item, wall);
    }

    /**
     * 该配方是否「输入保留」（培养机那类 {@code consume_input=false}）：机器只在开工时看"有没有料"，
     * 且<b>从不扣除</b>原料。换算"要多少料"时必须按在场量算一份，不能随批数翻倍。
     */
    static boolean isInputKept(RecipeCost recipe) {
        return recipe.recipe() instanceof IAkaishiMachineRecipe machine && !machine.consumeInput();
    }

    /**
     * <b>逐格</b>消耗数量（与 {@link RecipeCost#ingredients()} 同序等长）。
     * <p>
     * 优先读配方的 {@link IProcessSlotCounts}（双原料机器：A 要 9、B 要 4 这类每格不同）；
     * 未实现 / 长度不符时一律回退为 {@link RecipeCost#inputCount()} 的均匀口径 ——
     * 既有单原料配方（length 1）与它逐位等价，故老配方行为不变。
     */
    static int[] slotCounts(RecipeCost recipe) {
        int slots = recipe.ingredients().size();
        if (recipe.recipe() instanceof IProcessSlotCounts declared) {
            int[] counts = declared.inputCounts();
            if (counts != null && counts.length == slots) {
                return counts;
            }
        }
        int[] uniform = new int[slots];
        java.util.Arrays.fill(uniform, Math.max(1, recipe.inputCount()));
        return uniform;
    }

    /**
     * 该配方是否要付机器能量。
     * <p>用来把「纯能量配方」（生命提纯机：只吃赤能源 + 生命能量，没有物品原料）与
     * 「凭空造物」（既不吃材料也不耗能）区分开 —— 前者是合法的能量换物，后者必须一律拒绝。
     */
    private static boolean costsEnergy(RecipeCost recipe) {
        return !MachineProcessEnergy.costPerRun(recipe.recipe()).isFree();
    }

    /**
     * 「无原料格还放行」是否不安全（不安全者必须排除出虚拟加工）。
     * <p>
     * 无原料格有两种来源：① 配方本来就只吃机器能量（生命提纯机：赤能源 + 生命能量换物），这是
     * "能量换物"的本来语义，合法；② 原料格<b>没被读出来</b> —— 第三方配方的输入常是它自家的
     * {@code ItemStackIngredient} 之类，既不是原版 {@link net.minecraft.world.item.crafting.Ingredient}
     * 也不实现 {@code Collection}，{@link RecipeIngredients} 的反射兜底拿不到 ⇒ 看着像"不吃料"。
     * <p>
     * 第 ② 种一旦放进索引就是<b>凭空造物</b>：整合包只需给该配方类型在认可表里声明一个 {@code energy}，
     * {@link #costsEnergy} 即变为真，"能量换物"的豁免就会把它放行 —— 而它实际上<b>一分材料都不扣</b>。
     * 第三方那张到底是不是纯能量配方，我们无从证实（执行逻辑在它自己手里），故一律按危险处理。
     *
     * <p><b>为什么放在这里而不是改 {@code RecipeIngredients}</b>：那里是"配方 → 估价视图"的通用
     * 提取（价值分模块也在用），"能不能虚拟加工"是规划器自己的准入判据，不该污染上游。
     *
     * @return true = 必须排除（无原料格，且无法证明它合法）
     */
    static boolean isUnsafeEmpty(RecipeCost recipe) {
        if (!recipe.ingredients().isEmpty()) {
            return false;
        }
        if (!costsEnergy(recipe)) {
            return true; // 零材料 + 零能耗：无论如何都是凭空造物
        }
        // 有能耗但无原料格：只有本模组机器族的"纯能量配方"才认，第三方一律拒绝
        return MachineProcessEnergy.isThirdPartyProcess(recipe.recipe().getType());
    }
}
