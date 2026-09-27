package com.example.akaishi.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
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
 * 回响花茎：回响花（参考原版紫颂植株 chorus_plant）的第 1 格，由回响花种右键幽匿块顶面种下生成。
 * 只有「1 茎 + 1 顶花」两格：本格随机刻在正上方长出回响花顶花，不再向上续茎/分叉。
 * 存在条件对齐紫颂植株：下方必须为幽匿块，支撑（幽匿块）被破坏时本格自灭（updateShape 排程自毁）。
 * 挖掘掉落 1 颗回响花种（掉落表，1:1 返还，杜绝刷种子）。
 */
public class AkaishiEchoStemBlock extends BushBlock {

    /** 外形：细茎贯通整格（紫颂茎状），模型：akaishi_echo_stem */
    private static final VoxelShape SHAPE = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D);
    /** 每随机刻在顶端长出顶花的概率 */
    private static final float GROW_CHANCE = 0.35F;

    public AkaishiEchoStemBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_CYAN)
                .instabreak()
                .noCollission()
                .noOcclusion()
                .randomTicks()
                .sound(SoundType.SCULK));
    }

    /** 仅允许扎根在幽匿块上 */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.SCULK);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** 存活要求：下方必须是幽匿块（对齐紫颂植株的存在条件） */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(Blocks.SCULK);
    }

    /** 支撑消失即自灭：下方方块变化后若不再可存活，排程自毁（对齐紫颂植株） */
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            level.scheduleTick(pos, this, 1);
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /** 自毁结算：不可存活时连带破坏（掉落走掉落表） */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /** 随机刻：正上方为空时尝试长出顶花（只长 1 格，不做多段向上生长） */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isEmptyBlock(pos.above()) && random.nextFloat() < GROW_CHANCE) {
            level.setBlock(pos.above(), AkaishiTransgeneBlocks.CHISHI_ECHO_FLOWER.get().defaultBlockState(), 2);
        }
    }
}
