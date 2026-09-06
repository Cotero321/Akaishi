package com.example.akaishi.item;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * 转基因域物品注册：转基因植物（凋零藤）体系的种子与收获物。
 * 均由转基因工厂（凋零骷髅基因 + 缠怨藤 + 凋零玫瑰 + 固态物）产出。
 */
public final class AkaishiTransgeneItems {

    private AkaishiTransgeneItems() {
    }

    /** 转基因植物：凋零藤种子（右键种植生成根，根挖掘返还本种子） */
    public static RegistrySupplier<Item> akaishiWitherSeed;
    /** 转基因植物：凋零藤凝聚体（成熟藤收获，凋零系高级原料/介质） */
    public static RegistrySupplier<Item> akaishiWitherCondensate;

    public static void register() {
        akaishiWitherSeed = item("akaishi_wither_seed", () -> new AkaishiWitherSeedItem(new Item.Properties()));
        akaishiWitherCondensate = item("akaishi_wither_condensate");
    }

    private static RegistrySupplier<Item> item(String id) {
        return item(id, () -> new Item(new Item.Properties()));
    }

    private static RegistrySupplier<Item> item(String id, Supplier<Item> factory) {
        return RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id), factory);
    }
}
