package com.example.akaishi.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 生命融合护甲 tooltip 文案组装（纯展示，不参与效果判定）。
 * - appendEffectLines：静态说明（每件加成 / 全套效果 / 条件强化），无玩家依赖，物品自身即可渲染；
 * - appendSetStatus：实时状态（已穿件数 / 全套激活 / BOSS·龙条件）+ 母神台词收尾，
 *   需要当前玩家数据，由平台侧（forge ItemTooltipEvent）在客户端注入。
 * 数值统一取自 {@link AkaishiLifeFusionSet} 常量，避免两处硬编码漂移。
 */
public final class AkaishiLifeFusionTooltip {

    private static final String KEY = "item.akaishi.life_fusion.tooltip.";

    private AkaishiLifeFusionTooltip() {
    }

    /** 效果说明区：每件 + 全套 + 条件强化（灰色正文） */
    public static void appendEffectLines(List<Component> tooltip) {
        tooltip.add(Component.translatable(KEY + "per_piece", AkaishiLifeFusionSet.GENE_COMPAT_PER_PIECE)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(KEY + "full_title", AkaishiLifeFusionSet.SET_PIECES)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(KEY + "full_flight").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(KEY + "full_organ",
                percentGain(AkaishiLifeFusionSet.ORGAN_STRENGTH_MULTIPLIER)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(KEY + "full_rejection",
                percentCut(AkaishiLifeFusionSet.REJECTION_SLOW_FACTOR)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(KEY + "full_shield", AkaishiLifeFusionSet.ENERGY_PER_DAMAGE)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(KEY + "boss_bonus",
                trimNum(AkaishiLifeFusionSet.BOSS_DRAGON_HEALTH_BONUS)).withStyle(ChatFormatting.GRAY));
    }

    /**
     * 实时状态区（须在客户端渲染、有悬停玩家时调用）：
     * 已穿件数 / 全套激活与否 / BOSS·龙强化条件满足与否，最后以母神台词收尾。
     */
    public static void appendSetStatus(List<Component> tooltip, int worn, boolean hasBossOrDragon) {
        boolean full = worn >= AkaishiLifeFusionSet.SET_PIECES;
        tooltip.add(Component.translatable(KEY + "worn", worn, AkaishiLifeFusionSet.SET_PIECES)
                .withStyle(ChatFormatting.GRAY));
        if (full) {
            tooltip.add(Component.translatable(KEY + "set_active").withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable(KEY + "set_inactive",
                    AkaishiLifeFusionSet.SET_PIECES - worn).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (full && hasBossOrDragon) {
            tooltip.add(Component.translatable(KEY + "boss_active").withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable(KEY + "boss_inactive").withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable(KEY + "blessing").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
    }

    /** 倍率 → 增益百分比（1.2 → 20） */
    private static long percentGain(double multiplier) {
        return Math.round((multiplier - 1.0) * 100.0);
    }

    /** 倍率 → 减缓百分比（0.75 → 25） */
    private static long percentCut(double factor) {
        return Math.round((1.0 - factor) * 100.0);
    }

    /** 数字显示：整数不带小数，小数保留 1 位 */
    private static String trimNum(double d) {
        if (Math.abs(d - Math.round(d)) < 0.05) {
            return String.valueOf((long) Math.round(d));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", d);
    }
}
