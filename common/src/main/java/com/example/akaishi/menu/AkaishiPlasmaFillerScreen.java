package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.fluid.ModFluids;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * 离子体填装器界面（vanilla 灰色风格，198 高）：
 * - 3 条等离子体输入量条（混合蓝白/下界橙红/末地紫）
 * - 反应棒槽 + 3 只读燃料棒输出槽（自绘槽框）
 * - 填装进度条（金）
 * 悬停显示功能名 + 数值。
 */
public class AkaishiPlasmaFillerScreen extends AbstractContainerScreen<AkaishiPlasmaFillerMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_wireless_terminal.png");
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TRACK_X = 70, TRACK_W = 86, BAR_H = 8;
    private static final int PLASMA0_Y = 20;
    private static final int PLASMA1_Y = 32;
    private static final int PLASMA2_Y = 44;
    private static final int SLOT_Y = 60;
    private static final int PROGRESS_Y = 78;
    /** 升级槽 GUI 位置（与 Menu 槽位坐标一致，进度条与玩家背包之间空档） */
    private static final int SPEED_SLOT_X = 134, SPEED_SLOT_Y = 96;
    private static final int ENERGY_SLOT_X = 152, ENERGY_SLOT_Y = 96;

    public AkaishiPlasmaFillerScreen(AkaishiPlasmaFillerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
    }

    private static String format(long v) {
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

    private void drawBar(GuiGraphics gui, int x, int y, String labelKey, long value, long max, int color) {
        gui.drawString(this.font, Component.translatable(labelKey), x + 20, y + 1, TEXT, false);
        GuiWidgets.track(gui, x + TRACK_X, y, TRACK_W, BAR_H);
        long clamped = Math.max(0, Math.min(value, max));
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

        GuiWidgets.slotBox(gui, x + 44, y + SLOT_Y);
        GuiWidgets.slotBox(gui, x + 80, y + SLOT_Y);
        GuiWidgets.slotBox(gui, x + 116, y + SLOT_Y);
        GuiWidgets.slotBox(gui, x + 152, y + SLOT_Y);

        drawBar(gui, x, y + PLASMA0_Y, "gui.akaishi.aggregator.mixed",
                menu.getPlasmaAmount(0), menu.getPlasmaCapacity(0), ModFluids.COLOR_MIXED_PLASMA);
        drawBar(gui, x, y + PLASMA1_Y, "gui.akaishi.aggregator.nether",
                menu.getPlasmaAmount(1), menu.getPlasmaCapacity(1), ModFluids.COLOR_NETHER_PLASMA);
        drawBar(gui, x, y + PLASMA2_Y, "gui.akaishi.aggregator.end",
                menu.getPlasmaAmount(2), menu.getPlasmaCapacity(2), ModFluids.COLOR_END_PLASMA);
        drawBar(gui, x, y + PROGRESS_Y, "gui.akaishi.filler.progress",
                menu.getProgress(), ModConfig.fillerProcessTicks, 0xFFFFD030);

        // 升级槽（速度/能量，纹理无图案需自绘框 + 槽位上方标签）
        GuiWidgets.slotBox(gui, x + SPEED_SLOT_X, y + SPEED_SLOT_Y);
        GuiWidgets.slotBox(gui, x + ENERGY_SLOT_X, y + ENERGY_SLOT_Y);
        gui.drawString(this.font, Component.translatable("gui.akaishi.upgrade.tag"),
                x + SPEED_SLOT_X, y + SPEED_SLOT_Y - 9, 0xFF707070, false);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);
        if (isHovering(TRACK_X, PLASMA0_Y, TRACK_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.plasma_fluid",
                    Component.translatable("fluid.akaishi.mixed_plasma"),
                    format(menu.getPlasmaAmount(0)), format(menu.getPlasmaCapacity(0))), mouseX, mouseY);
        } else if (isHovering(TRACK_X, PLASMA1_Y, TRACK_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.plasma_fluid",
                    Component.translatable("fluid.akaishi.nether_plasma"),
                    format(menu.getPlasmaAmount(1)), format(menu.getPlasmaCapacity(1))), mouseX, mouseY);
        } else if (isHovering(TRACK_X, PLASMA2_Y, TRACK_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.plasma_fluid",
                    Component.translatable("fluid.akaishi.end_plasma"),
                    format(menu.getPlasmaAmount(2)), format(menu.getPlasmaCapacity(2))), mouseX, mouseY);
        } else if (isHovering(TRACK_X, PROGRESS_Y, TRACK_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.filler.progress_tip",
                    format(menu.getProgress()), format(ModConfig.fillerProcessTicks)), mouseX, mouseY);
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
