package com.example.akaishi.api.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * HUD 元素的绘制上下文（<b>仅客户端</b>）。
 *
 * <p><b>为什么是可复用的可变对象</b>：它每帧、每个元素都要一份，若做成 record 就会在渲染线程上
 * 每帧产生若干短命对象。这里由渲染层持有<b>唯一实例</b>并在每个元素绘制前重填字段，
 * 全程零分配 —— 因此实现里<b>禁止缓存 ctx、禁止跨元素/跨帧持有</b>（它的内容会被下一个元素覆盖）。
 */
public final class HudRenderContext {

    private GuiGraphics graphics;
    private Font font;
    private float partialTick;
    private int screenWidth;
    private int screenHeight;
    private HudAnchor anchor;
    private int availableWidth;
    private int x;
    private int y;
    private int width;
    private int height;

    /** 仅由渲染层调用（元素侧禁止调用）：填充本帧本元素的解算结果。 */
    public void prepare(GuiGraphics graphics, Font font, float partialTick,
                        int screenWidth, int screenHeight,
                        HudAnchor anchor, int availableWidth,
                        int x, int y, int width, int height) {
        this.graphics = graphics;
        this.font = font;
        this.partialTick = partialTick;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.anchor = anchor;
        this.availableWidth = availableWidth;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public GuiGraphics graphics() {
        return graphics;
    }

    public Font font() {
        return font;
    }

    public float partialTick() {
        return partialTick;
    }

    public int screenWidth() {
        return screenWidth;
    }

    public int screenHeight() {
        return screenHeight;
    }

    public HudAnchor anchor() {
        return anchor;
    }

    /** 本锚点在本屏宽下的可用宽度（可能大于元素自身宽度）。 */
    public int availableWidth() {
        return availableWidth;
    }

    /** 元素左边缘 x（已解算锚点 + 偏移 + 避让）。 */
    public int x() {
        return x;
    }

    /** 元素顶边 y（已解算锚线 + 偏移 + 堆叠 + 避让 + 屏幕内夹取）。 */
    public int y() {
        return y;
    }

    /** 渲染层为本次绘制保留的宽度（= {@link AkaishiHudElement#measure(int)} 的返回值）。 */
    public int width() {
        return width;
    }

    /** 渲染层为本次绘制保留的高度。 */
    public int height() {
        return height;
    }
}
