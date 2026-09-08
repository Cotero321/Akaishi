package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiGenMatrixControllerBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 发生器矩阵控制器界面：状态条 + 燃料槽 + 火焰 + 能量条 + 升级倍率。
 * 结构激活时显示当前产能（低级 45 倍 / 高级 200 倍），未激活时提示结构不完整。
 * 10 个升级组件槽固定面板右上角（y=8，5 列）；能量条与状态/倍率文案下移避让升级槽，
 * 保证文字不重叠、不超出（规则 1/3）。
 */
public class AkaishiGenMatrixControllerScreen extends AbstractContainerScreen<AkaishiGenMatrixControllerMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_super_generator.png");

    // 升级槽固定面板右上角 y=8（与 Menu addSlot 坐标一致，5 列整齐，规则 3）
    private static final int[] UPGRADE_COLS = {80, 98, 116, 134, 152};
    private static final int[] UPGRADE_ROWS = {8, 26};
    // 燃料槽（与 Menu FUEL_SLOT 坐标一致）
    private static final int FUEL_SLOT_X = 25, FUEL_SLOT_Y = 42;
    // 赤能源条：右侧垂直条，下移到升级槽下方（行2 底部 y=44 之下），底部避开玩家背包槽
    private static final int ENERGY_X = 154, ENERGY_Y = 48, ENERGY_W = 10, ENERGY_H = 38;
    // 燃料火焰动画（贴图满帧底 y=56，位于燃料槽右侧，与升级槽列1 留 6px 间隙）
    private static final int FLAME_X = 60, FLAME_Y = 56;
    // 状态条文字：下移到升级槽与燃料下方空白区（y=62），超宽截断避免压到能量条
    private static final int STATUS_X = 21, STATUS_Y = 62, STATUS_MAX_W = 128;
    // 加速倍率文案：状态条下方（y=75），左对齐避让能量条
    private static final int BOOST_X = 21, BOOST_Y = 75;

    public AkaishiGenMatrixControllerScreen(AkaishiGenMatrixControllerMenu menu, Inventory inv, Component title) {
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

        // 升级槽框（贴图无新位置的槽位图形，自绘补齐；燃料槽沿用贴图）
        for (int r = 0; r < UPGRADE_ROWS.length; r++) {
            for (int c = 0; c < UPGRADE_COLS.length; c++) {
                GuiWidgets.slotBox(gui, x + UPGRADE_COLS[c], y + UPGRADE_ROWS[r]);
            }
        }

        // 赤能源条：右侧垂直条，轨道框 + 从底部向上填充（ceil 保证低储量时至少 1px 可见）
        GuiWidgets.track(gui, x + ENERGY_X, y + ENERGY_Y, ENERGY_W, ENERGY_H);
        long maxEnergy = menu.tier().maxEnergy;
        long energy = Math.max(0, Math.min(menu.getEnergy(), maxEnergy));
        int energyHeight = (int) Math.ceil((ENERGY_H - 2.0) * energy / maxEnergy);
        if (energyHeight > 0) {
            // 内缩 1px 填充，避免满能量时覆盖轨道边框
            gui.fill(x + ENERGY_X + 1, y + ENERGY_Y + ENERGY_H - energyHeight,
                    x + ENERGY_X + ENERGY_W - 1, y + ENERGY_Y + ENERGY_H, 0xFFE03030);
        }

        // 燃料火焰动画（贴图满帧底 y=56）
        int burn = menu.getBurnTime();
        int burnTotal = menu.getBurnTimeTotal();
        if (burnTotal > 0 && burn > 0) {
            int flameHeight = (int) (14.0F * burn / burnTotal);
            gui.blit(TEXTURE, x + FLAME_X, y + FLAME_Y - flameHeight, 176, FLAME_Y - flameHeight, 14, flameHeight);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 状态条文字：结构激活状态（绿色已激活 / 黄色结构不完整），超宽截断避免压到能量条
        int sx = this.leftPos;
        int sy = this.topPos;
        Component status = menu.isFormed()
                ? Component.translatable("gui.akaishi.gen_matrix." + menu.tier().suffix + ".formed", menu.tier().multiply)
                : Component.translatable("gui.akaishi.gen_matrix.unformed");
        gui.drawString(this.font,
                this.font.plainSubstrByWidth(status.getString(), STATUS_MAX_W),
                sx + STATUS_X, sy + STATUS_Y,
                menu.isFormed() ? 0xFF55E050 : 0xFFE0C040, false);

        // 加速倍率文案（状态条下方）
        gui.drawString(this.font,
                Component.translatable("gui.akaishi.boost_mult",
                        String.format("%.1f", menu.getBoostMultiplier())),
                sx + BOOST_X, sy + BOOST_Y, 0xFF3F3F3F, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);
        if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.energy", menu.getEnergy(), menu.tier().maxEnergy),
                    mouseX, mouseY);
        } else if (isHovering(FLAME_X, FUEL_SLOT_Y, 14, 14, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.fuel", menu.getBurnTime()),
                    mouseX, mouseY);
        } else if (isHovering(STATUS_X - 2, STATUS_Y - 2, STATUS_MAX_W + 4, 14, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    menu.isFormed()
                            ? Component.translatable("gui.akaishi.gen_matrix." + menu.tier().suffix + ".formed_hint", menu.tier().multiply)
                            : Component.translatable("gui.akaishi.gen_matrix.unformed_hint"),
                    mouseX, mouseY);
        }
        // 10 个升级组件槽：仅空槽时提示用途（有物品时 vanilla 已显示物品名，避免重复 tooltip，规则 9）
        for (int r = 0; r < UPGRADE_ROWS.length; r++) {
            for (int c = 0; c < UPGRADE_COLS.length; c++) {
                int slotIndex = AkaishiGenMatrixControllerBlockEntity.UPGRADE_SLOT_START + r * UPGRADE_COLS.length + c;
                if (isHovering(UPGRADE_COLS[c], UPGRADE_ROWS[r], 16, 16, mouseX, mouseY)
                        && menu.slots.get(slotIndex).getItem().isEmpty()) {
                    gui.renderTooltip(this.font,
                            Component.translatable("gui.akaishi.gen_matrix.upgrade_slot",
                                    menu.getUpgradeCount(), "x" + String.format("%.1f", menu.getBoostMultiplier())),
                            mouseX, mouseY);
                    return; // 同一时刻只显示一个升级槽 tooltip
                }
            }
        }
    }
}
