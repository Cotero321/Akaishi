package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AbstractMechanicalMachineBlockEntity;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 机械三机"制作"请求包（C2S）：客户端点按钮 → 服务端校验距离后置位单次制作请求。
 * 三机共用（模板制造厂 / 加工制作厂 / 组装加工台），不区分具体机型。
 */
public final class MechanicalCraftSync {

    public static final ResourceLocation CHANNEL = new ResourceLocation(AkaishiMod.MOD_ID, "mech_craft");

    /** 交互距离上限（平方）：与容器 stillValid 同量级，防远程伪造包 */
    private static final double MAX_DISTANCE_SQR = 64.0D;

    private MechanicalCraftSync() {}

    /** 注册 C2S 接收器（AkaishiMod.init 调用，服务端处理） */
    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CHANNEL, (buf, context) -> {
            BlockPos pos = buf.readBlockPos();
            // 调度到服务端主线程再访问方块实体，避免网络线程并发访问区块
            context.queue(() -> {
                Player player = context.getPlayer();
                if (player == null) {
                    return;
                }
                if (player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_DISTANCE_SQR) {
                    return;
                }
                if (player.level().getBlockEntity(pos) instanceof AbstractMechanicalMachineBlockEntity be) {
                    be.requestCraft();
                }
            });
        });
    }

    /** 客户端发送制作请求；pos 为空（BE 缺失兜底）时直接忽略 */
    public static void sendCraft(BlockPos pos) {
        if (pos == null) {
            return;
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(pos);
        NetworkManager.sendToServer(CHANNEL, buf);
    }
}
