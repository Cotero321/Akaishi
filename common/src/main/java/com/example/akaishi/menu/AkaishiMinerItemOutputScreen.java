package com.example.akaishi.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 矿机物品输出口界面（vanilla 灰色风格，198 高）：
 * - 产物缓冲 9×3（只读，物品管道/漏斗抽取）
 * - 与矿机控制器的连接状态（无能量条）
 */
public class AkaishiMinerItemOutputScreen extends AbstractContainerScreen<AkaishiMinerItemOutputMenu> {

    private static final int TEXT = 0xFF3F3F3F;
    private static final int GREEN = 0xFF2E7D32;
    private static final int RED = 0xFFC62828;

    public AkaishiMinerItemOutputScreen(AkaishiMinerItemOutputMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        GuiWidgets.panel(gui, x, y, this.imageWidth, this.imageHeight);
        // 产物缓冲 9×3
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                GuiWidgets.slotBox(gui, x + 8 + col * 18, y + 40 + row * 18);
            }
        }
        // 玩家背包 + 快捷栏槽框
        GuiWidgets.playerInventory(gui, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        if (menu.isLinked()) {
            gui.drawString(this.font, Component.translatable("gui.akaishi.miner.linked"), 8, 104, GREEN, false);
        } else {
            gui.drawString(this.font, Component.translatable("gui.akaishi.miner.not_linked"), 8, 104, RED, false);
        }
        // 玩家背包标题
        gui.drawString(this.font, Component.translatable("container.inventory"), 8, 116, TEXT, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }
}
