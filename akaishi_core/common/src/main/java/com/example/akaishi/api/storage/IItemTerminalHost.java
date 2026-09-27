package com.example.akaishi.api.storage;

import java.util.List;
import java.util.UUID;

import com.example.akaishi.wireless.TerminalSecurity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.ContainerData;

/**
 * 物品终端宿主契约：库页菜单与界面对终端侧数据 / 行为的最小依赖面（DIP）。
 * <p>
 * 菜单只经本接口取数（数据槽读数、权限表、库内容版本、单元视图）与落账（费用结算），
 * 不再绑定方块实体类型 —— 微缩件等非方块实体宿主实现本接口即可直接复用整套界面。
 * <p>
 * 数据槽下标常量随接口一同下沉，实现方按同一版式填充 {@link #data()}。
 */
public interface IItemTerminalHost {

    // ===== 数据槽（long 一律拆 4 槽，防 2^31 截断） =====

    int DATA_FORMED = 0;
    int DATA_USED_LOW = 1;
    int DATA_USED_HIGH = 2;
    int DATA_USED_HIGH2 = 3;
    int DATA_USED_HIGH3 = 4;
    int DATA_CAPACITY_LOW = 5;
    int DATA_CAPACITY_HIGH = 6;
    int DATA_CAPACITY_HIGH2 = 7;
    int DATA_CAPACITY_HIGH3 = 8;
    int DATA_BUFFER_LOW = 9;
    int DATA_BUFFER_HIGH = 10;
    int DATA_BUFFER_HIGH2 = 11;
    int DATA_BUFFER_HIGH3 = 12;
    /** 已贴装并联的储存单元数量 */
    int DATA_UNIT_COUNT = 13;
    /** 有效赤能源缓冲容量（配置基准 + 缓冲扩展组件加成）：客户端据此显示正确的单笔上限 */
    int DATA_EFFECTIVE_BUFFER_LOW = 14;
    int DATA_EFFECTIVE_BUFFER_HIGH = 15;
    int DATA_EFFECTIVE_BUFFER_HIGH2 = 16;
    int DATA_EFFECTIVE_BUFFER_HIGH3 = 17;
    /** 生效的费率减免份数（0 ~ ItemTerminalFee.FEE_MODULE_MAX） */
    int DATA_FEE_MODULES = 18;

    // ===== 最近一次存取被拒绝的原因（以下全为追加槽，既有 0~18 下标一律未动） =====

    /** 最近一次存取被拒绝的原因（{@link #REJECT_*}）；{@link #REJECT_NONE} = 无失败 */
    int DATA_REJECT_REASON = 19;
    /** 被拒绝那一笔的「本批所需赤能源」低 16 位（只在赤能源不足 / 超单笔上限时有意义，其余写 0） */
    int DATA_REJECT_FEE_NEED_LOW = 20;
    /** 被拒绝那一笔的「本批所需赤能源」高 16 位 */
    int DATA_REJECT_FEE_NEED_HIGH = 21;
    /**
     * 已占用槽位合计（各单元非空槽；服务端权威）。
     * <p>
     * 与 IP 用量<b>同为累计型约束的用量</b>，必须一起上屏：可存量取「槽位余量」与「IP 余量」的较小者，
     * 只显示 IP 会让"IP 富余但槽位已满"在界面上完全看不出来（本项目曾因此连续两轮误判方向）。
     */
    int DATA_USED_SLOTS = 22;
    /** 槽位总数合计（各单元槽位数之和），与 {@link #DATA_USED_SLOTS} 配对展示 */
    int DATA_TOTAL_SLOTS = 23;
    int DATA_SLOTS = 24;

    // ===== 终端侧拒绝原因码（DATA_REJECT_REASON 取值；只追加，与储存口的 REASON_* 是两套独立命名空间） =====

