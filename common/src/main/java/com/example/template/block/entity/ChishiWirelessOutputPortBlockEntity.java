package com.example.template.block.entity;

import com.example.template.api.IDataCarrier;
import com.example.template.api.energy.IEnergyProvider;
import com.example.template.api.energy.IEnergyStorage;
import com.example.template.item.IdentityCardTier;
import com.example.template.energy.ChishiEnergyStorage;
import com.example.template.energy.ChishiEnergyType;
import com.example.template.menu.ChishiWirelessPortMenu;
import com.example.template.wireless.IWirelessPortHost;
import com.example.template.wireless.WirelessNetworkManager;
import com.example.template.wireless.WirelessTransferUtil;
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
 * 无线赤能源输出口方块实体：无线网络 → 能量管道的接收端（远程设备，无需在外墙上）。
 * <p>
 * 手持已授权身份卡右键口即可绑定（绑定卡 UUID 经 NBT 持久化）。每 tick 按卡反查授权终端：
 * 认证成功则注册上线并按需从终端绑定储能拉取能量存入缓冲（纯发电不可注入，贴邻管道抽取供给机器）；
 * 认证失败/卡被撤销授权则自动下线。损耗按距离从储能侧扣除（损耗在途中消失）。
 */
public class ChishiWirelessOutputPortBlockEntity extends BlockEntity implements IEnergyProvider, ExtendedMenuProvider, IDataCarrier, IWirelessPortHost {

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
    public static final int DATA_RATE_LOW = 7;
    public static final int DATA_RATE_HIGH = 8;
    /** 方向标志（1=输出口，0=输入口；供客户端 GUI 显示方向提示） */
    public static final int DATA_IS_OUTPUT = 9;
    public static final int DATA_SLOTS = 10;

    private final ChishiEnergyStorage buffer;
    private final SimpleContainerData data = new SimpleContainerData(DATA_SLOTS);
    /** 绑定身份卡 UUID（NBT 持久化）；null=未绑定 */
    private UUID boundCard;
    /** 绑定卡的等级（绑定时快照，NBT 持久化；未来卡升级后需重新绑定生效） */
    private IdentityCardTier boundTier = IdentityCardTier.BASIC;
    /** 当前认证成功的终端 ID（内存态，每 tick 重校验） */
    private UUID authenticatedTerminal;

    public ChishiWirelessOutputPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_WIRELESS_OUTPUT_PORT.get(), pos, state);
        this.buffer = new ChishiEnergyStorage(ChishiEnergyType.INSTANCE, BUFFER_CAPACITY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiWirelessOutputPortBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        if (boundCard == null) {
            unregisterSelf();
        } else {
            ChishiWirelessTerminalBlockEntity terminal = WirelessTransferUtil.resolveTerminal(level, boundCard);
            if (terminal != null) {
                UUID termId = terminal.terminalId();
                if (!termId.equals(authenticatedTerminal)) {
                    unregisterSelf(); // 换终端/重连：先注销旧注册再上线
                    authenticatedTerminal = termId;
                    WirelessNetworkManager.registerPort(termId, level.dimension(), worldPosition, false);
                }
                // 缓冲未满 → 从认证终端（绑定储能）按需拉取；跨维度由 resolveTerminal 校验解锁
                long space = buffer.getMaxEnergy() - buffer.getEnergyStored();
                if (space > 0) {
                    long want = Math.min(space, WirelessTransferUtil.transferRate(boundTier));
                    double loss = WirelessTransferUtil.lossRatio(level, worldPosition, terminal, true);
                    // 从储能扣除 = 需求 / (1-损耗)，损耗部分在途中消失
                    long drawGross = (long) Math.min(want / (1.0 - loss), Long.MAX_VALUE);
                    if (drawGross > 0) {
                        long got = terminal.extractWireless(drawGross);
                        long net = Math.min(got, want);
                        buffer.addEnergy(net, false);
                    }
                }
            } else {
                unregisterSelf();
            }
        }

        // 同步数据槽
        long stored = buffer.getEnergyStored();
        long max = buffer.getMaxEnergy();
        long rate = WirelessTransferUtil.transferRate(boundTier);
        data.set(DATA_STORED_LOW, (int) stored);
        data.set(DATA_STORED_HIGH, (int) (stored >>> 32));
        data.set(DATA_CAPACITY_LOW, (int) max);
        data.set(DATA_CAPACITY_HIGH, (int) (max >>> 32));
        data.set(DATA_CARD_HASH, boundCard == null ? 0 : shortHash(boundCard));
        data.set(DATA_TERMINAL_HASH, authenticatedTerminal == null ? 0 : shortHash(authenticatedTerminal));
        data.set(DATA_AUTHENTICATED, authenticatedTerminal != null ? 1 : 0);
        data.set(DATA_RATE_LOW, (int) rate);
        data.set(DATA_RATE_HIGH, (int) (rate >>> 32));
        data.set(DATA_IS_OUTPUT, 1);
    }

    // ===== 身份卡绑定 =====

    /** 绑定身份卡（手持卡右键口；覆盖旧绑定），同时快照卡等级 */
    public void bind(UUID card, IdentityCardTier tier) {
        if (card != null && !card.equals(boundCard)) {
            unregisterSelf();
            boundCard = card;
            boundTier = tier == null ? IdentityCardTier.BASIC : tier;
            setChanged();
        }
    }

    /** 解绑（GUI 按钮），断开与终端的所有连接 */
    public void unbind() {
        if (boundCard != null) {
            unregisterSelf();
            boundCard = null;
            boundTier = IdentityCardTier.BASIC;
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

    /** 当前绑定卡等级（GUI 显示） */
    public IdentityCardTier boundTier() {
        return boundTier;
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
            WirelessNetworkManager.unregisterPort(authenticatedTerminal, level.dimension(), worldPosition, false);
            authenticatedTerminal = null;
        }
    }

    @Override
    public ContainerData data() {
        return data;
    }

    @Override
    public boolean isOutput() {
        return true;
    }

    // ===== IEnergyProvider：纯发电端（管道抽取，不可注入） =====

    @Override
    public IEnergyStorage getEnergyStorage() {
        return buffer;
    }

    @Override
    public boolean canOutputEnergy() {
        return true;
    }

    @Override
    public boolean canInputEnergy() {
        return false;
    }

    // ===== 菜单 =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.template_mod.chishi_wireless_output_port");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ChishiWirelessPortMenu(id, inv, this);
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
            tag.putInt("BoundTier", boundTier.id());
        }
        tag.putLong("Energy", buffer.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        boundCard = tag.hasUUID("BoundCard") ? tag.getUUID("BoundCard") : null;
        boundTier = boundCard == null ? IdentityCardTier.BASIC : IdentityCardTier.byId(tag.getInt("BoundTier"));
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
