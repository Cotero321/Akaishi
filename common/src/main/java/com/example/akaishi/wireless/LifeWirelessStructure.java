package com.example.akaishi.wireless;

import com.example.akaishi.block.AkaishiLifeBlocks;
import com.example.akaishi.block.AkaishiLifeWirelessChunkLoaderBlock;
import com.example.akaishi.block.AkaishiLifeWirelessChunkRangeBlock;
import com.example.akaishi.block.AkaishiLifeWirelessDimBridgeBlock;
import com.example.akaishi.block.AkaishiLifeWirelessInputLossBlock;
import com.example.akaishi.block.AkaishiLifeWirelessOutputLossBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 生命能量无线终端多方块结构扫描器（固定边长 5；算法与红版 {@link WirelessTerminalStructure} 1:1 照搬）。
 * <p>
 * 结构约束：单层封闭 5×5×5 箱体；墙面仅由生命无线外壳/结构玻璃/终端/控制器四种方块组成，
 * 内腔中心恰好 1 个生命无线核心，其余可为空气或生命无线组件；组件分别提供跨维、区块加载、范围加载及输入/输出损耗抑制。
 * <p>
 * 算法：沿 3 轴双向数连续墙块得到箱体跨度；对无法从终端方块数出的轴（终端方块在该轴墙面内部）
 * 枚举边长（固定 5）与朝向的 4 种候选；对每个候选做「六面闭合 + 内腔合法」校验，首个通过者返回。
 * <p>
 * 注意：Result 字段名/类型与红版完全一致（{@link WirelessTerminalStructure.Result}），
 * 未来生命终端 BE 照抄红终端 BE 时依赖同名字段，勿改动。
 */
public final class LifeWirelessStructure {

    /** 固定外壳边长 */
    public static final int SIZE = 5;

    private LifeWirelessStructure() {
    }

    /** 一次扫描的不可变结果 */
    public static final class Result {
        public final BlockPos min, max;
        /** 外墙普通外壳 */
        public final List<BlockPos> shells;
        /** 内腔终端核心位置（恰好 1 个） */
        public final BlockPos terminalCore;
        /** 内腔终端跨维组件数量（生命族恒 0） */
        public final int crossDimCount;
        /** 内腔区块加载构架数量（生命族恒 0） */
        public final int chunkLoaderCount;
        /** 内腔区块加载扩展组件数量（生命族恒 0） */
        public final int chunkRangeCount;
        /** 内腔输入损耗抑制组件数量（生命族恒 0） */
        public final int inputLossCount;
        /** 内腔输出损耗抑制组件数量 */
        public final int outputLossCount;

        Result(BlockPos min, BlockPos max, List<BlockPos> shells, BlockPos terminalCore, int crossDimCount,
               int chunkLoaderCount, int chunkRangeCount, int inputLossCount, int outputLossCount) {
            this.min = min;
            this.max = max;
            this.shells = shells;
            this.terminalCore = terminalCore;
            this.crossDimCount = crossDimCount;
            this.chunkLoaderCount = chunkLoaderCount;
            this.chunkRangeCount = chunkRangeCount;
            this.inputLossCount = inputLossCount;
            this.outputLossCount = outputLossCount;
        }
    }

