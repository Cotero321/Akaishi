package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.fluid.ModFluids;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * 能量加工器界面：赤能源条（红）+ 至纯/复合能量输入条（青/紫）+ 至纯/复合燃料输出条（黄/橙）+ 加工进度条（黄）。
 * 数据全部来自 {@link AkaishiEnergyProcessorMenu} 的 ContainerData。
 */
public class AkaishiEnergyProcessorScreen extends AbstractContainerScreen<AkaishiEnergyProcessorMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    private static final int CHISHI_BAR_X = 20, BAR_W = 88, BAR_H = 8;
    private static final int CHISHI_BAR_Y = 24;
    private static final int PURE_IN_BAR_Y = 34;
    private static final int COMPOUND_IN_BAR_Y = 44;
    private static final int PURE_OUT_BAR_Y = 54;
    private static final int COMPOUND_OUT_BAR_Y = 64;
    private static final int PROGRESS_Y = 74;
    /** 机器槽位数量（升级槽 2 + 输入槽 1，贴图无槽位图形需自绘框） */
    private static final int MACHINE_SLOTS = 3;
    /** 升级槽 GUI 位置（与 Menu 槽位坐标一致） */
    private static final int SPEED_SLOT_X = 152, SPEED_SLOT_Y = 6;
    private static final int ENERGY_SLOT_X = 152, ENERGY_SLOT_Y = 24;

    public AkaishiEnergyProcessorScreen(AkaishiEnergyProcessorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    /** 大数值缩写：>=1M 百万，>=1K 千，否则原样输出 */
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

    /** 条内左侧固定标签区宽度（px），填充从标签区右端开始避免覆盖文字 */
    private static final int LABEL_W = 40;

    private void drawBar(GuiGraphics gui, int x, int y, String labelKey, long energy, long max, int color) {
        GuiWidgets.track(gui, x, y, BAR_W, BAR_H);
        gui.drawString(this.font, Component.translatable(labelKey), x + 2, y + 1, 0xFF3F3F3F, false);
        long clamped = Math.max(0, Math.min(energy, max));
        long cap = Math.max(1, max);
        int barWidth = (int) ((BAR_W - LABEL_W) * clamped / cap);
        if (barWidth > 0) {
            gui.fill(x + LABEL_W, y, x + LABEL_W + barWidth, y + BAR_H, color);
        }
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 机器槽位框（贴图无图形，自绘补齐；0/1=升级槽 2=输入槽）
        for (int i = 0; i < MACHINE_SLOTS; i++) {
            var slot = menu.slots.get(i);
            GuiWidgets.slotBox(gui, x + slot.x, y + slot.y);
        }
        // 升级槽标签（右上角小字提示，避免与槽框重叠）
        gui.drawString(this.font, Component.translatable("gui.akaishi.upgrade.tag"),
                x + SPEED_SLOT_X - 18, y + SPEED_SLOT_Y + 4, 0xFF707070, false);

        // 赤能源条（红）
        drawBar(gui, x + CHISHI_BAR_X, y + CHISHI_BAR_Y, "gui.akaishi.proc.akaishi",
                menu.getAkaishiEnergy(), menu.getAkaishiMax(), 0xFFE03030);
        // 至纯能量输入条（青）
        drawBar(gui, x + CHISHI_BAR_X, y + PURE_IN_BAR_Y, "gui.akaishi.proc.pure_in",
                menu.getPureInAmount(), menu.getPureInMax(), ModFluids.COLOR_NETHER_PURE_ENERGY);
        // 复合能量输入条（紫）
        drawBar(gui, x + CHISHI_BAR_X, y + COMPOUND_IN_BAR_Y, "gui.akaishi.proc.compound_in",
                menu.getCompoundInAmount(), menu.getCompoundInMax(), ModFluids.COLOR_NETHER_COMPOUND_ENERGY);
        // 至纯燃料输出条（黄）
        drawBar(gui, x + CHISHI_BAR_X, y + PURE_OUT_BAR_Y, "gui.akaishi.proc.pure_out",
                menu.getPureOutAmount(), menu.getPureOutMax(), ModFluids.COLOR_PURE_FUEL);
        // 复合燃料输出条（橙）
        drawBar(gui, x + CHISHI_BAR_X, y + COMPOUND_OUT_BAR_Y, "gui.akaishi.proc.compound_out",
                menu.getCompoundOutAmount(), menu.getCompoundOutMax(), ModFluids.COLOR_NETHER_COMPOUND_FUEL);
        // 加工进度条（亮黄，标签区固定，填充从标签区右端开始）
        GuiWidgets.track(gui, x + CHISHI_BAR_X, y + PROGRESS_Y, BAR_W, BAR_H);
        gui.drawString(this.font, Component.translatable("gui.akaishi.proc.progress"),
                x + CHISHI_BAR_X + 2, y + PROGRESS_Y + 1, 0xFF3F3F3F, false);
        int progressWidth = (int) ((BAR_W - LABEL_W) * menu.getProgress() / 100.0F);
        if (progressWidth > 0) {
            gui.fill(x + CHISHI_BAR_X + LABEL_W, y + PROGRESS_Y,
                    x + CHISHI_BAR_X + LABEL_W + progressWidth, y + PROGRESS_Y + BAR_H, 0xFFFFD030);
        }
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

        // 数值经各条形区悬停提示展示，不再绘制常驻文本避免与条形区重叠
        // 悬停提示
        if (isHovering(CHISHI_BAR_X, CHISHI_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.energy",
                            formatEnergy(menu.getAkaishiEnergy()), formatEnergy(menu.getAkaishiMax())),
                    mouseX, mouseY);
        }
        if (isHovering(CHISHI_BAR_X, PURE_IN_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.pure_energy",
                            menu.getPureInAmount(), menu.getPureInMax()),
                    mouseX, mouseY);
        }
        if (isHovering(CHISHI_BAR_X, COMPOUND_IN_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.compound_energy",
                            menu.getCompoundInAmount(), menu.getCompoundInMax()),
                    mouseX, mouseY);
        }
        if (isHovering(CHISHI_BAR_X, PURE_OUT_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.pure_fuel",
                            menu.getPureOutAmount(), menu.getPureOutMax()),
                    mouseX, mouseY);
        }
        if (isHovering(CHISHI_BAR_X, COMPOUND_OUT_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.compound_fuel",
                            menu.getCompoundOutAmount(), menu.getCompoundOutMax()),
                    mouseX, mouseY);
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
