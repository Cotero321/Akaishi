package com.example.akaishi.api.miniature;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/**
 * 微缩终端状态：微缩方块内承载的「原终端全部数据 + 对外能力」。
 * <p>
 * 一个状态实例对应一个微缩方块，由对应族的适配器创建与加载；各族差异全部收在这一层，
 * 方块本体（{@link com.example.akaishi.block.entity.MiniatureTerminalBlockEntity}）只做
 * 生命周期、持久化与能力转发，不认任何具体终端类型（OCP）。
 * <p>
 * <b>能力默认全关</b>：没有物品能力的族（如纯能量族）无需覆写任何方法，微缩方块自然不暴露物品槽；
 * 需要物品能力的族覆写 {@link #pipeInputSlots} / {@link #pipeOutputSlots} 与四个搬运入口即可。
 * <p>
 * <b>搬运入口的契约</b>（与储存无线输入/输出口同口径，切勿改成"真实槽位"语义）：
 * <ul>
 *   <li>{@link #previewSlot} 只读：输入侧恒空（本块不存"待收物"），输出侧给出"下一个会被取出的堆"的副本；
 *   <li>{@link #canAcceptSlot} 必须是<b>零副作用整堆预检</b>（空间 + 费用都过才 true），
 *       物流适配层据它承诺"整堆收得下"，否则会只收一半而丢物；
 *   <li>{@link #insertSlot} 返回<b>未接收的余量</b>，不吞物；
 *   <li>{@link #removeSlot} 返回<b>实抽出的堆</b>，不复制。
 * </ul>
 */
public interface MiniatureTerminalState {

    /** 状态存盘（终端全部数据；由微缩方块 NBT 承载，随掉落物走） */
    CompoundTag save();

    /** 服务端每 tick（默认无事可做） */
    default void tick() {
    }

    // ===== 物品能力（默认无） =====

    /** 物品输入槽（管道的输入槽声明）；返回空数组 = 本状态不可接收物品 */
    default int[] pipeInputSlots() {
        return new int[0];
    }

    /** 物品输出槽（管道的输出槽声明）；返回空数组 = 本状态不可输出物品 */
    default int[] pipeOutputSlots() {
        return new int[0];
    }

    /** 虚拟槽数量（输入槽 + 输出槽的并集上界；无物品能力返回 0） */
    default int containerSize() {
        return 0;
    }

    /** 只读预览：输入侧恒空，输出侧为"下一个会被取出的堆"副本 */
    default ItemStack previewSlot(int slot) {
        return ItemStack.EMPTY;
    }

    /** 零副作用整堆预检：空间与费用全部可行才 true */
    default boolean canAcceptSlot(int slot, ItemStack stack) {
        return false;
    }

    /** 外部塞入（返回未接收的余量；未绑定/未成型/费用不足一律原样退回） */
    default ItemStack insertSlot(int slot, ItemStack stack) {
        return stack;
    }

    /** 外部抽取（返回实抽出的堆；费用不足/无货返回空堆） */
    default ItemStack removeSlot(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    /** 物品终端宿主视图：实现 {@link com.example.akaishi.api.storage.IItemTerminalHost} 的族返回自身，
     *  以便直接复用库页菜单与界面；其它族返回 null（界面走适配器自己的入口） */
    @Nullable
    default com.example.akaishi.api.storage.IItemTerminalHost itemHost() {
        return null;
    }

    // ===== 能量能力（自研赤能源接口；默认无，绝不注册第三方能量能力） =====

    /** 本族持有的能量存储（无能量族返回 null） */
    @Nullable
    default com.example.akaishi.api.energy.IEnergyStorage energyStorage() {
        return null;
    }

    /** 是否允许能量网络向本方块注入（持能量的族应返回 true：微缩件没有贴装接入口，只能靠能量管道回充） */
    default boolean canInputEnergy() {
        return false;
    }

    /** 是否允许能量网络从本方块抽取（微缩件本身不发电，默认 false） */
    default boolean canOutputEnergy() {
        return false;
    }

    // ===== 无线网络中枢能力（仅两个能量族覆写；默认全部惰性 ⇒ 物品族永不被误判为任一能量族） =====

    /**
     * 所属无线网络族。
     * <p>
     * <b>默认 null = 不是无线终端</b>：{@code WirelessTransferUtil.terminalAt} 要求
     * {@code family() == 期望族}，而期望族恒为具体枚举值，故 null 永远无法命中，
     * 物品族微缩件即使摆在与端口同坐标处也不会被认作能量中枢。
     */
    @Nullable
    default com.example.akaishi.wireless.WirelessFamily family() {
        return null;
    }

    /** 是否已解锁跨维度传输（原结构含跨维组件；微缩件据此保留跨维口连接） */
    default boolean isCrossDim() {
        return false;
    }

    /** 输入口方向损耗削减比例（0-0.9） */
    default double inputLossReduction() {
        return 0.0;
    }

    /** 输出口方向损耗削减比例（0-0.9） */
    default double outputLossReduction() {
        return 0.0;
    }

    /** 是否解锁便携终端「随身供能」（原结构含便捷传输构架；赤能源族口径） */
    default boolean hasTransmitFrame() {
        return false;
    }

    /** 无线输入口推送能量（返回实收；非能量族恒 0） */
    default long receiveWireless(long amount) {
        return 0L;
    }

    /** 无线输出口/便携终端抽取能量（返回实取；非能量族恒 0） */
    default long extractWireless(long amount) {
        return 0L;
    }
}
