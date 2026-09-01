package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiEnergyGeneratorBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 赤能源发生机界面：顶部燃料槽 + 中央火焰动画 + 右侧能量条。
 * 数据来自 {@link AkaishiEnergyGeneratorMenu} 的 ContainerData。
 */
public class AkaishiEnergyGeneratorScreen extends AbstractContainerScreen<AkaishiEnergyGeneratorMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_generator.png");

    public AkaishiEnergyGeneratorScreen(AkaishiEnergyGeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 176;
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // 仅绘制标题：背包区下移后抑制原版"物品栏"标签，避免压在第二行升级槽上
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF3F3F3F, false);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 赤能源条：右侧垂直条，从底部向上填充（ceil 保证低储量时至少 1px 可见）
        int maxEnergy = AkaishiEnergyGeneratorBlockEntity.MAX_ENERGY;
        int energy = Math.max(0, Math.min(menu.getEnergy(), maxEnergy));
        int energyHeight = (int) Math.ceil(44.0 * energy / maxEnergy);
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

        // 加速组件装配数：左上角显示当前倍率
        gui.drawString(this.font,
                Component.translatable("gui.akaishi.boost_mult",
                        String.format("%.1f", menu.getBoostMultiplier())),
                x + 8, y + 40, 0xFF3F3F3F, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        if (isHovering(153, 18, 10, 44, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.energy", menu.getEnergy(), AkaishiEnergyGeneratorBlockEntity.MAX_ENERGY),
                    mouseX, mouseY);
        } else if (isHovering(81, 35, 14, 14, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.fuel", menu.getBurnTime()),
                    mouseX, mouseY);
        }
    }
}
