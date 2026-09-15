package com.example.akaishi.block;

import com.example.akaishi.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
 * 咒怨垂蔓茎：整株第 2~5 格（根之下最多 4 节茎），整株最长 5 格。
 * - 仅"尖梢"（正下方不再是茎）参与：未达最大长度且下方为空时随机刻继续向下垂挂抽茎；
 * - 已达最大长度或下方被阻挡（触地）→ 尖梢转入"结果"阶段：
 *   age 0 空蔓 → 1 结出幼花 → 2 咒怨花成熟；成熟后右键采摘必得 1 咒怨花并回到空蔓继续结果。
 * - 挖掘成熟尖梢（非创造）掉 1 咒怨花；未成熟尖梢与其余茎节无掉落。
 * 无物品形式（只能由根长出），杜绝绕过种子的繁殖。
 */
public class AkaishiCurseVineStemBlock extends BushBlock {

    /** 外形：主干 + 节瘤 + 侧棱，横向展至 4~12（模型：akaishi_curse_vine_stem 系列） */
    private static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D);
    /** 每随机刻向下抽出一节茎的概率 */
    private static final float GROW_CHANCE = 0.35F;
    /** 结果阶段推进概率（每随机刻） */
    private static final float FRUIT_CHANCE = 0.5F;
    /** 整株最多茎节数：根 + 4 茎 = 总长 5 格 */
    public static final int MAX_STEMS = 4;
    /** 结果阶段：0 空蔓 / 1 幼花 / 2 咒怨花成熟（可采摘） */
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 2);

    public AkaishiCurseVineStemBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
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

    /** 本格上方连续茎节的数量（0 = 直接挂在根下，用于限制整株最大长度） */
    private static int stemsAbove(BlockGetter level, BlockPos pos) {
        int count = 0;
        BlockPos cursor = pos.above();
        while (count < MAX_STEMS && level.getBlockState(cursor).is(ModBlocks.CHISHI_CURSE_VINE_STEM.get())) {
            count++;
            cursor = cursor.above();
        }
        return count;
    }

    /** 是否整株的尖梢（正下方不再是茎）——只有尖梢会继续垂挂抽茎或转入结果 */
    public static boolean isFruitingTip(BlockGetter level, BlockPos pos) {
        return !level.getBlockState(pos.below()).is(ModBlocks.CHISHI_CURSE_VINE_STEM.get());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** 必须吊挂在正上方的根或上一节茎之下 */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return above.is(ModBlocks.CHISHI_CURSE_VINE_ROOT.get()) || above.is(ModBlocks.CHISHI_CURSE_VINE_STEM.get());
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /**
     * 随机刻（仅尖梢参与，中间茎节不生长也不结果）：
     * - 未达整株最大长度且正下方为空：继续向下垂挂抽茎；
     * - 已达最大长度或正下方被阻挡：结果阶段推进（空蔓→幼花→成熟），触地也不会失效。
     */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!isFruitingTip(level, pos)) {
            return;
        }
        if (stemsAbove(level, pos) + 1 < MAX_STEMS && level.isEmptyBlock(pos.below())) {
            if (random.nextFloat() < GROW_CHANCE) {
                level.setBlock(pos.below(), defaultBlockState(), 2);
            }
            return;
        }
        int age = state.getValue(AGE);
        if (age < 2 && random.nextFloat() < FRUIT_CHANCE) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 2);
        }
    }

    /** 右键：仅整株尖梢且咒怨花成熟（age 2）时可采摘，必定得 1 咒怨花，蔓回到空蔓继续结果 */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                InteractionHand hand, BlockHitResult hit) {
        if (!isFruitingTip(level, pos) || state.getValue(AGE) < 2) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            Block.popResource(level, pos, new ItemStack(ModItems.akaishiCurseBlossom.get()));
            level.setBlock(pos, state.setValue(AGE, 0), 2);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 挖掘成熟尖梢（非创造）掉落 1 个咒怨花；未成熟尖梢与其余茎节无掉落（杜绝低矮空间刷花） */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.getAbilities().instabuild
                && isFruitingTip(level, pos) && state.getValue(AGE) >= 2) {
            Block.popResource(level, pos, new ItemStack(ModItems.akaishiCurseBlossom.get()));
        }
    }
}
