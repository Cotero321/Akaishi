package com.example.template.menu;

import com.example.template.TemplateMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * 赤能源储存串联器界面：横向赤能源条 + 单位缩写数值 + 结构状态提示。
 * 大数值以 K/M/B/T 单位缩写显示，避免超长数字。
 */
public class ChishiEnergyCellSerializerScreen extends AbstractContainerScreen<ChishiEnergyCellSerializerMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_energy_cell.png");

    /** 横向能量条区域（贴图内坐标） */
    private static final int BAR_X = 20;
    private static final int BAR_Y = 24;
    private static final int BAR_W = 136;
    private static final int BAR_H = 8;

    public ChishiEnergyCellSerializerScreen(ChishiEnergyCellSerializerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    /** 大数值单位缩写：>=1T 万亿，>=1B 十亿，>=1M 百万，>=1K 千，否则原样输出 */
    private static String formatEnergy(long v) {
        if (v >= 1_000_000_000_000L) {
            return trim(v / 1.0e12) + "T";
        }
        if (v >= 1_000_000_000L) {
            return trim(v / 1.0e9) + "B";
        }
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

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 纤细横向赤能源条：从左到右填充（long 计算避免溢出）
        long energy = Math.max(0, Math.min(menu.getEnergy(), menu.getMaxEnergy()));
        long max = Math.max(1, menu.getMaxEnergy());
        int barWidth = (int) (BAR_W * energy / max);
        if (barWidth > 0) {
            gui.fill(x + BAR_X, y + BAR_Y, x + BAR_X + barWidth, y + BAR_Y + BAR_H, 0xFFE03030);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // 仅绘制标题，抑制原版"物品栏"标签避免与信息面板重叠
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 能量数值文本（能量条下方居中，单位缩写显示）
        Component text = Component.literal(formatEnergy(menu.getEnergy()) + " / " + formatEnergy(menu.getMaxEnergy()));
        int textWidth = this.font.width(text);
        gui.drawString(this.font, text, this.leftPos + 88 - textWidth / 2, this.topPos + 40, 0xFFE0E0E0, false);

        // 结构状态提示（数值文本下方）
        Component hint = menu.isFormed()
                ? Component.translatable("gui.template_mod.serializer.formed", formatEnergy(menu.getMaxEnergy()))
                : Component.translatable("gui.template_mod.serializer.unformed");
        int hintWidth = this.font.width(hint);
        gui.drawString(this.font, hint, this.leftPos + 88 - hintWidth / 2, this.topPos + 52,
                menu.isFormed() ? 0xFF55FF55 : 0xFFFF5555, false);

        // 鼠标悬停在能量条上时显示名称与数值（单位缩写）
        if (isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.energy", formatEnergy(menu.getEnergy()), formatEnergy(menu.getMaxEnergy())),
                    mouseX, mouseY);
        }
    }
}
