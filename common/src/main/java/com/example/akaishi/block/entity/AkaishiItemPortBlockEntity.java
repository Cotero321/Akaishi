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
import com.example.akaishi.value.ItemTerminalFee;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;
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
 * 界面数据（方向/绑定态/上次搬运/赤能源不足标志/绑定身份短号/<b>搬运失败原因</b>/
 * <b>赤能源不足时的所需与缓冲数字</b>）经 {@link ContainerData} 同步；
 * 绑定清单走 {@link com.example.akaishi.menu.AkaishiItemPortBindingSync} 的 S2C 快照，
 * 过滤网则随 9 格真槽位走原版 menu 槽同步（不新增、不重排既有 DATA_* 下标）。
 * <p>
 * <b>失败原因</b>（{@link #DATA_REASON}，见 {@link #REASON_*}）：绑定目标不存在/已失效、终端未成型、
 * 无储存单元、赤能源不足、<b>方向权限不足</b>、<b>面朝处不是物品容器</b>、<b>终端已存满</b>、
 * <b>对面容器已满</b>、<b>过滤网不匹配</b>、<b>终端内无匹配物</b> ——
 * 服务器在搬运判定处写槽，客户端只读渲染；否则只表现为"一件都不搬"的静默失效。
 * <p>
 * <b>两条独立通路都要报原因</b>：① 本口<b>自身</b>的搬运判定（{@code tickServer}，面朝容器那条路）；
 * ② <b>外部通路</b>（管道 / 第三方物流经 {@link IItemPipeDevice} 塞入或抽取）。后者由第三方随时调用，
 * 其失败（含"先问 {@code canPlaceItem} 就被拒、于是根本不写"的<b>探测型入口</b>）同样写这个原因槽，
 * 见 {@link #noteExternalReason(int)}。
 * <p>
 * <b>信息态（非错误）</b>：{@link #REASON_PIPE}（面朝本模组物品管道）—— 物品由管道网络自动存取，
 * 本口的抓取/推出不适用，界面以中性色呈现（不报红、不打 WARN），与上述失败原因区分开。
 * <p>
 * <b>真实阻塞优先于信息态</b>：外部通路的失败原因带有效期（{@link #EXTERNAL_REASON_TTL_TICKS}），
 * 窗口内即使本口面朝物品管道（{@link #REASON_PIPE} 信息态）也优先显示该失败，
 * 避免"管道刚把物品退回来、界面却说一切正常"。
 */
public class AkaishiItemPortBlockEntity extends BlockEntity
        implements ExtendedMenuProvider, IDataCarrier, IItemPipeDevice {

    /** 排障日志：只在"持续搬不动"这类异常路径上按限流打点 */
    private static final org.apache.logging.log4j.Logger LOGGER =
            org.apache.logging.log4j.LogManager.getLogger("akaishi.itemport");

    /** 搬运间隔（tick）：每批 1 组，8 tick 一批 ≈ 8 组/秒 */
    public static final int PERIOD = 8;

    /** 持续搬不动时 WARN 日志的限流间隔（tick）；待调手感值 */
    private static final long FAIL_LOG_INTERVAL_TICKS = 200;

    /**
     * <b>成功搬运</b>日志的限流间隔（tick）；<b>待调手感值</b>（本值 = 100 tick ≈ 5 秒）。
     * <p>
     * 与失败日志分开限流：成功日志要回答的是"东西到底进了<b>哪台终端的哪个储存单元</b>"，
     * 故间隔取更小（每 5 秒可复现一次），玩家对着界面复现时必定能拿到一条；
     * 稳态长时间搬运时也不会刷屏。
     */
    private static final long SUCCESS_LOG_INTERVAL_TICKS = 100;

    /**
     * 外部通路（管道 / 第三方物流）失败原因的<b>有效期</b>（tick）；<b>待调手感值</b>（建议 2~5 秒，本值 = 3 秒）。
     * <p>
     * 为何需要：外部入口（{@link #externalInsert} 等）只在"被调用的那一瞬间"知道失败原因，而本口自身那条通路
     * （{@code tickServer}）每 {@link #PERIOD} tick 才判一次，且"面朝物品管道"分支只写信息态
     * （{@link #REASON_PIPE}）—— 若不设有效期，管道刚把物品退回来的原因会被下一次信息态判定当场盖掉，
     * 界面退回中性的"面朝物品管道 / 一切正常"，玩家无从判断为什么不传输。
     * <p>
     * 窗口内<b>真实阻塞优先于信息态</b>；窗口过期后自然回到信息态（不会永久压住本口自身的判定）。
     * 只要第三方还在持续尝试并失败，窗口就会被不断刷新 ⇒ 显示稳定，不闪烁（写入口的节流相位也不参与本窗口）。
     */
    private static final long EXTERNAL_REASON_TTL_TICKS = 60;

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
    /**
     * 搬运失败原因（{@link #REASON_*}；追加槽，不重排既有 0~5 下标）。
     * <p>
     * 只报"为什么搬不动"这一类结论，具体文案由界面按 lang 渲染（服务器权威 → 客户端只读）。
     */
    public static final int DATA_REASON = 6;
    /**
     * 赤能源不足时「本批所需赤能源」（<b>追加槽</b>；只在 {@link #REASON_ENERGY_SHORT} 时有意义，其余写 0）。
     * <p>
     * 与 {@link #DATA_FEE_HAVE} 一起让"赤能源不足"可操作：界面悬停据此给出「需 X，缓冲 Y」，
     * 玩家才知道该给接入口补多少电，而不是只看到一句"不足"。数据槽经 short 传输 ⇒ 写入时钳到 16 位。
     */
    public static final int DATA_FEE_NEED = 7;
    /**
     * 赤能源不足时「终端当前缓冲赤能源」（<b>追加槽</b>）。
     * <p>
     * 口径 = 终端权威数据槽的缓冲读数（{@code IItemTerminalHost.DATA_BUFFER_*}），
     * 换算所需费用走 {@link ItemTerminalFee}（与终端 {@code tryChargeFee} 同一算式，不另立费率口径）。
     */
    public static final int DATA_FEE_HAVE = 8;
    public static final int DATA_SLOTS = 9;

    // ===== 失败原因码（DATA_REASON 的取值；只追加，既有码数值语义不动） =====
    /** 无异常（未绑定 / 已绑定可用 / 本批成功 / 对面容器只是空转） */
    public static final int REASON_NONE = 0;
    /** 绑定目标不存在或已失效（被拆 / 坍缩后再拆芯片 / 未加载 / 换了别的终端） */
    public static final int REASON_NO_TARGET = 1;
    /** 终端未成型（结构不完整：无储存单元，缓冲也不汇聚） */
    public static final int REASON_NOT_FORMED = 2;
    /** 终端已成型但没有可用储存单元（一件也存不下） */
    public static final int REASON_NO_UNITS = 3;
    /** 赤能源不足，本批整批跳过 */
    public static final int REASON_ENERGY_SHORT = 4;
    /**
     * 绑定身份缺少本口方向所需权限（输入口 INJECT / 输出口 EXTRACT）。
     * <p>
     * 与绑定用的 {@code BUILD}（布局）<b>是不同的权限位</b> ⇒ "有布局能绑、无存入/取出照旧不搬"，
     * 界面按本口方向选文案（缺「存入」/ 缺「取出」）。
     */
    public static final int REASON_PERMISSION = 5;
    /** 面朝处不是可读写的物品容器（没放容器 / 面向不对 / 对面区块未加载）——区别于"容器在但空"的正常空转 */
    public static final int REASON_NO_CONTAINER = 6;
    /** 终端一件也存不下（输入口方向：各储存单元可存量合计为 0） */
    public static final int REASON_TERMINAL_FULL = 7;
    /** 对面容器一件也塞不进（输出口方向：对面满仓 / 拒收） */
    public static final int REASON_TARGET_FULL = 8;
    /**
     * 面朝本模组<b>物品管道</b>（{@link AkaishiItemPipeBlockEntity}）——<b>合法信息态，不是错误</b>。
     * <p>
     * 物品由管道网络自动搬运（管道代表节点每 tick 直接在「源设备 → 汇设备」之间搬运，管道本身不存物），
     * 因此本口<b>自身</b>的抓取 / 推出在此不适用：既不必从管道取货，也无货可抓（管道无库存）。
     * 界面以<b>中性色</b>（非红字）呈现，避免玩家误以为配置错误。
     * <p>
     * <b>判据为何取具体类而非 {@code AkaishiPipeControl}</b>：后者由物品 / 赤能源 / 液体三类管道共同实现，
     * 若按它判定，面朝液体 / 能量管道也会被误判为"合法信息态"，而那两类并非物品容器，
     * 仍须按"面朝非物品容器"报错（通道对象三态：空气 / 物品管道 / 其它方块）。
     */
    public static final int REASON_PIPE = 9;
    /**
     * 过滤网不匹配（<b>过滤网已配置</b>）：外部塞进来的物品不在过滤网内，整堆退回。
     * <p>
     * 过滤网<b>全空 = 全通</b> ⇒ {@link #matches(ItemStack)} 恒 true ⇒ 本码<b>只可能</b>在过滤网已配置时出现，
     * "未配置过滤网"不会产生任何过滤相关提示。
     */
    public static final int REASON_FILTER_MISMATCH = 10;
    /**
     * 终端内没有匹配过滤网的物品（<b>过滤网已配置</b>且终端内确有物品）：输出口对外"无货可给"。
     * <p>
     * 与 {@link #REASON_FILTER_MISMATCH} 分成两个码的理由：<b>要去改的东西不同</b> ——
     * 前者让玩家去改"送来的物品 / 过滤网"，后者让他去改"终端库存 / 过滤网"；
     * 合成一个码会把人往错的方向支。
     */
    public static final int REASON_NO_MATCHING_STOCK = 11;


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
    /** 上一次搬运判定的失败原因（{@link #REASON_*}；界面据此给出显式提示） */
    private int reason;
    /**
     * 外部通路（管道 / 第三方物流）最近一次失败的原因码（{@link #REASON_*}；{@link #REASON_NONE} = 无失败）。
     * <p>
     * 与 {@link #reason} 分开存：它必须带<b>有效期</b>（见 {@link #EXTERNAL_REASON_TTL_TICKS}），
     * 才能在本口自身通路写"中性态"时把这条真实阻塞顶上去。纯运行期诊断值，<b>不落盘</b>（重启即清）。
     */
    private int externalReason = REASON_NONE;
    /** 上一条外部失败原因的记录时刻（游戏时间）；配 {@link #EXTERNAL_REASON_TTL_TICKS} 判是否仍在有效期内 */
    private long externalReasonTick;
    /** 上次"持续搬不动"WARN 的游戏时间（限流用；初值 -FAIL_LOG_INTERVAL_TICKS ⇒ 首次即打，不出负数溢出） */
    private long lastFailLogTick = -FAIL_LOG_INTERVAL_TICKS;
    /**
     * 上次「虚拟槽余量落地」WARN 的游戏时间（限流用，间隔同 {@link #FAIL_LOG_INTERVAL_TICKS}）。
     * <p>
     * 这条日志一旦出现即代表 {@link #canPlaceItem} 的整堆承诺与实际写入不一致（预检说收得下、
     * {@link #externalInsert} 却退了余量），是本轮重点排查对象，故必须可被发现但不得刷屏。
     */
    private long lastDropLogTick = -FAIL_LOG_INTERVAL_TICKS;
    /** 上次尝试结算的本批 IP（限流日志 need_ip 字段用；纯诊断，不参与任何判定） */
    private long lastAttemptIp;
    /** 上次「赤能源不足」时本批所需赤能源（{@link #DATA_FEE_NEED}；纯诊断，不改扣费语义） */
    private long feeNeed;
    /** 上次「赤能源不足」时终端缓冲赤能源（{@link #DATA_FEE_HAVE}；纯诊断） */
    private long feeHave;

    public AkaishiItemPortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean output) {
        super(type, pos, state);
        this.output = output;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiItemPortBlockEntity be) {
        be.tickServer();
        be.syncData();
    }

    /** 数据槽同步（界面只读）：方向 / 绑定态 / 上次搬运件数 / 绑定身份短号（32 位拆 2 槽）/ 失败原因 */
    private void syncData() {
        data.set(DATA_IS_OUTPUT, output ? 1 : 0);
        data.set(DATA_BOUND, isBound() ? 1 : 0);
        data.set(DATA_LAST_MOVED, lastMoved);
        data.set(DATA_ENERGY_SHORT, energyShort ? 1 : 0);
        data.set(DATA_REASON, reason);
        // 费用数字只在「赤能源不足」这一态下有意义：其余写 0，免得界面把上次的旧数字当成现状
        boolean feeShort = reason == REASON_ENERGY_SHORT;
        data.set(DATA_FEE_NEED, feeShort ? shortSlot(feeNeed) : 0);
        data.set(DATA_FEE_HAVE, feeShort ? shortSlot(feeHave) : 0);
        LongDataSlots.writeInt(data, DATA_IDENTITY_HASH, DATA_IDENTITY_HASH_HIGH,
                boundIdentity == null ? 0 : shortHash(boundIdentity));
    }

    /** 数据槽经 short 传输（每槽仅低 16 位有效）：诊断数字钳到 16 位无符号范围，不产生脏值 */
    private static int shortSlot(long value) {
        return (int) Math.max(0L, Math.min(0xFFFFL, value));
    }

    /** 界面数据容器 */
    public ContainerData data() {
        return data;
    }

    /** UUID 前 4 字节（高位 32 bit）：界面 8 位 hex 短号，与身份卡短号一致 */
    private static int shortHash(UUID id) {
        return (int) (id.getMostSignificantBits() >>> 32);
    }

    /** 无事可做：清零运行页计数与告警，并记下失败原因（{@link #REASON_NONE} = 无异常） */
    private void idle(int why) {
        lastMoved = 0;
        energyShort = false;
        noteReason(why);
    }

    /**
     * 记录本口<b>自身通路</b>（{@code tickServer}）的判定结论；非中性态时按
     * {@link #FAIL_LOG_INTERVAL_TICKS} 限流打一条 WARN。
     * <p>
     * <b>真实阻塞优先于信息态</b>：若外部通路（管道 / 第三方物流）刚失败过且仍在
     * {@link #EXTERNAL_REASON_TTL_TICKS} 有效期内，<b>且</b>本次本口结论是中性态
     * （{@link #REASON_NONE} 无异常 / {@link #REASON_PIPE} 面朝物品管道），则显示外部那条失败原因 ——
     * 否则会出现"管道刚把物品退回来、界面却显示一切正常 / 面朝物品管道"的组合（本轮要修的诊断缺口）。
     * <p>
     * 反之，本口自己的<b>真实错误</b>不被外部原因顶替：否则一条与面朝对象无关的外部失败
     * 会永久压住"面朝处是空气 / 终端未成型"这类更该先修的错误 —— 那正是同一类遮蔽病。
     */
    private void noteReason(int why) {
        boolean externalWins = externalReasonLive() && isNeutralState(why);
        reason = externalWins ? externalReason : why;
        if (reason != REASON_NONE && reason != REASON_PIPE) {
            warnThrottled(reason, externalWins ? "external" : "tick");
        }
    }

    /** 中性态：无异常（{@link #REASON_NONE}）或合法信息态（{@link #REASON_PIPE}）—— 都可被外部失败原因顶替 */
    private static boolean isNeutralState(int why) {
        return why == REASON_NONE || why == REASON_PIPE;
    }

    /** 外部失败原因是否仍在有效期内（过期即让位给本口自身通路的判定） */
    private boolean externalReasonLive() {
        return externalReason != REASON_NONE && level != null
                && level.getGameTime() - externalReasonTick <= EXTERNAL_REASON_TTL_TICKS;
    }

    /**
     * 记录<b>外部通路</b>（管道 / 第三方物流）的失败原因：立即上屏 + 刷新有效期 + 限流 WARN。
     * <p>
     * 与 {@link #noteReason(int)} 分开，是因为它还要维护<b>有效期</b>（见 {@link #EXTERNAL_REASON_TTL_TICKS}）。
     * 成功（{@link #REASON_NONE}）只关闭窗口、<b>不</b>改写当前显示值 —— 免得第三方一次成功探测
     * 就把本口自身通路的结论一起抹掉。
     * <p>
     * <b>为什么四个外部入口都要调它</b>：第三方物流的"被拒"未必发生在真正的读写处 ——
     * 管道会先问 {@code canPlaceItem} / 读 {@code getItem} 做预检（{@code AkaishiItemPipeBlockEntity}
     * 的 {@code insertIntoSlots} / {@code transferFromSource}），预检不过就<b>根本不会</b>调写入 / 抽取，
     * 只挂在 {@link #externalInsert}/{@link #externalExtract} 上会漏掉绝大多数静默失败。
     * 本类是"只有虚拟转发槽、不存物"的接口面，这些预检即真实闸门。
     */
    private void noteExternalReason(int why) {
        externalReason = why;
        externalReasonTick = level == null ? 0L : level.getGameTime();
        if (why != REASON_NONE) {
            reason = why;
            warnThrottled(why, "external");
        }
    }

    /**
     * 持续搬不动日志（限流）：同一台口每 {@link #FAIL_LOG_INTERVAL_TICKS} tick 最多一条。
     * <p>
     * 打的是<b>闸门快照</b>而非单码：绑定态 / 绑定目标短号 / 目标能否解析 / 是否成型 / 储存单元数 /
     * 方向权限 / 过滤网是否已配置 / 本批所需 IP 与终端单笔上限 / 终端当前缓冲 / 本批所需赤能源，
     * 连同触发来源（{@code tick} 面朝通路 / {@code external} 管道等第三方通路）—— 一条日志即可定位
     * "为什么一件都不动"，不必再逐项猜。全部为只读量，不影响任何搬运判定。
     *
     * @param via 触发来源（{@code tick} 本口自身通路 / {@code external} 管道等第三方通路），便于远程区分
     */
    private void warnThrottled(int why, String via) {
        if (level == null || level.isClientSide) {
            return;
        }
        long now = level.getGameTime();
        if (now - lastFailLogTick < FAIL_LOG_INTERVAL_TICKS) {
            return;
        }
        lastFailLogTick = now;
        IItemTerminalHost terminal = resolveTerminal();
        boolean deposit = !output;
        AkaishiSecurityPermission required = output
                ? AkaishiSecurityPermission.EXTRACT : AkaishiSecurityPermission.INJECT;
        long buffer = terminal == null ? -1L
                : LongDataSlots.read(terminal.data(), IItemTerminalHost.DATA_BUFFER_LOW,
                        IItemTerminalHost.DATA_BUFFER_HIGH, IItemTerminalHost.DATA_BUFFER_HIGH2,
                        IItemTerminalHost.DATA_BUFFER_HIGH3);
        LOGGER.warn("[akaishi] item port {} {} stuck: facing={} reason={} via={} bound={} bound_ok={} "
                        + "terminal={} formed={} units={} dir_ok={} filter_set={} "
                        + "need_ip={} cap_ip={} buffer={} fee_need={} fee_have={}",
                output ? "OUTPUT" : "INPUT", worldPosition, facingDirection(), reasonName(why), via,
                ItemTerminalRegistry.shortId(boundTerminalId), isBound(),
                terminal != null,
                terminal != null && terminal.isFormed(),
                terminal == null ? -1 : terminal.storageUnits().size(),
                terminal != null && boundIdentity != null && terminal.security().check(boundIdentity, required),
                filterConfigured(),
                lastAttemptIp,
                terminal == null ? -1L : terminal.maxBatchIp(deposit),
                buffer, feeNeed, feeHave);
    }

    /** 原因码名（日志用 ASCII 短名；界面文案另走 lang） */
    private static String reasonName(int why) {
        return switch (why) {
            case REASON_NO_TARGET -> "no_target";
            case REASON_NOT_FORMED -> "not_formed";
            case REASON_NO_UNITS -> "no_units";
            case REASON_ENERGY_SHORT -> "energy_short";
            case REASON_PERMISSION -> "permission";
            case REASON_NO_CONTAINER -> "no_container";
            case REASON_TERMINAL_FULL -> "terminal_full";
            case REASON_TARGET_FULL -> "target_full";
            case REASON_PIPE -> "pipe";
            case REASON_FILTER_MISMATCH -> "filter_mismatch";
            case REASON_NO_MATCHING_STOCK -> "no_matching_stock";
            default -> "none";
        };
    }

    /** 搬运结果 → 数据槽原因码（费用闸门拒绝统一归"赤能源不足"，与 DATA_ENERGY_SHORT 同口径） */
    private static int mapReason(ItemPortTransfer.Reason why) {
        return switch (why) {
            case NO_CONTAINER -> REASON_NO_CONTAINER;
            case TERMINAL_FULL -> REASON_TERMINAL_FULL;
            case TARGET_FULL -> REASON_TARGET_FULL;
            case FEE_DENIED -> REASON_ENERGY_SHORT;
            case NONE -> REASON_NONE;
        };
    }

    private void tickServer() {
        if (boundTerminalId == null || boundIdentity == null) {
            idle(REASON_NONE);
            return;
        }
        // 节流：分周期搬运（错开不同端口的相位，避免同一 tick 全服集中扫描）
        // 非本口相位不做任何判定，保留上一次的原因（最坏滞后一个 PERIOD，界面不会闪）
        if ((level.getGameTime() + worldPosition.asLong()) % PERIOD != 0) {
            return;
        }
        IItemTerminalHost terminal = resolveTerminal();
        if (terminal == null) {
            // 绑定目标已失效（被拆 / 坍缩后再拆芯片 / 未加载 / 坐标被别的终端顶替）
            idle(REASON_NO_TARGET);
            return;
        }
        // 顺带刷新「已绑定终端」标签：终端搬动后 GUI 显示的位置随之更新（离线则保留旧标签）
        refreshBoundLabel();
        // 结构未成型：一件也搬不动（储存单元列表为空、能源口也不会汇聚），显式报"未成型"
        if (!terminal.isFormed()) {
            idle(REASON_NOT_FORMED);
            return;
        }
        if (terminal.storageUnits().isEmpty()) {
            idle(REASON_NO_UNITS);
            return;
        }
        // 方向权限：输入口=存入(INJECT)、输出口=取出(EXTRACT)；按绑定身份判定。
        // 注意与绑定用的 BUILD 是不同权限位 ⇒ 有布局能绑、无存入/取出照旧不搬，故必须显式报"权限不足"
        AkaishiSecurityPermission required = output
                ? AkaishiSecurityPermission.EXTRACT : AkaishiSecurityPermission.INJECT;
        if (!terminal.security().check(boundIdentity, required)) {
            idle(REASON_PERMISSION);
            return;
        }
        Direction facing = facingDirection();
        if (facing == null) {
            idle(REASON_NONE);
            return;
        }
        // 面朝容器在本口所在维度：传本口自己的 ServerLevel（终端绑定改用 ID 后可跨维度绑定，
        // 若误传终端所在维度，跨维度时会在错误维度里找面朝容器）
        if (!(level instanceof ServerLevel ownLevel)) {
            idle(REASON_NONE);
            return;
        }
        // 面朝本模组物品管道：合法信息态（物品由管道网络自动存取，本口的抓取 / 推出在此不适用）。
        // 必须在调用搬运之前拦下：管道既非 Forge IItemHandler 也无原版 Container，硬搬只会得到
        // "面朝处没有容器"的假错误（红字），而实际是设计不支持从管道取货（管道无库存）。
        if (level.getBlockEntity(worldPosition.relative(facing)) instanceof AkaishiItemPipeBlockEntity) {
            idle(REASON_PIPE);
            return;
        }
        // 过滤网（AE2 总线配置槽口径）：输入口只抓匹配物、输出口只推匹配物；全空 = 全通
        // 费用闸门：输入口=存入费率、输出口=取出费率（与物品终端 GUI 存取同一套算法，不足则整批跳过）
        energyShort = false;
        ItemPortTransfer.Result result = output
                ? ItemPortTransfer.pushOut(ownLevel, worldPosition, facing, terminal, this::matches,
                        (unit, slot, stored, batch) -> chargeWithdraw(terminal, unit, slot, stored, batch))
                : ItemPortTransfer.pullInto(ownLevel, worldPosition, facing, terminal, this::matches,
                        batch -> chargeDeposit(terminal, batch));
        lastMoved = result.moved();
        // 原因以闸门侧效应（赤能源不足）优先：费用回调在返回 false 时已置 energyShort
        noteReason(energyShort ? REASON_ENERGY_SHORT : mapReason(result.reason()));
    }

    // ===== 费用闸门（口径统一来自既有存取路径，本类只做「算 IP → tryChargeFee」） =====

    /**
     * 输入口：存入费率。IP = 单件占用 × 本批件数，算法同库页存入（{@code ItemPoints.perItem} 折算、
     * 基准 4 IP = 1 赤能源，减免并入 {@code tryChargeFee} 内部）。
     */
    private boolean chargeDeposit(IItemTerminalHost terminal, ItemStack batch) {
        long ip = ItemPoints.of(batch);
        lastAttemptIp = ip;
        if (ip <= 0L || terminal.tryChargeFee(ip, true)) {
            return true;
        }
        energyShort = true;
        reason = REASON_ENERGY_SHORT;
        noteFeeShortfall(terminal, ip, true);
        // 终端侧回执：本口真正尝试写入却被拒 ⇒ 让终端自己的界面也说得出来（纯回执，不改任何行为）
        terminal.noteReject(IItemTerminalHost.REJECT_DEPOSIT_ENERGY_SHORT, feeNeed);
        return false;
    }

    /**
     * 输出口：取出费率。IP 取<b>单元账本</b>（{@code TerminalActions.withdrawIp}，与库页取出同一方法），
     * 不重算价值表，故与 GUI 取出费用严格一致。
     * <p>
     * 件数须传<b>真实件数</b>（{@link IItemStorageUnit#storedCount(int)}）：一槽多堆下视图堆数量
     * 恒定夹到单堆上限，拿它当分母会把"取 64 / 槽内 5900"算成"整槽取空"而多扣费。
     */
    private boolean chargeWithdraw(IItemTerminalHost terminal, IItemStorageUnit unit,
            int slot, ItemStack stored, ItemStack batch) {
        long ip = TerminalActions.withdrawIp(unit, slot, unit.storedCount(slot), batch.getCount());
        lastAttemptIp = ip;
        if (ip <= 0L || terminal.tryChargeFee(ip, false)) {
            return true;
        }
        energyShort = true;
        reason = REASON_ENERGY_SHORT;
        noteFeeShortfall(terminal, ip, false);
        // 终端侧回执：本口真正尝试取出却被拒 ⇒ 让终端自己的界面也说得出来（纯回执，不改任何行为）
        terminal.noteReject(IItemTerminalHost.REJECT_WITHDRAW_ENERGY_SHORT, feeNeed);
        return false;
    }

    /**
     * 记下「赤能源不足」的<b>两个数字</b>（界面悬停 + 限流日志用）：本批所需赤能源与终端当前缓冲。
     * <p>
     * 全为<b>只读快照</b>：费率减免份数 / 缓冲都取终端权威数据槽，换算走 {@link ItemTerminalFee}
     * （与终端自己的 {@code tryChargeFee} 同一算式）⇒ 不引入第二套费率口径，也不改任何扣费语义。
     * <p>
     * <b>调用时点</b>：必须在写原因（{@link #noteExternalReason(int)} / {@link #noteReason(int)}）<b>之前</b>，
     * 否则那条限流日志里的 fee_need/fee_have 还是上一批的旧值。
     */
    private void noteFeeShortfall(IItemTerminalHost terminal, long ip, boolean deposit) {
        lastAttemptIp = ip;
        int modules = terminal.data().get(IItemTerminalHost.DATA_FEE_MODULES);
        feeNeed = deposit ? ItemTerminalFee.depositCost(ip, modules) : ItemTerminalFee.withdrawCost(ip, modules);
        feeHave = LongDataSlots.read(terminal.data(), IItemTerminalHost.DATA_BUFFER_LOW,
                IItemTerminalHost.DATA_BUFFER_HIGH, IItemTerminalHost.DATA_BUFFER_HIGH2,
                IItemTerminalHost.DATA_BUFFER_HIGH3);
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
        // 万一被不守契约的调用方直接写入，余量落地而不是凭空消失。
        // 落点取端口方块上方（可达、不嵌在方块内部），并按间隔限流 WARN（落地=预检与实写不一致）。
        long now = level.getGameTime();
        if (now - lastDropLogTick >= FAIL_LOG_INTERVAL_TICKS) {
            lastDropLogTick = now;
            LOGGER.warn("[akaishi] item port {} {} virtual-slot leftover dropped: port={} facing={} "
                            + "item={} count={} via=setItem",
                    output ? "OUTPUT" : "INPUT", worldPosition, worldPosition, facingDirection(),
                    leftover.getItem().builtInRegistryHolder().key().location(), leftover.getCount());
        }
        net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, leftover);
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
     * 输出口一律原样退回（不允许反向塞）。未绑定 / 终端离线未加载 / 未成型 / 无单元 / 无 INJECT 权限 /
     * 不匹配过滤 / 终端存不下 / 赤能源不足 全部原堆退回（不吞物），并把原因写入原因槽
     * （{@link #noteExternalReason(int)}，界面据此自证为什么不传输）。
     * <p>
     * 返回值语义（含 simulate）与原实现<b>逐位一致</b>：本次改动只增加"顺带记一条原因"，
     * 不动任何夹量 / 扣费 / 落账 / 退回逻辑（计费与物品守恒口径未变）。
     */
    public ItemStack externalInsert(ItemStack stack, boolean simulate) {
        if (output || stack == null || stack.isEmpty()) {
            return stack == null ? ItemStack.EMPTY : stack;
        }
        IItemTerminalHost terminal = externalTerminal(AkaishiSecurityPermission.INJECT);
        if (terminal == null) {
            return stack;
        }
        if (!matches(stack)) {
            noteExternalReason(REASON_FILTER_MISMATCH);
            return stack;
        }
        if (simulate) {
            int accept = ItemPortTransfer.acceptLimit(terminal, stack, null);
            noteExternalReason(accept <= 0 ? REASON_TERMINAL_FULL : REASON_NONE);
            return stack.copyWithCount(stack.getCount() - accept);
        }
        energyShort = false;
        ItemStack leftover = ItemPortTransfer.insertIntoTerminal(terminal, stack, null,
                batch -> chargeDeposit(terminal, batch));
        int moved = stack.getCount() - leftover.getCount();
        if (moved > 0) {
            lastMoved = moved;
        }
        // 一件没搬才报原因：费用闸门拒绝置 energyShort（chargeDeposit），否则只能是终端存不下；
        // 搬动了即便有余量也不算失败（不算"搬不动"），保持与"上次搬运：N 件"不矛盾
        noteExternalReason(moved > 0 ? REASON_NONE
                : energyShort ? REASON_ENERGY_SHORT : REASON_TERMINAL_FULL);
        return leftover;
    }

    /**
     * 外部抽取（第三方 {@code extractItem} / 管道抽取）：输出口从绑定终端取出（单次 ≤1 组），
     * 返回<b>实抽出的堆</b>；输入口一律返回空堆（不允许反向抽）。未绑定 / 终端离线未加载 / 未成型 /
     * 无单元 / 无 EXTRACT 权限 / 费用不足 / 终端内无匹配物 一律返回空堆，物品与账本零改动，
     * 并把原因写入原因槽（{@link #noteExternalReason(int)}）。
     *
     * @param simulate 只探测"能给出什么"（不扣费、不落账），返回值语义与原实现逐位一致
     */
    public ItemStack externalExtract(int amount, boolean simulate) {
        if (!output || amount <= 0) {
            return ItemStack.EMPTY;
        }
        IItemTerminalHost terminal = externalTerminal(AkaishiSecurityPermission.EXTRACT);
        if (terminal == null) {
            return ItemStack.EMPTY;
        }
        int want = Math.min(amount, ItemPortTransfer.BATCH_LIMIT);
        boolean anyStored = false;
        for (IItemStorageUnit unit : terminal.storageUnits()) {
            int slots = unit.slots();
            for (int slot = 0; slot < slots; slot++) {
                ItemStack stored = unit.getItem(slot);
                if (stored.isEmpty()) {
                    continue;
                }
                anyStored = true; // 终端里确有物品；用于把"空转"与"都被过滤网挡住"分开报
                if (!matches(stored)) {
                    continue;
                }
                // 一槽多堆：单次仍只抽 1 组（BATCH_LIMIT），件数用真实件数夹量
                int take = (int) Math.min(want, unit.storedCount(slot));
                if (simulate) {
                    noteExternalReason(REASON_NONE);
                    return stored.copyWithCount(take);
                }
                energyShort = false;
                if (!chargeWithdraw(terminal, unit, slot, stored, stored.copyWithCount(take))) {
                    noteExternalReason(REASON_ENERGY_SHORT); // 费用不足：整笔拒绝，物品与账本零改动
                    return ItemStack.EMPTY;
                }
                ItemStack extracted = unit.extract(slot, take);
                if (extracted.getCount() > 0) {
                    lastMoved = extracted.getCount();
                }
                noteExternalReason(REASON_NONE);
                return extracted;
            }
        }
        // 扫完没东西可给：终端本来就空（正常空转，不报红）与"有货但都被过滤网挡住"是两件事
        noteExternalReason(anyStored && filterConfigured() ? REASON_NO_MATCHING_STOCK : REASON_NONE);
        return ItemStack.EMPTY;
    }

    /**
     * 外部观察：输出口 = 当前会推出的堆（副本，供管道读取）；输入口 = 空（本口不存物）。
     * <p>
     * <b>本方法也是一个真实闸门</b>：物品管道取货前先读本槽（{@code AkaishiItemPipeBlockEntity}
     * 的 {@code transferFromSource} 首行），读到空就直接跳过、<b>根本不会调</b> {@link #externalExtract} ——
     * 所以"为什么输出口对外没货"必须在这里就能报出来（终端失效 / 未成型 / 无单元 / 无权限 / 无匹配物）。
     */
    public ItemStack externalPreview() {
        if (!output) {
            return ItemStack.EMPTY;
        }
        IItemTerminalHost terminal = externalTerminal(AkaishiSecurityPermission.EXTRACT);
        if (terminal == null) {
            return ItemStack.EMPTY;
        }
        boolean anyStored = false;
        for (IItemStorageUnit unit : terminal.storageUnits()) {
            int slots = unit.slots();
            for (int slot = 0; slot < slots; slot++) {
                ItemStack stored = unit.getItem(slot);
                if (stored.isEmpty()) {
                    continue;
                }
                anyStored = true;
                if (matches(stored)) {
                    noteExternalReason(REASON_NONE);
                    return stored.copy();
                }
            }
        }
        noteExternalReason(anyStored && filterConfigured() ? REASON_NO_MATCHING_STOCK : REASON_NONE);
        return ItemStack.EMPTY;
    }

    /**
     * 外部可接收预检（容器契约的 {@code canPlaceItem}）：绑定 + INJECT 权限 + 过滤 + 终端空间
     * + 费用<b>全部可行</b>才为 true —— 先承诺"整堆收得下"，写入端才会调 {@link #setItem}，
     * 因此不会出现"承诺了却只收下一部分"的丢物。
     * <p>
     * <b>本方法也是管道塞入的真实闸门</b>：管道插入前必问 {@code canPlaceItem}，问了不过就整批跳过、
     * <b>根本不会调</b> {@link #externalInsert} —— 历史上于是整条路静默无提示。现在失败原因在此写入原因槽，
     * 界面即可自证"管道为什么塞不进来"。判定仍为纯查询（零副作用），未改任何承诺语义。
     */
    public boolean externalCanAccept(ItemStack stack) {
        if (output || stack == null || stack.isEmpty()) {
            return false;
        }
        IItemTerminalHost terminal = externalTerminal(AkaishiSecurityPermission.INJECT);
        if (terminal == null) {
            return false;
        }
        if (!matches(stack)) {
            noteExternalReason(REASON_FILTER_MISMATCH);
            return false;
        }
        int batch = Math.min(stack.getCount(), ItemPortTransfer.BATCH_LIMIT);
        ItemStack probe = stack.copyWithCount(batch);
        if (ItemPortTransfer.acceptLimit(terminal, probe, null) < batch) {
            noteExternalReason(REASON_TERMINAL_FULL); // 无法承诺整堆收下（含"剩余空间 = 0"）
            return false;
        }
        long ip = ItemPoints.of(probe);
        if (!terminal.canAffordFee(ip, true)) {
            noteFeeShortfall(terminal, ip, true); // 先记数字，再写原因（限流日志要用它）
            noteExternalReason(REASON_ENERGY_SHORT);
            return false;
        }
        noteExternalReason(REASON_NONE);
        return true;
    }

    /** 绑定终端解析：未绑定 / 终端离线（未注册 / 区块未加载 / 已被拆）⇒ null（拒绝一切外部操作） */
    private IItemTerminalHost resolveTerminal() {
        if (level == null || level.isClientSide || level.getServer() == null
                || boundTerminalId == null || boundIdentity == null) {
            return null;
        }
        return ItemTerminalRegistry.resolve(level.getServer(), boundTerminalId);
    }

    /**
     * 外部通路（管道 / 第三方物流）的<b>公共前置闸门</b>：绑定 → 目标 → 未成型 → 无储存单元 → 方向权限。
     * <p>
     * 与 {@code tickServer} <b>同口径、同顺序</b>；任一项不过即把对应原因写入原因槽
     * （{@link #noteExternalReason(int)}）并返回 null，全部通过才返回终端。
     * <p>
     * <b>未绑定不报原因</b>：界面运行页已显示「已绑定终端：----」，此时没有"去改什么"可指，
     * 写原因只会与未绑定状态重复（判定表单列这一行）。
     */
    private IItemTerminalHost externalTerminal(AkaishiSecurityPermission required) {
        if (!isBound()) {
            return null;
        }
        IItemTerminalHost terminal = resolveTerminal();
        if (terminal == null) {
            noteExternalReason(REASON_NO_TARGET);
            return null;
        }
        if (!terminal.isFormed()) {
            noteExternalReason(REASON_NOT_FORMED);
            return null;
        }
        if (terminal.storageUnits().isEmpty()) {
            noteExternalReason(REASON_NO_UNITS);
            return null;
        }
        if (!terminal.security().check(boundIdentity, required)) {
            noteExternalReason(REASON_PERMISSION);
            return null;
        }
        return terminal;
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
     * <p>
     * 语义未变（等价改写）："未配置 = 全通"由 {@link #filterConfigured()} 先判，
     * 使"是否需要报过滤相关原因"能复用同一个判据（未配置时永不产生过滤提示，见
     * {@link #REASON_FILTER_MISMATCH} / {@link #REASON_NO_MATCHING_STOCK}）。
     */
    public boolean matches(ItemStack stack) {
        if (!filterConfigured()) {
            return true;
        }
        for (ItemStack slot : filter) {
            if (!slot.isEmpty() && !stack.isEmpty() && ItemStack.isSameItemSameTags(slot, stack)) {
                return true;
            }
        }
        return false;
    }

    /** 过滤网是否已配置（任一槽非空）；<b>未配置 = 全通</b>，此时不产生任何过滤相关提示 */
    private boolean filterConfigured() {
        for (ItemStack slot : filter) {
            if (!slot.isEmpty()) {
                return true;
            }
        }
        return false;
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
            clearExternalReason(); // 换绑后旧的对外失败原因不再成立（否则会顶替新目标的判定）
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
            clearExternalReason();
            setChanged();
        }
    }

    /** 清除外部通路失败原因（解绑 / 换绑时调用：旧原因对新绑定不再成立） */
    private void clearExternalReason() {
        externalReason = REASON_NONE;
        externalReasonTick = 0L;
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
     * 刷新已绑定终端标签（「名称 · 短号 · 维度 x,y,z」；终端在线才更新，离线保留旧值）。
     * <p>
     * 位置来自注册表（终端上报的心跳），因此终端搬走后标签会跟着变；名称同样取自心跳上报的显示名。
     * <b>注意</b>：这只是"最后已知"的显示串（NBT 持久化，供聊天提示与兜底），
     * "这条绑定现在还成不成立"由注册表实时判定并经数据槽 / 快照下发，别把本串当成绑定有效性。
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
        String name = snapshot.name() == null ? "" : snapshot.name().getString();
        String next = (name.isEmpty() ? "" : name + " · ") + snapshot.shortId() + " · "
                + AkaishiItemPortBindingSync.label(snapshot.dimension().location(), snapshot.pos());
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

    /** 搬运失败原因（{@link #REASON_*}；经追加槽 {@link #DATA_REASON} 同步给界面） */
    public int reason() {
        return reason;
    }

    /** 上次「赤能源不足」时本批所需赤能源（{@link #DATA_FEE_NEED}；仅诊断/界面悬停用） */
    public long feeNeed() {
        return feeNeed;
    }

    /** 上次「赤能源不足」时终端缓冲赤能源（{@link #DATA_FEE_HAVE}；仅诊断/界面悬停用） */
    public long feeHave() {
        return feeHave;
    }

    public boolean isOutput() {
        return output;
    }

    /**
     * 口朝向（方块状态的 {@code FACING}）—— 方向语义唯一真源，界面据此显示实际朝向。
     * <p>
     * 客户端读的是<b>已同步的本地方块状态</b>（同区块数据下发，与服务端一致），
     * 因此显示朝向<b>无需新增同步槽 / 修改协议</b>。
     *
     * @return 朝向；方块状态缺失时返回 null
     */
    public Direction facingDirection() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockStateProperties.FACING)
                ? state.getValue(BlockStateProperties.FACING) : null;
    }

    /**
     * 面朝处的方块状态（读不到返回 null）：界面据此把"面朝非容器"细分为"空气 / 具体是哪个方块"。
     * 纯读操作，服务端 / 客户端皆可调用。
     */
    public BlockState facingState() {
        Direction facing = facingDirection();
        if (facing == null || level == null) {
            return null;
        }
        return level.getBlockState(worldPosition.relative(facing));
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
