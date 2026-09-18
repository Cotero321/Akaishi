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
    int DATA_SLOTS = 19;

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

    // ===== 宿主定位（菜单视距校验用；方块实体天然满足） =====

    BlockPos getBlockPos();

    boolean isRemoved();
}
