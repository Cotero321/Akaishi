package com.example.akaishi.miniature;

import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.miniature.MiniatureTerminalState;
import com.example.akaishi.api.storage.IWirelessTerminalHost;
import com.example.akaishi.block.entity.MiniatureTerminalBlockEntity;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.util.LongDataSlots;
import com.example.akaishi.wireless.TerminalSecurity;
import com.example.akaishi.wireless.WirelessFamily;
import com.example.akaishi.wireless.WirelessNetworkManager;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 微缩后的「无线能量终端」状态（赤能源 / 生命能量两族共用，族与能量类型由适配器注入）。
 * <p>
 * <b>接着当网络中枢的做法</b>：微缩方块自己实现 {@code IWirelessTerminal}（本类提供全部转发值），
 * 每 tick 用<b>坍缩前同一个 terminalId</b> 重新注册心跳进 {@link WirelessNetworkManager}
 * （族、维度、坐标、安全表镜像随之一并恢复），端口按 ID 直达
 * （{@code WirelessTransferUtil.resolveTerminal} 要求实体型 {@code IWirelessTerminal + isFormed + 同族}）、
 * 便携终端按身份反查 —— 绑定关系不因坍缩而失效。
 * <p>
 * <b>池子口径（用户拍板，与物品终端统一）</b>：坍缩时贴身储能单元被<b>一并消耗</b>，
 * 容量与储量作为快照（{@link #TAG_POOL_CAPACITY} / {@link #TAG_POOL_STORED}）并入芯片 ——
 * 因此这里自持一个 {@link AkaishiEnergyStorage}，芯片搬到哪都带电，不再依赖世界里的原件。
 * （上一轮"记坐标 + 每 20 tick 在线重扫"的方案已废弃：那时单元留在世界上，快照必然是第二份电。）
 * {@link #TAG_POOL_CELLS} 仅作诊断/结构留档，<b>不再驱动任何能量读数</b>。
 * <p>
 * <b>不向能量管道开口</b>（{@link #canInputEnergy} / {@link #canOutputEnergy} 恒 false）：原无线终端
 * 不是 {@code IEnergyProvider}，能量只经无线端口进出；微缩件保持同口径，{@link #energyStorage()}
 * 如实暴露芯片自身的池子，但既不注册 forge 能量能力、也不对外声明可输入/可输出。
 * <p>
 * <b>界面</b>：实现 {@link IWirelessTerminalHost}，与两个终端方块实体共用同一套四页菜单与 Screen；
 * 结构相关读数（成型 / 端口数 / 区块加载 / 跨维）全部经数据槽给出合理值，微缩件没有结构也不会 NPE。
 */
public final class WirelessTerminalMiniatureState implements MiniatureTerminalState, IWirelessTerminalHost {

    /** payload 键：安全表（口径与原终端完全同构） */
    public static final String TAG_SECURITY = "Security";
    /** payload 键：贴身储能单元相对坐标列表（IntArrayTag[3]）——仅诊断留档，不再驱动能量读数 */
    public static final String TAG_POOL_CELLS = "PoolCells";
    /** payload 键：并入芯片的池子容量（long，= 坍缩时各成员容量之和） */
    public static final String TAG_POOL_CAPACITY = "PoolCapacity";
    /** payload 键：并入芯片的池子储量（long，= 坍缩时各成员储量之和） */
    public static final String TAG_POOL_STORED = "PoolStored";
    /** payload 键：原结构是否解锁跨维度 */
    public static final String TAG_CROSS_DIM = "CrossDim";
    /** payload 键：原结构输入方向损耗抑制组件数量 */
    public static final String TAG_INPUT_LOSS = "InputLoss";
    /** payload 键：原结构输出方向损耗抑制组件数量 */
    public static final String TAG_OUTPUT_LOSS = "OutputLoss";
    /** payload 键：原结构是否含便捷传输构架（便携终端随身供能解锁） */
    public static final String TAG_TRANSMIT_FRAME = "TransmitFrame";

    /** 损耗削减上限（与原终端 BE 一致） */
    private static final double MAX_LOSS_REDUCTION = 0.9;

    private final MiniatureTerminalBlockEntity be;
    private final WirelessFamily family;
    private final UUID terminalId;
    /** 安全表（归属者 + 权限表）：整体继承，坍缩不改授权 */
    private final TerminalSecurity security;

    /** 结构留档：坍缩时被吞下的储能单元相对坐标（仅诊断/未来解包） */
    private final List<BlockPos> poolCells = new ArrayList<>();
    /** 自持储能池：容量 = 坍缩快照容量、储量 = 坍缩快照储量（芯片自带电） */
    private final AkaishiEnergyStorage pool;
    /** 界面数据槽（版式见 {@link IWirelessTerminalHost}；每 tick 刷新，服务端广播给客户端） */
    private final SimpleContainerData data = new SimpleContainerData(DATA_SLOTS);

    private final boolean crossDim;
    private final int inputLossModules;
    private final int outputLossModules;
    private final boolean transmitFrame;

    public WirelessTerminalMiniatureState(MiniatureTerminalBlockEntity be, CompoundTag payload,
            WirelessFamily family, IEnergyType energyType) {
        this.be = be;
        this.family = family == null ? WirelessFamily.CHISHI : family;
        // 终端 ID 由方块自己持有（与微缩前同一个）：这里只借用，不重复落一份，避免两处 ID 漂移
        this.terminalId = be.terminalId() == null ? UUID.randomUUID() : be.terminalId();
        this.security = new TerminalSecurity(this::onSecurityChanged);
        if (payload.contains(TAG_SECURITY)) {
            this.security.load(payload.getCompound(TAG_SECURITY));
        }
        this.crossDim = payload.getBoolean(TAG_CROSS_DIM);
        this.inputLossModules = Math.max(0, payload.getInt(TAG_INPUT_LOSS));
        this.outputLossModules = Math.max(0, payload.getInt(TAG_OUTPUT_LOSS));
        this.transmitFrame = payload.getBoolean(TAG_TRANSMIT_FRAME);
        loadPoolCells(payload);
        this.pool = new AkaishiEnergyStorage(energyType, Math.max(0L, payload.getLong(TAG_POOL_CAPACITY)));
        this.pool.setEnergy(payload.getLong(TAG_POOL_STORED));
        // 读档/坍缩即推安全表镜像：端口与便携终端立刻能按 ID 完成权限判定
        this.security.pushTo(terminalId);
        pushData();
    }

    private void loadPoolCells(CompoundTag payload) {
        ListTag list = payload.getList(TAG_POOL_CELLS, Tag.TAG_INT_ARRAY);
        for (int i = 0; i < list.size(); i++) {
            int[] xyz = ((IntArrayTag) list.get(i)).getAsIntArray();
            if (xyz.length == 3) {
                poolCells.add(new BlockPos(xyz[0], xyz[1], xyz[2]));
            }
        }
    }

    // ===== 运行 =====

    @Override
    public void tick() {
        Level level = be.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        pushData();
        // 以同一个 terminalId 重新注册 + 心跳 + 声明族：原端口与便携终端据此按 ID 继续找到微缩件
        WirelessNetworkManager.registerTerminal(terminalId, level.dimension(), be.getBlockPos(),
                level.getGameTime(), family);
    }

    /**
     * 刷新界面数据槽：能量读数取芯片自持池子；端口数取<b>网络注册表真实值</b>
     * （端口仍按同一个 terminalId 绑定，搬走芯片也不丢连接）。
     * 微缩件没有结构 —— 成型恒 1、区块加载/3×3 恒 0、绑定单元数恒 0
     * （贴身单元已被吞入池子，再显示"外接单元"会误导；这些值均给出而非留空，故界面不会 NPE）。
     */
    private void pushData() {
        data.set(DATA_FORMED, 1);
        LongDataSlots.write(data, DATA_STORED_LOW, DATA_STORED_HIGH, DATA_STORED_HIGH2, DATA_STORED_HIGH3,
                pool.getEnergyStored());
        LongDataSlots.write(data, DATA_CAPACITY_LOW, DATA_CAPACITY_HIGH, DATA_CAPACITY_HIGH2,
                DATA_CAPACITY_HIGH3, pool.getMaxEnergy());
        data.set(DATA_INPUT_COUNT, WirelessNetworkManager.inputCount(terminalId));
        data.set(DATA_OUTPUT_COUNT, WirelessNetworkManager.outputCount(terminalId));
        data.set(DATA_BOUND_SERIALIZERS, 0);
        data.set(DATA_CROSS_DIM, crossDim ? 1 : 0);
        data.set(DATA_CHUNK_LOAD, 0);
        data.set(DATA_CHUNK_RANGE, 0);
        data.set(DATA_CHUNK_LOADED, 0);
        data.set(DATA_INPUT_LOSS, inputLossModules);
        data.set(DATA_OUTPUT_LOSS, outputLossModules);
        LongDataSlots.writeInt(data, DATA_TERMINAL_ID, DATA_TERMINAL_ID_HIGH,
                (int) (terminalId.getMostSignificantBits() >>> 32));
    }

    // ===== 安全表 =====

    /** 安全表变化：落盘 + 推镜像（端口/便携终端依赖注册表镜像） */
    private void onSecurityChanged() {
        be.setChanged();
        security.pushTo(terminalId);
    }

    // ===== IWirelessTerminalHost（四页菜单 / 安全页） =====

    @Override
    public SimpleContainerData data() {
        return data;
    }

    @Override
    public UUID terminalId() {
        return terminalId;
    }

    /** 宿主坐标：微缩件自己不是方块实体，位置向承载它的方块借用 */
    @Override
    public net.minecraft.core.BlockPos getBlockPos() {
        return be.getBlockPos();
    }

    @Override
    public TerminalSecurity security() {
        return security;
    }

    // ===== 无线中枢（MiniatureTerminalBlockEntity 逐项转发到本类） =====

    @Override
    public WirelessFamily family() {
        return family;
    }

    /** 跨维解锁随坍缩一并继承（原结构含跨维组件时才为 true） */
    @Override
    public boolean isCrossDim() {
        return crossDim;
    }

    @Override
    public double inputLossReduction() {
        return lossReduction(inputLossModules);
    }

    @Override
    public double outputLossReduction() {
        return lossReduction(outputLossModules);
    }

    @Override
    public boolean hasTransmitFrame() {
        return transmitFrame;
    }

    @Override
    public long receiveWireless(long amount) {
        return pool.addEnergy(amount, false);
    }

    @Override
    public long extractWireless(long amount) {
        return pool.extractEnergy(amount, false);
    }

    @Override
    public IEnergyStorage energyStorage() {
        return pool;
    }

    /** 原无线终端不是 IEnergyProvider：能量只经无线端口进出，微缩件不额外向能量管道开口 */
    @Override
    public boolean canInputEnergy() {
        return false;
    }

    @Override
    public boolean canOutputEnergy() {
        return false;
    }

    /** 损耗削减现算（不落计算值）：与配置项口径一致，改配置立刻生效 */
    private static double lossReduction(int modules) {
        return Math.min(MAX_LOSS_REDUCTION, modules * ModConfig.wirelessLossReductionPerModule);
    }

    // ===== 存盘 =====

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        CompoundTag sec = new CompoundTag();
        security.save(sec);
        tag.put(TAG_SECURITY, sec);
        tag.putLong(TAG_POOL_CAPACITY, pool.getMaxEnergy());
        tag.putLong(TAG_POOL_STORED, pool.getEnergyStored());
        ListTag cells = new ListTag();
        for (BlockPos offset : poolCells) {
            cells.add(new IntArrayTag(new int[] {offset.getX(), offset.getY(), offset.getZ()}));
        }
        tag.put(TAG_POOL_CELLS, cells);
        tag.putBoolean(TAG_CROSS_DIM, crossDim);
        tag.putInt(TAG_INPUT_LOSS, inputLossModules);
        tag.putInt(TAG_OUTPUT_LOSS, outputLossModules);
        tag.putBoolean(TAG_TRANSMIT_FRAME, transmitFrame);
        return tag;
    }
}
