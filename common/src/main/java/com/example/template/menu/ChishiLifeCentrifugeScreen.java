package com.example.template.menu;

import com.example.template.TemplateMod;
import com.example.template.block.entity.ChishiLifeCentrifugeBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * 生命离心机界面（vanilla 灰色风格，198 高）：
 * - 活化燃料输入条（复苏青绿）
 * - 赤能源条（红）
 * - 分离进度条（金，满 100mb 结算一批）
 * - 产物槽位（0=活化结晶主产物，1=衰竭结晶副产物），槽位自带 vanilla 槽框
 * 标签文字在轨道左侧，轨道整条填充不压字；悬停显示功能名 + 当前数值。
 */
public class ChishiLifeCentrifugeScreen extends AbstractContainerScreen<ChishiLifeCentrifugeMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_wireless_terminal.png");
    private static final int TEXT = 0xFF3F3F3F;
    private static final int LABEL_X = 20;
    private static final int TRACK_X = 70, TRACK_W = 86, BAR_H = 8;
    private static final int IN_Y = 22;
    private static final int ENERGY_Y = 32;
    private static final int PROGRESS_Y = 42;
    /** 产物标签行（槽位 y=56 上方） */
    private static final int LABEL_Y = 50;

    public ChishiLifeCentrifugeScreen(ChishiLifeCentrifugeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
    }

    /** 大数值缩写（沿用活化器样式） */
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

    /** 带标签状态条：标签在轨道左侧，轨道整条填充不压字 */
    private void drawBar(GuiGraphics gui, int x, int y, String labelKey, long energy, long max, int color) {
        gui.drawString(this.font, Component.translatable(labelKey), x + LABEL_X, y + 1, TEXT, false);
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

        drawBar(gui, x, y + IN_Y, "gui.template_mod.centrifuge.in",
                menu.getInAmount(), menu.getInMax(), 0xFF50E0B0);
        drawBar(gui, x, y + ENERGY_Y, "gui.template_mod.energy.short",
                menu.getEnergy(), menu.getEnergyCapacity(), 0xFFE03030);
        drawBar(gui, x, y + PROGRESS_Y, "gui.template_mod.centrifuge.progress",
                menu.getProgress(), ChishiLifeCentrifugeBlockEntity.BATCH_MB, 0xFFFFD030);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // renderLabels 已 translate(leftPos,topPos)，此处为 GUI 相对坐标
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        // 产物标签（槽位 62/98, y=56 上方）
        gui.drawString(this.font, Component.translatable("gui.template_mod.centrifuge.main"),
                62, LABEL_Y, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.template_mod.centrifuge.byproduct"),
                98, LABEL_Y, TEXT, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);
        if (isHovering(TRACK_X, IN_Y, TRACK_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.template_mod.centrifuge.in_tip",
                    formatEnergy(menu.getInAmount()), formatEnergy(menu.getInMax())), mouseX, mouseY);
        } else if (isHovering(TRACK_X, ENERGY_Y, TRACK_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.template_mod.energy",
                    formatEnergy(menu.getEnergy()), formatEnergy(menu.getEnergyCapacity())), mouseX, mouseY);
        } else if (isHovering(TRACK_X, PROGRESS_Y, TRACK_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.template_mod.centrifuge.progress_tip",
                    formatEnergy(menu.getProgress()), formatEnergy(ChishiLifeCentrifugeBlockEntity.BATCH_MB)), mouseX, mouseY);
        }
    }
}
