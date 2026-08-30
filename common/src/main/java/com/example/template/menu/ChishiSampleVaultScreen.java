package com.example.template.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 样本库界面：54 格样本槽（6 行 × 9 列）+ 背包，自绘面板风格。
 */
public class ChishiSampleVaultScreen extends AbstractContainerScreen<ChishiSampleVaultMenu> {

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 220;

    private static final int COLOR_BG = 0xFFC6C6C6;
    private static final int COLOR_LINE = 0xFF373737;
    private static final int COLOR_SLOT = 0xFF8B8B8B;

    public ChishiSampleVaultScreen(ChishiSampleVaultMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.fill(x, y, x + PANEL_W, y + PANEL_H, COLOR_BG);
        gui.fill(x + 6, y + 14, x + PANEL_W - 6, y + PANEL_H - 7, 0xFFB0B0B0);

        // 槽位背景框
        for (var slot : this.menu.slots) {
            drawSlotBox(gui, x + slot.x, y + slot.y);
        }
    }

    private void drawSlotBox(GuiGraphics gui, int x, int y) {
        gui.fill(x, y, x + 18, y + 18, COLOR_SLOT);
        gui.fill(x, y, x + 18, y + 1, COLOR_LINE);
        gui.fill(x, y + 17, x + 18, y + 18, COLOR_LINE);
        gui.fill(x, y, x + 1, y + 18, COLOR_LINE);
        gui.fill(x + 17, y, x + 18, y + 18, COLOR_LINE);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, 4, 0xFF3F3F3F, false);
    }
}
