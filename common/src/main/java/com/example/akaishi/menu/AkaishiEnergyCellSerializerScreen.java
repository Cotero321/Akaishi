package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
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
public class AkaishiEnergyCellSerializerScreen extends AbstractContainerScreen<AkaishiEnergyCellSerializerMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    /** 横向能量条区域（贴图内坐标） */
    private static final int BAR_X = 20;
    private static final int BAR_Y = 24;
    private static final int BAR_W = 136;
    private static final int BAR_H = 8;
    /** 居中状态文本最大宽度（防超出 176 面板，规则2） */
    private static final int TEXT_MAX_W = 156;

    public AkaishiEnergyCellSerializerScreen(AkaishiEnergyCellSerializerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    /** 大数值单位缩写（复用统一 EnergyFormat：>=1T/1B/1M/1K） */
    private static String formatEnergy(long v) {
        return EnergyFormat.format(v);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 纤细横向赤能源条：从左到右填充（long 计算避免溢出）
        GuiWidgets.track(gui, x + BAR_X, y + BAR_Y, BAR_W, BAR_H);
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
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF3F3F3F, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 能量数值文本（能量条下方居中，单位缩写显示）
        Component text = Component.literal(formatEnergy(menu.getEnergy()) + " / " + formatEnergy(menu.getMaxEnergy()));
        int textWidth = this.font.width(text);
        gui.drawString(this.font, text, this.leftPos + 88 - textWidth / 2, this.topPos + 40, 0xFF3F3F3F, false);

        // 结构状态提示（数值文本下方；超宽按面板宽度截断加省略号，规则2）
        Component hint = menu.isFormed()
                ? Component.translatable("gui.akaishi.serializer.formed", formatEnergy(menu.getMaxEnergy()))
                : Component.translatable("gui.akaishi.serializer.unformed");
        String hintStr = this.font.plainSubstrByWidth(hint.getString(), TEXT_MAX_W);
        gui.drawString(this.font, hintStr, this.leftPos + 88 - this.font.width(hintStr) / 2, this.topPos + 52,
                menu.isFormed() ? 0xFF55FF55 : 0xFFFF5555, false);

        // 鼠标悬停在能量条上时显示名称与数值（单位缩写）
        if (isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.energy", formatEnergy(menu.getEnergy()), formatEnergy(menu.getMaxEnergy())),
                    mouseX, mouseY);
        }
    }
}
