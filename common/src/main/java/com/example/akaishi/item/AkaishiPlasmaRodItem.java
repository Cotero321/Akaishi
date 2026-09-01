package com.example.akaishi.item;

import com.example.akaishi.config.ModConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 等离子体燃料棒：离子体填装器将对应等离子体灌入聚变反应棒的产物，聚变堆的燃料。
 * <p>
 * 三种类型决定产率/热值/散热加成：混合均衡、下界高产高热、末地低产低热 + 自带散热。
 * 燃料棒以 NBT 存储剩余能量（long，总量 {@link #ROD_ENERGY}），聚变堆燃烧时按 tick 消耗，
 * 能量归零即烧尽消失（不返还空反应棒）。
 */
public class AkaishiPlasmaRodItem extends Item {

    /** NBT 键：剩余赤能源总量 */
    public static final String KEY_ENERGY = "FusionEnergy";

    /** 燃料棒总能量（赤能源），与类型无关 */
    public static final long ROD_ENERGY = 6_000_000_000_000L;

    public enum RodType {
        /** 混合：均衡产率/热值，产量最高可再叠效率框架 */
        MIXED(4_000_000L, 27_000_000L, 0),
        /** 下界：高产高热，需强散热 */
        NETHER(4_000_000L, 40_000_000L, 0),
        /** 末地：低产低热，4 根无散热即落最佳温度区间（散热加成已移除） */
        END(2_600_000L, 15_000_000L, 0);

        /** 基础产率（赤能源/tick，效率框架系数在其上叠乘） */
        public final long baseYield;
        /** 热值（温度 M，效率框架系数在其上叠乘） */
        public final long heatValue;
        /** 每根散热效率加成（%） */
        public final int coolingBonus;

        RodType(long baseYield, long heatValue, int coolingBonus) {
            this.baseYield = baseYield;
            this.heatValue = heatValue;
            this.coolingBonus = coolingBonus;
        }
    }

    private final RodType rodType;

    public AkaishiPlasmaRodItem(RodType rodType) {
        super(new Item.Properties().stacksTo(1));
        this.rodType = rodType;
    }

    public RodType getRodType() {
        return rodType;
    }

    /** 读取剩余能量（未初始化时返回满能量） */
    public static long getEnergy(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof AkaishiPlasmaRodItem)) {
            return 0;
        }
        return stack.getOrCreateTag().getLong(KEY_ENERGY) <= 0
                ? ModConfig.fusionRodEnergy : stack.getTag().getLong(KEY_ENERGY);
    }

    /** 写入剩余能量 */
    public static void setEnergy(ItemStack stack, long energy) {
        stack.getOrCreateTag().putLong(KEY_ENERGY, energy);
    }

    /** 扣除指定量能量（满能量减去剩余为已消耗） */
    public static void consumeEnergy(ItemStack stack, long amount) {
        long left = getEnergy(stack) - amount;
        if (left <= 0) {
            stack.setTag(null); // 能量耗尽 → 移除 NBT 标记为已烧尽，由控制器槽位清空
            stack.setCount(0);
        } else {
            setEnergy(stack, left);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gui.akaishi.plasma_rod.type",
                Component.translatable("gui.akaishi.plasma_rod.type_" + rodType.name().toLowerCase())));
        tooltip.add(Component.translatable("gui.akaishi.plasma_rod.energy",
                AkaishiPlasmaRodItem.getEnergy(stack), ModConfig.fusionRodEnergy));
        tooltip.add(Component.translatable("gui.akaishi.plasma_rod.yield", rodType.baseYield));
        tooltip.add(Component.translatable("gui.akaishi.plasma_rod.heat", rodType.heatValue));
    }
}
