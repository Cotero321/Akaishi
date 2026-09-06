package com.example.akaishi.menu;

import com.example.akaishi.life.potion.PotionRegistry;
import com.example.akaishi.life.potion.PotionTemplate;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * 药剂库界面：顶部模板筛选按钮（全部/永久/突破，客户端本地切换），
 * 下方 54 格药剂槽——不匹配筛选的槽位隐藏（isActive 控制渲染与交互）。
 */
public class AkaishiPotionCabinetScreen extends AbstractContainerScreen<AkaishiPotionCabinetMenu> {

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 220;
    private static final int BTN_Y = 6, BTN_H = 10, BTN_GAP = 4;

    private static final int COLOR_BG = 0xFFC6C6C6;
    private static final int COLOR_LINE = 0xFF373737;
    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_SLOT_HIDDEN = 0xFF9E9E9E;
    private static final int COLOR_SELECTED = 0xFF5B8731;

    public AkaishiPotionCabinetScreen(AkaishiPotionCabinetMenu menu, Inventory inv, Component title) {
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

        // 筛选按钮：全部 + 各模板（超宽自动换行），选中高亮
        String filter = menu.getFilterTemplate();
        List<PotionTemplate> templates = PotionRegistry.all();
        int bx = 8;
        int by = y;
        for (int i = 0; i < templates.size() + 1; i++) {
            String id = i == 0 ? "" : templates.get(i - 1).id();
            String nameKey = filterNameKey(id);
            int w = this.font.width(Component.translatable(nameKey)) + 6;
            if (bx + w + BTN_GAP > PANEL_W - 6) {
                bx = 8;
                by += BTN_H + BTN_GAP;
            }
            drawFilterButton(gui, x + bx, by, w, id, nameKey, filter);
            bx += w + BTN_GAP;
        }

        // 槽位背景框（隐藏槽画暗色）
        for (var slot : this.menu.slots) {
            boolean active = slot.isActive();
            drawSlotBox(gui, x + slot.x, y + slot.y, active ? COLOR_SLOT : COLOR_SLOT_HIDDEN);
        }
    }

    /** 筛选按钮短标签（模板一多，行长名称会把按钮挤出面板，改用 2 字级标签） */
    private static String filterNameKey(String templateId) {
        return switch (templateId) {
            case "permanent" -> "gui.akaishi.potion_cabinet.f_permanent";
            case "balance" -> "gui.akaishi.potion_cabinet.f_balance";
            case "surge" -> "gui.akaishi.potion_cabinet.f_surge";
            case "endure" -> "gui.akaishi.potion_cabinet.f_endure";
            default -> "gui.akaishi.potion_cabinet.all";
        };
    }

    /** 绘制一个筛选按钮（w 由调用方按文本宽度算出，便于参与换行排版） */
    private void drawFilterButton(GuiGraphics gui, int x, int y, int w, String id, String nameKey, String currentFilter) {
        boolean selected = currentFilter.equals(id);
        gui.fill(x, y + BTN_Y, x + w, y + BTN_Y + BTN_H, selected ? COLOR_SELECTED : 0xFFB0B0B0);
        gui.drawString(this.font, Component.translatable(nameKey), x + 3, y + BTN_Y + 1,
                selected ? 0xFF2E7D32 : 0xFF3F3F3F, false);
    }

    private void drawSlotBox(GuiGraphics gui, int x, int y, int color) {
        gui.fill(x, y, x + 18, y + 18, color);
        gui.fill(x, y, x + 18, y + 1, COLOR_LINE);
        gui.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        gui.fill(x, y, x + 1, y + 18, COLOR_LINE);
        gui.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // 顶部为筛选按钮行，取消标题绘制避免与按钮重叠（库名经浮层/容器名可见）
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // 命中筛选按钮（与 renderBg 一致的换行排版）→ 切换筛选
            List<PotionTemplate> templates = PotionRegistry.all();
            int bx = 8;
            int by = this.topPos;
            for (int i = 0; i < templates.size() + 1; i++) {
                String id = i == 0 ? "" : templates.get(i - 1).id();
                String nameKey = filterNameKey(id);
                int w = this.font.width(Component.translatable(nameKey)) + 6;
                if (bx + w + BTN_GAP > PANEL_W - 6) {
                    bx = 8;
                    by += BTN_H + BTN_GAP;
                }
                int x0 = this.leftPos + bx;
                int y0 = by + BTN_Y;
                if (mouseX >= x0 && mouseX < x0 + w && mouseY >= y0 && mouseY < y0 + BTN_H) {
                    menu.setFilter(menu.getFilterTemplate().equals(id) ? "" : id);
                    return true;
                }
                bx += w + BTN_GAP;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
