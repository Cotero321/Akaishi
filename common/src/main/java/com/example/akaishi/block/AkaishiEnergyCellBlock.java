package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiEnergyCellBlockEntity;
import com.example.akaishi.block.entity.AkaishiEnergyCellSerializerBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import com.example.akaishi.energy.EnergyCellTier;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

/**
 * 赤能源储存单元：大容量赤能源存储方块，可被赤能源管道接入。
 * 按 {@link EnergyCellTier} 参数化，同一方块实体类型承载不同等级。
 */
public class AkaishiEnergyCellBlock extends AkaishiMachineBlock {

    private final EnergyCellTier tier;

    public AkaishiEnergyCellBlock(EnergyCellTier tier) {
        super(Properties.of().strength(5.0F, 6.0F).requiresCorrectToolForDrops());
        this.tier = tier;
    }

    /** 本方块对应的储存单元等级（构造方块实体时依据它决定容量） */
    public EnergyCellTier getTier() {
        return tier;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_ENERGY_CELL.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // 逻辑只在服务端运行，客户端仅负责 GUI 展示
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_ENERGY_CELL.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_ENERGY_CELL.get(), AkaishiEnergyCellBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // 右键打开储存单元界面，展示当前赤能源储量
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiEnergyCellBlockEntity cell) {
                // 作为串联器外壳时：代理打开中心串联器的界面（中心方块被外壳包围，玩家无法直接点击）
                AkaishiEnergyCellSerializerBlockEntity center = cell.findSerializerCenter();
                if (center != null) {
                    MenuRegistry.openExtendedMenu(serverPlayer, center);
                } else {
                    MenuRegistry.openExtendedMenu(serverPlayer, cell);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
