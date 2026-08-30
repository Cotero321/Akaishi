package com.example.template.menu;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 界面自绘控件工具：统一槽位框/按钮的绘制样式，供各 Screen 复用。
 * 仅客户端渲染代码调用，颜色与既有自绘界面（手术仓/药剂台）保持一致。
 */
public final class GuiWidgets {

    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_SLOT_DARK = 0xFF373737;
    private static final int COLOR_SLOT_LIGHT = 0xFFFFFFFF;

    private GuiWidgets() {
    }

    /** 绘制 18×18 原版风格槽位框（灰体内凹：上左暗边、下右亮边） */
    public static void slotBox(GuiGraphics gui, int x, int y) {
        gui.fill(x, y, x + 18, y + 18, COLOR_SLOT);
        gui.fill(x, y, x + 18, y + 1, COLOR_SLOT_DARK);
        gui.fill(x, y, x + 1, y + 18, COLOR_SLOT_DARK);
        gui.fill(x, y + 17, x + 18, y + 18, COLOR_SLOT_LIGHT);
        gui.fill(x + 17, y, x + 18, y + 18, COLOR_SLOT_LIGHT);
    }

    /** 绘制原版风格轨道框（能量/液体/进度条底槽，内凹样式） */
    public static void track(GuiGraphics gui, int x, int y, int w, int h) {
        gui.fill(x, y, x + w, y + h, COLOR_SLOT);
        gui.fill(x, y, x + w, y + 1, COLOR_SLOT_DARK);
        gui.fill(x, y, x + 1, y + h, COLOR_SLOT_DARK);
        gui.fill(x, y + h - 1, x + w, y + h, COLOR_SLOT_LIGHT);
        gui.fill(x + w - 1, y, x + w, y + h, COLOR_SLOT_LIGHT);
    }
}
