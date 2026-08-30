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

    /** 赤能源条区域（对齐贴图框 y=24..32，条宽收窄避开输出槽） */
    private static final int CHISHI_BAR_X = 20, CHISHI_BAR_Y = 24, BAR_W = 88, BAR_H = 8;
    /** 生命能量条区域 */
    private static final int LIFE_BAR_Y = 36;
    /** 固化进度条区域（位于两条下方空档） */
    private static final int PROGRESS_X = 20, PROGRESS_Y = 48, PROGRESS_W = 88, PROGRESS_H = 8;
    /** 机器槽位数量（输出槽，贴图无槽位图形需自绘框） */
    private static final int MACHINE_SLOTS = 1;

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

        // 机器槽位框（贴图无图形，自绘补齐；输出槽位于条形区右侧）
        for (int i = 0; i < MACHINE_SLOTS; i++) {
            var slot = menu.slots.get(i);
            GuiWidgets.slotBox(gui, x + slot.x, y + slot.y);
        }

        // 赤能源条（红）
        GuiWidgets.track(gui, x + CHISHI_BAR_X, y + CHISHI_BAR_Y, BAR_W, BAR_H);
        GuiWidgets.track(gui, x + CHISHI_BAR_X, y + LIFE_BAR_Y, BAR_W, BAR_H);
        GuiWidgets.track(gui, x + PROGRESS_X, y + PROGRESS_Y, PROGRESS_W, PROGRESS_H);
        drawBar(gui, x + CHISHI_BAR_X, y + CHISHI_BAR_Y, menu.getChishiEnergy(), menu.getChishiMax(), 0xFFE03030);
        // 生命能量条（绿）
        drawBar(gui, x + CHISHI_BAR_X, y + LIFE_BAR_Y, menu.getLifeEnergy(), menu.getLifeMax(), 0xFF28B428);
        // 固化进度条（黄）
        int progressWidth = (int) (PROGRESS_W * menu.getProgress() / 100.0F);
        if (progressWidth > 0) {
            gui.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + progressWidth, y + PROGRESS_Y + PROGRESS_H, 0xFFFFD030);
        }
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

        // 赤能源数值（生命能量数值经悬停提示展示，避免与条形区重叠）
        Component chishiText = Component.translatable("energy.template_mod.chishi")
                .append(Component.literal(" " + formatEnergy(menu.getChishiEnergy()) + " / " + formatEnergy(menu.getChishiMax())));
        int w1 = this.font.width(chishiText);
        gui.drawString(this.font, chishiText, this.leftPos + 88 - w1 / 2, this.topPos + CHISHI_BAR_Y - 10, 0xFF3F3F3F, false);

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
