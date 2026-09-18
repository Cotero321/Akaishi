package com.example.akaishi.menu;

import com.example.akaishi.item.MechanicalPartItem;
import com.example.akaishi.life.mechanical.MechanicalMachineCosts;
import com.example.akaishi.life.mechanical.MechanicalOrganType;
import com.example.akaishi.life.mechanical.MechanicalPartType;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 加工制作厂界面：左上双能源小半截条 + 右上升级槽 + 模板/材料/固态物输入 → 加工件输出 + 费用预览 + 进度。
 */
public class AkaishiMechanicalProcessingFactoryScreen extends AbstractContainerScreen<AkaishiMechanicalProcessingFactoryMenu> {

    private static final int BAR_X = 20, BAR_W = 60, BAR_H = 6;
    private static final int AKASHI_BAR_Y = 24, LIFE_BAR_Y = 32;
    private static final int SPEED_SLOT_X = 134, SPEED_SLOT_Y = 8;
    private static final int ENERGY_SLOT_X = 152, ENERGY_SLOT_Y = 8;
    private static final int WIRELESS_SLOT_X = 116, WIRELESS_SLOT_Y = 8;
    private static final int INPUT_TEMPLATE_X = 26, INPUT_MATERIAL_X = 44, INPUT_SOLID_X = 62, INPUT_Y = 56;
    private static final int OUTPUT_X = 116, OUTPUT_Y = 56;
    // 箭头即进度（指向输出槽）
    private static final int ARROW_X = 86, ARROW_Y = 56, ARROW_W = 22, ARROW_H = 18;
    // 费用预览文本行（y=80，槽行下方、背包上方，二者不重叠）
    private static final int COST_Y = 80;
    // 制作按钮（能源条下方、槽行上方，三机坐标一致）
    private static final int CRAFT_X = 73, CRAFT_Y = 42, CRAFT_W = 30, CRAFT_H = 12;
    // 面板尺寸（背包 y=104、快捷栏 y=168，底部留白 10px）
    private static final int PANEL_W = 176, PANEL_H = 196;

