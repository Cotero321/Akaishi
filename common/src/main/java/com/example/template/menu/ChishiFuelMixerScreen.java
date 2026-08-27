package com.example.template.menu;

import com.example.template.TemplateMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * 燃料混合器界面：赤能源条（红）+ 输入1条（紫）+ 输入2条（橙）+ 输出条（金）+ 混合进度条（黄）。
 * 数据来自 {@link ChishiFuelMixerMenu} 的 ContainerData。
 */
public class ChishiFuelMixerScreen extends AbstractContainerScreen<ChishiFuelMixerMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_energy_cell.png");

    private static final int CHISHI_BAR_X = 20, BAR_W = 136, BAR_H = 8;
    private static final int CHISHI_BAR_Y = 16;
    private static final int IN1_BAR_Y = 28;
    private static final int IN2_BAR_Y = 40;
    private static final int OUT_BAR_Y = 52;
    private static final int PROGRESS_Y = 64;

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

    private void drawBar(GuiGraphics gui, int x, int y, long energy, long max, int color) {
        long clamped = Math.max(0, Math.min(energy, max));
        long cap = Math.max(1, max);
        int barWidth = (int) (BAR_W * clamped / cap);
        if (barWidth > 0) {
            gui.fill(x, y, x + barWidth, y + BAR_H, color);
        }
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 赤能源条（红）
        drawBar(gui, x + CHISHI_BAR_X, y + CHISHI_BAR_Y, menu.getChishiEnergy(), menu.getChishiMax(), 0xFFE03030);
        // 输入1（末地紫）
        drawBar(gui, x + CHISHI_BAR_X, y + IN1_BAR_Y, menu.getIn1Amount(), menu.getIn1Max(), 0xFFC070FF);
        // 输入2（下界橙）
        drawBar(gui, x + CHISHI_BAR_X, y + IN2_BAR_Y, menu.getIn2Amount(), menu.getIn2Max(), 0xFFFF8040);
        // 输出（金）
        drawBar(gui, x + CHISHI_BAR_X, y + OUT_BAR_Y, menu.getOutAmount(), menu.getOutMax(), 0xFFFFB040);
        // 混合进度（黄）
        int progressWidth = (int) (BAR_W * menu.getProgress() / 100.0F);
        if (progressWidth > 0) {
            gui.fill(x + CHISHI_BAR_X, y + PROGRESS_Y, x + CHISHI_BAR_X + progressWidth, y + PROGRESS_Y + BAR_H, 0xFFFFD030);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);
        if (isHovering(CHISHI_BAR_X, CHISHI_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.energy",
                            formatEnergy(menu.getChishiEnergy()), formatEnergy(menu.getChishiMax())),
                    mouseX, mouseY);
        }
        if (isHovering(CHISHI_BAR_X, IN1_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.fluid", menu.getIn1Amount(), menu.getIn1Max()),
                    mouseX, mouseY);
        }
        if (isHovering(CHISHI_BAR_X, IN2_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.fluid", menu.getIn2Amount(), menu.getIn2Max()),
                    mouseX, mouseY);
        }
        if (isHovering(CHISHI_BAR_X, OUT_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.fluid", menu.getOutAmount(), menu.getOutMax()),
                    mouseX, mouseY);
        }
    }
}
