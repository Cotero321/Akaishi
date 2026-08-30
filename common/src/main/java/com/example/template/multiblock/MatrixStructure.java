package com.example.template.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 类反应堆式封闭箱体多方块扫描器（发生器/提纯/生命转换矩阵共用）。
 * 控制器位于墙面上（不再是中心），以控制器为基准枚举候选箱体，
 * 验证「六面闭合 + 内腔全为空气」；尺寸为奇数边长（低级 3 / 高级 5）。
 */
public final class MatrixStructure {

    /** 墙块判定：由调用方提供，需包含外壳、各类端口以及控制器自身 */
    @FunctionalInterface
    public interface IsWall {
        boolean test(Block block);
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

    private MatrixStructure() {
    }

    /**
     * 以控制器位置为基准扫描封闭箱体；未成型返回 null。
     * size 为边长（3 或 5），控制器必须位于箱体表面。
     */
    public static Result scan(Level level, BlockPos controller, int size, IsWall isWall) {
        int r = size / 2;
        // 枚举控制器相对箱体中心的所有表面位置（至少一个坐标位于 ±r 边界）
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r && Math.abs(dz) != r) {
                        continue; // 控制器必须贴墙（落在表面上）
                    }
                    BlockPos center = controller.offset(-dx, -dy, -dz);
                    if (verify(level, center.offset(-r, -r, -r), center.offset(r, r, r), isWall)) {
                        return new Result(center.offset(-r, -r, -r), center.offset(r, r, r));
                    }
                }
            }
        }
        return null;
    }

    /** 校验：墙面 26/98 格全部为合法墙块，内腔（中心）必须全为空气 */
    private static boolean verify(Level level, BlockPos min, BlockPos max, IsWall isWall) {
        int minX = min.getX(), maxX = max.getX();
        int minY = min.getY(), maxY = max.getY();
        int minZ = min.getZ(), maxZ = max.getZ();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean onWall = x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
                    BlockPos p = new BlockPos(x, y, z);
                    if (onWall) {
                        if (!isWall.test(level.getBlockState(p).getBlock())) {
                            return false; // 墙面缺口
                        }
                    } else if (!level.getBlockState(p).isAir()) {
                        return false; // 内腔必须为空（矩阵无需内部部件）
                    }
                }
            }
        }
        return true;
    }
}
