package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiTransgeneFactoryBlockEntity;
import com.example.akaishi.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 转基因工厂界面：全自绘原版灰面板风格。
 * 布局：标题(6) → 材料槽 4 + 产物 1(30) → 进度条(58) → 状态文字(70) → 玩家背包(124)。
 */
public class AkaishiTransgeneFactoryScreen extends AbstractContainerScreen<AkaishiTransgeneFactoryMenu> {

    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_DIM = 0xFF707070;
    private static final int TEXT_RED = 0xFFB03030;
    private static final int TEXT_GREEN = 0xFF2E7D32;
    /** 合成进度金色（语义色：进度金、能量红绿） */
    private static final int COLOR_PROGRESS = 0xFFFFD030;

    // 与 Menu 槽位坐标一致
    private static final int ENERGY_X = 26;
    private static final int ENERGY_Y = 16;
    private static final int ENERGY_W = 108;
    private static final int ENERGY_H = 8;
    private static final int TRACK_X = 26;
    private static final int TRACK_Y = 58;
    private static final int TRACK_W = 96;
    private static final int TRACK_H = 8;
    /** 生命能量条绿色（生命系机器通用语义色） */
    private static final int COLOR_LIFE = 0xFF28B428;

    public AkaishiTransgeneFactoryScreen(AkaishiTransgeneFactoryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        GuiWidgets.panel(gui, x, y, this.imageWidth, this.imageHeight);
        // 生命能量条（5k/次，供能不足时呈现空槽）
        GuiWidgets.track(gui, x + ENERGY_X, y + ENERGY_Y, ENERGY_W, ENERGY_H);
        long life = this.menu.getLifeEnergy();
        long cap = Math.max(1, this.menu.getLifeMax());
        int lifeW = (int) (ENERGY_W * Math.max(0, Math.min(life, cap)) / cap);
        if (lifeW > 0) {
            gui.fill(x + ENERGY_X, y + ENERGY_Y, x + ENERGY_X + lifeW, y + ENERGY_Y + ENERGY_H, COLOR_LIFE);
        }
        // 材料槽（基因/缠怨藤/凋零玫瑰/固态物）+ 产物槽
        GuiWidgets.slotBox(gui, x + 26, y + 30);
        GuiWidgets.slotBox(gui, x + 44, y + 30);
        GuiWidgets.slotBox(gui, x + 62, y + 30);
        GuiWidgets.slotBox(gui, x + 80, y + 30);
        GuiWidgets.slotBox(gui, x + 134, y + 30);
        // 进度条
        GuiWidgets.track(gui, x + TRACK_X, y + TRACK_Y, TRACK_W, TRACK_H);
        int pct = this.menu.getProgressPct();
        if (this.menu.isWorking() && pct > 0) {
            int w = (int) (TRACK_W * pct / 100.0F);
            gui.fill(x + TRACK_X, y + TRACK_Y, x + TRACK_X + w, y + TRACK_Y + TRACK_H, COLOR_PROGRESS);
        }
        // 玩家背包区
        GuiWidgets.playerInventory(gui, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, 8, 6, TEXT, false);
        Component state = statusText();
        gui.drawString(this.font, state, 8, 70, stateColor(), false);
        // 槽位角标说明
        gui.drawString(this.font, Component.literal(">"), 116, 33, TEXT_DIM, false);
    }

    /** 依据基因槽/能量推导状态文字（客户端可从已同步槽位读取判断） */
    private Component statusText() {
        if (this.menu.isWorking()) {
            return Component.translatable("gui.akaishi.transgene_factory.working", this.menu.getProgressPct());
        }
        ItemStack gene = this.menu.slots.get(0).getItem();
        if (!gene.isEmpty() && !AkaishiTransgeneFactoryBlockEntity.isWitherSkeletonGene(gene)) {
            return Component.translatable("gui.akaishi.transgene_factory.gene_bad");
        }
        if (this.menu.getLifeEnergy() < ModConfig.transgeneFactoryLifeCost) {
            return Component.translatable("gui.akaishi.transgene_factory.energy");
        }
        return Component.translatable("gui.akaishi.transgene_factory.idle");
    }

    private int stateColor() {
        if (this.menu.isWorking()) {
            return TEXT_GREEN;
        }
        ItemStack gene = this.menu.slots.get(0).getItem();
        if (!gene.isEmpty() && !AkaishiTransgeneFactoryBlockEntity.isWitherSkeletonGene(gene)) {
            return TEXT_RED;
        }
        if (this.menu.getLifeEnergy() < ModConfig.transgeneFactoryLifeCost) {
            return TEXT_RED;
        }
        return TEXT_DIM;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }
}
