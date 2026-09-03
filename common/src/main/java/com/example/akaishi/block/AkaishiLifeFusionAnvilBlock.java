package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiLifeFusionAnvilBlockEntity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 生命的融合砧：赤石护甲 + 生命的融合锭 → 生命融合护甲（保留升级数据）。
 * 换色铁砧外观，右键打开融合界面，无能量消耗。
 */
public class AkaishiLifeFusionAnvilBlock extends AkaishiMachineBlock {

    public AkaishiLifeFusionAnvilBlock() {
        super(Properties.of().mapColor(MapColor.METAL).strength(5.0F, 1200.0F).sound(SoundType.ANVIL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_LIFE_FUSION_ANVIL.get().create(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AkaishiLifeFusionAnvilBlockEntity be) {
                MenuRegistry.openExtendedMenu(serverPlayer, be);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
