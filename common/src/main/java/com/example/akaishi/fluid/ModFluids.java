package com.example.akaishi.fluid;

import com.example.akaishi.AkaishiMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * 液体常量：注册表 ID 与 GUI 渲染颜色。
 * 液体本体在 Forge 平台模块注册（需 FluidType），common 通过原版注册表按 ID 取用。
 * 颜色同时供 GUI 液体条与 Forge 端 FluidType 纹理着色使用。
 */
public final class ModFluids {

    /** 下界至纯能量：下界之星液化产物，加工固态物 → 至纯燃料 */
    public static final String NETHER_PURE_ENERGY_ID = "nether_pure_energy";
    /** 下界复合能量：凋零玫瑰液化产物，加工固态物 → 下界复合燃料 */
    public static final String NETHER_COMPOUND_ENERGY_ID = "nether_compound_energy";
    /** 至纯燃料：反应堆燃料（100mb/固态物） */
    public static final String PURE_FUEL_ID = "pure_fuel";
    /** 下界复合燃料：反应堆燃料（1000mb/固态物） */
    public static final String NETHER_COMPOUND_FUEL_ID = "nether_compound_fuel";
    /** 末地混合燃料：末地混合物液化产物（末影之眼+潜影贝壳+紫颂果合成） */
    public static final String END_MIXTURE_FUEL_ID = "end_mixture_fuel";
    /** 末地巨龙燃料：巨龙混合物液化产物（龙息+末地水晶+黑曜石合成） */
    public static final String DRAGON_FUEL_ID = "dragon_fuel";
    /** 世界基础燃料（原幽匿生命燃料）：世界基础燃料物品液化产物（金萝卜+幽匿块+金苹果+赤石精华块合成） */
    public static final String SCULK_LIFE_FUEL_ID = "sculk_life_fuel";
    /** 高级混合燃料：末地混合燃料 + 下界复合燃料 1:1:1 混合 */
    public static final String ADVANCED_MIXTURE_FUEL_ID = "advanced_mixture_fuel";
    /** 终极混合燃料：末地巨龙燃料 + 至纯燃料 1:1:1 混合 */
    public static final String ULTIMATE_MIXTURE_FUEL_ID = "ultimate_mixture_fuel";
    /** 衰竭燃料（7 种，与燃料一一对应）：反应堆燃烧废品（5mb 燃料 → 1mb 废品），仅反应堆废品口/保存桶可储 */
    public static final String EXHAUSTED_SCULK_FUEL_ID = "exhausted_sculk_fuel";
    public static final String EXHAUSTED_NETHER_COMPOUND_FUEL_ID = "exhausted_nether_compound_fuel";
    public static final String EXHAUSTED_END_MIXTURE_FUEL_ID = "exhausted_end_mixture_fuel";
    public static final String EXHAUSTED_ADVANCED_MIXTURE_FUEL_ID = "exhausted_advanced_mixture_fuel";
    public static final String EXHAUSTED_PURE_FUEL_ID = "exhausted_pure_fuel";
    public static final String EXHAUSTED_DRAGON_FUEL_ID = "exhausted_dragon_fuel";
    public static final String EXHAUSTED_ULTIMATE_MIXTURE_FUEL_ID = "exhausted_ultimate_mixture_fuel";
    /** 活化衰竭液体（7 种，对应 7 种衰竭燃料）：生命活化器缓慢无害化产物，可安全储存、普通液体管道可抽取 */
    public static final String ACTIVATED_EXHAUSTED_SCULK_FUEL_ID = "activated_exhausted_sculk_fuel";
    public static final String ACTIVATED_EXHAUSTED_NETHER_COMPOUND_FUEL_ID = "activated_exhausted_nether_compound_fuel";
    public static final String ACTIVATED_EXHAUSTED_END_MIXTURE_FUEL_ID = "activated_exhausted_end_mixture_fuel";
    public static final String ACTIVATED_EXHAUSTED_ADVANCED_MIXTURE_FUEL_ID = "activated_exhausted_advanced_mixture_fuel";
    public static final String ACTIVATED_EXHAUSTED_PURE_FUEL_ID = "activated_exhausted_pure_fuel";
    public static final String ACTIVATED_EXHAUSTED_DRAGON_FUEL_ID = "activated_exhausted_dragon_fuel";
    public static final String ACTIVATED_EXHAUSTED_ULTIMATE_MIXTURE_FUEL_ID = "activated_exhausted_ultimate_mixture_fuel";

    // ===== 等离子体（聚变燃料聚合器产物，离子体填装器灌入反应棒） =====
    public static final String MIXED_PLASMA_ID = "mixed_plasma";
    public static final String NETHER_PLASMA_ID = "nether_plasma";
    public static final String END_PLASMA_ID = "end_plasma";

