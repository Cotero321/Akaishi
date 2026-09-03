package com.example.akaishi.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * 单输入单输出处理机器界面抽象基类（vanilla 灰自绘，176×198）：
 * - 赤能源条（红）+ 加工进度条（金），轨道与槽框均自绘（GuiWidgets）
 * - 布局与 {@link AkaishiSingleSlotMachineMenu} 槽位坐标一致
 * 标签/提示文案复用公共翻译 key，机器差异仅体现在标题。
 */
public abstract class AkaishiSingleSlotMachineScreen<T extends AkaishiSingleSlotMachineMenu>
        extends AbstractContainerScreen<T> {

    private static final int TEXT = 0xFF3F3F3F;

    // 布局坐标（与 Menu 槽位一致）
    protected static final int ENERGY_X = 20, ENERGY_Y = 22, ENERGY_W = 136, BAR_H = 8;
    protected static final int PROGRESS_X = 26, PROGRESS_Y = 60, PROGRESS_W = 90;
    protected static final int INPUT_X = 26, OUTPUT_X = 98, SLOT_Y = 40;
    protected static final int SPEED_SLOT_X = 134, ENERGY_SLOT_X = 152;

    protected AkaishiSingleSlotMachineScreen(T menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        // 不透明背景面板（vanilla 灰，含四周内凹边框）
        GuiWidgets.panel(gui, x, y, this.imageWidth, this.imageHeight);
        // 机器槽框（输入/输出/升级，纹理无图案需自绘）
        GuiWidgets.slotBox(gui, x + INPUT_X, y + SLOT_Y);
        GuiWidgets.slotBox(gui, x + OUTPUT_X, y + SLOT_Y);
        GuiWidgets.slotBox(gui, x + SPEED_SLOT_X, y + SLOT_Y);
        GuiWidgets.slotBox(gui, x + ENERGY_SLOT_X, y + SLOT_Y);
        // 玩家背包 + 快捷栏槽框（解决物品栏背景看不到槽位的问题）
        GuiWidgets.playerInventory(gui, x, y);
        // 赤能源条（红）
        drawBar(gui, x + ENERGY_X, y + ENERGY_Y, ENERGY_W, BAR_H,
                (float) menu.getEnergy() / Math.max(1, menu.getEnergyCapacity()), 0xFFE03030);
        // 加工进度条（金）
        drawBar(gui, x + PROGRESS_X, y + PROGRESS_Y, PROGRESS_W, BAR_H,
                (float) menu.getProgress() / Math.max(1, menu.getRequired()), 0xFFFFD030);
    }

    /** 绘制轨道 + 按比例填充的数值条（填充内缩 1px 保留内凹边） */
    private void drawBar(GuiGraphics gui, int x, int y, int w, int h, float ratio, int color) {
        GuiWidgets.track(gui, x, y, w, h);
        int fillW = (int) (w * Math.min(1.0F, Math.max(0.0F, ratio)));
        if (fillW > 0) {
            gui.fill(x + 1, y + 1, x + 1 + fillW, y + h - 1, color);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // renderLabels 已 translate(leftPos,topPos)，此处为 GUI 相对坐标
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.single_slot.input"), INPUT_X, 30, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.single_slot.output"), OUTPUT_X, 30, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.upgrade.tag"),
                SPEED_SLOT_X, SLOT_Y + 20, 0xFF707070, false);
        // 玩家背包标题（背包槽起点 y=124，标签置于其上方 8px）
        gui.drawString(this.font, Component.translatable("container.inventory"), 8, 116, TEXT, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);
        if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.energy",
                    formatEnergy(menu.getEnergy()), formatEnergy(menu.getEnergyCapacity())), mouseX, mouseY);
        } else if (isHovering(PROGRESS_X, PROGRESS_Y, PROGRESS_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.single_slot.progress",
                    menu.getProgress(), menu.getRequired()), mouseX, mouseY);
        }
        // 升级槽悬停提示（倍率与组件数）
        if (isHovering(SPEED_SLOT_X, SLOT_Y, 16, 16, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.upgrade.speed_slot", menu.getSpeedUpgradeCount(),
                            "x" + (1F + 0.125F * menu.getSpeedUpgradeCount())),
                    mouseX, mouseY);
        }
        if (isHovering(ENERGY_SLOT_X, SLOT_Y, 16, 16, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.upgrade.energy_slot", menu.getEnergyUpgradeCount(),
                            "x" + (1F + 0.5F * menu.getEnergyUpgradeCount())),
                    mouseX, mouseY);
        }
    }

    /** 大数值缩写（沿用活化器样式） */
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
        return String.format(Locale.ROOT, "%.1f", d);
    }
}
