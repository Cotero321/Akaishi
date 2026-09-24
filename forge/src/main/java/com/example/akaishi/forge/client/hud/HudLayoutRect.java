package com.example.akaishi.forge.client.hud;

/**
 * 逻辑像素矩形（HUD 布局内部用）：坐标 + 尺寸，半开区间语义 {@code [x, x+width) × [y, y+height)}。
 *
 * <p>只服务于"避让判定"：本层需要把"元素占位"与"原版 HUD 保留区"做几何求交，
 * 故用 {@link #intersects(int, int, int, int)} 的重载<b>避免</b>为每个待判定元素新建矩形对象
 * （渲染线程每帧都跑，不该在这里分配）。
 */
record HudLayoutRect(int x, int y, int width, int height) {

    int right() {
        return x + width;
    }

    int bottom() {
        return y + height;
    }

    /** 与给定矩形求交（半开区间：贴边不算交叠）。 */
    boolean intersects(int ox, int oy, int ow, int oh) {
        return x < ox + ow && ox < right() && y < oy + oh && oy < bottom();
    }
}