    /** 无失败（本笔成功，或尚未发生过存取） */
    int REJECT_NONE = 0;
    /** 存入：结构未成型 */
    int REJECT_DEPOSIT_UNFORMED = 1;
    /** 存入：没有任何储存单元 */
    int REJECT_DEPOSIT_NO_UNIT = 2;
    /** 存入：各单元容量与空位合计放不下本批（两个约束都还有余量却凑不出一件的兜底码） */
    int REJECT_DEPOSIT_NO_SPACE = 3;
    /** 存入：赤能源缓冲不够付本批手续费 */
    int REJECT_DEPOSIT_ENERGY_SHORT = 4;
    /** 存入：本批占用量超过单笔 IP 上限（缓冲容量所决定的能量上限） */
    int REJECT_DEPOSIT_MAX_IP = 5;
    /** 存入：操作者缺少「存入」权限 */
    int REJECT_DEPOSIT_PERMISSION = 6;
    /** 存入：光标手持物与点中的条目不是同一件（含 NBT）——历史上这里是静默出口 */
    int REJECT_DEPOSIT_MISMATCH = 7;
    /**
     * 存入：<b>槽位</b>已占满（各单元既无空槽、也没有该物品的半堆余量），而 IP 仍有余量。
     * <p>
     * 与 {@link #REJECT_DEPOSIT_IP_FULL} 分开是因为<b>处置方式完全不同</b>：本码要"腾槽 / 加单元"，
     * 那个码要"释放 IP / 换更大容量档"。合成一个笼统的"无空间"会把人支到错的方向
     * （历史上界面只显示 IP ⇒ 玩家看到 1.8M/128M 必然认为还有空间）。
     */
    int REJECT_DEPOSIT_SLOTS_FULL = 8;
    /** 存入：<b>IP 容量</b>余量已不足一件（各单元剩余 IP / 单件 IP 皆为 0），而槽位仍有余量 */
    int REJECT_DEPOSIT_IP_FULL = 9;
    /** 取出：结构未成型 */
    int REJECT_WITHDRAW_UNFORMED = 10;
    /** 取出：没有任何储存单元 */
    int REJECT_WITHDRAW_NO_UNIT = 11;
    /** 取出：赤能源缓冲不够付本笔取出费 */
    int REJECT_WITHDRAW_ENERGY_SHORT = 12;
    /** 取出：操作者缺少「取出」权限 */
    int REJECT_WITHDRAW_PERMISSION = 13;
    /** 取出：库里已没有这件物品 */
    int REJECT_WITHDRAW_NO_ITEM = 14;
    /** 取出：玩家背包放不下 */
    int REJECT_WITHDRAW_NO_ROOM = 15;

    /** 同步给菜单的数据槽（服务端写入，客户端只读） */
    ContainerData data();

    /** 终端唯一 ID：端口 / 无线设备按它寻址（微缩前后不变，故绑定不失效） */
    UUID terminalId();

    /** 安全状态（归属者 + 权限表） */
    TerminalSecurity security();

    /** 结构是否已成型（未成型不接受存取） */
    boolean isFormed();

    /** 库内容版本号：指纹变化即自增，客户端据此判断条目快照是否过期 */
    int contentRevision();

    /** 存活储存单元（只读契约视图）：库页聚合与落账的唯一入口 */
    List<IItemStorageUnit> storageUnits();

    /** 单笔可处理的最大 IP（能量侧约束，已并入费率减免） */
    long maxBatchIp(boolean deposit);

    /**
     * 结算一笔一次性存取费用（原子预检，禁止部分扣费）。
     *
     * @param ip      本笔涉及的 IP 量
     * @param deposit true = 存入费率，false = 取出费率
     * @return true 表示费用已扣除，可继续写账本
     */
    boolean tryChargeFee(long ip, boolean deposit);

    /**
     * 非破坏性费用预检：这笔费用现在付得起吗？
     * <p>
     * 口径必须与 {@link #tryChargeFee} 严格一致，但<b>不扣费</b>：供外部物流能力在
     * {@code simulate} / {@code canPlaceItem} 阶段先给出"整堆收得下"的承诺，
     * 避免"承诺了却付不起"造成丢物。
     */
    boolean canAffordFee(long ip, boolean deposit);

    /**
     * 记录一次「存取被拒绝」的原因，供终端自己的界面显示（见 {@link #DATA_REJECT_REASON}）。
     * <p>
     * <b>纯通知</b>：不参与任何判定、计费与落账，也不改任何行为 —— 写入方失败时调用、
     * 成功时用 {@link #REJECT_NONE} 清空即可（界面只展示"最近一次"结论）。
     * <p>
     * <b>为什么由写入方（库页动作 / 搬运引擎 / 储存口）调用</b>：终端自己看不到"被谁拒绝"，
     * 而容量 / 空位预检分散在各入口；集中在这里回传，才能让
     * "同一业务错误在所有入口保持一致可观测"（历史上储存口报原因、终端界面一言不发）。
     *
     * @param rejectReason {@link #REJECT_*} 之一；{@link #REJECT_NONE} = 清空
     * @param feeNeed      被拒绝那一笔所需赤能源（无意义时传 0），口径与
     *                     {@code ItemTerminalFee} 一致，用于界面悬停给数字
     */
    default void noteReject(int rejectReason, long feeNeed) {
    }

    // ===== 宿主定位（菜单视距校验用；方块实体天然满足） =====

    BlockPos getBlockPos();

    boolean isRemoved();
}
