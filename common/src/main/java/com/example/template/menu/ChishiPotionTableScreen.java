package com.example.template.menu;

import com.example.template.block.entity.ChishiPotionTableBlockEntity;
import com.example.template.life.potion.PotionRegistry;
import com.example.template.life.potion.PotionTemplate;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * 药剂台界面：左侧模板按钮（永久/突破，点击 C2S 选择），中部样本/固态/输出槽，
 * 底部制作进度条。界面高 166 标准背包布局。
 */
public class ChishiPotionTableScreen extends AbstractContainerScreen<ChishiPotionTableMenu> {

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 166;
    /** 能量条 */
    private static final int ENERGY_X = 16, ENERGY_Y = 16, ENERGY_W = 96, ENERGY_H = 8;
    /** 模板按钮区 */
    private static final int BTN_X = 8, BTN_W = 44, BTN_H = 20, BTN_STEP = 26;
    /** 制作进度条 */
    private static final int PROGRESS_X = 56, PROGRESS_Y = 76, PROGRESS_W = 96, PROGRESS_H = 8;

    private static final int COLOR_BG = 0xFFC6C6C6;
    private static final int COLOR_LINE = 0xFF373737;
    private static final int COLOR_ENERGY = 0xFF28B428;
    private static final int COLOR_PROGRESS = 0xFFFFD030;
    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_SELECTED = 0xFFFFD030;

