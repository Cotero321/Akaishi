package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiMotherAltarBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import com.example.akaishi.life.altar.AkaishiOfferingInspector;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 * 母神祭坛：生命线终局多方块祭坛的核心，向"黑山羊之母"献上生命造物。
 * 无 GUI 交互：持"生命造物"右键 → 供奉（NBT 识别，认可才收下）；空手右键 → 取回；
 * 供奉物悬浮于祭坛上方缓慢旋转（客户端渲染）。
 * 材质参考黑曜石（硬度 50 / 抗爆 1200），象征不可亵渎的神龛。
 */
public class AkaishiMotherAltarBlock extends AkaishiMachineBlock {

    public AkaishiMotherAltarBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(50.0F, 1200.0F)
                .sound(SoundType.STONE));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_MOTHER_ALTAR.get().create(pos, state);
    }

    /** 祭坛用普通方块模型渲染本体（BaseEntityBlock 默认 INVISIBLE 会导致本体透明） */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_MOTHER_ALTAR.get(),
                AkaishiMotherAltarBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS; // 放/取由服务端权威处理，客户端放行等待同步
        }
        if (!(level.getBlockEntity(pos) instanceof AkaishiMotherAltarBlockEntity altar)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!altar.hasOffering()) {
            // 祭坛空置：持物 → 尝试供奉；空手 → 提示空荡
            if (held.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.akaishi.altar.empty"), false);
                return InteractionResult.CONSUME;
            }
            AkaishiOfferingInspector.OfferingInfo info = AkaishiOfferingInspector.inspect(held);
            if (!info.accepted()) {
                player.displayClientMessage(Component.translatable("message.akaishi.altar.place_refused"), false);
                return InteractionResult.CONSUME;
            }
            // 母神认可：取 1 件悬浮供奉（创造模式不消耗实物）
            ItemStack offering = held.copyWithCount(1);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            altar.setOffering(offering);
            player.displayClientMessage(Component.translatable("message.akaishi.altar.place_ok"), false);
            player.displayClientMessage(
                    Component.translatable("message.akaishi.altar.whisper", AkaishiOfferingInspector.whisper(offering)), false);
            return InteractionResult.CONSUME;
        }
        // 已有供奉：空手 → 取回；持物 → 提示先取回
        if (held.isEmpty()) {
            ItemStack back = altar.takeOffering();
            if (!player.getInventory().add(back)) {
                player.drop(back, false);
            }
            player.displayClientMessage(Component.translatable("message.akaishi.altar.take_ok"), false);
        } else {
            player.displayClientMessage(Component.translatable("message.akaishi.altar.occupied"), false);
        }
        return InteractionResult.CONSUME;
    }
}
