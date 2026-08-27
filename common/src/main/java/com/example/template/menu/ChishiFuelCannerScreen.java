package com.example.template.menu;

import com.example.template.TemplateMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 燃料装罐机界面：输入液体条（青）+ 空罐/满罐槽位。
 * 数据来自 {@link ChishiFuelCannerMenu} 的 ContainerData。
 */
public class ChishiFuelCannerScreen extends AbstractContainerScreen<ChishiFuelCannerMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_fuel_canner.png");

    /** 输入液体条区域 */
    private static final int FLUID_BAR_X = 20, FLUID_BAR_Y = 16, BAR_W = 136, BAR_H = 8;
    private static final int FLUID_COLOR = 0xFF40C8FF;

    public ChishiFuelCannerScreen(ChishiFuelCannerMenu menu, Inventory inv, Component title) {
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
        return String.format(java.util.Locale.ROOT, "%.1f", d);
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

        // 输入液体条（青）
        drawBar(gui, x + FLUID_BAR_X, y + FLUID_BAR_Y, menu.getFluidAmount(), menu.getFluidMax(), FLUID_COLOR);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFE0E0E0, false);

        // 液体数值
        Component fluidText = Component.translatable("gui.template_mod.fluid")
                .append(Component.literal(" " + formatEnergy(menu.getFluidAmount()) + " / " + formatEnergy(menu.getFluidMax())));
        int w = this.font.width(fluidText);
        gui.drawString(this.font, fluidText, 88 - w / 2, FLUID_BAR_Y - 2, 0xFFE0E0E0, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);
        if (isHovering(FLUID_BAR_X, FLUID_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.fluid",
                            menu.getFluidAmount(), menu.getFluidMax()),
                    mouseX, mouseY);
        }
    }
}
