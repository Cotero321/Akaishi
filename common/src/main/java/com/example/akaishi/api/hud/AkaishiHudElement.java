package com.example.akaishi.api.hud;

import net.minecraft.resources.ResourceLocation;

/**
 * 统一 HUD 元素契约（<b>仅客户端</b>）：任何"贴在屏幕某一带的自绘 HUD"都实现本接口并注册到
 * {@link AkaishiHudRegistry}，由渲染层统一解算位置、堆叠避让与绘制。
 *
 * <p><b>契约承诺</b>：
 * <ul>
 *   <li><b>何时被调用</b>：每个客户端帧调用 {@link #isVisible()} 至多一次、{@link #measure(int)} 一次、
 *       {@link #render(HudRenderContext)} 至多一次；未通过可见性检查的元素不占位也不绘制。</li>
 *   <li><b>线程</b>：全部在<b>客户端渲染线程</b>（MC 主线程）调用，与渲染同帧；实现里不要阻塞、不要做世界查询。</li>
 *   <li><b>坐标系</b>：逻辑像素、原点屏幕左上角（同原版 {@code GuiGraphics}）。
 *       {@link #render} 必须画在 {@code [x, x+width) × [y, y+height)} 内 —— 声明尺寸与实际绘制必须一致，
 *       否则堆叠避让会算错。</li>
 *   <li><b>坐标不对齐</b>：实现不得自行读取 {@code screenWidth} 反推锚点；一律用
 *       {@link HudRenderContext} 里已解算好的 x/y。</li>
 *   <li><b>异常隔离</b>：渲染层对每个元素做异常隔离（单个元素抛错不影响其它元素与本帧后续绘制），
 *       但实现仍应自行保证不抛。</li>
 * </ul>
 */
public interface AkaishiHudElement {

    /** 元素唯一 id（用 {@code akaishi:} 命名空间）；重复注册同 id 时后者抢占。 */
    ResourceLocation id();

    /** 所属锚点。 */
    HudAnchor anchor();

    /** 锚点内的横向微调（逻辑像素，待调手感值）；同锚点元素一般留 0。 */
    default int offsetX() {
        return 0;
    }

    /** 锚点内的纵向微调（逻辑像素，待调手感值）。 */
    default int offsetY() {
        return 0;
    }

    /** 同锚点内的堆叠次序：数值小的先放（离锚线更近）；相同则按注册顺序。 */
    default int priority() {
        return 0;
    }

    /**
     * 声明占位尺寸（逻辑像素）。必须与实际绘制范围一致。
     *
     * @param availableWidth 本锚点在当前屏宽下的可用宽度（见 {@link HudAnchor#availableWidth(int)}）；
     *                       元素应按它自适应（例：{@code clamp(preferred, min, availableWidth)}）
     */
    HudSize measure(int availableWidth);

    /** 当前是否应显示；返回 false 时既不绘制也不占堆叠位。 */
    default boolean isVisible() {
        return true;
    }

    /** 绘制。{@code ctx} 里的 x/y/width/height 已由渲染层解算完成。 */
    void render(HudRenderContext ctx);

    /** 元素占位尺寸（不可变值对象）。 */
    record HudSize(int width, int height) {
    }
}
