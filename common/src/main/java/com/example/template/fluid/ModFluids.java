package com.example.template.fluid;

import com.example.template.TemplateMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

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
    /** 幽匿生命燃料：幽匿生命体液化产物（回响碎片+幽匿块+金苹果+赤石精华块合成） */
    public static final String SCULK_LIFE_FUEL_ID = "sculk_life_fuel";
    /** 高级混合燃料：末地混合燃料 + 下界复合燃料 1:1:1 混合 */
    public static final String ADVANCED_MIXTURE_FUEL_ID = "advanced_mixture_fuel";
    /** 终极混合燃料：末地巨龙燃料 + 至纯燃料 1:1:1 混合 */
    public static final String ULTIMATE_MIXTURE_FUEL_ID = "ultimate_mixture_fuel";
    /** 衰竭的生命燃料：反应堆燃烧后的废品（1mb 燃料 → 3mb 废品），仅反应堆废品口/保存桶可储 */
    public static final String EXHAUSTED_LIFE_FUEL_ID = "exhausted_life_fuel";

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
    /** 衰竭的生命燃料：灰褐废料色 */
    public static final int COLOR_EXHAUSTED_LIFE_FUEL = 0xFF80705A;

    private ModFluids() {
    }

    /** 按注册表 ID 获取液体（注册在 Forge 平台完成，运行时必然存在） */
    public static Fluid get(String id) {
        return BuiltInRegistries.FLUID.get(new ResourceLocation(TemplateMod.MOD_ID, id));
    }
}
