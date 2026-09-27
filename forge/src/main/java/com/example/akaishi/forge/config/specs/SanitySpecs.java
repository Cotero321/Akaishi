package com.example.akaishi.forge.config.specs;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * 配置域：理智系统与赤石饰品扩展槽。
 * 仅由 {@link com.example.akaishi.forge.config.AkaishiConfig} 的 static 初始化按固定顺序调用；
 * 键名/注释/默认值/范围与拆分前逐字一致，调用顺序即 common.toml 分节顺序。
 */
public final class SanitySpecs {

    /** 暗处机制：重见光明后的冷却（tick，0 = 用内置默认 18000） */
    public static ForgeConfigSpec.IntValue SANITY_DARK_LIGHT_COOLDOWN_TICKS;
    /** 食补：连续食用衰减的重置时间（tick，0 = 用内置默认 18000） */
    public static ForgeConfigSpec.IntValue SANITY_FOOD_REFRESH_TICKS;
    /** 下界顶部基岩层判据 Y（0 = 用内置默认 128） */
    public static ForgeConfigSpec.IntValue SANITY_NETHER_ROOF_Y;
    /** 自然恢复基础周期（tick，0 = 用内置默认 6000 = 5 分钟） */
    public static ForgeConfigSpec.IntValue SANITY_NATURAL_REGEN_PERIOD_TICKS;
    /** 自然恢复的花丛加成所需花朵数量阈值（0 = 用内置默认 10） */
    public static ForgeConfigSpec.IntValue SANITY_FLOWER_COUNT_THRESHOLD;
    /** 睡眠剥夺：连续多少个游戏日不睡开始惩罚（0 = 用内置默认 5） */
    public static ForgeConfigSpec.IntValue SANITY_SLEEP_DEPRIVATION_DAYS;
    /** 睡眠剥夺：每个游戏日扣减的 SAN（0 = 用内置默认 10） */
    public static ForgeConfigSpec.DoubleValue SANITY_SLEEP_DEPRIVATION_DAILY_DEBIT;

    // ---- 赤石饰品扩展槽 ----
    public static ForgeConfigSpec.BooleanValue CURIO_SLOT_UNLOCK_REQUIRED;
    public static ForgeConfigSpec.ConfigValue<List<? extends Integer>> CURIO_SLOT_UNLOCK_THRESHOLDS;

    private SanitySpecs() {
    }

    /** 按拆分前的定义顺序在共享 Builder 上构建本域各节（仅由 AkaishiConfig 调用） */
    public static void build(ForgeConfigSpec.Builder b) {
        // ==================== 理智系统 ====================
        // 总开关与其它机制开关同放 toggles（便于一键全关）；三项手感时长单列一节
        b.push("sanity");
        SANITY_DARK_LIGHT_COOLDOWN_TICKS = b.comment("暗处机制：玩家「重见光明」（直见天空且白天）后的冷却 (tick)；",
                        "冷却期内 dark_high / dark_low 两条规则都不扣，冷却结束且仍在暗处则重新开始一次暴露周期；",
                        "0 = 用内置默认 18000（15 分钟）")
                .defineInRange("darkLightCooldownTicks", 18_000, 0, Integer.MAX_VALUE);
        SANITY_FOOD_REFRESH_TICKS = b.comment("食补：连续食用衰减的重置时间 (tick)；",
                        "超过该时长再吃同一物品，效力回到第 1 档（100%）；",
                        "0 = 用内置默认 18000（15 分钟）")
                .defineInRange("foodRefreshTicks", 18_000, 0, Integer.MAX_VALUE);
        SANITY_NETHER_ROOF_Y = b.comment("下界顶部基岩层判据：脚部 Y ≥ 该值视为身处下界顶部（每 60s 扣 5 理智）；",
                        "1.20.1 下界基岩顶面为 y=127，站上顶部即 y≥128，故默认 128；",
                        "0 = 用内置默认 128")
                .defineInRange("netherRoofY", 128, 0, 320);
        SANITY_NATURAL_REGEN_PERIOD_TICKS = b.comment("自然恢复：每多少 tick 回 1 点理智（基础周期）；",
                        "生效条件 = 有顶（不可见天）+ 在地面 + 脚部方块光 > 10；",
                        "效率加成（乘在速度上，用进度累积器落地，不会被取整抹平）：",
                        "  周围花朵数 > 阈值 +10%（不叠加）、饱食度满 +10%、再乘 COG 的自然恢复倍率（×1/×1.5/×2）；",
                        "0 = 用内置默认 6000（5 分钟）")
                .defineInRange("naturalRegenPeriodTicks", 6_000, 0, Integer.MAX_VALUE);
        SANITY_FLOWER_COUNT_THRESHOLD = b.comment("自然恢复：花丛加成所需的花朵数量阈值（脚部为中心 9×3×9 盒内、按方块标签 #minecraft:flowers 计数，5s 扫描一次）；",
                        "超过该值即 +10%，不随朵数叠加；设得极大可视为关闭花丛加成；",
                        "0 = 用内置默认 10")
                .defineInRange("flowerCountThreshold", 10, 0, 1000);
        SANITY_SLEEP_DEPRIVATION_DAYS = b.comment("睡眠剥夺：连续多少个游戏日不睡开始每日扣减（游戏日 = 主世界 dayTime / 24000）；",
                        "睡过整夜（自然起床）会重置该计时；离线跨越的日界不追罚；",
                        "0 = 用内置默认 5")
                .defineInRange("sleepDeprivationDays", 5, 0, 1000);
        SANITY_SLEEP_DEPRIVATION_DAILY_DEBIT = b.comment("睡眠剥夺：每个游戏日扣减的理智（原始量，临时保护可优先抵扣）；",
                        "该状态下幻翼对玩家的咬击会额外附带一段 akaishi:psychic 精神伤害；",
                        "0 = 用内置默认 10")
                .defineInRange("sleepDeprivationDailyDebit", 10.0, 0.0, 1000.0);
        b.pop();

        // ==================== 赤石饰品扩展槽 ====================
        b.push("curio_slots");
        CURIO_SLOT_UNLOCK_REQUIRED = b.comment("赤石饰品扩展槽是否需要进度解锁（false = 四个扩展槽始终开启）")
                .define("unlockRequired", true);
        CURIO_SLOT_UNLOCK_THRESHOLDS = b.comment("四个扩展槽各自所需的赤石进度节点数 [槽1, 槽2, 槽3, 槽4]；0 = 该槽无条件开启（默认全 0，四个槽登录即全开）")
                .defineList("unlockThresholds", List.of(0, 0, 0, 0),
                        (Object o) -> o instanceof Number n && n.intValue() >= 0);
        b.pop();
    }
}
