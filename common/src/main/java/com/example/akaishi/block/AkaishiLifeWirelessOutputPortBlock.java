package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiLifeWirelessOutputPortBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import com.example.akaishi.item.AkaishiWirelessIdentityCardItem;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

import java.util.UUID;

/**
 * 生命无线输出口：生命无线网络 → 生命能量管道的接收端方块（远程设备，无需在外墙上）。
 * 手持身份卡右键即绑定（覆盖旧绑定）；空手/其他物品右键打开口界面（运行/传输 两页）。
 * 镜像赤能源版 {@link AkaishiWirelessOutputPortBlock}，生命白调。
 */
public class AkaishiLifeWirelessOutputPortBlock extends AkaishiMachineBlock {

    public AkaishiLifeWirelessOutputPortBlock() {
        super(Properties.of()
                .mapColor(MapColor.SNOW)
                .strength(5.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_LIFE_WIRELESS_OUTPUT_PORT.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_LIFE_WIRELESS_OUTPUT_PORT.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_LIFE_WIRELESS_OUTPUT_PORT.get(),
                AkaishiLifeWirelessOutputPortBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AkaishiLifeWirelessOutputPortBlockEntity port
                && player instanceof ServerPlayer serverPlayer) {
            ItemStack held = player.getItemInHand(hand);
            // 手持身份卡右键 → 绑定到本口（覆盖旧绑定）：身份取卡上绑定的玩家，卡未绑定则用右键玩家自己
            if (held.getItem() instanceof AkaishiWirelessIdentityCardItem) {
                AkaishiWirelessIdentityCardItem.ensureUuid(held); // 服务端为卡生成唯一卡号（保证短 ID 可显示）
                UUID identity = AkaishiWirelessIdentityCardItem.playerOf(held);
                port.bind(identity == null ? player.getUUID() : identity);
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.akaishi.port.bound", AkaishiWirelessIdentityCardItem.shortId(held)), false);
                return InteractionResult.sidedSuccess(false);
            }
            MenuRegistry.openExtendedMenu(serverPlayer, port);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AkaishiLifeWirelessOutputPortBlockEntity p) {
            p.unbind(); // 注销网络上线 + 清绑定（网络注册表防悬空）
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
