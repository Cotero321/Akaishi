package com.example.akaishi.multiblock;

import com.example.akaishi.block.AkaishiItemStorageUnitBlock;
import com.example.akaishi.block.AkaishiItemTerminalBlock;
import com.example.akaishi.block.AkaishiItemTerminalBlocks;
import com.example.akaishi.block.AkaishiItemTerminalBufferModuleBlock;
import com.example.akaishi.block.AkaishiItemTerminalChunkLoaderBlock;
import com.example.akaishi.block.AkaishiItemTerminalChunkRangeBlock;
import com.example.akaishi.block.AkaishiItemTerminalCoreBlock;
import com.example.akaishi.block.AkaishiItemTerminalEnergyInputPortBlock;
import com.example.akaishi.block.AkaishiItemTerminalFeeModuleBlock;
import com.example.akaishi.block.AkaishiItemTerminalShellBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 物品终端多方块结构扫描器（固定边长 5）：物品终端<b>自有</b>的结构，与无线赤能源终端完全隔离。
 * <p>
 * 结构约束：
 * <ul>
 *   <li>单层封闭 5×5×5 箱体，壁厚 1，内腔 3×3×3；</li>
 *   <li>墙面只认本族方块：物品终端本体（<b>恰好 1 个</b>）+ 物品终端外壳 + 物品终端结构玻璃
 *       + 贴装件（各阶物品储存单元、赤能源接入口 —— 它们也可以直接镶嵌在墙上，见 {@link #isWallBlock}）。
 *       无线族的外壳/玻璃/安全块/控制器/核心一律不合法，反向亦已把物品终端移出无线白名单，
 *       因此两个体系不可能共用同一个箱体；</li>
 *   <li>限制「恰好 1 个物品终端」的原因：多个终端会扫到同一箱体、共享同一批储存单元，
 *       隔离要求下不允许这种隐式共享；</li>
 *   <li>内腔恰好 1 个物品终端核心，其余为空气或<b>内腔功能件</b>（缓冲扩展 / 费率减免 /
 *       区块加载构架 / 区块加载扩展；登记点见 {@link #verify}）。</li>
 * </ul>
 * 算法沿用无线终端结构扫描器：沿 3 轴双向数连续墙块得到箱体跨度；对无法从终端方块数出的轴
 * （终端方块位于该轴墙面内部）枚举朝向候选，首个通过校验者即为成型结果。
 */
public final class ItemTerminalStructure {

    /** 固定外壳边长 */
    public static final int SIZE = 5;

    private ItemTerminalStructure() {
    }

    /** 一次扫描的不可变结果：箱体范围 + 内腔各组件的原始计数（上限由消费方按各自常量钳制） */
    public static final class Result {
        public final BlockPos min;
        public final BlockPos max;
        /** 内腔缓冲扩展组件数量 */
        public final int bufferModuleCount;
        /** 内腔费率减免组件数量 */
        public final int feeModuleCount;
        /** 内腔区块加载构架数量（≥1 生效） */
        public final int chunkLoaderCount;
        /** 内腔区块加载扩展组件数量（≥1 把弱加载范围扩为 3×3） */
        public final int chunkRangeCount;

        Result(BlockPos min, BlockPos max, int bufferModuleCount, int feeModuleCount,
               int chunkLoaderCount, int chunkRangeCount) {
            this.min = min;
            this.max = max;
            this.bufferModuleCount = bufferModuleCount;
            this.feeModuleCount = feeModuleCount;
            this.chunkLoaderCount = chunkLoaderCount;
            this.chunkRangeCount = chunkRangeCount;
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
            int minX = terminal.getX() - neg[0];
            int maxX = terminal.getX() + pos[0];
            int minY = terminal.getY() - neg[1];
            int maxY = terminal.getY() + pos[1];
            int minZ = terminal.getZ() - neg[2];
            int maxZ = terminal.getZ() + pos[2];
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
     * 校验一组箱体边界：六面闭合、墙面恰好 1 个物品终端、内腔恰好 1 个核心且其余为空气。
     * <p>
     * <b>内腔功能件登记点</b>：目前登记 4 类（缓冲扩展 / 费率减免 / 区块加载构架 / 区块加载扩展），
     * 只计数不判成败；后续新增组件只需在此追加一个计数分支，墙面白名单无需改动。
     */
    private static Result verify(Level level, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        BlockPos core = null;
        int terminalCount = 0;
        int bufferModuleCount = 0;
        int feeModuleCount = 0;
        int chunkLoaderCount = 0;
        int chunkRangeCount = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean onWall = x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
                    BlockPos p = new BlockPos(x, y, z);
                    Block b = level.getBlockState(p).getBlock();
                    if (onWall) {
                        if (!isWallBlock(b)) {
                            return null; // 墙面缺口或混入了异族方块 → 不闭合 / 不隔离
                        }
                        if (b instanceof AkaishiItemTerminalBlock && ++terminalCount > 1) {
                            return null; // 多于一个物品终端 → 拒绝，避免多终端共享同一批储存单元
                        }
                    } else if (level.getBlockState(p).isAir()) {
                        continue;
                    } else if (b instanceof AkaishiItemTerminalCoreBlock) {
                        if (core != null) {
                            return null; // 内腔多于一个核心
                        }
                        core = p;
                    } else if (b instanceof AkaishiItemTerminalBufferModuleBlock) {
                        bufferModuleCount++;
                    } else if (b instanceof AkaishiItemTerminalFeeModuleBlock) {
                        feeModuleCount++;
                    } else if (b instanceof AkaishiItemTerminalChunkLoaderBlock) {
                        chunkLoaderCount++;
                    } else if (b instanceof AkaishiItemTerminalChunkRangeBlock) {
                        chunkRangeCount++;
                    } else {
                        return null; // 内腔存在无关方块
                    }
                }
            }
        }
        if (core == null || terminalCount != 1) {
            return null;
        }
        return new Result(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ),
                bufferModuleCount, feeModuleCount, chunkLoaderCount, chunkRangeCount);
    }

    /**
     * 是否为物品终端族的合法墙面块（本族专属，不含任何无线族方块）。
     * <p>
     * 除外壳/玻璃外，<b>贴装件也合法</b>：储存单元与赤能源接入口既能贴在外侧 1 格，也能直接镶嵌进墙面
     * —— 玩家的直觉就是"把口装到结构上"，若不允许上墙，一装就把结构判失效，且终端也读不到它。
     */
    private static boolean isWallBlock(Block b) {
        return b instanceof AkaishiItemTerminalBlock
                || b instanceof AkaishiItemTerminalShellBlock
                || b instanceof AkaishiItemStorageUnitBlock
                || b instanceof AkaishiItemTerminalEnergyInputPortBlock
                || b == AkaishiItemTerminalBlocks.CHISHI_ITEM_TERMINAL_STRUCTURE_GLASS.get();
    }
}
