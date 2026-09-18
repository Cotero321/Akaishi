package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.api.miniature.IMiniatureChipView;
import com.example.akaishi.block.entity.MiniatureTerminalBlockEntity;
import com.example.akaishi.block.AkaishiMiniMatrixUpgradeType;

import net.minecraft.core.BlockPos;
import com.example.akaishi.block.entity.AkaishiMiniMatrixTerminalBlockEntity;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 微缩矩阵终端视图快照包（S2C，只读）。
 * <p>
 * 芯片列表是<b>方块实体的运行时数据</b>（名称 / 短 ID / 族类型 / IP 与能量读数），
 * 塞不进原版 ContainerData（只传定长 int），故按安全页同范式走一次快照包；
 * 服务端侧只在「视图版本号变化」时推送（见 {@code AkaishiMiniMatrixTerminalMenu}），免空包。
 * <p>
 * 解码端不信任对端给的条数：按 {@link #MAX_ROWS} 钳制，防畸形包在客户端预分配爆内存。
 */
public final class AkaishiMiniMatrixSync {

    /** S2C：矩阵视图快照（成型 + 升级数 + 芯片行） */
    public static final ResourceLocation SNAPSHOT_CHANNEL =
            new ResourceLocation(AkaishiMod.MOD_ID, "mini_matrix");

    /** 单次快照最多携带的芯片行数（5×5×5 表面最多约 98 格，留足余量的硬上限） */
    public static final int MAX_ROWS = 64;

    /** 单行文本长度上限（服务端与解码端同口径，防超长串） */
    private static final int MAX_NAME = 64;
    private static final int MAX_SHORT_ID = 32;
    private static final int MAX_TYPE = 96;

    /**
     * 客户端只读芯片行：未装载时除 {@code name} 外全为占位（{"-"} / 0）。
     * 容量为 0 表示该族无该项读数（界面据此不显示该项）。
     * <p>
     * {@code pos} = 该芯片方块的坐标：界面左列要把玩家"送到那枚芯片自己的界面"，
     * 客户端必须知道目标是哪一格；服务端收到后还会拿它对照已识别芯片做白名单校验。
     */
    public record ChipRow(boolean loaded, String name, String shortId, String type,
                          long ipStored, long ipCapacity, long energyStored, long energyCapacity, BlockPos pos) {
    }

    /** 接收方：只有矩阵终端菜单实现 */
    public interface Target {
        void acceptMatrix(boolean formed, int[] upgradeCounts, long pushedEnergy, long chipTransfer,
                List<ChipRow> chips);
    }

    private AkaishiMiniMatrixSync() {
    }

    /** 客户端注册接收器（AkaishiMod.init 的客户端分支调用） */
    public static void registerClient() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SNAPSHOT_CHANNEL, (buf, context) -> {
            int containerId = buf.readInt();
            boolean formed = buf.readBoolean();
            int[] upgrades = new int[AkaishiMiniMatrixUpgradeType.values().length];
            for (int i = 0; i < upgrades.length; i++) {
                upgrades[i] = buf.readVarInt();
            }
            long pushedEnergy = buf.readVarLong();
            long chipTransfer = buf.readVarLong();
            int declared = buf.readVarInt();
            int count = Math.min(Math.max(declared, 0), MAX_ROWS);
            List<ChipRow> rows = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                rows.add(new ChipRow(buf.readBoolean(), buf.readUtf(MAX_NAME), buf.readUtf(MAX_SHORT_ID),
                        buf.readUtf(MAX_TYPE), buf.readVarLong(), buf.readVarLong(),
                        buf.readVarLong(), buf.readVarLong(), buf.readBlockPos()));
            }
            // 网络线程只解码，落地回客户端主线程，避免与渲染线程并发读写
            Minecraft.getInstance().execute(() -> {
                var player = Minecraft.getInstance().player;
                if (player != null && player.containerMenu instanceof Target target
                        && player.containerMenu.containerId == containerId) {
                    target.acceptMatrix(formed, upgrades, pushedEnergy, chipTransfer, rows);
                }
            });
        });
    }

    /** 服务端：推送一次矩阵视图快照（视图版本变化时调用） */
    public static void sendSnapshot(ServerPlayer player, int containerId,
            AkaishiMiniMatrixTerminalBlockEntity be) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(containerId);
        buf.writeBoolean(be.isFormed());
        for (int i = 0; i < AkaishiMiniMatrixUpgradeType.values().length; i++) {
            buf.writeVarInt(be.upgradeCount(i));
        }
        // 上一轮能源直供量：随视图快照一起下去，界面才看得到"联动升级真的在跑"
        buf.writeVarLong(be.lastPushedEnergy());
        // 上一轮芯片间搬运量（赤能源芯片 → 储存终端）：与直供分开报，两者是不同的链路
        buf.writeVarLong(be.lastChipTransfer());
        List<IMiniatureChipView> chips = be.chips();
        int rows = Math.min(chips.size(), MAX_ROWS);
        buf.writeVarInt(rows);
        for (int i = 0; i < rows; i++) {
            writeRow(buf, chips.get(i));
        }
        NetworkManager.sendToPlayer(player, SNAPSHOT_CHANNEL, buf);
    }

    /**
     * 单行编码：未装载只发「空壳」标记，读数由容量 0 表达「无此项」。
     * <p>
     * <b>字段顺序必须与解码端逐字对齐</b>（含末尾的芯片坐标）：顺序一旦错位，
     * 解出来的就不是读数而是下一个字段的长度前缀，会以
     * {@code DecoderException: ... longer than maximum allowed} 直接踢掉客户端连接。
     */
    private static void writeRow(FriendlyByteBuf buf, IMiniatureChipView chip) {
        UUID id = chip.terminalId();
        ResourceLocation type = chip.chipType();
        IMiniatureChipView.IpReadout ip = chip.itemReadout();
        IMiniatureChipView.EnergyReadout energy = chip.energyReadout();
        buf.writeBoolean(chip.loaded());
        buf.writeUtf(chip.displayName().getString(), MAX_NAME);
        buf.writeUtf(id == null ? "-" : id.toString().substring(0, 8), MAX_SHORT_ID);
        buf.writeUtf(type == null ? "-" : type.toString(), MAX_TYPE);
        buf.writeVarLong(ip == null ? 0L : ip.stored());
        buf.writeVarLong(ip == null ? 0L : ip.capacity());
        buf.writeVarLong(energy == null ? 0L : energy.stored());
        buf.writeVarLong(energy == null ? 0L : energy.capacity());
        // 芯片坐标（末位，与读端一致）：左列页签据此把玩家送到"那枚芯片自己的界面"
        buf.writeBlockPos(chip instanceof MiniatureTerminalBlockEntity be ? be.getBlockPos() : BlockPos.ZERO);
    }
}
