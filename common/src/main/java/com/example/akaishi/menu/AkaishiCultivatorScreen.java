package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiCultivatorBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * 部件培养舱界面：生命能量条（绿）+ 模式/进度显示 + 输入/材料槽。
 * 数据来自 {@link AkaishiCultivatorMenu} 的 ContainerData（模式 + 进度）。
 */
public class AkaishiCultivatorScreen extends AbstractContainerScreen<AkaishiCultivatorMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    private static final int PANEL_W = 176;

    private static final int LIFE_BAR_X = 20, LIFE_BAR_Y = 16, BAR_W = 136, BAR_H = 8;
    private static final int PROGRESS_X = 56, PROGRESS_Y = 74, PROGRESS_W = 56, PROGRESS_H = 8;
    /** 机器槽位数量（升级槽 2 + 输入/材料槽 2，贴图无槽位图形需自绘框） */
    private static final int MACHINE_SLOTS = 4;
    /** 升级槽 GUI 位置（与 Menu 槽位坐标一致，能量条下方右侧空位；标签置于槽位下方避开能量条） */
    private static final int SPEED_SLOT_X = 134, SPEED_SLOT_Y = 30;
    private static final int ENERGY_SLOT_X = 152, ENERGY_SLOT_Y = 30;

    public AkaishiCultivatorScreen(AkaishiCultivatorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

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

        GuiWidgets.track(gui, x + LIFE_BAR_X, y + LIFE_BAR_Y, BAR_W, BAR_H);
        long life = menu.getLifeEnergy();
        long cap = Math.max(1, menu.getLifeMax());
        int lifeWidth = (int) (BAR_W * Math.max(0, Math.min(life, cap)) / cap);
        if (lifeWidth > 0) {
            gui.fill(x + LIFE_BAR_X, y + LIFE_BAR_Y, x + LIFE_BAR_X + lifeWidth, y + LIFE_BAR_Y + BAR_H, 0xFF28B428);
        }
        // 机器槽位框（贴图无图形，自绘补齐）
        for (int i = 0; i < MACHINE_SLOTS; i++) {
            var slot = menu.slots.get(i);
            GuiWidgets.slotBox(gui, x + slot.x, y + slot.y);
        }
        // 进度条（提纯绿色 / 升级金色；置于材料槽下方空档）
        int mode = menu.getMode();
        int color = mode == AkaishiCultivatorBlockEntity.MODE_UPGRADE ? 0xFFFFD030 : 0xFF40E0D0;
        GuiWidgets.track(gui, x + PROGRESS_X, y + PROGRESS_Y, PROGRESS_W, PROGRESS_H);
        int progressWidth = (int) (PROGRESS_W * menu.getProgress() / 100.0F);
        if (progressWidth > 0) {
            gui.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + progressWidth, y + PROGRESS_Y + PROGRESS_H, color);
        }
        // 升级槽标签（槽位下方，避开上方能量条）
        gui.drawString(this.font, Component.translatable("gui.akaishi.upgrade.tag"),
                x + SPEED_SLOT_X, y + SPEED_SLOT_Y + 18, 0xFF707070, false);
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

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF3F3F3F, false);
        // 存储浮层打开时：隐藏模式标签（其位置被浮层覆盖）
        if (menu.linkState != null && menu.linkState.open) {
            return;
        }
        // 模式标签
        int mode = menu.getMode();
        Component modeText = switch (mode) {
            case AkaishiCultivatorBlockEntity.MODE_PURIFY ->
                    Component.translatable("gui.akaishi.cultivator.purify");
            case AkaishiCultivatorBlockEntity.MODE_UPGRADE ->
                    Component.translatable("gui.akaishi.cultivator.upgrade");
            default -> Component.translatable("gui.akaishi.cultivator.idle");
        };
        gui.drawString(this.font, modeText, 8, 74, 0xE0E0E0, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && menu.linkState != null) {
            // 存储开关按钮
            if (mouseX >= this.leftPos + PANEL_W - 40 && mouseX < this.leftPos + PANEL_W - 8
                    && mouseY >= this.topPos + 6 && mouseY < this.topPos + 16) {
                menu.linkState.open = !menu.linkState.open;
                return true;
            }
            if (menu.linkState.open) {
                // 浮层翻页
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
                // 浮层打开：其余点击交给槽位处理（联动槽优先）
                return super.mouseClicked(mouseX, mouseY, button);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 存储浮层打开时：隐藏主 UI 能量数值与能量/进度悬停提示，避免叠在浮层上
        if (menu.linkState != null && menu.linkState.open) {
            return;
        }

        // 生命能量数值随能量条悬停提示展示（原常驻文本 y+6 与标题重叠，已移除）

        if (isHovering(LIFE_BAR_X, LIFE_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.life",
                            formatEnergy(menu.getLifeEnergy()), formatEnergy(menu.getLifeMax())),
                    mouseX, mouseY);
        }
        if (isHovering(PROGRESS_X, PROGRESS_Y, PROGRESS_W, PROGRESS_H, mouseX, mouseY)) {
            int mode = menu.getMode();
            int rate = menu.getSuccessRate();
            Component tip = mode == AkaishiCultivatorBlockEntity.MODE_PURIFY
                    ? Component.translatable("gui.akaishi.cultivator.purify_tip", menu.getProgress(), rate)
                    : Component.translatable("gui.akaishi.cultivator.upgrade_tip", menu.getProgress(), rate);
            gui.renderTooltip(this.font, tip, mouseX, mouseY);
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
