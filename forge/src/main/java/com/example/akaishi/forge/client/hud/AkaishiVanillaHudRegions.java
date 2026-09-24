package com.example.akaishi.forge.client.hud;

/**
 * 原版 HUD 关键区域表（1.20.1）：给出"当前屏幕尺寸下，哪些矩形被原版 HUD 占用"，
 * 供统一渲染层做<b>几何</b>避让判定。
 *
 * <p><b>判据是几何，不是探测已注册元素</b>：本层无法遍历原版 overlay（Forge 未暴露"某一层占了哪块矩形"），
 * 且即便能遍历也拿不到矩形信息。因此这里按 1.20.1 原版 {@code Gui} 的<b>硬编码像素布局</b>反推保留区，
 * 是一份"已知常量表"而非运行时探测 —— 好处是零成本、可预测；代价是原版改版要同步（本项目锁 1.20.1，无此风险）。
 *
 * <p><b>为什么全都按"居中 + 底边"推导</b>：1.20.1 的主 HUD 元素（快捷栏 / 经验条 / 生命 / 饥饿 / 护甲 / 氧气）
 * 全部以 {@code screenWidth/2} 为中轴、以 {@code screenHeight} 为基准向上排列，故一张表即可覆盖所有缩放。
 *
 * <p>缓存：屏幕尺寸不变时复用同一份数组（只在窗口缩放时重建一次），稳态下渲染路径零分配。
 */
final class AkaishiVanillaHudRegions {

    /** 快捷栏：宽 182、高 22，中轴居中，贴屏幕底边。 */
    private static final int HOTBAR_WIDTH = 182;
    private static final int HOTBAR_HEIGHT = 22;
    /** 经验条：宽 182、高 5，底边在快捷栏顶边之上 7px（原版 {@code screenHeight - 29}）。 */
    private static final int XP_WIDTH = 182;
    private static final int XP_BOTTOM_OFFSET = 29;
    private static final int XP_HEIGHT = 5;
    /** 生命 / 饥饿条：各 81×9，原版 y = {@code screenHeight - 39}。 */
    private static final int STATUS_WIDTH = 81;
    private static final int STATUS_BOTTOM_OFFSET = 39;
    private static final int STATUS_HEIGHT = 9;
    /** 护甲 / 氧气：同宽同高，在原版里画在生命/饥饿之上 10px。 */
    private static final int UPPER_BOTTOM_OFFSET = 49;
    /** BOSS 血条：原版 {@code BOSS_EVENT_PROGRESS} 用 y 从 12 起、宽 182、高 19（本项目铭牌血条同区）。 */
    private static final int BOSS_BAR_TOP = 12;
    private static final int BOSS_BAR_WIDTH = 182;
    private static final int BOSS_BAR_HEIGHT = 19;
    /** 等级数字保留盒：数字居中于经验条上方，按最宽三位数 + 余量取 48×9（保守）。 */
    private static final int LEVEL_WIDTH = 48;
    private static final int LEVEL_HEIGHT = 9;
    private static final int LEVEL_BOTTOM_OFFSET = 38;

    private static int cachedWidth = Integer.MIN_VALUE;
    private static int cachedHeight = Integer.MIN_VALUE;
    private static HudLayoutRect[] cached = new HudLayoutRect[0];

    private AkaishiVanillaHudRegions() {
    }

    /** 当前屏幕尺寸下的原版保留区（只读，勿改内容；尺寸变化时才会重建）。 */
    static HudLayoutRect[] regions(int screenWidth, int screenHeight) {
        if (screenWidth == cachedWidth && screenHeight == cachedHeight) {
            return cached;
        }
        int center = screenWidth / 2;
        int bottom = screenHeight;
        cached = new HudLayoutRect[] {
                // 快捷栏
                new HudLayoutRect(center - HOTBAR_WIDTH / 2, bottom - HOTBAR_HEIGHT, HOTBAR_WIDTH, HOTBAR_HEIGHT),
                // 经验条（含等级数字上方的净区）
                new HudLayoutRect(center - XP_WIDTH / 2, bottom - XP_BOTTOM_OFFSET, XP_WIDTH, XP_HEIGHT),
                new HudLayoutRect(center - LEVEL_WIDTH / 2, bottom - LEVEL_BOTTOM_OFFSET, LEVEL_WIDTH, LEVEL_HEIGHT),
                // 生命 / 饥饿
                new HudLayoutRect(center - HOTBAR_WIDTH / 2, bottom - STATUS_BOTTOM_OFFSET, STATUS_WIDTH, STATUS_HEIGHT),
                new HudLayoutRect(center + HOTBAR_WIDTH / 2 - STATUS_WIDTH, bottom - STATUS_BOTTOM_OFFSET,
                        STATUS_WIDTH, STATUS_HEIGHT),
                // 护甲 / 氧气
                new HudLayoutRect(center - HOTBAR_WIDTH / 2, bottom - UPPER_BOTTOM_OFFSET, STATUS_WIDTH, STATUS_HEIGHT),
                new HudLayoutRect(center + HOTBAR_WIDTH / 2 - STATUS_WIDTH, bottom - UPPER_BOTTOM_OFFSET,
                        STATUS_WIDTH, STATUS_HEIGHT),
                // BOSS 血条带（本项目阿盖托洛丝铭牌血条同区，视作保留）
                new HudLayoutRect(center - BOSS_BAR_WIDTH / 2, BOSS_BAR_TOP, BOSS_BAR_WIDTH, BOSS_BAR_HEIGHT),
        };
        cachedWidth = screenWidth;
        cachedHeight = screenHeight;
        return cached;
    }
}
