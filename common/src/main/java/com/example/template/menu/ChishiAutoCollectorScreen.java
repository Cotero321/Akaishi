package com.example.template.menu;

import com.example.template.TemplateMod;
import com.example.template.block.entity.ChishiAutoCollectorBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 自动收集器界面：能量条（右侧）+ 收集进度条 + 27 槽存储区。
 * 数据全部来自 {@link ChishiAutoCollectorMenu} 的 ContainerData，随网络同步刷新。
 */
public class ChishiAutoCollectorScreen extends AbstractContainerScreen<ChishiAutoCollectorMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_auto_collector.png");

    public ChishiAutoCollectorScreen(ChishiAutoCollectorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 赤石能量条：右侧垂直条，从底部向上填充
        int maxEnergy = menu.getEnergyCapacity();
        int energy = Math.max(0, Math.min(menu.getEnergy(), maxEnergy));
        int energyHeight = (int) (44.0F * energy / Math.max(1, maxEnergy));
        if (energyHeight > 0) {
            gui.fill(x + 153, y + 62 - energyHeight, x + 163, y + 62, 0xFFE03030);
        }

        // 收集进度：进度条（中部下方），从左向右填充
        int progress = menu.getProgress();
        int progressWidth = (int) (30.0F * progress / 100);
        if (progressWidth > 0) {
            gui.fill(x + 79, y + 53, x + 79 + progressWidth, y + 57, 0xFFE8E8EA);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 鼠标悬停在功能图标上时显示名称与数值
        if (isHovering(153, 18, 10, 44, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.energy", menu.getEnergy(), menu.getEnergyCapacity()),
                    mouseX, mouseY);
        } else if (isHovering(79, 53, 30, 4, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.collect_progress", menu.getProgress()),
                    mouseX, mouseY);
        }
    }
}
