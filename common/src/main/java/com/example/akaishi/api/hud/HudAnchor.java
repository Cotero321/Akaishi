package com.example.akaishi.api.hud;

/**
 * HUD 元素锚点：声明元素"贴在屏幕的哪一带"，由统一渲染层解算为逻辑像素坐标。
 *
 * <p><b>坐标系</b>：与原版 {@code GuiGraphics} 完全一致 —— 逻辑像素、原点在屏幕左上角、
 * x 向右为正、y 向下为正；逻辑尺寸 = 物理像素 ÷ GUI 缩放（例：GUI 缩放 4 时整屏为 480×270 逻辑像素）。
 * 元素<b>不要</b>自己算锚点坐标，统一从 {@link HudRenderContext} 取解算后的 (x, y)。
 *
 * <p><b>锚点语义</b>：每个锚点定义一条<b>锚线</b>（横向位置 + 起始纵坐标）与一个<b>堆叠方向</b>；
 * 同锚点的元素沿堆叠方向依次排布、由渲染层保证互不重叠（详见 {@code AkaishiHudLayer}）。
 *
 * <p>本枚举只含"贴边/贴基准带"的锚点；屏幕正中（如 BOSS 血条）不在此体系内 —— 它属于"以屏幕中轴为不
 * 动点的独立层"，硬塞进锚点体系反而会破坏既有表现。
 */
public enum HudAnchor {

    /** 屏幕左上角：向下堆叠。 */
    TOP_LEFT(true, StackDirection.DOWN),
    /** 屏幕右上角：向下堆叠（右对齐由渲染层按元素宽度反算 x）。 */
    TOP_RIGHT(false, StackDirection.DOWN),
    /** 屏幕左下角（聊天框上方一带）：向上堆叠。 */
    BOTTOM_LEFT(true, StackDirection.UP),
    /** 屏幕右下角：向上堆叠。 */
    BOTTOM_RIGHT(false, StackDirection.UP),
    /**
     * 快捷栏右侧、与经验条同一水平带：向上堆叠（本层为理智 HUD 的主用锚点）。
     *
     * <p>锚线 = 快捷栏顶边再上移 {@link #HOTBAR_GAP_Y}；x 从快捷栏右边缘右移 {@link #HOTBAR_GAP_X} 起算。
     * 这是全屏最紧的一档可用宽度（GUI 缩放 4 / 1920×1080 ⇒ {@code 480/2−91−6−8 = 135} 逻辑像素），
     * 故元素必须能按可用宽度自适应。
     */
    HOTBAR_RIGHT(true, StackDirection.UP);

    // ===== 几何常量（原版硬编码值 + 待调手感值）=====

    /** 贴屏幕边时的留白（逻辑像素，待调手感值）。 */
    public static final int EDGE_MARGIN = 8;
    /** 同锚点内相邻元素之间的间隔（逻辑像素，待调手感值）。 */
    public static final int STACK_GAP = 2;
    /** 原版快捷栏半宽（182/2）：原版硬编码，此处仅作锚点几何常量，不改原版行为。 */
    public static final int HOTBAR_HALF_WIDTH = 91;
    /** 原版快捷栏高度：同上。 */
    public static final int HOTBAR_HEIGHT = 22;
    /** 快捷栏右边缘与元素左边缘之间的横向间隙（逻辑像素，待调手感值）。 */
    public static final int HOTBAR_GAP_X = 6;
    /** 快捷栏顶边与元素底边之间的纵向间隙（逻辑像素，待调手感值）。 */
    public static final int HOTBAR_GAP_Y = 4;

    private final boolean growsRightward;
    private final StackDirection stackDirection;

    HudAnchor(boolean growsRightward, StackDirection stackDirection) {
        this.growsRightward = growsRightward;
        this.stackDirection = stackDirection;
    }

    /** 堆叠方向（沿屏幕纵轴）。 */
    public enum StackDirection {
        /** 向下：元素顶边贴锚线，后续元素依次下移。 */
        DOWN,
        /** 向上：元素底边贴锚线，后续元素依次上移。 */
        UP
    }

    /** 横向是否自左边缘向右生长（false = 右对齐，x 需按元素宽度反算）。 */
    public boolean growsRightward() {
        return growsRightward;
    }

    public StackDirection stackDirection() {
        return stackDirection;
    }

    /** 锚点横向起点（解算后元素的左边缘 x）。 */
    public int anchorX(int screenWidth, int elementWidth) {
        return switch (this) {
            case TOP_LEFT, BOTTOM_LEFT -> EDGE_MARGIN;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - EDGE_MARGIN - elementWidth;
            case HOTBAR_RIGHT -> screenWidth / 2 + HOTBAR_HALF_WIDTH + HOTBAR_GAP_X;
        };
    }

    /**
     * 锚线纵坐标：向下堆叠时是第一个元素的<b>顶边</b>，向上堆叠时是第一个元素的<b>底边</b>。
     */
    public int anchorLineY(int screenHeight) {
        return switch (this) {
            case TOP_LEFT, TOP_RIGHT -> EDGE_MARGIN;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - EDGE_MARGIN;
            case HOTBAR_RIGHT -> screenHeight - HOTBAR_HEIGHT - HOTBAR_GAP_Y;
        };
    }

    /**
     * 该锚点在本屏宽下的可用宽度（逻辑像素）：元素应据此自适应，超出部分会被屏幕/原版 HUD 吃掉。
     */
    public int availableWidth(int screenWidth) {
        return switch (this) {
            case HOTBAR_RIGHT -> Math.max(0,
                    screenWidth / 2 - HOTBAR_HALF_WIDTH - HOTBAR_GAP_X - EDGE_MARGIN);
            case TOP_LEFT, BOTTOM_LEFT, TOP_RIGHT, BOTTOM_RIGHT -> Math.max(0, screenWidth - 2 * EDGE_MARGIN);
        };
    }
}
