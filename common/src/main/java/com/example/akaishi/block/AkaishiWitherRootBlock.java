package com.example.akaishi.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 凋零藤根：整株的第 1 格基底（种子种下后所在格）。
 * 随机刻在其上方长出第 2 格茎；破坏根（或根下支撑被拆）只掉落 1 颗种子，杜绝刷种子。
 */
public class AkaishiWitherRootBlock extends BushBlock {

    private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D);
    /** 每随机刻长出第一节茎的概率（生长条件后续再补） */
    private static final float GROW_CHANCE = 0.35F;

    public AkaishiWitherRootBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .instabreak()
                .noCollission()
                .noOcclusion()
                .randomTicks()
                .sound(SoundType.ROOTED_DIRT));
    }

    /** 允许种在任何非空气方块顶面 */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return !state.isAir();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        return !belowState.isAir() && belowState.isFaceSturdy(level, below, net.minecraft.core.Direction.UP);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /** 第一阶段：根在随机刻于正上方长出第 2 格（第一节茎） */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isEmptyBlock(pos.above()) && random.nextFloat() < GROW_CHANCE) {
            level.setBlock(pos.above(), ModBlocks.CHISHI_WITHER_STEM.get().defaultBlockState(), 2);
        }
    }
}
