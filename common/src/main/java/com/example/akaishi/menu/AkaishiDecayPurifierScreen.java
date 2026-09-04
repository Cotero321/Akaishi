package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 衰变净化塔界面：净化状态 + 范围内区域数 + 赤能源条 + 升级槽。
 * 无机器槽位；数据来自 {@link AkaishiDecayPurifierMenu} 的 ContainerData，随网络同步。
 * 复用无线终端 256×256 贴图（顶部 176×198 内容区，玩家背包槽位图案对齐）。
 */
public class AkaishiDecayPurifierScreen extends AbstractContainerScreen<AkaishiDecayPurifierMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_wireless_terminal.png");
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_GREEN = 0xFF2E7D32;
    private static final int TEXT_RED = 0xFFB03030;

    // 能量条（升级槽 y=30..46 下方空档）
    private static final int BAR_X = 20;
    private static final int BAR_Y = 58;
    private static final int BAR_W = 136;
    private static final int BAR_H = 8;
    // 升级槽（与 Menu 槽位坐标一致）
    private static final int SPEED_SLOT_X = 134, SPEED_SLOT_Y = 30;
    private static final int ENERGY_SLOT_X = 152, ENERGY_SLOT_Y = 30;

    public AkaishiDecayPurifierScreen(AkaishiDecayPurifierMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 能量条
        GuiWidgets.track(gui, x + BAR_X, y + BAR_Y, BAR_W, BAR_H);
        long max = Math.max(1, menu.getEnergyCapacity());
        int width = (int) (BAR_W * Math.max(0, Math.min(menu.getEnergy(), max)) / max);
        if (width > 0) {
            gui.fill(x + BAR_X, y + BAR_Y, x + BAR_X + width, y + BAR_Y + BAR_H, 0xFFE03030);
        }

        // 升级槽（速度/能量，贴图无图案需自绘框 + 槽位上方标签）
        GuiWidgets.slotBox(gui, x + SPEED_SLOT_X, y + SPEED_SLOT_Y);
        GuiWidgets.slotBox(gui, x + ENERGY_SLOT_X, y + ENERGY_SLOT_Y);
        gui.drawString(this.font, Component.translatable("gui.akaishi.upgrade.tag"),
                x + SPEED_SLOT_X, y + SPEED_SLOT_Y - 9, 0xFF707070, false);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // renderLabels 已 translate(leftPos,topPos)，此处为 GUI 相对坐标
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);

        // 净化状态：净化中 / 能量不足（有区域但停） / 待机（无区域）
        boolean working = menu.isWorking();
        boolean noEnergy = !working && menu.getZoneCount() > 0;
        Component status;
        int color;
        if (working) {
            status = Component.translatable("gui.akaishi.decay_purifier.working");
            color = TEXT_GREEN;
        } else if (noEnergy) {
            status = Component.translatable("gui.akaishi.decay_purifier.no_energy");
            color = TEXT_RED;
        } else {
            status = Component.translatable("gui.akaishi.decay_purifier.idle");
            color = 0xFF707070;
        }
        gui.drawString(this.font, status, 8, 24, color, false);

        // 范围内区域数（状态行下方，避开右侧升级槽 y=30..46 与能量数值 y=70）
        gui.drawString(this.font, Component.translatable("gui.akaishi.decay_purifier.zones",
                        menu.getZoneCount()),
                8, 33, TEXT, false);

        // 能量数值（居中，右对齐防出框）
        Component energyText = Component.translatable("gui.akaishi.energy",
                EnergyFormat.format(menu.getEnergy()), EnergyFormat.format(menu.getEnergyCapacity()));
        int textWidth = this.font.width(energyText);
        gui.drawString(this.font, energyText, 88 - textWidth / 2, BAR_Y + 12, TEXT, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 能量条悬停提示
        if (isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.energy",
                            EnergyFormat.format(menu.getEnergy()), EnergyFormat.format(menu.getEnergyCapacity())),
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
