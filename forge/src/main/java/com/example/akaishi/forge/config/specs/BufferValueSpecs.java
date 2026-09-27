package com.example.akaishi.forge.config.specs;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * 配置域：端口与电池缓冲、培养提纯与分馏、价值分。
 * 仅由 {@link com.example.akaishi.forge.config.AkaishiConfig} 的 static 初始化按固定顺序调用；
 * 键名/注释/默认值/范围与拆分前逐字一致，调用顺序即 common.toml 分节顺序。
 */
public final class BufferValueSpecs {

    // ---- 端口与电池缓冲 ----
    public static ForgeConfigSpec.LongValue LIFE_MATRIX_INPUT_PORT_BUFFER;
    public static ForgeConfigSpec.LongValue LIFE_MATRIX_OUTPUT_PORT_BUFFER;
    public static ForgeConfigSpec.LongValue PURIFIER_INPUT_PORT_BUFFER;
    public static ForgeConfigSpec.LongValue MINER_PORT_BUFFER;
    public static ForgeConfigSpec.LongValue MINER_ENERGY_INPUT_BUFFER;
    public static ForgeConfigSpec.LongValue WIRELESS_INPUT_PORT_BUFFER;
    public static ForgeConfigSpec.LongValue WIRELESS_OUTPUT_PORT_BUFFER;
    public static ForgeConfigSpec.LongValue GEN_ENERGY_OUTPUT_BUFFER;
    public static ForgeConfigSpec.LongValue FUSION_ENERGY_OUTPUT_BUFFER;
    public static ForgeConfigSpec.LongValue REACTOR_ENERGY_OUTPUT_BUFFER;
    public static ForgeConfigSpec.LongValue LIFE_ENERGY_CELL_SERIALIZER_CAPACITY;
    public static ForgeConfigSpec.LongValue PLASMA_TANK_CAPACITY;
    public static ForgeConfigSpec.LongValue ITEM_TERMINAL_ENERGY_BUFFER;
    public static ForgeConfigSpec.LongValue ITEM_TERMINAL_ENERGY_PORT_BUFFER;

    // ---- 培养机提纯与分馏机 ----
    public static ForgeConfigSpec.LongValue CULTIVATOR_LIFE_CAPACITY;
    public static ForgeConfigSpec.ConfigValue<List<? extends Integer>> CULTIVATOR_PURIFY_SUCCESS;
    public static ForgeConfigSpec.ConfigValue<List<? extends Long>> CULTIVATOR_PURIFY_ENERGY;
    public static ForgeConfigSpec.ConfigValue<List<? extends Integer>> CULTIVATOR_PURIFY_SOLID;
    public static ForgeConfigSpec.ConfigValue<List<? extends Integer>> CULTIVATOR_PURIFY_TICKS;
    public static ForgeConfigSpec.IntValue CULTIVATOR_PURIFY_GAIN;
    public static ForgeConfigSpec.LongValue FRACTIONATOR_ENERGY_CAPACITY;
    public static ForgeConfigSpec.LongValue FRACTIONATOR_COST_PER_CRAFT;
    public static ForgeConfigSpec.IntValue FRACTIONATOR_PROCESS_TICKS;

    // ---- 价值分（统一存储库定价内核） ----
    public static ForgeConfigSpec.DoubleValue VALUE_COST_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue VALUE_INGREDIENT_WEIGHT;
    public static ForgeConfigSpec.DoubleValue VALUE_MAGIC_BONUS;
    public static ForgeConfigSpec.DoubleValue VALUE_INGREDIENT_CAP;
    public static ForgeConfigSpec.IntValue VALUE_ITERATIONS;
    public static ForgeConfigSpec.BooleanValue VALUE_LOOT_ENABLED;
    public static ForgeConfigSpec.BooleanValue VALUE_LOOT_AUTO_APPLY;
    public static ForgeConfigSpec.DoubleValue VALUE_LOOT_CAP;
    public static ForgeConfigSpec.BooleanValue VALUE_FLUID_ENABLED;
    public static ForgeConfigSpec.DoubleValue VALUE_FLUID_PER_MB_CAP;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_OVERRIDES;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_FLUID_VALUES;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_TAG_VALUES;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_TIER_BONUS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_KEYWORD_EXCLUSIONS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_LOOT_BLACKLIST;
    public static ForgeConfigSpec.IntValue UNIFIED_VAULT_ROWS;