    public AkaishiMechanicalProcessingFactoryScreen(AkaishiMechanicalProcessingFactoryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        GuiWidgets.panel(gui, x, y, PANEL_W, PANEL_H);
        GuiWidgets.playerInventory(gui, x, y);

        // 双能源小半截条
        GuiWidgets.track(gui, x + BAR_X, y + AKASHI_BAR_Y, BAR_W, BAR_H);
        GuiWidgets.track(gui, x + BAR_X, y + LIFE_BAR_Y, BAR_W, BAR_H);
        GuiWidgets.bar(gui, x + BAR_X, y + AKASHI_BAR_Y, BAR_W, BAR_H, menu.getAkaishiEnergy(), menu.getAkaishiMax(), 0xFFE03030);
        GuiWidgets.bar(gui, x + BAR_X, y + LIFE_BAR_Y, BAR_W, BAR_H, menu.getLifeEnergy(), menu.getLifeMax(), 0xFF28B428);

        GuiWidgets.slotBox(gui, x + SPEED_SLOT_X, y + SPEED_SLOT_Y);
        GuiWidgets.slotBox(gui, x + ENERGY_SLOT_X, y + ENERGY_SLOT_Y);
        GuiWidgets.slotBox(gui, x + WIRELESS_SLOT_X, y + WIRELESS_SLOT_Y);

        // 输入三槽 → 输出
        GuiWidgets.slotBox(gui, x + INPUT_TEMPLATE_X, y + INPUT_Y);
        GuiWidgets.slotBox(gui, x + INPUT_MATERIAL_X, y + INPUT_Y);
        GuiWidgets.slotBox(gui, x + INPUT_SOLID_X, y + INPUT_Y);
        GuiWidgets.slotBox(gui, x + OUTPUT_X, y + OUTPUT_Y);

        // 箭头即进度（指向输出槽）
        GuiWidgets.progressArrow(gui, x + ARROW_X, y + ARROW_Y, ARROW_W, ARROW_H, menu.getProgressPercent());

        // 制作按钮：单次制作，条件不满足或制作中置灰
        GuiWidgets.buttonText(gui, this.font, x + CRAFT_X, y + CRAFT_Y, CRAFT_W, CRAFT_H,
                Component.translatable("gui.akaishi.mech.craft"), menu.canCraft() && !menu.isCrafting());

        // 费用预览（基于模板槽；服务端权威费用已 S2C 同步）
        ItemStack template = menu.container().getItem(0);
        var organ = MechanicalPartItem.getOrganType(template);
        var part = MechanicalPartItem.getPartType(template);
        if (organ != null && part != null) {
            long chishi = MechanicalMachineCosts.processChishiCost(organ, part);
            long life = MechanicalMachineCosts.processLifeCost(organ, part);
            int ticks = MechanicalMachineCosts.processTicks(organ, part);
            int mat = MechanicalMachineCosts.materialCount(part);
            gui.drawString(this.font, Component.translatable("gui.akaishi.mech.cost",
                    EnergyFormat.format(chishi), EnergyFormat.format(life), String.format("%.1fs", ticks / 20.0)),
                    x + 20, y + COST_Y, 0xFF3F3F3F, false);
            gui.drawString(this.font, Component.translatable("gui.akaishi.mech.material_x", mat),
                    x + 20, y + COST_Y + 8, 0xFF3F3F3F, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF3F3F3F, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);
        // 双能源条
        if (isHovering(BAR_X, AKASHI_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.mech.akaishi_bar",
                    EnergyFormat.format(menu.getAkaishiEnergy()), EnergyFormat.format(menu.getAkaishiMax())), mouseX, mouseY);
            return;
        }
        if (isHovering(BAR_X, LIFE_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.mech.life_bar",
                    EnergyFormat.format(menu.getLifeEnergy()), EnergyFormat.format(menu.getLifeMax())), mouseX, mouseY);
            return;
        }
        // 箭头即进度
        if (isHovering(ARROW_X, ARROW_Y, ARROW_W, ARROW_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.mech.arrow_progress",
                    menu.getProgressPercent()), mouseX, mouseY);
            return;
        }
        // 制作按钮：按状态区分提示（可制作 / 条件不满足 / 制作中）
        if (isHovering(CRAFT_X, CRAFT_Y, CRAFT_W, CRAFT_H, mouseX, mouseY)) {
            String key = menu.isCrafting() ? "gui.akaishi.mech.craft_hint_crafting"
                    : (menu.canCraft() ? "gui.akaishi.mech.craft_hint" : "gui.akaishi.mech.craft_hint_disabled");
            gui.renderTooltip(this.font, Component.translatable(key), mouseX, mouseY);
            return;
        }
        // 升级槽
        if (slotTip(gui, MachineUpgradeSlots.SLOT_SPEED, "gui.akaishi.upgrade.speed.hint", mouseX, mouseY)) {
            return;
        }
        if (slotTip(gui, MachineUpgradeSlots.SLOT_ENERGY, "gui.akaishi.upgrade.energy.hint", mouseX, mouseY)) {
            return;
        }
        // 无线接收槽：按已装/未装给出状态提示
        if (isHovering(WIRELESS_SLOT_X, WIRELESS_SLOT_Y, 16, 16, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.upgrade.wireless_slot",
                            Component.translatable(menu.hasWirelessReceiver()
                                    ? "gui.akaishi.upgrade.installed" : "gui.akaishi.upgrade.absent")),
                    mouseX, mouseY);
            return;
        }
        // 机器槽（模板 / 材料 / 固态物 / 输出）
        int base = AbstractMechanicalMachineMenu.UPGRADE_SLOT_COUNT;
        if (slotTip(gui, base, "gui.akaishi.mech.template_slot", mouseX, mouseY)) {
            return;
        }
        if (slotTip(gui, base + 1, "gui.akaishi.mech.material_slot", mouseX, mouseY)) {
            return;
        }
        if (slotTip(gui, base + 2, "gui.akaishi.mech.solid_slot", mouseX, mouseY)) {
            return;
        }
        slotTip(gui, base + 3, "gui.akaishi.mech.output_part_slot", mouseX, mouseY);
    }

    /** 空槽悬浮提示：命中且槽位为空时渲染对应键，返回是否已提示 */
    private boolean slotTip(GuiGraphics gui, int index, String key, int mouseX, int mouseY) {
        if (index < 0 || index >= menu.slots.size()) {
            return false;
        }
        var slot = menu.slots.get(index);
        if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) && slot.getItem().isEmpty()) {
            gui.renderTooltip(this.font, Component.translatable(key), mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // 制作按钮：单次制作请求（条件满足且未在制作中）
        if (button == 0 && inBox(mx, my, this.leftPos + CRAFT_X, this.topPos + CRAFT_Y, CRAFT_W, CRAFT_H)) {
            if (menu.canCraft() && !menu.isCrafting()) {
                MechanicalCraftSync.sendCraft(menu.getBlockPos());
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
