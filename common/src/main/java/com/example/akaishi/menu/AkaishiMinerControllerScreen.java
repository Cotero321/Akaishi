package com.example.akaishi.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 赤石矿机控制器界面（vanilla 灰色风格，198 高）：
 * - 赤能源条（红）
 * - 产物暂存 3×2（自绘槽框）
 * - 三类升级统计 + 挖矿进度条 + 结构成型状态
 * - 挖矿模式（正常/精准）双按钮：点击切换产出形式，选中按钮高亮
 * 悬停能量/进度条/模式按钮显示数值或说明提示。
 */
public class AkaishiMinerControllerScreen extends AbstractContainerScreen<AkaishiMinerControllerMenu> {

    private static final int TEXT = 0xFF3F3F3F;
    private static final int GREEN = 0xFF2E7D32;
    private static final int RED = 0xFFC62828;
    private static final int BAR_H = 8;

    /** 挖矿模式按钮区（右侧三行升级统计下方，两按钮并排，选中态高亮） */
    private static final int MODE_Y = 78;
    private static final int MODE_W = 30;
    private static final int MODE_H = 12;
    private static final int MODE_NORMAL_X = 88;
    private static final int MODE_PRECISE_X = 120;
    private static final int ACTIVE_BG = 0xFFB9B9B9;
    private static final int INACTIVE_TEXT = 0xFF707070;
    private static final int ACTIVE_TEXT = 0xFF1A1A1A;

    public AkaishiMinerControllerScreen(AkaishiMinerControllerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
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
        return String.format(java.util.Locale.ROOT, "%.1f", d);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        // 不透明背景面板（修复此前透明背景）
        GuiWidgets.panel(gui, x, y, this.imageWidth, this.imageHeight);
        // 产物暂存 3×2
        int[] cols = {26, 44, 62};
        int[] rows = {40, 58};
        for (int r : rows) {
            for (int c : cols) {
                GuiWidgets.slotBox(gui, x + c, y + r);
            }
        }
        // 玩家背包 + 快捷栏槽框
        GuiWidgets.playerInventory(gui, x, y);
        // 能量条
        GuiWidgets.track(gui, x + 20, y + 22, 136, BAR_H);
        long max = Math.max(1, menu.getCapacity());
        int width = (int) (136L * Math.max(0, Math.min(menu.getEnergy(), max)) / max);
        if (width > 0) {
            gui.fill(x + 20, y + 22, x + 20 + width, y + 22 + BAR_H, 0xFFE03030);
        }
        // 挖矿进度条
        GuiWidgets.track(gui, x + 26, y + 92, 90, BAR_H);
        long required = Math.max(1, menu.getRequired());
        int pWidth = (int) (90 * Math.min(menu.getProgress(), required) / required);
        if (pWidth > 0) {
            gui.fill(x + 26, y + 92, x + 26 + pWidth, y + 92 + BAR_H, 0xFFFFD030);
        }
        // 挖矿模式双按钮：当前模式高亮（凸起），另一枚为普通灰按钮
        boolean precise = menu.isPrecise();
        modeButton(gui, x + MODE_NORMAL_X, y + MODE_Y, !precise);
        modeButton(gui, x + MODE_PRECISE_X, y + MODE_Y, precise);
    }

