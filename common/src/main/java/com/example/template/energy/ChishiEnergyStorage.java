package com.example.template.energy;

import com.example.template.api.energy.IEnergyStorage;
import com.example.template.api.energy.IEnergyType;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于原子长整型实现的能量存储，线程安全。
 * 为绑定某种 {@link IEnergyType} 的通用存储，供机器方块（如赤石提纯器）持有。
 * 容量使用 long，可支撑超级储存元件等超大容量设备。
 */
public class ChishiEnergyStorage implements IEnergyStorage {

    private final IEnergyType type;
    private final long maxEnergy;
    private final AtomicLong energy = new AtomicLong();

    public ChishiEnergyStorage(IEnergyType type, long maxEnergy) {
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
        long current = energy.get();
        long toAdd = Math.min(amount, maxEnergy - current);
        if (toAdd > 0 && !simulate) {
            energy.addAndGet(toAdd);
        }
        return toAdd;
    }

    @Override
    public long extractEnergy(long amount, boolean simulate) {
        long current = energy.get();
        long toExtract = Math.min(amount, current);
        if (toExtract > 0 && !simulate) {
            energy.addAndGet(-toExtract);
        }
        return toExtract;
    }

    /**
     * 直接设置当前能量值（用于从 NBT 恢复），自动夹取到 [0, maxEnergy]。
     * 复用现有实例而非重建，避免外部引用失效导致能量"不累积"。
     */
    public void setEnergy(long amount) {
        energy.set(Math.max(0, Math.min(amount, maxEnergy)));
    }
}
