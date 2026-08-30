package com.example.template.menu;

import com.example.template.block.entity.ChishiSurgeryBlockEntity;
import com.example.template.life.body.BodySlot;
import com.example.template.life.body.ClientBodyData;
import com.example.template.life.organ.ChishiOrganItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 手术仓界面（自绘医学面板）：
 * - 左侧 3×3 躯体槽位：占用显示器官图标 + 排斥数值，点击选中目标（黄框）
 * - 右上器官输入槽 / 固态物槽，右侧「移植」「摘除」按钮（手术中禁用）
 * - 底部手术进度条；玩家躯体状态来自 ClientBodyData（PlayerBodySync S2C 推送）
 * 界面高 196：背包区自 y=118 起（与菜单槽位坐标一致）。
 */
public class ChishiSurgeryScreen extends AbstractContainerScreen<ChishiSurgeryMenu> {

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 196;

    /** 3×3 槽位面板 */
    private static final int GRID_X = 16;
    private static final int GRID_Y = 28;
    private static final int GRID_SPACING = 22;
    private static final int GRID_SIZE = 18;
    /** 能量条 */
    private static final int ENERGY_X = 16, ENERGY_Y = 16, ENERGY_W = 96, ENERGY_H = 8;
    /** 手术进度条 */
    private static final int PROGRESS_X = 16, PROGRESS_Y = 104, PROGRESS_W = 136, PROGRESS_H = 8;
    /** 移植/摘除按钮 */
    private static final int IMPLANT_X = 118, EXTRACT_X = 150, BTN_Y = 82, BTN_W = 28, BTN_H = 12;

    private static final int COLOR_BG = 0xFFC6C6C6;
    private static final int COLOR_PANEL = 0xFFB0B0B0;
    private static final int COLOR_LINE = 0xFF373737;
    private static final int COLOR_ENERGY = 0xFF28B428;
    private static final int COLOR_PROGRESS = 0xFFFFD030;
    private static final int COLOR_SELECTED = 0xFFFFD030;
    private static final int COLOR_SLOT = 0xFF8B8B8B;

    /** 本地选中的目标槽位索引 */
    private int selectedIndex;

    public ChishiSurgeryScreen(ChishiSurgeryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
    }

    // ===== 可用性判断 =====

    private boolean isOperating() {
        return menu.getOperation() != ChishiSurgeryBlockEntity.OP_NONE;
    }

    private BodySlot selectedSlot() {
        return BodySlot.values()[Math.max(0, Math.min(BodySlot.values().length - 1, selectedIndex))];
    }

    /** 移植按钮可用：目标槽位为空 + 输入槽器官匹配该槽位 + 资源足够 + 无手术进行中 */
    private boolean canImplant() {
        if (isOperating() || menu.getBlockPos() == null) {
            return false;
        }
        BodySlot target = selectedSlot();
        if (ClientBodyData.isOccupied(target)) {
            return false;
        }
        ItemStack organ = menu.getOrganInput();
        if (!(organ.getItem() instanceof ChishiOrganItem item) || item.slot != target) {
            return false;
        }
        return menu.getSolidCount() >= ChishiSurgeryBlockEntity.IMPLANT_SOLID_COST
                && menu.getLifeEnergy() >= ChishiSurgeryBlockEntity.IMPLANT_LIFE_COST;
    }

    /** 摘除按钮可用：目标槽位已占用 + 资源足够 + 无手术进行中 */
    private boolean canExtract() {
        if (isOperating() || menu.getBlockPos() == null) {
            return false;
        }
        if (!ClientBodyData.isOccupied(selectedSlot())) {
            return false;
        }
        return menu.getSolidCount() >= ChishiSurgeryBlockEntity.EXTRACT_SOLID_COST
                && menu.getLifeEnergy() >= ChishiSurgeryBlockEntity.EXTRACT_LIFE_COST;
    }

    // ===== 渲染 =====

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        // 背景
        gui.fill(x, y, x + PANEL_W, y + PANEL_H, COLOR_BG);

