package com.example.akaishi.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * 无线终端统一接口（ISP）：赤能源终端与生命能量终端共用，
 * 使损耗计算（{@link WirelessTransferUtil#lossRatio}）与族感知解析
 * （{@link WirelessTransferUtil#resolveTerminal}）不依赖具体终端实现类。
 */
public interface IWirelessTerminal {

    /** 终端唯一 ID（网络注册表 key） */
    UUID terminalId();

    /** 是否已成型（多方块结构完整） */
    boolean isFormed();

    /** 是否已解锁跨维度传输 */
    boolean isCrossDim();

    /** 输入口方向损耗削减比例（0-1） */
    double inputLossReduction();

    /** 输出口方向损耗削减比例（0-1） */
    double outputLossReduction();

    /** 存入能量，返回实收 */
    long receiveWireless(long amount);

    /** 取出能量，返回实取 */
    long extractWireless(long amount);

    /** 所属网络族（赤能源 / 生命能量） */
    WirelessFamily family();

    /** 终端所在维度（未加载/已移除时可能为 null） */
    Level getLevel();

    /** 终端方块位置 */
    BlockPos getBlockPos();
}
