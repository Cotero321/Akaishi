package com.example.akaishi.item;

import com.example.akaishi.life.body.IPlayerBodyState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 「动力护甲套装」数值查询的跨模块钩子（P3d 依赖倒置）。
 *
 * <p>生命融合护甲已按三模块定型迁往 {@code akaishi_forbidden}，但本体的器官/战斗系统
 * （{@code AkaishiBodyPassiveHandler} / {@code AkaishiBodyCombatHandler} / {@code AkaishiModForge}
 * / {@code AkaishiUpgradeHelper}）仍需读取套装派生数值与谓词。故在本体侧只定义抽象，
 * 由可选内容模块在初始化时注入实现；缺省实现全部返回中性值（未装禁忌包时生命融合护甲根本不存在，
 * 行为与既有版本逐位一致）。
 */
public final class PoweredArmorQuery {

    /** 套装数值与谓词查询 */
    public interface Query {
        /** 该物品是否为套装护甲（生命融合护甲） */
        boolean isSetArmor(ItemStack stack);

        /** 已穿戴件数 */
        int countWorn(Player player);

        /** 是否穿齐（全套激活） */
        boolean fullSet(Player player);

        /** 每件提供的全基因适配加成 × 件数 */
        int geneCompatBonus(Player player);

        /** 全套时生效器官属性强度倍率（未全套为 1.0） */
        double organStrengthMultiplier(Player player);

        /** 全套时排斥增长倍率（未全套为 1.0） */
        double rejectionSlowFactor(Player player);

        /** 全套且含 BOSS/龙族生效器官时追加的最大生命值（否则 0） */
        double bossDragonHealthBonus(Player player, IPlayerBodyState state);

        /** 体内是否存在生效的 BOSS/龙族来源器官 */
        boolean hasBossOrDragonOrgan(Player player, IPlayerBodyState state);

        /** 每抵消 1 点伤害消耗的能量 */
        long energyPerDamage();

        /** 从背包便携能量单元抽取能量，返回实际抽取量 */
        long drainEnergy(Player player, long amount);

        /** 便携能量单元修复套装护甲的速率倍率 */
        int repairMultiplier();
    }

    private static volatile Query query = new Query() {
        @Override
        public boolean isSetArmor(ItemStack stack) {
            return false;
        }

        @Override
        public int countWorn(Player player) {
            return 0;
        }

        @Override
        public boolean fullSet(Player player) {
            return false;
        }

        @Override
        public int geneCompatBonus(Player player) {
            return 0;
        }

        @Override
        public double organStrengthMultiplier(Player player) {
            return 1.0;
        }

        @Override
        public double rejectionSlowFactor(Player player) {
            return 1.0;
        }

        @Override
        public double bossDragonHealthBonus(Player player, IPlayerBodyState state) {
            return 0.0;
        }

        @Override
        public boolean hasBossOrDragonOrgan(Player player, IPlayerBodyState state) {
            return false;
        }

        @Override
        public long energyPerDamage() {
            return 1;
        }

        @Override
        public long drainEnergy(Player player, long amount) {
            return 0;
        }

        @Override
        public int repairMultiplier() {
            return 1;
        }
    };

    private PoweredArmorQuery() {
    }

    /** 由可选内容模块注入实现（null 忽略） */
    public static void install(Query impl) {
        if (impl != null) {
            query = impl;
        }
    }

    public static boolean isSetArmor(ItemStack stack) {
        return query.isSetArmor(stack);
    }

    public static int countWorn(Player player) {
        return query.countWorn(player);
    }

    public static boolean fullSet(Player player) {
        return query.fullSet(player);
    }

    public static int geneCompatBonus(Player player) {
        return query.geneCompatBonus(player);
    }

    public static double organStrengthMultiplier(Player player) {
        return query.organStrengthMultiplier(player);
    }

    public static double rejectionSlowFactor(Player player) {
        return query.rejectionSlowFactor(player);
    }

    public static double bossDragonHealthBonus(Player player, IPlayerBodyState state) {
        return query.bossDragonHealthBonus(player, state);
    }

    public static boolean hasBossOrDragonOrgan(Player player, IPlayerBodyState state) {
        return query.hasBossOrDragonOrgan(player, state);
    }

    public static long energyPerDamage() {
        return query.energyPerDamage();
    }

    public static long drainEnergy(Player player, long amount) {
        return query.drainEnergy(player, amount);
    }

    public static int repairMultiplier() {
        return query.repairMultiplier();
    }
}
