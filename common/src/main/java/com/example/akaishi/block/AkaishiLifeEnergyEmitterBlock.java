package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiLifeEnergyEmitterBlockEntity;
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
 * 生命能量发射器方块：从相邻生命能量源抽能蓄满，朝权杖绑定的坐标射出能量弹。
 * 右键（非权杖手持）打开极简界面查看能量与绑定坐标；用生命能量权杖右键则可绑定目标。
 * <p>
 * 炮口朝向不落在 BlockState 上（避免只有 6 向的限制）：方块本体 {@link RenderShape#INVISIBLE}，
 * 整机由 BER 按"方块中心 → 目标"连续向量旋转绘制，实现 360° 平滑瞄准。
 */
public class AkaishiLifeEnergyEmitterBlock extends AkaishiMachineBlock {

    public AkaishiLifeEnergyEmitterBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_GREEN)
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_LIFE_ENERGY_EMITTER.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_LIFE_ENERGY_EMITTER.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_LIFE_ENERGY_EMITTER.get(),
                AkaishiLifeEnergyEmitterBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AkaishiLifeEnergyEmitterBlockEntity emitter) {
                MenuRegistry.openExtendedMenu(serverPlayer, emitter);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // 本体不绘制，整机交由 LifeEnergyEmitterRenderer 连续旋转绘制
        return RenderShape.INVISIBLE;
    }
}
