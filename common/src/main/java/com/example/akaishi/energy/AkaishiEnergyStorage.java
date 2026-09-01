package com.example.akaishi.energy;

import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于原子长整型实现的能量存储，线程安全。
 * 为绑定某种 {@link IEnergyType} 的通用存储，供机器方块（如赤石提纯器）持有。
 * 容量使用 long，可支撑超级储存元件等超大容量设备。
 */
public class AkaishiEnergyStorage implements IEnergyStorage {

    private final IEnergyType type;
    /** 容量：机器升级组件（能量升级）可动态扩容，volatile 保证多线程可见 */
    private volatile long maxEnergy;
    private final AtomicLong energy = new AtomicLong();

    public AkaishiEnergyStorage(IEnergyType type, long maxEnergy) {
        this.type = type;
        this.maxEnergy = maxEnergy;
    }

    @Override
    public IEnergyType getType() {
        return type;
    }

    @Override
    public long getEnergyStored() {
        return energy.get();
    }

    @Override
    public long getMaxEnergy() {
        return maxEnergy;
    }

    @Override
    public long addEnergy(long amount, boolean simulate) {
        // CAS 循环：并发 add 不会超额（原 check-then-act 写法两个线程会同时通过检查导致超容量）
        while (true) {
            long current = energy.get();
            long toAdd = Math.min(amount, maxEnergy - current);
            if (toAdd <= 0) {
                return 0;
            }
            if (simulate) {
                return toAdd;
            }
            if (energy.compareAndSet(current, current + toAdd)) {
                return toAdd;
            }
        }
    }

    @Override
    public long extractEnergy(long amount, boolean simulate) {
        // CAS 循环：并发 extract 不会超额扣减为负
        while (true) {
            long current = energy.get();
            long toExtract = Math.min(amount, current);
            if (toExtract <= 0) {
                return 0;
            }
            if (simulate) {
                return toExtract;
            }
            if (energy.compareAndSet(current, current - toExtract)) {
                return toExtract;
            }
        }
    }

    /**
     * 直接设置当前能量值（用于从 NBT 恢复），自动夹取到 [0, maxEnergy]。
     * 复用现有实例而非重建，避免外部引用失效导致能量"不累积"。
     */
    public void setEnergy(long amount) {
        energy.set(Math.max(0, Math.min(amount, maxEnergy)));
    }

    /**
     * 动态调整容量（机器能量升级扩容），当前能量自动夹取到新容量内。
     * CAS 循环保证并发 add/extract 不会因容量变化而越界。
     */
    public void setMaxEnergy(long newMax) {
        this.maxEnergy = Math.max(0, newMax);
        while (true) {
            long current = energy.get();
            if (current <= maxEnergy) {
                return;
            }
            // 容量缩小时夹取超出部分，避免能量 > 容量
            if (energy.compareAndSet(current, maxEnergy)) {
                return;
            }
        }
    }
}
