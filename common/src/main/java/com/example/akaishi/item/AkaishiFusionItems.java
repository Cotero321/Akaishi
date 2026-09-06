package com.example.akaishi.item;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * 聚变域物品注册：聚变反应棒、等离子体燃料棒（3 种）、生命灰烬副产物与聚变散热片（6 档）。
 */
public final class AkaishiFusionItems {

    private AkaishiFusionItems() {
    }

    public static RegistrySupplier<Item> fusionRod;
    public static RegistrySupplier<Item> mixedPlasmaRod;
    public static RegistrySupplier<Item> netherPlasmaRod;
    public static RegistrySupplier<Item> endPlasmaRod;
    public static RegistrySupplier<Item> lifeAsh;
    public static RegistrySupplier<Item> fusionHeatSinkTier1;
    public static RegistrySupplier<Item> fusionHeatSinkTier2;
    public static RegistrySupplier<Item> fusionHeatSinkTier3;
    public static RegistrySupplier<Item> fusionHeatSinkTier4;
    public static RegistrySupplier<Item> fusionHeatSinkTier5;
    public static RegistrySupplier<Item> fusionHeatSinkLife;

    public static void register() {
        // 聚变反应棒：赤石锭 + 生命精华合成，作为离子体填装器的燃料载体
        fusionRod = item(ModItems.FUSION_ROD_ID);
        // 等离子体燃料棒（3 种）：填装器将对应等离子体灌入反应棒的产物，带能量 NBT 与类型参数
        mixedPlasmaRod = item(ModItems.MIXED_PLASMA_ROD_ID,
                () -> new AkaishiPlasmaRodItem(AkaishiPlasmaRodItem.RodType.MIXED));
        netherPlasmaRod = item(ModItems.NETHER_PLASMA_ROD_ID,
                () -> new AkaishiPlasmaRodItem(AkaishiPlasmaRodItem.RodType.NETHER));
        endPlasmaRod = item(ModItems.END_PLASMA_ROD_ID,
                () -> new AkaishiPlasmaRodItem(AkaishiPlasmaRodItem.RodType.END));
        // 生命灰烬：聚变堆燃烧副产物（物品输出口推出，生命散热片合成材料）
        lifeAsh = item(ModItems.LIFE_ASH_ID);
        // 聚变散热片（6 档）：放入控制器热量页的消耗品，效率 5%~20%，耐久 8000
        fusionHeatSinkTier1 = item(ModItems.FUSION_HEAT_SINK_TIER1_ID,
                () -> new AkaishiFusionHeatSinkItem(FusionHeatSinkQuality.TIER1));
        fusionHeatSinkTier2 = item(ModItems.FUSION_HEAT_SINK_TIER2_ID,
                () -> new AkaishiFusionHeatSinkItem(FusionHeatSinkQuality.TIER2));
        fusionHeatSinkTier3 = item(ModItems.FUSION_HEAT_SINK_TIER3_ID,
                () -> new AkaishiFusionHeatSinkItem(FusionHeatSinkQuality.TIER3));
        fusionHeatSinkTier4 = item(ModItems.FUSION_HEAT_SINK_TIER4_ID,
                () -> new AkaishiFusionHeatSinkItem(FusionHeatSinkQuality.TIER4));
        fusionHeatSinkTier5 = item(ModItems.FUSION_HEAT_SINK_TIER5_ID,
                () -> new AkaishiFusionHeatSinkItem(FusionHeatSinkQuality.TIER5));
        fusionHeatSinkLife = item(ModItems.FUSION_HEAT_SINK_LIFE_ID,
                () -> new AkaishiFusionHeatSinkItem(FusionHeatSinkQuality.LIFE));
    }

    private static RegistrySupplier<Item> item(String id) {
        return item(id, () -> new Item(new Item.Properties()));
    }

    private static RegistrySupplier<Item> item(String id, Supplier<Item> factory) {
        return RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id), factory);
    }
}
