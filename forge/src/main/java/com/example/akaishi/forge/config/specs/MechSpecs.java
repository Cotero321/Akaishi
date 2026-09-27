package com.example.akaishi.forge.config.specs;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * 配置域：机械改造机器、机械义体、基因权重、底层战斗、机器全局倍率、机制开关。
 * 仅由 {@link com.example.akaishi.forge.config.AkaishiConfig} 的 static 初始化按固定顺序调用；
 * 键名/注释/默认值/范围与拆分前逐字一致，调用顺序即 common.toml 分节顺序。
 */
public final class MechSpecs {

    // ==================== 机器全局倍率 ====================
    public static ForgeConfigSpec.DoubleValue MACHINE_WORK_SPEED;
    public static ForgeConfigSpec.DoubleValue MACHINE_COST_MULTIPLIER;

    // ==================== 机械改造机器 ====================
    /** 机械三机赤能源缓冲容量（共用） */
    public static ForgeConfigSpec.LongValue MECH_CHISHI_CAPACITY;
    /** 机械三机生命能量缓冲容量（共用） */
    public static ForgeConfigSpec.LongValue MECH_LIFE_CAPACITY;
    /** 模板制造厂：塑形一次赤能源消耗 */
    public static ForgeConfigSpec.LongValue MECH_TEMPLATE_CHISHI_COST;
    /** 模板制造厂：塑形一次生命能量消耗 */
    public static ForgeConfigSpec.LongValue MECH_TEMPLATE_LIFE_COST;
    /** 模板制造厂：塑形一次耗时（tick） */
    public static ForgeConfigSpec.IntValue MECH_TEMPLATE_TICKS;
    /** 加工厂：器官基价赤能（下标=器官序数 0~8），0=用内置默认 */
    public static ForgeConfigSpec.ConfigValue<List<? extends Long>> MECH_PROCESS_CHISHI_BASE;
    /** 加工厂：器官基价生命能（下标同上），0=用内置默认 */
    public static ForgeConfigSpec.ConfigValue<List<? extends Long>> MECH_PROCESS_LIFE_BASE;
    /** 加工厂：器官耗时基价 tick（下标同上），0=用内置默认 */
    public static ForgeConfigSpec.ConfigValue<List<? extends Integer>> MECH_PROCESS_TICKS_BASE;
    /** 加工厂：部件系数百分数（下标=部件序数 0~3），0=用内置默认 */
    public static ForgeConfigSpec.ConfigValue<List<? extends Integer>> MECH_PROCESS_PART_FACTOR;
    /** 加工厂：材料消耗份数（下标=部件序数 0~3），0=用内置默认 */
    public static ForgeConfigSpec.ConfigValue<List<? extends Integer>> MECH_PROCESS_MATERIAL_COUNT;
    /** 组装台：组装一次赤能源消耗（固定） */
    public static ForgeConfigSpec.LongValue MECH_ASSEMBLY_CHISHI_COST;
    /** 组装台：组装一次生命能量消耗（固定） */
    public static ForgeConfigSpec.LongValue MECH_ASSEMBLY_LIFE_COST;
    /** 组装台：组装一次耗时（tick） */
    public static ForgeConfigSpec.IntValue MECH_ASSEMBLY_TICKS;

