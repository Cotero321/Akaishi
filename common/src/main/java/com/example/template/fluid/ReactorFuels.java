package com.example.template.fluid;

import com.example.template.item.ChishiFuelCellItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * 反应堆燃料属性表：识别燃料罐内的燃料并返回能量利用率与每棒热值。
 * 利用率决定单槽产率（消耗速率恒定），热值决定反应堆原始温度（300 + Σ热值），
 * 供控制器燃烧结算统一取用。非反应堆燃料返回 0 / 不参与燃烧。
 */
public final class ReactorFuels {

    /** 世界基础燃料（原幽匿生命燃料，能量利用率） */
    public static final int UTIL_SCULK = 3;
    /** 下界复合燃料 */
    public static final int UTIL_NETHER_COMPOUND = 4;
    /** 末地混合燃料 */
    public static final int UTIL_END_MIXTURE = 4;
    /** 高级混合燃料 */
    public static final int UTIL_ADVANCED_MIXTURE = 5;
    /** 至纯燃料 */
    public static final int UTIL_PURE = 8;
    /** 末地巨龙燃料 */
    public static final int UTIL_DRAGON = 7;
    /** 终极混合燃料 */
    public static final int UTIL_ULTIMATE = 14;

    // 每棒热值 = 40 × 利用率 × 产热系数（40 = 4×10，10 为"产热×10"换算）
    // 基础 120 ｜ 复合 240 ｜ 末地 249.6 ｜ 高级 240 ｜ 至纯 384 ｜ 巨龙 252 ｜ 终极 448
    /** 世界基础燃料产热系数 */
    public static final double HEAT_SCULK = 1.0;
    /** 下界复合燃料产热系数（高热低产，白嫖原料） */
    public static final double HEAT_NETHER_COMPOUND = 1.5;
    /** 末地混合燃料产热系数（混合类基础 1.2 × 1.3 提高） */
    public static final double HEAT_END_MIXTURE = 1.56;
    /** 高级混合燃料产热系数 */
    public static final double HEAT_ADVANCED_MIXTURE = 1.2;
    /** 至纯燃料产热系数 */
    public static final double HEAT_PURE = 1.2;
    /** 末地巨龙燃料产热系数（低温高压型） */
    public static final double HEAT_DRAGON = 0.9;
    /** 终极混合燃料产热系数（高功率低热，配合更好的散热片） */
    public static final double HEAT_ULTIMATE = 0.8;

    private ReactorFuels() {
    }

    /** 返回液体燃料的能量利用率；非反应堆燃料返回 0 */
    public static int getEnergyUtilization(Fluid fluid) {
        if (fluid == null) {
            return 0;
        }
        if (fluid == ModFluids.get(ModFluids.SCULK_LIFE_FUEL_ID)) {
            return UTIL_SCULK;
        }
        if (fluid == ModFluids.get(ModFluids.NETHER_COMPOUND_FUEL_ID)) {
            return UTIL_NETHER_COMPOUND;
        }
        if (fluid == ModFluids.get(ModFluids.END_MIXTURE_FUEL_ID)) {
            return UTIL_END_MIXTURE;
        }
        if (fluid == ModFluids.get(ModFluids.ADVANCED_MIXTURE_FUEL_ID)) {
            return UTIL_ADVANCED_MIXTURE;
        }
        if (fluid == ModFluids.get(ModFluids.PURE_FUEL_ID)) {
            return UTIL_PURE;
        }
        if (fluid == ModFluids.get(ModFluids.DRAGON_FUEL_ID)) {
            return UTIL_DRAGON;
        }
        if (fluid == ModFluids.get(ModFluids.ULTIMATE_MIXTURE_FUEL_ID)) {
            return UTIL_ULTIMATE;
        }
        return 0;
    }

    /** 燃料罐是否为可燃烧燃料（非空且能量利用率 > 0） */
    public static boolean isBurnable(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ChishiFuelCellItem)) {
            return false;
        }
        if (ChishiFuelCellItem.isEmpty(stack)) {
            return false;
        }
        return getEnergyUtilization(ChishiFuelCellItem.getFluid(stack)) > 0;
    }

    /** 产热系数（每棒热值 = 40 × 利用率 × 系数）：各燃料独立配置，决定温度梯度 */
    public static double heatMultiplier(Fluid fluid) {
        if (fluid == ModFluids.get(ModFluids.SCULK_LIFE_FUEL_ID)) {
            return HEAT_SCULK;
        }
        if (fluid == ModFluids.get(ModFluids.NETHER_COMPOUND_FUEL_ID)) {
            return HEAT_NETHER_COMPOUND;
        }
        if (fluid == ModFluids.get(ModFluids.END_MIXTURE_FUEL_ID)) {
            return HEAT_END_MIXTURE;
        }
        if (fluid == ModFluids.get(ModFluids.ADVANCED_MIXTURE_FUEL_ID)) {
            return HEAT_ADVANCED_MIXTURE;
        }
        if (fluid == ModFluids.get(ModFluids.PURE_FUEL_ID)) {
            return HEAT_PURE;
        }
        if (fluid == ModFluids.get(ModFluids.DRAGON_FUEL_ID)) {
            return HEAT_DRAGON;
        }
        if (fluid == ModFluids.get(ModFluids.ULTIMATE_MIXTURE_FUEL_ID)) {
            return HEAT_ULTIMATE;
        }
        return 0.0;
    }

    /** 每棒热值（原始温度贡献 = 基础 300 + Σ热值）：非燃料返回 0 */
    public static double heatValue(Fluid fluid) {
        int util = getEnergyUtilization(fluid);
        if (util <= 0) {
            return 0;
        }
        return 40.0 * util * heatMultiplier(fluid);
    }
}
