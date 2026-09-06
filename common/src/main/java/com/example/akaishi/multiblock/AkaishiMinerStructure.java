package com.example.akaishi.multiblock;

import com.example.akaishi.block.AkaishiMinerDrillBitBlock;
import com.example.akaishi.block.AkaishiMinerEnergyInputBlock;
import com.example.akaishi.block.AkaishiMinerExternalFrameBlock;
import com.example.akaishi.block.AkaishiMinerFrameBlock;
import com.example.akaishi.block.AkaishiMinerItemOutputBlock;
import com.example.akaishi.block.AkaishiMinerPortBlock;
import com.example.akaishi.block.AkaishiMinerUpgradeBlock;
import com.example.akaishi.block.AkaishiMinerUpgradeFrameBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 赤石矿机固定结构检测（9×9×5 镂空塔）。
 * <p>
 * 上下两层（dy0 底层 / dy4 顶层）为完整十字底板，各含 24 个升级框架位（合计 48）；
 * 中间三层（dy1..dy3）镂空，仅保留四根立柱与中心柱，其余全部为宽松开放位。
 * <ul>
 *   <li>中心柱（自下而上 dy0..dy4）：钻机钻头 → 矿机架构 → 控制器 → 矿机架构 → 矿机转口；</li>
 *   <li>四根边立柱（东西南北最外端，每根贯穿 5 层）：每格允许 矿机架构 / 矿机架构【外接】 /
 *       矿机能量输入口 / 矿机物品输出口 任选；其中后三者（架构【外接】/输入口/输出口）互通，
 *       同属「外接块」，每根立柱合计最多 1 个（可挂任意高度），其余柱格必须放普通架构；</li>
 *   <li>开放位宽松：不校验任何内容，玩家在空格填充方块不会导致结构解体。</li>
 * </ul>
 * 布局数字：0=空气(宽松) 1=矿机架构 2=升级框架/升级模块 5=立柱位 6=中心柱位。
 */
public final class AkaishiMinerStructure {

    private static final int AIR = 0;
    private static final int FRAME = 1;
    private static final int UPGRADE = 2;
    private static final int PILLAR = 5;
    private static final int CENTER = 6;

    /** 垂直层数（y 方向）与水平边长 */
    public static final int HEIGHT = 5;
    public static final int SIZE = 9;
    /** 上下两片完整十字层各含的升级框架位数量；全机合计 48（中间镂空层无升级位） */
    public static final int UPGRADE_PER_DECK = 24;
    public static final int UPGRADE_TOTAL = UPGRADE_PER_DECK * 2;

    /** 完整十字层模板（dy0 底层 / dy4 顶层使用），含 24 个升级框架位 */
    private static final int[][] DECK_LAYER = {
            {AIR, AIR, AIR, AIR, PILLAR, AIR, AIR, AIR, AIR},
            {AIR, FRAME, UPGRADE, UPGRADE, FRAME, UPGRADE, UPGRADE, FRAME, AIR},
            {AIR, UPGRADE, AIR, AIR, UPGRADE, AIR, AIR, UPGRADE, AIR},
            {AIR, UPGRADE, AIR, AIR, UPGRADE, AIR, AIR, UPGRADE, AIR},
            {PILLAR, FRAME, UPGRADE, UPGRADE, CENTER, UPGRADE, UPGRADE, FRAME, PILLAR},
            {AIR, UPGRADE, AIR, AIR, UPGRADE, AIR, AIR, UPGRADE, AIR},
            {AIR, UPGRADE, AIR, AIR, UPGRADE, AIR, AIR, UPGRADE, AIR},
            {AIR, FRAME, UPGRADE, UPGRADE, FRAME, UPGRADE, UPGRADE, FRAME, AIR},
            {AIR, AIR, AIR, AIR, PILLAR, AIR, AIR, AIR, AIR}
    };

    /** 镂空层模板（dy1..dy3 使用）：仅四根立柱与中心柱，其余全为宽松空气 */
    private static final int[][] HOLLOW_LAYER = {
            {AIR, AIR, AIR, AIR, PILLAR, AIR, AIR, AIR, AIR},
            {AIR, AIR, AIR, AIR, AIR, AIR, AIR, AIR, AIR},
            {AIR, AIR, AIR, AIR, AIR, AIR, AIR, AIR, AIR},
            {AIR, AIR, AIR, AIR, AIR, AIR, AIR, AIR, AIR},
            {PILLAR, AIR, AIR, AIR, CENTER, AIR, AIR, AIR, PILLAR},
            {AIR, AIR, AIR, AIR, AIR, AIR, AIR, AIR, AIR},
            {AIR, AIR, AIR, AIR, AIR, AIR, AIR, AIR, AIR},
            {AIR, AIR, AIR, AIR, AIR, AIR, AIR, AIR, AIR},
            {AIR, AIR, AIR, AIR, PILLAR, AIR, AIR, AIR, AIR}
    };

