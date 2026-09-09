package com.example.akaishi.life.mechanical;

import com.example.akaishi.config.ModConfig;

/**
 * 机械改造三台机器的费用读取：统一从配置管道（ModConfig）取值，
 * 越界/值为 0 时回退内置默认（规则：配置 0 = 不覆盖、使用内置默认）。
 * 下标语义：器官 = {@link MechanicalOrganType#ordinal()}；部件 = {@link MechanicalPartType#ordinal()}（核心0/模块1/外壳2/散热3）。
 */
public final class MechanicalMachineCosts {

    private MechanicalMachineCosts() {}

    // 内置默认：器官基价（下标 0~8 = 眼/心/肺/内脏/肾/左臂/右臂/左腿/右腿）
    private static final long[] DEFAULT_CHISHI = {2_000L, 4_000L, 2_000L, 4_000L, 2_000L, 3_000L, 3_000L, 3_000L, 3_000L};
    private static final long[] DEFAULT_LIFE = {2_000L, 4_000L, 2_000L, 4_000L, 2_000L, 2_000L, 2_000L, 2_000L, 2_000L};
    private static final int[] DEFAULT_TICKS = {80, 160, 80, 160, 80, 120, 120, 120, 120};
    // 内置默认：部件系数百分数（×1.2 / ×1.0 / ×0.9 / ×1.1）
    private static final int[] DEFAULT_PART_FACTOR = {120, 100, 90, 110};
    // 内置默认：材料消耗份数（核心3 / 模块2 / 外壳4 / 散热2，与材料等级无关）
    private static final int[] DEFAULT_MATERIAL_COUNT = {3, 2, 4, 2};

    /** 加工厂：按 器官×部件 计算总赤能消耗 = 器官基价 × 部件系数 */
    public static long processChishiCost(MechanicalOrganType organ, MechanicalPartType part) {
        long base = getOrDefault(ModConfig.mechProcessChishiBase, organ.ordinal(), DEFAULT_CHISHI);
        return base * partFactor(part) / 100L;
    }

    /** 加工厂：总生命能消耗（同上） */
    public static long processLifeCost(MechanicalOrganType organ, MechanicalPartType part) {
        long base = getOrDefault(ModConfig.mechProcessLifeBase, organ.ordinal(), DEFAULT_LIFE);
        return base * partFactor(part) / 100L;
    }

    /** 加工厂：总耗时 tick（基价 × 部件系数） */
    public static int processTicks(MechanicalOrganType organ, MechanicalPartType part) {
        int base = getOrDefaultInt(ModConfig.mechProcessTicksBase, organ.ordinal(), DEFAULT_TICKS);
        return Math.max(1, base * partFactor(part) / 100);
    }

    /** 加工厂：该部件的材料消耗份数 */
    public static int materialCount(MechanicalPartType part) {
        return getOrDefaultInt(ModConfig.mechProcessMaterialCount, part.ordinal(), DEFAULT_MATERIAL_COUNT);
    }

    /** 部件系数百分数（120=×1.2），越界回退内置默认末元素 */
    public static int partFactor(MechanicalPartType part) {
        return getOrDefaultInt(ModConfig.mechProcessPartFactor, part.ordinal(), DEFAULT_PART_FACTOR);
    }

    /** 模板制造厂：塑形一次赤能源消耗 */
    public static long templateChishiCost() {
        return ModConfig.mechTemplateChishiCost > 0 ? ModConfig.mechTemplateChishiCost : 1_000L;
    }

    /** 模板制造厂：塑形一次生命能量消耗 */
    public static long templateLifeCost() {
        return ModConfig.mechTemplateLifeCost > 0 ? ModConfig.mechTemplateLifeCost : 1_000L;
    }

    /** 模板制造厂：塑形一次耗时 tick */
    public static int templateTicks() {
        return ModConfig.mechTemplateTicks > 0 ? ModConfig.mechTemplateTicks : 60;
    }

    /** 组装台：组装一次赤能源消耗（固定） */
    public static long assemblyChishiCost() {
        return ModConfig.mechAssemblyChishiCost > 0 ? ModConfig.mechAssemblyChishiCost : 2_000L;
    }

    /** 组装台：组装一次生命能量消耗（固定） */
    public static long assemblyLifeCost() {
        return ModConfig.mechAssemblyLifeCost > 0 ? ModConfig.mechAssemblyLifeCost : 2_000L;
    }

    /** 组装台：组装一次耗时 tick */
    public static int assemblyTicks() {
        return ModConfig.mechAssemblyTicks > 0 ? ModConfig.mechAssemblyTicks : 80;
    }

    /** 赤能源缓冲容量 */
    public static long chishiCapacity() {
        return ModConfig.mechanicalChishiCapacity > 0 ? ModConfig.mechanicalChishiCapacity : 100_000L;
    }

    /** 生命能量缓冲容量 */
    public static long lifeCapacity() {
        return ModConfig.mechanicalLifeCapacity > 0 ? ModConfig.mechanicalLifeCapacity : 20_000L;
    }

    // ---- 防越界 + 0=回退 ----

    private static long getOrDefault(long[] config, int idx, long[] def) {
        if (config != null && idx >= 0 && idx < config.length && config[idx] > 0) {
            return config[idx];
        }
        // 越界时回退内置默认数组末元素（而非 1），避免费用/工时被压到极低导致近乎免费加工
        if (idx >= 0 && idx < def.length) {
            return def[idx];
        }
        return def[def.length - 1];
    }

    private static int getOrDefaultInt(int[] config, int idx, int[] def) {
        if (config != null && idx >= 0 && idx < config.length && config[idx] > 0) {
            return config[idx];
        }
        // 越界时回退内置默认数组末元素（而非 1），避免工时/材料数被压到极低
        if (idx >= 0 && idx < def.length) {
            return def[idx];
        }
        return def[def.length - 1];
    }
}