    public ChishiPotionTableScreen(ChishiPotionTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.fill(x, y, x + PANEL_W, y + PANEL_H, COLOR_BG);

        // 存储联动浮层打开时：隐藏主 UI 内容（避免双 UI 叠加），仅保留背景 + 存储按钮 + 浮层 + 背包/联动槽
        if (menu.linkState != null && menu.linkState.open) {
            drawStorageButton(gui, x, y);
            drawStorageOverlay(gui, x, y);
            for (int i = ChishiPotionTableBlockEntity.SLOT_COUNT; i < this.menu.slots.size(); i++) {
                var slot = this.menu.slots.get(i);
                if (!slot.isActive()) {
                    continue;
                }
                drawSlotBox(gui, x + slot.x, y + slot.y);
            }
            return;
        }

        gui.fill(x + 6, y + 14, x + PANEL_W - 6, y + PANEL_H - 7, 0xFFB0B0B0);

        // 能量条
        long energy = menu.getLifeEnergy();
        long cap = Math.max(1, menu.getLifeMax());
        int w = (int) (ENERGY_W * Math.max(0, Math.min(energy, cap)) / cap);
        gui.fill(x + ENERGY_X, y + ENERGY_Y, x + ENERGY_X + ENERGY_W, y + ENERGY_Y + ENERGY_H, COLOR_LINE);
        if (w > 0) {
            gui.fill(x + ENERGY_X, y + ENERGY_Y, x + ENERGY_X + w, y + ENERGY_Y + ENERGY_H, COLOR_ENERGY);
        }

        // 制作进度条
        int p = (int) (PROGRESS_W * menu.getProgress() / 100.0F);
        gui.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + PROGRESS_W, y + PROGRESS_Y + PROGRESS_H, COLOR_LINE);
        if (p > 0) {
            gui.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + p, y + PROGRESS_Y + PROGRESS_H, COLOR_PROGRESS);
        }

        // 模板按钮（含名称 + 消耗），选中高亮
        List<PotionTemplate> templates = PotionRegistry.all();
        for (int i = 0; i < templates.size(); i++) {
            PotionTemplate t = templates.get(i);
            int by = y + BTN_Y(i);
            boolean selected = menu.getSelectedIndex() == i;
            gui.fill(x + BTN_X, by, x + BTN_X + BTN_W, by + BTN_H, selected ? 0xFF5B8731 : 0xFF8B8B8B);
            gui.drawString(this.font, Component.translatable(t.nameKey()), x + BTN_X + 4, by + 3,
                    selected ? 0xFF2E7D32 : 0xFF3F3F3F, false);
            gui.drawString(this.font, Component.translatable("gui.template_mod.potion_table.cost",
                            t.solidCost(), t.lifeCost() / 1000),
                    x + BTN_X + 4, by + 12, 0xFF6F6F6F, false);
        }

        // 槽位背景框（样本/固态/输出 + 背包 + 联动槽仅激活时）
        for (var slot : this.menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            drawSlotBox(gui, x + slot.x, y + slot.y);
        }

        // 存储联动：按钮 + 浮层
        if (menu.linkState != null) {
            drawStorageButton(gui, x, y);
            if (menu.linkState.open) {
                drawStorageOverlay(gui, x, y);
            }
        }
    }

    private void drawStorageButton(GuiGraphics gui, int x, int y) {
        boolean open = menu.linkState.open;
        gui.fill(x + PANEL_W - 40, y + 6, x + PANEL_W - 8, y + 16, open ? 0xFF5B8731 : 0xFFB0B0B0);
        gui.fill(x + PANEL_W - 40, y + 6, x + PANEL_W - 8, y + 7, COLOR_LINE);
        gui.drawString(this.font, Component.translatable("gui.template_mod.storage_link.open"),
                x + PANEL_W - 38, y + 7, open ? 0xFF2E7D32 : 0xFF3F3F3F, false);
    }

    private void drawStorageOverlay(GuiGraphics gui, int x, int y) {
        gui.fill(x + 4, y + 8, x + PANEL_W - 4, y + 72, COLOR_BG);
        gui.fill(x + 5, y + 9, x + PANEL_W - 5, y + 71, 0xFFA5A5A5);
        gui.drawString(this.font, Component.translatable(menu.linkState.nameKey), x + 8, y + 12, 0xFF3F3F3F, false);
        int pages = StorageLink.pageCount(menu.linkState);
        gui.drawString(this.font, (menu.linkState.page + 1) + "/" + pages, x + 8, y + 64, 0xFF3F3F3F, false);
        gui.drawString(this.font, "\u25C0", x + 112, y + 64,
                menu.linkState.canPagePrev() ? 0xFF2E7D32 : 0xFF8B8B8B, false);
        gui.drawString(this.font, "\u25B6", x + 130, y + 64,
                menu.linkState.canPageNext() ? 0xFF2E7D32 : 0xFF8B8B8B, false);
    }

    private static int BTN_Y(int index) {
        return 30 + index * BTN_STEP;
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
        // 能量数值经能量条悬停提示展示（原常驻文本与模板按钮重叠）
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 能量条悬停提示（数值信息）
        if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.life", menu.getLifeEnergy(), menu.getLifeMax()),
                    mouseX, mouseY);
        }
        // 模板按钮悬停：显示功效描述（存储浮层打开时其位置被覆盖，跳过）
        if (menu.linkState == null || !menu.linkState.open) {
            List<PotionTemplate> templates = PotionRegistry.all();
            for (int i = 0; i < templates.size(); i++) {
                int by = this.topPos + BTN_Y(i);
                if (mouseX >= this.leftPos + BTN_X && mouseX < this.leftPos + BTN_X + BTN_W
                        && mouseY >= by && mouseY < by + BTN_H) {
                    PotionTemplate t = templates.get(i);
                    Component desc = t.breakthrough()
                            ? Component.translatable("gui.template_mod.potion_table.breakthrough_desc")
                            : Component.translatable("gui.template_mod.potion_table.permanent_desc");
                    gui.renderTooltip(this.font, desc, mouseX, mouseY);
                }
            }
        }
        // 存储按钮悬停提示
        if (menu.linkState != null
                && mouseX >= this.leftPos + PANEL_W - 40 && mouseX < this.leftPos + PANEL_W - 8
                && mouseY >= this.topPos + 6 && mouseY < this.topPos + 16) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.storage_link.tip",
                            Component.translatable(menu.linkState.nameKey)), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && menu.linkState != null) {
            if (mouseX >= this.leftPos + PANEL_W - 40 && mouseX < this.leftPos + PANEL_W - 8
                    && mouseY >= this.topPos + 6 && mouseY < this.topPos + 16) {
                menu.linkState.open = !menu.linkState.open;
                return true;
            }
            if (menu.linkState.open) {
                if (mouseX >= this.leftPos + 112 && mouseX < this.leftPos + 126
                        && mouseY >= this.topPos + 64 && mouseY < this.topPos + 74) {
                    menu.linkState.flip(-1);
                    return true;
                }
                if (mouseX >= this.leftPos + 130 && mouseX < this.leftPos + 144
                        && mouseY >= this.topPos + 64 && mouseY < this.topPos + 74) {
                    menu.linkState.flip(1);
                    return true;
                }
                // 浮层打开：让位给浮层槽位点击
                return super.mouseClicked(mouseX, mouseY, button);
            }
        }
        if (button == 0 && menu.getBlockPos() != null) {
            List<PotionTemplate> templates = PotionRegistry.all();
            for (int i = 0; i < templates.size(); i++) {
                int by = this.topPos + BTN_Y(i);
                if (mouseX >= this.leftPos + BTN_X && mouseX < this.leftPos + BTN_X + BTN_W
                        && mouseY >= by && mouseY < by + BTN_H) {
                    ChishiPotionSync.sendSelect(menu.getBlockPos(), i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
