package com.example.akaishi.life.body;

import com.example.akaishi.AkaishiMod;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Function;

/**
 * 玩家躯体状态网络同步（S2C）：
 * 服务端打开躯体检查仪时，将玩家 9 槽位器官 + 排斥值打包推送到客户端缓存；
 * 末尾附带"躯体总览"属性加成列表（身体系统当前实际生效的净加成，由平台注入计算器产出）。
 * 仅使用 Architectury Networking，common 无平台差异。
 */
public final class PlayerBodySync {

    public static final ResourceLocation CHANNEL = new ResourceLocation(AkaishiMod.MOD_ID, "player_body_state");

    /** 平台注入的躯体总览计算器（forge 读取玩家实际属性修饰聚合 + 被动叠加计数）；未注入时不发送总览字段 */
    private static Function<ServerPlayer, BodyOverviewResult> overviewProvider;

    private PlayerBodySync() {
    }

    /** 平台在初始化时注册总览计算器 */
    public static void registerOverviewProvider(Function<ServerPlayer, BodyOverviewResult> provider) {
        overviewProvider = provider;
    }

    /** 客户端注册接收器（AkaishiMod.init 的 Env.CLIENT 分支调用） */
    public static void registerClient() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, CHANNEL, (buf, context) -> {
            // 网络线程接收，保留缓冲区并调度到客户端主线程应用（避免与界面渲染并发）
            buf.retain();
            Minecraft.getInstance().execute(() -> {
                try {
                    ClientBodyData.apply(buf);
                } finally {
                    buf.release();
                }
            });
        });
    }

    /** 服务端：向指定玩家发送其躯体状态（打开检查仪/基因管理器时调用） */
    public static void sendToPlayer(ServerPlayer player, IPlayerBodyState state) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        // 基因强化列表（来源 → 适配加成）
        java.util.Map<String, Integer> genes = state.getGeneBonuses();
        buf.writeVarInt(genes.size());
        for (java.util.Map.Entry<String, Integer> entry : genes.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeInt(entry.getValue());
        }
        for (BodySlot slot : BodySlot.values()) {
            buf.writeUtf(slot.getId());
            ItemStack organ = state.getOrgan(slot);
            buf.writeBoolean(!organ.isEmpty());
            if (!organ.isEmpty()) {
                buf.writeItem(organ);
            }
            buf.writeInt(state.getRejection(slot));
        }
        // 突破激活（单条：来源 + 额外适配 + 基础% + 截止时刻；无激活写 false）
        buf.writeBoolean(state.hasActiveBreakthrough());
        if (state.hasActiveBreakthrough()) {
            buf.writeUtf(state.getBreakthroughEntity());
            buf.writeInt(state.getBreakthroughExtra());
            buf.writeInt(state.getBreakthroughPct());
            buf.writeLong(state.getBreakthroughUntil());
        }
        // 躯体总览：身体系统当前实际生效的属性净加成 + 被动叠加计数
        BodyOverviewResult result = overviewProvider == null ? null : overviewProvider.apply(player);
        List<BodyOverviewEntry> overview = result == null ? List.of() : result.attributes();
        buf.writeVarInt(overview.size());
        for (BodyOverviewEntry entry : overview) {
            buf.writeUtf(entry.attributeKey());
            buf.writeDouble(entry.value());
        }
        List<BodyPassiveEntry> passives = result == null ? List.of() : result.passives();
        buf.writeVarInt(passives.size());
        for (BodyPassiveEntry entry : passives) {
            buf.writeUtf(entry.passiveId());
            buf.writeVarInt(entry.count());
        }
        NetworkManager.sendToPlayer(player, CHANNEL, buf);
    }
}
