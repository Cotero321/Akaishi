package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.api.miniature.IMiniatureChipView;
import com.example.akaishi.api.miniature.MiniatureTerminalAdapter;
import com.example.akaishi.api.miniature.MiniatureTerminalRegistry;
import com.example.akaishi.api.miniature.MiniatureTerminalState;
import com.example.akaishi.api.storage.IItemStorageUnit;
import com.example.akaishi.api.storage.IItemTerminalHost;
import com.example.akaishi.api.storage.IWirelessTerminalHost;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.wireless.IWirelessTerminal;
import com.example.akaishi.wireless.ItemPortTransfer;
import com.example.akaishi.wireless.ItemTerminalRegistry;
import com.example.akaishi.wireless.TerminalSecurity;
import com.example.akaishi.wireless.WirelessFamily;
import com.example.akaishi.wireless.WirelessNetworkManager;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 微缩终端方块实体：<b>通用</b>承载层，本身不认识任何具体终端族。
 * <p>
 * 数据分三层落盘：{@code TerminalType}（族 id → 找适配器）、{@code TerminalId}（终端唯一 ID，
 * 与微缩前<b>同一个</b>，故端口/无线设备的既有绑定不会因坍缩而失效）、{@code Payload}（族自有数据，
 * 由 {@link MiniatureTerminalState#save()} 产出）。
 * <p>
 * 对外能力<b>全部经状态转发</b>：物品能力按状态声明的虚拟槽（输入/输出各一，语义同储存无线口：
 * 无内部容器、纯转发、费用与权限由状态把关）；能量走项目自研 {@link IEnergyProvider}，
 * <b>绝不注册第三方能量能力</b>（赤能源/生命能量自研，见项目铁律）。
 * <p>
 * 掉落保留：实现 {@link IDataCarrier}，被拆时数据随掉落物走（由 {@code AkaishiMachineBlock} 写入）；
 * 因此"解包还原"即便暂未实现，玩家的数据也不会因为一次误拆而蒸发。
 */
public class MiniatureTerminalBlockEntity extends BlockEntity
        implements ExtendedMenuProvider, IDataCarrier, IItemPipeDevice, IEnergyProvider, IWirelessTerminal,
        IMiniatureChipView {

    /** 排障日志：只在"已装载却被二次覆盖"这类异常路径上打点 */
    private static final org.apache.logging.log4j.Logger LOGGER =
            org.apache.logging.log4j.LogManager.getLogger("akaishi.miniature");

    /** 空壳能量视图：族不持能量时兜底（{@link IEnergyProvider} 合同允许 null，但管道更愿意拿到非空） */
    private static final AkaishiEnergyStorage NO_ENERGY =
            new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, 0L);

    /** 终端族 id（空壳为 null） */
    private ResourceLocation terminalType;
    /** 终端唯一 ID（与微缩前一致） */
    private UUID terminalId;
    /** 族自有数据（状态存盘的原始 NBT，读档时交给适配器还原） */
    private CompoundTag payload = new CompoundTag();
    /** 族适配器（按 {@link #terminalType} 解析；未注册族为 null ⇒ 只读空壳） */
    private MiniatureTerminalAdapter adapter;
    /** 族状态（承载全部数据与能力） */
    private MiniatureTerminalState state;
    /**
     * 坍缩时的结构快照（每格「相对坐标 + 方块 id」+ 终端自身偏移）。
     * <p>
     * <b>首版不做解包，但快照必须照写</b>：不写，"以后能不能原样还原"这条路就永久堵死了。
     * 快照由本类持有而非塞进族 payload —— 族状态的 {@code save()} 只写自己那套键，混存会被抹掉。
     */
    private CompoundTag structureSnapshot = new CompoundTag();

    /** 快照内的逐格条目键 */
    public static final String TAG_STRUCTURE_BLOCKS = "Blocks";
    /** 快照内的终端方块偏移键（还原时用来定位主方块） */
    public static final String TAG_TERMINAL_OFFSET = "TerminalOffset";
    /** NBT 键：结构快照根节点 */
    private static final String TAG_SNAPSHOT = "Structure";

    public MiniatureTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_MINIATURE_TERMINAL.get(), pos, state);
    }

    /**
     * 装载终端数据（坍缩流程调用）。
     * <p>
     * <b>只允许装载一次</b>：已装载过的微缩方块拒绝二次装载。理由：二次装载要么让新旧两批数据
     * 并存（玩家白得一台终端），要么覆盖掉旧数据 —— 而旧数据对应的原终端在坍缩时<b>已经消失</b>，
     * 覆盖即等于凭空吃掉玩家一台机器。所以这里直接拒绝，由调用方决定这段数据的去处。
     *
     * @return true = 装载成功；false = 已有数据、本次拒绝（调用方<b>不得</b>丢弃传入的数据）
     */
    public boolean install(@Nullable ResourceLocation typeId, @Nullable UUID id, CompoundTag payload) {
        if (isLoadedTerminal()) {
            return false;
        }
        apply(typeId, id, payload);
        return true;
    }

    /**
     * 实际写入（读档与装载共用）。
     * <p>
     * 读档走这里而不是 {@link #install}：方块实体是新建的、门禁本无意义，而重复的 {@code load}
     * 不该把档案里已有的数据再叠一次。原始 NBT <b>无条件保留</b>：族 id 未注册（例如某个族被临时移除）
     * 时只退化为"只读空壳"，数据仍原样待在自己的 NBT 里，再次存盘不会被抹掉 —— 宁可读不出，也不能把玩家的库吃掉。
     */
    private void apply(@Nullable ResourceLocation typeId, @Nullable UUID id, CompoundTag payload) {
        this.payload = payload == null ? new CompoundTag() : payload;
        this.terminalType = typeId;
        this.terminalId = id;
        MiniatureTerminalAdapter found = MiniatureTerminalRegistry.adapter(typeId);
        this.adapter = found;
        this.state = found == null || id == null ? null : found.createState(this, this.payload);
        setChanged();
    }

    /** 族状态（未装载为 null） */
    @Nullable
    public MiniatureTerminalState state() {
        return state;
    }

    /** 终端唯一 ID（端口/无线设备寻址用；未装载为 null） */
    @Nullable
    public UUID terminalId() {
        return terminalId;
    }

    /**
     * 是否已持有终端数据。
     * <p>
     * 判据是"<b>手上有没有数据</b>"，而不是"状态对象建没建出来"：族适配器缺失时状态为 null、
     * 但原始 payload 仍在（能原样存回的只读空壳）。若按状态判断，这种方块会被误判成"未装载"，
     * 于是被二次装载<b>覆盖</b>掉玩家唯一的那份数据。
     */
    public boolean isLoadedTerminal() {
        return state != null || terminalType != null || terminalId != null || !payload.isEmpty();
    }

    /** 本族是否提供右键界面（无界面的族不响应右键；判据在适配器上，避免"有适配器却开不出菜单"） */
    public boolean hasMenu() {
        return adapter != null && adapter.hasMenu();
    }

    /** 记录坍缩时的结构快照（坍缩流程调用；解包功能落地时读它还原多方块） */
    public void setStructureSnapshot(CompoundTag snapshot) {
        this.structureSnapshot = snapshot == null ? new CompoundTag() : snapshot;
        setChanged();
    }

    /**
     * 把当前数据推给客户端。
     * <p>
     * <b>必须显式调用</b>：{@code setChanged()} 只标脏存盘，原版不会因此发方块实体数据包；
     * 而客户端要按坐标取本地方块实体造菜单（{@code ModMenus} 三个工厂），拿不到数据就只是空壳。
     * 只允许在"装载/放置完成后"调用，<b>不要</b>放进 {@code load()}（区块加载时会造成无谓刷包）。
     */
    public void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * 方块实体数据下发（读取端在客户端）。
     * <p>
     * <b>为什么必须实现</b>：界面构造发生在客户端（{@code ModMenus} 工厂按坐标取客户端方块实体），
     * 而客户端的状态是<b>从本类 NBT 还原</b>出来的 —— 不下发的话客户端只是空壳（state == null），
     * 界面宿主为 null，右键就会开出坏界面。默认 {@code getUpdatePacket()} 返回 null 即"永不主动下发"，
     * 故这里补上；配合 {@link #setChanged()}（装载 / 坍缩后都会调用）由原版排队发送。
     */
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    /** 结构快照（未坍缩而来 / 快照缺失时为空 tag） */
    public CompoundTag structureSnapshot() {
        return structureSnapshot;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState blockState, MiniatureTerminalBlockEntity be) {
        be.tickState();
    }

    private void tickState() {
        if (state != null) {
            state.tick();
        }
        // 物品族终端要能被储存无线输入/输出口按 ID 找到 ⇒ 上报心跳（与物品终端同一条注册表）
        IItemTerminalHost host = itemHost();
        if (host != null) {
            ItemTerminalRegistry.heartbeat(level, terminalId, worldPosition, host.security().ownerName());
        }
    }

    /**
     * 物品终端宿主视图（非物品族为 null）：菜单工厂与心跳据此复用物品终端库页。
     * <p>
     * 同时是 {@link com.example.akaishi.api.miniature.IMiniatureChipView#itemHost()} 的实现 ——
     * 储存无线输入/输出口按终端 ID 寻址时要靠它把微缩件认成"物品终端"。
     */
    @Override
    @Nullable
    public IItemTerminalHost itemHost() {
        return state == null ? null : state.itemHost();
    }

    /**
     * 无线终端宿主视图（非无线族为 null）。
     * <p>
     * 客户端菜单工厂要用它：客户端是按坐标取<b>本地</b>方块实体来造菜单的，
     * 只认具体终端方块类型的话会把微缩件判成"实体缺失"，开出一张全零的空菜单。
     */
    @Nullable
    public IWirelessTerminalHost wirelessHost() {
        return state instanceof IWirelessTerminalHost host ? host : null;
    }

    // ===== IMiniatureChipView：把两种族的读数归一，供微缩矩阵终端等宿主持有 =====

    @Override
    @Nullable
    public ResourceLocation chipType() {
        return terminalType;
    }

    @Override
    public boolean loaded() {
        return isLoadedTerminal();
    }

    @Override
    public Component displayName() {
        return getDisplayName();
    }

    /** 安全表：物品族取库宿主、能量族取无线宿主 —— 两边都是同一份 TerminalSecurity */
    @Override
    @Nullable
    public TerminalSecurity security() {
        IItemTerminalHost item = itemHost();
        if (item != null) {
            return item.security();
        }
        IWirelessTerminalHost wireless = wirelessHost();
        return wireless == null ? null : wireless.security();
    }

    /** 库读数（IP）：仅物品族有；按各单元账本求和，与库页显示同源 */
    @Override
    @Nullable
    public IpReadout itemReadout() {
        IItemTerminalHost item = itemHost();
        if (item == null) {
            return null;
        }
        long stored = 0L;
        long capacity = 0L;
        for (IItemStorageUnit unit : item.storageUnits()) {
            stored += unit.getStoredIp();
            capacity += unit.getIpCapacity();
        }
        return new IpReadout(stored, capacity);
    }

    /** 能量读数：自研能量池（物品族=赤能源缓冲；能量族=储能池） */
    @Override
    @Nullable
    public EnergyReadout energyReadout() {
        IEnergyStorage energy = state == null ? null : state.energyStorage();
        return energy == null ? null : new EnergyReadout(energy.getEnergyStored(), energy.getMaxEnergy());
    }

    /**
     * 采纳宿主（微缩矩阵终端）的安全表：<b>整体覆盖</b>本芯片的安全设置。
     * <p>
     * 口径「主卡装在矩阵上，一次登记使绑定的终端一同生效」即由此实现 —— 矩阵是唯一编辑入口，
     * 芯片仍持自己的权威表（本方法只是写入方，判定时仍读芯片自身的表）。
     * <p>
     * 用 {@code save → load} 搬运而非新增复制 API：{@link TerminalSecurity#load} 本身带归属者复位、
     * 条数裁剪与位掩码校验，直接复用它的边界处理，避免两套语义漂移。
     *
     * @return 是否写入成功（未装载/无安全表/同源时返回 false）
     */
    public boolean adoptSecurity(TerminalSecurity source) {
        TerminalSecurity target = security();
        if (source == null || target == null || target == source) {
            return false;
        }
        CompoundTag tmp = new CompoundTag();
        source.save(tmp);
        target.load(tmp);
        setChanged();
        syncToClient();
        UUID id = terminalId();
        // 只对已在无线注册表里的终端（能量族）推镜像：否则会凭空造出一条无人回收的条目
        if (id != null && WirelessNetworkManager.entryOf(id) != null) {
            target.pushTo(id);
        }
        return true;
    }

    // ===== 物品能力（虚拟槽，纯转发：语义与 AkaishiItemPortBlockEntity 完全一致） =====

    @Override
    public int getContainerSize() {
        return state == null ? 0 : state.containerSize();
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            if (!getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return state == null ? ItemStack.EMPTY : state.previewSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return state == null ? ItemStack.EMPTY : state.removeSlot(slot, amount);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (state == null) {
            return;
        }
        ItemStack leftover = state.insertSlot(slot, stack);
        // 防御性兜底：物流适配层会先经 canPlaceItem 整堆预检，正常到不了这里；
        // 万一被不守契约的调用方直写，余量落地而不是凭空消失
        if (!leftover.isEmpty() && level != null) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5, leftover);
        }
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return state != null && state.canAcceptSlot(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /** 虚拟槽不存物：外部"清空容器"无意义（数据在族状态里，清空会误伤玩家的库） */
    @Override
    public void clearContent() {
    }

    /** 单槽上限 = 单次转发上限（1 组），与储存口同源 */
    @Override
    public int getMaxStackSize() {
        return ItemPortTransfer.BATCH_LIMIT;
    }

    @Override
    public int[] getPipeInputSlots() {
        return state == null ? new int[0] : state.pipeInputSlots();
    }

    @Override
    public int[] getPipeOutputSlots() {
        return state == null ? new int[0] : state.pipeOutputSlots();
    }

    // ===== 能量（自研；不注册第三方能量能力） =====

    @Override
    public IEnergyStorage getEnergyStorage() {
        IEnergyStorage storage = state == null ? null : state.energyStorage();
        return storage == null ? NO_ENERGY : storage;
    }

    @Override
    public boolean canInputEnergy() {
        return state != null && state.canInputEnergy();
    }

    @Override
    public boolean canOutputEnergy() {
        return state != null && state.canOutputEnergy();
    }

    // ===== 无线网络中枢（经状态转发；物品族的状态默认惰性 ⇒ family() 为 null，永不被认作能量终端） =====

    /**
     * 是否成型：微缩件即成型件（没有多方块结构可失效）。
     * <p>
     * 物品族的状态 {@code family()} 返回 null，而端口解析恒拿具体族枚举比对，故这里返回 true
     * 也不会被误判成能量中枢。
     */
    @Override
    public boolean isFormed() {
        return state != null;
    }

    @Override
    public boolean isCrossDim() {
        return state != null && state.isCrossDim();
    }

    @Override
    public double inputLossReduction() {
        return state == null ? 0.0 : state.inputLossReduction();
    }

    @Override
    public double outputLossReduction() {
        return state == null ? 0.0 : state.outputLossReduction();
    }

    @Override
    public boolean hasTransmitFrame() {
        return state != null && state.hasTransmitFrame();
    }

    @Override
    public long receiveWireless(long amount) {
        return state == null ? 0L : state.receiveWireless(amount);
    }

    @Override
    public long extractWireless(long amount) {
        return state == null ? 0L : state.extractWireless(amount);
    }

    @Override
    public WirelessFamily family() {
        return state == null ? null : state.family();
    }

    // ===== 菜单 / 名称 =====

    @Override
    public Component getDisplayName() {
        return adapter == null
                ? Component.translatable("block.akaishi.akaishi_miniature_terminal")
                : adapter.displayName(payload);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return adapter == null ? null : adapter.createMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (terminalType != null) {
            tag.putString(MiniatureTerminalRegistry.TAG_TYPE, terminalType.toString());
        }
        if (terminalId != null) {
            tag.putUUID(MiniatureTerminalRegistry.TAG_TERMINAL_ID, terminalId);
        }
        if (state != null) {
            tag.put(MiniatureTerminalRegistry.TAG_PAYLOAD, state.save());
        } else if (!payload.isEmpty()) {
            // 空壳（族未注册 / 数据不合法）：原样写回，绝不因"读不出"而抹掉玩家的数据
            tag.put(MiniatureTerminalRegistry.TAG_PAYLOAD, payload);
        }
        if (!structureSnapshot.isEmpty()) {
            tag.put(TAG_SNAPSHOT, structureSnapshot);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ResourceLocation typeId = tag.contains(MiniatureTerminalRegistry.TAG_TYPE)
                ? ResourceLocation.tryParse(tag.getString(MiniatureTerminalRegistry.TAG_TYPE)) : null;
        UUID id = tag.hasUUID(MiniatureTerminalRegistry.TAG_TERMINAL_ID)
                ? tag.getUUID(MiniatureTerminalRegistry.TAG_TERMINAL_ID) : null;
        CompoundTag data = tag.getCompound(MiniatureTerminalRegistry.TAG_PAYLOAD);
        boolean incoming = typeId != null || id != null || !data.isEmpty();
        // 已持有数据时<b>拒绝被覆盖</b>：正常流程里服务端的 load 只发生在新造的方块实体上（放置 / 读档 / 区块加载），
        // 若这里出现"已有数据又被灌一份"，说明有别的路径在动它 —— 宁可保留原有那一份，也不覆盖玩家的终端。
        // <p>
        // <b>只限服务端</b>：客户端那份是"服务端镜像"，方块实体数据包本来就该覆盖它。若客户端也拒绝，
        // {@link #syncToClient()} 下发的更新会被静默丢弃，而菜单是按客户端方块实体构造的 ⇒ 界面读到旧值。
        if (incoming && isLoadedTerminal() && (level == null || !level.isClientSide)) {
            LOGGER.warn("[akaishi] miniature at {} already holds terminal data; incoming load ignored", worldPosition);
            return;
        }
        this.structureSnapshot = tag.getCompound(TAG_SNAPSHOT);
        apply(typeId, id, data);
    }
}
