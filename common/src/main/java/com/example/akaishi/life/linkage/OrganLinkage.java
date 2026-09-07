package com.example.akaishi.life.linkage;

import com.example.akaishi.config.ModConfig;
import com.example.akaishi.life.organ.AkaishiOrganItem;
import com.example.akaishi.life.organ.QualityTier;
import com.example.akaishi.life.sample.SampleGroup;
import net.minecraft.world.item.ItemStack;

import java.util.Random;

/**
 * 基因×肢体联动计算服务（本项目核心结构）。
 * 统一承载"基因物品 → 躯体效果"的全部数值推导，供物品、方块实体与平台生效层调用，
 * 避免数值逻辑散落各处——移植排斥、排斥增长、结构台产出全部经由本服务推导。
 *
 * 三个正交联动维度（可独立调整、可增量扩展）：
 * 1. 分组强度：SampleGroup.getRejectionFactor()——强基因（末影/龙）身体负担重
 * 2. 完整度：器官 purity——基因解析质量修正排斥（纯度链路：样本纯度→序列纯度→器官完整度→排斥）
 * 3. 纯度偏置：compatRoll()——高纯度基因造出的器官适配度更接近区间上限
 *
 * 扩展点（affinityOf）：当前返回分组排斥系数作为"基因契合度"基础值，
 * 未来可接入玩家维度（玩家对特定基因的固有/习得契合）、基因组合效应、基因等级等，
 * 只需在此扩展计算，所有调用方（移植/排斥增长/结构台）自动获得新行为。
 */
public final class OrganLinkage {

    /** 完整度对排斥的最大削减比例（0.5 = 完整度 100 时排斥减半） */
    public static final double PURITY_REJECTION_CAP = 0.5;
    /** 纯度对适配度偏置的最大权重（0.3 = 纯度 100 时 30% 权重由纯度决定） */
    public static final double PURITY_COMPAT_WEIGHT = 0.3;

    private OrganLinkage() {
    }

    /** 分组排斥系数（无来源回退 1.0） */
    public static double rejectionFactorOf(SampleGroup source) {
        return source != null ? source.getRejectionFactor() : 1.0;
    }

    /** 器官 → 分组排斥系数 */
    public static double rejectionFactorOf(ItemStack organ) {
        return rejectionFactorOf(AkaishiOrganItem.getSource(organ));
    }

    /**
     * 器官移植排斥总计算 = 品质基础排斥 × 分组排斥系数 × (1 − 完整度修正)。
     * 完整度越高排斥越低（基因质量 → 契合质量），原生器官恒为 0。
     * AkaishiOrganItem.getBaseRejection 委托本方法，移植/冲突解除/tooltip 自动同步。
     */
    public static int rejectionOf(ItemStack organ) {
        if (AkaishiOrganItem.isNative(organ)) {
            return 0;
        }
        QualityTier tier = AkaishiOrganItem.getTier(organ);
        if (tier == null) {
            return 0;
        }
        double factor = rejectionFactorOf(organ);
        double cap = ModConfig.purityRejectionCap > 0 ? ModConfig.purityRejectionCap : PURITY_REJECTION_CAP;
        double purityMod = 1.0 - (AkaishiOrganItem.getPurity(organ) / 100.0) * cap;
        return (int) Math.max(0, Math.round(tier.getBaseRejection() * factor * purityMod));
    }

    /**
     * 适配度随机偏置：纯度越高越接近分组区间上限。
     * compat = min + range × (纯度权重 × purity/100 + 随机权重 × rand²)。
     * rand² 保持"随机偏下端"的原始分布特性，纯度仅将整体向区间上限牵引。
     */
    public static int compatRoll(SampleGroup source, int purity, Random random) {
        if (source == null) {
            return AkaishiOrganItem.NATIVE_COMPAT;
        }
        int range = Math.max(1, source.getCompatMax() - source.getCompatMin());
        double weight = ModConfig.purityCompatWeight > 0 ? ModConfig.purityCompatWeight : PURITY_COMPAT_WEIGHT;
        double purityBias = weight * Math.max(0, Math.min(100, purity)) / 100.0;
        double roll = (1.0 - purityBias) * random.nextDouble() * random.nextDouble() + purityBias;
        return Math.min(AkaishiOrganItem.MAX_COMPAT, source.getCompatMin() + (int) (range * roll));
    }

    /**
     * 基因契合度（扩展点）：当前返回分组排斥系数（值越低越契合身体）。
     * 预留：未来接入玩家维度（习得契合/天生亲和）、基因组合效应、基因等级等，
     * 新维度只需在此扩展签名与计算，调用方无需感知。
     */
    public static double affinityOf(SampleGroup source, String entityId) {
        return rejectionFactorOf(source);
    }
}