    // GUI 液体条 / 液体纹理着色（ARGB）
    public static final int COLOR_NETHER_PURE_ENERGY = 0xFF7FE8C8;
    public static final int COLOR_NETHER_COMPOUND_ENERGY = 0xFFB066FF;
    public static final int COLOR_PURE_FUEL = 0xFFFFD060;
    public static final int COLOR_NETHER_COMPOUND_FUEL = 0xFFFF8040;
    public static final int COLOR_END_MIXTURE_FUEL = 0xFFC070FF;
    public static final int COLOR_DRAGON_FUEL = 0xFF80E0FF;
    public static final int COLOR_SCULK_LIFE_FUEL = 0xFF40E0C0;
    /** 高级混合燃料：末地紫 + 下界橙调和（橙粉） */
    public static final int COLOR_ADVANCED_MIXTURE_FUEL = 0xFFE08090;
    /** 终极混合燃料：巨龙青 + 至纯金调和（炽金） */
    public static final int COLOR_ULTIMATE_MIXTURE_FUEL = 0xFFFFB040;
    /** 衰竭燃料色：原燃料色去饱和（保留色相、灰化提暗，呈现废料观感） */
    public static final int COLOR_EXHAUSTED_SCULK_FUEL = ashen(COLOR_SCULK_LIFE_FUEL);
    public static final int COLOR_EXHAUSTED_NETHER_COMPOUND_FUEL = ashen(COLOR_NETHER_COMPOUND_FUEL);
    public static final int COLOR_EXHAUSTED_END_MIXTURE_FUEL = ashen(COLOR_END_MIXTURE_FUEL);
    public static final int COLOR_EXHAUSTED_ADVANCED_MIXTURE_FUEL = ashen(COLOR_ADVANCED_MIXTURE_FUEL);
    public static final int COLOR_EXHAUSTED_PURE_FUEL = ashen(COLOR_PURE_FUEL);
    public static final int COLOR_EXHAUSTED_DRAGON_FUEL = ashen(COLOR_DRAGON_FUEL);
    public static final int COLOR_EXHAUSTED_ULTIMATE_MIXTURE_FUEL = ashen(COLOR_ULTIMATE_MIXTURE_FUEL);
    /** 活化衰竭液体色：原燃料色向生命青绿调和（复苏生机观感，区别于灰暗废料） */
    public static final int COLOR_ACTIVATED_EXHAUSTED_SCULK_FUEL = revived(COLOR_SCULK_LIFE_FUEL);
    public static final int COLOR_ACTIVATED_EXHAUSTED_NETHER_COMPOUND_FUEL = revived(COLOR_NETHER_COMPOUND_FUEL);
    public static final int COLOR_ACTIVATED_EXHAUSTED_END_MIXTURE_FUEL = revived(COLOR_END_MIXTURE_FUEL);
    public static final int COLOR_ACTIVATED_EXHAUSTED_ADVANCED_MIXTURE_FUEL = revived(COLOR_ADVANCED_MIXTURE_FUEL);
    public static final int COLOR_ACTIVATED_EXHAUSTED_PURE_FUEL = revived(COLOR_PURE_FUEL);
    public static final int COLOR_ACTIVATED_EXHAUSTED_DRAGON_FUEL = revived(COLOR_DRAGON_FUEL);
    public static final int COLOR_ACTIVATED_EXHAUSTED_ULTIMATE_MIXTURE_FUEL = revived(COLOR_ULTIMATE_MIXTURE_FUEL);

    // ===== 等离子体颜色（共用一套等离子体贴图 + 各自着色；高温强光） =====
    /** 混合离子体：世界基础/高级混合/终极混合活化成分聚合 → 亮蓝白 */
    public static final int COLOR_MIXED_PLASMA = 0xFFA0C8FF;
    /** 下界离子体：下界复合/至纯活化成分聚合 → 橙红 */
    public static final int COLOR_NETHER_PLASMA = 0xFFFF8A50;
    /** 末地离子体：末地混合/末地巨龙活化成分聚合 → 紫 */
    public static final int COLOR_END_PLASMA = 0xFFC88AFF;

    private ModFluids() {
    }

    /** 按注册表 ID 获取液体（注册在 Forge 平台完成，运行时必然存在） */
    public static Fluid get(String id) {
        return BuiltInRegistries.FLUID.get(new ResourceLocation(AkaishiMod.MOD_ID, id));
    }

