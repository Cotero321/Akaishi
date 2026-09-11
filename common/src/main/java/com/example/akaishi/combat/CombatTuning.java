package com.example.akaishi.combat;

import com.example.akaishi.config.ModConfig;

/**
 * 底层战斗（暴击/闪避）调参读取端：统一解析配置的"0 = 用内置默认"约定。
 * 数值全部由配置管道（AkaishiConfig → AkaishiConfigSync → ModConfig）下发，
 * 此处只做解析，禁止旁路直读 ForgeConfigSpec。
 */
public final class CombatTuning {

    /** 暴击率内置上限（配置为 0 时使用） */
    private static final float DEFAULT_CRIT_CHANCE_CAP = 1.0F;
    /** 暴击伤害内置上限（配置为 0 时使用；追加倍率口径，5.0 = 最终 ×6） */
    private static final float DEFAULT_CRIT_DAMAGE_CAP = 5.0F;
    /** 闪避内置上限（配置为 0 时使用；留缺口避免物理无敌） */
    private static final float DEFAULT_DODGE_CHANCE_CAP = 0.8F;

    private CombatTuning() {
    }

    /** 暴击是否参与结算 */
    public static boolean critEnabled() {
        return ModConfig.combatCritEnabled;
    }

    /** 闪避是否参与结算 */
    public static boolean dodgeEnabled() {
        return ModConfig.combatDodgeEnabled;
    }

    /** 暴击率有效上限（0 = 用内置） */
    public static float critChanceCap() {
        return resolve(ModConfig.combatCritChanceCap, DEFAULT_CRIT_CHANCE_CAP);
    }

    /** 暴击伤害有效上限（0 = 用内置） */
    public static float critDamageCap() {
        return resolve(ModConfig.combatCritDamageCap, DEFAULT_CRIT_DAMAGE_CAP);
    }

    /** 闪避有效上限（0 = 用内置） */
    public static float dodgeChanceCap() {
        return resolve(ModConfig.combatDodgeChanceCap, DEFAULT_DODGE_CHANCE_CAP);
    }

    /** 配置值 ≤ 0 视为"用内置默认" */
    private static float resolve(double configured, float builtin) {
        return configured > 0.0 ? (float) configured : builtin;
    }
}
