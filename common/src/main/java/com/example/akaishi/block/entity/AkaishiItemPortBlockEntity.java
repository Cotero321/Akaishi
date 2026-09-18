package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.api.security.AkaishiSecurityPermission;
import com.example.akaishi.api.storage.IItemStorageUnit;
import com.example.akaishi.api.storage.IItemTerminalHost;
import com.example.akaishi.menu.AkaishiItemPortBindingSync;
import com.example.akaishi.menu.AkaishiItemPortMenu;
import com.example.akaishi.menu.TerminalActions;
import com.example.akaishi.util.LongDataSlots;
import com.example.akaishi.value.ItemPoints;
import com.example.akaishi.wireless.ItemPortTransfer;
import com.example.akaishi.wireless.ItemTerminalRegistry;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * 储存无线输入/输出口方块实体（输入/输出<b>共用此类</b>，方向由方块在构造时传入）。
 * <p>
 * 语义（照 AE2 输入/输出总线）：输入口把<b>面朝方向</b>容器的物品抓进绑定的物品终端储存；
 * 输出口把终端储存推给面朝方向的容器。搬运本身在 {@link ItemPortTransfer}，本类只负责：
 * <ul>
 *   <li><b>远程绑定</b>：记录「<b>终端 ID</b> + 绑定身份（玩家 UUID）」，全部 NBT 持久化（无身份卡）。
 *       寻址用终端 ID 而非坐标：终端搬动 / 被微缩后绑定依然有效（坐标随机器变，ID 不变）；</li>
 *   <li><b>节流</b>：每 {@link #PERIOD} tick 搬一批（每批 ≤1 组），避免每 tick 扫容器；</li>
 *   <li><b>权限</b>：按绑定身份查终端<b>本地权威</b>安全表 —— 输入口要 INJECT、输出口要 EXTRACT；
 *       归属者恒全权，终端没登记任何条目时全放行（单机不受影响）。</li>
 *   <li><b>过滤网</b>：9 格真槽位（照 AE2 总线配置槽；界面里就是 {@code menu.slots} 的 9 格），全空 = 全通；
 *       输入口只抓匹配物、输出口只推匹配物。槽内容即本类的过滤数组 —— <b>单一数据源</b>，
 *       界面经 {@code ItemPortFilterContainer} 适配直读写，客户端由原版 menu 槽同步承接，销毁随掉落物带走。</li>
 *   <li><b>费用</b>：搬运即终端存取，按<b>实际搬运量</b>以普通存储口径结算赤能源（输入口=存入费率、
 *       输出口=取出费率，算法与库页存取完全一致）；能量不足则整批跳过（不搬也不扣费），运行页红字提示。</li>
 *   <li><b>对外直达</b>：本类是「终端储存的远程接口面」，但<b>没有内部容器</b>（{@link IItemPipeDevice}
 *       只有 1 个虚拟转发槽）—— 输入口只认外部塞入、输出口只认外部抽取（单向硬约束），
 *       权限与费用复用上面同一套闸门，第三方物流（管道 / 漏斗 / MEK / AE2）经 forge 侧专用转发
 *       能力对接（通用适配层按"真实槽位"记账，对纯转发机器会丢物或复制，故不适用）。</li>
 * </ul>
 * 界面数据（方向/绑定态/上次搬运/赤能源不足标志/绑定身份短号）经 {@link ContainerData} 同步；
 * 绑定清单走 {@link com.example.akaishi.menu.AkaishiItemPortBindingSync} 的 S2C 快照，
 * 过滤网则随 9 格真槽位走原版 menu 槽同步（不新增、不重排既有 DATA_* 下标）。
 */
public class AkaishiItemPortBlockEntity extends BlockEntity
        implements ExtendedMenuProvider, IDataCarrier, IItemPipeDevice {

    /** 搬运间隔（tick）：每批 1 组，8 tick 一批 ≈ 8 组/秒 */
    public static final int PERIOD = 8;

    // ===== 数据槽（界面只读，只同步必要数字） =====
    /** 方向标志（1=输出口，0=输入口） */
    public static final int DATA_IS_OUTPUT = 0;
    /** 绑定态（1=已绑定终端） */
    public static final int DATA_BOUND = 1;
    /** 上次搬运件数 */
    public static final int DATA_LAST_MOVED = 2;
    /** 绑定身份短 ID 低 16 位（8 位 hex 需拆 2 槽） */
    public static final int DATA_IDENTITY_HASH = 3;
    /** 绑定身份短 ID 高 16 位 */
    public static final int DATA_IDENTITY_HASH_HIGH = 4;
    /** 赤能源不足本批跳过标志（1=已暂停搬运；追加槽，不重排既有下标） */
    public static final int DATA_ENERGY_SHORT = 5;
    public static final int DATA_SLOTS = 6;

    /** 过滤网槽数（3×3，照 AE2 总线配置槽规模）；全空 = 全通 */
    public static final int FILTER_SLOTS = 9;

    private final boolean output;

    private final SimpleContainerData data = new SimpleContainerData(DATA_SLOTS);

    /**
     * 过滤网（3×3，AE2 总线配置槽口径；恒单件，不占数据槽下标）。
     * 界面 9 格真槽位经 {@code ItemPortFilterContainer} 直读写本数组：过滤只有这一份数据源，
     * {@link #matches(ItemStack)} 读到的就是槽里那份物品。
     */
    private final ItemStack[] filter = emptyFilter();

    /** 绑定身份（绑定该口的玩家 UUID）；null = 未绑定 */
    private UUID boundIdentity;
    /** 绑定终端 ID（对外寻址唯一 key；null = 未选终端） */
    private UUID boundTerminalId;
    /** 已绑定终端标签缓存（「维度 x,y,z」，终端离线时 GUI 仍显示上次位置） */
    private String boundLabel = "";
    /** 上次搬运件数（GUI 显示 / 排障用） */
    private int lastMoved;
    /** 上次搬运尝试是否因赤能源不足整批跳过（GUI 红字提示 / 排障用） */
    private boolean energyShort;

    public AkaishiItemPortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean output) {
        super(type, pos, state);
        this.output = output;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiItemPortBlockEntity be) {
        be.tickServer();
        be.syncData();
    }

    /** 数据槽同步（界面只读）：方向 / 绑定态 / 上次搬运件数 / 绑定身份短号（32 位拆 2 槽） */
    private void syncData() {
        data.set(DATA_IS_OUTPUT, output ? 1 : 0);
        data.set(DATA_BOUND, isBound() ? 1 : 0);
        data.set(DATA_LAST_MOVED, lastMoved);
        data.set(DATA_ENERGY_SHORT, energyShort ? 1 : 0);
        LongDataSlots.writeInt(data, DATA_IDENTITY_HASH, DATA_IDENTITY_HASH_HIGH,
                boundIdentity == null ? 0 : shortHash(boundIdentity));
    }

    /** 界面数据容器 */
    public ContainerData data() {
        return data;
    }

    /** UUID 前 4 字节（高位 32 bit）：界面 8 位 hex 短号，与身份卡短号一致 */
    private static int shortHash(UUID id) {
        return (int) (id.getMostSignificantBits() >>> 32);
    }

    /** 无事可做：清零运行页计数与告警 */
    private void idle() {
        lastMoved = 0;
        energyShort = false;
    }

    private void tickServer() {
        if (boundTerminalId == null || boundIdentity == null) {
            idle();
            return;
        }
        // 节流：分周期搬运（错开不同端口的相位，避免同一 tick 全服集中扫描）
        if ((level.getGameTime() + worldPosition.asLong()) % PERIOD != 0) {
            return;
        }
        IItemTerminalHost terminal = resolveTerminal();
        if (terminal == null) {
            idle();
            return;
        }
        // 顺带刷新「已绑定终端」标签：终端搬动后 GUI 显示的位置随之更新（离线则保留旧标签）
        refreshBoundLabel();
        // 方向权限：输入口=存入(INJECT)、输出口=取出(EXTRACT)；按绑定身份判定
        AkaishiSecurityPermission required = output
                ? AkaishiSecurityPermission.EXTRACT : AkaishiSecurityPermission.INJECT;
        if (!terminal.security().check(boundIdentity, required)) {
            idle();
            return;
        }
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.DirectionalBlock.FACING);
        if (facing == null) {
            idle();
            return;
        }
        // 面朝容器在本口所在维度：传本口自己的 ServerLevel（终端绑定改用 ID 后可跨维度绑定，
        // 若误传终端所在维度，跨维度时会在错误维度里找面朝容器）
        if (!(level instanceof ServerLevel ownLevel)) {
            idle();
            return;
        }
        // 过滤网（AE2 总线配置槽口径）：输入口只抓匹配物、输出口只推匹配物；全空 = 全通
        // 费用闸门：输入口=存入费率、输出口=取出费率（与物品终端 GUI 存取同一套算法，不足则整批跳过）
        energyShort = false;
        lastMoved = output
                ? ItemPortTransfer.pushOut(ownLevel, worldPosition, facing, terminal, this::matches,
                        (unit, slot, stored, batch) -> chargeWithdraw(terminal, unit, slot, stored, batch))
                : ItemPortTransfer.pullInto(ownLevel, worldPosition, facing, terminal, this::matches,
                        batch -> chargeDeposit(terminal, batch));
    }

    // ===== 费用闸门（口径统一来自既有存取路径，本类只做「算 IP → tryChargeFee」） =====

    /**
     * 输入口：存入费率。IP = 单件占用 × 本批件数，算法同库页存入（{@code ItemPoints.perItem} 折算、
     * 基准 4 IP = 1 赤能源，减免并入 {@code tryChargeFee} 内部）。
     */
    private boolean chargeDeposit(IItemTerminalHost terminal, ItemStack batch) {
        long ip = ItemPoints.of(batch);
        if (ip <= 0L || terminal.tryChargeFee(ip, true)) {
            return true;
        }
        energyShort = true;
        return false;
    }

    /**
     * 输出口：取出费率。IP 取<b>单元账本</b>（{@code TerminalActions.withdrawIp}，与库页取出同一方法），
     * 不重算价值表，故与 GUI 取出费用严格一致。
     */
    private boolean chargeWithdraw(IItemTerminalHost terminal, IItemStorageUnit unit,
            int slot, ItemStack stored, ItemStack batch) {
        long ip = TerminalActions.withdrawIp(unit, slot, stored.getCount(), batch.getCount());
        if (ip <= 0L || terminal.tryChargeFee(ip, false)) {
            return true;
        }
        energyShort = true;
        return false;
    }

    // ===== 对外物品能力（管道 / 第三方物流直达） =====
    // 对外语义：把端口当作「终端储存的远程接口面」—— 输入口只允许外部塞入（塞进来的物品直接进
    // 绑定终端），输出口只允许外部抽取（从绑定终端取出给外部）。槽位是 1 个虚拟转发槽（下标恒 0，
    // 不存物）：输入口声明为输入槽、输出口声明为输出槽，方向由声明 + 实现双重硬约束。
    // 权限与费用复用面朝容器搬运那一套闸门（chargeDeposit / chargeWithdraw），不另立口径；
    // 单次转发量上限 = ItemPortTransfer.BATCH_LIMIT（1 组），避免一次巨量塞入绕过节流。

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public int[] getPipeInputSlots() {
        return output ? new int[0] : new int[] {0};
    }

    @Override
    public int[] getPipeOutputSlots() {
        return output ? new int[] {0} : new int[0];
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? externalPreview() : ItemStack.EMPTY;
    }

    /** 虚拟槽空否：输入口恒空（本口不存物），输出口以"当前会推出的东西"为准 */
    @Override
    public boolean isEmpty() {
        return getItem(0).isEmpty();
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return slot == 0 ? externalExtract(amount, false) : ItemStack.EMPTY;
    }

    /** 写入落点：只有输入口会真正转发（写入前必经 {@link #canPlaceItem} 的整堆预检） */
    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != 0) {
            return;
        }
        ItemStack leftover = externalInsert(stack, false);
        if (leftover.isEmpty() || level == null) {
            return;
        }
        // 防御性兜底：自家管道与 InvWrapper 都会先经 canPlaceItem 整堆预检，正常到不了这里；
        // 万一被不守契约的调用方直接写入，余量落地而不是凭空消失
        net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, leftover);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && !output && externalCanAccept(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /** 虚拟槽没有库存：过滤网是<b>配置</b>不是物品，外部清空不生效（防误清玩家的过滤表） */
    @Override
    public void clearContent() {
    }

    /** 单槽上限 = 单次转发上限（1 组），与 {@link ItemPortTransfer#BATCH_LIMIT} 同源 */
    @Override
    public int getMaxStackSize() {
        return ItemPortTransfer.BATCH_LIMIT;
    }

    /**
     * 外部塞入（第三方 {@code insertItem} / 管道写入）：输入口转发进绑定终端，返回<b>未接收的余量</b>；
     * 输出口一律原样退回（不允许反向塞）。未绑定 / 终端离线未加载 / 无 INJECT 权限 / 不匹配过滤
     * 全部原堆退回（不吞物）。simulate 只按终端空间给出上界，不扣费、不落账。
     */
    public ItemStack externalInsert(ItemStack stack, boolean simulate) {
        if (output || stack == null || stack.isEmpty()) {
            return stack == null ? ItemStack.EMPTY : stack;
        }
        IItemTerminalHost terminal = resolveTerminal();
        if (terminal == null
                || !terminal.security().check(boundIdentity, AkaishiSecurityPermission.INJECT)
                || !matches(stack)) {
            return stack;
        }
        if (simulate) {
            return stack.copyWithCount(
                    stack.getCount() - ItemPortTransfer.acceptLimit(terminal, stack, null));
        }
        energyShort = false;
        ItemStack leftover = ItemPortTransfer.insertIntoTerminal(terminal, stack, null,
                batch -> chargeDeposit(terminal, batch));
        int moved = stack.getCount() - leftover.getCount();
        if (moved > 0) {
            lastMoved = moved;
        }
        return leftover;
    }

    /**
     * 外部抽取（第三方 {@code extractItem} / 管道抽取）：输出口从绑定终端取出（单次 ≤1 组），
     * 返回<b>实抽出的堆</b>；输入口一律返回空堆（不允许反向抽）。未绑定 / 终端离线未加载 /
     * 无 EXTRACT 权限 / 费用不足 一律返回空堆，物品与账本零改动。
     */
    public ItemStack externalExtract(int amount, boolean simulate) {
        if (!output || amount <= 0) {
            return ItemStack.EMPTY;
        }
        IItemTerminalHost terminal = resolveTerminal();
        if (terminal == null
                || !terminal.security().check(boundIdentity, AkaishiSecurityPermission.EXTRACT)) {
            return ItemStack.EMPTY;
        }
        int want = Math.min(amount, ItemPortTransfer.BATCH_LIMIT);
        for (IItemStorageUnit unit : terminal.storageUnits()) {
            int slots = unit.slots();
            for (int slot = 0; slot < slots; slot++) {
                ItemStack stored = unit.getItem(slot);
                if (stored.isEmpty() || !matches(stored)) {
                    continue;
                }
                int take = Math.min(want, stored.getCount());
                if (simulate) {
                    return stored.copyWithCount(take);
                }
                energyShort = false;
                if (!chargeWithdraw(terminal, unit, slot, stored, stored.copyWithCount(take))) {
                    return ItemStack.EMPTY; // 费用不足：整笔拒绝，物品与账本零改动
                }
                ItemStack extracted = unit.extract(slot, take);
                if (extracted.getCount() > 0) {
                    lastMoved = extracted.getCount();
                }
                return extracted;
            }
        }
        return ItemStack.EMPTY;
    }

    /** 外部观察：输出口 = 当前会推出的堆（副本，供管道读取）；输入口 = 空（本口不存物） */
    public ItemStack externalPreview() {
        if (!output) {
            return ItemStack.EMPTY;
        }
        IItemTerminalHost terminal = resolveTerminal();
        if (terminal == null
                || !terminal.security().check(boundIdentity, AkaishiSecurityPermission.EXTRACT)) {
            return ItemStack.EMPTY;
        }
        for (IItemStorageUnit unit : terminal.storageUnits()) {
            int slots = unit.slots();
            for (int slot = 0; slot < slots; slot++) {
                ItemStack stored = unit.getItem(slot);
                if (!stored.isEmpty() && matches(stored)) {
                    return stored.copy();
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 外部可接收预检（容器契约的 {@code canPlaceItem}）：绑定 + INJECT 权限 + 过滤 + 终端空间
     * + 费用<b>全部可行</b>才为 true —— 先承诺"整堆收得下"，写入端才会调 {@link #setItem}，
     * 因此不会出现"承诺了却只收下一部分"的丢物。
     */
    public boolean externalCanAccept(ItemStack stack) {
        if (output || stack == null || stack.isEmpty() || !matches(stack)) {
            return false;
        }
        IItemTerminalHost terminal = resolveTerminal();
        if (terminal == null
                || !terminal.security().check(boundIdentity, AkaishiSecurityPermission.INJECT)) {
            return false;
        }
        int batch = Math.min(stack.getCount(), ItemPortTransfer.BATCH_LIMIT);
        ItemStack probe = stack.copyWithCount(batch);
        if (ItemPortTransfer.acceptLimit(terminal, probe, null) < batch) {
            return false; // 终端空间不足：不能承诺整堆收下
        }
        return terminal.canAffordFee(ItemPoints.of(probe), true);
    }

    /** 绑定终端解析：未绑定 / 终端离线（未注册 / 区块未加载 / 已被拆）⇒ null（拒绝一切外部操作） */
    private IItemTerminalHost resolveTerminal() {
        if (level == null || level.isClientSide || level.getServer() == null
                || boundTerminalId == null || boundIdentity == null) {
            return null;
        }
        return ItemTerminalRegistry.resolve(level.getServer(), boundTerminalId);
    }

    // ===== 过滤网 =====

    private static ItemStack[] emptyFilter() {
        ItemStack[] slots = new ItemStack[FILTER_SLOTS];
        java.util.Arrays.fill(slots, ItemStack.EMPTY);
        return slots;
    }

    /**
     * 过滤匹配（AE2 口径）：过滤网全空 ⇒ 全通；否则任一槽与目标堆<b>物品 + NBT 一致</b>即通过。
     * 不做模糊 / 矿物词典模式，保持简单可预期。
     */
    public boolean matches(ItemStack stack) {
        boolean configured = false;
        for (ItemStack slot : filter) {
            if (slot.isEmpty()) {
                continue;
            }
            configured = true;
            if (!stack.isEmpty() && ItemStack.isSameItemSameTags(slot, stack)) {
                return true;
            }
        }
        return !configured;
    }

    /**
     * 过滤槽读取（容器适配用；越界返回空堆）。
     * <p>
     * 直接返回底层堆（不复制）：界面槽位每个 tick 都会读它，复制会白白产生垃圾；
     * 单件语义由 {@link #setFilterSlot(int, ItemStack)} 保证，写入端一律取单件副本。
     */
    public ItemStack filterSlot(int slot) {
        return slot < 0 || slot >= FILTER_SLOTS ? ItemStack.EMPTY : filter[slot];
    }

    /** 过滤槽写入（容器适配用；传空 = 清除该槽，越界忽略）：只留单件副本，配置槽永不成堆 */
    public void setFilterSlot(int slot, ItemStack stack) {
        if (slot < 0 || slot >= FILTER_SLOTS) {
            return;
        }
        ItemStack next = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        if (ItemStack.matches(filter[slot], next)) {
            return;
        }
        filter[slot] = next;
        setChanged();
    }

    // ===== 远程绑定 =====

    /**
     * 远程绑定：终端 ID + 绑定身份（GUI 调用；覆盖旧绑定）。
     * <p>
     * 只落终端 ID，不落坐标：终端搬动 / 被微缩后本口仍指向同一终端，无需重新绑定。
     * 展示用的位置标签由 {@link #refreshBoundLabel()} 按注册表实时刷新并缓存。
     */
    public void bindTerminal(UUID terminalId, UUID identity) {
        if (terminalId == null || identity == null) {
            return;
        }
        if (!terminalId.equals(boundTerminalId) || !identity.equals(boundIdentity)) {
            boundTerminalId = terminalId;
            boundIdentity = identity;
            lastMoved = 0;
            setChanged();
        }
        refreshBoundLabel();
    }

    /** 解绑 */
    public void unbind() {
        if (boundTerminalId != null || boundIdentity != null || !boundLabel.isEmpty()) {
            boundTerminalId = null;
            boundIdentity = null;
            boundLabel = "";
            lastMoved = 0;
            setChanged();
        }
    }

    public boolean isBound() {
        return boundTerminalId != null && boundIdentity != null;
    }

    /** 绑定本口的玩家 UUID（未绑定为 null）：改绑/解绑的归属判据 */
    public UUID boundIdentityView() {
        return boundIdentity;
    }

    /** 当前绑定的终端 ID（未绑定为 null） */
    public UUID boundTerminalIdView() {
        return boundTerminalId;
    }

    /** 绑定终端标签（「维度 x,y,z」；未绑定为空串）：终端在线时取其当前位置，离线保留上次位置 */
    public String boundTargetLabel() {
        return boundLabel;
    }

    /**
     * 刷新已绑定终端的位置标签（终端在线才更新；离线保留旧值，GUI 仍能显示"绑定的是哪台"）。
     * 位置来自注册表（终端上报的心跳），因此终端搬走后标签会跟着变。
     */
    private void refreshBoundLabel() {
        if (boundTerminalId == null) {
            return;
        }
        if (level == null || level.isClientSide) {
            return;
        }
        ItemTerminalRegistry.Snapshot snapshot = ItemTerminalRegistry.locate(boundTerminalId);
        if (snapshot == null) {
            return;
        }
        String next = AkaishiItemPortBindingSync.label(snapshot.dimension().location(), snapshot.pos());
        if (!next.equals(boundLabel)) {
            boundLabel = next;
            setChanged();
        }
    }

    /** 上次搬运件数（GUI / 排障） */
    public int lastMoved() {
        return lastMoved;
    }

    /** 上次搬运尝试是否因赤能源不足整批跳过（GUI 红字告警） */
    public boolean energyShort() {
        return energyShort;
    }

    public boolean isOutput() {
        return output;
    }

    // ===== 菜单 =====

    @Override
    public Component getDisplayName() {
        return Component.translatable(output
                ? "block.akaishi.akaishi_item_output_port" : "block.akaishi.akaishi_item_input_port");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiItemPortMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (boundIdentity != null) {
            tag.putUUID("BoundIdentity", boundIdentity);
        }
        if (boundTerminalId != null) {
            tag.putUUID("BoundTerminal", boundTerminalId);
        }
        if (!boundLabel.isEmpty()) {
            tag.putString("BoundLabel", boundLabel);
        }
        // 过滤网：逐槽写（含 NBT）；空槽写空 CompoundTag 占位，保证读取时下标不错位
        ListTag filterTag = new ListTag();
        for (ItemStack slot : filter) {
            filterTag.add(slot.isEmpty() ? new CompoundTag() : slot.save(new CompoundTag()));
        }
        tag.put("Filter", filterTag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        boundIdentity = tag.hasUUID("BoundIdentity") ? tag.getUUID("BoundIdentity") : null;
        boundTerminalId = tag.hasUUID("BoundTerminal") ? tag.getUUID("BoundTerminal") : null;
        boundLabel = tag.getString("BoundLabel");
        // 过滤网：短列表 / 空占位 / 脏 id 一律当空槽，不让旧档或脏数据整块失效
        ListTag filterTag = tag.getList("Filter", Tag.TAG_COMPOUND);
        for (int i = 0; i < FILTER_SLOTS; i++) {
            filter[i] = i < filterTag.size() ? ItemStack.of(filterTag.getCompound(i)) : ItemStack.EMPTY;
        }
    }
}