    // ==================== 机械义体属性换算 ====================
    /** 机械义体：生命值权重 → 生命上限换算倍率 */
    public static ForgeConfigSpec.DoubleValue MECH_BODY_HEALTH_SCALE;
    /** 机械义体：攻击伤害权重 → 攻击伤害换算倍率 */
    public static ForgeConfigSpec.DoubleValue MECH_BODY_ATTACK_SCALE;
    /** 机械义体：攻击速度权重 → 攻击速度换算倍率 */
    public static ForgeConfigSpec.DoubleValue MECH_BODY_ATTACK_SPEED_SCALE;
    /** 机械义体：移动速度权重 → 移动速度换算倍率 */
    public static ForgeConfigSpec.DoubleValue MECH_BODY_MOVEMENT_SPEED_SCALE;
    /** 机械义体：护甲权重 → 护甲值换算倍率 */
    public static ForgeConfigSpec.DoubleValue MECH_BODY_ARMOR_SCALE;
    /** 机械义体：暴击率权重 → 暴击率换算倍率 */
    public static ForgeConfigSpec.DoubleValue MECH_BODY_CRIT_CHANCE_SCALE;
    /** 机械义体：暴击伤害权重 → 暴击伤害换算倍率 */
    public static ForgeConfigSpec.DoubleValue MECH_BODY_CRIT_DAMAGE_SCALE;
    /** 机械义体：攻击范围权重 → 攻击距离换算倍率 */
    public static ForgeConfigSpec.DoubleValue MECH_BODY_RANGE_SCALE;
    /** 机械义体：闪避权重 → 闪避率换算倍率 */
    public static ForgeConfigSpec.DoubleValue MECH_BODY_DODGE_SCALE;

    // ==================== 基因属性权重 ====================
    /** 基因属性权重：生物专精属性轴的最大加成比例（最强轴 ×(1+k)） */
    public static ForgeConfigSpec.DoubleValue GENE_WEIGHT_STRENGTH;

    // ==================== 底层战斗（暴击/闪避） ====================
    /** 底层战斗：暴击总开关 */
    public static ForgeConfigSpec.BooleanValue COMBAT_CRIT_ENABLED;
    /** 底层战斗：闪避总开关 */
    public static ForgeConfigSpec.BooleanValue COMBAT_DODGE_ENABLED;
    /** 底层战斗：暴击率上限 */
    public static ForgeConfigSpec.DoubleValue COMBAT_CRIT_CHANCE_CAP;
    /** 底层战斗：暴击伤害上限（追加倍率口径） */
    public static ForgeConfigSpec.DoubleValue COMBAT_CRIT_DAMAGE_CAP;
    /** 底层战斗：闪避上限 */
    public static ForgeConfigSpec.DoubleValue COMBAT_DODGE_CHANCE_CAP;

    // ==================== 机制开关 ====================
    public static ForgeConfigSpec.BooleanValue DECAY_ZONE_ENABLED;
    public static ForgeConfigSpec.BooleanValue SUNLIGHT_BURN_ENABLED;
    public static ForgeConfigSpec.BooleanValue OVERLOAD_ENABLED;

    // ==================== 理智系统 ====================
    /** 理智系统总开关（false = 环境规则/暗处状态机/食补窗口全不推进、HUD 不绘制；调试指令只可查询） */
    public static ForgeConfigSpec.BooleanValue SANITY_ENABLED;

    private MechSpecs() {
    }

