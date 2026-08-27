package com.example.template.energy;

import com.example.template.api.energy.IEnergyStorage;
import com.example.template.api.energy.IEnergyType;

import java.util.List;
import java.util.function.Supplier;

/**
 * 赤能源储存串联器聚合存储：将 3×3×3 结构内全部储存单元（26 个外壳 + 中心自身）聚合成单一逻辑存储。
 * 读操作汇总所有成员；写操作按成员顺序分配，等效于把各单元"串联"成一台大容量设备。
 * 成员列表由 Supplier 动态提供，结构变化（外壳被拆）后引用始终有效。
 */
public class ChishiEnergyCellArrayStorage implements IEnergyStorage {

    private final IEnergyType type;
    private final Supplier<List<IEnergyStorage>> memberSupplier;

    public ChishiEnergyCellArrayStorage(IEnergyType type, Supplier<List<IEnergyStorage>> memberSupplier) {
        this.type = type;
        this.memberSupplier = memberSupplier;
    }

    @Override
    public IEnergyType getType() {
        return type;
    }

    @Override
    public long getEnergyStored() {
        long sum = 0;
        for (IEnergyStorage member : memberSupplier.get()) {
            sum += member.getEnergyStored();
        }
        return sum;
    }

    @Override
    public long getMaxEnergy() {
        long sum = 0;
        for (IEnergyStorage member : memberSupplier.get()) {
            sum += member.getMaxEnergy();
        }
        return sum;
    }

    @Override
    public long addEnergy(long amount, boolean simulate) {
        long remaining = amount;
        for (IEnergyStorage member : memberSupplier.get()) {
            if (remaining <= 0) {
                break;
            }
            remaining -= member.addEnergy(remaining, simulate);
        }
        return amount - remaining;
    }

    @Override
    public long extractEnergy(long amount, boolean simulate) {
        long remaining = amount;
        for (IEnergyStorage member : memberSupplier.get()) {
            if (remaining <= 0) {
                break;
            }
            remaining -= member.extractEnergy(remaining, simulate);
        }
        return amount - remaining;
    }
}
