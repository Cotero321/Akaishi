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
    /** 升级槽 GUI 位置（与 Menu 槽位坐标一致，输出槽右侧，固定面板右上角并排 y=8 起，规则3） */
    private static final int SPEED_SLOT_X = 134, SPEED_SLOT_Y = 8;
    private static final int ENERGY_SLOT_X = 152, ENERGY_SLOT_Y = 8;
    /** 输出槽 GUI 位置（与 Menu 槽位坐标一致） */
    private static final int OUTPUT_SLOT_X = 116, OUTPUT_SLOT_Y = 30;

    public AkaishiLifePurifierScreen(AkaishiLifePurifierMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    /** 大数值缩写（复用统一 EnergyFormat） */
    private static String formatEnergy(long v) {
        return EnergyFormat.format(v);
    }

    private void drawBar(GuiGraphics gui, int x, int y, long energy, long max, int color) {
        GuiWidgets.bar(gui, x, y, BAR_W, BAR_H, energy, max, color);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 机器槽位框（贴图无图形，自绘补齐；输出槽位于条形区右侧）
        GuiWidgets.slotBox(gui, x + OUTPUT_SLOT_X, y + OUTPUT_SLOT_Y);

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
                x + SPEED_SLOT_X - 36, y + SPEED_SLOT_Y + 4, 0xFF707070, false);
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
                            "x" + (1F + menu.getSpeedUpgradeCount())),
                    mouseX, mouseY);
        }
        if (isHovering(ENERGY_SLOT_X, ENERGY_SLOT_Y, 16, 16, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.upgrade.energy_slot", menu.getEnergyUpgradeCount(),
                            "x" + (1F + 0.5F * menu.getEnergyUpgradeCount())),
                    mouseX, mouseY);
        }
        // 输出槽悬停：仅空槽时提示用途（有物品时 vanilla 已显示物品名，避免重复 tooltip）
        if (isHovering(OUTPUT_SLOT_X, OUTPUT_SLOT_Y, 16, 16, mouseX, mouseY)
                && menu.slots.get(AkaishiLifePurifierMenu.MACHINE_SLOT_END - 1).getItem().isEmpty()) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.life_purifier.output_tip"), mouseX, mouseY);
        }
    }
}
