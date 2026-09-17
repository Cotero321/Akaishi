package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiGenMatrixControllerBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 发生器矩阵控制器界面（176×198 自绘，与单槽机器/发生机风格一致，不依赖贴图）：
 * 燃料槽 + 单个升级组件槽（右上角）+ 赤能源条 + 燃烧进度条 + 结构状态 + 加速倍率。
 * 坐标全部由代码决定，并与 {@link AkaishiGenMatrixControllerMenu} 的槽位一致。
 */
public class AkaishiGenMatrixControllerScreen extends AbstractContainerScreen<AkaishiGenMatrixControllerMenu> {

    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_DIM = 0xFF707070;

    // 布局坐标（与 Menu 槽位严格一致）
    private static final int FUEL_X = 26, FUEL_Y = 40;
    private static final int UPGRADE_X = 152, UPGRADE_Y = 8;
    private static final int BAR_X = 20, BAR_W = 136, BAR_H = 8;
    private static final int ENERGY_Y = 60;
    private static final int BURN_Y = 76;
    /** 状态文案行（结构是否激活） */
    private static final int STATUS_X = 20, STATUS_Y = 90, STATUS_MAX_W = 136;
    private static final int BOOST_X = 20, BOOST_Y = 102;

    public AkaishiGenMatrixControllerScreen(AkaishiGenMatrixControllerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        // 不透明背景面板（vanilla 灰，含四周内凹边框）
        GuiWidgets.panel(gui, x, y, this.imageWidth, this.imageHeight);
        GuiWidgets.slotBox(gui, x + FUEL_X, y + FUEL_Y);
        GuiWidgets.slotBox(gui, x + UPGRADE_X, y + UPGRADE_Y);
        GuiWidgets.playerInventory(gui, x, y, 124, 180);

        // 赤能源条（红，内缩 1px 填充避免覆盖轨道边框）；容量随矩阵等级变化
        long maxEnergy = Math.max(1, menu.tier().maxEnergy);
        GuiWidgets.track(gui, x + BAR_X, y + ENERGY_Y, BAR_W, BAR_H);
        GuiWidgets.bar(gui, x + BAR_X + 1, y + ENERGY_Y + 1, BAR_W - 2, BAR_H - 2,
                menu.getEnergy(), maxEnergy, 0xFFE03030);
        // 燃烧进度条（金）
        GuiWidgets.track(gui, x + BAR_X, y + BURN_Y, BAR_W, BAR_H);
        GuiWidgets.bar(gui, x + BAR_X + 1, y + BURN_Y + 1, BAR_W - 2, BAR_H - 2,
                menu.getBurnTime(), Math.max(1, menu.getBurnTimeTotal()), 0xFFFFD030);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // renderLabels 已 translate(leftPos,topPos)，此处为 GUI 相对坐标
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.fuel.label"), FUEL_X, FUEL_Y - 10, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.upgrade.tag"),
                UPGRADE_X - 36, UPGRADE_Y + 4, TEXT_DIM, false);
        gui.drawString(this.font, Component.translatable("container.inventory"), 8, 116, TEXT, false);

        // 结构状态：激活（绿）/ 不完整（黄），超宽截断避免越出面板
        Component status = menu.isFormed()
                ? Component.translatable("gui.akaishi.gen_matrix." + menu.tier().suffix + ".formed", menu.tier().multiply)
                : Component.translatable("gui.akaishi.gen_matrix.unformed");
        gui.drawString(this.font, this.font.plainSubstrByWidth(status.getString(), STATUS_MAX_W),
                STATUS_X, STATUS_Y, menu.isFormed() ? 0xFF55E050 : 0xFFE0C040, false);

        gui.drawString(this.font, Component.translatable("gui.akaishi.boost_mult",
                String.format("%.1f", menu.getBoostMultiplier())), BOOST_X, BOOST_Y, TEXT, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);
        if (isHovering(BAR_X, ENERGY_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.energy",
                    EnergyFormat.format(menu.getEnergy()), EnergyFormat.format(Math.max(1, menu.tier().maxEnergy))),
                    mouseX, mouseY);
        } else if (isHovering(BAR_X, BURN_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.fuel", menu.getBurnTime()), mouseX, mouseY);
        } else if (isHovering(FUEL_X, FUEL_Y, 16, 16, mouseX, mouseY)
                && menu.slots.get(0).getItem().isEmpty()) {
            // 空燃料槽提示用途（有物品时 vanilla 已显示物品名，避免重复 tooltip）
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.gen_matrix.fuel_slot"), mouseX, mouseY);
        } else if (isHovering(UPGRADE_X, UPGRADE_Y, 16, 16, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.gen_matrix.upgrade_slot",
                            menu.getUpgradeCount(), String.format("%.1f", menu.getBoostMultiplier())),
                    mouseX, mouseY);
        } else if (isHovering(STATUS_X, STATUS_Y, STATUS_MAX_W, 10, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    menu.isFormed()
                            ? Component.translatable("gui.akaishi.gen_matrix." + menu.tier().suffix + ".formed_hint", menu.tier().multiply)
                            : Component.translatable("gui.akaishi.gen_matrix.unformed_hint"),
                    mouseX, mouseY);
        }
    }
}
