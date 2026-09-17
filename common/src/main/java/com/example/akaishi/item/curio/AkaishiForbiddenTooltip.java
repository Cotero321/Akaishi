package com.example.akaishi.item.curio;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 「禁忌」四件套装的 tooltip 文案组装（纯展示，不参与效果判定）。
 *
 * <ul>
 *   <li>{@link #appendSetEffectLines}：静态说明（套装四条效果），无玩家依赖，物品自身即可渲染；</li>
 *   <li>{@link #appendSetStatus}：实时状态（已集齐 / 还差几件）+ 母神台词收尾，
 *       需要当前玩家数据，由平台侧（forge ItemTooltipEvent）在客户端注入。</li>
 * </ul>
 *
 * <p>配色随文案自带（lang 内已含 §5/§7/§8），故此处不再叠加样式，避免与 lang 冲突。</p>
 */
public final class AkaishiForbiddenTooltip {

    private static final String KEY = "item.akaishi.curio.forbidden.";

    /** 成套装所需件数（D1：四件各占一槽，集齐四件即激活） */
    public static final int SET_PIECES = 4;

    /** 套装说明条目数（与件数解耦：效果条目可增删而不影响激活判定） */
    private static final int SET_LINES = 6;

    private AkaishiForbiddenTooltip() {
    }

    /** 套装效果说明区（静态）：标题行 + 效果条目，避免被误读为单件自带效果 */
    public static void appendSetEffectLines(List<Component> tooltip) {
        tooltip.add(AkaishiTooltipFx.crawling(Component.translatable(KEY + "set_header")));
        for (int i = 1; i <= SET_LINES; i++) {
            tooltip.add(AkaishiTooltipFx.crawling(Component.translatable(KEY + "set." + i)));
        }
    }

    /**
     * 实时状态区（须在客户端渲染、有悬停玩家时调用）：
     * 套装是否激活，最后以母神台词收尾。
     *
     * @param worn 当前佩戴的禁忌件数
     */
    public static void appendSetStatus(List<Component> tooltip, int worn) {
        if (worn >= SET_PIECES) {
            tooltip.add(AkaishiTooltipFx.crawling(Component.translatable(KEY + "set_active")));
        } else {
            tooltip.add(AkaishiTooltipFx.crawling(Component.translatable(KEY + "set_inactive", SET_PIECES - worn)));
        }
        tooltip.add(AkaishiTooltipFx.crawling(Component.translatable("item.akaishi.life_fusion.tooltip.blessing")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC)));
    }
}
