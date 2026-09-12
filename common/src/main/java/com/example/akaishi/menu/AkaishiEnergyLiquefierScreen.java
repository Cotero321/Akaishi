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
 * 能量液化装置界面：赤能源条（红）+ 输出液体条（青）+ 液化进度条（黄）。
 * 数据全部来自 {@link AkaishiEnergyLiquefierMenu} 的 ContainerData。
 */
public class AkaishiEnergyLiquefierScreen extends AbstractContainerScreen<AkaishiEnergyLiquefierMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    /** 赤能源条区域（对齐贴图框，三条间距 12；整体下移 2px 避开顶部并排升级槽） */
    private static final int CHISHI_BAR_X = 20, CHISHI_BAR_Y = 26, BAR_W = 136, BAR_H = 8;
    /** 输出液体条区域 */
    private static final int FLUID_BAR_Y = 38;
    /** 液化进度条区域 */
    private static final int PROGRESS_Y = 50;
    /** 机器槽位数量（升级槽 2 + 输入/固态物槽 2，贴图无槽位图形需自绘框） */
    private static final int MACHINE_SLOTS = 4;
    /** 升级槽 GUI 位置（与 Menu 槽位坐标一致，固定面板右上角并排 y=8 起，规则3） */
    private static final int SPEED_SLOT_X = 134, SPEED_SLOT_Y = 8;
    private static final int ENERGY_SLOT_X = 152, ENERGY_SLOT_Y = 8;
    /** 输入/固态物槽 GUI 位置（与 Menu 槽位坐标一致） */
    private static final int INPUT_SLOT_X = 116, INPUT_SLOT_Y = 58;
    private static final int SOLID_SLOT_X = 62, SOLID_SLOT_Y = 58;
    /** 液体条通用颜色（产物类型随输入物品而异，统一青色） */
    private static final int FLUID_COLOR = 0xFF40C8FF;

    public AkaishiEnergyLiquefierScreen(AkaishiEnergyLiquefierMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    /** 大数值缩写（复用统一 EnergyFormat） */
    private static String formatEnergy(long v) {
        return EnergyFormat.format(v);
    }

    /** 条内左侧固定标签区宽度（px），标签置于轨道左侧，填充从轨道右端开始避免覆盖文字 */
    private static final int LABEL_W = 46;

    /** 带标签状态条：标签在轨道左侧，轨道整条填充不压字 */
    private void drawBar(GuiGraphics gui, int x, int y, String labelKey, long value, long max, int color) {
        gui.drawString(this.font, Component.translatable(labelKey), x + 2, y + 1, 0xFF3F3F3F, false);
        int trackX = x + LABEL_W;
        int trackW = BAR_W - LABEL_W;
        GuiWidgets.track(gui, trackX, y, trackW, BAR_H);
        GuiWidgets.bar(gui, trackX, y, trackW, BAR_H, value, max, color);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 机器槽位框（贴图无图形，自绘补齐；0/1=升级槽 2/3=输入/固态物槽）
        for (int i = 0; i < MACHINE_SLOTS; i++) {
            var slot = menu.slots.get(i);
            GuiWidgets.slotBox(gui, x + slot.x, y + slot.y);
        }
        // 升级槽标签（输入槽行左侧小字提示）
        gui.drawString(this.font, Component.translatable("gui.akaishi.upgrade.tag"),
                x + SPEED_SLOT_X - 36, y + SPEED_SLOT_Y + 4, 0xFF707070, false);

        // 赤能源条（红）
        drawBar(gui, x + CHISHI_BAR_X, y + CHISHI_BAR_Y, "gui.akaishi.energy.short",
                menu.getAkaishiEnergy(), menu.getAkaishiMax(), 0xFFE03030);
        // 输出液体条（青，产物类型随输入物品而异）
        drawBar(gui, x + CHISHI_BAR_X, y + FLUID_BAR_Y, "gui.akaishi.liquefier.fluid",
                menu.getFluidAmount(), menu.getFluidMax(), FLUID_COLOR);
        // 液化进度条（黄）
        drawBar(gui, x + CHISHI_BAR_X, y + PROGRESS_Y, "gui.akaishi.liquefier.progress",
                menu.getProgress(), 100L, 0xFFFFD030);
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

        // 数值经悬停提示展示（能量/液体条 tooltip），不再绘制常驻文本避免与条形区重叠
        // 悬停提示
        if (isHovering(CHISHI_BAR_X, CHISHI_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.energy",
                            formatEnergy(menu.getAkaishiEnergy()), formatEnergy(menu.getAkaishiMax())),
                    mouseX, mouseY);
        }
        if (isHovering(CHISHI_BAR_X, FLUID_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.fluid",
                            menu.getFluidAmount(), menu.getFluidMax()),
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
        // 材料输入槽/生命固态物槽悬停：仅空槽时提示用途（有物品时 vanilla 已显示物品名）
        if (isHovering(INPUT_SLOT_X, INPUT_SLOT_Y, 16, 16, mouseX, mouseY)
                && menu.slots.get(AkaishiEnergyLiquefierMenu.MACHINE_SLOT_END - 2).getItem().isEmpty()) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.energy_liquefier.input_tip"), mouseX, mouseY);
        }
        if (isHovering(SOLID_SLOT_X, SOLID_SLOT_Y, 16, 16, mouseX, mouseY)
                && menu.slots.get(AkaishiEnergyLiquefierMenu.MACHINE_SLOT_END - 1).getItem().isEmpty()) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.energy_liquefier.solid_tip"), mouseX, mouseY);
        }
    }
}
