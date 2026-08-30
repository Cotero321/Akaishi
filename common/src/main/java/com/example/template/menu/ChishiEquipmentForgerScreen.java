package com.example.template.menu;

import com.example.template.TemplateMod;
import com.example.template.block.entity.ChishiEquipmentForgerBlockEntity;
import com.example.template.item.ChishiUpgradeHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 赤石装备打造器界面：
 * 顶部能量条 + 中部输入/输出槽 + 剩余升级点 + 5 个属性分配按钮（Shift 点击撤销）+ 锻造按钮。
 * 升级点为可选，点击锻造按钮才执行锻造；更换输入装备或取走产物后升级点自动重置。
 */
public class ChishiEquipmentForgerScreen extends AbstractContainerScreen<ChishiEquipmentForgerMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_energy_cell.png");

    private static final int BAR_X = 20, BAR_Y = 16, BAR_W = 136, BAR_H = 8;
    /** 属性分配按钮区（最多 6 个横向排列，效率按钮仅挖掘工具显示） */
    private static final int ATTR_BTN_Y = 56;
    private static final int ATTR_BTN_W = 18;
    private static final int ATTR_BTN_H = 16;
    private static final int ATTR_BTN_GAP = 1;
    /** 锻造按钮（右侧） */
    private static final int FORGE_BTN_X = 136;
    private static final int FORGE_BTN_Y = 56;
    private static final int FORGE_BTN_W = 38;
    private static final int FORGE_BTN_H = 16;

    /** 该升级按钮是否显示：效率按钮仅对挖掘类工具（铲/斧/镐）显示 */
    private boolean showUpgradeButton(int i) {
        return i != ChishiUpgradeHelper.UpgradeType.EFFICIENCY.ordinal()
                || ChishiUpgradeHelper.isEfficiencyTool(menu.getGearItem());
    }

    public ChishiEquipmentForgerScreen(ChishiEquipmentForgerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 赤能源条（红色）
        GuiWidgets.track(gui, x + BAR_X, y + BAR_Y, BAR_W, BAR_H);
        long max = Math.max(1, menu.getMaxEnergy());
        int energyWidth = (int) (BAR_W * Math.max(0, Math.min(menu.getEnergy(), menu.getMaxEnergy())) / max);
        if (energyWidth > 0) {
            gui.fill(x + BAR_X, y + BAR_Y, x + BAR_X + energyWidth, y + BAR_Y + BAR_H, 0xFFE03030);
        }

        // 输入/材料/输出槽位框（背景贴图复用能量单元，无槽位底框，自绘原版风格）
        GuiWidgets.slotBox(gui, x + 38, y + 30);
        GuiWidgets.slotBox(gui, x + 62, y + 30);
        GuiWidgets.slotBox(gui, x + 116, y + 30);

        // 属性分配按钮：有点数可分配时亮色，否则暗色；已选次数越多边框越亮（效率仅挖掘工具显示）
        for (int i = 0; i < ChishiUpgradeHelper.UpgradeType.values().length; i++) {
            if (!showUpgradeButton(i)) {
                continue;
            }
            int bx = x + attrBtnX(i);
            boolean enabled = menu.getUpgradePoints() > 0 && menu.getBaseCount(i) < ChishiUpgradeHelper.FORGE_UPGRADE_POINTS;
            int fill = enabled ? 0xFF6080E0 : 0xFF505050;
            int border = menu.getBaseCount(i) > 0 ? 0xFFFFD030 : 0xFF404040;
            fillButton(gui, bx, y + ATTR_BTN_Y, ATTR_BTN_W, ATTR_BTN_H, fill, border);
            // 按钮下方显示已选次数
            String count = String.valueOf(menu.getBaseCount(i));
            gui.drawCenteredString(this.font, count, bx + ATTR_BTN_W / 2, ATTR_BTN_Y + ATTR_BTN_H + 2,
                    menu.getBaseCount(i) > 0 ? 0xFFFFD030 : 0x808080);
        }
        // 锻造按钮（就绪绿色 / 未就绪灰色，点击触发锻造）
        boolean ready = menu.isForgeReady();
        fillButton(gui, x + FORGE_BTN_X, y + FORGE_BTN_Y, FORGE_BTN_W, FORGE_BTN_H,
                ready ? 0xFF30A030 : 0xFF505050, ready ? 0xFFE0FFE0 : 0xFF404040);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF3F3F3F, false);

        // 剩余升级点
        gui.drawString(this.font,
                Component.translatable("gui.template_mod.forger.points", menu.getUpgradePoints()),
                20, 48, menu.getUpgradePoints() > 0 ? 0xFFFFD030 : 0xE0E0E0, false);

        // 属性按钮文字（按钮 key 与 UpgradeType 顺序一致，效率仅挖掘工具显示）
        ChishiUpgradeHelper.UpgradeType[] types = ChishiUpgradeHelper.UpgradeType.values();
        int attrTextY = ATTR_BTN_Y + (ATTR_BTN_H - 8) / 2;
        for (int i = 0; i < types.length; i++) {
            if (!showUpgradeButton(i)) {
                continue;
            }
            gui.drawCenteredString(this.font, Component.translatable(types[i].buttonKey), attrBtnX(i) + ATTR_BTN_W / 2,
                    attrTextY, 0xFFFFFF);
        }
        gui.drawCenteredString(this.font, Component.translatable("gui.template_mod.forger.forge"),
                FORGE_BTN_X + FORGE_BTN_W / 2, FORGE_BTN_Y + (FORGE_BTN_H - 8) / 2, 0xFFFFFF);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 能量条悬停提示（M/K 缩写）
        if (isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.energy",
                            EnergyFormat.format(menu.getEnergy()), EnergyFormat.format(menu.getMaxEnergy())),
                    mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int btnCount = ChishiUpgradeHelper.UpgradeType.values().length;
        if (button == 0 || button == 1) {
            for (int i = 0; i < btnCount; i++) {
                if (!showUpgradeButton(i)) {
                    continue;
                }
                int bx = this.leftPos + attrBtnX(i);
                if (mouseX >= bx && mouseX < bx + ATTR_BTN_W
                        && mouseY >= this.topPos + ATTR_BTN_Y && mouseY < this.topPos + ATTR_BTN_Y + ATTR_BTN_H) {
                    // Shift 或右键点击 = 撤销；否则分配
                    int id = hasShiftDown() || button == 1
                            ? ChishiEquipmentForgerMenu.BTN_REMOVE_OFFSET + i
                            : ChishiEquipmentForgerMenu.BTN_ADD_BASE + i;
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
                    return true;
                }
            }
            // 锻造按钮
            if (mouseX >= this.leftPos + FORGE_BTN_X && mouseX < this.leftPos + FORGE_BTN_X + FORGE_BTN_W
                    && mouseY >= this.topPos + FORGE_BTN_Y && mouseY < this.topPos + FORGE_BTN_Y + FORGE_BTN_H) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, ChishiEquipmentForgerMenu.BUTTON_FORGE);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 第 i 个属性按钮的 x 坐标 */
    private static int attrBtnX(int i) {
        return 20 + i * (ATTR_BTN_W + ATTR_BTN_GAP);
    }

    /** 填充矩形并绘制 1px 边框 */
    private static void fillButton(GuiGraphics gui, int x, int y, int w, int h, int fill, int border) {
        gui.fill(x, y, x + w, y + h, fill);
        gui.fill(x, y, x + w, y + 1, border);
        gui.fill(x, y + h - 1, x + w, y + h, border);
        gui.fill(x, y, x + 1, y + h, border);
        gui.fill(x + w - 1, y, x + w, y + h, border);
    }
}
