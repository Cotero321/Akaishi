package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 赤石能量聚合器界面：顶部能量条 + 中部输入/进度/输出 + 数值文字标签。
 * 能量与进度数值使用 M/K 缩写显示。
 */
public class AkaishiEnergyAggregatorScreen extends AbstractContainerScreen<AkaishiEnergyAggregatorMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    private static final int BAR_X = 20, BAR_Y = 16, BAR_W = 136, BAR_H = 8;
    private static final int PROGRESS_X = 80, PROGRESS_Y = 34, PROGRESS_W = 28, PROGRESS_H = 16;
    /** 机器槽位数量（输入槽 + 输出槽，贴图无槽位图形需自绘框） */
    private static final int MACHINE_SLOTS = 2;

    public AkaishiEnergyAggregatorScreen(AkaishiEnergyAggregatorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 机器槽位框（贴图无图形，自绘补齐）
        for (int i = 0; i < MACHINE_SLOTS; i++) {
            var slot = menu.slots.get(i);
            GuiWidgets.slotBox(gui, x + slot.x, y + slot.y);
        }

        // 赤能源条（红色）
        GuiWidgets.track(gui, x + BAR_X, y + BAR_Y, BAR_W, BAR_H);
        long max = Math.max(1, menu.getMaxEnergy());
        int energyWidth = (int) (BAR_W * Math.max(0, Math.min(menu.getEnergy(), menu.getMaxEnergy())) / max);
        if (energyWidth > 0) {
            gui.fill(x + BAR_X, y + BAR_Y, x + BAR_X + energyWidth, y + BAR_Y + BAR_H, 0xFFE03030);
        }
        // 聚合进度条（黄色）
        GuiWidgets.track(gui, x + PROGRESS_X, y + PROGRESS_Y, PROGRESS_W, PROGRESS_H);
        int progressWidth = (int) (PROGRESS_W * menu.getProgress() / 100.0F);
        if (progressWidth > 0) {
            gui.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + progressWidth, y + PROGRESS_Y + PROGRESS_H, 0xFFFFD030);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF3F3F3F, false);

        // 能量数值文本
        gui.drawString(this.font,
                Component.translatable("gui.akaishi.energy",
                        EnergyFormat.format(menu.getEnergy()), EnergyFormat.format(menu.getMaxEnergy())),
                20, 50, 0xE0E0E0, false);
        // 可合成次数文本（按当前配方消耗计算，母岩升级/赤石锭聚合通用）
        int cost = Math.max(1, menu.getCurrentCost());
        gui.drawString(this.font,
                Component.translatable("gui.akaishi.craft_times",
                        menu.getEnergy() / cost),
                20, 62, 0xE0E0E0, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 能量条悬停提示（M/K 缩写）
        if (isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.energy",
                            EnergyFormat.format(menu.getEnergy()), EnergyFormat.format(menu.getMaxEnergy())),
                    mouseX, mouseY);
        }
    }
}
