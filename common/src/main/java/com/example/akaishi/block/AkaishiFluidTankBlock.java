package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiFluidTankBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import com.example.akaishi.fluid.FluidTankTier;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
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
 * 液体储罐：大容量液体存储方块，可被液体管道注入/抽取（含 MEK 等外部设备）。
 * 按 {@link FluidTankTier} 参数化，同一方块实体类型承载不同等级，单方块结构。
 */
public class AkaishiFluidTankBlock extends AkaishiMachineBlock {

    private final FluidTankTier tier;

    public AkaishiFluidTankBlock(FluidTankTier tier) {
        super(Properties.of()
                .mapColor(MapColor.COLOR_CYAN)
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
        this.tier = tier;
    }

    /** 本方块对应的储罐等级（构造方块实体时依据它决定容量） */
    public FluidTankTier getTier() {
        return tier;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_FLUID_TANK.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // 逻辑只在服务端运行，客户端仅负责 GUI 展示
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_FLUID_TANK.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_FLUID_TANK.get(), AkaishiFluidTankBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // 右键打开储罐界面，展示当前液体储量
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AkaishiFluidTankBlockEntity tank) {
                MenuRegistry.openExtendedMenu(serverPlayer, tank);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
