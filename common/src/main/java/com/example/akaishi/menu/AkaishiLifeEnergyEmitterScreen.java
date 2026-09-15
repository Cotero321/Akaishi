package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiLifeEnergyEmitterBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 生命能量发射器界面：绿色生命能量条 + 容量数值 + 绑定坐标/射程信息。
 * 复用生命储存器的基础贴图与能量条布局，下方追加两行只读信息文本。
 */
public class AkaishiLifeEnergyEmitterScreen extends AbstractContainerScreen<AkaishiLifeEnergyEmitterMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    /** 生命能量条区域（贴图内坐标） */
    private static final int BAR_X = 20;
    private static final int BAR_Y = 24;
    private static final int BAR_W = 136;
    private static final int BAR_H = 8;
    /** 容量数值文本基线（条下方居中） */
    private static final int VALUE_TEXT_Y = 40;
    /** 绑定坐标文本基线 */
    private static final int TARGET_TEXT_Y = 54;
    /** 射程文本基线 */
    private static final int RANGE_TEXT_Y = 66;

    public AkaishiLifeEnergyEmitterScreen(AkaishiLifeEnergyEmitterMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 纤细横向生命能量条：从左到右填充（long 计算避免溢出）
        GuiWidgets.track(gui, x + BAR_X, y + BAR_Y, BAR_W, BAR_H);
        long life = Math.max(0, Math.min(menu.getLifeEnergy(), menu.getLifeMax()));
        long max = Math.max(1, menu.getLifeMax());
        int barWidth = (int) (BAR_W * life / max);
        if (barWidth > 0) {
            gui.fill(x + BAR_X, y + BAR_Y, x + BAR_X + barWidth, y + BAR_Y + BAR_H, 0xFF28B428);
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

        // 能量数值（条下方居中，单位缩写显示）
        Component value = Component.literal(
                EnergyFormat.format(menu.getLifeEnergy()) + " / " + EnergyFormat.format(menu.getLifeMax()));
        gui.drawString(this.font, value, this.leftPos + 88 - this.font.width(value) / 2,
                this.topPos + VALUE_TEXT_Y, 0xFF3F3F3F, false);

        // 绑定坐标：未绑定时显示提示文本
        BlockPos target = menu.getTarget();
        Component targetText = target == null
                ? Component.translatable("gui.akaishi.emitter.unbound")
                : Component.translatable("gui.akaishi.emitter.target", target.getX(), target.getY(), target.getZ());
        gui.drawString(this.font, targetText, this.leftPos + 88 - this.font.width(targetText) / 2,
                this.topPos + TARGET_TEXT_Y, target == null ? 0xFFA06060 : 0xFF3F3F3F, false);

        // 射程
        Component rangeText = Component.translatable("gui.akaishi.emitter.range",
                AkaishiLifeEnergyEmitterBlockEntity.RANGE);
        gui.drawString(this.font, rangeText, this.leftPos + 88 - this.font.width(rangeText) / 2,
                this.topPos + RANGE_TEXT_Y, 0xFF3F3F3F, false);

        // 鼠标悬停能量条时显示名称与数值
        if (isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.life",
                            EnergyFormat.format(menu.getLifeEnergy()), EnergyFormat.format(menu.getLifeMax())),
                    mouseX, mouseY);
        }
    }
}
