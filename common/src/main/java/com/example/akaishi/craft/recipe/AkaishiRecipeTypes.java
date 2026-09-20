package com.example.akaishi.craft.recipe;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * 赤石加工配方类型与序列化器注册（Architectury {@link Registrar}，两端一致）。
 *
 * <p>新增机器配方族的成本 = 在这里加「一对」类型 + 序列化器：
 * <pre>
 * XXX = type("xxx");
 * XXX_SERIALIZER = serializer("xxx", XXX);
 * </pre>
 * 之后写 {@code data/akaishi/recipes/xxx/*.json} 即可，机器侧只需返回对应的
 * {@link RecipeType}（见 {@code AkaishiSingleSlotMachineBlockEntity#recipeType()}）。
 * 注册须在 {@link AkaishiMod#init()} 内、注册表冻结前完成。
 */
public final class AkaishiRecipeTypes {

    private AkaishiRecipeTypes() {
    }

    /** 打粉机：矿物/锭 → 粉末 */
    public static RegistrySupplier<RecipeType<AkaishiItemProcessRecipe>> PULVERIZING;
    public static RegistrySupplier<RecipeSerializer<AkaishiItemProcessRecipe>> PULVERIZING_SERIALIZER;

    /** 压缩机：粉末 → 块 */
    public static RegistrySupplier<RecipeType<AkaishiItemProcessRecipe>> COMPRESSING;
    public static RegistrySupplier<RecipeSerializer<AkaishiItemProcessRecipe>> COMPRESSING_SERIALIZER;

    /** 变化器：矿物 → 矿石基底 */
    public static RegistrySupplier<RecipeType<AkaishiItemProcessRecipe>> TRANSFORMING;
    public static RegistrySupplier<RecipeSerializer<AkaishiItemProcessRecipe>> TRANSFORMING_SERIALIZER;

    /** 植物培养机：种子/茎秆 → 成熟作物（输入保留） */
    public static RegistrySupplier<RecipeType<AkaishiItemProcessRecipe>> PLANT_CULTIVATING;
    public static RegistrySupplier<RecipeSerializer<AkaishiItemProcessRecipe>> PLANT_CULTIVATING_SERIALIZER;

    /** 能量聚合器：物品 + 赤能源 → 物品（赤石锭 / 母岩升级） */
    public static RegistrySupplier<RecipeType<AkaishiEnergyProcessRecipe>> AGGREGATING;
    public static RegistrySupplier<RecipeSerializer<AkaishiEnergyProcessRecipe>> AGGREGATING_SERIALIZER;

    /** 生命提纯机：纯能量 → 生命固态物（无物品输入） */
    public static RegistrySupplier<RecipeType<AkaishiEnergyProcessRecipe>> LIFE_PURIFYING;
    public static RegistrySupplier<RecipeSerializer<AkaishiEnergyProcessRecipe>> LIFE_PURIFYING_SERIALIZER;

    /** 活化分馏器：活化结晶 → 活化成分（副产衰竭结晶） */
    public static RegistrySupplier<RecipeType<AkaishiItemProcessRecipe>> FRACTIONATING;
    public static RegistrySupplier<RecipeSerializer<AkaishiItemProcessRecipe>> FRACTIONATING_SERIALIZER;

    /** 能量液化机：物品（+辅料） + 赤能源 → 流体 */
    public static RegistrySupplier<RecipeType<AkaishiFluidProcessRecipe>> LIQUEFYING;
    public static RegistrySupplier<RecipeSerializer<AkaishiFluidProcessRecipe>> LIQUEFYING_SERIALIZER;

    /** 能量加工机：流体 + 辅料（固态物） + 赤能源 → 流体 */
    public static RegistrySupplier<RecipeType<AkaishiFluidProcessRecipe>> PROCESSING;
    public static RegistrySupplier<RecipeSerializer<AkaishiFluidProcessRecipe>> PROCESSING_SERIALIZER;

    /** 燃料混合器：流体 + 流体 + 赤能源 → 流体 */
    public static RegistrySupplier<RecipeType<AkaishiFluidProcessRecipe>> MIXING;
    public static RegistrySupplier<RecipeSerializer<AkaishiFluidProcessRecipe>> MIXING_SERIALIZER;

    /** 聚变燃料聚合器：活化成分 + 赤能源 → 等离子体 */
    public static RegistrySupplier<RecipeType<AkaishiFluidProcessRecipe>> PLASMA_AGGREGATING;
    public static RegistrySupplier<RecipeSerializer<AkaishiFluidProcessRecipe>> PLASMA_AGGREGATING_SERIALIZER;

    /** 生命离心机：活化燃料 + 赤能源 → 活化结晶（副产衰竭结晶） */
    public static RegistrySupplier<RecipeType<AkaishiFluidProcessRecipe>> CENTRIFUGING;
    public static RegistrySupplier<RecipeSerializer<AkaishiFluidProcessRecipe>> CENTRIFUGING_SERIALIZER;

    /** 生命活化器：衰竭燃料 + 生命能量 → 活化燃料（1:1） */
    public static RegistrySupplier<RecipeType<AkaishiFluidProcessRecipe>> ACTIVATING;
    public static RegistrySupplier<RecipeSerializer<AkaishiFluidProcessRecipe>> ACTIVATING_SERIALIZER;

    /** 由 {@link AkaishiMod#init()} 调用（注册表冻结前） */
    public static void register() {
        PULVERIZING = type("pulverizing");
        PULVERIZING_SERIALIZER = serializer("pulverizing", PULVERIZING);
        COMPRESSING = type("compressing");
        COMPRESSING_SERIALIZER = serializer("compressing", COMPRESSING);
        TRANSFORMING = type("transforming");
        TRANSFORMING_SERIALIZER = serializer("transforming", TRANSFORMING);
        PLANT_CULTIVATING = type("plant_cultivating");
        PLANT_CULTIVATING_SERIALIZER = serializer("plant_cultivating", PLANT_CULTIVATING);
        AGGREGATING = energyType("aggregating");
        AGGREGATING_SERIALIZER = energySerializer("aggregating", AGGREGATING);
        LIFE_PURIFYING = energyType("life_purifying");
        LIFE_PURIFYING_SERIALIZER = energySerializer("life_purifying", LIFE_PURIFYING);
        FRACTIONATING = type("fractionating");
        FRACTIONATING_SERIALIZER = serializer("fractionating", FRACTIONATING);
        LIQUEFYING = fluidType("liquefying");
        LIQUEFYING_SERIALIZER = fluidSerializer("liquefying", LIQUEFYING);
        PROCESSING = fluidType("processing");
        PROCESSING_SERIALIZER = fluidSerializer("processing", PROCESSING);
        MIXING = fluidType("mixing");
        MIXING_SERIALIZER = fluidSerializer("mixing", MIXING);
        PLASMA_AGGREGATING = fluidType("plasma_aggregating");
        PLASMA_AGGREGATING_SERIALIZER = fluidSerializer("plasma_aggregating", PLASMA_AGGREGATING);
        CENTRIFUGING = fluidType("centrifuging");
        CENTRIFUGING_SERIALIZER = fluidSerializer("centrifuging", CENTRIFUGING);
        ACTIVATING = fluidType("activating");
        ACTIVATING_SERIALIZER = fluidSerializer("activating", ACTIVATING);
    }

    /** {@link RecipeType} 为无抽象方法的接口，注册一个匿名实例并让 toString 可读 */
    @SuppressWarnings("unchecked")
    private static RegistrySupplier<RecipeType<AkaishiItemProcessRecipe>> type(String id) {
        Registrar<RecipeType<?>> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.RECIPE_TYPE);
        return (RegistrySupplier<RecipeType<AkaishiItemProcessRecipe>>) (Object) registrar.register(
                new ResourceLocation(AkaishiMod.MOD_ID, id),
                () -> new RecipeType<AkaishiItemProcessRecipe>() {
                    @Override
                    public String toString() {
                        return AkaishiMod.MOD_ID + ":" + id;
                    }
                });
    }

    /**
     * 一个序列化器实例对应一个配方类型；配方实例持有该实例本身，
     * 以保证 {@code Recipe#getSerializer()} 与注册项一致（客户端同步靠它）。
     */
    @SuppressWarnings("unchecked")
    private static RegistrySupplier<RecipeSerializer<AkaishiItemProcessRecipe>> serializer(
            String id, RegistrySupplier<RecipeType<AkaishiItemProcessRecipe>> type) {
        Registrar<RecipeSerializer<?>> registrar = RegistrarManager.get(AkaishiMod.MOD_ID)
                .get(Registries.RECIPE_SERIALIZER);
        AkaishiItemProcessRecipe.Serializer serializer = new AkaishiItemProcessRecipe.Serializer(type);
        return (RegistrySupplier<RecipeSerializer<AkaishiItemProcessRecipe>>) (Object) registrar.register(
                new ResourceLocation(AkaishiMod.MOD_ID, id), () -> serializer);
    }

    /** 能量加工族的类型（物品 + 赤能源 → 物品 / 纯能量 → 物品） */
    @SuppressWarnings("unchecked")
    private static RegistrySupplier<RecipeType<AkaishiEnergyProcessRecipe>> energyType(String id) {
        Registrar<RecipeType<?>> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.RECIPE_TYPE);
        return (RegistrySupplier<RecipeType<AkaishiEnergyProcessRecipe>>) (Object) registrar.register(
                new ResourceLocation(AkaishiMod.MOD_ID, id),
                () -> new RecipeType<AkaishiEnergyProcessRecipe>() {
                    @Override
                    public String toString() {
                        return AkaishiMod.MOD_ID + ":" + id;
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private static RegistrySupplier<RecipeSerializer<AkaishiEnergyProcessRecipe>> energySerializer(
            String id, RegistrySupplier<RecipeType<AkaishiEnergyProcessRecipe>> type) {
        Registrar<RecipeSerializer<?>> registrar = RegistrarManager.get(AkaishiMod.MOD_ID)
                .get(Registries.RECIPE_SERIALIZER);
        AkaishiEnergyProcessRecipe.Serializer serializer = new AkaishiEnergyProcessRecipe.Serializer(type);
        return (RegistrySupplier<RecipeSerializer<AkaishiEnergyProcessRecipe>>) (Object) registrar.register(
                new ResourceLocation(AkaishiMod.MOD_ID, id), () -> serializer);
    }

    /** 流体加工族的类型（物品/流体 → 流体，可另产物品） */
    @SuppressWarnings("unchecked")
    private static RegistrySupplier<RecipeType<AkaishiFluidProcessRecipe>> fluidType(String id) {
        Registrar<RecipeType<?>> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.RECIPE_TYPE);
        return (RegistrySupplier<RecipeType<AkaishiFluidProcessRecipe>>) (Object) registrar.register(
                new ResourceLocation(AkaishiMod.MOD_ID, id),
                () -> new RecipeType<AkaishiFluidProcessRecipe>() {
                    @Override
                    public String toString() {
                        return AkaishiMod.MOD_ID + ":" + id;
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private static RegistrySupplier<RecipeSerializer<AkaishiFluidProcessRecipe>> fluidSerializer(
            String id, RegistrySupplier<RecipeType<AkaishiFluidProcessRecipe>> type) {
        Registrar<RecipeSerializer<?>> registrar = RegistrarManager.get(AkaishiMod.MOD_ID)
                .get(Registries.RECIPE_SERIALIZER);
        AkaishiFluidProcessRecipe.Serializer serializer = new AkaishiFluidProcessRecipe.Serializer(type);
        return (RegistrySupplier<RecipeSerializer<AkaishiFluidProcessRecipe>>) (Object) registrar.register(
                new ResourceLocation(AkaishiMod.MOD_ID, id), () -> serializer);
    }
}
