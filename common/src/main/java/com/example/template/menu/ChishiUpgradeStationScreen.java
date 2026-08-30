package com.example.template.menu;

import com.example.template.TemplateMod;
import com.example.template.block.entity.ChishiUpgradeStationBlockEntity;
import com.example.template.item.ChishiUpgradeHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 赤红升级台界面：顶部能量条 + 输入/输出槽 + 槽位信息 + 升级类型按钮（第一行）与执行按钮（第二行）。
 * 点击类型按钮发送 clickMenuButton(id) 切换升级类型，点击执行按钮触发升级。
 */
public class ChishiUpgradeStationScreen extends AbstractContainerScreen<ChishiUpgradeStationMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_energy_cell.png");

    private static final int BAR_X = 20, BAR_Y = 16, BAR_W = 136, BAR_H = 8;

    /** 升级类型按钮区（横向排列，数量随装备部位动态，最多 8 个） */
    private static final int TYPE_BTN_Y = 56;
    private static final int TYPE_BTN_W = 18;
    private static final int TYPE_BTN_H = 16;
    private static final int TYPE_BTN_GAP = 1;
    /** 执行按钮（第二行，居中；底边不超过背包区 y=84） */
    private static final int EXEC_BTN_X = 50;
    private static final int EXEC_BTN_Y = 72;
    private static final int EXEC_BTN_W = 76;
    private static final int EXEC_BTN_H = 12;

    public ChishiUpgradeStationScreen(ChishiUpgradeStationMenu menu, Inventory inv, Component title) {
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

        // 输入/模板/输出槽位框（背景贴图复用能量单元，无槽位底框，自绘原版风格）
        GuiWidgets.slotBox(gui, x + 44, y + 30);
        GuiWidgets.slotBox(gui, x + 62, y + 30);
        GuiWidgets.slotBox(gui, x + 116, y + 30);

        // 特殊能力按钮：选中橙色 + 亮边框，未选中深灰 + 暗边框（仅绘制当前装备可用的能力）
        List<Integer> visible = visibleAbilities();
        for (int idx = 0; idx < visible.size(); idx++) {
            int i = visible.get(idx);
            int bx = x + TYPE_BTN_X(idx);
            boolean selected = i == menu.getSelectedType();
            int fill = selected ? 0xFFFFA030 : 0xFF606060;
            int border = selected ? 0xFFFFFFFF : 0xFF404040;
            fillButton(gui, bx, y + TYPE_BTN_Y, TYPE_BTN_W, TYPE_BTN_H, fill, border);
        }
        // 执行按钮（绿色 + 亮边框）
        fillButton(gui, x + EXEC_BTN_X, y + EXEC_BTN_Y, EXEC_BTN_W, EXEC_BTN_H, 0xFF30A030, 0xFFE0FFE0);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF3F3F3F, false);

        // 槽位信息：直接读客户端已同步的输入槽物品，避免依赖服务端 data 广播时序导致残留
        ItemStack gear = this.menu.getSlot(ChishiUpgradeStationBlockEntity.INPUT_GEAR_SLOT).getItem();
        boolean hasGear = ChishiUpgradeHelper.isChishiEquipment(gear) || ChishiUpgradeHelper.isChishiGear(gear);
        String slotText = hasGear
                ? Component.translatable("gui.template_mod.upgrade.slots", ChishiUpgradeHelper.getSlots(gear)).getString()
                : Component.translatable("gui.template_mod.upgrade.no_gear").getString();
        gui.drawString(this.font, slotText, 20, 48, 0xE0E0E0, false);

        // 特殊能力按钮文字（按装备部位动态显示可用能力）
        ChishiUpgradeHelper.SpecialAbility[] abilities = ChishiUpgradeHelper.SpecialAbility.values();
        List<Integer> visible = visibleAbilities();
        int typeTextY = TYPE_BTN_Y + (TYPE_BTN_H - 8) / 2;
        for (int idx = 0; idx < visible.size(); idx++) {
            int lx = TYPE_BTN_X(idx) + TYPE_BTN_W / 2;
            gui.drawCenteredString(this.font, Component.translatable(abilities[visible.get(idx)].buttonKey),
                    lx, typeTextY, 0xFFFFFF);
        }
        gui.drawCenteredString(this.font, Component.translatable("gui.template_mod.upgrade.execute"),
                EXEC_BTN_X + EXEC_BTN_W / 2, EXEC_BTN_Y + (EXEC_BTN_H - 8) / 2, 0xFFFFFF);
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
        if (button == 0) {
            // 特殊能力按钮（仅当前装备可用的能力响应点击）
            List<Integer> visible = visibleAbilities();
            for (int idx = 0; idx < visible.size(); idx++) {
                int bx = this.leftPos + TYPE_BTN_X(idx);
                if (mouseX >= bx && mouseX < bx + TYPE_BTN_W
                        && mouseY >= this.topPos + TYPE_BTN_Y && mouseY < this.topPos + TYPE_BTN_Y + TYPE_BTN_H) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, visible.get(idx));
                    return true;
                }
            }
            // 执行按钮
            if (mouseX >= this.leftPos + EXEC_BTN_X && mouseX < this.leftPos + EXEC_BTN_X + EXEC_BTN_W
                    && mouseY >= this.topPos + EXEC_BTN_Y && mouseY < this.topPos + EXEC_BTN_Y + EXEC_BTN_H) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, ChishiUpgradeStationMenu.BUTTON_EXECUTE);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 第 i 个类型按钮的 x 坐标 */
    private static int TYPE_BTN_X(int i) {
        return 20 + i * (TYPE_BTN_W + TYPE_BTN_GAP);
    }

    /** 当前装备可用的特殊能力序号列表（通用能力 + 部位专属能力） */
    private List<Integer> visibleAbilities() {
        ItemStack gear = this.menu.getSlot(ChishiUpgradeStationBlockEntity.INPUT_GEAR_SLOT).getItem();
        ChishiUpgradeHelper.SpecialAbility[] abilities = ChishiUpgradeHelper.SpecialAbility.values();
        List<Integer> visible = new java.util.ArrayList<>();
        for (int i = 0; i < abilities.length; i++) {
            if (abilities[i].isApplicable(gear)) {
                visible.add(i);
            }
        }
        return visible;
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
