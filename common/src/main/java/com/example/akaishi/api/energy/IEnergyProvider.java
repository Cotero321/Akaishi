package com.example.akaishi.api.energy;

/**
 * 能量提供者：持有能量存储、可被管道等能量网络访问的方块实体实现此接口。
 * 管道扫描相邻方块时通过本接口统一获取能量存储，实现发送/接收与链式转发。
 * 支持多能量类型：默认返回主存储；同时持有多种能量类型的设备可覆盖
 * {@link #getEnergyStorage(IEnergyType)} 按类型分发。
 */
public interface IEnergyProvider {

    /** 返回暴露给能量网络的主存储，未持有时返回 null */
    IEnergyStorage getEnergyStorage();

    /**
     * 返回指定能量类型的存储，默认实现为主存储（类型匹配时）。
     * 多能量设备（如生命聚合转换器：赤能源进 + 生命能量出）应覆盖此方法。
     *
     * @return 该类型的存储；设备不持有该类型时返回 null
     */
    default IEnergyStorage getEnergyStorage(IEnergyType type) {
        IEnergyStorage storage = getEnergyStorage();
        return storage != null && storage.getType() == type ? storage : null;
    }

    /** 是否允许能量网络从此节点抽取主存储能量（纯消耗型机器应返回 false） */
    default boolean canOutputEnergy() {
        return true;
    }

    /** 是否允许能量网络向此节点注入主存储能量（纯发电型应返回 false） */
    default boolean canInputEnergy() {
        return true;
    }

    /** 是否允许能量网络从指定类型存储抽取能量，默认复用主存储的判定 */
    default boolean canOutputEnergy(IEnergyType type) {
        return canOutputEnergy();
    }

    /** 是否允许能量网络向指定类型存储注入能量，默认复用主存储的判定 */
    default boolean canInputEnergy(IEnergyType type) {
        return canInputEnergy();
    }
}
