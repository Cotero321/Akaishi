package com.example.template.block;

import com.example.template.block.entity.ChishiEnergyAssemblyBlockEntity;
import com.example.template.block.entity.ModBlockEntities;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
 * 小型赤能源组合结构：3x3 多方块结构的主方块。
 * 当 8 个水平邻居均为赤能源发生机时激活（formed=true），以 9 倍速率集中产能；
 * 结构不完整时失活，并将外壳发生机恢复为独立工作状态。
 */
public class ChishiEnergyAssemblyBlock extends ChishiMachineBlock {

    /** 结构是否完整（激活状态） */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public ChishiEnergyAssemblyBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
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
        return ModBlockEntities.CHISHI_ENERGY_ASSEMBLY.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_ENERGY_ASSEMBLY.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_ENERGY_ASSEMBLY.get(), ChishiEnergyAssemblyBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // 已停用为纯合成材料：不提供任何交互界面
        return InteractionResult.PASS;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // 主方块被拆时，先解除 26 个外壳发生机的休眠状态，避免外壳永久失效
            if (state.getValue(FORMED)) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) {
                                continue;
                            }
                            BlockPos p = pos.offset(dx, dy, dz);
                            BlockState s = level.getBlockState(p);
                            if (s.getBlock() instanceof ChishiEnergyGeneratorBlock && s.getValue(ChishiEnergyGeneratorBlock.FORMED)) {
                                level.setBlock(p, s.setValue(ChishiEnergyGeneratorBlock.FORMED, false), 3);
                            }
                        }
                    }
                }
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiEnergyAssemblyBlockEntity assembly) {
                Containers.dropContents(level, pos, assembly.inventory());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
