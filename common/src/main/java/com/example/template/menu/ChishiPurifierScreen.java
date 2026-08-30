package com.example.template.menu;

import com.example.template.TemplateMod;
import com.example.template.block.entity.ChishiPurifierBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 赤石提纯器界面：能量条（右侧）、火焰动画（燃料）、进度条（中间箭头）。
 * 数据全部来自 {@link ChishiPurifierMenu} 的 ContainerData，随网络同步刷新。
 */
public class ChishiPurifierScreen extends AbstractContainerScreen<ChishiPurifierMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_purifier.png");
    /** 提纯矩阵成型版贴图：抹去燃料槽与火焰区域（矩阵由外部赤能源驱动，无燃料槽） */
    private static final ResourceLocation TEXTURE_MATRIX = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_purifier_matrix.png");

    public ChishiPurifierScreen(ChishiPurifierMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        // 成型态切换无燃料版贴图（燃料槽/火焰区域已抹除）
        gui.blit(menu.isFormed() ? TEXTURE_MATRIX : TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 赤石能量条：右侧垂直条，与贴图内底(153,18..62)对齐，从底部向上填充
        int maxEnergy = ChishiPurifierBlockEntity.MAX_ENERGY;
        int energy = Math.max(0, Math.min(menu.getEnergy(), maxEnergy));
        int energyHeight = (int) (44.0F * energy / maxEnergy);
        if (energyHeight > 0) {
            gui.fill(x + 153, y + 62 - energyHeight, x + 163, y + 62, 0xFFE03030);
        }

        // 燃料火焰动画：仅未成型（有燃料槽）时绘制。火焰框位于燃料槽右侧空档（81,60..73），
        // 与提纯进度箭头（79,36..52）错开，避免重叠
        if (!menu.isFormed()) {
            int burn = menu.getBurnTime();
            int burnTotal = menu.getBurnTimeTotal();
            if (burnTotal > 0 && burn > 0) {
                int flameHeight = (int) (14.0F * burn / burnTotal);
                gui.blit(TEXTURE, x + 81, y + 74 - flameHeight, 176, 60 - flameHeight, 14, flameHeight);
            }
        }

        // 提纯进度箭头：覆盖贴图箭头左半(79,36..51)，从左向右填充
        int progress = menu.getProgress();
        int arrowWidth = (int) (10.0F * progress / ChishiPurifierBlockEntity.MAX_PROGRESS);
        if (arrowWidth > 0) {
            gui.fill(x + 79, y + 36, x + 79 + arrowWidth, y + 52, 0xFFE8E8EA);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 鼠标悬停在功能图标上时显示名称与数值
        if (isHovering(153, 18, 10, 44, mouseX, mouseY)) {
            // 右侧赤能源条
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.energy", menu.getEnergy(), ChishiPurifierBlockEntity.MAX_ENERGY),
                    mouseX, mouseY);
        } else if (!menu.isFormed() && isHovering(81, 60, 14, 14, mouseX, mouseY)) {
            // 燃料火焰（成型态无燃料槽，不显示）
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.fuel", menu.getBurnTime()),
                    mouseX, mouseY);
        } else if (isHovering(79, 36, 10, 16, mouseX, mouseY)) {
            // 提纯进度箭头
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.progress", menu.getProgress()),
                    mouseX, mouseY);
        }
    }
}
