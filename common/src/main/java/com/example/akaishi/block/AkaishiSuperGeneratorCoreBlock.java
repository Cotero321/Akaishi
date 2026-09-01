package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiEnergyGeneratorBlockEntity;
import com.example.akaishi.block.entity.AkaishiSuperGeneratorCoreBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 超级发生器架构核心：5x5x5 大型多方块结构的主方块。
 * 124 台赤能源发生机环绕成型（formed=true）后，以最高速率集中产能并统一输出。
 * 中心方块被外壳包围无法直接点击，右键任一发生机外壳可代理打开本界面。
 */
public class AkaishiSuperGeneratorCoreBlock extends BaseEntityBlock {

    /** 结构是否完整（激活状态） */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public AkaishiSuperGeneratorCoreBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(5.0F)
                .sound(SoundType.METAL));
        this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_SUPER_GENERATOR_CORE.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_SUPER_GENERATOR_CORE.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_SUPER_GENERATOR_CORE.get(),
                AkaishiSuperGeneratorCoreBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // 已停用为纯合成材料：不提供任何交互界面
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // 主方块被破坏时解除 124 个外壳的休眠（外壳通过 getEnergyStorage 惰性判断中心，此处仅需防止遗留状态）
        if (!state.is(newState.getBlock())) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos p = pos.offset(dx, dy, dz);
                        BlockState s = level.getBlockState(p);
                        if (s.getBlock() instanceof AkaishiEnergyGeneratorBlock
                                && s.getValue(AkaishiEnergyGeneratorBlock.FORMED)) {
                            level.setBlock(p, s.setValue(AkaishiEnergyGeneratorBlock.FORMED, false), 3);
                        }
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
