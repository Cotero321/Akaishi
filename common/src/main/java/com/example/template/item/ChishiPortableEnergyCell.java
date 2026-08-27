package com.example.template.item;

import com.example.template.energy.PortableCellTier;
import com.example.template.menu.EnergyFormat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 便捷赤能源储存单元：随身携带的赤能源电池。
 * 能量以 NBT 持久化于物品上；手持（主/副手）时由 Forge 事件自动为玩家身上的赤石装备
 * 补充耐久（消耗赤能源）；放置到赤能源储存单元的充能槽可自动充电。
 * 容量/速率按等级区分（初级/中级/高级）。
 */
public class ChishiPortableEnergyCell extends Item {

    /** 修复 1 点耐久消耗的赤能源 */
    public static final long ENERGY_PER_DURABILITY = 100;
    /** 物品 NBT 中能量存储键 */
    public static final String TAG_ENERGY = "PortableEnergy";

    /** 本物品的等级（容量/速率由等级决定） */
    public final PortableCellTier tier;

    public ChishiPortableEnergyCell(PortableCellTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    /** 是否为便携赤能源单元 */
    public static boolean isPortableCell(ItemStack stack) {
        return stack.getItem() instanceof ChishiPortableEnergyCell;
    }

    /** 返回便携单元的等级，非便携单元返回 null */
    public static PortableCellTier tierOf(ItemStack stack) {
        return stack.getItem() instanceof ChishiPortableEnergyCell cell ? cell.tier : null;
    }

    /** 当前存储能量（从 NBT 读取，缺省 0） */
    public long getEnergyStored(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ENERGY);
        return tag != null ? tag.getLong("Energy") : 0;
    }

    /** 容量上限（由等级决定） */
    public long getMaxEnergy() {
        return tier.capacity;
    }

    /** 存入能量，返回实际存入量 */
    public long addEnergy(ItemStack stack, long amount, boolean simulate) {
        long stored = getEnergyStored(stack);
        long toAdd = Math.min(amount, tier.capacity - stored);
        if (toAdd > 0 && !simulate) {
            setEnergy(stack, stored + toAdd);
        }
        return toAdd;
    }

    /** 取出能量，返回实际取出量 */
    public long extractEnergy(ItemStack stack, long amount, boolean simulate) {
        long stored = getEnergyStored(stack);
        long toExtract = Math.min(amount, stored);
        if (toExtract > 0 && !simulate) {
            setEnergy(stack, stored - toExtract);
        }
        return toExtract;
    }

    /** 直接设置能量（写入 NBT） */
    public void setEnergy(ItemStack stack, long amount) {
        CompoundTag tag = stack.getOrCreateTagElement(TAG_ENERGY);
        tag.putLong("Energy", Math.max(0, Math.min(amount, tier.capacity)));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gui.template_mod.energy",
                EnergyFormat.format(getEnergyStored(stack)), EnergyFormat.format(getMaxEnergy())));
        tooltip.add(Component.translatable("gui.template_mod.portable_cell.hint"));
    }
}
