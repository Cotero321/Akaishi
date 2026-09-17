package com.example.akaishi.forge.client;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.effect.ScreenFlashClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * 屏幕边缘泛红叠加层（D100/D103/D208，仅客户端 HUD）：
 * 复用「不可名状」的噪点贴图染成血色，只在四条边缘按由外向内递减的透明度铺开，
 * 中心保持清透，避免长时间红屏遮挡画面。强度来自 {@link ScreenFlashClient} 的一次性包络。
 */
public final class AkaishiErosionFlashOverlay implements IGuiOverlay {

    /** 噪点平铺源贴图：16×16 半透明噪点（与不可名状表现同源，零新增资源） */
    private static final ResourceLocation NOISE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/unnameable_noise.png");

    /** 低于此强度视为结束，直接跳过绘制 */
    private static final float MIN_LEVEL = 0.02f;
    /** 满强度时的边缘总厚度（像素） */
    private static final int MAX_BAND = 42;
    /** 由外向内分的渐变层数：层数越多过渡越柔（fill 只能画纯色，靠多层近似羽化） */
    private static final int GRADIENT_STEPS = 4;
    /** 边缘血色（0x8C0A12）与最大不透明度 */
    private static final int EDGE_RGB = 0x8C0A12;
    private static final float EDGE_MAX_ALPHA = 0.62f;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        // 打开界面或隐藏 HUD 时不叠加，避免遮挡交互控件
        if (minecraft.screen != null || minecraft.options.hideGui) {
            return;
        }
        float level = ScreenFlashClient.level();
        if (level <= MIN_LEVEL) {
            return;
        }
        int band = Math.max(GRADIENT_STEPS, Math.round(MAX_BAND * level));
        int step = Math.max(1, band / GRADIENT_STEPS);
        for (int i = 0; i < GRADIENT_STEPS; i++) {
            // 最外圈最浓，逐圈减淡：形成"从屏幕边缘向内渗入"的遮罩感
            float alpha = level * EDGE_MAX_ALPHA * (1.0f - i * 0.24f);
            int argb = (clampAlpha(alpha) << 24) | EDGE_RGB;
            fillRing(graphics, screenWidth, screenHeight, i * step, (i + 1) * step, argb);
        }
        // 最外圈叠一层染红噪点，与不可名状的视觉语言一致（只铺在最外圈，避免糊满屏幕）
        graphics.setColor(1.0f, 0.30f, 0.36f, 0.55f * level);
        graphics.blitRepeating(NOISE, 0, 0, screenWidth, step, 0, 0, 16, 16);
        graphics.blitRepeating(NOISE, 0, screenHeight - step, screenWidth, step, 0, 0, 16, 16);
        graphics.blitRepeating(NOISE, 0, step, step, screenHeight - 2 * step, 0, 0, 16, 16);
        graphics.blitRepeating(NOISE, screenWidth - step, step, step, screenHeight - 2 * step, 0, 0, 16, 16);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /** 画一圈矩形环带（内缩 offset 起、厚 thickness），四条边分别填充避免中心被覆盖 */
    private static void fillRing(GuiGraphics graphics, int width, int height, int offset, int thickness, int argb) {
        int end = offset + thickness;
        graphics.fill(offset, offset, width - offset, end, argb);
        graphics.fill(offset, height - end, width - offset, height - offset, argb);
        graphics.fill(offset, end, end, height - end, argb);
        graphics.fill(width - end, end, width - offset, height - end, argb);
    }

    private static int clampAlpha(float alpha) {
        return Math.round(Math.max(0.0f, Math.min(1.0f, alpha)) * 255.0f);
    }
}
