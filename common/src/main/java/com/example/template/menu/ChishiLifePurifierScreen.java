package com.example.template.menu;

import com.example.template.TemplateMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * 生命能量提纯器界面：赤能源条（红）+ 生命能量条（绿）+ 固化进度条（黄）+ 输出槽。
 * 数据全部来自 {@link ChishiLifePurifierMenu} 的 ContainerData。
 */
public class ChishiLifePurifierScreen extends AbstractContainerScreen<ChishiLifePurifierMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_energy_cell.png");

    /** 赤能源条区域 */
    private static final int CHISHI_BAR_X = 20, CHISHI_BAR_Y = 16, BAR_W = 136, BAR_H = 8;
    /** 生命能量条区域 */
    private static final int LIFE_BAR_Y = 28;
    /** 固化进度条区域 */
    private static final int PROGRESS_X = 80, PROGRESS_Y = 30, PROGRESS_W = 28, PROGRESS_H = 16;

    public ChishiLifePurifierScreen(ChishiLifePurifierMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    /** 大数值缩写：>=1M 百万，>=1K 千，否则原样输出 */
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
        // 生命能量条（绿）
        drawBar(gui, x + CHISHI_BAR_X, y + LIFE_BAR_Y, menu.getLifeEnergy(), menu.getLifeMax(), 0xFF30E030);
        // 固化进度条（黄）
        int progressWidth = (int) (PROGRESS_W * menu.getProgress() / 100.0F);
        if (progressWidth > 0) {
            gui.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + progressWidth, y + PROGRESS_Y + PROGRESS_H, 0xFFFFD030);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 赤能源数值
        Component chishiText = Component.translatable("energy.template_mod.chishi")
                .append(Component.literal(" " + formatEnergy(menu.getChishiEnergy()) + " / " + formatEnergy(menu.getChishiMax())));
        int w1 = this.font.width(chishiText);
        gui.drawString(this.font, chishiText, this.leftPos + 88 - w1 / 2, this.topPos + 16 - 2, 0xFFE0E0E0, false);

        // 生命能量数值
        Component lifeText = Component.translatable("energy.template_mod.life")
                .append(Component.literal(" " + formatEnergy(menu.getLifeEnergy()) + " / " + formatEnergy(menu.getLifeMax())));
        int w2 = this.font.width(lifeText);
        gui.drawString(this.font, lifeText, this.leftPos + 88 - w2 / 2, this.topPos + 28 - 2, 0xFFE0E0E0, false);

        // 悬停提示
        if (isHovering(CHISHI_BAR_X, CHISHI_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.energy",
                            formatEnergy(menu.getChishiEnergy()), formatEnergy(menu.getChishiMax())),
                    mouseX, mouseY);
        }
        if (isHovering(CHISHI_BAR_X, LIFE_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.life",
                            formatEnergy(menu.getLifeEnergy()), formatEnergy(menu.getLifeMax())),
                    mouseX, mouseY);
        }
    }
}
