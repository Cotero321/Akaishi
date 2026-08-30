package com.example.template.menu;

import com.example.template.TemplateMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * 生命转换界面（生命聚合转换器 / 生命转换架构共用）：
 * 上方赤能源条（红）+ 生命能量条（绿），数值文字在条上方居中（不遮条），
 * 底部两行结构状态提示（成型：45 倍；单台：独立工作）。
 */
public class ChishiLifeConverterScreen extends AbstractContainerScreen<ChishiLifeConverterMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_energy_cell.png");

    /** 赤能源条区域 */
    private static final int CHISHI_BAR_X = 20;
    private static final int CHISHI_BAR_Y = 31;
    /** 生命能量条区域（下移，为生命数值文字留出上行空间） */
    private static final int LIFE_BAR_Y = 51;
    private static final int BAR_W = 136;
    private static final int BAR_H = 8;
    /** 数值文字基线（条上方，字形不遮条） */
    private static final int CHISHI_TEXT_Y = 18;
    private static final int LIFE_TEXT_Y = 40;
    /** 结构状态提示两行 */
    private static final int STATUS_Y1 = 66;
    private static final int STATUS_Y2 = 74;

    public ChishiLifeConverterScreen(ChishiLifeConverterMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    /** 大数值单位缩写：>=1M 百万，>=1K 千，否则原样输出 */
    private static String formatEnergy(long v) {
        if (v >= 1_000_000L) {
            return trim(v / 1.0e6) + "M";
        }
        if (v >= 1_000L) {
            return trim(v / 1.0e3) + "K";
        }
        return String.valueOf(v);
    }

    /** 保留 1 位小数，整数时去掉小数部分（2.0 → 2） */
    private static String trim(double d) {
        if (Math.abs(d - Math.round(d)) < 0.05) {
            return String.valueOf((long) Math.round(d));
        }
        return String.format(Locale.ROOT, "%.1f", d);
    }

    /** 绘制一条横向能量条 */
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

        GuiWidgets.track(gui, x + CHISHI_BAR_X, y + CHISHI_BAR_Y, BAR_W, BAR_H);
        drawBar(gui, x + CHISHI_BAR_X, y + CHISHI_BAR_Y, menu.getChishiEnergy(), menu.getChishiMax(), 0xFFE03030);
        GuiWidgets.track(gui, x + CHISHI_BAR_X, y + LIFE_BAR_Y, BAR_W, BAR_H);
        drawBar(gui, x + CHISHI_BAR_X, y + LIFE_BAR_Y, menu.getLifeEnergy(), menu.getLifeMax(), 0xFF28B428);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF3F3F3F, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 赤能源数值（条上方居中，不遮条）
        Component chishiText = Component.translatable("energy.template_mod.chishi")
                .append(Component.literal(" " + formatEnergy(menu.getChishiEnergy()) + " / " + formatEnergy(menu.getChishiMax())));
        int w1 = this.font.width(chishiText);
        gui.drawString(this.font, chishiText, this.leftPos + 88 - w1 / 2, this.topPos + CHISHI_TEXT_Y, 0xFF3F3F3F, false);

        // 生命能量数值（条上方居中，不遮条）
        Component lifeText = Component.translatable("energy.template_mod.life")
                .append(Component.literal(" " + formatEnergy(menu.getLifeEnergy()) + " / " + formatEnergy(menu.getLifeMax())));
        int w2 = this.font.width(lifeText);
        gui.drawString(this.font, lifeText, this.leftPos + 88 - w2 / 2, this.topPos + LIFE_TEXT_Y, 0xFF3F3F3F, false);

        // 结构状态提示两行（成型绿：45 倍；单台橙：独立工作）
        boolean formed = menu.isFormed();
        Component line1 = Component.translatable(formed
                ? "gui.template_mod.life.formed" : "gui.template_mod.life.unformed");
        Component line2 = Component.translatable(formed
                ? "gui.template_mod.life.formed2" : "gui.template_mod.life.unformed2");
        int w3 = this.font.width(line1);
        int w4 = this.font.width(line2);
        gui.drawString(this.font, line1, this.leftPos + 88 - w3 / 2, this.topPos + STATUS_Y1,
                formed ? 0xFF55FF55 : 0xFFFFAA00, false);
        gui.drawString(this.font, line2, this.leftPos + 88 - w4 / 2, this.topPos + STATUS_Y2, 0xFF808080, false);

        // 悬停提示
        if (isHovering(CHISHI_BAR_X, CHISHI_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.energy", formatEnergy(menu.getChishiEnergy()), formatEnergy(menu.getChishiMax())),
                    mouseX, mouseY);
        }
        if (isHovering(CHISHI_BAR_X, LIFE_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.life", formatEnergy(menu.getLifeEnergy()), formatEnergy(menu.getLifeMax())),
                    mouseX, mouseY);
        }
    }
}