    private BufferValueSpecs() {
    }

    /** 按拆分前的定义顺序在共享 Builder 上构建本域各节（仅由 AkaishiConfig 调用） */
    public static void build(ForgeConfigSpec.Builder b) {
        // ==================== 端口与电池缓冲 ====================
        b.push("buffers");
        LIFE_MATRIX_INPUT_PORT_BUFFER = b.comment("生命矩阵能量输入口缓冲容量")
                .defineInRange("lifeMatrixInputPortBufferCapacity", 100_000_000L, 0L, Long.MAX_VALUE);
        LIFE_MATRIX_OUTPUT_PORT_BUFFER = b.comment("生命矩阵能量输出口缓冲容量")
                .defineInRange("lifeMatrixOutputPortBufferCapacity", 5_000L, 0L, Long.MAX_VALUE);
        PURIFIER_INPUT_PORT_BUFFER = b.comment("净化矩阵能量输入口缓冲容量")
                .defineInRange("purifierEnergyInputPortBufferCapacity", 1_000_000L, 0L, Long.MAX_VALUE);
        MINER_PORT_BUFFER = b.comment("矿机能量端口缓冲容量")
                .defineInRange("minerPortBufferCapacity", 10_000_000L, 0L, Long.MAX_VALUE);
        MINER_ENERGY_INPUT_BUFFER = b.comment("矿机能量输入口缓冲容量")
                .defineInRange("minerEnergyInputBufferCapacity", 10_000_000L, 0L, Long.MAX_VALUE);
        WIRELESS_INPUT_PORT_BUFFER = b.comment("无线能量输入口缓冲容量")
                .defineInRange("wirelessInputPortBufferCapacity", 100_000_000L, 0L, Long.MAX_VALUE);
        WIRELESS_OUTPUT_PORT_BUFFER = b.comment("无线能量输出口缓冲容量")
                .defineInRange("wirelessOutputPortBufferCapacity", 100_000_000L, 0L, Long.MAX_VALUE);
        GEN_ENERGY_OUTPUT_BUFFER = b.comment("生命矩阵结构【外接】能量输出口缓冲容量")
                .defineInRange("genEnergyOutputPortBufferCapacity", 100_000_000L, 0L, Long.MAX_VALUE);
        FUSION_ENERGY_OUTPUT_BUFFER = b.comment("聚变能量输出口缓冲容量")
                .defineInRange("fusionEnergyOutputBufferCapacity", 20_000_000_000L, 0L, Long.MAX_VALUE);
        REACTOR_ENERGY_OUTPUT_BUFFER = b.comment("反应堆能量输出口缓冲容量")
                .defineInRange("reactorEnergyOutputBufferCapacity", 5_000_000_000L, 0L, Long.MAX_VALUE);
        LIFE_ENERGY_CELL_SERIALIZER_CAPACITY = b.comment("生命储存串联器：自身基础容量（成型后总容量 = 该值 + 26 台外壳储存器容量之和；容量分级见各档储存器）")
                .defineInRange("lifeEnergyCellSerializerBaseCapacity", 2_000_000L, 0L, Long.MAX_VALUE);
        PLASMA_TANK_CAPACITY = b.comment("等离子储罐：容量 (mb)")
                .defineInRange("plasmaTankCapacity", 16_000L, 0L, Long.MAX_VALUE);
        ITEM_TERMINAL_ENERGY_BUFFER = b.comment("物品终端：赤能源缓冲容量（一次性存取费用的费用池）",
                        "须 ≥ 允许的最大单笔费用；单笔 IP 上限 = floor(该值 × 4 / 费率系数)，费率系数为存入 1.005 / 取出 1.0025",
                        "默认 1000000 赤能源 ⇒ 单笔最多约 398 万 IP（约 5.6 箱钻石），可一次存一箱")
                .defineInRange("itemTerminalEnergyBuffer", 1_000_000L, 1L, Long.MAX_VALUE);
        ITEM_TERMINAL_ENERGY_PORT_BUFFER = b.comment("物品终端赤能源接入口：单口自身缓冲容量",
                        "接入口由赤能源管道注入并暂存，终端在存取结算时主动汇聚抽取（多口并联，各口独立蓄能）",
                        "该值只影响单口可暂存量，不影响单笔费用上限（后者由 itemTerminalEnergyBuffer 决定）")
                .defineInRange("itemTerminalEnergyPortBufferCapacity", 1_000_000L, 1L, Long.MAX_VALUE);
        b.pop();

        // ==================== 培养机提纯与分馏机 ====================
        // 提纯表下标 = 纯度区间序数 [0, 25, 50, 75]；0 或缺失条目回退到内置默认
        b.push("cultivator_fractionator");
        CULTIVATOR_LIFE_CAPACITY = b.comment("培养机：生命能量缓冲容量")
                .defineInRange("cultivatorLifeCapacity", 500_000L, 0L, Long.MAX_VALUE);
        CULTIVATOR_PURIFY_SUCCESS = b.comment("提纯成功率（%）按纯度区间 [0, 25, 50, 75]；0 = 用内置默认")
                .defineList("cultivatorPurifySuccess", List.of(90, 80, 70, 60),
                        (Object o) -> o instanceof Number n && n.intValue() >= 0);
        CULTIVATOR_PURIFY_ENERGY = b.comment("提纯生命能量消耗按纯度区间 [0, 25, 50, 75]；0 = 用内置默认")
                .defineList("cultivatorPurifyEnergy", List.of(10_000L, 20_000L, 40_000L, 80_000L),
                        (Object o) -> o instanceof Number n && n.longValue() >= 0);
        CULTIVATOR_PURIFY_SOLID = b.comment("提纯固态生命精华消耗按纯度区间 [0, 25, 50, 75]；0 = 用内置默认")
                .defineList("cultivatorPurifySolid", List.of(1, 2, 4, 8),
                        (Object o) -> o instanceof Number n && n.intValue() >= 0);
        CULTIVATOR_PURIFY_TICKS = b.comment("提纯耗时 (tick) 按纯度区间 [0, 25, 50, 75]；0 = 用内置默认")
                .defineList("cultivatorPurifyTicks", List.of(300, 600, 1200, 2400),
                        (Object o) -> o instanceof Number n && n.intValue() >= 0);
        CULTIVATOR_PURIFY_GAIN = b.comment("培养机：单次提纯增加的纯度")
                .defineInRange("cultivatorPurifyGain", 10, 0, 100);
        FRACTIONATOR_ENERGY_CAPACITY = b.comment("活化分馏机：赤能源存储容量")
                .defineInRange("fractionatorEnergyCapacity", 100_000L, 0L, Long.MAX_VALUE);
        FRACTIONATOR_COST_PER_CRAFT = b.comment("活化分馏机：每次分馏消耗的赤能源")
                .defineInRange("fractionatorCostPerCraft", 2_000L, 0L, Long.MAX_VALUE);
        FRACTIONATOR_PROCESS_TICKS = b.comment("活化分馏机：每次分馏耗时 (tick)")
                .defineInRange("fractionatorProcessTicks", 100, 1, Integer.MAX_VALUE);
        b.pop();

        // ==================== 价值分（统一存储库定价内核） ====================
        // 说明：所有"分"都是相对价值单位，仅用于统一存储库的排序/统计/筛选，不做经济兑换。
        b.push("value");
        VALUE_COST_MULTIPLIER = b.comment("最终造价倍率；0 = 用内置默认 10")
                .defineInRange("costMultiplier", 10.0, 0.0, 10_000.0);
        VALUE_INGREDIENT_WEIGHT = b.comment("原料价值项权重；0 = 用内置默认 0.75")
                .defineInRange("ingredientWeight", 0.75, 0.0, 100.0);
        VALUE_MAGIC_BONUS = b.comment("魔法/功能类物品加成（卷轴/符文/法术书等）；0 = 用内置默认 40")
                .defineInRange("magicBonus", 40.0, 0.0, 10_000.0);
        VALUE_INGREDIENT_CAP = b.comment("单项原料价值封顶（防止一组配方把价炸飞）；0 = 用内置默认 80")
                .defineInRange("ingredientCap", 80.0, 0.0, 100_000.0);
        VALUE_ITERATIONS = b.comment("不动点迭代遍数（合成链传播层数）；0 = 用内置默认 4")
                .defineInRange("iterations", 4, 0, 32);
        VALUE_LOOT_ENABLED = b.comment("掉落来源估值开关（离线扫描战利品表）")
                .define("lootEnabled", true);
        VALUE_LOOT_AUTO_APPLY = b.comment("掉落来源项是否自动生效；false 时仅在索引中给出建议值，不加价")
                .define("lootAutoApply", false);
        VALUE_LOOT_CAP = b.comment("掉落来源项封顶；0 = 用内置默认 60")
                .defineInRange("lootCap", 60.0, 0.0, 100_000.0);
        VALUE_FLUID_ENABLED = b.comment("流体估值开关（桶代理 + 流标签表）")
                .define("fluidEnabled", true);
        VALUE_FLUID_PER_MB_CAP = b.comment("流体每 mB 价值上限；0 = 用内置默认 0.5（即单桶 500 分）")
                .defineInRange("fluidPerMbCap", 0.5, 0.0, 10_000.0);
        VALUE_OVERRIDES = b.comment("手动指定价值表，三级优先级：精确 id → 通配符 → #tag；格式 id=分值。"
                        + "命中者即钉住（权威值），不再被配方原料项抬高")
                .defineList("overrides", List.of(),
                        (Object o) -> o instanceof String s && s.contains("="));
        VALUE_FLUID_VALUES = b.comment("流体价值表，格式 流体id=每桶分值（支持 * 通配）；"
                + "优先于内置无桶推导与桶装折算，用于给不注册桶的流体单独定价")
                .defineList("fluidValues", List.of(),
                        (Object o) -> o instanceof String s && s.contains("="));
        VALUE_TAG_VALUES = b.comment("标签价值表，格式 #tag=分值（叠加计算，有总分上限）")
                .defineList("tagValues", List.of(),
                        (Object o) -> o instanceof String s && s.startsWith("#") && s.contains("="));
        VALUE_TIER_BONUS = b.comment("品质词加成表，格式 词=分值（对物品 id 词边界匹配，命中多个取最高）")
                .defineList("tierBonus", List.of(),
                        (Object o) -> o instanceof String s && s.contains("="));
        VALUE_KEYWORD_EXCLUSIONS = b.comment("关键词豁免表：id / 通配符（*）；命中则跳过品质词与魔法关键词加成，"
                + "空列表 = 用内置默认（豁免本 mod 的等级后缀 ultimate 与 Curios 槽位名 charm）")
                .defineList("keywordExclusions", List.of(),
                        (Object o) -> o instanceof String s && !s.isBlank());
        VALUE_LOOT_BLACKLIST = b.comment("掉落来源项黑名单：命名空间 / id / 通配符（*）")
                .defineList("lootBlacklist", List.of(),
                        (Object o) -> o instanceof String s && !s.isBlank());
        UNIFIED_VAULT_ROWS = b.comment("统一存储库每页行数（1 行 = 9 格）；0 = 用内置默认 6")
                .defineInRange("unifiedVaultRows", 6, 0, 26);
        b.pop();
    }
}
