package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.miniature.IMiniaturizableTerminal;
import com.example.akaishi.api.storage.IWirelessTerminalHost;
import com.example.akaishi.block.AkaishiEnergyCellBlock;
import com.example.akaishi.block.AkaishiWirelessTerminalBlock;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyCellArrayStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.menu.AkaishiWirelessTerminalMenu;
import com.example.akaishi.miniature.WirelessTerminalMiniatureAdapter;
import com.example.akaishi.miniature.WirelessTerminalMiniatureState;
import com.example.akaishi.util.LongDataSlots;
import com.example.akaishi.wireless.IWirelessTerminal;
import com.example.akaishi.wireless.TerminalSecurity;
import com.example.akaishi.wireless.WirelessFamily;
import com.example.akaishi.wireless.WirelessNetworkManager;
import com.example.akaishi.wireless.WirelessTerminalStructure;
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
 * 无线赤能源终端方块实体：无线终端多方块（5×5×5）主方块与网络能量中枢。
 * <p>
 * 每 tick 委托 {@link WirelessTerminalStructure} 扫描结构（成型才生效），成型后：
 * 1) 以唯一终端 ID 注册网络（registerTerminal + 心跳），成为该网络所有已认证口的能量中枢；
 * 2) 绑定贴身赤能源储存单元：扫描结构外围 {@link #BIND_RANGE} 格内的 {@link AkaishiEnergyCellBlockEntity}，
 *    聚合为本网络储能池（总容量 = 各单元容量之和，动态收集）；
 * 3) 惰性清理网络内失效的输入口/输出口；含区块加载构架时弱加载网络区块。
 * <p>
 * 认证：一律走终端安全表（归属者 + 权限表 + 默认权限条目，{@link TerminalSecurity}）——输入口凭
 * {@code INJECT}、输出口凭 {@code EXTRACT}、远程绑定凭 {@code BUILD}、便携终端凭 {@code CRAFT} 权限接入本终端。
 * 终端 ID 与安全状态经 NBT 持久化；数据槽同步储能/容量（long 拆 4 槽）+ 状态 + 终端 ID 摘要。
 */
