package com.example.template.fluid;

import com.example.template.item.ChishiFuelCellItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * 反应堆燃料能量利用率表：识别燃料罐内的燃料并返回能量利用率（幽匿 2 → 终极混合 10）。
 * 利用率决定单槽产率与产热（消耗速率恒定），供控制器燃烧结算统一取用。
 * 非反应堆燃料返回 0（该槽不参与燃烧）。
 */
public final class ReactorFuels {

    /** 幽匿生命燃料（能量利用率） */
    public static final int UTIL_SCULK = 2;
    /** 下界复合燃料 */
    public static final int UTIL_NETHER_COMPOUND = 4;
    /** 末地混合燃料 */
    public static final int UTIL_END_MIXTURE = 4;
    /** 高级混合燃料 */
    public static final int UTIL_ADVANCED_MIXTURE = 5;
    /** 至纯燃料 */
    public static final int UTIL_PURE = 7;
    /** 末地巨龙燃料 */
    public static final int UTIL_DRAGON = 7;
    /** 终极混合燃料 */
    public static final int UTIL_ULTIMATE = 10;

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
}
