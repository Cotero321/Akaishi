package com.example.akaishi.item;

import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.energy.PortableCellTier;
import net.minecraft.world.item.ItemStack;

/**
 * 便携单元的能量存储封装：将 {@link ItemStack} 上的 NBT 能量包装为统一的 IEnergyStorage，
 * 供储存单元充能、机器网络等以标准接口读写，解耦"能量在哪"与"如何读写"。
 */
public class PortableCellStorage implements IEnergyStorage {

    private final ItemStack stack;
    private final AkaishiPortableEnergyCell cell;
    private final PortableCellTier tier;

    public PortableCellStorage(ItemStack stack) {
        this.stack = stack;
        this.cell = (AkaishiPortableEnergyCell) stack.getItem();
        this.tier = cell.tier;
    }

    @Override
    public IEnergyType getType() {
        return AkaishiEnergyType.INSTANCE;
    }

    @Override
    public long getEnergyStored() {
        return cell.getEnergyStored(stack);
    }

    @Override
    public long getMaxEnergy() {
        return tier.capacity;
    }

    @Override
    public long addEnergy(long amount, boolean simulate) {
        return cell.addEnergy(stack, amount, simulate);
    }

    @Override
    public long extractEnergy(long amount, boolean simulate) {
        return cell.extractEnergy(stack, amount, simulate);
    }
}
