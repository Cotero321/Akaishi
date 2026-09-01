package com.example.akaishi.multiblock;

import com.example.akaishi.block.AkaishiMinerFrameBlock;
import com.example.akaishi.block.AkaishiMinerPortBlock;
import com.example.akaishi.block.AkaishiMinerUpgradeBlock;
import com.example.akaishi.block.AkaishiMinerUpgradeFrameBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 赤石矿机固定结构检测（9×9×3，非火柴盒形式）：
 * 底层/顶层为十字形框架布局，中间层仅四边中点 + 中心控制器。
 * 布局数字：0=空气 1=矿机架构 2=矿机架构【矿机升级】/升级模块方块 3=控制器 4=矿机转口。
 * 控制器位于中间层中心 (4,4,1)，上下两层中心各放 1 个转口。
 */
public final class AkaishiMinerStructure {

    private static final int AIR = 0;
    private static final int FRAME = 1;
    private static final int UPGRADE_FRAME = 2;
    private static final int CONTROLLER = 3;
    private static final int PORT = 4;

    /** 底层 / 顶层布局（完全相同） */
    private static final int[][] LAYER = {
            {0, 0, 0, 0, 1, 0, 0, 0, 0},
            {0, 1, 2, 2, 1, 2, 2, 1, 0},
            {0, 2, 0, 0, 2, 0, 0, 2, 0},
            {0, 2, 0, 0, 2, 0, 0, 2, 0},
            {1, 1, 2, 2, 4, 2, 2, 1, 1},
            {0, 2, 0, 0, 2, 0, 0, 2, 0},
            {0, 2, 0, 0, 2, 0, 0, 2, 0},
            {0, 1, 2, 2, 1, 2, 2, 1, 0},
            {0, 0, 0, 0, 1, 0, 0, 0, 0}
    };

    /** 中间层布局（中心为控制器） */
    private static final int[][] LAYER_MIDDLE = {
            {0, 0, 0, 0, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {1, 0, 0, 0, 3, 0, 0, 0, 1},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, 0, 0, 0, 0}
    };

    /** y=0 底层 / y=1 中间层 / y=2 顶层 */
    private static final int[][][] LAYERS = {LAYER, LAYER_MIDDLE, LAYER};

    /** 扫描结果：升级框架位置（升级统计用）与转口位置（关联用） */
    public record Result(List<BlockPos> upgradeFrames, List<BlockPos> ports) {
    }

    private AkaishiMinerStructure() {
    }

    /**
     * 以控制器为锚点做全量结构校验。
     *
     * @return 校验通过返回升级框架/转口位置，否则 null
     */
    public static Result scan(Level level, BlockPos controller) {
        List<BlockPos> upgradeFrames = new ArrayList<>(48);
        List<BlockPos> ports = new ArrayList<>(2);
        for (int dy = 0; dy < 3; dy++) {
            for (int dz = 0; dz < 9; dz++) {
                for (int dx = 0; dx < 9; dx++) {
                    int expect = LAYERS[dy][dz][dx];
                    BlockPos p = controller.offset(dx - 4, dy - 1, dz - 4);
                    if (expect == CONTROLLER) {
                        // 只有自身在控制器位
                        if (!p.equals(controller)) {
                            return null;
                        }
                        continue;
                    }
                    Block b = level.getBlockState(p).getBlock();
                    switch (expect) {
                        case AIR -> {
                            if (!level.getBlockState(p).isAir()) {
                                return null;
                            }
                        }
                        case FRAME -> {
                            if (!(b instanceof AkaishiMinerFrameBlock)) {
                                return null;
                            }
                        }
                        case UPGRADE_FRAME -> {
                            // 升级框架位置：可以是空升级框架，也可以是已安装的升级模块方块
                            if (!(b instanceof AkaishiMinerUpgradeFrameBlock)
                                    && !(b instanceof AkaishiMinerUpgradeBlock)) {
                                return null;
                            }
                            upgradeFrames.add(p.immutable());
                        }
                        case PORT -> {
                            if (!(b instanceof AkaishiMinerPortBlock)) {
                                return null;
                            }
                            ports.add(p.immutable());
                        }
                        default -> {
                            return null;
                        }
                    }
                }
            }
        }
        return new Result(List.copyOf(upgradeFrames), List.copyOf(ports));
    }

    /** 成型后的箱体范围（解除转口关联时使用） */
    public static BlockPos minPos(BlockPos controller) {
        return controller.offset(-4, -1, -4);
    }

    public static BlockPos maxPos(BlockPos controller) {
        return controller.offset(4, 1, 4);
    }
}
