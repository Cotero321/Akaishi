package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiPlasmaTankBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
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
 * 等离子体燃料储罐：仅存储等离子体的专用储罐。
 * 罐层拒收非等离子体液体，管道对接仅限等离子体管道（家族隔离），单方块结构。
 */
public class AkaishiPlasmaTankBlock extends AkaishiMachineBlock {

    public AkaishiPlasmaTankBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_BLUE)
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_PLASMA_TANK.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // 逻辑只在服务端运行，客户端仅负责 GUI 展示
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_PLASMA_TANK.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_PLASMA_TANK.get(), AkaishiPlasmaTankBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // 右键打开储罐界面，展示当前等离子体储量
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AkaishiPlasmaTankBlockEntity tank) {
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
