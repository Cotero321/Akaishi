package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiPurifierMatrixControllerBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 提纯矩阵控制器界面：状态条 + 输入/输出槽 + 进度箭头 + 能量条。
 * 复用成型版贴图（无燃料槽，矩阵由外部赤能源驱动），结构激活时实时显示提纯进度。
 */
public class AkaishiPurifierMatrixControllerScreen extends AbstractContainerScreen<AkaishiPurifierMatrixControllerMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_purifier_matrix.png");
    /** 升级槽 GUI 位置（与 Menu 槽位坐标一致，输入槽正下方空地，避开成型状态文字区） */
    private static final int SPEED_SLOT_X = 56, SPEED_SLOT_Y = 53;
    private static final int ENERGY_SLOT_X = 74, ENERGY_SLOT_Y = 53;

    public AkaishiPurifierMatrixControllerScreen(AkaishiPurifierMatrixControllerMenu menu, Inventory inv, Component title) {
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
        long maxEnergy = AkaishiPurifierMatrixControllerBlockEntity.MAX_ENERGY;
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

        // 升级槽（速度/能量，纹理无图案需自绘框 + 槽位上方独立标签，避免与状态文本重合）
        GuiWidgets.slotBox(gui, x + SPEED_SLOT_X, y + SPEED_SLOT_Y);
        GuiWidgets.slotBox(gui, x + ENERGY_SLOT_X, y + ENERGY_SLOT_Y);
        gui.drawString(this.font, Component.translatable("gui.akaishi.upgrade.speed_tag"),
                x + SPEED_SLOT_X, y + SPEED_SLOT_Y - 9, 0xFF707070, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.upgrade.energy_tag"),
                x + ENERGY_SLOT_X, y + ENERGY_SLOT_Y - 9, 0xFF707070, false);
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
            gui.drawString(this.font, Component.translatable("gui.akaishi.purifier_matrix.formed"),
                    sx + 3, sy + 3, 0xFF55E050, false);
        } else {
            gui.drawString(this.font, Component.translatable("gui.akaishi.purifier_matrix.unformed"),
                    sx + 3, sy + 3, 0xFFE0C040, false);
        }

        // 功能图标悬停提示
        if (isHovering(153, 18, 10, 44, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.energy", menu.getEnergy(), AkaishiPurifierMatrixControllerBlockEntity.MAX_ENERGY),
                    mouseX, mouseY);
        } else if (isHovering(79, 36, 10, 16, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.progress", menu.getProgress()),
                    mouseX, mouseY);
        } else if (isHovering(8, 56, 160, 14, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    menu.isFormed()
                            ? Component.translatable("gui.akaishi.purifier_matrix.formed_hint")
                            : Component.translatable("gui.akaishi.purifier_matrix.unformed_hint"),
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
