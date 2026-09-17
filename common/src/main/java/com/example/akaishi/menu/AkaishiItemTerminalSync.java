package com.example.akaishi.menu;

import java.util.ArrayList;
import java.util.List;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiItemTerminalBlockEntity;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 物品终端库页网络包。
 * <p>
 * <b>为什么滚动不占网络</b>：库页槽位是客户端只读虚拟槽（同 AE2 的 {@code RepoSlot}），
 * 滚动位置纯客户端状态，因此不存在"翻页包"；服务端只知道玩家点了哪一条。
 * 本类只承载两件事：
 * <ul>
 *   <li>C2S 动作包：把「点中的条目 + 动作」送到服务端落账（AE2 {@code MEInteractionPacket} 的同构简化版）；</li>
 *   <li>S2C 条目快照：库内容变化时下发「展示堆 + 聚合总量」，客户端据此渲染虚拟槽。</li>
 * </ul>
 * 之所以必须下发：终端 / 单元方块实体都不覆写 {@code getUpdateTag}，且结构扫描只在服务端跑，
 * 客户端读不到库内容。
 */
public final class AkaishiItemTerminalSync {

    public static final ResourceLocation ACTION_CHANNEL =
            new ResourceLocation(AkaishiMod.MOD_ID, "item_terminal_action");
    public static final ResourceLocation ENTRIES_CHANNEL =
            new ResourceLocation(AkaishiMod.MOD_ID, "item_terminal_entries");

    /** 客户端只读条目：只含渲染所需字段，不含任何服务端分片信息 */
    public record Entry(ItemStack display, long amount) {
    }

    private AkaishiItemTerminalSync() {
    }

    // ===== C2S：库页交互 =====

    /** 服务端注册接收器（AkaishiMod.init 调用，客户端注册无害） */
    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ACTION_CHANNEL, (buf, context) -> {
            int containerId = buf.readInt();
            byte action = buf.readByte();
            ItemStack key = readFullStack(buf);
            context.queue(() -> {
                var player = context.getPlayer();
                if (player == null) {
                    return;
                }
                var menu = player.containerMenu;
                // 校验：必须是打开中的物品终端菜单且容器 ID 一致，拦截伪造包
                if (menu instanceof AkaishiItemTerminalMenu m && m.containerId == containerId) {
                    TerminalActions.perform(m.terminal(), m, player, action, key);
                }
            });
        });
    }

    /** 客户端：发送一次库页交互（key 为空堆表示点击空白格，只允许放入） */
    public static void sendAction(int containerId, byte action, ItemStack key) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(containerId);
        buf.writeByte(action);
        writeFullStack(buf, key);
        NetworkManager.sendToServer(ACTION_CHANNEL, buf);
    }

    // ===== S2C：条目快照 =====

    /** 客户端注册接收器（AkaishiMod.init 的 Env.CLIENT 分支调用） */
    public static void registerClient() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ENTRIES_CHANNEL, (buf, context) -> {
            int containerId = buf.readInt();
            boolean formed = buf.readBoolean();
            int revision = buf.readVarInt();
            int size = buf.readVarInt();
            List<Entry> entries = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                entries.add(new Entry(readFullStack(buf), buf.readVarLong()));
            }
            // 网络线程只做解码，落地回客户端主线程，避免与渲染线程并发读写
            Minecraft.getInstance().execute(() -> {
                var player = Minecraft.getInstance().player;
                if (player != null
                        && player.containerMenu instanceof AkaishiItemTerminalMenu menu
                        && menu.containerId == containerId) {
                    menu.acceptEntries(formed, revision, entries);
                }
            });
        });
    }

    /** 服务端：下发一次完整条目快照（库内容已变或菜单刚打开） */
    public static void sendEntries(ServerPlayer player, int containerId, boolean formed, int revision,
            List<TerminalEntry> entries) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(containerId);
        buf.writeBoolean(formed);
        buf.writeVarInt(revision);
        buf.writeVarInt(entries.size());
        for (TerminalEntry entry : entries) {
            writeFullStack(buf, entry.display());
            buf.writeVarLong(entry.amount());
        }
        NetworkManager.sendToPlayer(player, ENTRIES_CHANNEL, buf);
    }

    /**
     * 完整物品序列化（自研，替代 {@code writeItem}）。
     * <p>
     * {@code FriendlyByteBuf.writeItem} 只对「可损坏物品」或显式覆写
     * {@code shouldOverrideMultiplayerNbt} 的物品写 NBT，其余物品的<b>自定义 NBT 会被静默丢弃</b>。
     * 对物品库而言这是硬伤：客户端拿到的展示堆会缺 NBT（悬浮文本无法确认 NBT 功能），
     * 服务端也拿不到客户端点中的条目 NBT ⇒ 同物品但 NBT 不同的条目无法定位。
     */
    private static void writeFullStack(FriendlyByteBuf buf, ItemStack stack) {
        if (stack.isEmpty()) {
            buf.writeBoolean(false);
            return;
        }
        buf.writeBoolean(true);
        buf.writeId(BuiltInRegistries.ITEM, stack.getItem());
        buf.writeByte(stack.getCount());
        buf.writeNbt(stack.getTag());
    }

    /** 与 {@link #writeFullStack} 对称的读取 */
    private static ItemStack readFullStack(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return ItemStack.EMPTY;
        }
        Item item = buf.readById(BuiltInRegistries.ITEM);
        int count = buf.readByte();
        CompoundTag tag = buf.readNbt();
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item, count);
        stack.setTag(tag);
        return stack;
    }

    /** 供菜单按版本号判断是否需要重推（避免每 tick 空包） */
    public static List<TerminalEntry> snapshot(AkaishiItemTerminalBlockEntity terminal) {
        return TerminalInventory.snapshot(terminal.storageUnits());
    }
}
