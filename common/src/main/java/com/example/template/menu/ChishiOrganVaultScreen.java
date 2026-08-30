package com.example.template.menu;

import com.example.template.block.entity.ChishiOrganVaultBlockEntity;
import com.example.template.life.body.BodySlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 器官储藏库界面：左侧 3×3 页按钮（点击切换选中页，纯本地状态），
 * 中部当前页 9 格，右侧暂存区 9 格，顶部生命能量维持条。
 */
public class ChishiOrganVaultScreen extends AbstractContainerScreen<ChishiOrganVaultMenu> {

    private static final int PANEL_W = 192;
    private static final int PANEL_H = 178;
    /** 页按钮区（3×3） */
    private static final int BTN_X = 8, BTN_Y = 22, BTN_STEP = 18;
    /** 页槽区（3×3） */
    private static final int PAGE_X = 66, PAGE_Y = 22;
    /** 暂存区（3×3） */
    private static final int TEMP_X = 124, TEMP_Y = 22;
    /** 能量条（标题与活性状态右侧，避免文字覆盖条体） */
    private static final int ENERGY_X = 60, ENERGY_Y = 8, ENERGY_W = 80, ENERGY_H = 8;

    private static final int COLOR_BG = 0xFFC6C6C6;
    private static final int COLOR_LINE = 0xFF373737;
    private static final int COLOR_ENERGY = 0xFF28B428;
    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_SELECTED = 0xFF5B8731;
    private static final int COLOR_ACTIVE = 0xFF2E7D32;
    private static final int COLOR_DORMANT = 0xFFFF5040;

    public ChishiOrganVaultScreen(ChishiOrganVaultMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.fill(x, y, x + PANEL_W, y + PANEL_H, COLOR_BG);
        gui.fill(x + 6, y + 14, x + PANEL_W - 6, y + PANEL_H - 7, 0xFFB0B0B0);

        // 生命能量维持条
        long energy = menu.getLifeEnergy();
        long cap = Math.max(1, menu.getLifeMax());
        int w = (int) (ENERGY_W * Math.max(0, Math.min(energy, cap)) / cap);
        gui.fill(x + ENERGY_X, y + ENERGY_Y, x + ENERGY_X + ENERGY_W, y + ENERGY_Y + ENERGY_H, COLOR_LINE);
        if (w > 0) {
            gui.fill(x + ENERGY_X, y + ENERGY_Y, x + ENERGY_X + w, y + ENERGY_Y + ENERGY_H, COLOR_ENERGY);
        }

        // 页按钮（1-9，选中高亮）
        for (int i = 0; i < ChishiOrganVaultBlockEntity.PAGE_COUNT; i++) {
            int bx = x + BTN_X + (i % 3) * BTN_STEP;
            int by = y + BTN_Y + (i / 3) * BTN_STEP;
            boolean selected = menu.getCurrentPage() == i;
            gui.fill(bx, by, bx + 18, by + 18, selected ? COLOR_SELECTED : 0xFF8B8B8B);
            gui.fill(bx, by, bx + 18, by + 1, COLOR_LINE);
            gui.fill(bx, by + 17, bx + 18, by + 18, COLOR_LINE);
            gui.fill(bx, by, bx + 1, by + 18, COLOR_LINE);
            gui.fill(bx + 17, by, bx + 18, by + 18, COLOR_LINE);
            // 页码数字（居中；完整部位名经悬停提示展示）
            BodySlot slot = BodySlot.values()[i];
            String label = String.valueOf(i + 1);
            gui.drawString(this.font, label, bx + 6, by + 5,
                    selected ? COLOR_ACTIVE : 0xFF3F3F3F, false);
        }

        // 槽位背景框
        for (var slot : this.menu.slots) {
            drawSlotBox(gui, x + slot.x, y + slot.y);
        }
    }

    private void drawSlotBox(GuiGraphics gui, int x, int y) {
        gui.fill(x, y, x + 18, y + 18, COLOR_SLOT);
        gui.fill(x, y, x + 18, y + 1, COLOR_LINE);
        gui.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        gui.fill(x, y, x + 1, y + 18, COLOR_LINE);
        gui.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, 4, 0xFF3F3F3F, false);
        // 活性状态（颜色由参数控制，不再叠加 § 色码）
        gui.drawString(this.font,
                Component.translatable(menu.isActive()
                        ? "gui.template_mod.organ_vault.active"
                        : "gui.template_mod.organ_vault.dormant"),
                144, 10, menu.isActive() ? COLOR_ACTIVE : COLOR_DORMANT, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 能量条悬停提示
        if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.life", menu.getLifeEnergy(), menu.getLifeMax()),
                    mouseX, mouseY);
        }

        // 页按钮悬停：槽位名 tooltip
        for (int i = 0; i < ChishiOrganVaultBlockEntity.PAGE_COUNT; i++) {
            int bx = this.leftPos + BTN_X + (i % 3) * BTN_STEP;
            int by = this.topPos + BTN_Y + (i / 3) * BTN_STEP;
            if (mouseX >= bx && mouseX < bx + 18 && mouseY >= by && mouseY < by + 18) {
                BodySlot slot = BodySlot.values()[i];
                gui.renderTooltip(this.font,
                        Component.translatable(slot.getNameKey()), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < ChishiOrganVaultBlockEntity.PAGE_COUNT; i++) {
                int bx = this.leftPos + BTN_X + (i % 3) * BTN_STEP;
                int by = this.topPos + BTN_Y + (i / 3) * BTN_STEP;
                if (mouseX >= bx && mouseX < bx + 18 && mouseY >= by && mouseY < by + 18) {
                    menu.setCurrentPage(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
