package com.example.template.block;

import com.example.template.block.entity.ChishiEnergyAssemblyBlockEntity;
import com.example.template.block.entity.ChishiEnergyGeneratorBlockEntity;
import com.example.template.block.entity.ChishiSuperGeneratorCoreBlockEntity;
import com.example.template.block.entity.ModBlockEntities;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
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
 * 赤能源发生机：燃烧赤石材料产生赤能源，单方块产能 75/tick。
 * 作为 3x3 多方块结构的外壳时（formed=true）自动休眠，产能交由中心主方块统一输出。
 */
public class ChishiEnergyGeneratorBlock extends BaseEntityBlock {

    /** 是否为多方块结构外壳（休眠标记，由中心主方块写入） */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public ChishiEnergyGeneratorBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(3.5F)
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
        return ModBlockEntities.CHISHI_ENERGY_GENERATOR.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_ENERGY_GENERATOR.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_ENERGY_GENERATOR.get(), ChishiEnergyGeneratorBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // 成型后作为多方块外壳：右键代理打开中心主方块的界面（中心方块被外壳包围，玩家无法直接点击）
        if (state.getValue(FORMED)) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                // 优先 5×5×5 超级发生器架构核心，其次 3×3×3 组合结构
                BlockPos superCenter = findSuperCenter(level, pos);
                if (superCenter != null) {
                    if (level.getBlockEntity(superCenter) instanceof ChishiSuperGeneratorCoreBlockEntity core) {
                        MenuRegistry.openExtendedMenu(serverPlayer, core);
                    }
                } else {
                    BlockPos center = findAssemblyCenter(level, pos);
                    if (center != null && level.getBlockEntity(center) instanceof ChishiEnergyAssemblyBlockEntity assembly) {
                        MenuRegistry.openExtendedMenu(serverPlayer, assembly);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiEnergyGeneratorBlockEntity generator) {
                MenuRegistry.openExtendedMenu(serverPlayer, generator);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 在自身为中心的 5×5×5 范围内查找成型中的超级发生器架构核心 */
    private static BlockPos findSuperCenter(Level level, BlockPos shellPos) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos p = shellPos.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof ChishiSuperGeneratorCoreBlock
                            && s.getValue(ChishiSuperGeneratorCoreBlock.FORMED)) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    /** 在自身为中心的 3×3×3 范围内查找成型中的组合结构主方块 */
    private static BlockPos findAssemblyCenter(Level level, BlockPos shellPos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = shellPos.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof ChishiEnergyAssemblyBlock
                            && s.getValue(ChishiEnergyAssemblyBlock.FORMED)) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // 方块被替换/破坏时倒出内部物品
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiEnergyGeneratorBlockEntity generator) {
                Containers.dropContents(level, pos, generator.inventory());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
