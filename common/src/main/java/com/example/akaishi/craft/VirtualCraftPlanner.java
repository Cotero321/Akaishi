package com.example.akaishi.craft;

import com.example.akaishi.craft.recipe.AkaishiItemProcessRecipe;
import com.example.akaishi.value.ItemPoints;
import com.example.akaishi.value.ItemTerminalFee;
import com.example.akaishi.value.RecipeIngredients.RecipeCost;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * 因此本规划器<b>不自造任何汇率</b>：存取手续费直接用 {@link ItemTerminalFee} 折算，
 * 机器加工能耗则按 {@link MachineProcessEnergy} 的配方族口径累加。
 * <p>
 * <b>成本口径</b>：
 * <ul>
 *   <li>材料 = 配方树的叶子清单（同名合并，数量真实可扣）；</li>
 *   <li>IP = 叶子材料的 IP 之和（<b>仅用于界面显示</b>"这一单动用了多少库容量"）；</li>
 *   <li>时间 = <b>关键路径（最长路）</b>：每个节点自己的「批数 × {@link #TICKS_PER_STEP} + 该节点机器墙钟」
 *       沿依赖链取最长分支（不同族的节点可并行，故不能简单求和）；</li>
 *   <li>赤能源 = 取出材料手续费 + 产物入库手续费（按<b>无减免</b>估算，即上限）
 *       + Σ 批数 × 单批机器赤能源（{@link MachineProcessEnergy}，按无升级件基准估算）；</li>
 *   <li>生命能量 = Σ 批数 × 单批生命能量（同样来自 {@link MachineProcessEnergy}）。
 *       两种能量都在开工时从<b>场域内对应类型的能量芯片</b>实扣（见 {@code ICraftEnergyPool}）。</li>
 * </ul>
 * <p>
 * <b>性能闸门</b>（口径要求必须自补的四道之一：配方树结果缓存）：
 * 配方索引按 {@link com.example.akaishi.value.ValueCache.Fingerprint} 缓存，配方热重载（数量或内容变化）才重建；
 * 单次求解另有 {@link PlannerSolve#MAX_NODES} 与件数相关的批次闸门（{@link PlannerSolve#STEP_BUDGET_BASE}）双闸门，超限即判定为"规划失败"，
 * 绝不做无界递归。所有缓存方法都是服务端专有（配方表只在服务端权威）。
 * <p>
 * <b>实现布局（2026-09-24 拆分）</b>：本类只保留公开入口与结果类型（{@link Plan} / {@link Step} / {@link Leaf}）；
 * 配方匹配见 {@link PlannerRouting}，求解调度见 {@link PlannerSolve}，结算与产出原语见 {@link PlannerSettlement}，
 * 配方索引与分片构建见 {@link PlannerIndex} —— 均为同包包私有协作类，方法体逐位搬迁，行为不变。
 */
public final class VirtualCraftPlanner {

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
     * @param machineCost 机器加工能耗（赤能源 + 生命能量）：Σ 批数 × 单批成本。<b>真机加工下这只是"预计"</b> ——
     *                    能量由机台自己按配方消耗（终端只做门槛判断与报价，不代扣）
     * @param machineKinds 本单要跑的<b>机械工序族</b>（去重）：场域内必须有对应机台才能开工，见 {@code IMachineProcessKind}
     * @param totalTicks  总耗时（tick）= <b>关键路径</b>：各节点「批数 × {@link #TICKS_PER_STEP} + 机器墙钟」沿依赖链取最长分支
     * @param totalEnergy 本单要消耗的赤能源总量 = 存取手续费 + {@code machineCost.chishi()}（界面显示用）
     * @param steps       配方节点总数
     * @param order       真机加工的执行序（<b>依赖优先</b>：原料节点在前），执行器按它逐节点投料/收货
     */
    public record Plan(ItemStack target, List<Leaf> leaves, List<Leaf> direct, List<Leaf> taken,
                       List<ItemStack> outputs, long materialIp, long resultIp, MachineProcessEnergy.Cost machineCost,
                       Set<RecipeType<?>> machineKinds, long totalTicks, long totalEnergy, int steps,
                       List<Step> order) {

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
     * 真机加工的<b>可执行节点</b>：一个配方 + 批数 + "该吃多少料 / 该收多少货"。
     * <p>
     * <b>为什么由规划器产出</b>：节点选择、拓扑顺序、批数、每格消耗量这四件事只有 {@code solve} 算得出来。
     * 让执行器自己再推一遍就是第二套账，必然出现"报价 ≠ 实收"（本项目明令禁止的两套口径）。
     *
     * @param item      本节点产出的物品
     * @param recipe    选定的配方
     * @param batches   批数（机器要跑几次）
     * @param units     <b>逐格</b>输入件数（与 {@code picks} 同序等长）：批数 × 该格消耗；输入保留型只算一份
     * @param picks     每格选定的输入物品（与配方的原料格同序等长）
     * @param inputKept 输入保留型（培养机）：机器不扣料，收完产物要把料取回
     * @param wallTicks 本节点预计墙钟（步时 + 机器墙钟）：真机加工拿它当"等多久算超时"的基准
     */
    public record Step(Item item, RecipeCost recipe, long batches, List<Long> units, List<Item> picks,
                      boolean inputKept, long wallTicks) {

        /** 本节点要从库里取出的输入（同名格先合并） */
        public Map<Item, Long> inputs() {
            Map<Item, Long> need = new LinkedHashMap<>();
            for (int i = 0; i < picks.size(); i++) {
                long amount = i < units.size() ? Math.max(0L, units.get(i)) : 0L;
                need.merge(picks.get(i), amount, Long::sum);
            }
            return need;
        }

        /**
         * 本节点机器<b>应当</b>产出的总量（主产物按批数放大 + 副产物按批数计）。
         * <p>收货只按它对账：机台是共用的（不锁），输出槽里可能有别人的东西，多取就是偷。
         */
        public Map<Item, Long> expected() {
            Map<Item, Long> out = new LinkedHashMap<>();
            long produced = MachineProcessEnergy.saturatingMultiply(batches, Math.max(1, recipe.outputCount()));
            if (produced > 0L) {
                out.merge(recipe.resultItem(), produced, Long::sum);
            }
            if (recipe.recipe() instanceof AkaishiItemProcessRecipe process) {
                ItemStack byproduct = process.byproduct();
                if (!byproduct.isEmpty()) {
                    out.merge(byproduct.getItem(),
                            MachineProcessEnergy.saturatingMultiply(batches, byproduct.getCount()), Long::sum);
                }
            }
            return out;
        }

        /** 是否免机台工序（原版工作台/熔炉那类）：场域里没有对应机台，由终端按步时代劳 */
        public boolean handsFree() {
            return MachineProcessEnergy.machineKind(recipe.recipe()) == null;
        }
    }

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
        return plan(manager, access, target, stock, ProcessCoverage.EMPTY);
    }

    /** 同上，带场域工序供给快照（判定方已有快照时用，避免重复扫场） */
    @Nullable
    public static Plan plan(RecipeManager manager, RegistryAccess access, ItemStack target,
            Map<Item, Long> stock, ProcessCoverage coverage) {
        if (target.isEmpty()) {
            return null;
        }
        return plan(manager, access, target.getItem(),
                Math.max(1, Math.min(target.getCount(), MAX_TARGET_COUNT)), stock, coverage);
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
        return plan(manager, access, item, amount, stock, ProcessCoverage.EMPTY);
    }

    /**
     * 同上，但带上<b>场域工序供给快照</b>：机器的能耗与耗时按场域内该族机台的真实升级与台数核算
     * （见 {@link MachineProcessEnergy} / {@link MachineScheduler}）。
     * <p>拿不到场域信息时传 {@link ProcessCoverage#EMPTY}：退回"一台无升级机台"的基准，
     * 成本仍可算（列表预筛、下级材料复核等只关心可解性，不看成本）。
     */
    @Nullable
    public static Plan plan(RecipeManager manager, RegistryAccess access, Item item, int amount,
            Map<Item, Long> stock, ProcessCoverage coverage) {
        if (item == null || item == Items.AIR || amount <= 0) {
            return null;
        }
        Map<Item, List<RecipeCost>> byResult = PlannerIndex.index(manager, access).byResult();
        // 目标本身就是基础材料时不做虚拟加工（没有配方可执行）
        List<RecipeCost> recipes = byResult.get(item);
        if (recipes == null || recipes.isEmpty()) {
            return null;
        }
        PlannerSolve.Solution solution = PlannerSolve.solve(byResult, item, amount, stock, coverage);
        if (solution == null) {
            return null;
        }
        List<Leaf> leaves = PlannerSettlement.buildLeaves(solution.leafTotals);
        List<Leaf> taken = PlannerSettlement.buildLeaves(solution.takenTotals);
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
        long feeEnergy = ItemTerminalFee.withdrawCost(materialIp, 0) + ItemTerminalFee.depositCost(resultIp, 0);
        // 目标栈必须是「被点的那件物品」：客户端按 {@code plan.target().getItem() == item} 认账。
        // <b>不能取 outputs.get(0)</b> —— addByproduct 先于 addOutput 记账，含副产的配方那里放的是副产物，
        // 客户端认不上账 ⇒ 详情页永远停在"正在规划…"且不画开始按钮（分馏族的 7 个活化结晶就点不动）。
        ItemStack head = new ItemStack(item, 1);
        // 执行序反转成「依赖优先」：{@code solve} 的拓扑序是父在前（目标先出队），
        // 而真机加工必须先做出下级材料喂上一级 —— 反转后任一节点都排在它的原料节点之后。
        List<Step> execOrder = new ArrayList<>(solution.order);
        Collections.reverse(execOrder);
        return new Plan(head, leaves, List.copyOf(solution.direct), taken, List.copyOf(solution.outputs),
                materialIp, resultIp, solution.machineCost, Set.copyOf(solution.machineKinds),
                solution.criticalTicks,
                MachineProcessEnergy.saturatingAdd(feeEnergy, solution.machineCost.chishi()), solution.steps,
                List.copyOf(execOrder));
    }

    /**
     * 该物品是否可被虚拟加工（有可解析配方，且配方树能完整展开）。
     * <p>
     * <b>会阻塞</b>建索引（见 {@link PlannerIndex#index}）：界面路径请改用
     * {@link #isIndexReady} + {@link #startBuild} + {@link #stepBuild} 的分片流程。
     */
    public static boolean isCraftable(RecipeManager manager, RegistryAccess access, Item item) {
        return PlannerIndex.index(manager, access).craftableSet().contains(item);
    }

    /**
     * 全部<b>可解</b>的产物物品（按注册名排序，顺序稳定；调用方负责筛选与截断）。
     * <p>
     * 只列"能真正规划成功"的：列表里出现点进去必然失败的条目，等于给了用不了的入口。
     */
    public static List<Item> craftableItems(RecipeManager manager, RegistryAccess access) {
        return PlannerIndex.index(manager, access).craftable();
    }

    // ===== 分片构建（首次打开加工页不再卡服务端，实现见 PlannerIndex） =====

    /** 索引是否已就绪（就绪即可直接取目录，无需等待分片构建） */
    public static boolean isIndexReady(RecipeManager manager) {
        return PlannerIndex.isIndexReady(manager);
    }

    /**
     * 开始（或复用）一次分片构建。
     * <p>
     * <b>为什么要有它</b>：目录首次拉取原本在服务端线程上现场建索引 ——
     * 「收集全部配方 + 逐物品跑一遍可解性预筛」是秒级工作量（实测 1017 项 14.8 秒，且
     * {@code ItemPoints} 热路径重复扫全表是主因，另见 {@code ValueCache#fingerprint} 的复用窗口），
     * 直接把服务端卡住，玩家看到的就是"加工页刚打开没反应、等一会儿才能用"。
     * 改成分片推进后每 tick 只做 {@link PlannerIndex#SLICE_NANOS} 一小段，服务端不再出现长阻塞。
     * <p>
     * 阶段 A 一次做完：只是遍历配方表建索引并排序，没有递归，代价与配方数同阶。
     * 阶段 B 交给 {@link #stepBuild}。
     */
    public static void startBuild(RecipeManager manager, RegistryAccess access) {
        PlannerIndex.startBuild(manager, access);
    }

    /**
     * 推进一步分片构建（每 tick 调用一次）。
     *
     * @return true = 索引已就绪（本次刚好建完）；false = 仍需等待
     */
    public static boolean stepBuild(RecipeManager manager) {
        return PlannerIndex.stepBuild(manager);
    }

    /** 主动失效（调试命令/热重载钩子可用） */
    public static void invalidate() {
        PlannerIndex.invalidate();
    }
}
