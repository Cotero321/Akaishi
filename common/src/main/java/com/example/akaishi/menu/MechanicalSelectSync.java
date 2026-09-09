package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiMechanicalTemplateFactoryBlockEntity;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * 模板制造厂目标模板选择包（C2S）：客户端点选器官/部件 → 服务端写入方块实体选择并持久化。
 */
public final class MechanicalSelectSync {

    public static final ResourceLocation CHANNEL = new ResourceLocation(AkaishiMod.MOD_ID, "mech_select");

    private MechanicalSelectSync() {}

    /** 注册 C2S 接收器（AkaishiMod.init 调用，服务端处理） */
    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CHANNEL, (buf, context) -> {
            int organ = buf.readVarInt();
            int part = buf.readVarInt();
            BlockPos pos = buf.readBlockPos();
            // 调度到服务端主线程再访问方块实体，避免网络线程并发访问区块导致选择丢失
            context.queue(() -> {
                Player player = context.getPlayer();
                if (player == null || player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D
                        || !(player.containerMenu instanceof AkaishiMechanicalTemplateFactoryMenu menu)
                        || menu.getBlockPos() == null || !menu.getBlockPos().equals(pos) || !menu.stillValid(player)) {
                    return;
                }
                if (player.level().getBlockEntity(pos) instanceof AkaishiMechanicalTemplateFactoryBlockEntity be) {
                    be.setSelection(organ, part);
                }
            });
        });
    }

    /** 客户端发送选择（organ/part 序数）；pos 为空（BE 缺失兜底）时直接忽略 */
    public static void sendSelection(BlockPos pos, int organ, int part) {
        if (pos == null) {
            return;
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(organ);
        buf.writeVarInt(part);
        buf.writeBlockPos(pos);
        NetworkManager.sendToServer(CHANNEL, buf);
    }
}