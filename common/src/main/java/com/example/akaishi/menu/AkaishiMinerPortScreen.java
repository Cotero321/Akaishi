package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiMinerPortBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 矿机转口界面（vanilla 灰色风格，198 高）：
 * - 赤能源缓冲条（红，能量管道充能）
 * - 产物缓冲 9×3（只读，物品管道/漏斗抽取）
 * - 与矿机控制器的连接状态
 */
public class AkaishiMinerPortScreen extends AbstractContainerScreen<AkaishiMinerPortMenu> {

    private static final int TEXT = 0xFF3F3F3F;
    private static final int GREEN = 0xFF2E7D32;
    private static final int RED = 0xFFC62828;
    private static final int BAR_H = 8;

    public AkaishiMinerPortScreen(AkaishiMinerPortMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
    }

    private static String formatEnergy(long v) {
        return EnergyFormat.format(v);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        // 不透明背景面板（修复此前透明背景）
        GuiWidgets.panel(gui, x, y, this.imageWidth, this.imageHeight);
        // 产物缓冲 9×3
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                GuiWidgets.slotBox(gui, x + 8 + col * 18, y + 40 + row * 18);
            }
        }
        // 玩家背包 + 快捷栏槽框
        GuiWidgets.playerInventory(gui, x, y);
        // 能量条
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
        // 显式触发 tooltip（与主流机器 render 模板一致），保证悬停提示能显示
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
        // 产物缓冲空槽悬停：仅空槽时提示用途（有物品时 vanilla 已显示物品名，规则 9）
        for (int i = 0; i < AkaishiMinerPortBlockEntity.BUFFER_SLOTS; i++) {
            var slot = menu.slots.get(i);
            if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) && slot.getItem().isEmpty()) {
                gui.renderTooltip(this.font, Component.translatable("gui.akaishi.miner.buffer_tip"), mouseX, mouseY);
                break;
            }
        }
    }
}
