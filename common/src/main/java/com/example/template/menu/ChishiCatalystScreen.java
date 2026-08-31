package com.example.template.menu;

import com.example.template.TemplateMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 赤石催化器界面：显示是否工作（能量充足即工作中）+ 能量槽。
 * 无机器槽位；数据来自 {@link ChishiCatalystMenu} 的 ContainerData，随网络同步。
 */
public class ChishiCatalystScreen extends AbstractContainerScreen<ChishiCatalystMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_wireless_terminal.png");
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_DIM = 0xFF707070;
    private static final int TEXT_RED = 0xFFB03030;
    private static final int TEXT_GREEN = 0xFF2E7D32;

    // 能量槽（内容区居中）
    private static final int BAR_X = 20;
    private static final int BAR_Y = 46;
    private static final int BAR_W = 136;
    private static final int BAR_H = 8;

    public ChishiCatalystScreen(ChishiCatalystMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 能量条
        GuiWidgets.track(gui, x + BAR_X, y + BAR_Y, BAR_W, BAR_H);
        int max = menu.getEnergyCapacity();
        int energy = Math.max(0, Math.min(menu.getEnergy(), max));
        int width = (int) (BAR_W * energy / Math.max(1, max));
        if (width > 0) {
            gui.fill(x + BAR_X, y + BAR_Y, x + BAR_X + width, y + BAR_Y + BAR_H, 0xFFE03030);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // renderLabels 已 translate(leftPos,topPos)，此处为 GUI 相对坐标
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        // 工作状态
        boolean working = menu.isWorking();
        gui.drawString(this.font, Component.translatable(working
                        ? "gui.template_mod.catalyst.working" : "gui.template_mod.catalyst.idle"),
                8, 30, working ? TEXT_GREEN : TEXT_RED, false);
        // 能量数值（居中，右对齐防出框）
        Component energyText = Component.translatable("gui.template_mod.energy",
                EnergyFormat.format(menu.getEnergy()), EnergyFormat.format(menu.getEnergyCapacity()));
        int textWidth = this.font.width(energyText);
        gui.drawString(this.font, energyText, 88 - textWidth / 2, BAR_Y + 12, TEXT, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 能量条悬停提示
        if (isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.energy",
                            EnergyFormat.format(menu.getEnergy()), EnergyFormat.format(menu.getEnergyCapacity())),
                    mouseX, mouseY);
        }
    }
}
