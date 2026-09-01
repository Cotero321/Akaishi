package com.example.akaishi.item.curio;

import com.example.akaishi.item.AkaishiPortableEnergyCell;
import com.example.akaishi.menu.EnergyFormat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * 赤石饰品抽象基类：内置赤能源存储（NBT 持久化），能量耗尽时自动从玩家背包的
 * 便携赤能源单元抽取补充，形成"随身能量池"经济。
 * 子类仅需覆写对应效果钩子，由平台层（Forge Curios 集成）统一调用：
 * - curioTick   ：每 tick 被动效果（饱食补充 / 防火 / 状态免疫）
 * - onKill      ：击杀生物回调
 * - onBlockBreak：挖掘方块回调
 * - onHurt      ：受伤减免回调（防爆），返回减免后的伤害
 */
public abstract class AkaishiCurioItem extends Item implements ICurioItem {

    /** 物品 NBT 中能量存储键 */
    public static final String TAG_ENERGY = "CurioEnergy";

    /** 饰品能量容量上限 */
    private final long capacity;

    public AkaishiCurioItem(Properties properties, long capacity) {
        super(properties);
        this.capacity = capacity;
    }

    public long getCapacity() {
        return capacity;
    }

    /** 当前存储能量（NBT 读取，缺省 0） */
    public long getEnergyStored(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ENERGY);
        return tag != null ? tag.getLong("Energy") : 0;
    }

    /** 存入能量，返回实际存入量 */
    public long addEnergy(ItemStack stack, long amount, boolean simulate) {
        long stored = getEnergyStored(stack);
        long toAdd = Math.min(amount, capacity - stored);
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

    /** 直接设置能量（写入 NBT，钳制到 [0, capacity]） */
    public void setEnergy(ItemStack stack, long amount) {
        CompoundTag tag = stack.getOrCreateTagElement(TAG_ENERGY);
        tag.putLong("Energy", Math.max(0, Math.min(amount, capacity)));
    }

    /**
     * 尝试消耗能量：优先扣饰品自身存储，不足部分从背包中的便携赤能源单元抽取。
     * 返回实际可用的能量（可能小于 amount）。
     */
    public long tryConsume(Player player, ItemStack stack, long amount) {
        long stored = getEnergyStored(stack);
        if (stored >= amount) {
            extractEnergy(stack, amount, false);
            return amount;
        }
        extractEnergy(stack, stored, false);
        long missing = amount - stored;
        long pulled = pullFromInventory(player, missing);
        return stored + pulled;
    }

    /** 从玩家背包便携单元抽取能量：遍历全部单元，累计抽取直到满足需求或抽空 */
    private static long pullFromInventory(Player player, long amount) {
        long remaining = amount;
        long totalPulled = 0;
        for (ItemStack inv : player.getInventory().items) {
            if (remaining <= 0) {
                break;
            }
            if (inv.getItem() instanceof AkaishiPortableEnergyCell cell) {
                long got = cell.extractEnergy(inv, remaining, false);
                totalPulled += got;
                remaining -= got;
            }
        }
        return totalPulled;
    }

    /** 允许装备的 Curios 槽位标识（charm/ring/hands/necklace/body/bracelet/belt），由平台层 canEquip 校验 */
    public String[] curioSlots() {
        return new String[]{"any"};
    }

    /** ICurioItem：每 tick 由 Curios 调用，仅服务端转发到效果钩子（避免重复逻辑） */
    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        Entity entity = slotContext.entity();
        if (entity instanceof Player player && !entity.level().isClientSide) {
            curioTick(player.level(), player, stack);
        }
    }

    /** ICurioItem：装备槽位校验，只允许在 curioSlots() 声明的槽位装备 */
    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        for (String slot : curioSlots()) {
            if (slotContext.identifier().equals(slot)) {
                return true;
            }
        }
        return false;
    }

    /** 每 tick 被动效果（默认无） */
    public void curioTick(Level level, Player player, ItemStack stack) {
    }

    /** 击杀生物回调（默认无） */
    public void onKill(Player player, ItemStack stack, LivingEntity target) {
    }

    /** 挖掘方块回调（默认无） */
    public void onBlockBreak(Player player, ItemStack stack, BlockPos pos, BlockState state) {
    }

    /** 受伤回调（默认不减伤），返回减免后的伤害 */
    public float onHurt(Player player, ItemStack stack, float amount, DamageSource source) {
        return amount;
    }

    /** 效果描述 tooltip 翻译键（默认无） */
    protected String tooltipKey() {
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        String key = tooltipKey();
        if (key != null) {
            tooltip.add(Component.translatable(key));
        }
        tooltip.add(Component.translatable("gui.akaishi.energy",
                EnergyFormat.format(getEnergyStored(stack)), EnergyFormat.format(capacity)));
    }
}
