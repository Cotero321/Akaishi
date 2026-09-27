package com.example.akaishi.block;

import com.example.akaishi.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 回响花顶花：回响花（参考原版紫颂花 chorus_flower）的顶端第 2 格，由回响花茎随机刻长出（无物品形态）。
 * 年岁三阶（对齐紫颂花的 age 成熟度思路）：0 花苞 / 1 半开 / 2 盛开；盛开时微光。
 * 成熟（age 2）后「破坏」或「右键」均可收获 1 颗幽匿果，右键后回到花苞继续开花。
 * 存在条件对齐紫颂花：下方必须为本株茎，茎消失时本格自灭（updateShape 排程自毁）。
 */
public class AkaishiEchoFlowerBlock extends BushBlock {

    /** 各年岁外形：花苞 / 半开 / 盛开 */
    private static final VoxelShape[] SHAPES = new VoxelShape[]{
            Block.box(6.0D, 0.0D, 6.0D, 10.0D, 6.0D, 10.0D),
            Block.box(5.0D, 0.0D, 5.0D, 11.0D, 10.0D, 11.0D),
            Block.box(4.0D, 0.0D, 4.0D, 12.0D, 14.0D, 12.0D)
    };
    /** 花态推进概率（每随机刻） */
    private static final float GROW_CHANCE = 0.35F;
    /** 年岁：0 花苞 / 1 半开 / 2 盛开（可采摘幽匿果，微光） */
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 2);

    public AkaishiEchoFlowerBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_CYAN)
                .instabreak()
                .noCollission()
                .noOcclusion()
                .randomTicks()
                // 仅盛开（age 2）时微光 7；体现幽匿（黍菌）发光调性
                .lightLevel(state -> state.getValue(AGE) == 2 ? 7 : 0)
                .sound(SoundType.SCULK));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }

    /** 存活要求：下方必须是本株回响花茎（对齐紫颂花的顶端花） */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(AkaishiTransgeneBlocks.CHISHI_ECHO_STEM.get());
    }

    /** 茎消失即自灭：下方方块变化后若不再可存活，排程自毁（对齐紫颂花） */
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            level.scheduleTick(pos, this, 1);
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /** 自毁结算：不可存活时连带破坏 */
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

    /** 花态推进：未盛开（age<2）时按概率推进一阶 */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < 2 && random.nextFloat() < GROW_CHANCE) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 2);
        }
    }

    /** 右键采摘：仅盛开（age 2）且下方是茎时收获 1 幽匿果，花回到花苞继续开花 */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.getBlockState(pos.below()).is(AkaishiTransgeneBlocks.CHISHI_ECHO_STEM.get())
                || state.getValue(AGE) < 2) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            Block.popResource(level, pos, new ItemStack(ModItems.akaishiEchoFruit.get()));
            level.setBlock(pos, state.setValue(AGE, 0), 2);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 非创造破坏盛开顶花必定掉 1 幽匿果（茎被拆导致的坍塌不在此列） */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.getAbilities().instabuild
                && level.getBlockState(pos.below()).is(AkaishiTransgeneBlocks.CHISHI_ECHO_STEM.get())
                && state.getValue(AGE) == 2) {
            Block.popResource(level, pos, new ItemStack(ModItems.akaishiEchoFruit.get()));
        }
    }
}
