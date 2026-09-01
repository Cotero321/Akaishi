package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiAutoCollectorBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 自动收集器界面（198 高）：状态行 + 能量条 + 27 槽存储区 + 收集进度条。
 * 各区域纵向错开互不重叠：状态 y=18、能量条 y=30、存储槽 y=46..100、
 * 进度条 y=106、玩家背包 y=124 起。数据来自 {@link AkaishiAutoCollectorMenu} 的 ContainerData。
 */
public class AkaishiAutoCollectorScreen extends AbstractContainerScreen<AkaishiAutoCollectorMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_auto_collector.png");
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

    /** 升级槽 GUI 位置（与 Menu 槽位坐标一致，顶部右侧避开状态行/能量条） */
    private static final int SPEED_SLOT_X = 134, SPEED_SLOT_Y = 8;
    private static final int ENERGY_SLOT_X = 152, ENERGY_SLOT_Y = 8;

    public AkaishiAutoCollectorScreen(AkaishiAutoCollectorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 升级槽（速度/能量，顶部右侧，纹理无图案需自绘框 + 标签）
        GuiWidgets.slotBox(gui, x + SPEED_SLOT_X, y + SPEED_SLOT_Y);
        GuiWidgets.slotBox(gui, x + ENERGY_SLOT_X, y + ENERGY_SLOT_Y);
        gui.drawString(this.font, Component.translatable("gui.akaishi.upgrade.tag"),
                x + SPEED_SLOT_X - 36, y + SPEED_SLOT_Y + 4, 0xFF707070, false);

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
            case AkaishiAutoCollectorBlockEntity.DATA_STATUS_WORKING -> "gui.akaishi.collector.working";
            case AkaishiAutoCollectorBlockEntity.DATA_STATUS_NO_ENERGY -> "gui.akaishi.collector.no_energy";
            default -> "gui.akaishi.collector.idle";
        };
        int statusColor = switch (menu.getStatus()) {
            case AkaishiAutoCollectorBlockEntity.DATA_STATUS_WORKING -> TEXT_GREEN;
            case AkaishiAutoCollectorBlockEntity.DATA_STATUS_NO_ENERGY -> TEXT_RED;
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
                    Component.translatable("gui.akaishi.energy", menu.getEnergy(), menu.getEnergyCapacity()),
                    mouseX, mouseY);
        } else if (isHovering(PROGRESS_X, PROGRESS_Y, PROGRESS_W, PROGRESS_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.collect_progress", menu.getProgress()),
                    mouseX, mouseY);
        }
        // 升级槽悬停提示
        if (isHovering(SPEED_SLOT_X, SPEED_SLOT_Y, 16, 16, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.upgrade.speed_slot", menu.getSpeedUpgradeCount(),
                            "x" + (1F + 0.125F * menu.getSpeedUpgradeCount())),
                    mouseX, mouseY);
        }
        if (isHovering(ENERGY_SLOT_X, ENERGY_SLOT_Y, 16, 16, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.upgrade.energy_slot", menu.getEnergyUpgradeCount(),
                            "x" + (1F + 0.5F * menu.getEnergyUpgradeCount())),
                    mouseX, mouseY);
        }
    }
}