    /** 按拆分前的定义顺序在共享 Builder 上构建本域各节（仅由 AkaishiConfig 调用） */
    public static void build(ForgeConfigSpec.Builder b) {
        // ==================== 机械改造机器 ====================
        b.push("mechanical_machines");
        MECH_CHISHI_CAPACITY = b.comment("机械三机赤能源缓冲容量（共用）")
                .defineInRange("chishiCapacity", 100_000L, 0L, Long.MAX_VALUE);
        MECH_LIFE_CAPACITY = b.comment("机械三机生命能量缓冲容量（共用）")
                .defineInRange("lifeCapacity", 20_000L, 0L, Long.MAX_VALUE);
        MECH_TEMPLATE_CHISHI_COST = b.comment("模板制造厂：塑形一次赤能源消耗")
                .defineInRange("templateChishiCost", 1_000L, 0L, Long.MAX_VALUE);
        MECH_TEMPLATE_LIFE_COST = b.comment("模板制造厂：塑形一次生命能量消耗")
                .defineInRange("templateLifeCost", 1_000L, 0L, Long.MAX_VALUE);
        MECH_TEMPLATE_TICKS = b.comment("模板制造厂：塑形一次耗时 (tick)")
                .defineInRange("templateTicks", 60, 1, Integer.MAX_VALUE);
        MECH_PROCESS_CHISHI_BASE = b.comment("加工厂：器官基价赤能（下标=器官序数 0~8：眼/心/肺/内脏/肾/左臂/右臂/左腿/右腿）；0 = 用内置默认")
                .defineList("processChishiBase", List.of(2_000L, 4_000L, 2_000L, 4_000L, 2_000L, 3_000L, 3_000L, 3_000L, 3_000L),
                        (Object o) -> o instanceof Number n && n.longValue() >= 0);
        MECH_PROCESS_LIFE_BASE = b.comment("加工厂：器官基价生命能（下标同上）；0 = 用内置默认")
                .defineList("processLifeBase", List.of(2_000L, 4_000L, 2_000L, 4_000L, 2_000L, 2_000L, 2_000L, 2_000L, 2_000L),
                        (Object o) -> o instanceof Number n && n.longValue() >= 0);
        MECH_PROCESS_TICKS_BASE = b.comment("加工厂：器官耗时基价 tick（下标同上）；0 = 用内置默认")
                .defineList("processTicksBase", List.of(80, 160, 80, 160, 80, 120, 120, 120, 120),
                        (Object o) -> o instanceof Number n && n.intValue() >= 0);
        MECH_PROCESS_PART_FACTOR = b.comment("加工厂：部件系数百分数（下标=部件序数 0~3：核心/模块/外壳/散热；核心 120=×1.2、模块 100、外壳 90、散热 110）；0 = 用内置默认")
                .defineList("processPartFactor", List.of(120, 100, 90, 110),
                        (Object o) -> o instanceof Number n && n.intValue() >= 0);
        MECH_PROCESS_MATERIAL_COUNT = b.comment("加工厂：材料消耗份数（下标=部件序数 0~3：核心 3/模块 2/外壳 4/散热 2，与材料等级无关）；0 = 用内置默认")
                .defineList("processMaterialCount", List.of(3, 2, 4, 2),
                        (Object o) -> o instanceof Number n && n.intValue() >= 0);
        MECH_ASSEMBLY_CHISHI_COST = b.comment("组装台：组装一次赤能源消耗（固定，与器官无关）")
                .defineInRange("assemblyChishiCost", 2_000L, 0L, Long.MAX_VALUE);
        MECH_ASSEMBLY_LIFE_COST = b.comment("组装台：组装一次生命能量消耗（固定）")
                .defineInRange("assemblyLifeCost", 2_000L, 0L, Long.MAX_VALUE);
        MECH_ASSEMBLY_TICKS = b.comment("组装台：组装一次耗时 (tick)")
                .defineInRange("assemblyTicks", 80, 1, Integer.MAX_VALUE);
        b.pop();

        // ==================== 机械义体属性换算 ====================
        b.push("mechanical_body");
        MECH_BODY_HEALTH_SCALE = b.comment("机械义体：生命值权重 → 生命上限换算倍率（实际加成 = 聚合权重 × 整合度折扣 × 总体倍率 × 该倍率）；0 = 用内置 0.03")
                .defineInRange("healthScale", 0.03, 0.0, 1000.0);
        MECH_BODY_ATTACK_SCALE = b.comment("机械义体：攻击伤害权重 → 攻击伤害换算倍率；0 = 用内置 0.03")
                .defineInRange("attackScale", 0.03, 0.0, 1000.0);
        MECH_BODY_ATTACK_SPEED_SCALE = b.comment("机械义体：攻击速度权重 → 攻击速度换算倍率；0 = 用内置 0.01")
                .defineInRange("attackSpeedScale", 0.01, 0.0, 1000.0);
        MECH_BODY_MOVEMENT_SPEED_SCALE = b.comment("机械义体：移动速度权重 → 移动速度换算倍率；0 = 用内置 0.01")
                .defineInRange("movementSpeedScale", 0.01, 0.0, 1000.0);
        MECH_BODY_ARMOR_SCALE = b.comment("机械义体：护甲权重 → 护甲值换算倍率；0 = 用内置 0.02")
                .defineInRange("armorScale", 0.02, 0.0, 1000.0);
        MECH_BODY_CRIT_CHANCE_SCALE = b.comment("机械义体：暴击率权重 → 暴击率换算倍率（最终受暴击率上限约束）；0 = 用内置 0.1")
                .defineInRange("critChanceScale", 0.1, 0.0, 1000.0);
        MECH_BODY_CRIT_DAMAGE_SCALE = b.comment("机械义体：暴击伤害权重 → 暴击伤害换算倍率（追加倍率口径，最终受暴击伤害上限约束）；0 = 用内置 0.2")
                .defineInRange("critDamageScale", 0.2, 0.0, 1000.0);
        MECH_BODY_RANGE_SCALE = b.comment("机械义体：攻击范围权重 → 攻击距离换算倍率（单位：格）；0 = 用内置 0.02")
                .defineInRange("rangeScale", 0.02, 0.0, 1000.0);
        MECH_BODY_DODGE_SCALE = b.comment("机械义体：闪避权重 → 闪避率换算倍率（最终受闪避上限约束）；0 = 用内置 0.1")
                .defineInRange("dodgeScale", 0.1, 0.0, 1000.0);
        b.pop();

        // ==================== 基因属性权重 ====================
        b.push("gene_weight");
        GENE_WEIGHT_STRENGTH = b.comment("基因属性权重：生物专精轴最大加成比例（最强轴 ×(1+k)，其余按强度占比递减）；0 = 用内置 0.25")
                .defineInRange("strength", 0.25, 0.0, 2.0);
        b.pop();

        // ==================== 底层战斗（暴击/闪避） ====================
        b.push("combat");
        COMBAT_CRIT_ENABLED = b.comment("底层战斗：暴击总开关；false 时暴击率/暴击伤害不参与结算")
                .define("critEnabled", true);
        COMBAT_DODGE_ENABLED = b.comment("底层战斗：闪避总开关；false 时闪避不参与结算")
                .define("dodgeEnabled", true);
        COMBAT_CRIT_CHANCE_CAP = b.comment("底层战斗：暴击率上限（0~1）；0 = 用内置 1.0")
                .defineInRange("critChanceCap", 1.0, 0.0, 1.0);
        COMBAT_CRIT_DAMAGE_CAP = b.comment("底层战斗：暴击伤害上限（追加倍率，0.5 = 最终 ×1.5）；0 = 用内置 5.0")
                .defineInRange("critDamageCap", 5.0, 0.0, 100.0);
        COMBAT_DODGE_CHANCE_CAP = b.comment("底层战斗：闪避上限（0~1）；0 = 用内置 0.8")
                .defineInRange("dodgeChanceCap", 0.8, 0.0, 1.0);
        b.pop();

        // ==================== 机器全局倍率 ====================
        b.push("machine");
        MACHINE_WORK_SPEED = b.comment("全部可升级加工机器的全局速度倍率（与机器自身速度升级叠乘；1.0 = 不变）")
                .defineInRange("workSpeed", 1.0, 0.05, 10.0);
        MACHINE_COST_MULTIPLIER = b.comment("持续耗能机器的全局运行能耗倍率（仅放大运行扣费，活化器/重铸仪等固定工艺费不变；1.0 = 不变）")
                .defineInRange("costMultiplier", 1.0, 0.05, 10.0);
        b.pop();

        // ==================== 机制开关 ====================
        b.push("toggles");
        DECAY_ZONE_ENABLED = b.comment("是否生成衰竭区域（管道/桶/反应堆泄漏等）")
                .define("decayZone", true);
        SUNLIGHT_BURN_ENABLED = b.comment("日光自燃负面被动是否生效（骷髅腿 / 幻翼肺）")
                .define("sunlightBurn", true);
        OVERLOAD_ENABLED = b.comment("躯体超载减益（按总排斥结算）是否生效")
                .define("overload", true);
        SANITY_ENABLED = b.comment("理智系统总开关：",
                        "false = 环境规则与暗处状态机不推进、食补窗口不分摊、客户端 HUD 不绘制（整套停摆）",
                        "调试指令 /akaishi sanity get 仍可查询，但 set / env / food 会被拒绝",
                        "该值随配置同步包下发，专用服务器上客户端不会出现「服务端关了、HUD 还在画」的分歧")
                .define("sanityEnabled", true);
        b.pop();
    }
}
