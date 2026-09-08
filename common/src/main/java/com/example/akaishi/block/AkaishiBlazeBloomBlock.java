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
 * 烈焰花冠：烈焰花丛的顶端第 2 格，由成株花株随机刻长出（无物品形态，不可手持放置）。
 * 花态三段：花蕾/初绽/盛开（age 0~2）；盛开时自发光（亮度 12，不引燃）。
 * 盛开后右键采摘必定得 1 烈焰凝聚物并回到花蕾继续开花；非创造挖掘盛开花冠同样得 1 凝聚物。
 */
public class AkaishiBlazeBloomBlock extends BushBlock {

    private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D);
    /** 花态推进概率（每随机刻） */
    private static final float GROW_CHANCE = 0.35F;
    /** 花态：0 花蕾 / 1 初绽 / 2 盛开（可采摘凝聚物，自发光） */
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 2);

    public AkaishiBlazeBloomBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.FIRE)
                .instabreak()
                .noCollission()
                .noOcclusion()
                .randomTicks()
                // 仅盛开（age 2）时发光 12；默认无引燃注册，不会被火焰蔓延点燃
                .lightLevel(state -> state.getValue(AGE) == 2 ? 12 : 0)
                .sound(SoundType.ROOTED_DIRT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** 存活要求：下方必须是烈焰花株（整株基座） */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(AkaishiTransgeneBlocks.CHISHI_BLAZE_FLOWER_ROOT.get());
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /** 花态推进：花蕾/初绽未盛开（age<2）时按概率推进一阶 */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < 2 && random.nextFloat() < GROW_CHANCE) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 2);
        }
    }

    /** 右键采摘：仅盛开（age 2）且下方是花株（植株顶端）时收获 1 烈焰凝聚物，花冠回到花蕾继续开花 */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                InteractionHand hand, BlockHitResult hit) {
        if (!level.getBlockState(pos.below()).is(AkaishiTransgeneBlocks.CHISHI_BLAZE_FLOWER_ROOT.get())
                || state.getValue(AGE) < 2) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            Block.popResource(level, pos, new net.minecraft.world.item.ItemStack(ModItems.akaishiBlazeCondensate.get()));
            level.setBlock(pos, state.setValue(AGE, 0), 2);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 非创造挖掘盛开花冠必定掉 1 烈焰凝聚物（花株基座被拆导致的坍塌不在此列） */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.getAbilities().instabuild
                && level.getBlockState(pos.below()).is(AkaishiTransgeneBlocks.CHISHI_BLAZE_FLOWER_ROOT.get())
                && state.getValue(AGE) == 2) {
            Block.popResource(level, pos, new net.minecraft.world.item.ItemStack(ModItems.akaishiBlazeCondensate.get()));
        }
    }
}
