package com.example.akaishi.craft;

import com.example.akaishi.config.ModConfig;
import com.example.akaishi.craft.recipe.AkaishiEnergyProcessRecipe;
import com.example.akaishi.craft.recipe.AkaishiFluidProcessRecipe;
import com.example.akaishi.craft.recipe.AkaishiRecipeTypes;
import com.example.akaishi.craft.recipe.IAkaishiMachineRecipe;
import com.example.akaishi.craft.thirdparty.ThirdPartyProcesses;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 机器加工的「单批成本」核算：赤能源 + 生命能量 + 加工耗时。
 *
 * <p>虚拟加工要让玩家看到并真正支付机器成本，就必须有<b>唯一口径</b>。机器侧的成本散在各
 * {@code BlockEntity} 里（单槽族 = 每 tick 抽能 × 耗时；聚合器/提纯机 = 一口价），
 * 而规划器在 {@code common} 里拿不到方块实例，故按<b>配方族</b>在这里重算一遍。
 *
 * <p><b>成本与耗时按场域内该族机台的真实升级换算</b>（见 {@link #costPerRun(Recipe, MachineSpec)}）：
 * 速度升级缩短耗时、同时抬高每 tick 能耗（照搬机器侧抽取算式）；能量升级只抬容量、不参与成本。
 * 拿不到场域信息时（列表预筛等）退回 {@link MachineSpec#BASE} = 无升级机台。
 *
 * <p>本类同时是<b>"哪些配方属于机械工序"</b>的唯一判据（{@link #machineKind}）：
 * 成本、耗时、准入门槛三者必须同源，否则会出现"算了能耗却不要求机台"这类半吊子口径。
 */
public final class MachineProcessEnergy {

    /** 本模组的机械工序族（配方族与机台族一一对应；新增一族 = 枚举 + {@link #familyOf} + 两个 switch 各一行） */
    private enum Family {
        PULVERIZING,
        COMPRESSING,
        TRANSFORMING,
        PLANT_CULTIVATING,
        AGGREGATING,
        LIFE_PURIFYING,
        FRACTIONATING,
        LIQUEFYING,
        FUEL_PROCESSING,
        FUEL_MIXING,
        PLASMA_AGGREGATING,
        CENTRIFUGING,
        ACTIVATING
    }

    private MachineProcessEnergy() {
    }

    /** 单批加工的机器成本（两种能量各一笔） */
    public record Cost(long chishi, long life) {

        public static final Cost ZERO = new Cost(0L, 0L);

        /** 两者都为 0 = 这台机器/这张配方不耗能 */
        public boolean isFree() {
            return chishi <= 0L && life <= 0L;
        }

        /** 按批数放大（饱和乘法） */
        public Cost times(long batches) {
            return new Cost(saturatingMultiply(batches, chishi), saturatingMultiply(batches, life));
        }

        public Cost plus(Cost other) {
            return new Cost(saturatingAdd(chishi, other.chishi), saturatingAdd(life, other.life));
        }
    }

    /**
     * 该配方是否属于<b>机械工序</b>（必须由本模组的机台完成）。
     * <p>虚拟加工据此要求"场域内真有这一族的机台"；原版工作台/熔炉/切石等不算 ——
     * 它们本来就不需要机台。
     */
    public static boolean needsMachine(@Nullable Recipe<?> recipe) {
        return machineKind(recipe) != null;
    }

    /**
     * 该配方所需的机台族（= 配方类型本身，供与机台自述 {@code IMachineProcessKind} 比对）。
     *
     * @return 机械工序返回其 {@link RecipeType}；非机械工序返回 null
     */
    @Nullable
    public static RecipeType<?> machineKind(@Nullable Recipe<?> recipe) {
        if (recipe == null) {
            return null;
        }
        RecipeType<?> type = recipe.getType();
        return familyOf(type) != null || isThirdPartyProcess(type) ? type : null;
    }

    /**
     * 是否属于<b>第三方机器工序</b>：既不是本模组的六族，也不是原版那几种"不需要机台"的配方类型。
     * <p>这类工序在我们这边无从得知它由哪台机器、耗多少能跑，故一律要求"场域里有经接入器认可的第三方机器"
     * （见 {@code ProcessCoverage}），成本则由第三方认可表声明。
     */
    public static boolean isThirdPartyProcess(@Nullable RecipeType<?> type) {
        if (type == null) {
            return false;
        }
        return familyOf(type) == null && !isVanillaType(type);
    }

    /** 原版配方类型：工作台/熔炉/切石等，本来就不需要机台，虚拟加工直接代劳 */
    private static boolean isVanillaType(RecipeType<?> type) {
        return type == RecipeType.CRAFTING || type == RecipeType.SMELTING || type == RecipeType.BLASTING
                || type == RecipeType.SMOKING || type == RecipeType.CAMPFIRE_COOKING
                || type == RecipeType.STONECUTTING || type == RecipeType.SMITHING;
    }

    /**
     * 工序来源分档（界面「这条工序由谁提供」用）：
     * <ul>
     *   <li><b>0</b> = 本模组机台族；</li>
     *   <li><b>1</b> = 第三方，且认可表已声明（能指名到具体方块）；</li>
     *   <li><b>2</b> = 第三方，未声明（粗粒度放行，只要场域里有任意一台已认可机器）。</li>
     * </ul>
     * <p>分档口径与 {@code ProcessCoverage.covers} 的准入判定<b>同源</b>：界面显示的档位必须等于判定用的档位，
     * 否则会出现"界面写着精确、实际按粗粒度放行"这类自相矛盾。
     */
    public static byte sourceTier(@Nullable RecipeType<?> type) {
        if (type == null) {
            return 2;
        }
        if (familyOf(type) != null) {
            return 0;
        }
        return ThirdPartyProcesses.isDeclared(type) ? (byte) 1 : (byte) 2;
    }

    /**
     * 已声明工序的「提供方块」id 清单（未声明 / 非第三方时返回空表）。
     * <p>只回 id 不回名字：名字要按客户端语言翻译，服务端不知道客户端语言。
     * 顺序按 id 字典序固定 —— 集合遍历序不稳定，直接用会让界面文字每次刷新都在跳。
     */
    public static List<String> declaredOwners(@Nullable RecipeType<?> type) {
        ThirdPartyProcesses.Entry entry = ThirdPartyProcesses.entryOf(type);
        if (entry == null) {
            return List.of();
        }
        List<String> ids = new ArrayList<>(entry.blocks().size());
        for (Block block : entry.blocks()) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (id != null) {
                ids.add(id.toString());
            }
        }
        ids.sort(Comparator.naturalOrder());
        return ids;
    }

    /**
     * 一批（机器跑一次）的机器成本。
     * <p>规则：配方显式声明 {@code energy} 优先（只作用于赤能源）；否则按配方族取该机器的配置。
     * 非机械工序（原版工作台/熔炉等）返回 {@link Cost#ZERO}。
     */
    public static Cost costPerRun(@Nullable Recipe<?> recipe) {
        return costPerRun(recipe, MachineSpec.BASE);
    }

    /**
     * 一批（机器跑一次）的机器成本，按<b>该台机台的实际升级</b>换算。
     *
     * <p>换算口径<b>照搬机器侧的抽取算式</b>（这是唯一诚实的来源）：
     * 单槽族每 tick 抽 {@code (long)(perTick × costMultiplier × energyCostMultiplier)}，
     * 而进度按 {@code speedMultiplier} 推进 ⇒ 实际抽取次数 = {@code ceil(基础耗时 / speedMultiplier)}；
     * 于是<b>提速会降总能耗</b>（抽取次数降得比每 tick 涨幅多）。能量升级只抬容量，不参与成本。
     */
    public static Cost costPerRun(@Nullable Recipe<?> recipe, MachineSpec spec) {
        if (recipe == null) {
            return Cost.ZERO;
        }
        Family family = familyOf(recipe.getType());
        if (family == null) {
            if (!isThirdPartyProcess(recipe.getType())) {
                return Cost.ZERO; // 原版工作台/熔炉那类：不需要机台，也就没有机器能耗
            }
            // 第三方工序：成本只能由第三方认可表声明；未声明则不发明数字（见 DEFAULT_CHISHI 注释）。
            // 也<b>不套用升级倍率</b>：第三方机器不可能装我们的升级件，读不到。
            ThirdPartyProcesses.Entry entry = ThirdPartyProcesses.entryOf(recipe.getType());
            return entry == null ? new Cost(ThirdPartyProcesses.DEFAULT_CHISHI, 0L)
                    : new Cost(entry.chishi(), entry.life());
        }
        return switch (family) {
            case PULVERIZING -> tickFamily(ModConfig.pulverizerCostPerTick, ModConfig.pulverizerTicks, spec);
            case COMPRESSING -> tickFamily(ModConfig.compressorCostPerTick, ModConfig.compressorTicks, spec);
            case TRANSFORMING -> tickFamily(ModConfig.transformerCostPerTick, ModConfig.transformerTicks, spec);
            case PLANT_CULTIVATING ->
                    tickFamily(ModConfig.plantCultivatorCostPerTick, ModConfig.plantCultivatorTicks, spec);
            // 聚合器：单次一口价，机器侧不套任何倍率（速度在这台机器上表现为"每 tick 连做几次"）
            case AGGREGATING -> chishiOnly(aggregatorCost(recipe));
            // 提纯机双料：赤能源一口价（机器侧乘了 [machine] costMultiplier 与耗能倍率），生命能量按件计
            case LIFE_PURIFYING -> new Cost(
                    saturatingMultiply(scaled(ModConfig.lifePurifierTotalCost), energyCostMultiplier(spec)),
                    lifePurifierLifeCost(spec));
            // 分馏器：一口价（每完成一次扣一次），机器侧没乘 [machine] costMultiplier ⇒ 这里也不乘，
            // 否则"计划报价 ≠ 机器实收"
            case FRACTIONATING -> chishiOnly(
                    saturatingMultiply(ModConfig.fractionatorCostPerCraft, energyCostMultiplier(spec)));
            // 液化机：各档成本由配方声明（下界之星 50M / 凋零玫瑰 5M …），机器侧乘了
            // [machine] costMultiplier 与耗能倍率 ⇒ 这里同乘
            case LIQUEFYING -> chishiOnly(
                    saturatingMultiply(scaled(declaredEnergy(recipe)), energyCostMultiplier(spec)));
            // 加工机 / 混合器：单批成本来自机器配置，机器侧乘了 [machine] costMultiplier 与耗能倍率
            case FUEL_PROCESSING -> chishiOnly(
                    saturatingMultiply(scaled(ModConfig.energyProcessorChishiCost), energyCostMultiplier(spec)));
            case FUEL_MIXING -> chishiOnly(
                    saturatingMultiply(scaled(ModConfig.fuelMixerChishiCost), energyCostMultiplier(spec)));
            // 聚变燃料聚合器：一口价，机器侧没乘 [machine] costMultiplier ⇒ 这里也不乘
            case PLASMA_AGGREGATING -> chishiOnly(
                    saturatingMultiply(ModConfig.aggregatorCostPerCraft, energyCostMultiplier(spec)));
            // 生命离心机：按批计费。机器侧 unitCost = costPerMb × [machine] costMultiplier × 耗能倍率，
            // 一批 = 配方声明的 mB ⇒ 这里同乘（scaled 承担 costMultiplier）
            case CENTRIFUGING -> chishiOnly(saturatingMultiply(
                    scaled(saturatingMultiply(declaredFluidAmount(recipe), ModConfig.lifeCentrifugeCostPerMb)),
                    energyCostMultiplier(spec)));
            // 生命活化器：吃的是生命能量（机器侧不乘任何倍率），一批 = 配方声明的 mB
            case ACTIVATING -> new Cost(0L,
                    saturatingMultiply(declaredFluidAmount(recipe), ModConfig.lifeActivatorCostPerMb));
        };
    }

    /**
     * 一批机器的加工耗时（tick），按<b>该台机台的实际升级</b>换算。
     * <p>聚合器充能满即产出、没有固定时长；提纯机按"积满该批赤能源所需 tick"折算
     * （抽取速率随速度升级提升，故提速即缩短）。
     */
    public static long ticksPerRun(@Nullable Recipe<?> recipe, MachineSpec spec) {
        if (recipe == null) {
            return 0L;
        }
        Family family = familyOf(recipe.getType());
        if (family == null) {
            if (!isThirdPartyProcess(recipe.getType())) {
                return 0L; // 原版工作台/熔炉那类：不占机械工时
            }
            ThirdPartyProcesses.Entry entry = ThirdPartyProcesses.entryOf(recipe.getType());
            return entry == null ? ThirdPartyProcesses.DEFAULT_TICKS : entry.ticks();
        }
        return switch (family) {
            case PULVERIZING -> scaledTicks(ModConfig.pulverizerTicks, spec);
            case COMPRESSING -> scaledTicks(ModConfig.compressorTicks, spec);
            case TRANSFORMING -> scaledTicks(ModConfig.transformerTicks, spec);
            case PLANT_CULTIVATING -> scaledTicks(ModConfig.plantCultivatorTicks, spec);
            case AGGREGATING -> 0L;
            // 成本里的 costMultiplier 与抽取速率里的同一项相消 ⇒ 只需 基础总耗 / (速率 × 速度倍率)
            case LIFE_PURIFYING -> poolTicks(ModConfig.lifePurifierTotalCost,
                    ModConfig.lifePurifierChishiRate, spec);
            // 分馏器：进度按速度倍率推进，故实际耗时 = 基础耗时 / 速度倍率
            case FRACTIONATING -> scaledTicks(ModConfig.fractionatorProcessTicks, spec);
            // 液化机：能量池模式，耗时 = 总耗 / (抽取速率 × 速度倍率)
            case LIQUEFYING -> poolTicks(declaredEnergy(recipe), ModConfig.energyLiquefierChishiRate, spec);
            // 加工机 / 混合器：同上（能量池模式）
            case FUEL_PROCESSING -> poolTicks(ModConfig.energyProcessorChishiCost,
                    ModConfig.energyProcessorChishiRate, spec);
            case FUEL_MIXING -> poolTicks(ModConfig.fuelMixerChishiCost, ModConfig.fuelMixerChishiRate, spec);
            // 聚变燃料聚合器：进度按速度倍率推进（一口价，非能量池）
            case PLASMA_AGGREGATING -> scaledTicks(ModConfig.aggregatorProcessTicks, spec);
            // 生命离心机：一批 = 配方声明的 mB，按每 tick 分离速率折算（速率随速度升级提升）
            case CENTRIFUGING -> poolTicks(declaredFluidAmount(recipe), ModConfig.lifeCentrifugeConvertRate, spec);
            // 生命活化器：本机没有升级槽、也不读全局 workSpeed，故直接按转化速率折算
            case ACTIVATING -> {
                long amount = declaredFluidAmount(recipe);
                long rate = ModConfig.lifeActivatorConvertRate;
                yield rate <= 0L ? 0L : (long) Math.ceil(amount / (double) rate);
            }
        };
    }

    /**
     * 「能量池」型机器的实际耗时 = ceil(总耗 / (抽取速率 × 速度倍率))。
     * <p>为什么倍率不见了：机器侧的总耗与每 tick 抽取额<b>同时</b>乘了
     * {@code [machine] costMultiplier} 与速度升级耗能倍率 ⇒ 相除后相消，
     * 因此提速只让抽取次数变少、总耗不变（与提纯机、液化机、加工机、混合器四处一致）。
     */
    private static long poolTicks(long totalCost, long rate, MachineSpec spec) {
        double effective = rate * speedMultiplier(spec);
        return effective <= 0.0D ? 0L : (long) Math.ceil(Math.max(0L, totalCost) / effective);
    }

    /** 一批机器的加工耗时（无场域信息时的基准 = 无升级机台） */
    public static long ticksPerRun(@Nullable Recipe<?> recipe) {
        return ticksPerRun(recipe, MachineSpec.BASE);
    }

    /** 单槽族：每 tick 能耗 × 耗能倍率，抽取次数 = ceil(基础耗时 / 速度倍率) */
    private static Cost tickFamily(long perTick, int ticks, MachineSpec spec) {
        long effectiveTicks = scaledTicks(ticks, spec);
        if (effectiveTicks <= 0L) {
            return Cost.ZERO;
        }
        long perTickUpgraded = saturatingMultiply(scaled(perTick), energyCostMultiplier(spec));
        return chishiOnly(saturatingMultiply(perTickUpgraded, effectiveTicks));
    }

    /** 实际耗时 = 基础耗时 / 速度倍率（向上取整；基础 > 0 时至少 1 tick） */
    private static long scaledTicks(int baseTicks, MachineSpec spec) {
        if (baseTicks <= 0) {
            return 0L;
        }
        double speed = speedMultiplier(spec);
        return speed <= 0.0D ? baseTicks : Math.max(1L, (long) Math.ceil(baseTicks / speed));
    }

    /** 速度倍率：与 {@code MachineUpgradeSlots#getSpeedMultiplier} 同口径（含全局 [machine] workSpeed） */
    private static double speedMultiplier(MachineSpec spec) {
        return Math.min(1.0D + spec.speedCount(), 8.0D) * ModConfig.machineWorkSpeed;
    }

    /** 耗能倍率：与 {@code MachineUpgradeSlots#getEnergyCostMultiplier} 同口径（随速度升级抬高，4 封顶） */
    private static long energyCostMultiplier(MachineSpec spec) {
        return (long) Math.min(1.0D + spec.speedCount(), 4.0D);
    }

    /** 配方族 → 机器族（唯一的映射点：新增机器族只需在这里加一行，其余分支都用 switch 强校验） */
    @Nullable
    private static Family familyOf(@Nullable RecipeType<?> type) {
        if (type == null) {
            return null;
        }
        if (type == AkaishiRecipeTypes.PULVERIZING.get()) {
            return Family.PULVERIZING;
        }
        if (type == AkaishiRecipeTypes.COMPRESSING.get()) {
            return Family.COMPRESSING;
        }
        if (type == AkaishiRecipeTypes.TRANSFORMING.get()) {
            return Family.TRANSFORMING;
        }
        if (type == AkaishiRecipeTypes.PLANT_CULTIVATING.get()) {
            return Family.PLANT_CULTIVATING;
        }
        if (type == AkaishiRecipeTypes.AGGREGATING.get()) {
            return Family.AGGREGATING;
        }
        if (type == AkaishiRecipeTypes.LIFE_PURIFYING.get()) {
            return Family.LIFE_PURIFYING;
        }
        if (type == AkaishiRecipeTypes.FRACTIONATING.get()) {
            return Family.FRACTIONATING;
        }
        if (type == AkaishiRecipeTypes.LIQUEFYING.get()) {
            return Family.LIQUEFYING;
        }
        if (type == AkaishiRecipeTypes.PROCESSING.get()) {
            return Family.FUEL_PROCESSING;
        }
        if (type == AkaishiRecipeTypes.MIXING.get()) {
            return Family.FUEL_MIXING;
        }
        if (type == AkaishiRecipeTypes.PLASMA_AGGREGATING.get()) {
            return Family.PLASMA_AGGREGATING;
        }
        if (type == AkaishiRecipeTypes.CENTRIFUGING.get()) {
            return Family.CENTRIFUGING;
        }
        if (type == AkaishiRecipeTypes.ACTIVATING.get()) {
            return Family.ACTIVATING;
        }
        return null;
    }

    /**
     * 流体配方声明的"一批"用量（mB）；非流体配方或无输入时为 0。
     * <p>离心机/活化器的成本与耗时都以"一批多少 mB"为基数，故由配方声明（数据包可调）。
     */
    private static long declaredFluidAmount(@Nullable Recipe<?> recipe) {
        if (recipe instanceof AkaishiFluidProcessRecipe fluid && !fluid.fluidInputs().isEmpty()) {
            return Math.max(0L, fluid.fluidInputs().get(0).amount());
        }
        return 0L;
    }

    /**
     * 配方声明的单批赤能源（0 = 配方没声明）。
     * <p>只有"成本天然逐条不同"的族才读它（如液化机各档 5M/10M/50M）；
     * 单槽族读自己的 {@code energyPerTick}、提纯机读配置总值，都不读声明值 ——
     * 计划侧与机器侧必须同源，否则出现"计划报价 ≠ 机器实收"。
     */
    private static long declaredEnergy(@Nullable Recipe<?> recipe) {
        return recipe instanceof IAkaishiMachineRecipe machine ? Math.max(0L, machine.energy()) : 0L;
    }

    /**
     * 提纯机单件生命能量消耗。
     * <p>
     * 机器侧算式是 {@code lifePurifierLifeCost * getEnergyCostMultiplier()}（不乘 [machine] costMultiplier，
     * <b>但乘</b>速度升级带来的耗能倍率，最多 4×）。这里必须同口径，否则装了速度升级后
     * 机器实收是报价的 2~4 倍（计划报价偏少 ⇒ 又是"报价 ≠ 实收"）。
     */
    private static long lifePurifierLifeCost(MachineSpec spec) {
        return saturatingMultiply(Math.max(0L, ModConfig.lifePurifierLifeCost), energyCostMultiplier(spec));
    }

    /**
     * 聚合器单批成本：配方声明的 {@code energy} 优先，其次按 {@code energy_config} 档位取配置默认。
     *
     * <p><b>为什么只有这一支读配方声明值</b>：只有聚合器的机器侧会走本类
     * （{@code AkaishiEnergyAggregatorBlockEntity#energyCost} → 本方法），因此"声明值"它认。
     * 单槽族读自己的 {@code energyPerTick}、提纯机读 {@code lifePurifierTotalCost}，**都不读声明值** ——
     * 若计划侧单独认了声明值，就会出现"计划报价 ≠ 机器实收"。所以声明值只在这里生效。
     */
    private static long aggregatorCost(Recipe<?> recipe) {
        if (recipe instanceof IAkaishiMachineRecipe machine && machine.energy() > 0L) {
            return machine.energy();
        }
        String key = recipe instanceof AkaishiEnergyProcessRecipe energy ? energy.costKey() : "";
        return AkaishiEnergyProcessRecipe.ENERGY_CONFIG_GEODE_UPGRADE.equals(key)
                ? ModConfig.energyAggregatorEnergyPerGeodeUpgrade
                : ModConfig.energyAggregatorEnergyPerIngot;
    }

    private static Cost chishiOnly(long chishi) {
        return chishi <= 0L ? Cost.ZERO : new Cost(chishi, 0L);
    }

    /** [machine] costMultiplier 全局放大 */
    private static long scaled(long base) {
        return base <= 0L ? 0L : (long) (base * ModConfig.machineCostMultiplier);
    }

    /**
     * 饱和乘法（批数 × 单批成本用）。
     * <p>配置项允许填到 {@code Long.MAX_VALUE}，直接相乘会溢出成负数，
     * 显示与扣费都会变成垃圾值；溢出时按上限封顶。
     */
    public static long saturatingMultiply(long a, long b) {
        if (a <= 0L || b <= 0L) {
            return 0L;
        }
        return a > Long.MAX_VALUE / b ? Long.MAX_VALUE : a * b;
    }

    /** 饱和加法（多批/多节点累加用） */
    public static long saturatingAdd(long a, long b) {
        long sum = a + b;
        return sum < 0L ? Long.MAX_VALUE : sum;
    }
}
