package com.example.akaishi.decay;

/**
 * 客户端污染氛围状态（仅客户端加载）：保存服务端同步的目标强度，
 * 雾渲染每帧按指数衰减趋近目标，实现进出区域时氛围的平滑渐变。
 */
public final class DecayClientAmbience {

    /** 每帧向目标趋近比例（越大过渡越快） */
    private static final float LERP = 0.08f;

    private static float target;
    private static float current;

    private DecayClientAmbience() {
    }

    /** 网络/主线程设置目标强度（0~1），自动钳制 */
    public static void setTarget(float value) {
        target = Math.max(0f, Math.min(1f, value));
    }

    /** 渲染线程调用：推进平滑过渡并返回当前强度（0~1） */
    public static float current() {
        float diff = target - current;
        if (diff * diff < 1e-6f) {
            current = target;
        } else {
            current += diff * LERP;
        }
        return current;
    }
}
