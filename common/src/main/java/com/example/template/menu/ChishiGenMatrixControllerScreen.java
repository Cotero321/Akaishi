package com.example.template.menu;

import com.example.template.TemplateMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 发生器矩阵控制器界面：状态条 + 燃料槽 + 火焰 + 能量条 + 升级倍率。
 * 结构激活时显示当前产能（低级 45 倍 / 高级 200 倍），未激活时提示结构不完整。
 */
public class ChishiGenMatrixControllerScreen extends AbstractContainerScreen<ChishiGenMatrixControllerMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_super_generator.png");

    public ChishiGenMatrixControllerScreen(ChishiGenMatrixControllerMenu menu, Inventory inv, Component title) {
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

        // 赤能源条：右侧垂直条，轨道框 + 从底部向上填充（ceil 保证低储量时至少 1px 可见）
        GuiWidgets.track(gui, x + 154, y + 39, 10, 38);
        long maxEnergy = menu.tier().maxEnergy;
        long energy = Math.max(0, Math.min(menu.getEnergy(), maxEnergy));
        int energyHeight = (int) Math.ceil(36.0 * energy / maxEnergy);
        if (energyHeight > 0) {
            // 内缩 1px 填充，避免满能量时覆盖轨道边框
            gui.fill(x + 155, y + 76 - energyHeight, x + 163, y + 76, 0xFFE03030);
        }

        // 燃料火焰动画（贴图满帧底 y=56）
        int burn = menu.getBurnTime();
        int burnTotal = menu.getBurnTimeTotal();
        if (burnTotal > 0 && burn > 0) {
            int flameHeight = (int) (14.0F * burn / burnTotal);
            gui.blit(TEXTURE, x + 60, y + 56 - flameHeight, 176, 56 - flameHeight, 14, flameHeight);
        }

        // 加速组件装配数：火焰右侧空档显示当前倍率（避开燃料槽）
        gui.drawString(this.font,
                Component.translatable("gui.template_mod.boost_mult",
                        String.format("%.1f", menu.getBoostMultiplier())),
                x + 80, y + 45, 0xFF3F3F3F, false);
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
            Component status = Component.translatable(
                    "gui.template_mod.gen_matrix." + menu.tier().suffix + ".formed", menu.tier().multiply);
            gui.drawString(this.font, status, sx + 3, sy + 3, 0xFF55E050, false);
        } else {
            Component status = Component.translatable("gui.template_mod.gen_matrix.unformed");
            gui.drawString(this.font, status, sx + 3, sy + 3, 0xFFE0C040, false);
        }

        if (isHovering(154, 39, 10, 38, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.energy", menu.getEnergy(), menu.tier().maxEnergy),
                    mouseX, mouseY);
        } else if (isHovering(60, 42, 14, 14, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.fuel", menu.getBurnTime()),
                    mouseX, mouseY);
        } else if (isHovering(21, 19, 134, 14, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    menu.isFormed()
                            ? Component.translatable("gui.template_mod.gen_matrix." + menu.tier().suffix + ".formed_hint", menu.tier().multiply)
                            : Component.translatable("gui.template_mod.gen_matrix.unformed_hint"),
                    mouseX, mouseY);
        }
    }
}
