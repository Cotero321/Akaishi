package com.example.akaishi.block.entity;

import com.example.akaishi.block.AkaishiMiniMatrixNetworkNodeBlock;
import com.example.akaishi.menu.AkaishiMiniMatrixNodeMenu;
import com.example.akaishi.wireless.WirelessNodeRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import dev.architectury.registry.menu.ExtendedMenuProvider;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * 无线网络节点方块实体：<b>节点登记表的生命周期</b> + <b>申领方归属者同步</b>（无界面、无 tick、无落盘）。
 * <p>
 * <b>为什么要它</b>：登记表原本只靠方块的 {@code onPlace}/{@code onRemove} 写入，是纯内存的静态表 ——
 * 服务器重启后世界里的节点仍在，但表空了，微缩矩阵终端申领不到节点 ⇒「无线拓展升级」静默失效，
 * 玩家只能把节点拆掉重放。挂上方块实体后，登记改由方块实体的生命周期驱动：
 * {@code setLevel}（区块加载读档 / 放置）自动登记、{@code setRemoved}（区块卸载 / 被拆）自动注销。
 * <p>
 * <b>归属者同步</b>：节点子场域屏障要与主场域同口径地遵守「仅归属者/同队可见」配置，
 * 而"谁是归属者"只有申领的矩阵终端知道 —— 故由终端在申领时写入，经方块实体同步标签下发客户端。
 * 客户端用 {@code getUpdateTag}（随区块下发），服务端不落盘（真源在矩阵终端的申领名单里）。
 * <p>
 * <b>界面</b>：右键打开 {@link AkaishiMiniMatrixNodeMenu}（176×112 小面板）——只读展示绑定终端 /
 * 归属者 / 子场域，外加<b>本节点独立</b>的「节点屏障」开关；归属者与开关都不落盘之外的东西，
 * 屏障开关本身持久化（玩家关了就是关了）。
 */
public class AkaishiMiniMatrixNetworkNodeBlockEntity extends BlockEntity implements ExtendedMenuProvider {

    /** 同步标签键：申领方归属者（与矩阵终端同名同义） */
    private static final String TAG_CLAIMANT = "FieldOwner";
    private static final String TAG_CLAIMANT_NAME = "FieldOwnerName";
    /** 节点屏障开关（每个节点独立，默认开） */
    private static final String TAG_BARRIER = "Barrier";

    /** 申领方归属者（服务端由矩阵终端写入；客户端由同步标签带下来） */
    @Nullable
    private UUID claimantId;
    @Nullable
    private String claimantName;
    /** 申领方（矩阵终端）坐标：界面显示"绑定终端"用；不落盘，只随同步标签下发 */
    @Nullable
    private BlockPos claimantPos;
    /** 节点屏障是否显示（默认开）：每个节点独立，落盘 + 随同步标签下发 */
    private boolean barrierEnabled = true;
    /** 是否已挂着"自持弱加载票"（见 {@link #attachSelfTicket()}） */
    private boolean selfTicketAttached;
    /** 连续多少 tick 处于"状态说已申领、却没有任何终端认领"（幽灵判据） */
    private int orphanTicks;
    /**
     * 幽灵自愈宽限期（tick）：矩阵终端重启后要在首轮重扫（≤20 tick）里重新申领，这里留 5 倍余量。
     * 超过它仍无人认领 ⇒ 判定为残留状态，由节点自己清掉（见 {@link #tick}）。
     */
    private static final int ORPHAN_GRACE_TICKS = 100;

    public AkaishiMiniMatrixNetworkNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_MINI_MATRIX_NETWORK_NODE.get(), pos, state);
    }

    // ===== 界面（节点 GUI） =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.akaishi.matrix.node.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiMiniMatrixNodeMenu(id, inv, this);
    }

    /** 客户端据此定位方块实体；节点读数全在同步标签里，故不需要额外字段 */
    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    /**
     * 登记时机取 {@code setLevel}：原版在"区块加载读档"与"方块放置"两条路径上都会调用它
     * （{@code LevelChunk#setBlockEntity}），而 Forge 的 {@code onLoad} 钩子在 common 模块不可用
     * （common 编译面是原版 merged jar）。客户端也会走到这里，故用 {@link ServerLevel} 收窄。
     */
    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level instanceof ServerLevel serverLevel) {
            WirelessNodeRegistry.register(serverLevel, worldPosition);
        }
    }

    /** 区块卸载 / 方块被拆：注销登记并摘掉自持票据。场域票据由申领方管理，此处不释放 */
    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            WirelessNodeRegistry.unregister(serverLevel, worldPosition);
        }
        detachSelfTicket();
        super.setRemoved();
    }

    /**
     * 节点自持弱加载票：只要方块状态是"已申领"，节点就钉住自己所在的那一个区块。
     * <p>
     * <b>为什么必须由节点自己持有</b>（用户口径「给节点增加弱加载能力」）：
     * 申领方（矩阵终端）的票据在<b>释放时会被摘掉</b>，而释放要写 {@code ACTIVE = false}，
     * 这需要区块处于加载态。一旦那一步时区块恰好没加载（重启后票据尚未重挂、或票据被同区块的
     * 其它场域摘走），这一笔就写不进去，而释放后没人再碰这个坐标 ⇒ 场域屏障永久残留（幽灵场域）。
     * 节点自持票据后，"写的时候一定加载着"变成<b>结构性保证</b>，不再依赖调用顺序。
     * <p>
     * 代价与护栏：被钉住的只有节点自己那 1 个区块，且仅在"状态为已申领"期间；
     * 若这份状态其实是残留（无人认领），{@link #tick} 会在宽限期后自愈清除 —— 否则就成了区块加载器漏洞。
     */
    private void attachSelfTicket() {
        if (selfTicketAttached || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(worldPosition),
                0, worldPosition);
        selfTicketAttached = true;
    }

    private void detachSelfTicket() {
        if (!selfTicketAttached || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.getChunkSource().removeRegionTicket(TicketType.PORTAL, new ChunkPos(worldPosition),
                0, worldPosition);
        selfTicketAttached = false;
    }

    /**
     * 服务端逐 tick 自检（由方块注册 ticker）：
     * <ul>
     *   <li>状态为"已申领" ⇒ 挂着自持票据，并把"无人认领"的计时清零；</li>
     *   <li>状态为"未申领" ⇒ 摘掉自持票据，不留加载开销；</li>
     *   <li>状态说已申领、却连续 {@link #ORPHAN_GRACE_TICKS} tick 无人认领 ⇒
     *       判定残留状态并自清（此刻区块由自持票据钉着，这一笔一定写得进）。</li>
     * </ul>
     */
    public static void tick(Level level, BlockPos pos, BlockState state,
            AkaishiMiniMatrixNetworkNodeBlockEntity node) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return; // 客户端不参与（该状态由服务端权威写入并同步下发）
        }
        if (!state.getValue(AkaishiMiniMatrixNetworkNodeBlock.ACTIVE)) {
            node.orphanTicks = 0;
            node.detachSelfTicket();
            return;
        }
        node.attachSelfTicket();
        if (node.claimantId != null) {
            node.orphanTicks = 0;
            return;
        }
        if (++node.orphanTicks > ORPHAN_GRACE_TICKS) {
            node.orphanTicks = 0;
            serverLevel.setBlock(pos,
                    state.setValue(AkaishiMiniMatrixNetworkNodeBlock.ACTIVE, false), 3);
        }
    }

    /**
     * 申领方（矩阵终端）写入/清除归属者，供客户端做屏障可见性过滤。
     * <p>
     * <b>幂等</b>：值未变不刷包 —— 申领每 {@code RESCAN_INTERVAL} tick 复核一次，
     * 不幂等就会变成每 20 tick 一次的同步风暴。
     */
    public void setClaimant(@Nullable UUID id, @Nullable String name, @Nullable BlockPos pos) {
        if (Objects.equals(claimantId, id) && Objects.equals(claimantName, name)
                && Objects.equals(claimantPos, pos)) {
            return;
        }
        claimantId = id;
        claimantName = name;
        claimantPos = pos == null ? null : pos.immutable();
        // 被认领即钉住自己（释放后的残留由 tick 的宽限期自愈兜底）
        if (id != null) {
            orphanTicks = 0;
            attachSelfTicket();
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** 申领方归属者 UUID（未申领为 null） */
    @Nullable
    public UUID claimantId() {
        return claimantId;
    }

    /** 申领方归属者名字（离线也能判同队，故同步名字而不只 UUID） */
    @Nullable
    public String claimantName() {
        return claimantName;
    }

    /** 申领方（矩阵终端）坐标；未申领为 null。界面「绑定终端」一行的数据源 */
    @Nullable
    public BlockPos claimantPos() {
        return claimantPos;
    }

    /** 节点屏障是否显示（默认开）：只有开启时才画那 1 区块子场域 */
    public boolean barrierEnabled() {
        return barrierEnabled;
    }

    /**
     * 切换节点屏障（服务端调用）：改状态 → 落盘 → 推客户端。
     * <p>必须显式 {@code sendBlockUpdated}：{@code setChanged()} 只标脏存盘，原版不会因此发数据包，
     * 而渲染端与界面都读的是客户端那份。
     */
    public void setBarrierEnabled(boolean enabled) {
        if (barrierEnabled == enabled) {
            return;
        }
        barrierEnabled = enabled;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** 同步标签：带归属者与屏障开关（归属者不落盘，故不进 saveAdditional） */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        if (claimantId != null) {
            tag.putUUID(TAG_CLAIMANT, claimantId);
        }
        if (claimantName != null && !claimantName.isEmpty()) {
            tag.putString(TAG_CLAIMANT_NAME, claimantName);
        }
        if (claimantPos != null) {
            tag.putLong("FieldOwnerPos", claimantPos.asLong());
        }
        tag.putBoolean(TAG_BARRIER, barrierEnabled);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID(TAG_CLAIMANT)) {
            claimantId = tag.getUUID(TAG_CLAIMANT);
        }
        if (tag.contains(TAG_CLAIMANT_NAME)) {
            claimantName = tag.getString(TAG_CLAIMANT_NAME);
        }
        if (tag.contains("FieldOwnerPos")) {
            claimantPos = BlockPos.of(tag.getLong("FieldOwnerPos"));
        }
        if (tag.contains(TAG_BARRIER)) {
            barrierEnabled = tag.getBoolean(TAG_BARRIER);
        }
    }

    /** 只有屏障开关需要落盘（归属者是"谁在申领"的运行时事实，重载后由矩阵终端重新写入） */
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean(TAG_BARRIER, barrierEnabled);
    }
}
