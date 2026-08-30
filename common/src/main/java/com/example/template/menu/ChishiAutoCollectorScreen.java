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

        // 赤石能量条：横向条置于收集进度条下方空档（y79..83，避开 9×3 存储槽 y17..71 与进度条 y74..77）
        GuiWidgets.track(gui, x + 8, y + 79, 160, 4);
        int maxEnergy = menu.getEnergyCapacity();
        int energy = Math.max(0, Math.min(menu.getEnergy(), maxEnergy));
        int energyWidth = (int) (160.0F * energy / Math.max(1, maxEnergy));
        if (energyWidth > 0) {
            gui.fill(x + 8, y + 79, x + 8 + energyWidth, y + 83, 0xFFE03030);
        }

        // 收集进度：进度条（存储区与背包之间的空档，避免压在存储槽上）
        int progress = menu.getProgress();
        int progressWidth = (int) (160.0F * progress / 100);
        if (progressWidth > 0) {
            gui.fill(x + 8, y + 74, x + 8 + progressWidth, y + 78, 0xFFE8E8EA);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 鼠标悬停在功能图标上时显示名称与数值
        if (isHovering(8, 79, 160, 4, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.energy", menu.getEnergy(), menu.getEnergyCapacity()),
                    mouseX, mouseY);
        } else if (isHovering(8, 74, 160, 4, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.collect_progress", menu.getProgress()),
                    mouseX, mouseY);
        }
    }
}
