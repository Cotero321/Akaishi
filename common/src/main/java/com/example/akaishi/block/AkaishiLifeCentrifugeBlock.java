package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiLifeCentrifugeBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
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
 * 生命离心机：消耗赤能源将活化衰竭液体分离为活化结晶（主）+ 衰竭结晶（副）。
 * 无方向属性；液体经普通液体管道注入输入罐，产物由玩家从 GUI 取出，破坏时产物掉落。
 */
public class AkaishiLifeCentrifugeBlock extends AkaishiMachineBlock {

    public AkaishiLifeCentrifugeBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_CYAN)
                .strength(5.0F)
                .sound(SoundType.METAL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_LIFE_CENTRIFUGE.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_LIFE_CENTRIFUGE.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_LIFE_CENTRIFUGE.get(),
                AkaishiLifeCentrifugeBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AkaishiLifeCentrifugeBlockEntity centrifuge) {
                MenuRegistry.openExtendedMenu(serverPlayer, centrifuge);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AkaishiLifeCentrifugeBlockEntity centrifuge) {
            // 输出槽产物掉落（输入罐为安全活化液体，无需触发衰竭区域）
            Containers.dropContents(level, pos, centrifuge.outputContainer());
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
