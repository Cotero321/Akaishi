package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.menu.AkaishiWirelessPortMenu;
import com.example.akaishi.wireless.IWirelessPortHost;
import com.example.akaishi.wireless.WirelessNetworkManager;
import com.example.akaishi.wireless.WirelessTransferUtil;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * 无线赤能源输入口方块实体：能量管道 → 无线网络的发送端（远程设备，无需在外墙上）。
 * <p>
 * 手持已授权身份卡右键口即可绑定（绑定卡 UUID 经 NBT 持久化）。每 tick 按卡反查授权终端：
 * 认证成功则注册上线并推送缓冲能量（存入终端绑定储能，损耗按距离计算并消失）；
 * 认证失败/卡被撤销授权则自动下线（每 tick 校验天然支持「撤销即断连」）。
 * 无线传输不限速（无速率上限、无卡档概念）；口区块弱加载由终端区块加载构架统一管理。
 */
public class AkaishiWirelessInputPortBlockEntity extends BlockEntity implements IEnergyProvider, ExtendedMenuProvider, IDataCarrier, IWirelessPortHost {

    /** 缓冲容量 */
    public static final long BUFFER_CAPACITY = 100_000_000L;

    // ===== 数据槽 =====
    public static final int DATA_STORED_LOW = 0;
    public static final int DATA_STORED_HIGH = 1;
    public static final int DATA_CAPACITY_LOW = 2;
    public static final int DATA_CAPACITY_HIGH = 3;
    /** 绑定卡短 ID（hashCode；0=未绑定） */
    public static final int DATA_CARD_HASH = 4;
    /** 认证终端短 ID（hashCode；0=未认证） */
    public static final int DATA_TERMINAL_HASH = 5;
    public static final int DATA_AUTHENTICATED = 6;
    /** 方向标志（1=输出口，0=输入口；供客户端 GUI 显示方向提示） */
    public static final int DATA_IS_OUTPUT = 7;
    public static final int DATA_SLOTS = 8;

    private final AkaishiEnergyStorage buffer;
    private final SimpleContainerData data = new SimpleContainerData(DATA_SLOTS);
    /** 绑定身份卡 UUID（NBT 持久化）；null=未绑定 */
    private UUID boundCard;
    /** 当前认证成功的终端 ID（内存态，每 tick 重校验） */
    private UUID authenticatedTerminal;

    public AkaishiWirelessInputPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_WIRELESS_INPUT_PORT.get(), pos, state);
        this.buffer = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, BUFFER_CAPACITY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiWirelessInputPortBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        if (boundCard == null) {
            unregisterSelf();
        } else {
            AkaishiWirelessTerminalBlockEntity terminal = WirelessTransferUtil.resolveTerminal(level, boundCard);
            if (terminal != null) {
                UUID termId = terminal.terminalId();
                if (!termId.equals(authenticatedTerminal)) {
                    unregisterSelf(); // 换终端/重连：先注销旧注册再上线
                    authenticatedTerminal = termId;
                    WirelessNetworkManager.registerPort(termId, level.dimension(), worldPosition, true);
                }
                // 缓冲有能量 → 推送给认证终端（存入绑定储能；跨维度由 resolveTerminal 校验解锁）
                // 传输不限速：无速率上限，尽力一次清空缓冲（实收受储能容量限制）
                if (buffer.getEnergyStored() > 0) {
                    long toSend = buffer.getEnergyStored();
                    double loss = WirelessTransferUtil.lossRatio(level, worldPosition, terminal, false);
                    long sendNet = (long) (toSend * (1.0 - loss));
                    if (sendNet > 0) {
                        long sent = terminal.receiveWireless(sendNet); // 实收（储能可能满）
                        // 实扣 = 全额推走 - 储能拒收：损耗部分从缓冲蒸发，储能拒收部分退回缓冲
                        long taken = toSend - (sendNet - sent);
                        buffer.extractEnergy(taken, false);
                    }
                }
            } else {
                unregisterSelf();
            }
        }

        // 同步数据槽
        long stored = buffer.getEnergyStored();
        long max = buffer.getMaxEnergy();
        data.set(DATA_STORED_LOW, (int) stored);
        data.set(DATA_STORED_HIGH, (int) (stored >>> 32));
        data.set(DATA_CAPACITY_LOW, (int) max);
        data.set(DATA_CAPACITY_HIGH, (int) (max >>> 32));
        data.set(DATA_CARD_HASH, boundCard == null ? 0 : shortHash(boundCard));
        data.set(DATA_TERMINAL_HASH, authenticatedTerminal == null ? 0 : shortHash(authenticatedTerminal));
        data.set(DATA_AUTHENTICATED, authenticatedTerminal != null ? 1 : 0);
        data.set(DATA_IS_OUTPUT, 0);
    }

    // ===== 身份卡绑定 =====

    /** 绑定身份卡（手持卡右键口；覆盖旧绑定） */
    public void bind(UUID card) {
        if (card != null && !card.equals(boundCard)) {
            unregisterSelf();
            boundCard = card;
            setChanged();
        }
    }

    /** 解绑（GUI 按钮），断开与终端的所有连接 */
    public void unbind() {
        if (boundCard != null) {
            unregisterSelf();
            boundCard = null;
            setChanged();
        }
    }

    public boolean hasCard() {
        return boundCard != null;
    }

    /** 当前绑定卡短 ID 文本（GUI 显示） */
    public String cardShortId() {
        return boundCard == null ? "" : boundCard.toString().substring(0, 8).toUpperCase();
    }

    /** UUID 前 4 字节（高位 32 bit）：GUI 短 ID 显示的 8 位 hex，与卡片 ID 一致 */
    private static int shortHash(UUID id) {
        return (int) (id.getMostSignificantBits() >>> 32);
    }

    /** 供终端 purge 校验：该口当前是否认证于指定终端 */
    public boolean authenticated(UUID terminalId) {
        return terminalId != null && terminalId.equals(authenticatedTerminal);
    }

    /** 从注册表注销自己（未注册时幂等） */
    private void unregisterSelf() {
        if (authenticatedTerminal != null) {
            WirelessNetworkManager.unregisterPort(authenticatedTerminal, level.dimension(), worldPosition, true);
            authenticatedTerminal = null;
        }
    }

    @Override
    public ContainerData data() {
        return data;
    }

    @Override
    public boolean isOutput() {
        return false;
    }

    // ===== IEnergyProvider：纯接收端（管道注入，不可抽取） =====

    @Override
    public IEnergyStorage getEnergyStorage() {
        return buffer;
    }

    @Override
    public boolean canOutputEnergy() {
        return false;
    }

    @Override
    public boolean canInputEnergy() {
        return true;
    }

    // ===== 菜单 =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_wireless_input_port");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiWirelessPortMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (boundCard != null) {
            tag.putUUID("BoundCard", boundCard);
        }
        tag.putLong("Energy", buffer.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        boundCard = tag.hasUUID("BoundCard") ? tag.getUUID("BoundCard") : null;
        buffer.setEnergy(tag.getLong("Energy"));
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            unregisterSelf();
        }
        super.setRemoved();
    }
}
