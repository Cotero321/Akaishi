package com.example.akaishi.block;

import com.example.akaishi.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
 * 凋零藤茎：整株的第 2/3 格。
 * - 第 2 格（下方是根）：随机刻向上长第 3 格；
 * - 第 3 格（下方是茎）为成熟顶端，转入"结果"阶段：
 *   age 0 空藤 → 1 挂出幼果 → 2 果实成熟；成熟后右键采摘必定得 1 凋零凝聚体并回到空藤继续结果。
 * - 挖掘第 3 格（非创造）必定掉 1 凝聚体；挖掘第 2 格/茎坍塌无掉落。
 * 无物品形式（无法手持放置，只能由根长出），杜绝绕过种子的繁殖。
 */
public class AkaishiWitherStemBlock extends BushBlock {

    private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D);
    /** 每随机刻第 2 格长出第 3 格的概率（生长条件后续再补） */
    private static final float GROW_CHANCE = 0.35F;
    /** 结果阶段推进概率（每随机刻） */
    private static final float FRUIT_CHANCE = 0.5F;
    /** 结果阶段：0 空藤 / 1 挂出幼果 / 2 果实成熟（可采摘） */
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 2);

    public AkaishiWitherStemBlock() {
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

    /** 是否整株成熟的顶端节（第 3 格）：自己下方也是茎 */
    public static boolean isMatureTip(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(ModBlocks.CHISHI_WITHER_STEM.get());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(ModBlocks.CHISHI_WITHER_ROOT.get()) || below.is(ModBlocks.CHISHI_WITHER_STEM.get());
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /**
     * 随机刻双分支：
     * - 第 2 格（下方是根）：向上长出第 3 格空藤；
     * - 第 3 格（下方是茎）：结果阶段推进（空藤→幼果→成熟）。
     */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        if (below.is(ModBlocks.CHISHI_WITHER_ROOT.get())) {
            if (level.isEmptyBlock(pos.above()) && random.nextFloat() < GROW_CHANCE) {
                level.setBlock(pos.above(), ModBlocks.CHISHI_WITHER_STEM.get().defaultBlockState(), 2);
            }
        } else if (below.is(ModBlocks.CHISHI_WITHER_STEM.get())) {
            int age = state.getValue(AGE);
            if (age < 2 && random.nextFloat() < FRUIT_CHANCE) {
                level.setBlock(pos, state.setValue(AGE, age + 1), 2);
            }
        }
    }

    /** 右键：仅成熟顶端（第 3 格）且果实成熟（age 2）时可采摘，必定得 1 凝聚体，藤回到空藤继续结果 */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                InteractionHand hand, BlockHitResult hit) {
        if (!isMatureTip(level, pos) || state.getValue(AGE) < 2) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            Block.popResource(level, pos, new net.minecraft.world.item.ItemStack(ModItems.akaishiWitherCondensate.get()));
            level.setBlock(pos, state.setValue(AGE, 0), 2);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 挖掘成熟顶端（第 3 格，非创造）必定掉落 1 个凋零凝聚体；茎无 loot，其它节挖掘无掉落 */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.getAbilities().instabuild && isMatureTip(level, pos)) {
            Block.popResource(level, pos, new net.minecraft.world.item.ItemStack(ModItems.akaishiWitherCondensate.get()));
        }
    }
}
