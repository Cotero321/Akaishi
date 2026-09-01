package com.example.akaishi.fusion;

import com.example.akaishi.block.AkaishiFusionControllerBlock;
import com.example.akaishi.block.AkaishiFusionCoolerFrameBlock;
import com.example.akaishi.block.AkaishiFusionCoreBlock;
import com.example.akaishi.block.AkaishiFusionEfficiencyFrameBlock;
import com.example.akaishi.block.AkaishiFusionEnergyOutputBlock;
import com.example.akaishi.block.AkaishiFusionFuelFrameBlock;
import com.example.akaishi.block.AkaishiFusionInsulationBlock;
import com.example.akaishi.block.AkaishiFusionItemInputPortBlock;
import com.example.akaishi.block.AkaishiFusionItemOutputPortBlock;
import com.example.akaishi.block.AkaishiFusionShellBlock;
import com.example.akaishi.block.entity.AkaishiFusionCoolerFrameBlockEntity;
import com.example.akaishi.item.AkaishiFusionHeatSinkItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 聚变堆多方块结构扫描器（洋葱式边长 7 封闭箱体，独立于控制器便于测试复用）。
 * <p>
 * 层结构（相对控制器所在墙面枚举 6 种候选箱体）：
 * <ul>
 *   <li>外壳层：7×7×7 表面（218 格），必须为外壳/控制器/能量输出口/物品输入口/物品输出口</li>
 *   <li>隔热层：5×5×5 表面（98 格），必须全部为聚变隔热层</li>
 *   <li>框架层：3×3×3 表面（26 格），仅允许燃料/散热/效率框架与空气</li>
 *   <li>中心：聚变核心恰好 1 个</li>
 * </ul>
 * 框架上限：燃料 4 / 散热 10 / 效率 12；控制器必须位于外壳墙面。
 */
public final class FusionStructure {

    public static final int EDGE = 7;
    public static final int MAX_FUEL_FRAMES = 4;
    public static final int MAX_COOLER_FRAMES = 10;
    public static final int MAX_EFFICIENCY_FRAMES = 12;

    private FusionStructure() {
    }

    /** 一次扫描的不可变结果 */
    public static final class Result {
        public final BlockPos min, max;
        public final int fuelFrames;
        public final int efficiencyFrames;
        /** 全部散热框架位置（≤ {@link #MAX_COOLER_FRAMES}） */
        public final List<BlockPos> coolerFrames;
        /** Σ 散热片效率（%），未叠框架乘数 */
        public final int coolingPercent;
        /** 墙面能量输出口 */
        public final List<BlockPos> energyPorts;
        /** 墙面物品输入口 */
        public final List<BlockPos> itemInputPorts;
        /** 墙面物品输出口 */
        public final List<BlockPos> itemOutputPorts;

        Result(BlockPos min, BlockPos max, int fuelFrames, int efficiencyFrames,
               List<BlockPos> coolerFrames, int coolingPercent,
               List<BlockPos> energyPorts, List<BlockPos> itemInputPorts, List<BlockPos> itemOutputPorts) {
            this.min = min;
            this.max = max;
            this.fuelFrames = fuelFrames;
            this.efficiencyFrames = efficiencyFrames;
            this.coolerFrames = coolerFrames;
            this.coolingPercent = coolingPercent;
            this.energyPorts = energyPorts;
            this.itemInputPorts = itemInputPorts;
            this.itemOutputPorts = itemOutputPorts;
        }
    }

