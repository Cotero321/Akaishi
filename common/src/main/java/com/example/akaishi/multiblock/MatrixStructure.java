package com.example.akaishi.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

/**
 * 类反应堆式封闭箱体多方块扫描器（发生器/提纯/生命转换矩阵共用）。
 * 控制器位于墙面上（不再是中心），以控制器为基准枚举候选箱体，
 * 验证「六面闭合 + 内腔合规」；尺寸为奇数边长（低级 3 / 高级 5）。
 */
public final class MatrixStructure {

    /** 诊断时每个箱体最多回报几处不合规（够定位即可，避免刷屏） */
    private static final int MAX_REPORT = 3;

    /** 墙块判定：由调用方提供，需包含外壳、各类端口以及控制器自身 */
    @FunctionalInterface
    public interface IsWall {
        boolean test(Block block);
    }

    /**
     * 内腔格判定：默认只允许空气；需要容纳内腔部件（终端升级组件等）的结构
     * 可传入放行谓词，从而在不放宽墙面校验的前提下允许指定方块入腔。
     */
    @FunctionalInterface
    public interface IsInterior {
        boolean test(Level level, BlockPos pos);
    }

    /** 一次成功扫描的箱体范围 */
    public static final class Result {
        public final BlockPos min;
        public final BlockPos max;

        Result(BlockPos min, BlockPos max) {
            this.min = min;
            this.max = max;
        }
    }

    /** 一处不合规格：坐标 + 它在墙面上还是内腔里（决定给玩家的提示语） */
    public static final class Problem {
        public final BlockPos pos;
        /** true = 墙面缺口；false = 内腔有未放行方块 */
        public final boolean wall;

        Problem(BlockPos pos, boolean wall) {
            this.pos = pos;
            this.wall = wall;
        }
    }

    /** 诊断结果：被评估的箱体 + 不合规总数 + 前 {@link #MAX_REPORT} 处问题格 */
    public static final class Failure {
        public final BlockPos min;
        public final BlockPos max;
        /** 该箱体的不合规格总数（可能大于 {@code problems.size()}） */
        public final int total;
        public final List<Problem> problems;

        Failure(BlockPos min, BlockPos max, int total, List<Problem> problems) {
            this.min = min;
            this.max = max;
            this.total = total;
            this.problems = problems;
        }
    }

    private MatrixStructure() {
    }

    /** 以控制器位置为基准扫描封闭箱体（内腔必须全为空气）；未成型返回 null */
    public static Result scan(Level level, BlockPos controller, int size, IsWall isWall) {
        return scan(level, controller, size, isWall, (lvl, pos) -> lvl.getBlockState(pos).isAir());
    }

    /**
     * 以控制器位置为基准扫描封闭箱体；未成型返回 null。
     * size 为边长（3 或 5），控制器必须位于箱体表面。
     */
    public static Result scan(Level level, BlockPos controller, int size, IsWall isWall, IsInterior isInterior) {
        int r = size / 2;
        // 枚举控制器相对箱体中心的所有表面位置（至少一个坐标位于 ±r 边界）
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r && Math.abs(dz) != r) {
                        continue; // 控制器必须贴墙（落在表面上）
                    }
                    BlockPos center = controller.offset(-dx, -dy, -dz);
                    if (verify(level, center.offset(-r, -r, -r), center.offset(r, r, r), isWall, isInterior)) {
                        return new Result(center.offset(-r, -r, -r), center.offset(r, r, r));
                    }
                }
            }
        }
        return null;
    }

    /**
     * 失败诊断：未成型时指出<b>最接近成型的那套箱体</b>及其不合规格子，用于「为什么不成型」的可读回执。
     * <p>
     * <b>为什么不是「第一个候选」</b>：候选枚举从「控制器位于箱体最小角」开始，那套箱体通常不是玩家搭的那套，
     * 直接报它会指向一个无关坐标。这里取<b>不合规格数最少</b>的候选 —— 玩家真正搭的箱体必然最接近成型，
     * 于是回报的箱体范围与问题格才有定位价值。
     * <p>
     * <b>只在玩家主动询问时调用</b>（右键 / 命令）：最坏要走完 98 个候选面 × 全箱格，
     * 不进每 {@code RESCAN_INTERVAL} tick 的重扫路径。
     * <p>
     * 只评估<b>整箱已加载</b>的候选：诊断不该为了一次提示去同步加载/生成区块
     * （与场域侧「绝不触发同步加载」同纪律）。
     *
     * @return 已成型的场合返回 null；未成型返回最接近成型的那套箱体的不合规详情
     */
    public static Failure diagnose(Level level, BlockPos controller, int size, IsWall isWall,
            IsInterior isInterior) {
        if (scan(level, controller, size, isWall, isInterior) != null) {
            return null; // 已成型：无须诊断
        }
        int r = size / 2;
        Failure best = null;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r && Math.abs(dz) != r) {
                        continue;
                    }
                    BlockPos center = controller.offset(-dx, -dy, -dz);
                    BlockPos min = center.offset(-r, -r, -r);
                    BlockPos max = center.offset(r, r, r);
                    if (!isLoaded(level, min, max)) {
                        continue;
                    }
                    Failure failure = inspect(level, min, max, isWall, isInterior, false);
                    if (failure == null) {
                        return null; // 与 scan 判定不一致的兜底（理论上 scan 已拦下）
                    }
                    if (best == null || failure.total < best.total) {
                        best = failure;
                    }
                }
            }
        }
        return best;
    }

    /** 整箱覆盖的区块是否都已加载（不触发任何加载/生成） */
    private static boolean isLoaded(Level level, BlockPos min, BlockPos max) {
        ChunkSource source = level.getChunkSource();
        ChunkPos from = new ChunkPos(min);
        ChunkPos to = new ChunkPos(max);
        for (int cx = from.x; cx <= to.x; cx++) {
            for (int cz = from.z; cz <= to.z; cz++) {
                if (source.getChunkNow(cx, cz) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 校验：墙面 26/98 格全部为合法墙块，内腔按 {@code isInterior} 逐格判定 */
    private static boolean verify(Level level, BlockPos min, BlockPos max, IsWall isWall,
            IsInterior isInterior) {
        // 重扫路径要快：找到第一处不合规就收工，不做全箱计数
        return inspect(level, min, max, isWall, isInterior, true) == null;
    }

    /**
     * 遍历箱体逐格判定：墙面须为合法墙块，内腔须被放行。
     *
     * @param stopAtFirst true = 只找首处（tick 路径的快校验）；false = 全箱计数并留前 {@link #MAX_REPORT} 处
     * @return 全部合规返回 null
     */
    private static Failure inspect(Level level, BlockPos min, BlockPos max, IsWall isWall,
            IsInterior isInterior, boolean stopAtFirst) {
        int minX = min.getX(), maxX = max.getX();
        int minY = min.getY(), maxY = max.getY();
        int minZ = min.getZ(), maxZ = max.getZ();
        List<Problem> problems = new ArrayList<>();
        int total = 0;
        box:
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean onWall = x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
                    BlockPos p = new BlockPos(x, y, z);
                    boolean bad = onWall
                            ? !isWall.test(level.getBlockState(p).getBlock())
                            : !isInterior.test(level, p);
                    if (bad) {
                        total++;
                        if (problems.size() < MAX_REPORT) {
                            problems.add(new Problem(p, onWall));
                        }
                        if (stopAtFirst) {
                            break box;
                        }
                    }
                }
            }
        }
        return total == 0 ? null : new Failure(min, max, total, problems);
    }
}
