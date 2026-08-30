package com.example.template.menu;

import com.example.template.TemplateMod;
import com.example.template.fluid.ModFluids;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * 能量加工器界面：赤能源条（红）+ 至纯/复合能量输入条（青/紫）+ 至纯/复合燃料输出条（黄/橙）+ 加工进度条（黄）。
 * 数据全部来自 {@link ChishiEnergyProcessorMenu} 的 ContainerData。
 */
public class ChishiEnergyProcessorScreen extends AbstractContainerScreen<ChishiEnergyProcessorMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_energy_cell.png");

    private static final int CHISHI_BAR_X = 20, BAR_W = 88, BAR_H = 8;
    private static final int CHISHI_BAR_Y = 24;
    private static final int PURE_IN_BAR_Y = 34;
    private static final int COMPOUND_IN_BAR_Y = 44;
    private static final int PURE_OUT_BAR_Y = 54;
    private static final int COMPOUND_OUT_BAR_Y = 64;
    private static final int PROGRESS_Y = 74;
    /** 机器槽位数量（输入槽，贴图无槽位图形需自绘框） */
    private static final int MACHINE_SLOTS = 1;

    public ChishiEnergyProcessorScreen(ChishiEnergyProcessorMenu menu, Inventory inv, Component title) {
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

    /** 条内左侧固定标签区宽度（px），填充从标签区右端开始避免覆盖文字 */
    private static final int LABEL_W = 40;

    private void drawBar(GuiGraphics gui, int x, int y, String labelKey, long energy, long max, int color) {
        GuiWidgets.track(gui, x, y, BAR_W, BAR_H);
        gui.drawString(this.font, Component.translatable(labelKey), x + 2, y + 1, 0xFF3F3F3F, false);
        long clamped = Math.max(0, Math.min(energy, max));
        long cap = Math.max(1, max);
        int barWidth = (int) ((BAR_W - LABEL_W) * clamped / cap);
        if (barWidth > 0) {
            gui.fill(x + LABEL_W, y, x + LABEL_W + barWidth, y + BAR_H, color);
        }
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 机器槽位框（贴图无图形，自绘补齐；输入槽位于条形区右侧）
        for (int i = 0; i < MACHINE_SLOTS; i++) {
            var slot = menu.slots.get(i);
            GuiWidgets.slotBox(gui, x + slot.x, y + slot.y);
        }

        // 赤能源条（红）
        drawBar(gui, x + CHISHI_BAR_X, y + CHISHI_BAR_Y, "gui.template_mod.proc.chishi",
                menu.getChishiEnergy(), menu.getChishiMax(), 0xFFE03030);
        // 至纯能量输入条（青）
        drawBar(gui, x + CHISHI_BAR_X, y + PURE_IN_BAR_Y, "gui.template_mod.proc.pure_in",
                menu.getPureInAmount(), menu.getPureInMax(), ModFluids.COLOR_NETHER_PURE_ENERGY);
        // 复合能量输入条（紫）
        drawBar(gui, x + CHISHI_BAR_X, y + COMPOUND_IN_BAR_Y, "gui.template_mod.proc.compound_in",
                menu.getCompoundInAmount(), menu.getCompoundInMax(), ModFluids.COLOR_NETHER_COMPOUND_ENERGY);
        // 至纯燃料输出条（黄）
        drawBar(gui, x + CHISHI_BAR_X, y + PURE_OUT_BAR_Y, "gui.template_mod.proc.pure_out",
                menu.getPureOutAmount(), menu.getPureOutMax(), ModFluids.COLOR_PURE_FUEL);
        // 复合燃料输出条（橙）
        drawBar(gui, x + CHISHI_BAR_X, y + COMPOUND_OUT_BAR_Y, "gui.template_mod.proc.compound_out",
                menu.getCompoundOutAmount(), menu.getCompoundOutMax(), ModFluids.COLOR_NETHER_COMPOUND_FUEL);
        // 加工进度条（亮黄，标签区固定，填充从标签区右端开始）
        GuiWidgets.track(gui, x + CHISHI_BAR_X, y + PROGRESS_Y, BAR_W, BAR_H);
        gui.drawString(this.font, Component.translatable("gui.template_mod.proc.progress"),
                x + CHISHI_BAR_X + 2, y + PROGRESS_Y + 1, 0xFF3F3F3F, false);
        int progressWidth = (int) ((BAR_W - LABEL_W) * menu.getProgress() / 100.0F);
        if (progressWidth > 0) {
            gui.fill(x + CHISHI_BAR_X + LABEL_W, y + PROGRESS_Y,
                    x + CHISHI_BAR_X + LABEL_W + progressWidth, y + PROGRESS_Y + BAR_H, 0xFFFFD030);
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

        // 数值经各条形区悬停提示展示，不再绘制常驻文本避免与条形区重叠
        // 悬停提示
        if (isHovering(CHISHI_BAR_X, CHISHI_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.energy",
                            formatEnergy(menu.getChishiEnergy()), formatEnergy(menu.getChishiMax())),
                    mouseX, mouseY);
        }
        if (isHovering(CHISHI_BAR_X, PURE_IN_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.pure_energy",
                            menu.getPureInAmount(), menu.getPureInMax()),
                    mouseX, mouseY);
        }
        if (isHovering(CHISHI_BAR_X, COMPOUND_IN_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.compound_energy",
                            menu.getCompoundInAmount(), menu.getCompoundInMax()),
                    mouseX, mouseY);
        }
        if (isHovering(CHISHI_BAR_X, PURE_OUT_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.pure_fuel",
                            menu.getPureOutAmount(), menu.getPureOutMax()),
                    mouseX, mouseY);
        }
        if (isHovering(CHISHI_BAR_X, COMPOUND_OUT_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.compound_fuel",
                            menu.getCompoundOutAmount(), menu.getCompoundOutMax()),
                    mouseX, mouseY);
        }
    }
}
