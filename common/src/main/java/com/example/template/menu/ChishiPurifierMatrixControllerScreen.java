package com.example.template.menu;

import com.example.template.TemplateMod;
import com.example.template.block.entity.ChishiPurifierMatrixControllerBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 提纯矩阵控制器界面：状态条 + 输入/输出槽 + 进度箭头 + 能量条。
 * 复用成型版贴图（无燃料槽，矩阵由外部赤能源驱动），结构激活时实时显示提纯进度。
 */
public class ChishiPurifierMatrixControllerScreen extends AbstractContainerScreen<ChishiPurifierMatrixControllerMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_purifier_matrix.png");

    public ChishiPurifierMatrixControllerScreen(ChishiPurifierMatrixControllerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 赤能源条：右侧垂直条（153,18..62），轨道框 + 从底部向上填充
        GuiWidgets.track(gui, x + 153, y + 18, 10, 44);
        long maxEnergy = ChishiPurifierMatrixControllerBlockEntity.MAX_ENERGY;
        long energy = Math.max(0, Math.min(menu.getEnergy(), maxEnergy));
        int energyHeight = (int) Math.ceil(42.0 * energy / maxEnergy);
        if (energyHeight > 0) {
            // 内缩 1px 填充，避免满能量时覆盖轨道边框
            gui.fill(x + 154, y + 61 - energyHeight, x + 162, y + 61, 0xFFE03030);
        }

        // 提纯进度箭头：覆盖贴图箭头左半(79,36..51)，从左向右填充
        int progress = menu.getProgress();
        int arrowWidth = (int) (10.0F * progress / 100);
        if (arrowWidth > 0) {
            gui.fill(x + 79, y + 36, x + 79 + arrowWidth, y + 52, 0xFFE8E8EA);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 状态条文字：结构激活状态（绿色已激活 / 黄色结构不完整），置于进度箭头下方、避开物品栏标签
        int sx = this.leftPos + 8;
        int sy = this.topPos + 56;
        if (menu.isFormed()) {
            gui.drawString(this.font, Component.translatable("gui.template_mod.purifier_matrix.formed"),
                    sx + 3, sy + 3, 0xFF55E050, false);
        } else {
            gui.drawString(this.font, Component.translatable("gui.template_mod.purifier_matrix.unformed"),
                    sx + 3, sy + 3, 0xFFE0C040, false);
        }

        // 功能图标悬停提示
        if (isHovering(153, 18, 10, 44, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.energy", menu.getEnergy(), ChishiPurifierMatrixControllerBlockEntity.MAX_ENERGY),
                    mouseX, mouseY);
        } else if (isHovering(79, 36, 10, 16, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.progress", menu.getProgress()),
                    mouseX, mouseY);
        } else if (isHovering(8, 56, 160, 14, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    menu.isFormed()
                            ? Component.translatable("gui.template_mod.purifier_matrix.formed_hint")
                            : Component.translatable("gui.template_mod.purifier_matrix.unformed_hint"),
                    mouseX, mouseY);
        }
    }
}
