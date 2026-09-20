package com.example.akaishi.craft;

import net.minecraft.world.item.crafting.Recipe;

import java.util.List;

/**
 * 同族机台的活件调度：把一次订单要跑的加工次数按<b>各机台的真实吞吐</b>摊给场域内多台同族机台。
 *
 * <p><b>用户口径（2026-09-19）</b>：件数超过 64 就挪到下一台相同机器；件数小于 64、但场域里
 * 有好几台时<b>同样要一起分担</b>（不许只让一台干活、其余闲着）。
 *
 * <p><b>分摊方式：按吞吐比例</b>（而不是"先切成每份 64 再分轮"）。设第 i 台的"每件耗时"为
 * {@code t_i}，吞吐 {@code r_i = 1/t_i}：
 * <ul>
 *   <li>每台分到的次数 {@code n_i ≈ 次数 × r_i / Σr}，余数补给"补完后墙钟仍最小"的那台；</li>
 *   <li>墙钟 = {@code max(n_i × t_i)}（多台同时跑，最晚完工的那台说了算）；</li>
 *   <li>能耗 = {@code Σ n_i × 该台单件能耗}（按各机自己的单件能耗结算：
 *       各机升级相同时与单机总量完全一致；升级不同时，把活多分给快机<b>同时省时间也省电</b>，
 *       因为提速让单件总能耗下降 —— 与 {@link MachineProcessEnergy} 的算式同源）。</li>
 * </ul>
 *
 * <p><b>为什么不用"每份 64 次分轮"</b>：那是一刀切 —— 300 件 / 3 台会切成 5 份（64,64,64,64,44）
 * 分两轮跑，墙钟 2×64 件的时间；按吞吐比例直接给每台 100 件，墙钟 100 件的时间，明显更快；
 * 件数 &lt; 64 时"一刀切"更是只会分出 1 份 ⇒ 只动 1 台机台，其余全闲着（旧实现的 bug）。
 * 装了速度升级的机台吞吐更高，按比例自然多分活 —— 这就是"按内部升级进一步优化"。
 *
 * <p><b>件数拆不开</b>：1 次加工是不可分的，1 件活只能落在 1 台上。
 */
public final class MachineScheduler {

    /** 一次调度结果：<b>墙钟</b>耗时（并行后玩家实际等待的 tick）+ 总机器成本 */
    public record Workout(long wallTicks, MachineProcessEnergy.Cost cost) {

        public static final Workout NONE = new Workout(0L, MachineProcessEnergy.Cost.ZERO);
    }

    private MachineScheduler() {
    }

    /**
     * 把 {@code batches} 次加工摊给同族机台。
     *
     * @param machines 该族场域内的机台（空表按"一台无升级机台"处理，保证总有可算的成本）
     */
    public static Workout schedule(Recipe<?> recipe, List<MachineSpec> machines, long batches) {
        if (recipe == null || batches <= 0L) {
            return Workout.NONE;
        }
        List<MachineSpec> pool = machines == null || machines.isEmpty() ? List.of(MachineSpec.BASE) : machines;
        int count = pool.size();
        long[] perBatchTicks = new long[count];
        long[] perBatchChishi = new long[count];
        long[] perBatchLife = new long[count];
        double totalRate = 0.0D;
        for (int i = 0; i < count; i++) {
            MachineSpec spec = pool.get(i);
            perBatchTicks[i] = MachineProcessEnergy.ticksPerRun(recipe, spec);
            MachineProcessEnergy.Cost perRun = MachineProcessEnergy.costPerRun(recipe, spec);
            perBatchChishi[i] = perRun.chishi();
            perBatchLife[i] = perRun.life();
            if (perBatchTicks[i] > 0L) {
                totalRate += 1.0D / perBatchTicks[i];
            }
        }
        if (totalRate <= 0.0D) {
            // 全是"充能满即产出"型（聚合器）：没有可并行的时长，能耗按台数无关的量算一遍即可
            MachineProcessEnergy.Cost perRun = MachineProcessEnergy.costPerRun(recipe, pool.get(0));
            return new Workout(0L, perRun.times(batches));
        }
        return allocate(batches, count, perBatchTicks, perBatchChishi, perBatchLife, totalRate);
    }

    /**
     * 按吞吐比例分摊并结算。
     * <p>先按比例取整（快机分得多），余数再逐个补给"补完之后墙钟仍最小"的那台 ——
     * 比"看小数部分"更直接：目标是让最晚完工的那台尽早收工。
     */
    private static Workout allocate(long batches, int count, long[] perBatchTicks,
            long[] perBatchChishi, long[] perBatchLife, double totalRate) {
        long[] assigned = new long[count];
        long used = 0L;
        for (int i = 0; i < count; i++) {
            if (perBatchTicks[i] <= 0L) {
                continue; // 瞬时型机台不参与分摊（其耗时不随件数增长）
            }
            double exact = batches * (1.0D / perBatchTicks[i]) / totalRate;
            assigned[i] = (long) Math.floor(exact);
            used += assigned[i];
        }
        long remainder = batches - used;
        while (remainder > 0L) {
            int pick = -1;
            long bestFinish = Long.MAX_VALUE;
            for (int i = 0; i < count; i++) {
                if (perBatchTicks[i] <= 0L) {
                    continue;
                }
                long finish = MachineProcessEnergy.saturatingMultiply(assigned[i] + 1L, perBatchTicks[i]);
                if (finish < bestFinish) {
                    bestFinish = finish;
                    pick = i;
                }
            }
            if (pick < 0) {
                break; // 理论上不可达：totalRate > 0 至少有一台可承接
            }
            assigned[pick]++;
            remainder--;
        }
        long wall = 0L;
        long chishi = 0L;
        long life = 0L;
        for (int i = 0; i < count; i++) {
            long n = assigned[i];
            if (n <= 0L) {
                continue;
            }
            wall = Math.max(wall, MachineProcessEnergy.saturatingMultiply(n, perBatchTicks[i]));
            chishi = MachineProcessEnergy.saturatingAdd(chishi,
                    MachineProcessEnergy.saturatingMultiply(n, perBatchChishi[i]));
            life = MachineProcessEnergy.saturatingAdd(life,
                    MachineProcessEnergy.saturatingMultiply(n, perBatchLife[i]));
        }
        return new Workout(wall, new MachineProcessEnergy.Cost(chishi, life));
    }
}
