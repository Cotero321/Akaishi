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
import net.minecraft.world.level.block.Blocks;
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
 * 末影花丛：末影花（参考原版杜鹃花丛 azalea）的单块丛状方块，无茎、无花冠。
 * 由末影花种右键末地石顶面种下生成；仅能存活在末地石上（canSurvive 校验）。
 * 成熟度两阶：0 未成熟 / 1 成熟（随机刻推进）。
 * 收获：成熟后右键 ⇒ 收获 1 末影果并回到未成熟（可重复收获）；破坏成熟丛 ⇒ 掉 1 末影果；
 * 破坏未成熟丛 ⇒ 掉 1 末影花种（掉落表按 age 判定）。
 */
public class AkaishiEnderBushBlock extends BushBlock {

    /** 各成熟度外形：未成熟矮丛 / 成熟满丛，模型：akaishi_ender_bush_0/_1 */
    private static final VoxelShape[] SHAPES = new VoxelShape[]{
            Block.box(2.0D, 0.0D, 2.0D, 14.0D, 10.0D, 14.0D),
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D)
    };
    /** 成熟推进概率（每随机刻） */
    private static final float GROW_CHANCE = 0.35F;
    /** 成熟度：0 未成熟 / 1 成熟（可收末影果） */
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 1);

    public AkaishiEnderBushBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .instabreak()
                .noCollission()
                .noOcclusion()
                .randomTicks()
                .sound(SoundType.AZALEA));
    }

    /** 仅允许扎根在末地石上 */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.END_STONE);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }

    /** 存活要求：下方必须是末地石 */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(Blocks.END_STONE);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /** 随机刻：未成熟（age 0）时按概率转为成熟 */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) == 0 && random.nextFloat() < GROW_CHANCE) {
            level.setBlock(pos, state.setValue(AGE, 1), 2);
        }
    }

    /** 右键收获：仅成熟（age 1）时可收 1 末影果，丛回到未成熟继续成熟（可重复收获） */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(AGE) < 1) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            Block.popResource(level, pos, new ItemStack(ModItems.akaishiEnderFruit.get()));
            level.setBlock(pos, state.setValue(AGE, 0), 2);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 非创造破坏成熟丛必定掉 1 末影果（未成熟丛的种子掉落走掉落表） */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.getAbilities().instabuild && state.getValue(AGE) >= 1) {
            Block.popResource(level, pos, new ItemStack(ModItems.akaishiEnderFruit.get()));
        }
    }
}
