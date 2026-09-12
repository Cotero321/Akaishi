package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.block.AkaishiLifeEnergyCellBlock;
import com.example.akaishi.block.AkaishiLifeWirelessTerminalBlock;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyCellArrayStorage;
import com.example.akaishi.energy.LifeEnergyType;
import com.example.akaishi.menu.AkaishiLifeWirelessTerminalMenu;
import com.example.akaishi.util.LongDataSlots;
import com.example.akaishi.wireless.IWirelessTerminal;
import com.example.akaishi.wireless.LifeWirelessStructure;
import com.example.akaishi.wireless.WirelessFamily;
import com.example.akaishi.wireless.WirelessNetworkManager;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 生命无线终端方块实体：生命无线终端多方块（5×5×5）主方块与网络生命能量中枢
 * （赤能源无线终端 {@link AkaishiWirelessTerminalBlockEntity} 的生命族镜像）。
 * <p>
 * 每 tick 委托 {@link LifeWirelessStructure} 扫描结构（成型才生效），成型后：
 * 1) 以唯一终端 ID 按 {@link WirelessFamily#LIFE} 族注册网络（registerTerminal + 心跳），
 *    成为该族网络所有已认证口的生命能量中枢（族隔离，跨族卡互不可命中）；
 * 2) 绑定贴身生命能量储存单元：扫描结构外围 {@link #BIND_RANGE} 格内的
 *    {@link AkaishiLifeEnergyCellBlockEntity}（含生命储存串联器外壳，按中心去重聚合），
 *    聚合为本网络储能池（总容量 = 各单元容量之和，动态收集）；
 * 3) 惰性清理网络内失效的输入口/输出口。
 * <p>
 * 认证：身份卡 UUID 集合经「安全卡认证」页维护（最多 {@link WirelessNetworkManager#MAX_AUTHORIZED_CARDS} 张），
 * 与注册表同步持久化；输入口/输出口凭同 UUID 卡片接入本终端。
 * 生命无线组件可解锁跨维连接、终端及端口区块加载、3×3 加载范围，并按输入/输出方向独立抑制无线损耗。
 * 终端 ID 与授权卡集合经 NBT 持久化；数据槽同步储能/容量（long 拆 4 槽）+ 状态 + 终端 ID 摘要
 * （数量与下标和赤版完全一致，菜单读取兼容）。
 */
public class AkaishiLifeWirelessTerminalBlockEntity extends BlockEntity
        implements ExtendedMenuProvider, IDataCarrier, IWirelessTerminal {

    // ===== 数据槽（数量与下标与赤版 AkaishiWirelessTerminalBlockEntity 完全一致） =====
    public static final int DATA_FORMED = 0;
    public static final int DATA_STORED_LOW = 1;
    public static final int DATA_STORED_HIGH = 2;
    public static final int DATA_CAPACITY_LOW = 3;
    public static final int DATA_CAPACITY_HIGH = 4;
    public static final int DATA_INPUT_COUNT = 5;
    public static final int DATA_OUTPUT_COUNT = 6;
    public static final int DATA_BOUND_SERIALIZERS = 7;
    public static final int DATA_AUTHORIZED = 8;
    public static final int DATA_CROSS_DIM = 9;
    public static final int DATA_CHUNK_LOAD = 10;
    public static final int DATA_CHUNK_RANGE = 11;
    public static final int DATA_INPUT_LOSS = 12;
    public static final int DATA_OUTPUT_LOSS = 13;
    /** 终端 ID 摘要（UUID.hashCode，GUI 显示短 ID） */
    public static final int DATA_TERMINAL_ID = 14;
    /** 当前弱加载区块数 */
    public static final int DATA_CHUNK_LOADED = 15;
    /** 区块加载能量税停用标志（生命族恒 0，槽位保留以免索引错位） */
    public static final int DATA_TAX_DISABLED = 16;
    /** 储能 64 位高段：储能/容量超过 2^31（生命串联器聚合 10.42 亿/台）时，低 32 位槽 1..4 无法承载，追加高 32 位槽 */
    public static final int DATA_STORED_HIGH2 = 17;
    public static final int DATA_STORED_HIGH3 = 18;
    public static final int DATA_CAPACITY_HIGH2 = 19;
    public static final int DATA_CAPACITY_HIGH3 = 20;
    /** 终端 ID 高 16 位（低 16 位见 {@link #DATA_TERMINAL_ID}）：数据槽仅 16 位有效，8 位 hex 短 ID 需拆 2 槽 */
    public static final int DATA_TERMINAL_ID_HIGH = 21;
    public static final int DATA_SLOTS = 22;

    /** 绑定储能单元的搜索半径：结构外围 1 格（单元单方块直接贴身布置即可，范围小不误扫无关方块） */
    private static final int BIND_RANGE = 1;
    private static final int CHUNK_REFRESH_INTERVAL = 20;

    /** 结构扫描缓存失效标记 */
    private boolean structureDirty = true;
    private int scanCooldown;
    private int bindCooldown;
    private int purgeCooldown;
    private int chunkLoadCooldown;
    private LifeWirelessStructure.Result structure;

    /** 终端唯一 ID（首次放置生成，NBT 持久化；网络注册表 key） */
    private UUID terminalId = UUID.randomUUID();
    /** 授权身份卡集合（安全卡认证页管理，NBT 持久化） */
    private final Set<UUID> authorizedCards = new HashSet<>();
    private final AkaishiEnergyCellArrayStorage boundStorage;
    private final SimpleContainerData data = new SimpleContainerData(DATA_SLOTS);
    /** 最近一次聚合储能读数缓存（供便携终端等高频读取，避免每 tick 全量求和） */
    private long lastStored;
    private long lastMax;
    private final Set<DimChunk> loadedChunks = new HashSet<>();
    /** 绑定储能成员缓存：每 20 tick 刷新，避免每次能量操作都全量扫描周围方块 */
    private List<com.example.akaishi.api.energy.IEnergyStorage> cachedMembers = List.of();

    private record DimChunk(ResourceKey<Level> dimension, ChunkPos pos) {
    }

    public AkaishiLifeWirelessTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_WIRELESS_TERMINAL.get(), pos, state);
        this.boundStorage = new AkaishiEnergyCellArrayStorage(LifeEnergyType.INSTANCE, () -> cachedMembers);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiLifeWirelessTerminalBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        // 结构扫描缓存：成型期间方块几乎不变，定时兜底重扫（20 tick）
        LifeWirelessStructure.Result scanned;
        if (structureDirty || --scanCooldown <= 0) {
            scanned = LifeWirelessStructure.scan(level, worldPosition);
            structureDirty = false;
            scanCooldown = 20;
        } else {
            scanned = structure;
        }
        boolean formed = scanned != null;
        boolean wasFormed = getBlockState().getValue(AkaishiLifeWirelessTerminalBlock.FORMED);
        if (wasFormed != formed) {
            level.setBlock(worldPosition, getBlockState().setValue(AkaishiLifeWirelessTerminalBlock.FORMED, formed), 3);
        }
        this.structure = scanned;

        if (formed) {
            // 注册网络 + 心跳（带族声明 LIFE；授权卡集合已在授权操作时同步）
            WirelessNetworkManager.registerTerminal(terminalId, level.dimension(), worldPosition,
                    level.getGameTime(), WirelessFamily.LIFE);
            // 惰性清理失效口（跨维度，按各口所在维度查询；校验口绑定卡仍被本终端授权）
            if (--purgeCooldown <= 0) {
                purgeCooldown = 20;
                WirelessNetworkManager.purge(terminalId, ((ServerLevel) level).getServer(),
                        be -> be instanceof AkaishiLifeWirelessInputPortBlockEntity p && p.authenticated(terminalId),
                        be -> be instanceof AkaishiLifeWirelessOutputPortBlockEntity p && p.authenticated(terminalId));
            }
            if (--chunkLoadCooldown <= 0) {
                chunkLoadCooldown = CHUNK_REFRESH_INTERVAL;
                if (structure.chunkLoaderCount > 0) {
                    updateChunkLoad();
                } else {
                    releaseChunkLoad();
                }
            }
            // 绑定储能成员缓存：每 20 tick 重扫贴身储能单元（结构变化不频繁，避免每 tick 全量扫描）
            if (--bindCooldown <= 0) {
                bindCooldown = 20;
                cachedMembers = collectSerializers();
            }
        } else if (wasFormed) {
            // 结构失效：解除网络在线 + 清空储能成员（口将因找不到授权终端而停止传输）
            WirelessNetworkManager.unregisterTerminal(terminalId);
            releaseChunkLoad();
            cachedMembers = List.of();
        }

        // 同步数据槽到 GUI（下标与赤版一致）
        long stored = boundStorage.getEnergyStored();
        long max = boundStorage.getMaxEnergy();
        lastStored = stored;
        lastMax = max;
        data.set(DATA_FORMED, formed ? 1 : 0);
        LongDataSlots.write(data, DATA_STORED_LOW, DATA_STORED_HIGH, DATA_STORED_HIGH2, DATA_STORED_HIGH3, stored);
        LongDataSlots.write(data, DATA_CAPACITY_LOW, DATA_CAPACITY_HIGH, DATA_CAPACITY_HIGH2, DATA_CAPACITY_HIGH3, max);
        data.set(DATA_INPUT_COUNT, WirelessNetworkManager.inputCount(terminalId));
        data.set(DATA_OUTPUT_COUNT, WirelessNetworkManager.outputCount(terminalId));
        data.set(DATA_BOUND_SERIALIZERS, cachedMembers.size());
        data.set(DATA_AUTHORIZED, WirelessNetworkManager.authorizedCount(terminalId));
        data.set(DATA_CROSS_DIM, structure != null && structure.crossDimCount > 0 ? 1 : 0);
        data.set(DATA_CHUNK_LOAD, structure != null && structure.chunkLoaderCount > 0 ? 1 : 0);
        data.set(DATA_CHUNK_RANGE, structure != null && structure.chunkRangeCount > 0 ? 1 : 0);
        data.set(DATA_CHUNK_LOADED, loadedChunks.size());
        data.set(DATA_TAX_DISABLED, 0);
        data.set(DATA_INPUT_LOSS, structure == null ? 0 : structure.inputLossCount);
        data.set(DATA_OUTPUT_LOSS, structure == null ? 0 : structure.outputLossCount);
        // 终端短 ID（UUID 前 4 字节）：GUI 8 位 hex 与身份卡/终端显示格式一致；
        // 数据槽每槽仅 16 位有效，拆低/高 2 槽同步，避免高 16 位被截断
        LongDataSlots.writeInt(data, DATA_TERMINAL_ID, DATA_TERMINAL_ID_HIGH,
                (int) (terminalId.getMostSignificantBits() >>> 32));
    }

    private void updateChunkLoad() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Set<DimChunk> wantedChunks = computeWantChunks();
        for (DimChunk chunk : loadedChunks) {
            if (!wantedChunks.contains(chunk)) {
                ServerLevel target = serverLevel.getServer().getLevel(chunk.dimension());
                if (target != null) {
                    target.getChunkSource().removeRegionTicket(TicketType.PORTAL, chunk.pos(), 1,
                            chunk.pos().getWorldPosition());
                }
            }
        }
        for (DimChunk chunk : wantedChunks) {
            if (!loadedChunks.contains(chunk)) {
                ServerLevel target = serverLevel.getServer().getLevel(chunk.dimension());
                if (target != null) {
                    target.getChunkSource().addRegionTicket(TicketType.PORTAL, chunk.pos(), 1,
                            chunk.pos().getWorldPosition());
                }
            }
        }
        loadedChunks.clear();
        loadedChunks.addAll(wantedChunks);
    }

    private Set<DimChunk> computeWantChunks() {
        Set<DimChunk> wantedChunks = new HashSet<>();
        boolean range = structure != null && structure.chunkRangeCount > 0;
        addChunkArea(wantedChunks, level.dimension(), new ChunkPos(worldPosition), range);
        WirelessNetworkManager.TerminalEntry entry = WirelessNetworkManager.entryOf(terminalId);
        if (entry != null) {
            for (WirelessNetworkManager.PortKey port : entry.inputs) {
                addChunkArea(wantedChunks, port.dimension(), new ChunkPos(BlockPos.of(port.pos())), range);
            }
            for (WirelessNetworkManager.PortKey port : entry.outputs) {
                addChunkArea(wantedChunks, port.dimension(), new ChunkPos(BlockPos.of(port.pos())), range);
            }
        }
        return wantedChunks;
    }

    private static void addChunkArea(Set<DimChunk> chunks, ResourceKey<Level> dimension,
                                     ChunkPos center, boolean range) {
        if (!range) {
            chunks.add(new DimChunk(dimension, center));
            return;
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                chunks.add(new DimChunk(dimension, new ChunkPos(center.x + x, center.z + z)));
            }
        }
    }

    public void releaseChunkLoad() {
        if (level instanceof ServerLevel serverLevel) {
            for (DimChunk chunk : loadedChunks) {
                ServerLevel target = serverLevel.getServer().getLevel(chunk.dimension());
                if (target != null) {
                    target.getChunkSource().removeRegionTicket(TicketType.PORTAL, chunk.pos(), 1,
                            chunk.pos().getWorldPosition());
                }
            }
        }
        loadedChunks.clear();
    }

    // ===== 绑定储能 =====

    /** 收集绑定储能成员：结构外围 BIND_RANGE 格内的全部生命能量储存单元（动态求值，防悬空引用） */
    private List<BlockEntity> serializerEntities() {
        List<BlockEntity> list = new ArrayList<>();
        if (structure == null) {
            return list;
        }
        BlockPos min = structure.min.offset(-BIND_RANGE, -BIND_RANGE, -BIND_RANGE);
        BlockPos max = structure.max.offset(BIND_RANGE, BIND_RANGE, BIND_RANGE);
        BlockPos innerMin = structure.min;
        BlockPos innerMax = structure.max;
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            if (p.getX() >= innerMin.getX() && p.getX() <= innerMax.getX()
                    && p.getY() >= innerMin.getY() && p.getY() <= innerMax.getY()
                    && p.getZ() >= innerMin.getZ() && p.getZ() <= innerMax.getZ()) {
                continue; // 结构内部方块不算绑定
            }
            // 仅对生命能量单元方块取 BE（避免对无关 EntityBlock 触发创建，也避免扫描范围扩大后误触其他机器）
            if (level.getBlockState(p).getBlock() instanceof AkaishiLifeEnergyCellBlock) {
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof AkaishiLifeEnergyCellBlockEntity && !list.contains(be)) {
                    list.add(be);
                }
            }
        }
        return list;
    }

    /**
     * 收集绑定储能：普通单元取自身存储；3×3×3 生命储存串联器的外壳聚合为中心串联器的聚合存储
     * （按中心去重，任一外壳落在绑定范围内即整台接入，支持多方块串联）。
     */
    private List<com.example.akaishi.api.energy.IEnergyStorage> collectSerializers() {
        List<com.example.akaishi.api.energy.IEnergyStorage> storages = new ArrayList<>();
        Set<BlockPos> seenCenters = new HashSet<>();
        for (BlockEntity be : serializerEntities()) {
            if (be instanceof AkaishiLifeEnergyCellBlockEntity cell) {
                AkaishiLifeEnergyCellSerializerBlockEntity center = cell.findSerializerCenter();
                if (center != null) {
                    if (seenCenters.add(center.getBlockPos())) {
                        storages.add(center.getEnergyStorage());
                    }
                } else {
                    storages.add(cell.energy());
                }
            }
        }
        return storages;
    }

    // ===== 能量中转（口侧调用，经 IWirelessTerminal） =====

    /** 输入口推送：把能量存入绑定储能，返回实收（含损耗已在口侧扣除） */
    @Override
    public long receiveWireless(long amount) {
        return boundStorage.addEnergy(amount, false);
    }

    /** 输出口拉取：从绑定储能取出能量（损耗已在口侧扣除） */
    @Override
    public long extractWireless(long amount) {
        return boundStorage.extractEnergy(amount, false);
    }

    // ===== 安全卡认证 =====

    /** 授权一张身份卡（上限 {@link WirelessNetworkManager#MAX_AUTHORIZED_CARDS}，重复返回 false） */
    public boolean authorizeCard(UUID card) {
        if (authorizedCards.size() >= WirelessNetworkManager.MAX_AUTHORIZED_CARDS) {
            return false;
        }
        if (authorizedCards.add(card)) {
            WirelessNetworkManager.addAuthorizedCard(terminalId, card);
            setChanged();
            return true;
        }
        return false;
    }

    /** 撤销一张身份卡的授权 */
    public void revokeCard(UUID card) {
        if (authorizedCards.remove(card)) {
            WirelessNetworkManager.removeAuthorizedCard(terminalId, card);
            setChanged();
        }
    }

    /** 该卡是否已被本终端授权 */
    public boolean isCardAuthorized(UUID card) {
        return card != null && authorizedCards.contains(card);
    }

    public int authorizedCount() {
        return authorizedCards.size();
    }

    // ===== 访问器 =====

    /** 终端唯一 ID（网络注册表 key） */
    @Override
    public UUID terminalId() {
        return terminalId;
    }

    /** 所属网络族：生命能量终端固定为 LIFE（与赤能源族隔离） */
    @Override
    public WirelessFamily family() {
        return WirelessFamily.LIFE;
    }

    @Override
    public boolean isFormed() {
        return structure != null;
    }

    /** 是否已解锁跨维度（内腔含生命无线跨维组件） */
    @Override
    public boolean isCrossDim() {
        return structure != null && structure.crossDimCount > 0;
    }

    /** 输入口方向损耗削减比例（最多 90%） */
    @Override
    public double inputLossReduction() {
        return structure == null ? 0.0
                : Math.min(0.9, structure.inputLossCount * ModConfig.wirelessLossReductionPerModule);
    }

    /** 输出口方向损耗削减比例（最多 90%） */
    @Override
    public double outputLossReduction() {
        return structure == null ? 0.0
                : Math.min(0.9, structure.outputLossCount * ModConfig.wirelessLossReductionPerModule);
    }

    /** 缓存储能（tickServer 更新，供便携终端高频读取） */
    public long cachedStored() {
        return lastStored;
    }

    /** 缓存容量（tickServer 更新） */
    public long cachedMax() {
        return lastMax;
    }

    public ContainerData data() {
        return data;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_life_wireless_terminal");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiLifeWirelessTerminalMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
        buf.writeInt(0); // 初始页：终端方块默认运行情况页
    }

    // ===== NBT 持久化 =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putUUID("TerminalId", terminalId);
        ListTag cards = new ListTag();
        for (UUID c : authorizedCards) {
            CompoundTag ct = new CompoundTag();
            ct.putUUID("Card", c);
            cards.add(ct);
        }
        tag.put("AuthorizedCards", cards);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("TerminalId")) {
            terminalId = tag.getUUID("TerminalId");
        }
        authorizedCards.clear();
        ListTag cards = tag.getList("AuthorizedCards", Tag.TAG_COMPOUND);
        // 按授权上限裁剪，防伪造 NBT 导致本地集合与网络注册表（同样受限）不一致
        for (int i = 0; i < cards.size() && authorizedCards.size() < WirelessNetworkManager.MAX_AUTHORIZED_CARDS; i++) {
            authorizedCards.add(cards.getCompound(i).getUUID("Card"));
        }
        // 加载后同步授权卡到网络注册表（重启恢复在线认证）
        for (UUID c : authorizedCards) {
            WirelessNetworkManager.addAuthorizedCard(terminalId, c);
        }
    }

    /** 结构失效标记：结构方块被破坏时由事件驱动重扫 */
    public void invalidateStructure() {
        structureDirty = true;
    }

    /** 供结构内方块被破坏/放置事件调用（反应堆同款兜底，此处直接标记重扫） */
    public static void invalidateNearby(Level level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-5, -5, -5), pos.offset(5, 5, 5))) {
            Block b = level.getBlockState(p).getBlock();
            if (b instanceof AkaishiLifeWirelessTerminalBlock) {
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof AkaishiLifeWirelessTerminalBlockEntity t) {
                    t.invalidateStructure();
                }
            }
        }
    }
}
