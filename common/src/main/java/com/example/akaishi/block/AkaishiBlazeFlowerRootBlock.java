package com.example.akaishi.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 烈焰花株：烈焰花丛转基因植物的第 1 格基座（种子种下后所在格）。
 * 仅能种在灵魂沙上（下界熔岩湖畔生态位）。年岁三段：萌发/幼苗/成株（age 0~2）；
 * 成株后在随机刻于正上方长出第 2 格烈焰花冠。挖掘花株不掉任何物品（不产花种，杜绝刷种子）。
 */
public class AkaishiBlazeFlowerRootBlock extends BushBlock {

    /** 各年岁外形：萌发矮丛 / 幼苗半高 / 成株满高，与三段模型（_0/_1/_2）逐段对齐 */
    private static final VoxelShape[] SHAPES = new VoxelShape[]{
            Block.box(4.0D, 0.0D, 4.0D, 12.0D, 5.0D, 12.0D),
            Block.box(3.0D, 0.0D, 3.0D, 13.0D, 15.0D, 13.0D),
            Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D)
    };
    /** 年岁推进 / 成株开花的概率（每随机刻） */
    private static final float GROW_CHANCE = 0.35F;
    /** 年岁：0 萌发 / 1 幼苗 / 2 成株（成株后可长出花冠） */
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 2);

    public AkaishiBlazeFlowerRootBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.FIRE)
                .instabreak()
                .noCollission()
                .noOcclusion()
                .randomTicks()
                .sound(SoundType.ROOTED_DIRT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    /** 仅允许种在灵魂沙上 */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.SOUL_SAND);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }

    /** 存活要求：下方必须是灵魂沙 */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(Blocks.SOUL_SAND);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /** 随机刻双分支：未成株（age<2）推进年岁；成株（age 2）且正上方空时尝试长出烈焰花冠 */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < 2) {
            if (random.nextFloat() < GROW_CHANCE) {
                level.setBlock(pos, state.setValue(AGE, age + 1), 2);
            }
        } else if (level.isEmptyBlock(pos.above()) && random.nextFloat() < GROW_CHANCE) {
            level.setBlock(pos.above(), AkaishiTransgeneBlocks.CHISHI_BLAZE_BLOOM.get().defaultBlockState(), 2);
        }
    }

    /** 挖掘花株不掉任何物品（不产花种，防止破坏刷种子） */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
    }
}
