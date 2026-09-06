package com.example.akaishi.item;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * 反应堆域物品注册：燃料罐（装罐机充装液体燃料）与 6 档散热片消耗品。
 */
public final class AkaishiReactorItems {

    private AkaishiReactorItems() {
    }

    public static RegistrySupplier<Item> fuelCell;
    public static RegistrySupplier<Item> heatSinkPoor;
    public static RegistrySupplier<Item> heatSinkNormal;
    public static RegistrySupplier<Item> heatSinkGood;
    public static RegistrySupplier<Item> heatSinkFine;
    public static RegistrySupplier<Item> heatSinkExquisite;
    public static RegistrySupplier<Item> heatSinkUltimate;

    public static void register() {
        // 燃料罐：装罐机充装液体燃料，容量 10L（10000mb），装液后不可堆叠
        fuelCell = item(ModItems.FUEL_CELL_ID, () -> new AkaishiFuelCellItem(new Item.Properties()));
        // 散热片（5 品质 + 终极）：插入反应堆散热组件，消耗品带耐久
        heatSinkPoor = item(ModItems.HEAT_SINK_POOR_ID, () -> new AkaishiHeatSinkItem(HeatSinkQuality.POOR));
        heatSinkNormal = item(ModItems.HEAT_SINK_NORMAL_ID, () -> new AkaishiHeatSinkItem(HeatSinkQuality.NORMAL));
        heatSinkGood = item(ModItems.HEAT_SINK_GOOD_ID, () -> new AkaishiHeatSinkItem(HeatSinkQuality.GOOD));
        heatSinkFine = item(ModItems.HEAT_SINK_FINE_ID, () -> new AkaishiHeatSinkItem(HeatSinkQuality.FINE));
        heatSinkExquisite = item(ModItems.HEAT_SINK_EXQUISITE_ID, () -> new AkaishiHeatSinkItem(HeatSinkQuality.EXQUISITE));
        heatSinkUltimate = item(ModItems.HEAT_SINK_ULTIMATE_ID, () -> new AkaishiHeatSinkItem(HeatSinkQuality.ULTIMATE));
    }

    private static RegistrySupplier<Item> item(String id) {
        return item(id, () -> new Item(new Item.Properties()));
    }

    private static RegistrySupplier<Item> item(String id, Supplier<Item> factory) {
        return RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id), factory);
    }
}