public class AkaishiWirelessTerminalBlockEntity extends BlockEntity
        implements ExtendedMenuProvider, IDataCarrier, IWirelessTerminal, IMiniaturizableTerminal,
        IWirelessTerminalHost {

    // 数据槽下标（DATA_*）随 IWirelessTerminalHost 一并下沉：菜单只依赖接口，不依赖本类
    /** 旧授权卡计数（白名单已废弃，槽位与下标保持不变以免索引错位） */
    public static final int DATA_AUTHORIZED = 8;
    /** 区块加载能量税停用标志（已废弃：税已移除，槽位保留以免索引错位） */
    public static final int DATA_TAX_DISABLED = 16;

    /** 绑定储能单元的搜索半径：结构外围 1 格（单元单方块直接贴身布置即可，范围小不误扫无关方块） */
    private static final int BIND_RANGE = 1;
    /** 区块加载刷新间隔（tick）：每 20 tick 按网络变化 diff 刷新弱加载 ticket（免能量税） */
    private static final int CHUNK_REFRESH_INTERVAL = 20;

    /** 结构扫描缓存失效标记 */
    private boolean structureDirty = true;
    private int scanCooldown;
    private int bindCooldown;
    private int purgeCooldown;
    private int chunkLoadCooldown;
    private WirelessTerminalStructure.Result structure;

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
     * 安全状态（归属者 + 权限表 + 默认权限条目）：权威数据。
     * 变化即落盘并把镜像推送到网络注册表，供端口/便携终端与客户端 UI 查询。
     * <p>
     * 回调走方法引用而非 lambda：lambda 里直接写 {@code security} 会构成字段自引用，Java 拒绝编译。
     */
    private final TerminalSecurity security = new TerminalSecurity(this::onSecurityChanged);
    private final AkaishiEnergyCellArrayStorage boundStorage;
    private final SimpleContainerData data = new SimpleContainerData(DATA_SLOTS);
    /** 最近一次聚合储能读数缓存（供便携终端等高频读取，避免每 tick 全量求和） */
    private long lastStored;
    private long lastMax;
    /** 当前弱加载的「维度 + 区块」集合（区块加载构架生效时维护，diff 更新防泄漏） */
    private final Set<DimChunk> loadedChunks = new HashSet<>();
    /** 绑定储能成员缓存：每 20 tick 刷新，避免每次能量操作都全量扫描周围方块 */
    private List<com.example.akaishi.api.energy.IEnergyStorage> cachedMembers = List.of();

    /** 弱加载目标：维度 + 区块 */
    private record DimChunk(ResourceKey<Level> dimension, ChunkPos pos) {
    }

    public AkaishiWirelessTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_WIRELESS_TERMINAL.get(), pos, state);
        this.boundStorage = new AkaishiEnergyCellArrayStorage(AkaishiEnergyType.INSTANCE, () -> cachedMembers);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiWirelessTerminalBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        // 结构扫描缓存：成型期间方块几乎不变，定时兜底重扫（20 tick）
        WirelessTerminalStructure.Result scanned;
        if (structureDirty || --scanCooldown <= 0) {
            scanned = WirelessTerminalStructure.scan(level, worldPosition);
            structureDirty = false;
            scanCooldown = 20;
        } else {
            scanned = structure;
        }
        boolean formed = scanned != null;
        boolean wasFormed = getBlockState().getValue(AkaishiWirelessTerminalBlock.FORMED);
        if (wasFormed != formed) {
            level.setBlock(worldPosition, getBlockState().setValue(AkaishiWirelessTerminalBlock.FORMED, formed), 3);
        }
        if (formed && !wasFormed) {
            // 成型瞬间把权限表镜像推回注册表：重载/换结构后端口与便携终端立即恢复判定
            security.pushTo(terminalId);
        }
        this.structure = scanned;

        if (formed) {
            // 注册网络 + 心跳（成为本网络能量中枢；授权卡集合已在授权操作时同步）
            WirelessNetworkManager.registerTerminal(terminalId, level.dimension(), worldPosition, level.getGameTime());
            // 惰性清理失效口（跨维度，按各口所在维度查询；校验口绑定卡仍被本终端授权）
            if (--purgeCooldown <= 0) {
                purgeCooldown = 20;
                WirelessNetworkManager.purge(terminalId, serverLevel.getServer(),
                        be -> be instanceof AkaishiWirelessInputPortBlockEntity p && p.authenticated(terminalId),
                        be -> be instanceof AkaishiWirelessOutputPortBlockEntity p && p.authenticated(terminalId));
            }
            // 区块加载（免费）：每 20 tick 按终端/口的在线变化 diff 刷新弱加载 ticket，防止 ticket 泄漏；
            // 不再收取能量税，避免税成为持续负载导致满储能时聚变堆仍周期性点火
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
            // 结构失效：解除网络在线 + 释放弱加载区块 + 清空储能成员（口将因找不到授权终端而停止传输）
            WirelessNetworkManager.unregisterTerminal(terminalId);
            releaseChunkLoad();
            cachedMembers = List.of();
        }

        // 同步数据槽到 GUI
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
        data.set(DATA_TAX_DISABLED, 0); // 废弃占位：区块加载税已移除，槽位保留以免后续索引错位
        data.set(DATA_INPUT_LOSS, structure == null ? 0 : structure.inputLossCount);
        data.set(DATA_OUTPUT_LOSS, structure == null ? 0 : structure.outputLossCount);
        // 终端短 ID（UUID 高 32 位）：GUI 8 位 hex 与身份卡/终端显示格式一致；
        // 数据槽每槽仅 16 位有效，拆低/高 2 槽同步，避免高 16 位被截断
        LongDataSlots.writeInt(data, DATA_TERMINAL_ID, DATA_TERMINAL_ID_HIGH,
                (int) (terminalId.getMostSignificantBits() >>> 32));
    }

    // ===== 网络区块弱加载 =====

    /**
     * 计算当前应弱加载的「维度 + 区块」集合：终端所在区块 + 频道全部已认证输入口/输出口所在区块
     * （跨维度亦加载）。默认只锁单区块；内腔含区块加载扩展组件时，终端与每个口的加载范围均扩为
     * 以目标区块为中心的 3×3（使相邻区块的机器在玩家远离时照常运转）。
     * 每 20 tick diff 更新，防 ticket 泄漏。
     */
    private void updateChunkLoad() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Set<DimChunk> want = computeWantChunks();
        for (DimChunk dc : loadedChunks) {
            if (!want.contains(dc)) {
                ServerLevel target = serverLevel.getServer().getLevel(dc.dimension());
                if (target != null) {
                    target.getChunkSource().removeRegionTicket(TicketType.PORTAL, dc.pos, 1, dc.pos.getWorldPosition());
                }
            }
        }
        for (DimChunk dc : want) {
            if (!loadedChunks.contains(dc)) {
                ServerLevel target = serverLevel.getServer().getLevel(dc.dimension());
                if (target != null) {
                    target.getChunkSource().addRegionTicket(TicketType.PORTAL, dc.pos, 1, dc.pos.getWorldPosition());
                }
            }
        }
        loadedChunks.clear();
        loadedChunks.addAll(want);
    }

    /** 计算应弱加载的「维度 + 区块」集合（能量税按此集合大小计费） */
    private Set<DimChunk> computeWantChunks() {
        Set<DimChunk> want = new HashSet<>();
        boolean range = structure != null && structure.chunkRangeCount > 0;
        addChunkArea(want, level.dimension(), new ChunkPos(worldPosition), range); // 终端（含扩展）
        WirelessNetworkManager.TerminalEntry e = WirelessNetworkManager.entryOf(terminalId);
        if (e != null) {
            for (WirelessNetworkManager.PortKey k : e.inputs) {
                addChunkArea(want, k.dimension(), new ChunkPos(BlockPos.of(k.pos())), range);
            }
            for (WirelessNetworkManager.PortKey k : e.outputs) {
                addChunkArea(want, k.dimension(), new ChunkPos(BlockPos.of(k.pos())), range);
            }
        }
        return want;
    }

    /** 将目标区块加入弱加载集合；range=true 时扩为以目标为中心的 3×3 区块 */
    private static void addChunkArea(Set<DimChunk> set, ResourceKey<Level> dimension, ChunkPos center, boolean range) {
        if (!range) {
            set.add(new DimChunk(dimension, center));
            return;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set.add(new DimChunk(dimension, new ChunkPos(center.x + dx, center.z + dz)));
            }
        }
    }

    /** 释放全部弱加载区块（结构失效 / 终端被拆时调用，防 ticket 泄漏） */
    public void releaseChunkLoad() {
        if (level instanceof ServerLevel serverLevel) {
            for (DimChunk dc : loadedChunks) {
                ServerLevel target = serverLevel.getServer().getLevel(dc.dimension());
                if (target != null) {
                    target.getChunkSource().removeRegionTicket(TicketType.PORTAL, dc.pos, 1, dc.pos.getWorldPosition());
                }
            }
        }
        loadedChunks.clear();
    }

    // ===== 绑定储能 =====

    /** 收集绑定储能成员：结构外围 BIND_RANGE 格内的全部赤能源储存单元（动态求值，防悬空引用） */
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
            // 仅对能量单元方块取 BE（避免对无关 EntityBlock 触发创建，也避免扫描范围扩大后误触其他机器）
            if (level.getBlockState(p).getBlock() instanceof AkaishiEnergyCellBlock) {
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof AkaishiEnergyCellBlockEntity && !list.contains(be)) {
                    list.add(be);
                }
            }
        }
        return list;
    }

    /**
     * 收集绑定储能：普通单元取自身存储；3×3×3 串联器的外壳聚合为中心串联器的聚合存储
     * （按中心去重，任一外壳落在绑定范围内即整台接入，支持多方块串联）。
     */
    private List<com.example.akaishi.api.energy.IEnergyStorage> collectSerializers() {
        List<com.example.akaishi.api.energy.IEnergyStorage> storages = new ArrayList<>();
        Set<BlockPos> seenCenters = new HashSet<>();
        for (BlockEntity be : serializerEntities()) {
            if (be instanceof AkaishiEnergyCellBlockEntity cell) {
                AkaishiEnergyCellSerializerBlockEntity center = cell.findSerializerCenter();
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

    // ===== 能量中转（口侧调用） =====

    /** 输入口推送：把能量存入绑定储能，返回实收（含损耗已在口侧扣除） */
    public long receiveWireless(long amount) {
        return boundStorage.addEnergy(amount, false);
    }

    /** 输出口拉取：从绑定储能取出能量（损耗已在口侧扣除） */
    public long extractWireless(long amount) {
        return boundStorage.extractEnergy(amount, false);
    }

    // ===== 安全状态（归属者 + 权限表；权限载体为身份卡，判定规则见 TerminalSecurity） =====

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

    // ===== 访问器 =====

    /** 终端唯一 ID（网络注册表 key） */
    public UUID terminalId() {
        return terminalId;
    }

    /** 所属网络族：赤能源终端固定为 CHISHI（生命能量无线终端走另一族，见生命族实现） */
    @Override
    public WirelessFamily family() {
        return WirelessFamily.CHISHI;
    }

    public boolean isFormed() {
        return structure != null;
    }

    /** 是否已解锁跨维度（内腔含终端跨维组件） */
    public boolean isCrossDim() {
        return structure != null && structure.crossDimCount > 0;
    }

    /** 是否已解锁便携终端「随身供能」（内腔含 ≥1 便捷传输构架） */
    public boolean hasTransmitFrame() {
        return structure != null && structure.transmitFrameCount > 0;
    }

    /** 输入口方向损耗削减比例（0-0.9，内腔输入损耗抑制组件提供） */
    public double inputLossReduction() {
        return structure == null ? 0.0 : Math.min(0.9, structure.inputLossCount * ModConfig.wirelessLossReductionPerModule);
    }

    /** 输出口方向损耗削减比例（0-0.9，内腔输出损耗抑制组件提供） */
    public double outputLossReduction() {
        return structure == null ? 0.0 : Math.min(0.9, structure.outputLossCount * ModConfig.wirelessLossReductionPerModule);
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
        return Component.translatable("block.akaishi.akaishi_wireless_terminal");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiWirelessTerminalMenu(id, inv, this);
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
        return WirelessTerminalMiniatureAdapter.CHISHI_ID;
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
     * 导出微缩数据：安全表 + <b>储能池快照（容量 + 储量）</b> + 跨维/损耗抑制/便捷传输构架读数，
     * 键名与 {@link WirelessTerminalMiniatureState} 的读回口径严格对应（单向漂移就会变成脏数据）。
     * <p>
     * <b>口径（用户拍板，与物品终端统一）</b>：坍缩时连那圈贴身储能单元一起被消耗，容量与储量
     * 真正并入芯片 —— 因此这里写的是<b>池子快照</b>，不是坐标。代价是那些单元必须出现在
     * {@link #extraConsumedBlocks()} 里，否则同一份电会"世界一份 + 芯片一份"。
     * <p>
     * {@code PoolCells} 一并写出但<b>仅作诊断/结构留档</b>，不再驱动任何能量读数。
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
        payload.putBoolean(WirelessTerminalMiniatureState.TAG_TRANSMIT_FRAME, hasTransmitFrame());
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
     * 除 5×5×5 箱体外必须一并消耗的方块：<b>所有把能量算进了池子快照的方块</b>。
     * <p>
     * 硬不变量：能量进了快照 ⇒ 方块必须在这里出现，否则它留在世界上就是第二份电。
     * <ul>
     *   <li>普通储能单元（未接入串联器）：消耗其自身；</li>
     *   <li>3×3×3 串联器：只要任一外壳落进绑定范围，整台的聚合存储就被计入 ——
     *       因此<b>中心 + 全部 26 个外壳</b>都要消耗（只取真实能量单元，不越界误伤无关方块）。</li>
     * </ul>
     */
    @Override
    public List<BlockPos> extraConsumedBlocks() {
        if (level == null) {
            return List.of();
        }
        Set<BlockPos> consumed = new LinkedHashSet<>();
        for (BlockEntity be : serializerEntities()) {
            if (!(be instanceof AkaishiEnergyCellBlockEntity cell)) {
                continue;
            }
            AkaishiEnergyCellSerializerBlockEntity center = cell.findSerializerCenter();
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
                    if (level.getBlockState(p).getBlock() instanceof AkaishiEnergyCellBlock) {
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
            IEnergyStorage storage = provider.getEnergyStorage(AkaishiEnergyType.INSTANCE);
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
            if (b instanceof AkaishiWirelessTerminalBlock) {
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof AkaishiWirelessTerminalBlockEntity t) {
                    t.invalidateStructure();
                }
            }
        }
    }
}
