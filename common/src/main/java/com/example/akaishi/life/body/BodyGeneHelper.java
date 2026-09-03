package com.example.akaishi.life.body;

import com.example.akaishi.life.organ.AkaishiOrganItem;
import net.minecraft.world.item.ItemStack;

/**
 * 身体基因强化工具：永久药剂吸收的基因型（来源 → 适配加成）以"身体侧修饰"生效——
 * 该来源器官的"有效适配度" = 器官自身适配 + 身体加成，参与排斥速率、属性倍率、部位 debuff 判定。
 * 不修改器官 NBT，故在基因管理器卸载后加成立即整体撤销，未来移植的同来源器官也自动享受。
 * 突破药剂激活期内（30 分钟），该来源额外获得适配加成且可临时突破 100 上限。
 */
public final class BodyGeneHelper {

    private BodyGeneHelper() {
    }

    /** 同源套装阈值：≥4 枚同一来源的已移植器官 → 额外适配 +5 */
    public static final int SYNERGY_TIER = 4;
    /** 同源套装适配加成（叠加于基因加成，仍受 0-100 钳制） */
    public static final int SYNERGY_COMPAT_BONUS = 5;

    /** 身体中已移植的同一生物来源非原生器官数（同源套装判定；含自身） */
    public static int sameSourceCount(IPlayerBodyState state, String entityId) {
        if (state == null || entityId == null || entityId.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (BodySlot slot : BodySlot.values()) {
            ItemStack organ = state.getOrgan(slot);
            if (organ.getItem() instanceof AkaishiOrganItem && !AkaishiOrganItem.isNative(organ)
                    && entityId.equals(AkaishiOrganItem.getEntityId(organ))) {
                n++;
            }
        }
        return n;
    }

    /** 有效适配度：器官自身适配 + 身体基因加成（0-100 钳制）；
     *  仅非原生器官计入身体加成；突破激活且来源匹配时再叠加额外适配（可临时超 100） */
    public static int effectiveCompat(IPlayerBodyState state, ItemStack organ) {
        return effectiveCompat(state, organ, 0);
    }

    /** 有效适配度（含装备额外适配）：extraCompat 为装备侧全基因适配加成（如生命融合护甲每件 +2），
     *  在 0-100 钳制前叠加，随装备穿戴/脱下即时生效 */
    public static int effectiveCompat(IPlayerBodyState state, ItemStack organ, int extraCompat) {
        if (state == null || organ == null || organ.isEmpty()
                || !(organ.getItem() instanceof AkaishiOrganItem) || AkaishiOrganItem.isNative(organ)) {
            return organ != null && !organ.isEmpty() ? AkaishiOrganItem.getCompat(organ) : 0;
        }
        int compat = AkaishiOrganItem.getCompat(organ);
        String entityId = AkaishiOrganItem.getEntityId(organ);
        if (!entityId.isEmpty()) {
            compat += state.getGeneBonus(entityId);
            // 同源套装：专注同一生物来源（≥4 枚）额外适配 +5
            if (sameSourceCount(state, entityId) >= SYNERGY_TIER) {
                compat += SYNERGY_COMPAT_BONUS;
            }
        }
        // 装备全基因适配加成（穿戴即生效，参与排斥速率 / 属性倍率 / debuff 阈值）
        compat += extraCompat;
        int effective = Math.min(AkaishiOrganItem.MAX_COMPAT, compat);
        // 突破激活且来源匹配：叠加额外适配（突破 100 上限，激活结束后自动回落）
        if (!entityId.isEmpty() && state.isBreakthroughActive(entityId)) {
            effective += state.getBreakthroughExtra();
        }
        return effective;
    }
}
