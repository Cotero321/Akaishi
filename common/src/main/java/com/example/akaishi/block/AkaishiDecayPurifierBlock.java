package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiDecayPurifierBlockEntity;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 衰变净化塔：消耗赤能源净化范围内衰竭区域的单方块机器。
 * 效果：每 tick 消耗能量，加速范围内同维度衰竭区域的自然消散（削减剩余时间）。
 * 无物品槽位；能量仅由赤能源管道输入；右键打开工作界面。
 */
public class AkaishiDecayPurifierBlock extends BaseEntityBlock {

    public AkaishiDecayPurifierBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GREEN)
                .strength(3.5F)
                .sound(SoundType.METAL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_DECAY_PURIFIER.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.CHISHI_DECAY_PURIFIER.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_DECAY_PURIFIER.get(), AkaishiDecayPurifierBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // 右键打开工作界面：能量条 + 净化状态 + 范围内区域数 + 升级槽
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AkaishiDecayPurifierBlockEntity purifier) {
                MenuRegistry.openExtendedMenu(serverPlayer, purifier);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
