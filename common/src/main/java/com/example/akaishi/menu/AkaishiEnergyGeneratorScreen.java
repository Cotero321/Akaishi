package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiEnergyGeneratorBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * 赤能源发生机界面：顶部燃料槽 + 中央火焰动画 + 右侧能量条。
 * 数据来自 {@link AkaishiEnergyGeneratorMenu} 的 ContainerData。
 */
public class AkaishiEnergyGeneratorScreen extends AbstractContainerScreen<AkaishiEnergyGeneratorMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_generator.png");

    public AkaishiEnergyGeneratorScreen(AkaishiEnergyGeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 176;
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // 仅绘制标题：背包区下移后抑制原版"物品栏"标签，避免压在第二行升级槽上
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF3F3F3F, false);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 升级槽框自绘：按 Menu 槽位坐标补齐（右上角 y=8 起两行 5 列），保证框与槽位一致
        for (int i = 0; i < 10; i++) {
            Slot slot = menu.slots.get(AkaishiEnergyGeneratorBlockEntity.UPGRADE_SLOT_START + i);
            GuiWidgets.slotBox(gui, x + slot.x, y + slot.y);
        }

        // 赤能源条：右侧垂直条固定轨道占位（能量为 0 也显示轨道，避免缺条错位），从底部向上填充
        int maxEnergy = AkaishiEnergyGeneratorBlockEntity.MAX_ENERGY;
        int energy = Math.max(0, Math.min(menu.getEnergy(), maxEnergy));
        int energyHeight = (int) Math.ceil(44.0 * energy / maxEnergy);
        GuiWidgets.track(gui, x + 153, y + 48, 10, 44);
        if (energyHeight > 0) {
            gui.fill(x + 153, y + 92 - energyHeight, x + 163, y + 92, 0xFFE03030);
        }

        // 燃料火焰动画：位于燃料槽下方，按剩余比例裁剪满帧
        int burn = menu.getBurnTime();
        int burnTotal = menu.getBurnTimeTotal();
        if (burnTotal > 0 && burn > 0) {
            int flameHeight = (int) (14.0F * burn / burnTotal);
            gui.blit(TEXTURE, x + 58, y + 49 - flameHeight, 176, 49 - flameHeight, 14, flameHeight);
        }

        // 加速组件装配数：左上角显示当前倍率
        gui.drawString(this.font,
                Component.translatable("gui.akaishi.boost_mult",
                        String.format("%.1f", menu.getBoostMultiplier())),
                x + 8, y + 40, 0xFF3F3F3F, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);
        // 能量条悬停（含轨道占位区）
        if (isHovering(153, 48, 10, 44, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.energy", menu.getEnergy(), AkaishiEnergyGeneratorBlockEntity.MAX_ENERGY),
                    mouseX, mouseY);
        } else if (isHovering(58, 35, 14, 14, mouseX, mouseY)) {
            // 燃料火焰悬停
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.fuel", menu.getBurnTime()),
                    mouseX, mouseY);
        } else {
            // 10 个升级槽：空槽也提示用途（含当前加速倍率）
            for (int i = 0; i < 10; i++) {
                Slot slot = menu.slots.get(AkaishiEnergyGeneratorBlockEntity.UPGRADE_SLOT_START + i);
                if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) && slot.getItem().isEmpty()) {
                    gui.renderTooltip(this.font,
                            Component.translatable("gui.akaishi.energy_generator.upgrade_slot",
                                    String.format("%.1f", menu.getBoostMultiplier())),
                            mouseX, mouseY);
                    break;
                }
            }
        }
    }
}