    /** 判断液体是否为衰竭燃料（反应堆废品） */
    public static boolean isExhaustedFuel(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return false;
        }
        return fluid == get(EXHAUSTED_SCULK_FUEL_ID)
                || fluid == get(EXHAUSTED_NETHER_COMPOUND_FUEL_ID)
                || fluid == get(EXHAUSTED_END_MIXTURE_FUEL_ID)
                || fluid == get(EXHAUSTED_ADVANCED_MIXTURE_FUEL_ID)
                || fluid == get(EXHAUSTED_PURE_FUEL_ID)
                || fluid == get(EXHAUSTED_DRAGON_FUEL_ID)
                || fluid == get(EXHAUSTED_ULTIMATE_MIXTURE_FUEL_ID);
    }

    /** 燃料 → 对应衰竭燃料；非反应堆燃料返回空液体 */
    public static Fluid exhaustedFuelFor(Fluid fuel) {
        if (fuel == get(SCULK_LIFE_FUEL_ID)) {
            return get(EXHAUSTED_SCULK_FUEL_ID);
        }
        if (fuel == get(NETHER_COMPOUND_FUEL_ID)) {
            return get(EXHAUSTED_NETHER_COMPOUND_FUEL_ID);
        }
        if (fuel == get(END_MIXTURE_FUEL_ID)) {
            return get(EXHAUSTED_END_MIXTURE_FUEL_ID);
        }
        if (fuel == get(ADVANCED_MIXTURE_FUEL_ID)) {
            return get(EXHAUSTED_ADVANCED_MIXTURE_FUEL_ID);
        }
        if (fuel == get(PURE_FUEL_ID)) {
            return get(EXHAUSTED_PURE_FUEL_ID);
        }
        if (fuel == get(DRAGON_FUEL_ID)) {
            return get(EXHAUSTED_DRAGON_FUEL_ID);
        }
        if (fuel == get(ULTIMATE_MIXTURE_FUEL_ID)) {
            return get(EXHAUSTED_ULTIMATE_MIXTURE_FUEL_ID);
        }
        return Fluids.EMPTY;
    }

    /** 判断液体是否为活化衰竭液体（生命活化器无害化产物） */
    public static boolean isActivatedFuel(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return false;
        }
        return fluid == get(ACTIVATED_EXHAUSTED_SCULK_FUEL_ID)
                || fluid == get(ACTIVATED_EXHAUSTED_NETHER_COMPOUND_FUEL_ID)
                || fluid == get(ACTIVATED_EXHAUSTED_END_MIXTURE_FUEL_ID)
                || fluid == get(ACTIVATED_EXHAUSTED_ADVANCED_MIXTURE_FUEL_ID)
                || fluid == get(ACTIVATED_EXHAUSTED_PURE_FUEL_ID)
                || fluid == get(ACTIVATED_EXHAUSTED_DRAGON_FUEL_ID)
                || fluid == get(ACTIVATED_EXHAUSTED_ULTIMATE_MIXTURE_FUEL_ID);
    }

    /** 判断液体是否为等离子体（聚变燃料聚合器产物，仅等离子体管道/填装器可处理） */
    public static boolean isPlasma(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return false;
        }
        return fluid == get(MIXED_PLASMA_ID) || fluid == get(NETHER_PLASMA_ID) || fluid == get(END_PLASMA_ID);
    }

    /** 衰竭燃料 → 对应活化衰竭液体；非衰竭燃料返回空液体 */
    public static Fluid activatedFuelFor(Fluid exhausted) {
        if (exhausted == get(EXHAUSTED_SCULK_FUEL_ID)) {
            return get(ACTIVATED_EXHAUSTED_SCULK_FUEL_ID);
        }
        if (exhausted == get(EXHAUSTED_NETHER_COMPOUND_FUEL_ID)) {
            return get(ACTIVATED_EXHAUSTED_NETHER_COMPOUND_FUEL_ID);
        }
        if (exhausted == get(EXHAUSTED_END_MIXTURE_FUEL_ID)) {
            return get(ACTIVATED_EXHAUSTED_END_MIXTURE_FUEL_ID);
        }
        if (exhausted == get(EXHAUSTED_ADVANCED_MIXTURE_FUEL_ID)) {
            return get(ACTIVATED_EXHAUSTED_ADVANCED_MIXTURE_FUEL_ID);
        }
        if (exhausted == get(EXHAUSTED_PURE_FUEL_ID)) {
            return get(ACTIVATED_EXHAUSTED_PURE_FUEL_ID);
        }
        if (exhausted == get(EXHAUSTED_DRAGON_FUEL_ID)) {
            return get(ACTIVATED_EXHAUSTED_DRAGON_FUEL_ID);
        }
        if (exhausted == get(EXHAUSTED_ULTIMATE_MIXTURE_FUEL_ID)) {
            return get(ACTIVATED_EXHAUSTED_ULTIMATE_MIXTURE_FUEL_ID);
        }
        return Fluids.EMPTY;
    }

    /** 颜色去饱和：向 0x78 灰度混合 55%，使衰竭燃料呈现"暗淡废料"质感 */
    private static int ashen(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        return 0xFF000000 | ((r * 45 + 120 * 55) / 100 << 16)
                | ((g * 45 + 120 * 55) / 100 << 8)
                | ((b * 45 + 120 * 55) / 100);
    }

    /** 颜色复苏：向生命青绿（0x40E0C0）混合 30%，使活化液体呈现"被生命能量净化"的观感 */
    private static int revived(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        return 0xFF000000 | ((r * 70 + 0x40 * 30) / 100 << 16)
                | ((g * 70 + 0xE0 * 30) / 100 << 8)
                | ((b * 70 + 0xC0 * 30) / 100);
    }
}
