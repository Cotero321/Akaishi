package com.example.template.block;

import net.minecraft.core.BlockPos;
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

    /** 随机 tick：按等级概率在顶面生长水晶簇（顶面需为空） */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < tier.growthChance) {
            BlockPos above = pos.above();
            if (level.getBlockState(above).isAir()) {
                level.setBlock(above, ModBlocks.CHISHI_CRYSTAL_CLUSTER.get().defaultBlockState(), 3);
            }
        }
    }
}
