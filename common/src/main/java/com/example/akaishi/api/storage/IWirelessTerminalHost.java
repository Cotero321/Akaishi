package com.example.akaishi.api.storage;

import com.example.akaishi.wireless.TerminalSecurity;

import net.minecraft.world.inventory.ContainerData;

import java.util.UUID;

/**
 * 无线能量终端宿主契约（赤能源 / 生命能量共用）：终端界面的「运行情况 / 能量储存 / 安全卡认证 /
 * 能量传输」四页菜单与 Screen 对终端侧数据的最小依赖面（DIP）。
 * <p>
 * 菜单只经本接口取数与取身份 —— 数据槽读数（{@link #data()}）、安全表（{@link #security()}）、
 * 终端 ID（{@link #terminalId()}），不再绑定具体方块实体类型；微缩件等非方块实体宿主实现本接口
 * 即可零成本复用整套界面。
 * <p>
 * 与 {@link IItemTerminalHost} 并列的理由：两者都是"终端宿主对界面层的最小依赖面"，
 * 同一归口便于查找；无线族本身不含存储职责，但它同样是终端宿主契约，不另立新包。
 * <p>
 * <b>只声明菜单真正用到的成员与常量</b>：结构相关读数（成型 / 端口数 / 区块加载 / 跨维）全部经
 * {@link #data()} 的固定版式传递，因此宿主不需要额外暴露访问器；数据槽下标常量随接口一同下沉，
 * 实现方按同一版式填充。
 */
public interface IWirelessTerminalHost {

    // ===== 数据槽（long 一律拆 4 槽，防 2^31 截断） =====

    /** 结构是否成型（1=成型） */
    int DATA_FORMED = 0;
    int DATA_STORED_LOW = 1;
    int DATA_STORED_HIGH = 2;
    /** 储能 64 位高段（>2^31 时低槽无法承载） */
    int DATA_STORED_HIGH2 = 17;
    int DATA_STORED_HIGH3 = 18;
    int DATA_CAPACITY_LOW = 3;
    int DATA_CAPACITY_HIGH = 4;
    int DATA_CAPACITY_HIGH2 = 19;
    int DATA_CAPACITY_HIGH3 = 20;
    /** 已认证输入口数 */
    int DATA_INPUT_COUNT = 5;
    /** 已认证输出口数 */
    int DATA_OUTPUT_COUNT = 6;
    /** 绑定储能单元数量 */
    int DATA_BOUND_SERIALIZERS = 7;
    /** 跨维度是否已解锁（1=已解锁） */
    int DATA_CROSS_DIM = 9;
    /** 区块加载是否已启用 */
    int DATA_CHUNK_LOAD = 10;
    /** 区块加载范围是否扩展为 3×3 */
    int DATA_CHUNK_RANGE = 11;
    /** 输入方向损耗抑制组件数量 */
    int DATA_INPUT_LOSS = 12;
    /** 输出方向损耗抑制组件数量 */
    int DATA_OUTPUT_LOSS = 13;
    /** 终端短 ID 低 16 位 */
    int DATA_TERMINAL_ID = 14;
    /** 当前弱加载区块数 */
    int DATA_CHUNK_LOADED = 15;
    /** 终端短 ID 高 16 位（数据槽仅 16 位有效，8 位 hex 需拆 2 槽） */
    int DATA_TERMINAL_ID_HIGH = 21;
    int DATA_SLOTS = 22;

    /** 同步给菜单的数据槽（服务端写入，客户端只读） */
    ContainerData data();

    /** 终端唯一 ID：端口 / 无线设备按它寻址（微缩前后不变，故绑定不失效） */
    UUID terminalId();

    /** 安全表（归属者 + 权限表）：安全页渲染与服务端动作的唯一真源 */
    TerminalSecurity security();

    /** 宿主所在坐标：界面距离校验用（方块实体天然满足；非方块宿主要给出自身位置） */
    net.minecraft.core.BlockPos getBlockPos();
}
