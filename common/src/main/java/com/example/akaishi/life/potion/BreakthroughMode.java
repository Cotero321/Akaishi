package com.example.akaishi.life.potion;

/**
 * 突破药剂模式：同一套「唤醒基因强化」机制下的三种方向配方（均衡/狂涌/深沉）。
 * - 每点额外适配对应的基础数值强化百分比 pct = extra × pctScale
 * - 持续时间按分钟（转换 tick = 分钟 × 60 × 20）
 * 负基础值属性词条在激活期内照旧暂时失效，同一时间仍只允许 1 种激活。
 */
public enum BreakthroughMode {

    /** 非突破（永久药剂占位） */
    NONE(false, 0, 0),
    /** 均衡：标准 30 分钟，每点额外适配强化 +5%（默认方向，原突破药剂数值） */
    BALANCE(true, 5, 30),
    /** 狂涌：15 分钟高爆发，每点 +10%——短窗口内把来源器官数值顶到极限 */
    SURGE(true, 10, 15),
    /** 深沉：60 分钟长效低幅，每点 +3%——细水长流，适合常驻型构筑 */
    ENDURE(true, 3, 60);

    private final boolean breakthrough;
    private final int pctScale;
    private final int durationMinutes;

    BreakthroughMode(boolean breakthrough, int pctScale, int durationMinutes) {
        this.breakthrough = breakthrough;
        this.pctScale = pctScale;
        this.durationMinutes = durationMinutes;
    }

    public boolean isBreakthrough() {
        return breakthrough;
    }

    /** 该模式的强化百分比：额外适配每点对应的基础数值加成（%） */
    public int pctFor(int extraCompat) {
        return extraCompat * pctScale;
    }

    /** 强化时长（分钟），界面与提示展示用 */
    public int durationMinutes() {
        return durationMinutes;
    }

    /** 强化时长（tick），激活计时用 */
    public long durationTicks() {
        return durationMinutes * 60L * 20L;
    }
}
