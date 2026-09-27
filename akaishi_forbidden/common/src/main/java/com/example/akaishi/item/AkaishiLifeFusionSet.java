package com.example.akaishi.item;

import com.example.akaishi.life.body.IPlayerBodyState;
import com.example.akaishi.life.organ.AkaishiOrganItem;
import com.example.akaishi.life.organ.OrganEffectResolver;
import com.example.akaishi.api.life.ISampleGroup;
import com.example.akaishi.life.sample.SampleGroup;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 生命融合装备套装：统一判定穿戴件数 / 全套激活与套装加成。
 * 每件护甲 +2 全基因适配；穿齐 4 件触发飞行 / 器官强度 20% / BOSS·龙肢体强化 / 排异减速 / 能量抵伤。
 */
public final class AkaishiLifeFusionSet {

    /** 全套件数（满 4 件激活全套效果） */
    public static final int SET_PIECES = 4;
    /** 单件护甲提供的全基因适配加成 */
    public static final int GENE_COMPAT_PER_PIECE = 2;
    /** 全套激活时器官属性强度倍率（+20%） */
    public static final double ORGAN_STRENGTH_MULTIPLIER = 1.2;
    /** 全套激活时排斥增长速度倍率（-25%） */
    public static final double REJECTION_SLOW_FACTOR = 0.75;
    /** 每抵消 1 点伤害消耗的赤能源 */
    public static final long ENERGY_PER_DAMAGE = 100;
    /** 便携赤能源单元修复生命融合护甲的速率倍率（相对普通赤石装备，恢复显著更快） */
    public static final int LIFE_FUSION_REPAIR_MULTIPLIER = 4;
    /** BOSS / 龙肢体强化：额外最大生命值 */
    public static final double BOSS_DRAGON_HEALTH_BONUS = 10.0;

    private AkaishiLifeFusionSet() {
    }

    /** 是否为生命融合护甲 */
    public static boolean isLifeFusionArmor(ItemStack stack) {
        return stack != null && stack.getItem() instanceof AkaishiLifeFusionArmorItem;
    }

    /** 已穿戴的生命融合护甲件数（0-4） */
    public static int countWorn(Player player) {
        int count = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR && isLifeFusionArmor(player.getItemBySlot(slot))) {
                count++;
            }
        }
        return count;
    }

    /** 是否穿齐 4 件（全套激活） */
    public static boolean isFullSet(Player player) {
        return countWorn(player) >= SET_PIECES;
    }

    /** 全套穿戴带来的全基因适配加成（每件 +2，可叠加） */
    public static int geneCompatBonus(Player player) {
        return countWorn(player) * GENE_COMPAT_PER_PIECE;
    }

    /** 是否移植了 BOSS / 龙族来源的生效器官（供全套 BOSS·龙肢体强化判定） */
    public static boolean hasBossOrDragonOrgan(Player player, IPlayerBodyState state) {
        if (state == null) {
            return false;
        }
        for (OrganEffectResolver.ActiveOrgan organ : OrganEffectResolver.collect(state)) {
            ISampleGroup group = OrganEffectResolver.groupOf(AkaishiOrganItem.getEntityId(organ.stack()), player.level());
            if (group == SampleGroup.BOSS || group == SampleGroup.DRAGON) {
                return true;
            }
        }
        return false;
    }

    /** 从背包便携赤能源单元中抽取能量（遍历主物品栏），返回实际抽取量 */
    public static long drainEnergy(Player player, long amount) {
        if (amount <= 0) {
            return 0;
        }
        long remaining = amount;
        for (ItemStack stack : player.getInventory().items) {
            if (remaining <= 0) {
                break;
            }
            if (!(stack.getItem() instanceof AkaishiPortableEnergyCell cell)) {
                continue;
            }
            remaining -= cell.extractEnergy(stack, remaining, false);
        }
        return amount - remaining;
    }

    /**
     * 导出为本体可消费的查询实现（P3d 依赖倒置）：
     * 本体的器官/战斗/修复逻辑经 {@link PoweredArmorQuery} 读取套装数值，由禁忌模块注入本实现。
     * 条件式（全套 ×倍率 / 未全套中性值）内联在此，保证与迁前的判定口径逐位一致。
     */
    public static PoweredArmorQuery.Query asQuery() {
        return new PoweredArmorQuery.Query() {
            @Override
            public boolean isSetArmor(ItemStack stack) {
                return isLifeFusionArmor(stack);
            }

            @Override
            public int countWorn(Player player) {
                return AkaishiLifeFusionSet.countWorn(player);
            }

            @Override
            public boolean fullSet(Player player) {
                return isFullSet(player);
            }

            @Override
            public int geneCompatBonus(Player player) {
                return AkaishiLifeFusionSet.geneCompatBonus(player);
            }

            @Override
            public double organStrengthMultiplier(Player player) {
                return isFullSet(player) ? ORGAN_STRENGTH_MULTIPLIER : 1.0;
            }

            @Override
            public double rejectionSlowFactor(Player player) {
                return isFullSet(player) ? REJECTION_SLOW_FACTOR : 1.0;
            }

            @Override
            public double bossDragonHealthBonus(Player player, IPlayerBodyState state) {
                return isFullSet(player) && hasBossOrDragonOrgan(player, state) ? BOSS_DRAGON_HEALTH_BONUS : 0.0;
            }

            @Override
            public boolean hasBossOrDragonOrgan(Player player, IPlayerBodyState state) {
                return AkaishiLifeFusionSet.hasBossOrDragonOrgan(player, state);
            }

            @Override
            public long energyPerDamage() {
                return ENERGY_PER_DAMAGE;
            }

            @Override
            public long drainEnergy(Player player, long amount) {
                return AkaishiLifeFusionSet.drainEnergy(player, amount);
            }

            @Override
            public int repairMultiplier() {
                return LIFE_FUSION_REPAIR_MULTIPLIER;
            }
        };
    }
}
