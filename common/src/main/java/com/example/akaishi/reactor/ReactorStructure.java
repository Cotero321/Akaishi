package com.example.akaishi.reactor;

import com.example.akaishi.block.AkaishiReactorControllerBlock;
import com.example.akaishi.block.AkaishiReactorCoolerBlock;
import com.example.akaishi.block.AkaishiReactorCoreBlock;
import com.example.akaishi.block.AkaishiReactorEnergyOutputBlock;
import com.example.akaishi.block.AkaishiReactorFuelPortBlock;
import com.example.akaishi.block.AkaishiReactorFuelRodBlock;
import com.example.akaishi.block.AkaishiReactorShellBlock;
import com.example.akaishi.block.AkaishiReactorWastePortBlock;
import com.example.akaishi.block.entity.AkaishiReactorCoolerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 反应堆多方块结构扫描器（独立于控制器方块实体，便于测试与复用）。
 * <p>
 * 结构约束：单层封闭长方体，边长 ∈ [{@link #MIN_SIZE}, {@link #MAX_SIZE}]；墙面由
 * 外壳/控制器/燃料投放口/能量输出口/废品口组成，内腔仅允许燃料棒/散热组件/反应核心/空气。
 * 控制器本身是墙面的一部分，扫描以其位置为基准。
 * <p>
 * 算法：沿 3 轴双向数连续墙块得到箱体跨度；对无法从控制器数出的轴（控制器在该轴墙面内部）
 * 枚举边长与朝向的 4 种候选；对每个候选做「六面闭合 + 内腔合法」校验，首个通过者返回结果。
 */
public final class ReactorStructure {

    /** 外壳边长下限/上限 */
    public static final int MIN_SIZE = 5;
    public static final int MAX_SIZE = 7;
    /** 燃料棒（=燃料槽）数量上限 */
    public static final int MAX_RODS = 10;
    /** 散热组件数量上限 */
    public static final int MAX_COOLERS = 20;

    private ReactorStructure() {
    }

    /** 一次扫描的不可变结果，全部字段在验证通过后才提交 */
    public static final class Result {
        public final BlockPos min, max;
        public final int rodCount;
        public final int effectiveCoolers;
        public final int coolingPercent;
        /** 墙面上的能量输出口 */
        public final List<BlockPos> energyPorts;
        /** 墙面上的废品输出口 */
        public final List<BlockPos> wastePorts;
        /** 墙面上的燃料投放口 */
        public final List<BlockPos> fuelPorts;
        /** 墙面上的普通外壳 */
        public final List<BlockPos> shellPositions;
        /** 内腔的全部散热组件 */
        public final List<BlockPos> coolers;
        /** 内腔中贴邻燃料棒且装有散热片的有效散热组件 */
        public final List<BlockPos> effectiveCoolerPositions;

        Result(BlockPos min, BlockPos max, int rodCount, int effectiveCoolers, int coolingPercent,
               List<BlockPos> energyPorts, List<BlockPos> wastePorts, List<BlockPos> fuelPorts,
               List<BlockPos> shellPositions, List<BlockPos> coolers, List<BlockPos> effectiveCoolerPositions) {
            this.min = min;
            this.max = max;
            this.rodCount = rodCount;
            this.effectiveCoolers = effectiveCoolers;
            this.coolingPercent = coolingPercent;
            this.energyPorts = energyPorts;
            this.wastePorts = wastePorts;
            this.fuelPorts = fuelPorts;
            this.shellPositions = shellPositions;
            this.coolers = coolers;
            this.effectiveCoolerPositions = effectiveCoolerPositions;
        }
    }

    /**
     * 扫描以 controller 位置为墙面基准的结构，返回 {@link Result}；未成型返回 null。
     */
    public static Result scan(Level level, BlockPos controller) {
        int[] neg = new int[3];
        int[] pos = new int[3];
        boolean[] solved = new boolean[3];
        int unsolved = -1;
        for (int axis = 0; axis < 3; axis++) {
            neg[axis] = wallExtent(level, controller, axis, -1);
            pos[axis] = wallExtent(level, controller, axis, 1);
            int span = neg[axis] + pos[axis] + 1;
            solved[axis] = span >= MIN_SIZE && span <= MAX_SIZE;
            if (!solved[axis]) {
                if (unsolved != -1) {
                    return null; // 多于一轴不可解 → 结构必失效
                }
                unsolved = axis;
            }
        }

        int candidates = unsolved == -1 ? 1 : 4;
        for (int c = 0; c < candidates; c++) {
            int minX = controller.getX() - neg[0], maxX = controller.getX() + pos[0];
            int minY = controller.getY() - neg[1], maxY = controller.getY() + pos[1];
            int minZ = controller.getZ() - neg[2], maxZ = controller.getZ() + pos[2];
            if (unsolved != -1) {
                int size = (c & 1) == 0 ? MIN_SIZE : MAX_SIZE;
                int dir = (c & 2) == 0 ? -1 : 1;
                if (unsolved == 0) {
                    minX = dir < 0 ? controller.getX() - (size - 1) : controller.getX();
                    maxX = dir < 0 ? controller.getX() : controller.getX() + (size - 1);
                } else if (unsolved == 1) {
                    minY = dir < 0 ? controller.getY() - (size - 1) : controller.getY();
                    maxY = dir < 0 ? controller.getY() : controller.getY() + (size - 1);
                } else {
                    minZ = dir < 0 ? controller.getZ() - (size - 1) : controller.getZ();
                    maxZ = dir < 0 ? controller.getZ() : controller.getZ() + (size - 1);
                }
            }
            Result r = verify(level, minX, maxX, minY, maxY, minZ, maxZ);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    /** 沿指定轴（0=x,1=y,2=z）与方向（-1/+1）数连续墙块，最多数到 {@link #MAX_SIZE} 防止跨结构误判 */
    private static int wallExtent(Level level, BlockPos c, int axis, int dir) {
        BlockPos.MutableBlockPos p = c.mutable();
        int n = 0;
        while (n < MAX_SIZE) {
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

    /** 校验一组箱体边界：六面闭合、内腔仅含合法部件、核心恰 1 个且数量不超上限 */
    private static Result verify(Level level, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        int rods = 0, coolers = 0, cores = 0, cooling = 0, coolersEffective = 0;
        List<BlockPos> energy = new ArrayList<>();
        List<BlockPos> waste = new ArrayList<>();
        List<BlockPos> fuel = new ArrayList<>();
        List<BlockPos> shells = new ArrayList<>();
        List<BlockPos> coolerList = new ArrayList<>();
        List<BlockPos> coolerEff = new ArrayList<>();
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
                        if (b instanceof AkaishiReactorEnergyOutputBlock) {
                            energy.add(p);
                        } else if (b instanceof AkaishiReactorWastePortBlock) {
                            waste.add(p);
                        } else if (b instanceof AkaishiReactorFuelPortBlock) {
                            fuel.add(p);
                        } else if (b instanceof AkaishiReactorShellBlock) {
                            shells.add(p);
                        }
                    } else if (level.getBlockState(p).isAir()) {
                        continue;
                    } else if (b instanceof AkaishiReactorFuelRodBlock) {
                        rods++;
                    } else if (b instanceof AkaishiReactorCoolerBlock) {
                        coolers++;
                        coolerList.add(p);
                        if (AkaishiReactorCoolerBlockEntity.hasAdjacentFuelRod(level, p)) {
                            int q = AkaishiReactorCoolerBlockEntity.getQualityAt(level, p);
                            if (q > 0) {
                                cooling += q;
                                coolersEffective++;
                                coolerEff.add(p);
                            }
                        }
                    } else if (b instanceof AkaishiReactorCoreBlock) {
                        cores++;
                    } else {
                        return null; // 内腔存在无关方块
                    }
                }
            }
        }
        if (cores != 1 || rods > MAX_RODS || coolers > MAX_COOLERS) {
            return null;
        }
        return new Result(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ),
                rods, coolersEffective, cooling, energy, waste, fuel, shells, coolerList, coolerEff);
    }

    /** 是否为合法墙面块（构成封闭壳体的方块） */
    public static boolean isWallBlock(Block b) {
        return b instanceof AkaishiReactorShellBlock
                || b instanceof AkaishiReactorControllerBlock
                || b instanceof AkaishiReactorFuelPortBlock
                || b instanceof AkaishiReactorEnergyOutputBlock
                || b instanceof AkaishiReactorWastePortBlock;
    }
}
