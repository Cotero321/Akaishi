package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.multiblock.AkaishiMotherAltarStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 母神祭坛方块实体：承载唯一"供奉物"（单格、无 GUI 放取）。
 * - 供奉物不做掉落式容器：随方块 NBT 保存（拆走祭坛供奉不落地；爆炸摧毁则随之一并湮灭）
 * - 供奉变更后同步客户端（悬浮展示渲染依赖）
 * - 实现 IDataCarrier：挖掘掉落 / 创造中键拾取时保留供奉数据
 */
public class AkaishiMotherAltarBlockEntity extends BlockEntity implements IDataCarrier {

    private static final String TAG_OFFERING = "Offering";
    /** 结构检测节流计数（每 20 tick 检测一次，结构变化不频繁） */
    private int tick;

    private ItemStack offering = ItemStack.EMPTY;

    public AkaishiMotherAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_MOTHER_ALTAR.get(), pos, state);
    }

    public ItemStack getOffering() {
        return offering;
    }

    public boolean hasOffering() {
        return !offering.isEmpty();
    }

    /** 供奉成功：写入并同步客户端（仅服务端调用） */
    public void setOffering(ItemStack stack) {
        this.offering = stack.copy();
        sync();
    }

    /** 取回供奉物（仅服务端调用） */
    public ItemStack takeOffering() {
        ItemStack back = offering;
        offering = ItemStack.EMPTY;
        sync();
        return back;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiMotherAltarBlockEntity be) {
        be.tickServer();
    }

    /** 定期检测仪式结构：结构成型且供奉齐全时触发一次仪式 */
    private void tickServer() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (++tick % 20 != 0) {
            return;
        }
        AkaishiMotherAltarStructure.Result result = AkaishiMotherAltarStructure.scan(level, worldPosition);
        if (result == null) {
            return;
        }
        triggerRitual(result.altars());
    }

    /** 仪式结算：消耗全部供奉物 → 产出融合锭跳出 → 屏幕文字 + 视角颤抖 + 1s 黑暗 */
    private void triggerRitual(List<BlockPos> altars) {
        for (BlockPos p : altars) {
            if (level.getBlockEntity(p) instanceof AkaishiMotherAltarBlockEntity altar) {
                altar.setOffering(ItemStack.EMPTY);
            }
        }
        ItemStack result = new ItemStack(ModItems.lifeFusionIngot.get());
        ItemEntity item = new ItemEntity(level,
                worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, result);
        item.setDeltaMovement(0.0, 0.35, 0.0);
        level.addFreshEntity(item);
        if (level instanceof ServerLevel serverLevel) {
            double range = 32.0 * 32.0;
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) > range) {
                    continue;
                }
                player.connection.send(new ClientboundSetTitleTextPacket(
                        Component.translatable("message.akaishi.ritual.complete")));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20, 0));
            }
        }
    }

    /** 数据变更 → 落盘 + 向在线玩家广播方块实体数据包（悬浮渲染需客户端持有最新供奉物） */
    private void sync() {
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(this);
            for (ServerPlayer player : serverLevel.players()) {
                player.connection.send(packet);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        // 始终写入 Offering 键：空供奉也会写入空 ItemStack。
        // 否则 getUpdateTag() 返回空 tag，ClientboundBlockEntityDataPacket 会把它转成 null，
        // 客户端 onDataPacket 跳过 load，导致"取回后供奉物悬浮模型不消失"。
        tag.put(TAG_OFFERING, offering.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        offering = tag.contains(TAG_OFFERING)
                ? ItemStack.of(tag.getCompound(TAG_OFFERING)) : ItemStack.EMPTY;
    }
}
