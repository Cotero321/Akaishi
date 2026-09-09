package com.example.akaishi.block;

import com.example.akaishi.block.entity.AbstractMechanicalMachineBlockEntity;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * 机械域机器方块（模板制造厂/加工制作厂/组装加工台）。
 * 三种机器共用本类，方块实体类型与 ticker 由构造参数 supplier 延迟解引用
 * （ModBlockEntities 在方块之后注册，运行时求值安全）。
 */
public class AkaishiMechanicalMachineBlock extends AkaishiMachineBlock {

    private final Supplier<? extends BlockEntityType<? extends AbstractMechanicalMachineBlockEntity>> beType;

    public AkaishiMechanicalMachineBlock(Properties properties,
                                         Supplier<? extends BlockEntityType<? extends AbstractMechanicalMachineBlockEntity>> beType) {
        super(properties);
        this.beType = beType;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return beType.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != beType.get()) {
            return null;
        }
        return createTickerHelper(type, beType.get(),
                (l, p, s, be) -> AbstractMechanicalMachineBlockEntity.tick(l, p, s, be));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof AbstractMechanicalMachineBlockEntity machine) {
            // 主库存与独立升级库存均在破坏时掉落，避免写入 BlockEntityTag 后复制。
            Containers.dropContents(level, pos, machine);
            Containers.dropContents(level, pos, machine.getUpgradeSlots());
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AbstractMechanicalMachineBlockEntity machine) {
                MenuRegistry.openExtendedMenu(serverPlayer, machine);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
