package com.example.akaishi.value;

/**
 * 物品终端存取一次性费用（赤能源）换算：基准 4 IP = 1 赤能源，手续费并入系数（D11 / D12）。
 * <p>
 * <b>只有一次性费用、无维持费</b>（D7）：放入结算一次、取出结算一次，不做每 tick 抽能。
 * IP 是「占用量」而非余额，因此扣费只扣赤能源、IP 账本不增不减（D9，保证与物品本体一致）。
 * <ul>
 *     <li>存入：{@code ceil(IP × 1.005 / 4)}（含 0.5% 手续费）</li>
 *     <li>取出：{@code ceil(IP × 1.0025 / 4)}（含 0.25% 手续费）</li>
 * </ul>
 * <b>费率减免组件</b>（内腔功能件）：每格把上述费率降低 {@link #FEE_MODULE_STEP}%，最多
 * {@link #FEE_MODULE_MAX} 格（下限 −50%）；换算全程用整数比例，不引入浮点漂移。
 * <p>
 * 所有费率口径（含减免尺度的上限）集中在本类，终端与界面都只调这里，避免两处口径不一致。
 */
public final class ItemTerminalFee {

    /** 费率减免组件：每个降低的百分点 */
    public static final int FEE_MODULE_STEP = 10;
    /** 费率减免组件：生效上限（超出部分忽略，下限 −50%） */
    public static final int FEE_MODULE_MAX = 5;
    /** 减免换算的份额分母：费率按 10 份计，每份 {@link #FEE_MODULE_STEP}% */
    private static final long FEE_SCALE = 10L;

    /** 存入系数 1.005 / 4 */
    private static final long DEPOSIT_NUM = 1005L;
    private static final long DEPOSIT_DEN = 4_000L;
    /** 取出系数 1.0025 / 4 */
    private static final long WITHDRAW_NUM = 10025L;
    private static final long WITHDRAW_DEN = 40_000L;

    private ItemTerminalFee() {
    }

    /** 生效的减免格数（每格 1 份，钳到 [0, {@link #FEE_MODULE_MAX}]） */
    public static int effectiveModules(int feeModules) {
        return Math.max(0, Math.min(feeModules, FEE_MODULE_MAX));
    }

    /** 存入费用（赤能源）= ceil(IP × 1.005 / 4 × 减免系数) */
    public static long depositCost(long ip, int feeModules) {
        return scaledCeil(ip, DEPOSIT_NUM, DEPOSIT_DEN, effectiveModules(feeModules));
    }

    /** 取出费用（赤能源）= ceil(IP × 1.0025 / 4 × 减免系数) */
    public static long withdrawCost(long ip, int feeModules) {
        return scaledCeil(ip, WITHDRAW_NUM, WITHDRAW_DEN, effectiveModules(feeModules));
    }

    /**
     * 单笔可处理的最大 IP（能量约束）：费用 ≤ 缓冲 ⟺ {@code IP ≤ floor(缓冲 × 分母/分子)}。
     * <p>
     * 已并入费率减免（减免后同样缓冲能承担更大一笔）；实际单笔上限还须与单元剩余容量取 min。
     */
    public static long maxIp(long buffer, boolean deposit, int feeModules) {
        if (buffer <= 0L) {
            return 0L;
        }
        long num = (deposit ? DEPOSIT_NUM : WITHDRAW_NUM) * (FEE_SCALE - effectiveModules(feeModules));
        long den = (deposit ? DEPOSIT_DEN : WITHDRAW_DEN) * FEE_SCALE;
        if (buffer > Long.MAX_VALUE / den) {
            return Long.MAX_VALUE; // 缓冲极大时视作无能量约束，防乘法溢出
        }
        return buffer * den / num;
    }

    /** 带费率减免的向上取整换算；减免为 0 时走与基准完全相同的算式 */
    private static long scaledCeil(long ip, long num, long den, int modules) {
        if (ip <= 0L) {
            return 0L;
        }
        if (modules <= 0) {
            return ceilScale(ip, num, den);
        }
        long scaledNum = num * (FEE_SCALE - modules);
        long scaledDen = den * FEE_SCALE;
        if (ip > Long.MAX_VALUE / scaledNum) {
            return Long.MAX_VALUE;
        }
        return (ip * scaledNum + scaledDen - 1L) / scaledDen;
    }

    /** 向上取整的比例换算，防溢出保护（IP 上限 6.4e7，正常不触发） */
    private static long ceilScale(long ip, long num, long den) {
        if (ip <= 0L) {
            return 0L;
        }
        if (ip > Long.MAX_VALUE / num) {
            return Long.MAX_VALUE;
        }
        return (ip * num + den - 1L) / den;
    }
}
