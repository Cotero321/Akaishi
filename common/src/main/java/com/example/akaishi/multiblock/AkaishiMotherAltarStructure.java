package com.example.akaishi.multiblock;

import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.block.entity.AkaishiMotherAltarBlockEntity;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.organ.AkaishiOrganItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 母神祭坛仪式结构检测（17×17×2，锚点为中央祭坛）。
 * 第一层（y-1）：哭泣黑曜石构成菱形外环与十字中线，菱形内部四象限填仪式石。
 * 第二层（y=0）：8 个灵魂营火 + 17 个黑山羊祭坛（中央 1 + 内圈 8 器官 + 外围 4 胚胎 + 4 灰烬）。
 * 供奉物校验：中央=心脏（适配度 100）、内圈=8 种互不相同的非心脏器官（适配度 100）、
 * 十字外围=生命胚胎、对角外围=生命灰烬。
 */
public final class AkaishiMotherAltarStructure {

    /** 第一层（y-1）：0=空气 1=哭泣黑曜石 2=仪式石 */
    private static final int AIR = 0;
    private static final int CRYING = 1;
    private static final int ALTAR_STONE = 2;
    /** 第二层（y=0）：0=空气 3=灵魂营火 4=黑山羊祭坛 */
    private static final int CAMPFIRE = 3;
    private static final int ALTAR = 4;

    /** 半宽：17 = 2×8+1，锚点位于矩阵中心 */
    private static final int HALF = 8;

    private static final int[][] LAYER_BOTTOM = {
            {0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 1, 2, 1, 2, 1, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 1, 2, 2, 1, 2, 2, 1, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, 2, 2, 2, 1, 2, 2, 2, 1, 0, 0, 0, 0},
            {0, 0, 0, 1, 2, 2, 2, 2, 1, 2, 2, 2, 2, 1, 0, 0, 0},
            {0, 0, 1, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 1, 0, 0},
            {0, 1, 2, 2, 2, 2, 2, 1, 1, 1, 2, 2, 2, 2, 2, 1, 0},
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {0, 1, 2, 2, 2, 2, 2, 1, 1, 1, 2, 2, 2, 2, 2, 1, 0},
            {0, 0, 1, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 1, 0, 0},
            {0, 0, 0, 1, 2, 2, 2, 2, 1, 2, 2, 2, 2, 1, 0, 0, 0},
            {0, 0, 0, 0, 1, 2, 2, 2, 1, 2, 2, 2, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 1, 2, 2, 1, 2, 2, 1, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 1, 2, 1, 2, 1, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0}
    };