    /** 扫描以 controller 为墙面基准的结构，返回 {@link Result}；未成型返回 null */
    public static Result scan(Level level, BlockPos controller) {
        for (int axis = 0; axis < 3; axis++) {
            for (int dir : new int[]{-1, 1}) {
                Result r = verify(level, boxFor(controller, axis, dir));
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    /** 构造 6 种候选箱体：控制器位于 axis 轴 dir 方向墙面上（其余两轴以控制器为中心 ±3） */
    private static BlockPos[] boxFor(BlockPos c, int axis, int dir) {
        int minX = axis == 0 ? (dir < 0 ? c.getX() : c.getX() - (EDGE - 1)) : c.getX() - 3;
        int maxX = axis == 0 ? (dir < 0 ? c.getX() + (EDGE - 1) : c.getX()) : c.getX() + 3;
        int minY = axis == 1 ? (dir < 0 ? c.getY() : c.getY() - (EDGE - 1)) : c.getY() - 3;
        int maxY = axis == 1 ? (dir < 0 ? c.getY() + (EDGE - 1) : c.getY()) : c.getY() + 3;
        int minZ = axis == 2 ? (dir < 0 ? c.getZ() : c.getZ() - (EDGE - 1)) : c.getZ() - 3;
        int maxZ = axis == 2 ? (dir < 0 ? c.getZ() + (EDGE - 1) : c.getZ()) : c.getZ() + 3;
        return new BlockPos[]{new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ)};
    }

    /** 校验一组箱体边界：四层洋葱结构 + 各层合法性 + 数量上限 */
    private static Result verify(Level level, BlockPos[] box) {
        BlockPos min = box[0], max = box[1];
        int fuel = 0, efficiency = 0, cores = 0, cooling = 0;
        List<BlockPos> coolers = new ArrayList<>();
        List<BlockPos> energy = new ArrayList<>();
        List<BlockPos> itemIn = new ArrayList<>();
        List<BlockPos> itemOut = new ArrayList<>();
        // 中心：箱体正中
        BlockPos center = new BlockPos((min.getX() + max.getX()) / 2,
                (min.getY() + max.getY()) / 2, (min.getZ() + max.getZ()) / 2);
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    boolean shellLayer = x == min.getX() || x == max.getX()
                            || y == min.getY() || y == max.getY()
                            || z == min.getZ() || z == max.getZ();
                    boolean insLayer = !shellLayer && (x == min.getX() + 1 || x == max.getX() - 1
                            || y == min.getY() + 1 || y == max.getY() - 1
                            || z == min.getZ() + 1 || z == max.getZ() - 1);
                    boolean frameLayer = !shellLayer && !insLayer
                            && !(x == center.getX() && y == center.getY() && z == center.getZ());
                    BlockPos p = new BlockPos(x, y, z);
                    Block b = level.getBlockState(p).getBlock();
                    if (shellLayer) {
                        if (!isWallBlock(b)) {
                            return null; // 外壳缺口
                        }
                        if (b instanceof AkaishiFusionEnergyOutputBlock) {
                            energy.add(p);
                        } else if (b instanceof AkaishiFusionItemInputPortBlock) {
                            itemIn.add(p);
                        } else if (b instanceof AkaishiFusionItemOutputPortBlock) {
                            itemOut.add(p);
                        }
                    } else if (insLayer) {
                        if (!(b instanceof AkaishiFusionInsulationBlock)) {
                            return null; // 隔热层缺口/错误方块
                        }
                    } else if (frameLayer) {
                        if (level.getBlockState(p).isAir()) {
                            continue; // 框架层允许留空
                        }
                        if (b instanceof AkaishiFusionFuelFrameBlock) {
                            fuel++;
                        } else if (b instanceof AkaishiFusionCoolerFrameBlock) {
                            coolers.add(p);
                            cooling += AkaishiFusionCoolerFrameBlockEntity.getQualityAt(level, p);
                        } else if (b instanceof AkaishiFusionEfficiencyFrameBlock) {
                            efficiency++;
                        } else {
                            return null; // 框架层无关方块
                        }
                    } else {
                        // 中心
                        if (!(b instanceof AkaishiFusionCoreBlock)) {
                            return null;
                        }
                        cores++;
                    }
                }
            }
        }
        if (cores != 1 || fuel > MAX_FUEL_FRAMES || coolers.size() > MAX_COOLER_FRAMES
                || efficiency > MAX_EFFICIENCY_FRAMES) {
            return null;
        }
        return new Result(min, max, fuel, efficiency, coolers, cooling, energy, itemIn, itemOut);
    }

    /** 是否为合法墙面块（外壳层） */
    public static boolean isWallBlock(Block b) {
        return b instanceof AkaishiFusionShellBlock
                || b instanceof AkaishiFusionControllerBlock
                || b instanceof AkaishiFusionEnergyOutputBlock
                || b instanceof AkaishiFusionItemInputPortBlock
                || b instanceof AkaishiFusionItemOutputPortBlock;
    }
}
