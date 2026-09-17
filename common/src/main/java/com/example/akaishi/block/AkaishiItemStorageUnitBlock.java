package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiItemStorageUnitBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

/**
 * 物品储存单元：贴装于物品终端外侧 1 格（D5）的 IP 容量方块。
 * <p>
 * 按 {@link ItemStorageUnitTier} 参数化，三阶共用一个方块实体类型，等级由方块本身决定；
 * 本机为<b>被动存储</b>：物品进出只由物品终端驱动，不开放第三方物流槽（用户裁定），
 * 从根上杜绝外部 insert 绕过 {@code slotIp} 账本造成容量与计费失守。
 * <p>
 * 继承 {@link AkaishiMachineBlock} ⇒ 拆卸/中键拾取时容器与账本随掉落物完整保留（D10 无偏差）。
 */
public class AkaishiItemStorageUnitBlock extends AkaishiMachineBlock {

    private final ItemStorageUnitTier tier;

    public AkaishiItemStorageUnitBlock(ItemStorageUnitTier tier) {
        super(Properties.of().strength(4.0F, 6.0F).requiresCorrectToolForDrops());
        this.tier = tier;
    }

    /** 本方块对应的单元等级（方块实体据此决定 IP 容量） */
    public ItemStorageUnitTier getTier() {
        return tier;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_ITEM_STORAGE_UNIT.get().create(pos, state);
    }

    /** 无方块实体渲染器，走普通模型渲染 */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** 右键打开只读视图（D18）：只能看，存取一律回物品终端 */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AkaishiItemStorageUnitBlockEntity unit) {
                MenuRegistry.openExtendedMenu(serverPlayer, unit);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