    /** 扫描以 terminal 位置为墙面基准的结构，未成型返回 null */
    public static Result scan(Level level, BlockPos terminal) {
        int[] neg = new int[3];
        int[] pos = new int[3];
        boolean[] solved = new boolean[3];
        int unsolved = -1;
        for (int axis = 0; axis < 3; axis++) {
            neg[axis] = wallExtent(level, terminal, axis, -1);
            pos[axis] = wallExtent(level, terminal, axis, 1);
            solved[axis] = neg[axis] + pos[axis] + 1 == SIZE;
            if (!solved[axis]) {
                if (unsolved != -1) {
                    return null; // 多于一轴不可解 → 结构必失效
                }
                unsolved = axis;
            }
        }

        int candidates = unsolved == -1 ? 1 : 4;
        for (int c = 0; c < candidates; c++) {
            int minX = terminal.getX() - neg[0], maxX = terminal.getX() + pos[0];
            int minY = terminal.getY() - neg[1], maxY = terminal.getY() + pos[1];
            int minZ = terminal.getZ() - neg[2], maxZ = terminal.getZ() + pos[2];
            if (unsolved != -1) {
                int dir = (c & 2) == 0 ? -1 : 1;
                if (unsolved == 0) {
                    minX = dir < 0 ? terminal.getX() - (SIZE - 1) : terminal.getX();
                    maxX = dir < 0 ? terminal.getX() : terminal.getX() + (SIZE - 1);
                } else if (unsolved == 1) {
                    minY = dir < 0 ? terminal.getY() - (SIZE - 1) : terminal.getY();
                    maxY = dir < 0 ? terminal.getY() : terminal.getY() + (SIZE - 1);
                } else {
                    minZ = dir < 0 ? terminal.getZ() - (SIZE - 1) : terminal.getZ();
                    maxZ = dir < 0 ? terminal.getZ() : terminal.getZ() + (SIZE - 1);
                }
            }
            Result r = verify(level, minX, maxX, minY, maxY, minZ, maxZ);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    /** 沿指定轴（0=x,1=y,2=z）与方向（-1/+1）数连续墙块，最多数到 SIZE-1 防止跨结构误判 */
    private static int wallExtent(Level level, BlockPos c, int axis, int dir) {
        BlockPos.MutableBlockPos p = c.mutable();
        int n = 0;
        while (n < SIZE - 1) {
            if (axis == 0) {
                p.move(dir, 0, 0);
            } else if (axis == 1) {
                p.move(0, dir, 0);
            } else {
                p.move(0, 0, dir);
            }
            if (!isWallBlock(level.getBlockState(p).getBlock())) {
                break;
            }
            n++;
        }
        return n;
    }

    /**
     * 校验一组箱体边界：六面闭合、内腔含恰好一个生命无线核心，其他位置为空气或合法组件。
     */
    private static Result verify(Level level, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        List<BlockPos> shells = new ArrayList<>();
        BlockPos core = null;
        int crossDimCount = 0;
        int chunkLoaderCount = 0;
        int chunkRangeCount = 0;
        int inputLossCount = 0;
        int outputLossCount = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean onWall = x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
                    BlockPos p = new BlockPos(x, y, z);
                    Block b = level.getBlockState(p).getBlock();
                    if (onWall) {
                        if (!isWallBlock(b)) {
                            return null; // 墙面缺口 → 不闭合
                        }
                        if (b == AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_SHELL.get()) {
                            shells.add(p);
                        }
                    } else if (level.getBlockState(p).isAir()) {
                        continue;
                    } else if (b == AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_CORE.get()) {
                        if (core != null) {
                            return null;
                        }
                        core = p;
                    } else if (b instanceof AkaishiLifeWirelessDimBridgeBlock) {
                        crossDimCount++;
                    } else if (b instanceof AkaishiLifeWirelessChunkLoaderBlock) {
                        chunkLoaderCount++;
                    } else if (b instanceof AkaishiLifeWirelessChunkRangeBlock) {
                        chunkRangeCount++;
                    } else if (b instanceof AkaishiLifeWirelessInputLossBlock) {
                        inputLossCount++;
                    } else if (b instanceof AkaishiLifeWirelessOutputLossBlock) {
                        outputLossCount++;
                    } else {
                        return null;
                    }
                }
            }
        }
        if (core == null) {
            return null;
        }
        return new Result(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ),
                shells, core, crossDimCount, chunkLoaderCount, chunkRangeCount, inputLossCount, outputLossCount);
    }

    /** 是否为合法墙面块（构成封闭壳体的方块）：生命无线终端/控制器/外壳/结构玻璃 */
    public static boolean isWallBlock(Block b) {
        return b == AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_TERMINAL.get()
                || b == AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_CONTROLLER.get()
                || b == AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_SHELL.get()
                || b == AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_STRUCTURE_GLASS.get();
    }
}
