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
import com.example.akaishi.block.AkaishiFusionBlocks;
import com.example.akaishi.block.ModBlocks;
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
        /** 全部散热框架位置（≤ {@link #MAX_COOLER_FRAMES}）；框架仅决定可用散热片槽位数量，散热片存放于控制器 */
        public final List<BlockPos> coolerFrames;
        /** 墙面能量输出口 */
        public final List<BlockPos> energyPorts;
        /** 墙面物品输入口 */
        public final List<BlockPos> itemInputPorts;
        /** 墙面物品输出口 */
        public final List<BlockPos> itemOutputPorts;

        Result(BlockPos min, BlockPos max, int fuelFrames, int efficiencyFrames,
               List<BlockPos> coolerFrames,
               List<BlockPos> energyPorts, List<BlockPos> itemInputPorts, List<BlockPos> itemOutputPorts) {
            this.min = min;
            this.max = max;
            this.fuelFrames = fuelFrames;
            this.efficiencyFrames = efficiencyFrames;
            this.coolerFrames = coolerFrames;
            this.energyPorts = energyPorts;
            this.itemInputPorts = itemInputPorts;
            this.itemOutputPorts = itemOutputPorts;
        }
    }

    /**
     * 扫描以 controller 为墙面件的 7×7×7 结构，返回 {@link Result}；未成型返回 null。
     * <p>
     * 与反应堆同源算法：沿 3 轴双向数「连续墙面块」定位盒体（固定边长 {@link #EDGE}）。
     * 控制器可以位于任意外墙面的任意一格（含棱、角），不再要求墙面中心：
     * 面内两轴可直接数满定界，控制器法线轴（墙的里外方向）无法直接数出，枚举 2 种盒体朝向校验。
     */
    public static Result scan(Level level, BlockPos controller) {
        int[] neg = new int[3];
        int[] pos = new int[3];
        int unsolved = -1;
        for (int axis = 0; axis < 3; axis++) {
            neg[axis] = wallExtent(level, controller, axis, -1);
            pos[axis] = wallExtent(level, controller, axis, 1);
            if (neg[axis] + pos[axis] + 1 != EDGE) {
                if (unsolved != -1) {
                    return null; // 多于一轴无法闭合 → 结构必失效
                }
                unsolved = axis;
            }
        }
        int candidates = unsolved == -1 ? 1 : 2;
        for (int c = 0; c < candidates; c++) {
            int dir = c == 0 ? -1 : 1;
            int minX = controller.getX() - neg[0], maxX = controller.getX() + pos[0];
            int minY = controller.getY() - neg[1], maxY = controller.getY() + pos[1];
            int minZ = controller.getZ() - neg[2], maxZ = controller.getZ() + pos[2];
            if (unsolved == 0) {
                minX = dir < 0 ? controller.getX() - (EDGE - 1) : controller.getX();
                maxX = dir < 0 ? controller.getX() : controller.getX() + (EDGE - 1);
            } else if (unsolved == 1) {
                minY = dir < 0 ? controller.getY() - (EDGE - 1) : controller.getY();
                maxY = dir < 0 ? controller.getY() : controller.getY() + (EDGE - 1);
            } else if (unsolved == 2) {
                minZ = dir < 0 ? controller.getZ() - (EDGE - 1) : controller.getZ();
                maxZ = dir < 0 ? controller.getZ() : controller.getZ() + (EDGE - 1);
            }
            Result r = verify(level, new BlockPos[]{new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ)});
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    /** 沿指定轴（0=x,1=y,2=z）与方向（-1/+1）数连续墙面块（控制器所在墙面上的延伸），最多数到盒边长 */
    private static int wallExtent(Level level, BlockPos c, int axis, int dir) {
        BlockPos.MutableBlockPos p = c.mutable();
        int n = 0;
        while (n < EDGE) {
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

    /** 校验一组箱体边界：四层洋葱结构 + 各层合法性 + 数量上限 */
    private static Result verify(Level level, BlockPos[] box) {
        BlockPos min = box[0], max = box[1];
        int fuel = 0, efficiency = 0, cores = 0;
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
                            coolers.add(p); // 散热框架只作槽位计数，散热片统一存放在控制器
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
        return new Result(min, max, fuel, efficiency, coolers, energy, itemIn, itemOut);
    }

    /** 是否为合法墙面块（外壳层）：外壳 / 控制器 / 端口 / 结构玻璃 */
    public static boolean isWallBlock(Block b) {
        return b instanceof AkaishiFusionShellBlock
                || b instanceof AkaishiFusionControllerBlock
                || b instanceof AkaishiFusionEnergyOutputBlock
                || b instanceof AkaishiFusionItemInputPortBlock
                || b instanceof AkaishiFusionItemOutputPortBlock
                || b == AkaishiFusionBlocks.CHISHI_FUSION_STRUCTURE_GLASS.get();
    }
}
