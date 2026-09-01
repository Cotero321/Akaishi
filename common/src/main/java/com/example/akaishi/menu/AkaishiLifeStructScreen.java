package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.life.body.BodySlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;

/**
 * 生命结构台界面：
 * - 生命能量条（绿）+ 构造进度条（黄）
 * - 3×3 目标槽位选择区：可用槽位绿框、选中槽位黄框、不可用灰框，点击发送 C2S 包
 * - 顶部显示当前基因序列的来源生物
 * 数据来自 {@link AkaishiLifeStructMenu} 的 ContainerData。
 */
public class AkaishiLifeStructScreen extends AbstractContainerScreen<AkaishiLifeStructMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_life_struct.png");

    /** 生命能量条区域（对齐贴图烘焙条框 y=24..32，避免双条重叠） */
    private static final int LIFE_BAR_X = 20, LIFE_BAR_Y = 24, BAR_W = 136, BAR_H = 8;
    /** 构造进度条区域（位于按钮区下方空档） */
    private static final int PROGRESS_X = 34, PROGRESS_Y = 112, PROGRESS_W = 80, PROGRESS_H = 8;
    /** 目标槽位按钮区（3×3，每个 16×16；槽位行 y=30..48 之下） */
    private static final int BTN_X = 34, BTN_Y = 52, BTN_SPACING = 20, BTN_SIZE = 16;
    /** 机器槽位数量（输入 + 材料 + 输出，贴图无槽位图形需自绘框） */
    private static final int MACHINE_SLOTS = 3;
    /** 升级槽 GUI 位置（与 Menu 槽位坐标一致，按钮区右侧空位；标签置于槽位上方） */
    private static final int SPEED_SLOT_X = 134, SPEED_SLOT_Y = 56;
    private static final int ENERGY_SLOT_X = 152, ENERGY_SLOT_Y = 56;

    private static final int COLOR_AVAILABLE = 0xFF28B428;
    private static final int COLOR_SELECTED = 0xFFFFD030;
    private static final int COLOR_LOCKED = 0xFF606060;

    public AkaishiLifeStructScreen(AkaishiLifeStructMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        // 贴图快捷栏烘焙在 y180..197（菜单快捷栏槽 y180），窗口高度须含快捷栏，否则被裁剪隐形
        this.imageHeight = 198;
    }

    /** 大数值缩写：>=1M 百万，>=1K 千，否则原样输出 */
    private static String formatEnergy(long v) {
        if (v >= 1_000_000L) {
            return trim(v / 1.0e6) + "M";
        }
        if (v >= 1_000L) {
            return trim(v / 1.0e3) + "K";
        }
        return String.valueOf(v);
    }

    private static String trim(double d) {
        if (Math.abs(d - Math.round(d)) < 0.05) {
            return String.valueOf((long) Math.round(d));
        }
        return String.format(Locale.ROOT, "%.1f", d);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 存储联动浮层打开时：隐藏主 UI 内容（避免双 UI 叠加），仅保留背景 + 存储按钮 + 浮层
        if (menu.linkState != null && menu.linkState.open) {
            drawStorageButton(gui, x, y);
            drawStorageOverlay(gui, x, y);
            return;
        }

        // 机器槽位框（贴图无图形，自绘补齐）
        for (int i = 0; i < MACHINE_SLOTS; i++) {
            var slot = menu.slots.get(i);
            GuiWidgets.slotBox(gui, x + slot.x, y + slot.y);
        }
        // 生命能量条（绿）
        GuiWidgets.track(gui, x + LIFE_BAR_X, y + LIFE_BAR_Y, BAR_W, BAR_H);
        long life = menu.getLifeEnergy();
        long cap = Math.max(1, menu.getLifeMax());
        int lifeWidth = (int) (BAR_W * Math.max(0, Math.min(life, cap)) / cap);
        if (lifeWidth > 0) {
            gui.fill(x + LIFE_BAR_X, y + LIFE_BAR_Y, x + LIFE_BAR_X + lifeWidth, y + LIFE_BAR_Y + BAR_H, 0xFF28B428);
        }
        // 构造进度条（黄）
        GuiWidgets.track(gui, x + PROGRESS_X, y + PROGRESS_Y, PROGRESS_W, PROGRESS_H);
        int progressWidth = (int) (PROGRESS_W * menu.getProgress() / 100.0F);
        if (progressWidth > 0) {
            gui.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + progressWidth, y + PROGRESS_Y + PROGRESS_H, 0xFFFFD030);
        }
        // 目标槽位选择区（3×3）
        List<BodySlot> available = menu.getAvailableSlots();
        int selected = menu.getTargetSlot();
        BodySlot[] slots = BodySlot.values();
        for (int i = 0; i < slots.length; i++) {
            int col = i % 3;
            int row = i / 3;
            int bx = x + BTN_X + col * BTN_SPACING;
            int by = y + BTN_Y + row * BTN_SPACING;
            boolean usable = available.contains(slots[i]);
            int color = i == selected ? COLOR_SELECTED : usable ? COLOR_AVAILABLE : COLOR_LOCKED;
            // 外框（选中槽位 2px 高亮，其余 1px）
            gui.fill(bx - 1, by - 1, bx + BTN_SIZE + 1, by + BTN_SIZE + 1, color);
            gui.fill(bx, by, bx + BTN_SIZE, by + BTN_SIZE, 0xFF8B8B8B);
        }
        // 升级槽（速度/能量，纹理无图案需自绘框 + 槽位上方标签）
        GuiWidgets.slotBox(gui, x + SPEED_SLOT_X, y + SPEED_SLOT_Y);
        GuiWidgets.slotBox(gui, x + ENERGY_SLOT_X, y + ENERGY_SLOT_Y);
        gui.drawString(this.font, Component.translatable("gui.akaishi.upgrade.tag"),
                x + SPEED_SLOT_X, y + SPEED_SLOT_Y - 9, 0xFF707070, false);
    }

    /** 右上角"存储"开关按钮 */
    private void drawStorageButton(GuiGraphics gui, int x, int y) {
        boolean open = menu.linkState.open;
        gui.fill(x + PANEL_W - 40, y + 6, x + PANEL_W - 8, y + 16, open ? 0xFF5B8731 : 0xFFB0B0B0);
        gui.drawString(this.font, Component.translatable("gui.akaishi.storage_link.open"),
                x + PANEL_W - 38, y + 7, open ? 0xFF2E7D32 : 0xFF3F3F3F, false);
    }

    /** 存储联动浮层：标题 + 18 槽位框 + 页码 + 翻页按钮 */
    private void drawStorageOverlay(GuiGraphics gui, int x, int y) {
        gui.fill(x + 4, y + 8, x + PANEL_W - 4, y + 72, 0xFFC6C6C6);
        gui.fill(x + 5, y + 9, x + PANEL_W - 5, y + 71, 0xFFA5A5A5);
        gui.drawString(this.font, Component.translatable(menu.linkState.nameKey), x + 8, y + 12, 0xFF3F3F3F, false);
        // 槽位框（两行九列）
        for (int i = 0; i < StorageLink.PAGE_SLOTS; i++) {
            int sx = x + StorageLink.SLOT_X + (i % 9) * 18;
            int sy = y + StorageLink.SLOT_Y + (i / 9) * 18;
            gui.fill(sx, sy, sx + 18, sy + 18, 0xFF8B8B8B);
            gui.fill(sx, sy, sx + 18, sy + 1, 0xFF373737);
            gui.fill(sx, sy + 17, sx + 18, sy + 18, 0xFF373737);
            gui.fill(sx, sy, sx + 1, sy + 18, 0xFF373737);
            gui.fill(sx + 17, sy, sx + 18, sy + 18, 0xFF373737);
        }
        int pages = StorageLink.pageCount(menu.linkState);
        gui.drawString(this.font, (menu.linkState.page + 1) + "/" + pages, x + 8, y + 64, 0xFF3F3F3F, false);
        gui.drawString(this.font, "\u25C0", x + 112, y + 64,
                menu.linkState.canPagePrev() ? 0xFF2E7D32 : 0xFF8B8B8B, false);
        gui.drawString(this.font, "\u25B6", x + 130, y + 64,
                menu.linkState.canPageNext() ? 0xFF2E7D32 : 0xFF8B8B8B, false);
    }

    private static final int PANEL_W = 176;

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF3F3F3F, false);
        // 基因来源/消耗信息移至进度条悬停提示（原常驻文本与能量条/槽位重叠）
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
                return super.mouseClicked(mouseX, mouseY, button);
            }
        }
        if (button == 0 && menu.getBlockPos() != null) {
            // 命中 3×3 槽位按钮 → 发送目标槽位选择
            for (int i = 0; i < BodySlot.values().length; i++) {
                int col = i % 3;
                int row = i / 3;
                int bx = this.leftPos + BTN_X + col * BTN_SPACING;
                int by = this.topPos + BTN_Y + row * BTN_SPACING;
                if (mouseX >= bx - 1 && mouseX < bx + BTN_SIZE + 1
                        && mouseY >= by - 1 && mouseY < by + BTN_SIZE + 1) {
                    AkaishiLifeStructSync.sendSelect(menu.getBlockPos(), i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 存储浮层打开时：隐藏主 UI 能量数值与能量/进度/槽位悬停提示，避免叠在浮层上
        if (menu.linkState != null && menu.linkState.open) {
            return;
        }

        // 生命能量数值随能量条悬停提示展示（原常驻文本 y+6 与标题重叠，已移除）

        // 悬停提示：能量条 / 进度条 / 槽位按钮
        if (isHovering(LIFE_BAR_X, LIFE_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.life",
                            formatEnergy(menu.getLifeEnergy()), formatEnergy(menu.getLifeMax())),
                    mouseX, mouseY);
        }
        if (isHovering(PROGRESS_X, PROGRESS_Y, PROGRESS_W, PROGRESS_H, mouseX, mouseY)) {
            // 进度 + 基因来源 + 消耗合并为多行悬停提示
            List<Component> tip = new java.util.ArrayList<>();
            tip.add(Component.translatable("gui.akaishi.life_struct.progress", menu.getProgress()));
            String entityId = menu.getSequenceEntityId();
            if (entityId != null) {
                tip.add(Component.translatable("gui.akaishi.life_struct.gene",
                        Component.translatable("entity." + entityId.replace(':', '.'))));
            }
            tip.add(Component.translatable("gui.akaishi.life_struct.cost"));
            gui.renderComponentTooltip(this.font, tip, mouseX, mouseY);
        }
        // 槽位按钮悬停提示（含可用/选中状态）
        BodySlot[] slots = BodySlot.values();
        for (int i = 0; i < slots.length; i++) {
            int col = i % 3;
            int row = i / 3;
            int bx = this.leftPos + BTN_X + col * BTN_SPACING;
            int by = this.topPos + BTN_Y + row * BTN_SPACING;
            if (mouseX >= bx - 1 && mouseX < bx + BTN_SIZE + 1
                    && mouseY >= by - 1 && mouseY < by + BTN_SIZE + 1) {
                Component tip = Component.translatable(slots[i].getNameKey());
                if (i == menu.getTargetSlot()) {
                    tip = Component.translatable("gui.akaishi.life_struct.selected", tip);
                } else if (!menu.getAvailableSlots().contains(slots[i])) {
                    tip = Component.translatable("gui.akaishi.life_struct.unavailable", tip);
                }
                gui.renderTooltip(this.font, tip, mouseX, mouseY);
            }
        }
        // 存储按钮悬停提示
        if (menu.linkState != null
                && mouseX >= this.leftPos + PANEL_W - 40 && mouseX < this.leftPos + PANEL_W - 8
                && mouseY >= this.topPos + 6 && mouseY < this.topPos + 16) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.storage_link.tip",
                            Component.translatable(menu.linkState.nameKey)), mouseX, mouseY);
        }
        // 升级槽悬停提示（速度/能量倍率随组件数量提升）
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
