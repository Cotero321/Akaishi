package com.example.akaishi.block.entity;

import com.example.akaishi.wireless.WirelessNodeRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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
 */
public class AkaishiMiniMatrixNetworkNodeBlockEntity extends BlockEntity {

    /** 同步标签键：申领方归属者（与矩阵终端同名同义） */
    private static final String TAG_CLAIMANT = "FieldOwner";
    private static final String TAG_CLAIMANT_NAME = "FieldOwnerName";

    /** 申领方归属者（服务端由矩阵终端写入；客户端由同步标签带下来） */
    @Nullable
    private UUID claimantId;
    @Nullable
    private String claimantName;

    public AkaishiMiniMatrixNetworkNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_MINI_MATRIX_NETWORK_NODE.get(), pos, state);
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

    /** 区块卸载 / 方块被拆：注销登记。场域票据由申领方管理，此处不释放 */
    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            WirelessNodeRegistry.unregister(serverLevel, worldPosition);
        }
        super.setRemoved();
    }

    /**
     * 申领方（矩阵终端）写入/清除归属者，供客户端做屏障可见性过滤。
     * <p>
     * <b>幂等</b>：值未变不刷包 —— 申领每 {@code RESCAN_INTERVAL} tick 复核一次，
     * 不幂等就会变成每 20 tick 一次的同步风暴。
     */
    public void setClaimant(@Nullable UUID id, @Nullable String name) {
        if (Objects.equals(claimantId, id) && Objects.equals(claimantName, name)) {
            return;
        }
        claimantId = id;
        claimantName = name;
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

    /** 同步标签：只带归属者两项（不落盘，故不进 saveAdditional） */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        if (claimantId != null) {
            tag.putUUID(TAG_CLAIMANT, claimantId);
        }
        if (claimantName != null && !claimantName.isEmpty()) {
            tag.putString(TAG_CLAIMANT_NAME, claimantName);
        }
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
    }
}
