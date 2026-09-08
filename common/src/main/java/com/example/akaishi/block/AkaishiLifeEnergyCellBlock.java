package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiLifeEnergyCellBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifeEnergyCellSerializerBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import com.example.akaishi.energy.LifeEnergyCellTier;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 生命能量储存器：大容量生命能量存储方块，可被生命能量管道接入充放。
 * 按 {@link LifeEnergyCellTier} 参数化（基础/高级/超级），三个等级共用同一方块实体类型。
 * 作为生命储存串联器（3×3×3）外壳时，右键代理打开中心串联器界面。
 */
public class AkaishiLifeEnergyCellBlock extends AkaishiMachineBlock {

    private final LifeEnergyCellTier tier;

    public AkaishiLifeEnergyCellBlock(LifeEnergyCellTier tier) {
        super(Properties.of()
                .mapColor(MapColor.COLOR_GREEN)
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
        this.tier = tier;
    }

    /** 本方块对应的储存器等级（构造方块实体时依据它决定容量） */
    public LifeEnergyCellTier getTier() {
        return tier;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_LIFE_ENERGY_CELL.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_LIFE_ENERGY_CELL.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_LIFE_ENERGY_CELL.get(), AkaishiLifeEnergyCellBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AkaishiLifeEnergyCellBlockEntity cell) {
                // 作为串联器外壳时：代理打开中心串联器界面（中心被 26 台储存器包围，玩家无法直接点击）
                AkaishiLifeEnergyCellSerializerBlockEntity center = cell.findSerializerCenter();
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