    /** 扫描结果：升级框架位置（升级统计用）与设备端口位置（关联用） */
    public record Result(List<BlockPos> upgradeFrames, List<BlockPos> ports) {
    }

    private AkaishiMinerStructure() {
    }

    /**
     * 以控制器为锚点做全量结构校验（dy 相对控制器 -2..+2，dx/dz -4..+4）。
     *
     * @return 校验通过返回升级框架/端口位置，否则 null
     */
    public static Result scan(Level level, BlockPos controller) {
        List<BlockPos> upgradeFrames = new ArrayList<>(UPGRADE_TOTAL);
        List<BlockPos> ports = new ArrayList<>(5);
        int[] pillarDevices = new int[4]; // 每根立柱当前累计的外接设备数（上限 1）
        for (int dy = 0; dy < HEIGHT; dy++) {
            int[][] layer = isDeckLayer(dy) ? DECK_LAYER : HOLLOW_LAYER;
            for (int dz = 0; dz < SIZE; dz++) {
                for (int dx = 0; dx < SIZE; dx++) {
                    int expect = layer[dz][dx];
                    if (expect == AIR) {
                        continue; // 空气宽松：空格任意填充不影响结构
                    }
                    BlockPos p = controller.offset(dx - 4, dy - 2, dz - 4);
                    if (expect == CENTER) {
                        if (!matchCenter(level, p, dy, controller, ports)) {
                            return null;
                        }
                        continue;
                    }
                    if (expect == PILLAR) {
                        if (!matchPillar(level, p, pillarIndex(dz, dx), pillarDevices, ports)) {
                            return null;
                        }
                        continue;
                    }
                    Block b = level.getBlockState(p).getBlock();
                    switch (expect) {
                        case FRAME -> {
                            if (!(b instanceof AkaishiMinerFrameBlock)) {
                                return null;
                            }
                        }
                        case UPGRADE -> {
                            // 升级框架位：空升级框架或已安装的升级模块方块均可
                            if (!(b instanceof AkaishiMinerUpgradeFrameBlock)
                                    && !(b instanceof AkaishiMinerUpgradeBlock)) {
                                return null;
                            }
                            upgradeFrames.add(p.immutable());
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

    /** 是否为铺满升级位的完整十字层（仅底层 dy0 与顶层 dy4），其余层镂空 */
    private static boolean isDeckLayer(int dy) {
        return dy == 0 || dy == HEIGHT - 1;
    }

    /** 中心柱各层内容校验（dy0 钻头 / dy1,dy3 架构 / dy2 控制器自身 / dy4 转口，转口兼作设备登记） */
    private static boolean matchCenter(Level level, BlockPos p, int dy, BlockPos controller, List<BlockPos> ports) {
        if (dy == 2) {
            return p.equals(controller); // 只有控制器位于正中层中心
        }
        Block b = level.getBlockState(p).getBlock();
        switch (dy) {
            case 0:
                return b instanceof AkaishiMinerDrillBitBlock;
            case 1, 3:
                return b instanceof AkaishiMinerFrameBlock;
            case 4:
                if (b instanceof AkaishiMinerPortBlock) {
                    ports.add(p.immutable()); // 顶层中心转口：产物接收 + 能量输入设备
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    /** 立柱位置 → 柱索引（北/南/西/东），用于按柱统计外接设备数 */
    private static int pillarIndex(int dz, int dx) {
        if (dz == 0 && dx == 4) {
            return 0; // 北柱
        }
        if (dz == 8 && dx == 4) {
            return 1; // 南柱
        }
        if (dz == 4 && dx == 0) {
            return 2; // 西柱
        }
        return 3; // 东柱 (dz==4 && dx==8)
    }

    /**
     * 立柱位校验：矿机架构 / 架构【外接】可任意填充（不限量）；
     * 能量输入口 / 物品输出口 属于外接设备，可与结构方块互通互换，但每根立柱合计至多 1 个。
     */
    private static boolean matchPillar(Level level, BlockPos p, int pillar, int[] pillarDevices, List<BlockPos> ports) {
        Block b = level.getBlockState(p).getBlock();
        if (b instanceof AkaishiMinerFrameBlock || b instanceof AkaishiMinerExternalFrameBlock) {
            return true;
        }
        if (b instanceof AkaishiMinerEnergyInputBlock || b instanceof AkaishiMinerItemOutputBlock) {
            if (pillarDevices[pillar] >= 1) {
                return false; // 每根立柱至多 1 个外接设备
            }
            pillarDevices[pillar]++;
            ports.add(p.immutable()); // 设备立柱位：控制器需建立关联/推送产物
            return true;
        }
        return false;
    }

    /** 成型后的箱体范围（解除端口关联时使用） */
    public static BlockPos minPos(BlockPos controller) {
        return controller.offset(-4, -2, -4);
    }

    public static BlockPos maxPos(BlockPos controller) {
        return controller.offset(4, 2, 4);
    }
}
