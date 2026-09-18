package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.miniature.IMiniaturizableTerminal;
import com.example.akaishi.api.storage.IWirelessTerminalHost;
import com.example.akaishi.block.AkaishiLifeEnergyCellBlock;
import com.example.akaishi.block.AkaishiLifeWirelessTerminalBlock;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyCellArrayStorage;
import com.example.akaishi.energy.LifeEnergyType;
import com.example.akaishi.menu.AkaishiLifeWirelessTerminalMenu;
import com.example.akaishi.miniature.WirelessTerminalMiniatureAdapter;
import com.example.akaishi.miniature.WirelessTerminalMiniatureState;
import com.example.akaishi.util.LongDataSlots;
import com.example.akaishi.wireless.IWirelessTerminal;
import com.example.akaishi.wireless.LifeWirelessStructure;
import com.example.akaishi.wireless.TerminalSecurity;
import com.example.akaishi.wireless.WirelessFamily;
import com.example.akaishi.wireless.WirelessNetworkManager;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
import java.util.LinkedHashSet;
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
 * 认证：一律走终端安全表（归属者 + 权限表 + 默认权限条目，{@link TerminalSecurity}）——输入口凭
 * {@code INJECT}、输出口凭 {@code EXTRACT}、远程绑定凭 {@code BUILD}、便携终端凭 {@code CRAFT} 权限接入本终端。
 * 生命无线组件可解锁跨维连接、终端及端口区块加载、3×3 加载范围，并按输入/输出方向独立抑制无线损耗。
 * 终端 ID 与安全状态经 NBT 持久化；数据槽同步储能/容量（long 拆 4 槽）+ 状态 + 终端 ID 摘要
 * （数量与下标和赤版完全一致，菜单读取兼容）。
 */
