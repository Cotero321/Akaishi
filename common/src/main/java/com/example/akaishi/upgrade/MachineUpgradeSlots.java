package com.example.akaishi.upgrade;

import com.example.akaishi.config.ModConfig;
import com.example.akaishi.item.AkaishiMachineUpgradeItem;
import com.example.akaishi.item.MachineUpgradeType;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * 机器升级槽：2 格（速度格/能量格），mayPlace 按组件类型互斥。
 * 单格最多堆叠 8 个（物品 maxStackSize 限制），堆叠数即等级：
 * 速度每级 +100%（即 1+数量，封顶 8×），能量每级 +50% 容量（封顶 +400%）。
 * 速度升级同时抬高耗能：耗能倍率 = min(1+速度数量, 4)（封顶 4×）。
 */
public class MachineUpgradeSlots extends SimpleContainer {

    public static final int SLOT_SPEED = 0;
    public static final int SLOT_ENERGY = 1;
    public static final int SLOT_COUNT = 2;

    /** 槽位变更回调（由持有 BE 注入 setChanged，保证升级槽变化被持久化） */
    private Runnable onChange;

    public MachineUpgradeSlots() {
        super(SLOT_COUNT);
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (onChange != null) {
            onChange.run();
        }
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        if (stack.getItem() instanceof AkaishiMachineUpgradeItem upgrade) {
            return index == SLOT_SPEED && upgrade.getUpgradeType() == MachineUpgradeType.SPEED
                    || index == SLOT_ENERGY && upgrade.getUpgradeType() == MachineUpgradeType.ENERGY;
        }
        return false;
    }

    /** 当前速度升级数量（0~8） */
    public int getSpeedCount() {
        return getItem(SLOT_SPEED).getCount();
    }

    /** 当前能量升级数量（0~8） */
    public int getEnergyCount() {
        return getItem(SLOT_ENERGY).getCount();
    }

    /** 速度倍率：1 + 数量，8 个封顶 8.0；再乘配置 [machine] workSpeed 全局速度倍率（默认 1.0 = 不变） */
    public float getSpeedMultiplier() {
        return Math.min(1F + getSpeedCount(), 8F) * (float) ModConfig.machineWorkSpeed;
    }

    /** 耗能倍率：随速度升级抬高，8 个封顶 4.0（每 tick 耗电功率 / 单次加工耗能的上限） */
    public float getEnergyCostMultiplier() {
        return Math.min(1F + getSpeedCount(), 4F);
    }

    /** 容量倍率：1 + 0.5 × 数量，8 个封顶 5.0 */
    public float getEnergyCapacityMultiplier() {
        return 1F + 0.5F * getEnergyCount();
    }

    /** 序列化到 NBT（1.20 的 ContainerHelper 仅支持 NonNullList，且 key 固定为 "Items"，此处中转） */
    public CompoundTag save(CompoundTag tag) {
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.set(i, getItem(i));
        }
        return ContainerHelper.saveAllItems(tag, items);
    }

    /** 从 NBT 恢复（无 "Items" 键时保持空槽） */
    public void load(CompoundTag tag) {
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < SLOT_COUNT; i++) {
            setItem(i, items.get(i));
        }
    }
}
