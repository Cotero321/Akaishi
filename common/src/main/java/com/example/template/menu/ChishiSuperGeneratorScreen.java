package com.example.template.menu;

import com.example.template.TemplateMod;
import com.example.template.block.entity.ChishiSuperGeneratorCoreBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 超级发生器架构核心界面：状态条 + 燃料槽 + 火焰 + 能量条。
 * 结构激活时显示当前产能（200 倍），未激活时提示结构不完整。
 */
public class ChishiSuperGeneratorScreen extends AbstractContainerScreen<ChishiSuperGeneratorMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_energy_assembly.png");

    public ChishiSuperGeneratorScreen(ChishiSuperGeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 赤能源条：右侧垂直条，从底部向上填充（贴图内底 154,39..164,77）
        int maxEnergy = (int) ChishiSuperGeneratorCoreBlockEntity.MAX_ENERGY;
        int energy = Math.max(0, Math.min(menu.getEnergy(), maxEnergy));
        int energyHeight = (int) (38.0F * energy / maxEnergy);
        if (energyHeight > 0) {
            gui.fill(x + 154, y + 77 - energyHeight, x + 164, y + 77, 0xFFE03030);
        }

        // 燃料火焰动画（贴图满帧底 y=56）
        int burn = menu.getBurnTime();
        int burnTotal = menu.getBurnTimeTotal();
        if (burnTotal > 0 && burn > 0) {
            int flameHeight = (int) (14.0F * burn / burnTotal);
            gui.blit(TEXTURE, x + 60, y + 56 - flameHeight, 176, 56 - flameHeight, 14, flameHeight);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 状态条文字：结构激活状态（绿色已激活 / 黄色结构不完整）
        int sx = this.leftPos + 21;
        int sy = this.topPos + 20;
        if (menu.isFormed()) {
            Component status = Component.translatable("gui.template_mod.super_generator.formed",
                    ChishiSuperGeneratorCoreBlockEntity.GENERATE_RATE);
            gui.drawString(this.font, status, sx + 3, sy + 3, 0xFF55E050, false);
        } else {
            Component status = Component.translatable("gui.template_mod.super_generator.unformed");
            gui.drawString(this.font, status, sx + 3, sy + 3, 0xFFE0C040, false);
        }

        if (isHovering(154, 39, 10, 38, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.energy", menu.getEnergy(),
                            ChishiSuperGeneratorCoreBlockEntity.MAX_ENERGY),
                    mouseX, mouseY);
        } else if (isHovering(60, 42, 14, 14, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.fuel", menu.getBurnTime()),
                    mouseX, mouseY);
        } else if (isHovering(21, 19, 134, 14, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    menu.isFormed()
                            ? Component.translatable("gui.template_mod.super_generator.formed_hint",
                                    ChishiSuperGeneratorCoreBlockEntity.GENERATE_RATE)
                            : Component.translatable("gui.template_mod.super_generator.unformed_hint"),
                    mouseX, mouseY);
        }
    }
}
