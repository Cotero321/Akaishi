package com.example.akaishi.api.energy;

/**
 * 生命能量接收方：由能量发射器在发射前探测、在弹体命中时注入。
 * <p>与 {@link IEnergyProvider} 的区别在于"不接受管道"——实现本接口的设备只经由发射器
 * 的弹体获得能量，管道对其不可见（不实现 IEnergyProvider 即天然断开连接）。
 * <p>对外预留：附属设备实现本接口即可被发射器供能，无需修改发射器。
 */
public interface ILifeEnergyReceiver {

    /** 是否仍需注入；返回 false 时发射器不再抽能、也不发射，避免无谓消耗 */
    boolean needsLifeEnergy();

    /**
     * 接收一次弹体注入。
     *
     * @param amount 注入量（&gt;0）
     * @return 实际接收量（0 表示拒绝）
     */
    long receiveLifeEnergy(long amount);
}
