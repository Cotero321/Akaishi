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
 * 能量液化装置界面：赤能源条（红）+ 输出液体条（青）+ 液化进度条（黄）。
 * 数据全部来自 {@link ChishiEnergyLiquefierMenu} 的 ContainerData。
 */
public class ChishiEnergyLiquefierScreen extends AbstractContainerScreen<ChishiEnergyLiquefierMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_energy_cell.png");

    /** 赤能源条区域（对齐贴图框 y=24..32，三条间距 12） */
    private static final int CHISHI_BAR_X = 20, CHISHI_BAR_Y = 24, BAR_W = 136, BAR_H = 8;
    /** 输出液体条区域 */
    private static final int FLUID_BAR_Y = 36;
    /** 液化进度条区域 */
    private static final int PROGRESS_Y = 48;
    /** 机器槽位数量（输入槽 + 固态物槽，贴图无槽位图形需自绘框） */
    private static final int MACHINE_SLOTS = 2;
    /** 液体条通用颜色（产物类型随输入物品而异，统一青色） */
    private static final int FLUID_COLOR = 0xFF40C8FF;

    public ChishiEnergyLiquefierScreen(ChishiEnergyLiquefierMenu menu, Inventory inv, Component title) {
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

        // 机器槽位框（贴图无图形，自绘补齐；槽位于条形区下方 y=58）
        for (int i = 0; i < MACHINE_SLOTS; i++) {
            var slot = menu.slots.get(i);
            GuiWidgets.slotBox(gui, x + slot.x, y + slot.y);
        }

        // 赤能源条（红）
        GuiWidgets.track(gui, x + CHISHI_BAR_X, y + CHISHI_BAR_Y, BAR_W, BAR_H);
        GuiWidgets.track(gui, x + CHISHI_BAR_X, y + FLUID_BAR_Y, BAR_W, BAR_H);
        GuiWidgets.track(gui, x + CHISHI_BAR_X, y + PROGRESS_Y, BAR_W, BAR_H);
        drawBar(gui, x + CHISHI_BAR_X, y + CHISHI_BAR_Y, menu.getChishiEnergy(), menu.getChishiMax(), 0xFFE03030);
        // 输出液体条（青，产物类型随输入物品而异）
        drawBar(gui, x + CHISHI_BAR_X, y + FLUID_BAR_Y, menu.getFluidAmount(), menu.getFluidMax(), FLUID_COLOR);
        // 液化进度条（黄）
        int progressWidth = (int) (BAR_W * menu.getProgress() / 100.0F);
        if (progressWidth > 0) {
            gui.fill(x + CHISHI_BAR_X, y + PROGRESS_Y, x + CHISHI_BAR_X + progressWidth, y + PROGRESS_Y + BAR_H, 0xFFFFD030);
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

        // 数值经悬停提示展示（能量/液体条 tooltip），不再绘制常驻文本避免与条形区重叠
        // 悬停提示
        if (isHovering(CHISHI_BAR_X, CHISHI_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.energy",
                            formatEnergy(menu.getChishiEnergy()), formatEnergy(menu.getChishiMax())),
                    mouseX, mouseY);
        }
        if (isHovering(CHISHI_BAR_X, FLUID_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.fluid",
                            menu.getFluidAmount(), menu.getFluidMax()),
                    mouseX, mouseY);
        }
    }
}
