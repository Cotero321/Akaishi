package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * 生命转换界面（生命聚合转换器 / 生命转换架构共用）：
 * 上方赤能源条（红）+ 生命能量条（绿），数值文字在条上方居中（不遮条），
 * 底部两行结构状态提示（成型：45 倍；单台：独立工作）。
 */
public class AkaishiLifeConverterScreen extends AbstractContainerScreen<AkaishiLifeConverterMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    /** 赤能源条区域 */
    private static final int CHISHI_BAR_X = 20;
    private static final int CHISHI_BAR_Y = 31;
    /** 生命能量条区域（下移，为生命数值文字留出上行空间） */
    private static final int LIFE_BAR_Y = 51;
    private static final int BAR_W = 136;
    private static final int BAR_H = 8;
    /** 数值文字基线（条上方，字形不遮条） */
    private static final int CHISHI_TEXT_Y = 18;
    private static final int LIFE_TEXT_Y = 40;
    /** 结构状态提示两行 */
    private static final int STATUS_Y1 = 66;
    private static final int STATUS_Y2 = 74;
    /** 居中文案最大宽度（防超面板，规则2） */
    private static final int TEXT_MAX_W = 156;

    public AkaishiLifeConverterScreen(AkaishiLifeConverterMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    /** 大数值缩写（复用统一 EnergyFormat） */
    private static String formatEnergy(long v) {
        return EnergyFormat.format(v);
    }

    /** 绘制一条横向能量条 */
    private void drawBar(GuiGraphics gui, int x, int y, long energy, long max, int color) {
        GuiWidgets.bar(gui, x, y, BAR_W, BAR_H, energy, max, color);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        GuiWidgets.track(gui, x + CHISHI_BAR_X, y + CHISHI_BAR_Y, BAR_W, BAR_H);
        drawBar(gui, x + CHISHI_BAR_X, y + CHISHI_BAR_Y, menu.getAkaishiEnergy(), menu.getAkaishiMax(), 0xFFE03030);
        GuiWidgets.track(gui, x + CHISHI_BAR_X, y + LIFE_BAR_Y, BAR_W, BAR_H);
        drawBar(gui, x + CHISHI_BAR_X, y + LIFE_BAR_Y, menu.getLifeEnergy(), menu.getLifeMax(), 0xFF28B428);
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

        // 赤能源数值（条上方居中，不遮条）
        Component akaishiText = Component.translatable("energy.akaishi.akaishi")
                .append(Component.literal(" " + formatEnergy(menu.getAkaishiEnergy()) + " / " + formatEnergy(menu.getAkaishiMax())));
        int w1 = this.font.width(akaishiText);
        gui.drawString(this.font, akaishiText, this.leftPos + 88 - w1 / 2, this.topPos + CHISHI_TEXT_Y, 0xFF3F3F3F, false);

        // 生命能量数值（条上方居中，不遮条）
        Component lifeText = Component.translatable("energy.akaishi.life")
                .append(Component.literal(" " + formatEnergy(menu.getLifeEnergy()) + " / " + formatEnergy(menu.getLifeMax())));
        int w2 = this.font.width(lifeText);
        gui.drawString(this.font, lifeText, this.leftPos + 88 - w2 / 2, this.topPos + LIFE_TEXT_Y, 0xFF3F3F3F, false);

        // 结构状态提示两行（聚合器：单台·独立转换橙；矩阵：成型绿 45 倍 / 未成型橙）
        boolean standalone = menu.isStandalone();
        boolean formed = menu.isFormed();
        Component line1;
        Component line2;
        if (standalone) {
            line1 = Component.translatable("gui.akaishi.life.standalone");
            line2 = Component.translatable("gui.akaishi.life.formed2");
        } else {
            line1 = Component.translatable(formed
                    ? "gui.akaishi.life.formed" : "gui.akaishi.life.unformed");
            line2 = Component.translatable(formed
                    ? "gui.akaishi.life.formed2" : "gui.akaishi.life.unformed2");
        }
        // 超宽按面板宽度截断并加省略号，防超出面板（规则2）
        int line1Color = standalone || !formed ? 0xFFFFAA00 : 0xFF55FF55;
        String line1Str = this.font.plainSubstrByWidth(line1.getString(), TEXT_MAX_W);
        String line2Str = this.font.plainSubstrByWidth(line2.getString(), TEXT_MAX_W);
        gui.drawString(this.font, line1Str, this.leftPos + 88 - this.font.width(line1Str) / 2, this.topPos + STATUS_Y1,
                line1Color, false);
        gui.drawString(this.font, line2Str, this.leftPos + 88 - this.font.width(line2Str) / 2, this.topPos + STATUS_Y2, 0xFF808080, false);

        // 悬停提示
        if (isHovering(CHISHI_BAR_X, CHISHI_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.energy", formatEnergy(menu.getAkaishiEnergy()), formatEnergy(menu.getAkaishiMax())),
                    mouseX, mouseY);
        }
        if (isHovering(CHISHI_BAR_X, LIFE_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.life", formatEnergy(menu.getLifeEnergy()), formatEnergy(menu.getLifeMax())),
                    mouseX, mouseY);
        }
    }
}