    private static final int[][] LAYER_TOP = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 4, 0, 0, 4, 0, 0, 4, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 3, 0, 3, 0, 0, 0, 0, 0, 0, 0},
            {0, 3, 0, 4, 0, 4, 0, 0, 4, 0, 0, 4, 0, 4, 0, 3, 0},
            {0, 0, 0, 0, 0, 0, 0, 3, 0, 3, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 4, 0, 0, 4, 0, 0, 4, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
    };

    /** 内圈 8 个非心脏器官祭坛相对锚点的偏移（曼哈顿距离 3 十字 + 距离 4 对角） */
    private static final int[][] ORGAN_OFFSETS = {
            {-3, 0}, {3, 0}, {0, -3}, {0, 3},
            {-2, -2}, {2, -2}, {-2, 2}, {2, 2}
    };
    /** 外围 4 个生命胚胎祭坛（曼哈顿距离 5 十字） */
    private static final int[][] EMBRYO_OFFSETS = {
            {-5, 0}, {5, 0}, {0, -5}, {0, 5}
    };
    /** 外围 4 个生命灰烬祭坛（曼哈顿距离 6 对角） */
    private static final int[][] ASH_OFFSETS = {
            {-3, -3}, {3, -3}, {-3, 3}, {3, 3}
    };

    /** 校验通过返回全部 17 个祭坛位置（供消耗供奉物），否则 null */
    public record Result(List<BlockPos> altars) {
    }

    private AkaishiMotherAltarStructure() {
    }

    /**
     * 以中央祭坛为锚点做全量结构校验。
     *
     * @return 校验通过返回 17 个祭坛位置，否则 null
     */
    public static Result scan(Level level, BlockPos center) {
        // 第一层（y-1）方块校验
        for (int z = 0; z < 17; z++) {
            for (int x = 0; x < 17; x++) {
                BlockPos p = center.offset(x - HALF, -1, z - HALF);
                BlockState state = level.getBlockState(p);
                switch (LAYER_BOTTOM[z][x]) {
                    case AIR -> {
                        if (!state.isAir()) {
                            return null;
                        }
                    }
                    case CRYING -> {
                        if (!state.is(Blocks.CRYING_OBSIDIAN)) {
                            return null;
                        }
                    }
                    case ALTAR_STONE -> {
                        if (!state.is(ModBlocks.CHISHI_ALTAR_STONE.get())) {
                            return null;
                        }
                    }
                    default -> {
                        return null;
                    }
                }
            }
        }
        // 第二层（y=0）方块校验（中央祭坛即锚点自身，跳过方块类型）
        for (int z = 0; z < 17; z++) {
            for (int x = 0; x < 17; x++) {
                int expect = LAYER_TOP[z][x];
                if (expect == ALTAR && x == HALF && z == HALF) {
                    continue;
                }
                BlockPos p = center.offset(x - HALF, 0, z - HALF);
                BlockState state = level.getBlockState(p);
                switch (expect) {
                    case AIR -> {
                        if (!state.isAir()) {
                            return null;
                        }
                    }
                    case CAMPFIRE -> {
                        if (!state.is(Blocks.SOUL_CAMPFIRE)) {
                            return null;
                        }
                    }
                    case ALTAR -> {
                        if (!state.is(ModBlocks.CHISHI_MOTHER_ALTAR.get())) {
                            return null;
                        }
                    }
                    default -> {
                        return null;
                    }
                }
            }
        }
        // 供奉物校验：中央心脏
        ItemStack heart = offeringAt(level, center);
        if (!(heart.getItem() instanceof AkaishiOrganItem heartOrgan)
                || heartOrgan.slot != BodySlot.HEART
                || AkaishiOrganItem.getCompat(heart) != AkaishiOrganItem.MAX_COMPAT) {
            return null;
        }
        // 供奉物校验：内圈 8 种互不相同的非心脏器官，适配度 100
        Set<BodySlot> seen = new HashSet<>();
        for (int[] off : ORGAN_OFFSETS) {
            BlockPos p = center.offset(off[0], 0, off[1]);
            ItemStack stack = offeringAt(level, p);
            if (!(stack.getItem() instanceof AkaishiOrganItem organ)) {
                return null;
            }
            if (organ.slot == BodySlot.HEART) {
                return null;
            }
            if (AkaishiOrganItem.getCompat(stack) != AkaishiOrganItem.MAX_COMPAT) {
                return null;
            }
            if (!seen.add(organ.slot)) {
                return null; // 内圈器官重复
            }
        }
        // 供奉物校验：外围胚胎与灰烬
        for (int[] off : EMBRYO_OFFSETS) {
            if (!offeringAt(level, center.offset(off[0], 0, off[1])).is(ModItems.lifeEmbryo.get())) {
                return null;
            }
        }
        for (int[] off : ASH_OFFSETS) {
            if (!offeringAt(level, center.offset(off[0], 0, off[1])).is(ModItems.lifeAsh.get())) {
                return null;
            }
        }
        // 汇总全部祭坛位置
        List<BlockPos> altars = new ArrayList<>(17);
        altars.add(center);
        for (int[] off : ORGAN_OFFSETS) {
            altars.add(center.offset(off[0], 0, off[1]));
        }
        for (int[] off : EMBRYO_OFFSETS) {
            altars.add(center.offset(off[0], 0, off[1]));
        }
        for (int[] off : ASH_OFFSETS) {
            altars.add(center.offset(off[0], 0, off[1]));
        }
        return new Result(List.copyOf(altars));
    }

    /** 读取指定祭坛的供奉物（无祭坛/空则返回 EMPTY） */
    private static ItemStack offeringAt(Level level, BlockPos p) {
        BlockEntity be = level.getBlockEntity(p);
        if (be instanceof AkaishiMotherAltarBlockEntity altar) {
            return altar.getOffering();
        }
        return ItemStack.EMPTY;
    }
}
