package com.example.akaishi.item;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * 转基因域物品注册：转基因植物（凋零藤 / 烈焰花）体系的种子与收获物。
 * 凋零藤系均由转基因工厂（凋零骷髅基因 + 缠怨藤 + 凋零玫瑰 + 固态物）产出。
 */
public final class AkaishiTransgeneItems {

    private AkaishiTransgeneItems() {
    }

    /** 转基因植物：凋零藤种子（右键种植生成根，根挖掘返还本种子） */
    public static RegistrySupplier<Item> akaishiWitherSeed;
    /** 转基因植物：凋零果（成熟藤收获，凋零系高级原料/介质） */
    public static RegistrySupplier<Item> akaishiWitherCondensate;
    /** 转基因植物：烈焰花种（仅可种于灵魂沙生成烈焰花株，烈焰系火种来源） */
    public static RegistrySupplier<Item> akaishiBlazeSeed;
    /** 转基因植物：烈焰花瓣（盛开的烈焰花冠收获，烈焰系高级原料/介质） */
    public static RegistrySupplier<Item> akaishiBlazeCondensate;
    /** 转基因植物：咒怨垂蔓种子（右键方块底面种植生成吊挂的根，根挖掘返还本种子） */
    public static RegistrySupplier<Item> akaishiCurseVineSeed;
    /** 转基因植物：咒怨花（成熟垂蔓最底端收获，恶魂系高级原料/介质） */
    public static RegistrySupplier<Item> akaishiCurseBlossom;

    public static void register() {
        akaishiWitherSeed = item("akaishi_wither_seed", () -> new AkaishiWitherSeedItem(new Item.Properties()));
        akaishiWitherCondensate = item("akaishi_wither_condensate");
        akaishiBlazeSeed = item("akaishi_blaze_seed", () -> new AkaishiBlazeSeedItem(new Item.Properties()));
        akaishiBlazeCondensate = item("akaishi_blaze_condensate");
        akaishiCurseVineSeed = item("akaishi_curse_vine_seed", () -> new AkaishiCurseVineSeedItem(new Item.Properties()));
        akaishiCurseBlossom = item("akaishi_curse_blossom");
    }

    private static RegistrySupplier<Item> item(String id) {
        return item(id, () -> new Item(new Item.Properties()));
    }

    private static RegistrySupplier<Item> item(String id, Supplier<Item> factory) {
        return RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id), factory);
    }
}
