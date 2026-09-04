package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * 物品重构仪界面（vanilla 灰色风格，198 高）：
 * - 赤能源条（红）
 * - 三个机器槽：原料（26）/ 衰竭结晶代价（62）/ 产物（98），槽位自带 vanilla 槽框
 * - 重构进度条（金，满 = 配方所需结晶数）
 * 标签文字在轨道左侧不压字；悬停显示数值详情。
 */
public class AkaishiItemReconstructorScreen extends AbstractContainerScreen<AkaishiItemReconstructorMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_wireless_terminal.png");
    private static final int TEXT = 0xFF3F3F3F;
    private static final int ENERGY_Y = 22;
    private static final int SLOT_Y = 40;
    private static final int LABEL_Y = 30;
    private static final int PROGRESS_X = 26, PROGRESS_Y = 60, PROGRESS_W = 90, BAR_H = 8;
    /** 升级槽 GUI 位置（与 Menu 槽位坐标一致，机器槽行右侧） */
    private static final int SPEED_SLOT_X = 134, SPEED_SLOT_Y = 40;
    private static final int ENERGY_SLOT_X = 152, ENERGY_SLOT_Y = 40;

    public AkaishiItemReconstructorScreen(AkaishiItemReconstructorMenu menu, Inventory inv, Component title) {
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

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 机器槽（原料 26 / 衰竭结晶 62 / 产物 98）自绘框：贴图为通用终端贴图，无机器槽图案
        GuiWidgets.slotBox(gui, x + 26, y + SLOT_Y);
        GuiWidgets.slotBox(gui, x + 62, y + SLOT_Y);
        GuiWidgets.slotBox(gui, x + 98, y + SLOT_Y);
        // 升级槽（速度/能量，机器槽行右侧，纹理无图案需自绘框；标签置于槽位下方避让产物槽）
        GuiWidgets.slotBox(gui, x + SPEED_SLOT_X, y + SPEED_SLOT_Y);
        GuiWidgets.slotBox(gui, x + ENERGY_SLOT_X, y + ENERGY_SLOT_Y);
        gui.drawString(this.font, Component.translatable("gui.akaishi.upgrade.tag"),
                x + SPEED_SLOT_X, y + SPEED_SLOT_Y + 20, 0xFF707070, false);

        // 能量条
        GuiWidgets.track(gui, x + 20, y + ENERGY_Y, 136, BAR_H);
        long max = Math.max(1, menu.getEnergyCapacity());
        int width = (int) (136L * Math.max(0, Math.min(menu.getEnergy(), max)) / max);
        if (width > 0) {
            gui.fill(x + 20, y + ENERGY_Y, x + 20 + width, y + ENERGY_Y + BAR_H, 0xFFE03030);
        }
        // 重构进度条（满 = 所需结晶数；无配方时归零）
        GuiWidgets.track(gui, x + PROGRESS_X, y + PROGRESS_Y, PROGRESS_W, BAR_H);
        long required = Math.max(1, menu.getRequired());
        int pWidth = (int) (PROGRESS_W * Math.min(menu.getProgress(), required) / required);
        if (pWidth > 0) {
            gui.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + pWidth, y + PROGRESS_Y + BAR_H, 0xFFFFD030);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // renderLabels 已 translate(leftPos,topPos)，此处为 GUI 相对坐标
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.reconstructor.input"), 26, LABEL_Y, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.reconstructor.crystal"), 62, LABEL_Y, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.reconstructor.output"), 98, LABEL_Y, TEXT, false);
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
        if (isHovering(20, ENERGY_Y, 136, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.energy",
                    formatEnergy(menu.getEnergy()), formatEnergy(menu.getEnergyCapacity())), mouseX, mouseY);
        } else if (isHovering(PROGRESS_X, PROGRESS_Y, PROGRESS_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.reconstructor.progress_tip",
                    formatEnergy(menu.getProgress()), formatEnergy(menu.getRequired()),
                    formatEnergy(menu.getCrystals())), mouseX, mouseY);
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
