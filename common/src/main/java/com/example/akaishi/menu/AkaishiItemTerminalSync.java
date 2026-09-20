package com.example.akaishi.menu;

import java.util.ArrayList;
import java.util.List;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.api.storage.IItemTerminalHost;
import com.example.akaishi.craft.VirtualCraftPlanner;

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

    /**
     * 条目快照条数上限（解码端防畸形包）。
     * <p>
     * 取 ≥ 结构扫描包络的理论上限：7³ 贴装区 − 3³ 内腔 = 316 个贴装位 × 54 槽 = 17,064 条。
     * 正常快照远小于此值，该上限只为拦住"对端报个天文数字 ⇒ 客户端预分配爆内存"。
     */
    private static final int MAX_SNAPSHOT_ENTRIES = 18_000;

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
            // 条数钳到上限：解码端不信任对端给的 size（畸形包会让客户端预分配爆内存）
            int count = Math.min(Math.max(size, 0), MAX_SNAPSHOT_ENTRIES);
            List<Entry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
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
     * <p>
     * 公开供同包其它同步包复用（如输入/输出口的过滤网快照），口径必须唯一，避免各自写一份再漂移。
     */
    public static void writeFullStack(FriendlyByteBuf buf, ItemStack stack) {
        if (stack.isEmpty()) {
            buf.writeBoolean(false);
            return;
        }
        buf.writeBoolean(true);
        buf.writeId(BuiltInRegistries.ITEM, stack.getItem());
        // 数量用 varint：虚拟加工单的件数上限是订单级的（可达 9999），单字节会在 128 处溢出成负数
        buf.writeVarInt(stack.getCount());
        buf.writeNbt(stack.getTag());
    }

    /** 与 {@link #writeFullStack} 对称的读取（同样供过滤网快照复用） */
    public static ItemStack readFullStack(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return ItemStack.EMPTY;
        }
        Item item = buf.readById(BuiltInRegistries.ITEM);
        // 数量钳制：本通道承载的最大合法件数是虚拟加工单的订单件数（可达 MAX_TARGET_COUNT），
        // 解码端不信任对端给的 varint（原版 readItem 用单字节天然受限，这里没有）
        int count = Math.min(Math.max(buf.readVarInt(), 0), VirtualCraftPlanner.MAX_TARGET_COUNT);
        CompoundTag tag = buf.readNbt();
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item, count);
        stack.setTag(tag);
        return stack;
    }

    /** 供菜单按版本号判断是否需要重推（避免每 tick 空包） */
    public static List<TerminalEntry> snapshot(IItemTerminalHost terminal) {
        return TerminalInventory.snapshot(terminal.storageUnits());
    }
}
