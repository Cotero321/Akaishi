package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * 生命能量提纯器界面：赤能源条（红）+ 生命能量条（绿）+ 固化进度条（黄）+ 输出槽。
 * 数据全部来自 {@link AkaishiLifePurifierMenu} 的 ContainerData。
 */
public class AkaishiLifePurifierScreen extends AbstractContainerScreen<AkaishiLifePurifierMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    /** 赤能源条区域（对齐贴图框 y=24..32，条宽收窄避开输出槽） */
    private static final int CHISHI_BAR_X = 20, CHISHI_BAR_Y = 24, BAR_W = 88, BAR_H = 8;
    /** 生命能量条区域 */
    private static final int LIFE_BAR_Y = 36;
    /** 固化进度条区域（位于两条下方空档） */
    private static final int PROGRESS_X = 20, PROGRESS_Y = 48, PROGRESS_W = 88, PROGRESS_H = 8;
    /** 机器槽位数量（输出槽，贴图无槽位图形需自绘框） */
    private static final int MACHINE_SLOTS = 1;
    /** 升级槽 GUI 位置（与 Menu 槽位坐标一致，输出槽右侧避开条形区） */
    private static final int SPEED_SLOT_X = 134, SPEED_SLOT_Y = 30;
    private static final int ENERGY_SLOT_X = 152, ENERGY_SLOT_Y = 30;

    public AkaishiLifePurifierScreen(AkaishiLifePurifierMenu menu, Inventory inv, Component title) {
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

    private void drawBar(GuiGraphics gui, int x, int y, long energy, long max, int color) {
        long clamped = Math.max(0, Math.min(energy, max));
        long cap = Math.max(1, max);
        int barWidth = (int) (BAR_W * clamped / cap);
        if (barWidth > 0) {
            gui.fill(x, y, x + barWidth, y + BAR_H, color);
        }
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 机器槽位框（贴图无图形，自绘补齐；输出槽位于条形区右侧）
        for (int i = 0; i < MACHINE_SLOTS; i++) {
            var slot = menu.slots.get(i);
            GuiWidgets.slotBox(gui, x + slot.x, y + slot.y);
        }

        // 赤能源条（红）
        GuiWidgets.track(gui, x + CHISHI_BAR_X, y + CHISHI_BAR_Y, BAR_W, BAR_H);
        GuiWidgets.track(gui, x + CHISHI_BAR_X, y + LIFE_BAR_Y, BAR_W, BAR_H);
        GuiWidgets.track(gui, x + PROGRESS_X, y + PROGRESS_Y, PROGRESS_W, PROGRESS_H);
        drawBar(gui, x + CHISHI_BAR_X, y + CHISHI_BAR_Y, menu.getAkaishiEnergy(), menu.getAkaishiMax(), 0xFFE03030);
        // 生命能量条（绿）
        drawBar(gui, x + CHISHI_BAR_X, y + LIFE_BAR_Y, menu.getLifeEnergy(), menu.getLifeMax(), 0xFF28B428);
        // 固化进度条（黄）
        int progressWidth = (int) (PROGRESS_W * menu.getProgress() / 100.0F);
        if (progressWidth > 0) {
            gui.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + progressWidth, y + PROGRESS_Y + PROGRESS_H, 0xFFFFD030);
        }

        // 升级槽（速度/能量，纹理无图案需自绘框 + 槽位上方标签）
        GuiWidgets.slotBox(gui, x + SPEED_SLOT_X, y + SPEED_SLOT_Y);
        GuiWidgets.slotBox(gui, x + ENERGY_SLOT_X, y + ENERGY_SLOT_Y);
        gui.drawString(this.font, Component.translatable("gui.akaishi.upgrade.tag"),
                x + SPEED_SLOT_X, y + SPEED_SLOT_Y - 9, 0xFF707070, false);
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

        // 赤能源数值（生命能量数值经悬停提示展示，避免与条形区重叠）
        Component akaishiText = Component.translatable("energy.akaishi.akaishi")
                .append(Component.literal(" " + formatEnergy(menu.getAkaishiEnergy()) + " / " + formatEnergy(menu.getAkaishiMax())));
        int w1 = this.font.width(akaishiText);
        gui.drawString(this.font, akaishiText, this.leftPos + 88 - w1 / 2, this.topPos + CHISHI_BAR_Y - 10, 0xFF3F3F3F, false);

        // 悬停提示
        if (isHovering(CHISHI_BAR_X, CHISHI_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.energy",
                            formatEnergy(menu.getAkaishiEnergy()), formatEnergy(menu.getAkaishiMax())),
                    mouseX, mouseY);
        }
        if (isHovering(CHISHI_BAR_X, LIFE_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.life",
                            formatEnergy(menu.getLifeEnergy()), formatEnergy(menu.getLifeMax())),
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
