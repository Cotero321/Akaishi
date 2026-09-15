package com.example.akaishi.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 咒怨垂蔓根：整株的第 1 格基底（种子种在方块底面后所在格）。
 * 随机刻在其下方长出第 2 格（第一节茎）；破坏根（或上方支撑被拆）只掉落 1 颗种子，杜绝刷种子。
 */
public class AkaishiCurseVineRootBlock extends BushBlock {

    /** 外形：根盘 + 四向根须外扩至 2~14，主干贯通整格（模型：akaishi_curse_vine_root） */
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);
    /** 每随机刻长出第一节茎的概率 */
    private static final float GROW_CHANCE = 0.35F;

    public AkaishiCurseVineRootBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .instabreak()
                .noCollission()
                .noOcclusion()
                .randomTicks()
                .sound(SoundType.ROOTED_DIRT));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** 必须吊挂在正上方坚固方块的底面之下 */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        return !aboveState.isAir() && aboveState.isFaceSturdy(level, above, Direction.DOWN);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /** 第一阶段：根在随机刻于正下方长出第 2 格（第一节茎） */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isEmptyBlock(pos.below()) && random.nextFloat() < GROW_CHANCE) {
            level.setBlock(pos.below(), ModBlocks.CHISHI_CURSE_VINE_STEM.get().defaultBlockState(), 2);
        }
    }
}