        // 存储联动浮层打开时：隐藏主 UI 内容（避免双 UI 叠加），保留存储界面与背包/联动槽
        if (menu.linkState != null && menu.linkState.open) {
            drawStorageButton(gui, x, y);
            drawStorageOverlay(gui, x, y);
            drawLinkedSlots(gui, x, y);
            return;
        }

        // 主面板
        gui.fill(x + 6, y + 14, x + PANEL_W - 6, y + 112, COLOR_PANEL);

        // 能量条
        long energy = menu.getLifeEnergy();
        long cap = Math.max(1, menu.getLifeMax());
        int w = (int) (ENERGY_W * Math.max(0, Math.min(energy, cap)) / cap);
        gui.fill(x + ENERGY_X, y + ENERGY_Y, x + ENERGY_X + ENERGY_W, y + ENERGY_Y + ENERGY_H, COLOR_LINE);
        if (w > 0) {
            gui.fill(x + ENERGY_X, y + ENERGY_Y, x + ENERGY_X + w, y + ENERGY_Y + ENERGY_H, COLOR_ENERGY);
        }

        // 手术进度条（手术中显示）
        if (isOperating()) {
            int p = (int) (PROGRESS_W * menu.getProgress() / 100.0F);
            gui.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + PROGRESS_W, y + PROGRESS_Y + PROGRESS_H, COLOR_LINE);
            if (p > 0) {
                gui.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + p, y + PROGRESS_Y + PROGRESS_H, COLOR_PROGRESS);
            }
        }

        // 3×3 躯体槽位格子
        BodySlot[] slots = BodySlot.values();
        for (int i = 0; i < slots.length; i++) {
            int col = i % 3;
            int row = i / 3;
            int gx = x + GRID_X + col * GRID_SPACING;
            int gy = y + GRID_Y + row * GRID_SPACING;
            drawSlotBox(gui, gx, gy);
            // 选中高亮边框
            if (i == selectedIndex) {
                gui.fill(gx - 1, gy - 1, gx + GRID_SIZE + 1, gy + GRID_SIZE + 1, COLOR_SELECTED);
                drawSlotBox(gui, gx, gy);
            }
            // 器官图标 + 排斥值
            ItemStack organ = ClientBodyData.getOrgan(slots[i]);
            if (!organ.isEmpty()) {
                gui.renderItem(organ, gx + 1, gy + 1);
            } else {
                gui.drawString(this.font, Component.translatable(slots[i].getNameKey()).getString().substring(0, 1),
                        gx + 6, gy + 5, 0xFF6F6F6F, false);
            }
            int rej = ClientBodyData.getRejection(slots[i]);
            gui.drawString(this.font, String.valueOf(rej), gx + GRID_SIZE - 1 - this.font.width(String.valueOf(rej)),
                    gy + GRID_SIZE - 9, rejectionColor(rej), false);
        }

        // 器官输入槽 / 固态物槽
        drawSlotBox(gui, x + 120, y + 28);
        drawSlotBox(gui, x + 120, y + 54);
        gui.drawString(this.font, Component.translatable("gui.template_mod.surgery.organ_in"),
                x + 138, y + 30, 0xFF3F3F3F, false);
        gui.drawString(this.font, Component.translatable("gui.template_mod.surgery.solid_in", menu.getSolidCount()),
                x + 138, y + 56, 0xFF3F3F3F, false);

        // 移植/摘除按钮
        drawButton(gui, x + IMPLANT_X, y + BTN_Y, "gui.template_mod.surgery.implant", canImplant());
        drawButton(gui, x + EXTRACT_X, y + BTN_Y, "gui.template_mod.surgery.extract", canExtract());

        // 背包槽位框（菜单槽位坐标；联动槽仅浮层打开时激活）
        drawLinkedSlots(gui, x, y);

        // 存储联动：按钮 + 浮层（浮层槽位由菜单注入的 LinkedVaultSlot 渲染在浮层区域内）
        if (menu.linkState != null) {
            drawStorageButton(gui, x, y);
        }
    }

    /** 绘制背包/联动槽位框（浮层打开时联动槽激活可见） */
    private void drawLinkedSlots(GuiGraphics gui, int x, int y) {
        for (int i = ChishiSurgeryBlockEntity.SLOT_COUNT; i < this.menu.slots.size(); i++) {
            var slot = this.menu.slots.get(i);
            if (i >= ChishiSurgeryBlockEntity.SLOT_COUNT + 36 && !slot.isActive()) {
                continue;
            }
            drawSlotBox(gui, x + slot.x, y + slot.y);
        }
    }

    /** 右上角"存储"开关按钮 */
    private void drawStorageButton(GuiGraphics gui, int x, int y) {
        boolean open = menu.linkState.open;
        gui.fill(x + PANEL_W - 40, y + 6, x + PANEL_W - 8, y + 16, open ? 0xFF5B8731 : 0xFFB0B0B0);
        gui.fill(x + PANEL_W - 40, y + 6, x + PANEL_W - 8, y + 7, COLOR_LINE);
        gui.drawString(this.font, Component.translatable("gui.template_mod.storage_link.open"),
                x + PANEL_W - 38, y + 7, open ? 0xFF2E7D32 : 0xFF3F3F3F, false);
    }

    /** 存储联动浮层：标题 + 页码 + 翻页按钮（槽位由 LinkedVaultSlot 自动渲染） */
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

    /** 画 18×18 原版风格槽位框（上左暗、下右亮） */
    private void drawSlotBox(GuiGraphics gui, int x, int y) {
        gui.fill(x, y, x + 18, y + 18, COLOR_SLOT);
        gui.fill(x, y, x + 18, y + 1, COLOR_LINE);
        gui.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        gui.fill(x, y, x + 1, y + 18, COLOR_LINE);
        gui.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
    }

    /** 画按钮（可用绿色文字 / 不可用灰色） */
    private void drawButton(GuiGraphics gui, int x, int y, String key, boolean enabled) {
        gui.fill(x, y, x + BTN_W, y + BTN_H, enabled ? 0xFF5B8731 : 0xFF9E9E9E);
        gui.fill(x, y, x + BTN_W, y + 1, enabled ? 0xFF2E7D32 : COLOR_LINE);
        Component label = Component.translatable(key);
        gui.drawString(this.font, label, x + (BTN_W - this.font.width(label)) / 2, y + 2,
                enabled ? 0xFF2E7D32 : 0xFF6F6F6F, false);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, 4, 0xFF3F3F3F, false);
        // 存储浮层打开时：隐藏手术状态文本（浮层区域覆盖主 UI）
        if (menu.linkState != null && menu.linkState.open) {
            return;
        }
        // 手术状态文本（进度条上方，避免与背包槽位重叠）
        if (isOperating()) {
            int target = menu.getTargetSlot();
            Component status = Component.translatable("gui.template_mod.surgery.operating",
                    Component.translatable(BodySlot.values()[Math.max(0, Math.min(BodySlot.values().length - 1, target))].getNameKey()));
            gui.drawString(this.font, status, this.titleLabelX, PROGRESS_Y - 10, 0xFF8B6F1E, false);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 存储按钮悬停提示（浮层打开时仍保留）
        if (menu.linkState != null
                && mouseX >= this.leftPos + PANEL_W - 40 && mouseX < this.leftPos + PANEL_W - 8
                && mouseY >= this.topPos + 6 && mouseY < this.topPos + 16) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.storage_link.tip",
                            Component.translatable(menu.linkState.nameKey)), mouseX, mouseY);
        }

        // 存储浮层打开时：隐藏主 UI 能量数值与悬停提示，避免叠在浮层上
        if (menu.linkState != null && menu.linkState.open) {
            return;
        }

        // 能量数值经能量条悬停提示展示（原常驻文本与标题重叠）

        // 悬停提示：能量条 / 槽位 / 按钮
        if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.life", menu.getLifeEnergy(), menu.getLifeMax()),
                    mouseX, mouseY);
        }
        BodySlot[] slots = BodySlot.values();
        for (int i = 0; i < slots.length; i++) {
            int col = i % 3;
            int row = i / 3;
            int gx = this.leftPos + GRID_X + col * GRID_SPACING;
            int gy = this.topPos + GRID_Y + row * GRID_SPACING;
            if (mouseX >= gx && mouseX < gx + GRID_SIZE && mouseY >= gy && mouseY < gy + GRID_SIZE) {
                Component tip = Component.translatable(slots[i].getNameKey())
                        .append(Component.literal(" "))
                        .append(Component.translatable("gui.template_mod.surgery.rejection",
                                ClientBodyData.getRejection(slots[i])));
                gui.renderTooltip(this.font, tip, mouseX, mouseY);
            }
        }
        if (mouseX >= this.leftPos + IMPLANT_X && mouseX < this.leftPos + IMPLANT_X + BTN_W
                && mouseY >= this.topPos + BTN_Y && mouseY < this.topPos + BTN_Y + BTN_H) {
            gui.renderTooltip(this.font, Component.translatable("gui.template_mod.surgery.implant_tip",
                    ChishiSurgeryBlockEntity.IMPLANT_SOLID_COST, ChishiSurgeryBlockEntity.IMPLANT_LIFE_COST),
                    mouseX, mouseY);
        }
        if (mouseX >= this.leftPos + EXTRACT_X && mouseX < this.leftPos + EXTRACT_X + BTN_W
                && mouseY >= this.topPos + BTN_Y && mouseY < this.topPos + BTN_Y + BTN_H) {
            gui.renderTooltip(this.font, Component.translatable("gui.template_mod.surgery.extract_tip",
                    ChishiSurgeryBlockEntity.EXTRACT_SOLID_COST, ChishiSurgeryBlockEntity.EXTRACT_LIFE_COST),
                    mouseX, mouseY);
        }
    }

    // ===== 交互 =====

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // 存储联动：开关按钮 / 浮层翻页（浮层打开时优先，避免与躯体槽点击冲突）
            if (menu.linkState != null) {
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
                    // 浮层打开：躯体槽位选择与手术按钮让位给浮层槽位（落入 super 处理）
                    return super.mouseClicked(mouseX, mouseY, button);
                }
            }
            // 点击 3×3 槽位 → 选中目标
            for (int i = 0; i < BodySlot.values().length; i++) {
                int col = i % 3;
                int row = i / 3;
                int gx = this.leftPos + GRID_X + col * GRID_SPACING;
                int gy = this.topPos + GRID_Y + row * GRID_SPACING;
                if (mouseX >= gx && mouseX < gx + GRID_SIZE && mouseY >= gy && mouseY < gy + GRID_SIZE) {
                    selectedIndex = i;
                    return true;
                }
            }
            // 移植 / 摘除按钮
            if (menu.getBlockPos() != null) {
                if (mouseX >= this.leftPos + IMPLANT_X && mouseX < this.leftPos + IMPLANT_X + BTN_W
                        && mouseY >= this.topPos + BTN_Y && mouseY < this.topPos + BTN_Y + BTN_H) {
                    if (canImplant()) {
                        ChishiSurgerySync.sendStart(menu.getBlockPos(), ChishiSurgeryBlockEntity.OP_IMPLANT, selectedIndex);
                    }
                    return true;
                }
                if (mouseX >= this.leftPos + EXTRACT_X && mouseX < this.leftPos + EXTRACT_X + BTN_W
                        && mouseY >= this.topPos + BTN_Y && mouseY < this.topPos + BTN_Y + BTN_H) {
                    if (canExtract()) {
                        ChishiSurgerySync.sendStart(menu.getBlockPos(), ChishiSurgeryBlockEntity.OP_EXTRACT, selectedIndex);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 排斥值 → 颜色 */
    private int rejectionColor(int rej) {
        if (rej >= 100) return 0xFFD64545;
        if (rej >= 60) return 0xFFE08A3A;
        if (rej >= 30) return 0xFF8B6F1E;
        return 0xFF2E7D32;
    }
}
