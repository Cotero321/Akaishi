package com.example.akaishi.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 矿机能量输入口界面（vanilla 灰色风格，166 高）：赤能源缓冲条 + 连接状态 + 玩家背包。
 * 能量值以 K/M 缩写显示，悬停能量条查看精确数值。
 */
public class AkaishiMinerEnergyInputScreen extends AbstractContainerScreen<AkaishiMinerEnergyInputMenu> {

    private static final int TEXT = 0xFF3F3F3F;
    private static final int GREEN = 0xFF2E7D32;
    private static final int RED = 0xFFC62828;
    private static final int BAR_H = 8;

    public AkaishiMinerEnergyInputScreen(AkaishiMinerEnergyInputMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

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

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        GuiWidgets.panel(gui, x, y, this.imageWidth, this.imageHeight);
        // 玩家背包 3×9 + 快捷栏槽框（与 Menu 槽位坐标一致）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                GuiWidgets.slotBox(gui, x + 8 + col * 18, y + 84 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            GuiWidgets.slotBox(gui, x + 8 + col * 18, y + 142);
        }
        // 能量缓冲条（赤能源管道充能）
        GuiWidgets.track(gui, x + 20, y + 22, 136, BAR_H);
        long max = Math.max(1, menu.getCapacity());
        int width = (int) (136L * Math.max(0, Math.min(menu.getEnergy(), max)) / max);
        if (width > 0) {
            gui.fill(x + 20, y + 22, x + 20 + width, y + 22 + BAR_H, 0xFFE03030);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        if (menu.isLinked()) {
            gui.drawString(this.font, Component.translatable("gui.akaishi.miner.linked"), 8, 36, GREEN, false);
        } else {
            gui.drawString(this.font, Component.translatable("gui.akaishi.miner.not_linked"), 8, 36, RED, false);
        }
        // 玩家背包标题
        gui.drawString(this.font, Component.translatable("container.inventory"), 8, 70, TEXT, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);
        int x = this.leftPos;
        int y = this.topPos;
        if (mouseX >= x + 20 && mouseX < x + 156 && mouseY >= y + 22 && mouseY < y + 30) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.energy",
                    formatEnergy(menu.getEnergy()), formatEnergy(menu.getCapacity())), mouseX, mouseY);
        }
    }
}