public class AkaishiLifeWirelessTerminalBlockEntity extends BlockEntity
        implements ExtendedMenuProvider, IDataCarrier, IWirelessTerminal, IMiniaturizableTerminal,
        IWirelessTerminalHost {

    // 数据槽下标（DATA_*）随 IWirelessTerminalHost 一并下沉：菜单只依赖接口，不依赖本类
    /** 旧授权卡计数（白名单已废弃，槽位与下标保持不变以免索引错位） */
    public static final int DATA_AUTHORIZED = 8;
    /** 区块加载能量税停用标志（生命族恒 0，槽位保留以免索引错位） */
    public static final int DATA_TAX_DISABLED = 16;

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
    /** 旧授权卡白名单条数上限（对齐旧版实现，仅用于裁剪旧存档 NBT，防伪造数据撑爆内存） */
    private static final int LEGACY_AUTHORIZED_CARD_CAP = 8;
    /**
     * 旧授权卡白名单：权限判定已由安全表接管，本集合仅在加载旧存档时读入（不再写回、不推注册表），
     * 保留读取以避免老存档报错并让计数槽延续旧值。
     */
    private final Set<UUID> authorizedCards = new HashSet<>();
    /**
     * 安全状态（归属者 + 权限表 + 默认权限条目）：权威数据，变化即落盘并推镜像到网络注册表。
     * 回调走方法引用而非 lambda：lambda 里直接写 {@code security} 会构成字段自引用。
     */
    private final TerminalSecurity security = new TerminalSecurity(this::onSecurityChanged);
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
        if (formed && !wasFormed) {
            // 成型瞬间把权限表镜像推回注册表：重载/换结构后端口与便携终端立即恢复判定
            security.pushTo(terminalId);
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
        data.set(DATA_AUTHORIZED, authorizedCards.size()); // 旧授权卡计数（白名单已废弃，槽位与下标保持不变）
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

    // ===== 访问器 =====

    /** 安全状态（安全页 / GUI / 判定入口） */
    public TerminalSecurity security() {
        return security;
    }

    /** 记录归属者（结构主方块放置时由方块调用）；归属者恒全权限，不占权限表条目 */
    public void setOwner(UUID owner, String name) {
        security.setOwner(owner, name);
    }

    /** 安全状态变化：落盘 + 推镜像（端口/便携终端/客户端 UI 依赖注册表镜像） */
    private void onSecurityChanged() {
        setChanged();
        security.pushTo(terminalId);
    }

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
        security.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("TerminalId")) {
            terminalId = tag.getUUID("TerminalId");
        }
        // 旧存档兼容：旧授权卡白名单仅读入本地（按上限裁剪，防伪造 NBT 撑爆内存），不再推注册表
        authorizedCards.clear();
        ListTag cards = tag.getList("AuthorizedCards", Tag.TAG_COMPOUND);
        for (int i = 0; i < cards.size() && authorizedCards.size() < LEGACY_AUTHORIZED_CARD_CAP; i++) {
            authorizedCards.add(cards.getCompound(i).getUUID("Card"));
        }
        // 安全状态（归属者 + 权限表）读入后立刻推镜像：端口/便携终端/客户端 UI 都依赖它
        security.load(tag);
        security.pushTo(terminalId);
    }

    /** 结构失效标记：结构方块被破坏时由事件驱动重扫 */
    public void invalidateStructure() {
        structureDirty = true;
    }

    // ===== 微缩（坍缩为单方块）：见 IMiniaturizableTerminal 与 MiniatureCollapse =====

    @Override
    public ResourceLocation miniatureTypeId() {
        return WirelessTerminalMiniatureAdapter.LIFE_ID;
    }

    @Override
    public BlockPos structureMin() {
        return structure == null ? null : structure.min;
    }

    @Override
    public BlockPos structureMax() {
        return structure == null ? null : structure.max;
    }

    /**
     * 导出微缩数据（生命族版）：安全表 + <b>储能池快照（容量 + 储量）</b> + 跨维/损耗抑制读数
     * （生命结构不含便捷传输构架，故该位恒 false），键名与 {@link WirelessTerminalMiniatureState} 一致。
     * <p>
     * 池子口径与赤能源族完全一致：坍缩连贴身储能单元一起吃掉，容量/储量并入芯片；代价是
     * 那些单元必须出现在 {@link #extraConsumedBlocks()} 里（见该方法的硬不变量）。
     * {@code PoolCells} 仅作诊断/结构留档。
     */
    @Override
    public CompoundTag captureMiniature() {
        CompoundTag payload = new CompoundTag();
        CompoundTag sec = new CompoundTag();
        security.save(sec);
        payload.put(WirelessTerminalMiniatureState.TAG_SECURITY, sec);
        payload.putBoolean(WirelessTerminalMiniatureState.TAG_CROSS_DIM, isCrossDim());
        payload.putInt(WirelessTerminalMiniatureState.TAG_INPUT_LOSS, structure == null ? 0 : structure.inputLossCount);
        payload.putInt(WirelessTerminalMiniatureState.TAG_OUTPUT_LOSS, structure == null ? 0 : structure.outputLossCount);
        payload.putBoolean(WirelessTerminalMiniatureState.TAG_TRANSMIT_FRAME, false);
        long capacity = 0L;
        long stored = 0L;
        for (IEnergyStorage storage : collectSerializers()) {
            capacity += storage.getMaxEnergy();
            stored += storage.getEnergyStored();
        }
        payload.putLong(WirelessTerminalMiniatureState.TAG_POOL_CAPACITY, capacity);
        payload.putLong(WirelessTerminalMiniatureState.TAG_POOL_STORED, stored);
        payload.put(WirelessTerminalMiniatureState.TAG_POOL_CELLS, capturePoolCells());
        return payload;
    }

    /**
     * 除 5×5×5 箱体外必须一并消耗的方块：<b>所有把能量算进了池子快照的方块</b>
     * （硬不变量：能量进了快照 ⇒ 方块必须在这里出现，否则即第二份电）。
     * <p>
     * 普通单元消耗自身；生命储存串联器只要任一外壳落进绑定范围就整台接入，
     * 故<b>中心 + 全部 26 个外壳</b>都要消耗（只取真实能量单元，不越界误伤无关方块）。
     */
    @Override
    public List<BlockPos> extraConsumedBlocks() {
        if (level == null) {
            return List.of();
        }
        Set<BlockPos> consumed = new LinkedHashSet<>();
        for (BlockEntity be : serializerEntities()) {
            if (!(be instanceof AkaishiLifeEnergyCellBlockEntity cell)) {
                continue;
            }
            AkaishiLifeEnergyCellSerializerBlockEntity center = cell.findSerializerCenter();
            if (center == null) {
                consumed.add(cell.getBlockPos());
            } else {
                collectSerializerBlocks(consumed, center.getBlockPos());
            }
        }
        return List.copyOf(consumed);
    }

    /** 串联器整台的组成方块：中心串联器 + 3×3×3 内的 26 个外壳单元（非单元不取，避免误伤） */
    private void collectSerializerBlocks(Set<BlockPos> out, BlockPos center) {
        out.add(center);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos p = center.offset(dx, dy, dz);
                    if (level.getBlockState(p).getBlock() instanceof AkaishiLifeEnergyCellBlock) {
                        out.add(p);
                    }
                }
            }
        }
    }

    /** 记录绑定储能单元的相对坐标（仅诊断/结构留档，不再驱动能量读数） */
    private ListTag capturePoolCells() {
        ListTag cells = new ListTag();
        List<IEnergyStorage> seen = new ArrayList<>();
        for (BlockEntity be : serializerEntities()) {
            if (!(be instanceof IEnergyProvider provider)) {
                continue;
            }
            IEnergyStorage storage = provider.getEnergyStorage(LifeEnergyType.INSTANCE);
            if (storage == null || seen.contains(storage)) {
                continue;
            }
            seen.add(storage);
            BlockPos offset = be.getBlockPos().subtract(worldPosition);
            cells.add(new IntArrayTag(new int[] {offset.getX(), offset.getY(), offset.getZ()}));
        }
        return cells;
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
