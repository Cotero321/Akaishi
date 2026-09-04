package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * 活化分馏器界面（vanilla 灰色风格，198 高）：
 * - 输入槽（活化结晶，自绘槽框）
 * - 赤能源条（红）+ 分馏进度条（金）
 * - 输出槽（活化成分/衰竭结晶，自绘槽框）
 * 标签文字在槽位上方，进度条整条填充不压字；悬停显示功能名 + 当前数值。
 */
public class AkaishiActivatedFractionatorScreen extends AbstractContainerScreen<AkaishiActivatedFractionatorMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_wireless_terminal.png");
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TRACK_X = 70, TRACK_W = 86, BAR_H = 8;
    private static final int ENERGY_Y = 26;
    private static final int SLOT_Y = 52;
    private static final int PROGRESS_Y = 88;
    private static final int LABEL_Y = 44;
    /** 升级槽 GUI 位置（与 Menu 槽位坐标一致，输入/输出槽行右侧） */
    private static final int SPEED_SLOT_X = 134, SPEED_SLOT_Y = 52;
    private static final int ENERGY_SLOT_X = 152, ENERGY_SLOT_Y = 52;

    public AkaishiActivatedFractionatorScreen(AkaishiActivatedFractionatorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
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

    /** 带标签状态条：标签在轨道左侧，轨道整条填充不压字 */
    private void drawBar(GuiGraphics gui, int x, int y, String labelKey, long energy, long max, int color) {
        gui.drawString(this.font, Component.translatable(labelKey), x + 20, y + 1, TEXT, false);
        GuiWidgets.track(gui, x + TRACK_X, y, TRACK_W, BAR_H);
        long clamped = Math.max(0, Math.min(energy, max));
        long cap = Math.max(1, max);
        int barWidth = (int) (TRACK_W * clamped / cap);
        if (barWidth > 0) {
            gui.fill(x + TRACK_X, y, x + TRACK_X + barWidth, y + BAR_H, color);
        }
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 槽位自绘框（纹理对应区域无预绘槽框）
        GuiWidgets.slotBox(gui, x + 44, y + SLOT_Y);
        GuiWidgets.slotBox(gui, x + 80, y + SLOT_Y);
        GuiWidgets.slotBox(gui, x + 116, y + SLOT_Y);
        // 升级槽（速度/能量，输入槽行右侧）
        GuiWidgets.slotBox(gui, x + SPEED_SLOT_X, y + SPEED_SLOT_Y);
        GuiWidgets.slotBox(gui, x + ENERGY_SLOT_X, y + ENERGY_SLOT_Y);
        // 升级槽标签（槽位左侧小字提示）
        gui.drawString(this.font, Component.translatable("gui.akaishi.upgrade.tag"),
                x + SPEED_SLOT_X - 36, y + SPEED_SLOT_Y + 4, 0xFF707070, false);

        drawBar(gui, x, y + ENERGY_Y, "gui.akaishi.energy.short",
                menu.getEnergy(), menu.getEnergyCapacity(), 0xFFE03030);
        drawBar(gui, x, y + PROGRESS_Y, "gui.akaishi.fractionator.progress",
                menu.getProgress(), ModConfig.fractionatorProcessTicks, 0xFFFFD030);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // renderLabels 已 translate(leftPos,topPos)，此处为 GUI 相对坐标
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        // 输出槽标签（槽位 y=40 上方，x 对齐槽位中心）
        gui.drawString(this.font, Component.translatable("gui.akaishi.fractionator.main"), 80, LABEL_Y, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.fractionator.byproduct"), 116, LABEL_Y, TEXT, false);
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
        if (isHovering(TRACK_X, ENERGY_Y, TRACK_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.energy",
                    formatEnergy(menu.getEnergy()), formatEnergy(menu.getEnergyCapacity())), mouseX, mouseY);
        } else if (isHovering(TRACK_X, PROGRESS_Y, TRACK_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.fractionator.progress_tip",
                    formatEnergy(menu.getProgress()), formatEnergy(ModConfig.fractionatorProcessTicks)), mouseX, mouseY);
        }
        // 升级槽悬停提示
        if (isHovering(SPEED_SLOT_X, SPEED_SLOT_Y, 16, 16, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.upgrade.speed_slot", menu.getSpeedUpgradeCount(),
                            "x" + (1F + 0.125F * menu.getSpeedUpgradeCount())),
                    mouseX, mouseY);
        }
        if (isHovering(ENERGY_SLOT_X, ENERGY_SLOT_Y, 16, 16, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.upgrade.energy_slot", menu.getEnergyUpgradeCount(),
                            "x" + (1F + 0.5F * menu.getEnergyUpgradeCount())),
                    mouseX, mouseY);
        }
    }
}
