package com.example.akaishi.menu;

import com.example.akaishi.item.MechanicalPartItem;
import com.example.akaishi.life.mechanical.MechanicalOrganType;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 组装加工台界面：左上双能源小半截条 + 右上升级槽 + 四部件输入 → 成品输出 + 器官一致性提示 + 进度。
 */
public class AkaishiMechanicalAssemblyStationScreen extends AbstractContainerScreen<AkaishiMechanicalAssemblyStationMenu> {

    private static final int BAR_X = 20, BAR_W = 60, BAR_H = 6;
    private static final int AKASHI_BAR_Y = 24, LIFE_BAR_Y = 32;
    private static final int SPEED_SLOT_X = 134, SPEED_SLOT_Y = 8;
    private static final int ENERGY_SLOT_X = 152, ENERGY_SLOT_Y = 8;
    private static final int PART_X = 20, PART_SPACING = 18, PART_Y = 56;
    private static final int OUTPUT_X = 116, OUTPUT_Y = 56;
    // 箭头即进度（指向输出槽；四部件槽到 92，箭头右移收窄避免与第 4 槽重叠）
    private static final int ARROW_X = 94, ARROW_Y = 56, ARROW_W = 20, ARROW_H = 18;
    // 器官一致性提示行（y=80，槽行下方、背包 y=104 上方，二者不重叠）
    private static final int STATUS_Y = 80;
    // 制作按钮（能源条下方、槽行上方，三机坐标一致）
    private static final int CRAFT_X = 73, CRAFT_Y = 42, CRAFT_W = 30, CRAFT_H = 12;
    private static final int PANEL_W = 176, PANEL_H = 196;

    public AkaishiMechanicalAssemblyStationScreen(AkaishiMechanicalAssemblyStationMenu menu, Inventory inv, Component title) {
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

        // 四部件槽 → 输出
        for (int i = 0; i < 4; i++) {
            GuiWidgets.slotBox(gui, x + PART_X + i * PART_SPACING, y + PART_Y);
        }
        GuiWidgets.slotBox(gui, x + OUTPUT_X, y + OUTPUT_Y);

        // 箭头即进度（指向输出槽）
        GuiWidgets.progressArrow(gui, x + ARROW_X, y + ARROW_Y, ARROW_W, ARROW_H, menu.getProgressPercent());

        // 制作按钮：单次制作，条件不满足或制作中置灰
        GuiWidgets.buttonText(gui, this.font, x + CRAFT_X, y + CRAFT_Y, CRAFT_W, CRAFT_H,
                Component.translatable("gui.akaishi.mech.craft"), menu.canCraft() && !menu.isCrafting());

        // 器官一致性提示：四槽齐且同器官时绿色 "READY"，异器官/缺件时灰色
        ItemStack first = menu.container().getItem(0);
        MechanicalOrganType common = first.isEmpty() ? null : MechanicalPartItem.getOrganType(first);
        boolean ready = false;
        if (common != null) {
            ready = true;
            for (int i = 0; i < 4; i++) {
                ItemStack s = menu.container().getItem(i);
                if (s.isEmpty() || MechanicalPartItem.getOrganType(s) != common) {
                    ready = false;
                    break;
                }
            }
        }
        if (ready) {
            gui.drawString(this.font,
                    Component.translatable("gui.akaishi.mech.ready", Component.translatable("mechanical.organ." + common.name().toLowerCase())),
                    x + 20, y + STATUS_Y, 0xFF2F7F2F, false);
        } else if (common != null) {
            gui.drawString(this.font, Component.translatable("gui.akaishi.mech.organ_mismatch"),
                    x + 20, y + STATUS_Y, 0xFF8F6F2F, false);
        } else {
            gui.drawString(this.font, Component.translatable("gui.akaishi.mech.place_four"),
                    x + 20, y + STATUS_Y, 0xFF707070, false);
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
        // 四部件输入槽 + 成品输出槽
        int base = AbstractMechanicalMachineMenu.UPGRADE_SLOT_COUNT;
        String[] keys = {"gui.akaishi.mech.core_slot", "gui.akaishi.mech.module_slot",
                "gui.akaishi.mech.shell_slot", "gui.akaishi.mech.cooling_slot"};
        for (int i = 0; i < keys.length; i++) {
            if (slotTip(gui, base + i, keys[i], mouseX, mouseY)) {
                return;
            }
        }
        slotTip(gui, base + 4, "gui.akaishi.mech.output_organ_slot", mouseX, mouseY);
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