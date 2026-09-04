package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * 生命活化器界面（vanilla 灰色风格）：
 * - 输入区：废料罐（衰竭燃料·灰绿）
 * - 输出区：活化罐（活化衰竭液体·复苏青绿）
 * - 能源区：生命能量（青）
 * - 无害化区：累计活化量（金）
 * 标签文字在轨道左侧，轨道整条填充不压字；悬停显示功能名 + 当前数值。
 */
public class AkaishiLifeActivatorScreen extends AbstractContainerScreen<AkaishiLifeActivatorMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    /** 标签文字左对齐起点（轨道左侧，文字不压在条上） */
    private static final int LABEL_X = 20;
    /** 进度条轨道：位于标签右侧，整条填充 */
    private static final int TRACK_X = 70, TRACK_W = 86, BAR_H = 8;
    /** 四条状态条（间距 10），底缘避开下方说明文字与物品栏标签（y72 起） */
    private static final int IN_Y = 20;
    private static final int OUT_Y = 30;
    private static final int LIFE_Y = 40;
    private static final int PROCESSED_Y = 50;
    /** 管道说明文字行：解释本机无物品槽（纯管道输送），填补状态条与物品栏之间的空档 */
    private static final int HINT_Y = 64;

    public AkaishiLifeActivatorScreen(AkaishiLifeActivatorMenu menu, Inventory inv, Component title) {
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

    /** 绘制带标签的状态条：标签在轨道左侧，轨道整条填充不压字 */
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

        drawBar(gui, x, y + IN_Y, "gui.akaishi.activator.in",
                menu.getInAmount(), menu.getInMax(), 0xFF809070);
        drawBar(gui, x, y + OUT_Y, "gui.akaishi.activator.out",
                menu.getOutAmount(), menu.getOutMax(), 0xFF50E0B0);
        drawBar(gui, x, y + LIFE_Y, "gui.akaishi.life.short",
                menu.getLifeEnergy(), menu.getLifeMax(), 0xFF40E0C0);
        drawBar(gui, x, y + PROCESSED_Y, "gui.akaishi.activator.processed",
                menu.getProcessed(), Math.max(1, menu.getProcessed()), 0xFFFFD030);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        // 显式触发 tooltip（与主流机器 render 模板一致），保证悬停提示能显示
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);
        if (isHovering(TRACK_X, IN_Y, TRACK_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.activator.in_tip",
                    formatEnergy(menu.getInAmount()), formatEnergy(menu.getInMax())), mouseX, mouseY);
        } else if (isHovering(TRACK_X, OUT_Y, TRACK_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.activator.out_tip",
                    formatEnergy(menu.getOutAmount()), formatEnergy(menu.getOutMax())), mouseX, mouseY);
        } else if (isHovering(TRACK_X, LIFE_Y, TRACK_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.life",
                    formatEnergy(menu.getLifeEnergy()), formatEnergy(menu.getLifeMax())), mouseX, mouseY);
        } else if (isHovering(TRACK_X, PROCESSED_Y, TRACK_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.activator.processed_tip",
                    formatEnergy(menu.getProcessed())), mouseX, mouseY);
        }
    }
}
