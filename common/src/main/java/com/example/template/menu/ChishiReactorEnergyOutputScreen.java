package com.example.template.menu;

import com.example.template.TemplateMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 能量输出口界面：赤能源缓冲条 + 数值信息 + 纯发电提示，无机器槽位。
 * 数值以 K/M/B/T 单位缩写显示，数据来自 {@link ChishiReactorEnergyOutputMenu} 的 ContainerData。
 */
public class ChishiReactorEnergyOutputScreen extends AbstractContainerScreen<ChishiReactorEnergyOutputMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_energy_cell.png");

    /** 横向能量条区域（贴图内坐标）：x=20..156，y=24..32，高 8 */
    private static final int BAR_X = 20;
    private static final int BAR_Y = 24;
    private static final int BAR_W = 136;
    private static final int BAR_H = 8;

    public ChishiReactorEnergyOutputScreen(ChishiReactorEnergyOutputMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
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
        Component text = Component.literal(EnergyFormat.format(menu.getEnergy()) + " / " + EnergyFormat.format(menu.getMaxEnergy()));
        int textWidth = this.font.width(text);
        gui.drawString(this.font, text, this.leftPos + 88 - textWidth / 2, this.topPos + 40, 0xFF3F3F3F, false);

        // 纯发电提示（缓冲槽说明）
        Component hint = Component.translatable("gui.template_mod.reactor.energy_output_hint");
        gui.drawString(this.font, hint, this.leftPos + 8, this.topPos + 56, 0xFF707070, false);

        // 鼠标悬停在能量条上时显示名称与数值
        if (isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.energy",
                            EnergyFormat.format(menu.getEnergy()), EnergyFormat.format(menu.getMaxEnergy())),
                    mouseX, mouseY);
        }
    }
}
