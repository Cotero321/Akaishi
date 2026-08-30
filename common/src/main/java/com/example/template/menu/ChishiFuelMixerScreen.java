package com.example.template.menu;

import com.example.template.TemplateMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 燃料混合器界面（按功能分区重设计）：
 * - 输入区：输入Ⅰ（末地混合燃料·紫）、输入Ⅱ（下界复合燃料·橙）
 * - 输出区：输出（高级/终极混合燃料·金）
 * - 能源区：赤能源（红）
 * - 进度区：混合进度（黄）
 * 每条标签文字在轨道左侧，轨道整条填充不压字；
 * 悬停显示功能名 + 当前数值。
 */
public class ChishiFuelMixerScreen extends AbstractContainerScreen<ChishiFuelMixerMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_energy_cell.png");

    /** 标签文字左对齐起点（轨道左侧，文字不压在条上） */
    private static final int LABEL_X = 20;
    /** 进度条轨道：位于标签右侧，整条填充 */
    private static final int TRACK_X = 70, TRACK_W = 86, BAR_H = 8;
    /** 五条状态条整体上移（间距 10），进度条底缘 y70 避开物品栏文字（y72 起） */
    private static final int IN1_Y = 22;
    private static final int IN2_Y = 32;
    private static final int OUT_Y = 42;
    private static final int ENERGY_Y = 52;
    private static final int PROGRESS_Y = 62;

    public ChishiFuelMixerScreen(ChishiFuelMixerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    /** 大数值缩写 */
    private static String formatEnergy(long v) {
        if (v >= 1_000_000L) {
            return trim(v / 1.0e6) + "M";
        }
        if (v >= 1_000L) {
            return trim(v / 1.0e3) + "K";
        }
        return String.valueOf(v);
    }

    private static String trim(double d) {
        if (Math.abs(d - Math.round(d)) < 0.05) {
            return String.valueOf((long) Math.round(d));
        }
        return String.format(Locale.ROOT, "%.1f", d);
    }

    /** 绘制带标签的能量/液体条：标签在轨道左侧，轨道整条填充不压字 */
    private void drawBar(GuiGraphics gui, int x, int y, String labelKey, long energy, long max, int color) {
        gui.drawString(this.font, Component.translatable(labelKey), x + LABEL_X, y + 1, 0xFF3F3F3F, false);
        GuiWidgets.track(gui, x + TRACK_X, y, TRACK_W, BAR_H);
        long clamped = Math.max(0, Math.min(energy, max));
        long cap = Math.max(1, max);
        int barWidth = (int) (TRACK_W * clamped / cap);
        if (barWidth > 0) {
            gui.fill(x + TRACK_X, y, x + TRACK_X + barWidth, y + BAR_H, color);
        }
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 输入区
        drawBar(gui, x, y + IN1_Y, "gui.template_mod.mixer.in1",
                menu.getIn1Amount(), menu.getIn1Max(), 0xFFC070FF);
        drawBar(gui, x, y + IN2_Y, "gui.template_mod.mixer.in2",
                menu.getIn2Amount(), menu.getIn2Max(), 0xFFFF8040);
        // 输出区
        drawBar(gui, x, y + OUT_Y, "gui.template_mod.mixer.out",
                menu.getOutAmount(), menu.getOutMax(), 0xFFFFB040);
        // 能源区
        drawBar(gui, x, y + ENERGY_Y, "gui.template_mod.energy.short",
                menu.getChishiEnergy(), menu.getChishiMax(), 0xFFE03030);
        // 进度区
        gui.drawString(this.font, Component.translatable("gui.template_mod.mixer.progress_label"),
                x + LABEL_X, y + PROGRESS_Y + 1, 0xFF3F3F3F, false);
        GuiWidgets.track(gui, x + TRACK_X, y + PROGRESS_Y, TRACK_W, BAR_H);
        int progressWidth = (int) (TRACK_W * menu.getProgress() / 100.0F);
        if (progressWidth > 0) {
            gui.fill(x + TRACK_X, y + PROGRESS_Y,
                    x + TRACK_X + progressWidth, y + PROGRESS_Y + BAR_H, 0xFFFFD030);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);
        // 输入Ⅰ
        if (isHovering(TRACK_X, IN1_Y, TRACK_W, BAR_H, mouseX, mouseY)) {
            tip(gui, "gui.template_mod.mixer.in1_tip",
                    formatEnergy(menu.getIn1Amount()), formatEnergy(menu.getIn1Max()), mouseX, mouseY);
        } else if (isHovering(TRACK_X, IN2_Y, TRACK_W, BAR_H, mouseX, mouseY)) {
            tip(gui, "gui.template_mod.mixer.in2_tip",
                    formatEnergy(menu.getIn2Amount()), formatEnergy(menu.getIn2Max()), mouseX, mouseY);
        } else if (isHovering(TRACK_X, OUT_Y, TRACK_W, BAR_H, mouseX, mouseY)) {
            tip(gui, "gui.template_mod.mixer.out_tip",
                    formatEnergy(menu.getOutAmount()), formatEnergy(menu.getOutMax()), mouseX, mouseY);
        } else if (isHovering(TRACK_X, ENERGY_Y, TRACK_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.energy",
                            formatEnergy(menu.getChishiEnergy()), formatEnergy(menu.getChishiMax())),
                    mouseX, mouseY);
        } else if (isHovering(TRACK_X, PROGRESS_Y, TRACK_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.mixer.progress", menu.getProgress()),
                    mouseX, mouseY);
        }
    }

    /** 多行悬停提示：功能名 + 液体数值 */
    private void tip(GuiGraphics gui, String nameKey, String amount, String max, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(nameKey, amount, max));
        lines.add(Component.translatable("gui.template_mod.mixer.tip"));
        gui.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }
}
