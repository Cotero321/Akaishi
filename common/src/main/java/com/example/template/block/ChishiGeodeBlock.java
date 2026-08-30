package com.example.template.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * 赤石水晶母岩：放置后按等级概率在顶面生长赤石水晶簇。
 * 等级越高产出率越高；等级可通过赤石能量聚合器消耗赤能源逐级提升。
 */
public class ChishiGeodeBlock extends Block {

    /** 母岩等级：决定水晶簇生长概率（产出率） */
    public enum GeodeTier {
        FLAWED(0.02f),
        NORMAL(0.035f),
        PRISTINE(0.06f),
        PERFECT(0.12f);

        /** 每次随机 tick 生长水晶簇的概率 */
        public final float growthChance;

        GeodeTier(float growthChance) {
            this.growthChance = growthChance;
        }
    }

    private final GeodeTier tier;

    public ChishiGeodeBlock(GeodeTier tier, MapColor color) {
        super(BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(3.0F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE));
        this.tier = tier;
    }

    /** 参与区块随机 tick（平均每 68 tick 被抽中一次） */
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /** 随机 tick：按等级概率在随机 6 个面之一生长水晶簇（该方向邻格需为空，晶体自附着面朝外） */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        tryGrow(level, pos, random, tier.growthChance);
    }

    /**
     * 尝试在母岩随机 6 面之一生长水晶簇（邻格需为空）。
     * 供母岩自身随机 tick 与赤石催化器（催生）共用。
     */
    public static void tryGrow(ServerLevel level, BlockPos geodePos, RandomSource random, float chance) {
        if (random.nextFloat() >= chance) {
            return;
        }
        // 收集所有空邻格后随机选一个生长，避免随机方向命中已占用格直接失败导致生长率偏低
        List<Direction> airDirs = new ArrayList<>(6);
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(geodePos.relative(dir)).isAir()) {
                airDirs.add(dir);
            }
        }
        if (airDirs.isEmpty()) {
            return;
        }
        Direction dir = airDirs.get(random.nextInt(airDirs.size()));
        level.setBlock(geodePos.relative(dir), ModBlocks.CHISHI_CRYSTAL_CLUSTER.get().defaultBlockState()
                .setValue(ChishiCrystalClusterBlock.FACING, dir), 3);
    }
}