    /** 绘制一枚 30×12 模式按钮；active=true 时高亮（凸起）表示当前模式 */
    private static void modeButton(GuiGraphics gui, int x, int y, boolean active) {
        if (active) {
            gui.fill(x, y, x + MODE_W, y + MODE_H, ACTIVE_BG);
            gui.fill(x, y, x + MODE_W, y + 1, 0xFFFFFFFF);
            gui.fill(x, y, x + 1, y + MODE_H, 0xFFFFFFFF);
            gui.fill(x, y + MODE_H - 1, x + MODE_W, y + MODE_H, 0xFF373737);
            gui.fill(x + MODE_W - 1, y, x + MODE_W, y + MODE_H, 0xFF373737);
        } else {
            GuiWidgets.button(gui, x, y, MODE_W, MODE_H);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        // 产物暂存（左上，位于 3×2 槽上方）
        gui.drawString(this.font, Component.translatable("gui.akaishi.miner.output"),
                36, 32, 0xFF707070, false);
        // 升级统计（右侧空闲区竖向三行，行距 14px，避免三种语言文案互相压字）
        gui.drawString(this.font, Component.translatable("gui.akaishi.miner.speed", menu.getSpeedCount()),
                88, 40, 0xFF707070, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.miner.fortune", menu.getFortuneCount()),
                88, 54, 0xFF707070, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.miner.storage", menu.getStorageCount()),
                88, 68, 0xFF707070, false);
        // 成型状态（进度条下方，与玩家背包标题留足行距）
        if (menu.isFormed()) {
            gui.drawString(this.font, Component.translatable("gui.akaishi.miner.formed"), 26, 102, GREEN, false);
        } else {
            gui.drawString(this.font, Component.translatable("gui.akaishi.miner.not_formed"), 26, 102, RED, false);
        }
        // 挖矿模式按钮文字（中心对齐；当前模式深色，未选中浅灰；renderLabels 阶段已平移至 GUI 原点，用相对坐标）
        boolean precise = menu.isPrecise();
        gui.drawCenteredString(this.font, Component.translatable("gui.akaishi.miner.mode_normal"),
                MODE_NORMAL_X + MODE_W / 2, MODE_Y + 2,
                precise ? INACTIVE_TEXT : ACTIVE_TEXT);
        gui.drawCenteredString(this.font, Component.translatable("gui.akaishi.miner.mode_precise"),
                MODE_PRECISE_X + MODE_W / 2, MODE_Y + 2,
                precise ? ACTIVE_TEXT : INACTIVE_TEXT);
        // 玩家背包标题
        gui.drawString(this.font, Component.translatable("container.inventory"), 8, 114, TEXT, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        // 显式触发 tooltip（与主流机器 render 模板一致），保证悬停提示能显示
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = this.leftPos;
            int y = this.topPos;
            int mx = (int) mouseX;
            int my = (int) mouseY;
            if (mx >= x + MODE_NORMAL_X && mx < x + MODE_NORMAL_X + MODE_W
                    && my >= y + MODE_Y && my < y + MODE_Y + MODE_H) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
                        AkaishiMinerControllerMenu.BTN_MODE_NORMAL);
                return true;
            }
            if (mx >= x + MODE_PRECISE_X && mx < x + MODE_PRECISE_X + MODE_W
                    && my >= y + MODE_Y && my < y + MODE_Y + MODE_H) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
                        AkaishiMinerControllerMenu.BTN_MODE_PRECISE);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);
        int x = this.leftPos;
        int y = this.topPos;
        if (mouseX >= x + 20 && mouseX < x + 156 && mouseY >= y + 22 && mouseY < y + 30) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.energy",
                    formatEnergy(menu.getEnergy()), formatEnergy(menu.getCapacity())), mouseX, mouseY);
        } else if (mouseX >= x + 26 && mouseX < x + 116 && mouseY >= y + 92 && mouseY < y + 100) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.miner.progress",
                    menu.getProgress(), menu.getRequired()), mouseX, mouseY);
        } else if (mouseX >= x + MODE_NORMAL_X && mouseX < x + MODE_PRECISE_X + MODE_W
                && mouseY >= y + MODE_Y && mouseY < y + MODE_Y + MODE_H) {
            // 悬停在模式按钮区：按具体按钮给出模式说明
            boolean overPrecise = mouseX >= x + MODE_PRECISE_X;
            gui.renderTooltip(this.font, Component.translatable(overPrecise
                    ? "gui.akaishi.miner.mode_tip_precise"
                    : "gui.akaishi.miner.mode_tip_normal"), mouseX, mouseY);
        }
    }
}
