package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiReactorFuelPortBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
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
 * 燃料投放口：反应堆外壳上的燃料输入口（可多个，数据共享）。
 * 既作物流输入接口（管道插入燃料罐/取出空罐），也可右键打开 27 格燃料缓冲界面手动操作。
 */
public class AkaishiReactorFuelPortBlock extends AkaishiMachineBlock {

    public AkaishiReactorFuelPortBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_YELLOW)
                .sound(SoundType.METAL)
                );
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_REACTOR_FUEL_PORT.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_REACTOR_FUEL_PORT.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_REACTOR_FUEL_PORT.get(),
                AkaishiReactorFuelPortBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // 右键打开 27 格燃料缓冲界面（同族端口统一走 ExtendedMenuProvider 携带方块坐标）
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof AkaishiReactorFuelPortBlockEntity port) {
            MenuRegistry.openExtendedMenu(serverPlayer, port);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
