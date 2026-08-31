package com.example.template.forge.fluid;

import com.example.template.TemplateMod;
import com.example.template.fluid.ModFluids;
import dev.architectury.core.fluid.ArchitecturyFlowingFluid;
import dev.architectury.core.fluid.SimpleArchitecturyFluidAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * 液体注册（Forge 平台）。
 * 液体必须注册在 Forge 端：FluidType 是 Forge 专属概念，Architectury 在 Forge 平台
 * 将 {@link ArchitecturyFlowingFluid} 替换为 ForgeFlowingFluid 实现，自动为液体创建 FluidType。
 * 所有液体均不注册液体方块与桶（block/bucket 属性默认为空气），因此不会在世界中流动/放置，
 * 只能通过机器与液体管道传输——符合"能量/燃料液体"的定位。
 * common 模块通过 {@link ModFluids#get(String)} 按注册表 ID 取用。
 * attributes 用惰性方法创建：其 supplier 前向引用同类的液体注册对象，字段初始化器中
 * 直接引用会触发 javac"非法前向引用"，改为方法体内延迟求值（注册事件时字段已就绪）。
 */
public final class ModFluidsImpl {

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, TemplateMod.MOD_ID);

    // ===== 下界至纯能量：下界之星液化产物（青绿色，发光，轻于空气） =====
    private static SimpleArchitecturyFluidAttributes netherPureEnergyAttrs;

    private static SimpleArchitecturyFluidAttributes netherPureEnergyAttrs() {
        if (netherPureEnergyAttrs == null) {
            netherPureEnergyAttrs = SimpleArchitecturyFluidAttributes.ofSupplier(() -> NETHER_PURE_ENERGY_FLOWING, () -> NETHER_PURE_ENERGY)
                    .color(ModFluids.COLOR_NETHER_PURE_ENERGY)
                    .density(-300).viscosity(400).temperature(300).luminosity(10)
                    .sourceTexture(new ResourceLocation(TemplateMod.MOD_ID, "block/fluid/nether_pure_energy_still"))
                    .flowingTexture(new ResourceLocation(TemplateMod.MOD_ID, "block/fluid/nether_pure_energy_flow"));
        }
        return netherPureEnergyAttrs;
    }

    // ===== 下界复合能量：凋零玫瑰液化产物（紫色，发光，轻于空气） =====
    private static SimpleArchitecturyFluidAttributes netherCompoundEnergyAttrs;

    private static SimpleArchitecturyFluidAttributes netherCompoundEnergyAttrs() {
        if (netherCompoundEnergyAttrs == null) {
            netherCompoundEnergyAttrs = SimpleArchitecturyFluidAttributes.ofSupplier(() -> NETHER_COMPOUND_ENERGY_FLOWING, () -> NETHER_COMPOUND_ENERGY)
                    .color(ModFluids.COLOR_NETHER_COMPOUND_ENERGY)
                    .density(-300).viscosity(500).temperature(300).luminosity(8)
                    .sourceTexture(new ResourceLocation(TemplateMod.MOD_ID, "block/fluid/nether_compound_energy_still"))
                    .flowingTexture(new ResourceLocation(TemplateMod.MOD_ID, "block/fluid/nether_compound_energy_flow"));
        }
        return netherCompoundEnergyAttrs;
    }

    // ===== 至纯燃料：生命固态物 + 下界至纯能量加工产物（金黄色液态燃料） =====
    private static SimpleArchitecturyFluidAttributes pureFuelAttrs;

    private static SimpleArchitecturyFluidAttributes pureFuelAttrs() {
        if (pureFuelAttrs == null) {
            pureFuelAttrs = SimpleArchitecturyFluidAttributes.ofSupplier(() -> PURE_FUEL_FLOWING, () -> PURE_FUEL)
                    .color(ModFluids.COLOR_PURE_FUEL)
                    .density(900).viscosity(1500).temperature(400).luminosity(6)
                    .sourceTexture(new ResourceLocation(TemplateMod.MOD_ID, "block/fluid/pure_fuel_still"))
                    .flowingTexture(new ResourceLocation(TemplateMod.MOD_ID, "block/fluid/pure_fuel_flow"));
        }
        return pureFuelAttrs;
    }

    // ===== 下界复合燃料：生命固态物 + 下界复合能量加工产物（橙红稠重燃料） =====
    private static SimpleArchitecturyFluidAttributes netherCompoundFuelAttrs;

    private static SimpleArchitecturyFluidAttributes netherCompoundFuelAttrs() {
        if (netherCompoundFuelAttrs == null) {
            netherCompoundFuelAttrs = SimpleArchitecturyFluidAttributes.ofSupplier(() -> NETHER_COMPOUND_FUEL_FLOWING, () -> NETHER_COMPOUND_FUEL)
                    .color(ModFluids.COLOR_NETHER_COMPOUND_FUEL)
                    .density(1200).viscosity(2200).temperature(500).luminosity(5)
                    .sourceTexture(new ResourceLocation(TemplateMod.MOD_ID, "block/fluid/nether_compound_fuel_still"))
                    .flowingTexture(new ResourceLocation(TemplateMod.MOD_ID, "block/fluid/nether_compound_fuel_flow"));
        }
        return netherCompoundFuelAttrs;
    }

    // ===== 高级混合燃料：末地紫 + 下界橙调和（燃料混合器 1:1:1 产物） =====
    private static SimpleArchitecturyFluidAttributes advancedMixtureFuelAttrs;

    private static SimpleArchitecturyFluidAttributes advancedMixtureFuelAttrs() {
        if (advancedMixtureFuelAttrs == null) {
            advancedMixtureFuelAttrs = SimpleArchitecturyFluidAttributes.ofSupplier(() -> ADVANCED_MIXTURE_FUEL_FLOWING, () -> ADVANCED_MIXTURE_FUEL)
                    .color(ModFluids.COLOR_ADVANCED_MIXTURE_FUEL)
                    .density(1150).viscosity(2200).temperature(500).luminosity(6)
                    .sourceTexture(new ResourceLocation(TemplateMod.MOD_ID, "block/fluid/advanced_mixture_fuel_still"))
                    .flowingTexture(new ResourceLocation(TemplateMod.MOD_ID, "block/fluid/advanced_mixture_fuel_flow"));
        }
        return advancedMixtureFuelAttrs;
    }

    // ===== 终极混合燃料：巨龙青 + 至纯金调和（炽金浓缩燃料） =====
    private static SimpleArchitecturyFluidAttributes ultimateMixtureFuelAttrs;

    private static SimpleArchitecturyFluidAttributes ultimateMixtureFuelAttrs() {
        if (ultimateMixtureFuelAttrs == null) {
            ultimateMixtureFuelAttrs = SimpleArchitecturyFluidAttributes.ofSupplier(() -> ULTIMATE_MIXTURE_FUEL_FLOWING, () -> ULTIMATE_MIXTURE_FUEL)
                    .color(ModFluids.COLOR_ULTIMATE_MIXTURE_FUEL)
                    .density(1200).viscosity(2500).temperature(700).luminosity(9)
                    .sourceTexture(new ResourceLocation(TemplateMod.MOD_ID, "block/fluid/ultimate_mixture_fuel_still"))
                    .flowingTexture(new ResourceLocation(TemplateMod.MOD_ID, "block/fluid/ultimate_mixture_fuel_flow"));
        }
        return ultimateMixtureFuelAttrs;
    }

    // ===== 末地混合燃料：末地混合物液化产物（紫色稠重燃料） =====
    private static SimpleArchitecturyFluidAttributes endMixtureFuelAttrs;

    private static SimpleArchitecturyFluidAttributes endMixtureFuelAttrs() {
        if (endMixtureFuelAttrs == null) {
            endMixtureFuelAttrs = SimpleArchitecturyFluidAttributes.ofSupplier(() -> END_MIXTURE_FUEL_FLOWING, () -> END_MIXTURE_FUEL)
                    .color(ModFluids.COLOR_END_MIXTURE_FUEL)
                    .density(1100).viscosity(2000).temperature(450).luminosity(5)
                    .sourceTexture(new ResourceLocation(TemplateMod.MOD_ID, "block/fluid/end_mixture_fuel_still"))
                    .flowingTexture(new ResourceLocation(TemplateMod.MOD_ID, "block/fluid/end_mixture_fuel_flow"));
        }
        return endMixtureFuelAttrs;
    }

    // ===== 末地巨龙燃料：巨龙混合物液化产物（龙息青澈燃料） =====
    private static SimpleArchitecturyFluidAttributes dragonFuelAttrs;

    private static SimpleArchitecturyFluidAttributes dragonFuelAttrs() {
        if (dragonFuelAttrs == null) {
            dragonFuelAttrs = SimpleArchitecturyFluidAttributes.ofSupplier(() -> DRAGON_FUEL_FLOWING, () -> DRAGON_FUEL)
                    .color(ModFluids.COLOR_DRAGON_FUEL)
                    .density(1000).viscosity(1800).temperature(600).luminosity(7)
                    .sourceTexture(new ResourceLocation(TemplateMod.MOD_ID, "block/fluid/dragon_fuel_still"))
                    .flowingTexture(new ResourceLocation(TemplateMod.MOD_ID, "block/fluid/dragon_fuel_flow"));
        }
        return dragonFuelAttrs;
    }

    // ===== 幽匿生命燃料：幽匿生命体液化产物（幽匿青绿脉动燃料） =====
    private static SimpleArchitecturyFluidAttributes sculkLifeFuelAttrs;

    private static SimpleArchitecturyFluidAttributes sculkLifeFuelAttrs() {
        if (sculkLifeFuelAttrs == null) {
            sculkLifeFuelAttrs = SimpleArchitecturyFluidAttributes.ofSupplier(() -> SCULK_LIFE_FUEL_FLOWING, () -> SCULK_LIFE_FUEL)
                    .color(ModFluids.COLOR_SCULK_LIFE_FUEL)
                    .density(1050).viscosity(1900).temperature(350).luminosity(8)
                    .sourceTexture(new ResourceLocation(TemplateMod.MOD_ID, "block/fluid/sculk_life_fuel_still"))
                    .flowingTexture(new ResourceLocation(TemplateMod.MOD_ID, "block/fluid/sculk_life_fuel_flow"));
        }
        return sculkLifeFuelAttrs;
    }

    // ===== 衰竭燃料（7 种）：反应堆燃烧废品（共用废料贴图 + 各自去饱和着色，稠重无光） =====

    /** 统一构建衰竭燃料 attributes：物理属性一致，仅颜色区分（贴图复用同一废料纹理） */
    private static SimpleArchitecturyFluidAttributes exhaustedAttrs(Supplier<RegistryObject<Fluid>> flowing, Supplier<RegistryObject<Fluid>> source, int color) {
        return SimpleArchitecturyFluidAttributes.ofSupplier(flowing, source)
                .color(color)
                .density(1400).viscosity(2600).temperature(200).luminosity(0)
                .sourceTexture(new ResourceLocation(TemplateMod.MOD_ID, "block/fluid/exhausted_life_fuel_still"))
                .flowingTexture(new ResourceLocation(TemplateMod.MOD_ID, "block/fluid/exhausted_life_fuel_flow"));
    }

    // ===== 活化衰竭液体（7 种）：生命活化器无害化产物（共用活化贴图 + 各自复苏着色，澄清微光） =====

    /** 统一构建活化衰竭液体 attributes：物理属性一致，仅颜色区分（贴图复用同一活化纹理） */
    private static SimpleArchitecturyFluidAttributes activatedAttrs(Supplier<RegistryObject<Fluid>> flowing, Supplier<RegistryObject<Fluid>> source, int color) {
        return SimpleArchitecturyFluidAttributes.ofSupplier(flowing, source)
                .color(color)
                .density(1050).viscosity(1200).temperature(300).luminosity(3)
                .sourceTexture(new ResourceLocation(TemplateMod.MOD_ID, "block/fluid/activated_life_fuel_still"))
                .flowingTexture(new ResourceLocation(TemplateMod.MOD_ID, "block/fluid/activated_life_fuel_flow"));
    }

    // 各衰竭燃料的惰性 attrs（方法体内引用字段合法，规避字段初始化期"非法前向引用"）
    private static SimpleArchitecturyFluidAttributes sculkExhaustedFuelAttrs;

    private static SimpleArchitecturyFluidAttributes sculkExhaustedFuelAttrs() {
        if (sculkExhaustedFuelAttrs == null) {
            sculkExhaustedFuelAttrs = exhaustedAttrs(() -> EXHAUSTED_SCULK_FUEL_FLOWING, () -> EXHAUSTED_SCULK_FUEL, ModFluids.COLOR_EXHAUSTED_SCULK_FUEL);
        }
        return sculkExhaustedFuelAttrs;
    }

    private static SimpleArchitecturyFluidAttributes netherCompoundExhaustedFuelAttrs;

    private static SimpleArchitecturyFluidAttributes netherCompoundExhaustedFuelAttrs() {
        if (netherCompoundExhaustedFuelAttrs == null) {
            netherCompoundExhaustedFuelAttrs = exhaustedAttrs(() -> EXHAUSTED_NETHER_COMPOUND_FUEL_FLOWING, () -> EXHAUSTED_NETHER_COMPOUND_FUEL, ModFluids.COLOR_EXHAUSTED_NETHER_COMPOUND_FUEL);
        }
        return netherCompoundExhaustedFuelAttrs;
    }

    private static SimpleArchitecturyFluidAttributes endMixtureExhaustedFuelAttrs;

    private static SimpleArchitecturyFluidAttributes endMixtureExhaustedFuelAttrs() {
        if (endMixtureExhaustedFuelAttrs == null) {
            endMixtureExhaustedFuelAttrs = exhaustedAttrs(() -> EXHAUSTED_END_MIXTURE_FUEL_FLOWING, () -> EXHAUSTED_END_MIXTURE_FUEL, ModFluids.COLOR_EXHAUSTED_END_MIXTURE_FUEL);
        }
        return endMixtureExhaustedFuelAttrs;
    }

    private static SimpleArchitecturyFluidAttributes advancedMixtureExhaustedFuelAttrs;

    private static SimpleArchitecturyFluidAttributes advancedMixtureExhaustedFuelAttrs() {
        if (advancedMixtureExhaustedFuelAttrs == null) {
            advancedMixtureExhaustedFuelAttrs = exhaustedAttrs(() -> EXHAUSTED_ADVANCED_MIXTURE_FUEL_FLOWING, () -> EXHAUSTED_ADVANCED_MIXTURE_FUEL, ModFluids.COLOR_EXHAUSTED_ADVANCED_MIXTURE_FUEL);
        }
        return advancedMixtureExhaustedFuelAttrs;
    }

    private static SimpleArchitecturyFluidAttributes pureExhaustedFuelAttrs;

    private static SimpleArchitecturyFluidAttributes pureExhaustedFuelAttrs() {
        if (pureExhaustedFuelAttrs == null) {
            pureExhaustedFuelAttrs = exhaustedAttrs(() -> EXHAUSTED_PURE_FUEL_FLOWING, () -> EXHAUSTED_PURE_FUEL, ModFluids.COLOR_EXHAUSTED_PURE_FUEL);
        }
        return pureExhaustedFuelAttrs;
    }

    private static SimpleArchitecturyFluidAttributes dragonExhaustedFuelAttrs;

    private static SimpleArchitecturyFluidAttributes dragonExhaustedFuelAttrs() {
        if (dragonExhaustedFuelAttrs == null) {
            dragonExhaustedFuelAttrs = exhaustedAttrs(() -> EXHAUSTED_DRAGON_FUEL_FLOWING, () -> EXHAUSTED_DRAGON_FUEL, ModFluids.COLOR_EXHAUSTED_DRAGON_FUEL);
        }
        return dragonExhaustedFuelAttrs;
    }

    private static SimpleArchitecturyFluidAttributes ultimateMixtureExhaustedFuelAttrs;

    private static SimpleArchitecturyFluidAttributes ultimateMixtureExhaustedFuelAttrs() {
        if (ultimateMixtureExhaustedFuelAttrs == null) {
            ultimateMixtureExhaustedFuelAttrs = exhaustedAttrs(() -> EXHAUSTED_ULTIMATE_MIXTURE_FUEL_FLOWING, () -> EXHAUSTED_ULTIMATE_MIXTURE_FUEL, ModFluids.COLOR_EXHAUSTED_ULTIMATE_MIXTURE_FUEL);
        }
        return ultimateMixtureExhaustedFuelAttrs;
    }

    // 各活化衰竭液体的惰性 attrs（同衰竭燃料模式）
    private static SimpleArchitecturyFluidAttributes sculkActivatedFuelAttrs;

    private static SimpleArchitecturyFluidAttributes sculkActivatedFuelAttrs() {
        if (sculkActivatedFuelAttrs == null) {
            sculkActivatedFuelAttrs = activatedAttrs(() -> ACTIVATED_EXHAUSTED_SCULK_FUEL_FLOWING, () -> ACTIVATED_EXHAUSTED_SCULK_FUEL, ModFluids.COLOR_ACTIVATED_EXHAUSTED_SCULK_FUEL);
        }
        return sculkActivatedFuelAttrs;
    }

    private static SimpleArchitecturyFluidAttributes netherCompoundActivatedFuelAttrs;

    private static SimpleArchitecturyFluidAttributes netherCompoundActivatedFuelAttrs() {
        if (netherCompoundActivatedFuelAttrs == null) {
            netherCompoundActivatedFuelAttrs = activatedAttrs(() -> ACTIVATED_EXHAUSTED_NETHER_COMPOUND_FUEL_FLOWING, () -> ACTIVATED_EXHAUSTED_NETHER_COMPOUND_FUEL, ModFluids.COLOR_ACTIVATED_EXHAUSTED_NETHER_COMPOUND_FUEL);
        }
        return netherCompoundActivatedFuelAttrs;
    }

    private static SimpleArchitecturyFluidAttributes endMixtureActivatedFuelAttrs;

    private static SimpleArchitecturyFluidAttributes endMixtureActivatedFuelAttrs() {
        if (endMixtureActivatedFuelAttrs == null) {
            endMixtureActivatedFuelAttrs = activatedAttrs(() -> ACTIVATED_EXHAUSTED_END_MIXTURE_FUEL_FLOWING, () -> ACTIVATED_EXHAUSTED_END_MIXTURE_FUEL, ModFluids.COLOR_ACTIVATED_EXHAUSTED_END_MIXTURE_FUEL);
        }
        return endMixtureActivatedFuelAttrs;
    }

    private static SimpleArchitecturyFluidAttributes advancedMixtureActivatedFuelAttrs;

    private static SimpleArchitecturyFluidAttributes advancedMixtureActivatedFuelAttrs() {
        if (advancedMixtureActivatedFuelAttrs == null) {
            advancedMixtureActivatedFuelAttrs = activatedAttrs(() -> ACTIVATED_EXHAUSTED_ADVANCED_MIXTURE_FUEL_FLOWING, () -> ACTIVATED_EXHAUSTED_ADVANCED_MIXTURE_FUEL, ModFluids.COLOR_ACTIVATED_EXHAUSTED_ADVANCED_MIXTURE_FUEL);
        }
        return advancedMixtureActivatedFuelAttrs;
    }

    private static SimpleArchitecturyFluidAttributes pureActivatedFuelAttrs;

    private static SimpleArchitecturyFluidAttributes pureActivatedFuelAttrs() {
        if (pureActivatedFuelAttrs == null) {
            pureActivatedFuelAttrs = activatedAttrs(() -> ACTIVATED_EXHAUSTED_PURE_FUEL_FLOWING, () -> ACTIVATED_EXHAUSTED_PURE_FUEL, ModFluids.COLOR_ACTIVATED_EXHAUSTED_PURE_FUEL);
        }
        return pureActivatedFuelAttrs;
    }

    private static SimpleArchitecturyFluidAttributes dragonActivatedFuelAttrs;

    private static SimpleArchitecturyFluidAttributes dragonActivatedFuelAttrs() {
        if (dragonActivatedFuelAttrs == null) {
            dragonActivatedFuelAttrs = activatedAttrs(() -> ACTIVATED_EXHAUSTED_DRAGON_FUEL_FLOWING, () -> ACTIVATED_EXHAUSTED_DRAGON_FUEL, ModFluids.COLOR_ACTIVATED_EXHAUSTED_DRAGON_FUEL);
        }
        return dragonActivatedFuelAttrs;
    }

    private static SimpleArchitecturyFluidAttributes ultimateMixtureActivatedFuelAttrs;

    private static SimpleArchitecturyFluidAttributes ultimateMixtureActivatedFuelAttrs() {
        if (ultimateMixtureActivatedFuelAttrs == null) {
            ultimateMixtureActivatedFuelAttrs = activatedAttrs(() -> ACTIVATED_EXHAUSTED_ULTIMATE_MIXTURE_FUEL_FLOWING, () -> ACTIVATED_EXHAUSTED_ULTIMATE_MIXTURE_FUEL, ModFluids.COLOR_ACTIVATED_EXHAUSTED_ULTIMATE_MIXTURE_FUEL);
        }
        return ultimateMixtureActivatedFuelAttrs;
    }

    public static final RegistryObject<Fluid> NETHER_PURE_ENERGY =
            FLUIDS.register(ModFluids.NETHER_PURE_ENERGY_ID, () -> new ArchitecturyFlowingFluid.Source(netherPureEnergyAttrs()));
    public static final RegistryObject<Fluid> NETHER_PURE_ENERGY_FLOWING =
            FLUIDS.register(ModFluids.NETHER_PURE_ENERGY_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(netherPureEnergyAttrs()));

    public static final RegistryObject<Fluid> NETHER_COMPOUND_ENERGY =
            FLUIDS.register(ModFluids.NETHER_COMPOUND_ENERGY_ID, () -> new ArchitecturyFlowingFluid.Source(netherCompoundEnergyAttrs()));
    public static final RegistryObject<Fluid> NETHER_COMPOUND_ENERGY_FLOWING =
            FLUIDS.register(ModFluids.NETHER_COMPOUND_ENERGY_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(netherCompoundEnergyAttrs()));

    public static final RegistryObject<Fluid> PURE_FUEL =
            FLUIDS.register(ModFluids.PURE_FUEL_ID, () -> new ArchitecturyFlowingFluid.Source(pureFuelAttrs()));
    public static final RegistryObject<Fluid> PURE_FUEL_FLOWING =
            FLUIDS.register(ModFluids.PURE_FUEL_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(pureFuelAttrs()));

    public static final RegistryObject<Fluid> NETHER_COMPOUND_FUEL =
            FLUIDS.register(ModFluids.NETHER_COMPOUND_FUEL_ID, () -> new ArchitecturyFlowingFluid.Source(netherCompoundFuelAttrs()));
    public static final RegistryObject<Fluid> NETHER_COMPOUND_FUEL_FLOWING =
            FLUIDS.register(ModFluids.NETHER_COMPOUND_FUEL_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(netherCompoundFuelAttrs()));

    public static final RegistryObject<Fluid> END_MIXTURE_FUEL =
            FLUIDS.register(ModFluids.END_MIXTURE_FUEL_ID, () -> new ArchitecturyFlowingFluid.Source(endMixtureFuelAttrs()));
    public static final RegistryObject<Fluid> END_MIXTURE_FUEL_FLOWING =
            FLUIDS.register(ModFluids.END_MIXTURE_FUEL_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(endMixtureFuelAttrs()));

    public static final RegistryObject<Fluid> DRAGON_FUEL =
            FLUIDS.register(ModFluids.DRAGON_FUEL_ID, () -> new ArchitecturyFlowingFluid.Source(dragonFuelAttrs()));
    public static final RegistryObject<Fluid> DRAGON_FUEL_FLOWING =
            FLUIDS.register(ModFluids.DRAGON_FUEL_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(dragonFuelAttrs()));

    public static final RegistryObject<Fluid> SCULK_LIFE_FUEL =
            FLUIDS.register(ModFluids.SCULK_LIFE_FUEL_ID, () -> new ArchitecturyFlowingFluid.Source(sculkLifeFuelAttrs()));
    public static final RegistryObject<Fluid> SCULK_LIFE_FUEL_FLOWING =
            FLUIDS.register(ModFluids.SCULK_LIFE_FUEL_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(sculkLifeFuelAttrs()));

    public static final RegistryObject<Fluid> ADVANCED_MIXTURE_FUEL =
            FLUIDS.register(ModFluids.ADVANCED_MIXTURE_FUEL_ID, () -> new ArchitecturyFlowingFluid.Source(advancedMixtureFuelAttrs()));
    public static final RegistryObject<Fluid> ADVANCED_MIXTURE_FUEL_FLOWING =
            FLUIDS.register(ModFluids.ADVANCED_MIXTURE_FUEL_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(advancedMixtureFuelAttrs()));

    public static final RegistryObject<Fluid> ULTIMATE_MIXTURE_FUEL =
            FLUIDS.register(ModFluids.ULTIMATE_MIXTURE_FUEL_ID, () -> new ArchitecturyFlowingFluid.Source(ultimateMixtureFuelAttrs()));
    public static final RegistryObject<Fluid> ULTIMATE_MIXTURE_FUEL_FLOWING =
            FLUIDS.register(ModFluids.ULTIMATE_MIXTURE_FUEL_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(ultimateMixtureFuelAttrs()));

    public static final RegistryObject<Fluid> EXHAUSTED_SCULK_FUEL =
            FLUIDS.register(ModFluids.EXHAUSTED_SCULK_FUEL_ID, () -> new ArchitecturyFlowingFluid.Source(sculkExhaustedFuelAttrs()));
    public static final RegistryObject<Fluid> EXHAUSTED_SCULK_FUEL_FLOWING =
            FLUIDS.register(ModFluids.EXHAUSTED_SCULK_FUEL_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(sculkExhaustedFuelAttrs()));

    public static final RegistryObject<Fluid> EXHAUSTED_NETHER_COMPOUND_FUEL =
            FLUIDS.register(ModFluids.EXHAUSTED_NETHER_COMPOUND_FUEL_ID, () -> new ArchitecturyFlowingFluid.Source(netherCompoundExhaustedFuelAttrs()));
    public static final RegistryObject<Fluid> EXHAUSTED_NETHER_COMPOUND_FUEL_FLOWING =
            FLUIDS.register(ModFluids.EXHAUSTED_NETHER_COMPOUND_FUEL_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(netherCompoundExhaustedFuelAttrs()));

    public static final RegistryObject<Fluid> EXHAUSTED_END_MIXTURE_FUEL =
            FLUIDS.register(ModFluids.EXHAUSTED_END_MIXTURE_FUEL_ID, () -> new ArchitecturyFlowingFluid.Source(endMixtureExhaustedFuelAttrs()));
    public static final RegistryObject<Fluid> EXHAUSTED_END_MIXTURE_FUEL_FLOWING =
            FLUIDS.register(ModFluids.EXHAUSTED_END_MIXTURE_FUEL_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(endMixtureExhaustedFuelAttrs()));

    public static final RegistryObject<Fluid> EXHAUSTED_ADVANCED_MIXTURE_FUEL =
            FLUIDS.register(ModFluids.EXHAUSTED_ADVANCED_MIXTURE_FUEL_ID, () -> new ArchitecturyFlowingFluid.Source(advancedMixtureExhaustedFuelAttrs()));
    public static final RegistryObject<Fluid> EXHAUSTED_ADVANCED_MIXTURE_FUEL_FLOWING =
            FLUIDS.register(ModFluids.EXHAUSTED_ADVANCED_MIXTURE_FUEL_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(advancedMixtureExhaustedFuelAttrs()));

    public static final RegistryObject<Fluid> EXHAUSTED_PURE_FUEL =
            FLUIDS.register(ModFluids.EXHAUSTED_PURE_FUEL_ID, () -> new ArchitecturyFlowingFluid.Source(pureExhaustedFuelAttrs()));
    public static final RegistryObject<Fluid> EXHAUSTED_PURE_FUEL_FLOWING =
            FLUIDS.register(ModFluids.EXHAUSTED_PURE_FUEL_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(pureExhaustedFuelAttrs()));

    public static final RegistryObject<Fluid> EXHAUSTED_DRAGON_FUEL =
            FLUIDS.register(ModFluids.EXHAUSTED_DRAGON_FUEL_ID, () -> new ArchitecturyFlowingFluid.Source(dragonExhaustedFuelAttrs()));
    public static final RegistryObject<Fluid> EXHAUSTED_DRAGON_FUEL_FLOWING =
            FLUIDS.register(ModFluids.EXHAUSTED_DRAGON_FUEL_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(dragonExhaustedFuelAttrs()));

    public static final RegistryObject<Fluid> EXHAUSTED_ULTIMATE_MIXTURE_FUEL =
            FLUIDS.register(ModFluids.EXHAUSTED_ULTIMATE_MIXTURE_FUEL_ID, () -> new ArchitecturyFlowingFluid.Source(ultimateMixtureExhaustedFuelAttrs()));
    public static final RegistryObject<Fluid> EXHAUSTED_ULTIMATE_MIXTURE_FUEL_FLOWING =
            FLUIDS.register(ModFluids.EXHAUSTED_ULTIMATE_MIXTURE_FUEL_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(ultimateMixtureExhaustedFuelAttrs()));

    // ===== 活化衰竭液体（7 种，与衰竭燃料一一对应） =====
    public static final RegistryObject<Fluid> ACTIVATED_EXHAUSTED_SCULK_FUEL =
            FLUIDS.register(ModFluids.ACTIVATED_EXHAUSTED_SCULK_FUEL_ID, () -> new ArchitecturyFlowingFluid.Source(sculkActivatedFuelAttrs()));
    public static final RegistryObject<Fluid> ACTIVATED_EXHAUSTED_SCULK_FUEL_FLOWING =
            FLUIDS.register(ModFluids.ACTIVATED_EXHAUSTED_SCULK_FUEL_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(sculkActivatedFuelAttrs()));

    public static final RegistryObject<Fluid> ACTIVATED_EXHAUSTED_NETHER_COMPOUND_FUEL =
            FLUIDS.register(ModFluids.ACTIVATED_EXHAUSTED_NETHER_COMPOUND_FUEL_ID, () -> new ArchitecturyFlowingFluid.Source(netherCompoundActivatedFuelAttrs()));
    public static final RegistryObject<Fluid> ACTIVATED_EXHAUSTED_NETHER_COMPOUND_FUEL_FLOWING =
            FLUIDS.register(ModFluids.ACTIVATED_EXHAUSTED_NETHER_COMPOUND_FUEL_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(netherCompoundActivatedFuelAttrs()));

    public static final RegistryObject<Fluid> ACTIVATED_EXHAUSTED_END_MIXTURE_FUEL =
            FLUIDS.register(ModFluids.ACTIVATED_EXHAUSTED_END_MIXTURE_FUEL_ID, () -> new ArchitecturyFlowingFluid.Source(endMixtureActivatedFuelAttrs()));
    public static final RegistryObject<Fluid> ACTIVATED_EXHAUSTED_END_MIXTURE_FUEL_FLOWING =
            FLUIDS.register(ModFluids.ACTIVATED_EXHAUSTED_END_MIXTURE_FUEL_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(endMixtureActivatedFuelAttrs()));

    public static final RegistryObject<Fluid> ACTIVATED_EXHAUSTED_ADVANCED_MIXTURE_FUEL =
            FLUIDS.register(ModFluids.ACTIVATED_EXHAUSTED_ADVANCED_MIXTURE_FUEL_ID, () -> new ArchitecturyFlowingFluid.Source(advancedMixtureActivatedFuelAttrs()));
    public static final RegistryObject<Fluid> ACTIVATED_EXHAUSTED_ADVANCED_MIXTURE_FUEL_FLOWING =
            FLUIDS.register(ModFluids.ACTIVATED_EXHAUSTED_ADVANCED_MIXTURE_FUEL_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(advancedMixtureActivatedFuelAttrs()));

    public static final RegistryObject<Fluid> ACTIVATED_EXHAUSTED_PURE_FUEL =
            FLUIDS.register(ModFluids.ACTIVATED_EXHAUSTED_PURE_FUEL_ID, () -> new ArchitecturyFlowingFluid.Source(pureActivatedFuelAttrs()));
    public static final RegistryObject<Fluid> ACTIVATED_EXHAUSTED_PURE_FUEL_FLOWING =
            FLUIDS.register(ModFluids.ACTIVATED_EXHAUSTED_PURE_FUEL_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(pureActivatedFuelAttrs()));

    public static final RegistryObject<Fluid> ACTIVATED_EXHAUSTED_DRAGON_FUEL =
            FLUIDS.register(ModFluids.ACTIVATED_EXHAUSTED_DRAGON_FUEL_ID, () -> new ArchitecturyFlowingFluid.Source(dragonActivatedFuelAttrs()));
    public static final RegistryObject<Fluid> ACTIVATED_EXHAUSTED_DRAGON_FUEL_FLOWING =
            FLUIDS.register(ModFluids.ACTIVATED_EXHAUSTED_DRAGON_FUEL_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(dragonActivatedFuelAttrs()));

    public static final RegistryObject<Fluid> ACTIVATED_EXHAUSTED_ULTIMATE_MIXTURE_FUEL =
            FLUIDS.register(ModFluids.ACTIVATED_EXHAUSTED_ULTIMATE_MIXTURE_FUEL_ID, () -> new ArchitecturyFlowingFluid.Source(ultimateMixtureActivatedFuelAttrs()));
    public static final RegistryObject<Fluid> ACTIVATED_EXHAUSTED_ULTIMATE_MIXTURE_FUEL_FLOWING =
            FLUIDS.register(ModFluids.ACTIVATED_EXHAUSTED_ULTIMATE_MIXTURE_FUEL_ID + "_flowing", () -> new ArchitecturyFlowingFluid.Flowing(ultimateMixtureActivatedFuelAttrs()));

    private ModFluidsImpl() {
    }

    /** 将液体注册表挂到 Forge mod 事件总线（在平台入口构造器调用） */
    public static void register(IEventBus modBus) {
        FLUIDS.register(modBus);
    }
}
