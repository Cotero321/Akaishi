package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiEnergyAggregatorBlockEntity;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 赤石能量聚合器界面：顶部能量条 + 中部输入/进度/输出 + 数值文字标签。
 * 能量与进度数值使用 M/K 缩写显示。
 */
public class AkaishiEnergyAggregatorScreen extends AbstractContainerScreen<AkaishiEnergyAggregatorMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    /**
     * 顶部能量条。<b>宽度刻意收窄到 88</b>：右上角要留给三格升级槽（x=116 起），
     * 条子铺到 136 宽会压住无线接收格。
     */
    private static final int BAR_X = 20, BAR_Y = 16, BAR_W = 88, BAR_H = 8;
    private static final int PROGRESS_X = 80, PROGRESS_Y = 34, PROGRESS_W = 28, PROGRESS_H = 16;
    /** 机器槽位数量（输入槽 + 输出槽，贴图无槽位图形需自绘框；升级槽在它们之后，单独画） */
    private static final int MACHINE_SLOTS = 2;

    /** 升级槽（固定面板右上角 Y=8；无线接收格坐标与其余机器统一，见 MachineUpgradeSlots） */
    private static final int UPGRADE_Y = 8;
    private static final int SPEED_SLOT_X = 134;
    private static final int ENERGY_SLOT_X = 152;

    public AkaishiEnergyAggregatorScreen(AkaishiEnergyAggregatorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 机器槽位框（贴图无图形，自绘补齐）
        for (int i = 0; i < MACHINE_SLOTS; i++) {
            var slot = menu.slots.get(i);
            GuiWidgets.slotBox(gui, x + slot.x, y + slot.y);
        }
        // 升级槽框（速度 / 能量 / 无线接收，固定面板右上角 Y=8）
        GuiWidgets.slotBox(gui, x + SPEED_SLOT_X, y + UPGRADE_Y);
        GuiWidgets.slotBox(gui, x + ENERGY_SLOT_X, y + UPGRADE_Y);
        GuiWidgets.slotBox(gui, x + MachineUpgradeSlots.WIRELESS_SLOT_X, y + UPGRADE_Y);

        // 赤能源条（红色）
        GuiWidgets.track(gui, x + BAR_X, y + BAR_Y, BAR_W, BAR_H);
        long max = Math.max(1L, menu.getMaxEnergy());
        int energyWidth = (int) (BAR_W * Math.max(0L, Math.min(menu.getEnergy(), menu.getMaxEnergy())) / max);
        if (energyWidth > 0) {
            gui.fill(x + BAR_X, y + BAR_Y, x + BAR_X + energyWidth, y + BAR_Y + BAR_H, 0xFFE03030);
        }
        // 聚合进度条（黄色）
        GuiWidgets.track(gui, x + PROGRESS_X, y + PROGRESS_Y, PROGRESS_W, PROGRESS_H);
        int progressWidth = (int) (PROGRESS_W * menu.getProgress() / 100.0F);
        if (progressWidth > 0) {
            gui.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + progressWidth, y + PROGRESS_Y + PROGRESS_H, 0xFFFFD030);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF3F3F3F, false);

        // 能量数值文本
        gui.drawString(this.font,
                Component.translatable("gui.akaishi.energy",
                        EnergyFormat.format(menu.getEnergy()), EnergyFormat.format(menu.getMaxEnergy())),
                20, 50, 0xE0E0E0, false);
        // 可合成次数文本（按当前配方消耗计算，母岩升级/赤石锭聚合通用）
        long cost = Math.max(1L, menu.getCurrentCost());
        gui.drawString(this.font,
                Component.translatable("gui.akaishi.craft_times",
                        menu.getEnergy() / cost),
                20, 62, 0xE0E0E0, false);
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

        // 能量条悬停提示（M/K 缩写）
        if (isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.energy",
                            EnergyFormat.format(menu.getEnergy()), EnergyFormat.format(menu.getMaxEnergy())),
                    mouseX, mouseY);
            return;
        }
        // 输入槽（下界合金锭 / 母岩）
        var inputSlot = menu.slots.get(AkaishiEnergyAggregatorBlockEntity.INPUT_SLOT);
        if (isHovering(inputSlot.x, inputSlot.y, 16, 16, mouseX, mouseY) && inputSlot.getItem().isEmpty()) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.aggregator.input_slot"), mouseX, mouseY);
            return;
        }
        // 输出槽（赤石锭 / 升级母岩）
        var outputSlot = menu.slots.get(AkaishiEnergyAggregatorBlockEntity.OUTPUT_SLOT);
        if (isHovering(outputSlot.x, outputSlot.y, 16, 16, mouseX, mouseY) && outputSlot.getItem().isEmpty()) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.aggregator.output_slot"), mouseX, mouseY);
            return;
        }
        // 升级槽（与单槽族机器同一套提示键）
        if (isHovering(SPEED_SLOT_X, UPGRADE_Y, 16, 16, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.upgrade.speed_slot", menu.getSpeedUpgradeCount(),
                            "x" + (1 + menu.getSpeedUpgradeCount())),
                    mouseX, mouseY);
            return;
        }
        if (isHovering(ENERGY_SLOT_X, UPGRADE_Y, 16, 16, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.upgrade.energy_slot", menu.getEnergyUpgradeCount(),
                            "x" + (1F + 0.5F * menu.getEnergyUpgradeCount())),
                    mouseX, mouseY);
            return;
        }
        if (isHovering(MachineUpgradeSlots.WIRELESS_SLOT_X, UPGRADE_Y, 16, 16, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.upgrade.wireless_slot",
                            Component.translatable(menu.hasWirelessReceiver()
                                    ? "gui.akaishi.upgrade.installed" : "gui.akaishi.upgrade.absent")),
                    mouseX, mouseY);
        }
    }
}
