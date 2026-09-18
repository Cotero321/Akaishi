package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.miniature.IMiniaturizableTerminal;
import com.example.akaishi.api.storage.IItemStorageUnit;
import com.example.akaishi.api.storage.IItemTerminalEnergyPort;
import com.example.akaishi.api.storage.IItemTerminalHost;
import com.example.akaishi.block.AkaishiItemStorageUnitBlock;
import com.example.akaishi.block.AkaishiItemTerminalBlock;
import com.example.akaishi.block.AkaishiItemTerminalBufferModuleBlock;
import com.example.akaishi.block.ItemStorageUnitTier;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.menu.AkaishiItemTerminalMenu;
import com.example.akaishi.miniature.ItemTerminalMiniatureAdapter;
import com.example.akaishi.miniature.ItemTerminalMiniatureState;
import com.example.akaishi.multiblock.ItemTerminalStructure;
import com.example.akaishi.storage.ItemStorageUnitData;
import com.example.akaishi.util.LongDataSlots;
import com.example.akaishi.value.ItemTerminalFee;
import com.example.akaishi.wireless.ItemTerminalRegistry;
import com.example.akaishi.wireless.TerminalSecurity;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 物品终端方块实体：5×5×5 同族壳体多方块的 IP 物品库中枢。
 * <p>
 * 结构判定走 {@link ItemTerminalStructure}（物品终端自有结构，与无线赤能源终端完全隔离），
 * 成型后每 {@link #RESCAN_INTERVAL} tick 重扫
 * 结构外围 {@link #BIND_RANGE} 格内贴装的各阶物品储存单元，聚合为「总容量 / 已占用 IP」。
 * <p>
 * 本体不参与无线能量网络：自持一份赤能源缓冲（{@link #energy}），存入/取出时由能源接入口填补
 * 并一次性扣费（D7 无维持费 ⇒ 无每 tick 抽能）。
 */
public class AkaishiItemTerminalBlockEntity extends BlockEntity
        implements IDataCarrier, IEnergyProvider, ExtendedMenuProvider, IItemTerminalHost, IMiniaturizableTerminal {

    // 数据槽下标（DATA_*）随 IItemTerminalHost 一并下沉：菜单只依赖接口，不依赖本类

    /** 储存单元搜索范围：墙面 + 结构外围 1 格（单元可贴外侧 1 格，也可镶嵌进墙面） */
    private static final int BIND_RANGE = 1;
    /** 结构 / 单元重扫间隔（tick）：结构方块几乎不变，定时兜底即可，避免每 tick 全量扫描 */
    private static final int RESCAN_INTERVAL = 20;
    /** 弱加载票据刷新间隔（tick）：diff 更新，防票据泄漏 */
    private static final int CHUNK_REFRESH_INTERVAL = 40;
    /** 票据半径：与无线终端族同口径（PORTAL + 半径 1 ⇒ 区块保持加载且方块实体 tick） */
    private static final int CHUNK_TICKET_RADIUS = 1;
    /** 区块加载扩展组件带来的外扩（格）：16 格 ⇒ 目标区间两侧各多覆盖 1 个区块（3×3 效果） */
    private static final int CHUNK_RANGE_PAD = 16;

    /** 结构扫描缓存失效标记 */
    private boolean structureDirty = true;
    private int scanCooldown;
    /**
     * 安全状态（归属者 + 权限表 + 默认权限条目）：权威数据。
     * 物品终端不参与无线网络注册表（没有端口/便携终端来查它），判定走本地
     * （{@code SecurityPage#checkLocal}），因此变化回调只需落盘。
     */
    private final TerminalSecurity security = new TerminalSecurity(this::setChanged);
    /**
     * 终端唯一 ID（首次生成随机，NBT 持久化）。
     * <p>
     * 与无线族同口径：对外寻址一律用本 ID，<b>不用坐标</b> —— 坐标会随方块搬动 / 终端微缩而变，
     * ID 不会。储存无线输入/输出口即按本 ID 绑定（见 {@link com.example.akaishi.wireless.ItemTerminalRegistry}）。
     */
    private UUID terminalId = UUID.randomUUID();
    private int unitCooldown;
    private ItemTerminalStructure.Result structure;

    /** 本体赤能源缓冲：一次性费用的唯一扣费来源（容量由配置决定） */
    private final AkaishiEnergyStorage energy;
    /** 已贴装储存单元（动态求值，防悬空引用；每 20 tick 重扫） */
    private List<BlockEntity> unitEntities = List.of();
    /** 库内容指纹（每 tick 重算，成本与既有的总容量/总占用聚合同级） */
    private long contentHash;
    /** 库内容版本号：指纹变化即自增，客户端据此判断条目快照是否过期 */
    private int contentRevision;
    private int chunkLoadCooldown;
    /** 已施加弱加载票据的区块（diff 用：目标集合之外的一律释放，防票据泄漏） */
    private final Set<ChunkPos> loadedChunks = new LinkedHashSet<>();
    private final SimpleContainerData data = new SimpleContainerData(DATA_SLOTS);

    public AkaishiItemTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_ITEM_TERMINAL.get(), pos, state);
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, ModConfig.itemTerminalEnergyBuffer);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiItemTerminalBlockEntity be) {
        // 心跳：储存无线输入/输出口靠它按终端 ID 定位本终端（超时自动摘除）
        ItemTerminalRegistry.heartbeat(level, be.terminalId, pos, be.security.ownerName());
        be.tickServer();
    }

    /** 终端唯一 ID（对外寻址 key；NBT 持久化，微缩时随数据搬迁） */
    public UUID terminalId() {
        return terminalId;
    }

    private void tickServer() {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        // 结构扫描缓存：成型期间方块几乎不变，定时兜底重扫
        ItemTerminalStructure.Result scanned;
        if (structureDirty || --scanCooldown <= 0) {
            scanned = ItemTerminalStructure.scan(level, worldPosition);
            structureDirty = false;
            scanCooldown = RESCAN_INTERVAL;
        } else {
            scanned = structure;
        }
        boolean formed = scanned != null;
        boolean wasFormed = getBlockState().getValue(AkaishiItemTerminalBlock.FORMED);
        if (wasFormed != formed) {
            level.setBlock(worldPosition, getBlockState().setValue(AkaishiItemTerminalBlock.FORMED, formed), 3);
        }
        this.structure = scanned;

        if (!formed) {
            // 结构失效：清空单元缓存（未成型的终端不接受任何存取）
            unitEntities = List.of();
            contentHash = 0L;
        } else if (--unitCooldown <= 0) {
            unitCooldown = RESCAN_INTERVAL;
            unitEntities = scanUnits();
        }
        if (formed) {
            // 库内容指纹：变化才推进版本号，客户端条目快照据此按需重推（免每 tick 网络包）
            long hash = computeContentHash();
            if (hash != contentHash) {
                contentHash = hash;
                contentRevision++;
            }
        }

        // 缓冲容量 = 配置基准（热重载即时生效）+ 内腔缓冲扩展组件加成（组件被拆自动回落）
        long bufferMax = effectiveBufferCapacity();
        energy.setMaxEnergy(bufferMax);
        if (energy.getEnergyStored() > bufferMax) {
            energy.setEnergy(bufferMax); // 降配后夹回，避免超限余量被算进单笔上限
        }
        // 弱加载票据：按 diff 刷新（无构架或结构失效时目标为空集 ⇒ 自动释放，不泄漏）
        if (--chunkLoadCooldown <= 0) {
            chunkLoadCooldown = CHUNK_REFRESH_INTERVAL;
            updateChunkLoad();
        }

        // 同步数据槽到 GUI
        long used = totalStoredIp();
        long capacity = totalIpCapacity();
        data.set(DATA_FORMED, formed ? 1 : 0);
        LongDataSlots.write(data, DATA_USED_LOW, DATA_USED_HIGH, DATA_USED_HIGH2, DATA_USED_HIGH3, used);
        LongDataSlots.write(data, DATA_CAPACITY_LOW, DATA_CAPACITY_HIGH, DATA_CAPACITY_HIGH2, DATA_CAPACITY_HIGH3, capacity);
        LongDataSlots.write(data, DATA_BUFFER_LOW, DATA_BUFFER_HIGH, DATA_BUFFER_HIGH2, DATA_BUFFER_HIGH3, energy.getEnergyStored());
        data.set(DATA_UNIT_COUNT, liveUnitCount());
        LongDataSlots.write(data, DATA_EFFECTIVE_BUFFER_LOW, DATA_EFFECTIVE_BUFFER_HIGH,
                DATA_EFFECTIVE_BUFFER_HIGH2, DATA_EFFECTIVE_BUFFER_HIGH3, bufferMax);
        data.set(DATA_FEE_MODULES, effectiveFeeModules());
    }

    // ===== 储存单元聚合（D5：外侧 1 格贴装） =====

    /**
     * 立即重扫结构与贴装件（不缓存）。
     * <p>
     * <b>坍缩取数前必须调用</b>：结构与单元平时是每 {@link #RESCAN_INTERVAL} tick 重扫一次的缓存，
     * 若拿最多 20 tick 前的旧列表去销毁世界，刚放下的储存单元既不会被写进 payload、又会被连方块一起清掉
     * —— 那就是真的丢东西。
     */
    public void refreshUnitsNow() {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ItemTerminalStructure.Result scanned = ItemTerminalStructure.scan(level, worldPosition);
        structureDirty = false;
        scanCooldown = RESCAN_INTERVAL;
        unitCooldown = RESCAN_INTERVAL;
        this.structure = scanned;
        this.unitEntities = scanned == null ? List.of() : scanUnits();
    }

    /**
     * 扫描墙面 + 结构外围 {@link #BIND_RANGE} 格内的物品储存单元（内腔除外，那是功能件的地盘）。
     * <p>
     * 仅对 EntityBlock 取方块实体，避免对无关方块触发方块实体创建（同无线终端绑定储能的取舍）。
     */
    private List<BlockEntity> scanUnits() {
        if (structure == null) {
            return List.of();
        }
        List<BlockEntity> found = new ArrayList<>();
        BlockPos min = structure.min.offset(-BIND_RANGE, -BIND_RANGE, -BIND_RANGE);
        BlockPos max = structure.max.offset(BIND_RANGE, BIND_RANGE, BIND_RANGE);
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            if (insideCavity(p)) {
                continue; // 内腔是功能件的地盘，不算贴装位
            }
            if (!(level.getBlockState(p).getBlock() instanceof net.minecraft.world.level.block.EntityBlock)) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof IItemStorageUnit unit && !found.contains(be)) {
                found.add(be);
            }
        }
        return found;
    }

    /**
     * 坐标是否落在结构<b>内腔</b>（不含墙面）。
     * <p>
     * 贴装件（储存单元 / 赤能源接入口）两种装法都算数：贴在外侧 {@link #BIND_RANGE} 格，或直接镶嵌在墙面上。
     * 因此扫描时只排除内腔，不排除墙面。
     */
    private boolean insideCavity(BlockPos p) {
        return p.getX() > structure.min.getX() && p.getX() < structure.max.getX()
                && p.getY() > structure.min.getY() && p.getY() < structure.max.getY()
                && p.getZ() > structure.min.getZ() && p.getZ() < structure.max.getZ();
    }

    /** 存活单元数量（剔除已拆除的方块实体，防残留引用虚报） */
    private int liveUnitCount() {
        int n = 0;
        for (BlockEntity be : unitEntities) {
            if (!be.isRemoved() && be instanceof IItemStorageUnit) {
                n++;
            }
        }
        return n;
    }

    /** 聚合总容量 IP（各阶单元容量之和） */
    public long totalIpCapacity() {
        long total = 0;
        for (BlockEntity be : unitEntities) {
            if (!be.isRemoved() && be instanceof IItemStorageUnit unit) {
                total += unit.getIpCapacity();
            }
        }
        return total;
    }

    /** 聚合已占用 IP（各单元账本求和） */
    public long totalStoredIp() {
        long total = 0;
        for (BlockEntity be : unitEntities) {
            if (!be.isRemoved() && be instanceof IItemStorageUnit unit) {
                total += unit.getStoredIp();
            }
        }
        return total;
    }

    /** 存活储存单元（只读契约视图）：库页聚合与落账的唯一入口 */
    public List<IItemStorageUnit> storageUnits() {
        List<IItemStorageUnit> units = new ArrayList<>(unitEntities.size());
        for (BlockEntity be : unitEntities) {
            if (!be.isRemoved() && be instanceof IItemStorageUnit unit) {
                units.add(unit);
            }
        }
        return units;
    }

    /** 库内容版本号（指纹变化即自增）：客户端仅需比较该值即可判断快照是否过期 */
    public int contentRevision() {
        return contentRevision;
    }

    /**
     * 库内容指纹：按「单元序 → 槽序」混合（物品 + 堆量 + NBT）。
     * <p>
     * 显式混入堆量而非直接用 {@code ItemStack.hashCode()}：后者在部分实现下不含数量，
     * 会让「并入已有堆」这种只增数量的改动被判为无变化而漏推快照。
     */
    private long computeContentHash() {
        long hash = 1L;
        for (BlockEntity be : unitEntities) {
            if (be.isRemoved() || !(be instanceof IItemStorageUnit unit)) {
                continue;
            }
            int slots = unit.slots();
            for (int slot = 0; slot < slots; slot++) {
                ItemStack stack = unit.getItem(slot);
                hash = 31L * hash + (stack.isEmpty() ? 0L
                        : 31L * (31L * stack.getItem().hashCode() + stack.getCount())
                                + Objects.hashCode(stack.getTag()));
            }
        }
        return hash;
    }

    // ===== 能量（自研接口，不走 Forge 能量能力） =====

    @Override
    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    /** 费用只出不进：缓冲由能源接入口在存取时汇聚填入（D13 单一能量入口路径） */
    @Override
    public boolean canOutputEnergy() {
        return false;
    }

    @Override
    public boolean canInputEnergy() {
        return true;
    }

    public ContainerData data() {
        return data;
    }

    /** 结构是否已成型（未成型不接受存取） */
    public boolean isFormed() {
        return structure != null;
    }

    /** 本体赤能源缓冲当前余量 */
    public long bufferedEnergy() {
        return energy.getEnergyStored();
    }

    /** 本体赤能源缓冲容量（客户端据此推导单笔上限，不必占数据槽） */
    public long bufferCapacity() {
        return energy.getMaxEnergy();
    }

    /** 单笔可处理的最大 IP（能量侧约束 = 缓冲容量能承担的最大费用对应的 IP，已并入费率减免） */
    public long maxBatchIp(boolean deposit) {
        return ItemTerminalFee.maxIp(energy.getMaxEnergy(), deposit, effectiveFeeModules());
    }

    /** 有效缓冲容量 = 配置基准 + 内腔缓冲扩展组件加成（超出生效上限的组件忽略） */
    private long effectiveBufferCapacity() {
        int modules = structure == null ? 0
                : Math.min(structure.bufferModuleCount, AkaishiItemTerminalBufferModuleBlock.MAX_EFFECTIVE);
        return ModConfig.itemTerminalEnergyBuffer
                + (long) modules * AkaishiItemTerminalBufferModuleBlock.BONUS_ENERGY;
    }

    /** 生效的费率减免份数（0 ~ FEE_MODULE_MAX，超出部分忽略） */
    private int effectiveFeeModules() {
        return structure == null ? 0 : ItemTerminalFee.effectiveModules(structure.feeModuleCount);
    }

    // ===== 区块弱加载（内腔：区块加载构架 + 区块加载扩展组件） =====

    /**
     * 按 diff 刷新弱加载票据：
     * 目标集合 = 覆盖「结构 + 外侧 {@link #BIND_RANGE} 格贴装区」的全部区块
     * （终端自身与全部储存单元 / 赤能源接入口一网打尽），
     * 内腔含区块加载扩展组件时向四周各外扩 1 区块（3×3 效果）。
     * <p>
     * 无构架或结构失效 ⇒ 目标为空集 ⇒ 自动释放全部票据，不会泄漏。
     */
    private void updateChunkLoad() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Set<ChunkPos> want = computeWantedChunks();
        for (ChunkPos cp : new ArrayList<>(loadedChunks)) {
            if (!want.contains(cp)) {
                serverLevel.getChunkSource().removeRegionTicket(TicketType.PORTAL, cp, CHUNK_TICKET_RADIUS,
                        cp.getWorldPosition());
            }
        }
        for (ChunkPos cp : want) {
            // 幂等刷新：重复施加同一 owner 的票据不会叠加
            serverLevel.getChunkSource().addRegionTicket(TicketType.PORTAL, cp, CHUNK_TICKET_RADIUS,
                    cp.getWorldPosition());
        }
        loadedChunks.clear();
        loadedChunks.addAll(want);
    }

    /** 当前应弱加载的区块集合（空集 = 不加载任何区块） */
    private Set<ChunkPos> computeWantedChunks() {
        if (structure == null || structure.chunkLoaderCount <= 0) {
            return Set.of();
        }
        int pad = structure.chunkRangeCount > 0 ? CHUNK_RANGE_PAD : 0;
        int span = BIND_RANGE + pad;
        BlockPos min = structure.min.offset(-span, 0, -span);
        BlockPos max = structure.max.offset(span, 0, span);
        ChunkPos first = new ChunkPos(min.getX(), min.getZ());
        ChunkPos last = new ChunkPos(max.getX(), max.getZ());
        Set<ChunkPos> want = new LinkedHashSet<>();
        for (int cx = first.x; cx <= last.x; cx++) {
            for (int cz = first.z; cz <= last.z; cz++) {
                want.add(new ChunkPos(cx, cz));
            }
        }
        return want;
    }

    /**
     * 释放全部弱加载票据。
     * <p>
     * 只在<b>方块被拆</b>时由方块调用；切勿挪进 {@code setRemoved()} —— 区块卸载同样会触发它，
     * 那样一卸载就把票放了，弱加载机制会自废。
     */
    public void releaseChunkLoad() {
        if (level instanceof ServerLevel serverLevel) {
            for (ChunkPos cp : loadedChunks) {
                serverLevel.getChunkSource().removeRegionTicket(TicketType.PORTAL, cp, CHUNK_TICKET_RADIUS,
                        cp.getWorldPosition());
            }
        }
        loadedChunks.clear();
    }

    // ===== 能源接入口汇聚 + 一次性费用结算（D13 三条硬约束） =====

    /**
     * 汇聚贴装的赤能源接入口能量（多口并联）：墙面镶嵌与外侧 1 格贴装都算。
     * <p>
     * 唯一收能路径：只在存取结算时由本方法主动抽取，口自身不推送、本机也无每 tick 遍历，
     * 避免同一份能量被两条路径抽两次（D13 约束 1）。按 pos 去重（约束 2），且每笔重新扫描，
     * 因此不存在脏口列表；墙面 + 外围 1 格最多 316 个候选，成本可忽略。
     */
    private void absorbEnergyFromPorts() {
        if (structure == null || !(level instanceof ServerLevel) || energy.getEnergyStored() >= energy.getMaxEnergy()) {
            return;
        }
        Set<BlockPos> visited = new LinkedHashSet<>();
        BlockPos min = structure.min.offset(-BIND_RANGE, -BIND_RANGE, -BIND_RANGE);
        BlockPos max = structure.max.offset(BIND_RANGE, BIND_RANGE, BIND_RANGE);
        for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
            BlockPos p = cursor.immutable();
            if (insideCavity(p) || !visited.add(p)) {
                continue;
            }
            if (!(level.getBlockEntity(p) instanceof IItemTerminalEnergyPort port)) {
                continue;
            }
            // 先模拟问本机还能装多少，再按该上限抽口 ⇒ 不会出现"抽了却装不下"的能量凭空丢失
            long room = energy.addEnergy(Long.MAX_VALUE, true);
            if (room <= 0L) {
                return; // 缓冲已满，无需继续扫
            }
            long drained = port.drainEnergy(Math.min(room, port.availableEnergy()));
            if (drained <= 0L) {
                continue;
            }
            energy.addEnergy(drained, false);
        }
    }

    /**
     * 结算一笔一次性存取费用（原子预检，禁止部分扣费）。
     * <p>
     * 顺序固定为「汇聚各口 → 判缓冲 ≥ 费用 → 不足整笔拒绝」：失败时能量、物品与账本零改动，
     * 调用方必须放弃本笔（D13 约束 3）。费用口径见 {@link ItemTerminalFee}（4 IP = 1 赤能源）。
     *
     * @param ip      本笔涉及的 IP 量
     * @param deposit true = 存入费率（k=1.005），false = 取出费率（k=1.0025）
     * @return true 表示费用已扣除，可继续写账本
     */
    public boolean tryChargeFee(long ip, boolean deposit) {
        if (ip <= 0L) {
            return true;
        }
        absorbEnergyFromPorts();
        long fee = feeCost(ip, deposit);
        if (fee <= 0L) {
            return true;
        }
        if (energy.getEnergyStored() < fee) {
            return false;
        }
        energy.extractEnergy(fee, false);
        setChanged();
        return true;
    }

    /**
     * 非破坏性费用预检：这笔费用现在付得起吗？
     * <p>
     * 口径与 {@link #tryChargeFee} 完全一致（先汇聚各口，再判缓冲 ≥ 费用），但<b>不扣费</b>：
     * 供外部物流能力在 {@code simulate} / {@code canPlaceItem} 阶段先给出承诺，避免"承诺了却付不起"。
     */
    public boolean canAffordFee(long ip, boolean deposit) {
        if (ip <= 0L) {
            return true;
        }
        absorbEnergyFromPorts();
        return energy.getEnergyStored() >= feeCost(ip, deposit);
    }

    /** 单笔费用换算（IP → 赤能源，0 = 免费）；费率口径唯一来源见 {@link ItemTerminalFee} */
    private long feeCost(long ip, boolean deposit) {
        return deposit ? ItemTerminalFee.depositCost(ip, effectiveFeeModules())
                : ItemTerminalFee.withdrawCost(ip, effectiveFeeModules());
    }

    // ===== 安全状态（归属者 + 权限表；权限载体为身份卡，规则见 TerminalSecurity） =====

    /** 安全状态（安全页 / 判定入口） */
    public TerminalSecurity security() {
        return security;
    }

    /** 记录归属者（结构主方块放置时由方块调用）；归属者恒全权限，不占权限表条目 */
    public void setOwner(UUID owner, String name) {
        security.setOwner(owner, name);
    }

    // ===== NBT 持久化 =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        // 缓冲随物品走（拆除后重新放置不丢能量，同矿机能量输入口取舍）
        tag.putLong("Energy", energy.getEnergyStored());
        tag.putUUID("TerminalId", terminalId);
        security.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
        // 旧档无 ID 时保留构造期生成的随机 ID（模组未发布，不做跨版本兼容）
        if (tag.hasUUID("TerminalId")) {
            terminalId = tag.getUUID("TerminalId");
        }
        security.load(tag);
    }

    // ===== 微缩（坍缩为单方块）：见 IMiniaturizableTerminal 与 MiniatureCollapse =====

    @Override
    public ResourceLocation miniatureTypeId() {
        return ItemTerminalMiniatureAdapter.ID;
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
     * 导出微缩数据：把「各储存单元的等阶 / 内容 / 账本 + 安全表 + 缓冲余额 + 生效的扩展与减免份数」
     * 一次写全，键名与 {@link ItemTerminalMiniatureState} 的读回口径严格对应（单向漂移就会变成脏数据）。
     * <p>
     * 储存单元此刻还是方块实体，故经 {@link ItemStorageUnitData#writeEntry} 从接口面导出。
     */
    @Override
    public CompoundTag captureMiniature() {
        // 结构与贴装件是 20 tick 缓存：取数前必须重扫，否则刚放下的单元会被"连方块一起清掉但数据没进 payload"
        refreshUnitsNow();
        // 接入口的能量要一并带走：先把各口汇聚进本机缓冲，再导出缓冲余额
        // （口本身会随后被消耗，不汇聚就等于把这些赤能源凭空销毁）
        absorbEnergyFromPorts();
        CompoundTag payload = new CompoundTag();
        CompoundTag sec = new CompoundTag();
        security.save(sec);
        payload.put(ItemTerminalMiniatureState.TAG_SECURITY, sec);
        payload.putLong(ItemTerminalMiniatureState.TAG_ENERGY, energy.getEnergyStored());
        payload.putInt(ItemTerminalMiniatureState.TAG_BUFFER_MODULES, structure == null ? 0
                : Math.min(structure.bufferModuleCount, AkaishiItemTerminalBufferModuleBlock.MAX_EFFECTIVE));
        payload.putInt(ItemTerminalMiniatureState.TAG_FEE_MODULES, structure == null ? 0 : structure.feeModuleCount);
        ListTag units = new ListTag();
        for (BlockEntity be : unitEntities) {
            if (be.isRemoved() || !(be instanceof IItemStorageUnit unit)) {
                continue;
            }
            units.add(ItemStorageUnitData.writeEntry(unit, tierOf(be)));
        }
        payload.put(ItemTerminalMiniatureState.TAG_UNITS, units);
        return payload;
    }

    /** 储存单元方块的等阶（异常方块退回基础档，防脏数据把整个微缩流程带崩） */
    private static ItemStorageUnitTier tierOf(BlockEntity be) {
        return be.getBlockState().getBlock() instanceof AkaishiItemStorageUnitBlock block
                ? block.getTier() : ItemStorageUnitTier.BASIC;
    }

    /**
     * 箱体之外、必须随坍缩一并消耗的贴装件：储存单元 + 赤能源接入口。
     * <p>
     * 两类方块都允许装在外侧 1 格（在箱体之外）：
     * <ul>
     *   <li>储存单元：内容已进 payload，留下就是同一批物品的第二份；</li>
     *   <li>接入口：能量已汇聚进 payload（{@link #captureMiniature}），留下就是一个带电、却再也没有主机可服务的孤儿口。</li>
     * </ul>
     * 墙内镶嵌的那些本来就在箱体里，会被箱体遍历消耗掉；这里重复返回它们是幂等的。
     */
    @Override
    public List<BlockPos> extraConsumedBlocks() {
        List<BlockPos> positions = new ArrayList<>(unitEntities.size());
        for (BlockEntity be : unitEntities) {
            if (!be.isRemoved() && be instanceof IItemStorageUnit) {
                positions.add(be.getBlockPos());
            }
        }
        positions.addAll(energyPortPositions());
        return positions;
    }

    /** 结构范围内（含外圈 1 格、不含内腔）的赤能源接入口位置 */
    private List<BlockPos> energyPortPositions() {
        if (structure == null) {
            return List.of();
        }
        List<BlockPos> positions = new ArrayList<>();
        Set<BlockPos> visited = new LinkedHashSet<>();
        BlockPos min = structure.min.offset(-BIND_RANGE, -BIND_RANGE, -BIND_RANGE);
        BlockPos max = structure.max.offset(BIND_RANGE, BIND_RANGE, BIND_RANGE);
        for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
            BlockPos p = cursor.immutable();
            if (insideCavity(p) || !visited.add(p)
                    || !(level.getBlockEntity(p) instanceof IItemTerminalEnergyPort)) {
                continue;
            }
            positions.add(p);
        }
        return positions;
    }

    // ===== 菜单入口（ExtendedMenuProvider：坐标经 saveExtraData 传给客户端工厂） =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_item_terminal");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        // 把本机交给菜单：库页聚合读单元视图、每笔扣费读缓冲，均需终端实例
        return new AkaishiItemTerminalMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }
}
