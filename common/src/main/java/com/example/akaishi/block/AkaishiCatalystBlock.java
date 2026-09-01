package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiCatalystBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 赤石催化器：消耗赤能源"催生"范围内母岩的水晶簇生长。
 * 4 级（初/中/高/终）共用本方块类与方块实体，等级参数由构造器传入。
 */
public class AkaishiCatalystBlock extends BaseEntityBlock {

    /** 催化器等级：范围边长 / 单次催化成功率% / 每 tick 耗能（5 的 1-4 次方） */
    public enum CatalystTier {
        BASIC(3, 20, 5),
        MEDIUM(5, 30, 25),
        ADVANCED(9, 40, 125),
        ULTIMATE(16, 50, 625);

        /** 作用范围边长（催化器为中心） */
        public final int range;
        /** 单次催化成功率（%） */
        public final int efficiency;
        /** 每 tick 消耗赤能源 */
        public final int energyCost;

        CatalystTier(int range, int efficiency, int energyCost) {
            this.range = range;
            this.efficiency = efficiency;
            this.energyCost = energyCost;
        }
    }

    private final CatalystTier tier;

    /** 当前等级（供方块实体读取，等级由方块本身决定） */
    public CatalystTier tier() {
        return tier;
    }

    public AkaishiCatalystBlock(CatalystTier tier) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(3.5F)
                .sound(SoundType.METAL));
        this.tier = tier;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_CATALYST.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.CHISHI_CATALYST.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_CATALYST.get(), AkaishiCatalystBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // 右键打开工作界面：显示是否工作 + 能量槽
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AkaishiCatalystBlockEntity catalyst) {
                MenuRegistry.openExtendedMenu(serverPlayer, catalyst);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
