package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 聚变物品口界面：27 格缓冲槽（9×3）+ 底部提示。
 * 输入口提示"燃料棒自动供给控制器"，输出口提示"灰烬自动取出"。
 */
public class AkaishiFusionItemPortScreen extends AbstractContainerScreen<AkaishiFusionItemPortMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_fusion_item_port.png");

    public AkaishiFusionItemPortScreen(AkaishiFusionItemPortMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        gui.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF3F3F3F, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 缓冲槽下方提示（背包区上方空档）
        Component hint = Component.translatable(this.menu.getKind() == AkaishiFusionItemPortMenu.BufferKind.INPUT_RODS
                ? "gui.akaishi.fusion.item_input_hint" : "gui.akaishi.fusion.item_output_hint");
        gui.drawString(this.font, hint, this.leftPos + 8, this.topPos + 74, 0xFF707070, false);
    }
}
