package com.example.template.menu;

import com.example.template.TemplateMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * 衰竭保存桶界面：单条液位（灰褐，对应衰竭燃料色）+ 数值文本。
 * 数据来自 {@link ChishiExhaustedBarrelMenu} 的 ContainerData。
 */
public class ChishiExhaustedBarrelScreen extends AbstractContainerScreen<ChishiExhaustedBarrelMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_energy_cell.png");

    private static final int BAR_X = 20, BAR_Y = 30, BAR_W = 136, BAR_H = 8;
    private static final int FLUID_COLOR = 0xFF80705A;

    public ChishiExhaustedBarrelScreen(ChishiExhaustedBarrelMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    private static String trim(double d) {
        if (Math.abs(d - Math.round(d)) < 0.05) {
            return String.valueOf((long) Math.round(d));
        }
        return String.format(Locale.ROOT, "%.1f", d);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        long clamped = Math.max(0, Math.min(menu.getFluidAmount(), menu.getFluidMax()));
        long cap = Math.max(1, menu.getFluidMax());
        int barWidth = (int) (BAR_W * clamped / cap);
        if (barWidth > 0) {
            gui.fill(x + BAR_X, y + BAR_Y, x + BAR_X + barWidth, y + BAR_Y + BAR_H, FLUID_COLOR);
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

        long amount = menu.getFluidAmount();
        long max = menu.getFluidMax();
        String text;
        if (max >= 1_000L) {
            text = trim(amount / 1.0e3) + "K / " + trim(max / 1.0e3) + "K mb";
        } else {
            text = amount + " / " + max + " mb";
        }
        Component label = Component.literal(text);
        int w = this.font.width(label);
        gui.drawString(this.font, label, this.leftPos + 88 - w / 2, this.topPos + BAR_Y - 2, 0xFFE0E0E0, false);

        if (isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.fluid", amount, max),
                    mouseX, mouseY);
        }
    }
}
