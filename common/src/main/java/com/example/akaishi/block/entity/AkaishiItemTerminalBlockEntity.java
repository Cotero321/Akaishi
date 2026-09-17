package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.storage.IItemStorageUnit;
import com.example.akaishi.api.storage.IItemTerminalEnergyPort;
import com.example.akaishi.block.AkaishiItemTerminalBlock;
import com.example.akaishi.block.AkaishiItemTerminalBufferModuleBlock;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.menu.AkaishiItemTerminalMenu;
import com.example.akaishi.multiblock.ItemTerminalStructure;
import com.example.akaishi.util.LongDataSlots;
import com.example.akaishi.value.ItemTerminalFee;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
        implements IDataCarrier, IEnergyProvider, ExtendedMenuProvider {

    // ===== 数据槽（long 一律拆 4 槽，防 2^31 截断） =====
    public static final int DATA_FORMED = 0;
    public static final int DATA_USED_LOW = 1;
    public static final int DATA_USED_HIGH = 2;
    public static final int DATA_USED_HIGH2 = 3;
    public static final int DATA_USED_HIGH3 = 4;
    public static final int DATA_CAPACITY_LOW = 5;
    public static final int DATA_CAPACITY_HIGH = 6;
    public static final int DATA_CAPACITY_HIGH2 = 7;
    public static final int DATA_CAPACITY_HIGH3 = 8;
    public static final int DATA_BUFFER_LOW = 9;
    public static final int DATA_BUFFER_HIGH = 10;
    public static final int DATA_BUFFER_HIGH2 = 11;
    public static final int DATA_BUFFER_HIGH3 = 12;
    /** 已贴装并联的储存单元数量 */
    public static final int DATA_UNIT_COUNT = 13;
    /** 有效赤能源缓冲容量（配置基准 + 缓冲扩展组件加成）：客户端据此显示正确的单笔上限 */
    public static final int DATA_EFFECTIVE_BUFFER_LOW = 14;
    public static final int DATA_EFFECTIVE_BUFFER_HIGH = 15;
    public static final int DATA_EFFECTIVE_BUFFER_HIGH2 = 16;
    public static final int DATA_EFFECTIVE_BUFFER_HIGH3 = 17;
    /** 生效的费率减免份数（0 ~ ItemTerminalFee.FEE_MODULE_MAX） */
    public static final int DATA_FEE_MODULES = 18;
    public static final int DATA_SLOTS = 19;

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
        be.tickServer();
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
        long fee = deposit ? ItemTerminalFee.depositCost(ip, effectiveFeeModules())
                : ItemTerminalFee.withdrawCost(ip, effectiveFeeModules());
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

    // ===== NBT 持久化 =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        // 缓冲随物品走（拆除后重新放置不丢能量，同矿机能量输入口取舍）
        tag.putLong("Energy", energy.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
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
