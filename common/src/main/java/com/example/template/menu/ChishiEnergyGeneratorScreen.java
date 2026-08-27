package com.example.template.menu;

import com.example.template.TemplateMod;
import com.example.template.block.entity.ChishiEnergyGeneratorBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 赤能源发生机界面：顶部燃料槽 + 中央火焰动画 + 右侧能量条。
 * 数据来自 {@link ChishiEnergyGeneratorMenu} 的 ContainerData。
 */
public class ChishiEnergyGeneratorScreen extends AbstractContainerScreen<ChishiEnergyGeneratorMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_energy_generator.png");

    public ChishiEnergyGeneratorScreen(ChishiEnergyGeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 赤能源条：右侧垂直条，从底部向上填充
        int maxEnergy = ChishiEnergyGeneratorBlockEntity.MAX_ENERGY;
        int energy = Math.max(0, Math.min(menu.getEnergy(), maxEnergy));
        int energyHeight = (int) (44.0F * energy / maxEnergy);
        if (energyHeight > 0) {
            gui.fill(x + 153, y + 62 - energyHeight, x + 163, y + 62, 0xFFE03030);
        }

        // 燃料火焰动画：按剩余比例裁剪满帧
        int burn = menu.getBurnTime();
        int burnTotal = menu.getBurnTimeTotal();
        if (burnTotal > 0 && burn > 0) {
            int flameHeight = (int) (14.0F * burn / burnTotal);
            gui.blit(TEXTURE, x + 81, y + 49 - flameHeight, 176, 49 - flameHeight, 14, flameHeight);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        if (isHovering(153, 18, 10, 44, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.energy", menu.getEnergy(), ChishiEnergyGeneratorBlockEntity.MAX_ENERGY),
                    mouseX, mouseY);
        } else if (isHovering(81, 35, 14, 14, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.fuel", menu.getBurnTime()),
                    mouseX, mouseY);
        }
    }
}
