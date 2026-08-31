package com.example.template.menu;

import com.example.template.TemplateMod;
import com.example.template.block.entity.ChishiAutoCollectorBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 自动收集器界面（198 高）：状态行 + 能量条 + 27 槽存储区 + 收集进度条。
 * 各区域纵向错开互不重叠：状态 y=18、能量条 y=30、存储槽 y=46..100、
 * 进度条 y=106、玩家背包 y=124 起。数据来自 {@link ChishiAutoCollectorMenu} 的 ContainerData。
 */
public class ChishiAutoCollectorScreen extends AbstractContainerScreen<ChishiAutoCollectorMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_auto_collector.png");
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_DIM = 0xFF707070;
    private static final int TEXT_RED = 0xFFB03030;
    private static final int TEXT_GREEN = 0xFF2E7D32;

    // 能量条
    private static final int BAR_X = 20;
    private static final int BAR_Y = 30;
    private static final int BAR_W = 136;
    private static final int BAR_H = 8;

    // 收集进度条（存储槽 y=46..100 与玩家背包 y=124 之间的空档）
    private static final int PROGRESS_X = 8;
    private static final int PROGRESS_Y = 106;
    private static final int PROGRESS_W = 160;
    private static final int PROGRESS_H = 4;

    public ChishiAutoCollectorScreen(ChishiAutoCollectorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 赤石能量条
        GuiWidgets.track(gui, x + BAR_X, y + BAR_Y, BAR_W, BAR_H);
        int maxEnergy = menu.getEnergyCapacity();
        int energy = Math.max(0, Math.min(menu.getEnergy(), maxEnergy));
        int energyWidth = (int) (BAR_W * energy / Math.max(1, maxEnergy));
        if (energyWidth > 0) {
            gui.fill(x + BAR_X, y + BAR_Y, x + BAR_X + energyWidth, y + BAR_Y + BAR_H, 0xFFE03030);
        }

        // 收集进度条（工作/能量不足时按进度显示，待机恒为 0）
        int progress = menu.getProgress();
        int progressWidth = (int) (PROGRESS_W * progress / 100);
        if (progressWidth > 0) {
            gui.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + progressWidth, y + PROGRESS_Y + PROGRESS_H, 0xFFE8E8EA);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // renderLabels 已 translate(leftPos,topPos)，此处为 GUI 相对坐标
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        // 状态行
        String statusKey = switch (menu.getStatus()) {
            case ChishiAutoCollectorBlockEntity.DATA_STATUS_WORKING -> "gui.template_mod.collector.working";
            case ChishiAutoCollectorBlockEntity.DATA_STATUS_NO_ENERGY -> "gui.template_mod.collector.no_energy";
            default -> "gui.template_mod.collector.idle";
        };
        int statusColor = switch (menu.getStatus()) {
            case ChishiAutoCollectorBlockEntity.DATA_STATUS_WORKING -> TEXT_GREEN;
            case ChishiAutoCollectorBlockEntity.DATA_STATUS_NO_ENERGY -> TEXT_RED;
            default -> TEXT_DIM;
        };
        gui.drawString(this.font, Component.translatable(statusKey), 8, 18, statusColor, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 悬停提示：能量条 / 进度条
        if (isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.energy", menu.getEnergy(), menu.getEnergyCapacity()),
                    mouseX, mouseY);
        } else if (isHovering(PROGRESS_X, PROGRESS_Y, PROGRESS_W, PROGRESS_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.collect_progress", menu.getProgress()),
                    mouseX, mouseY);
        }
    }
}
