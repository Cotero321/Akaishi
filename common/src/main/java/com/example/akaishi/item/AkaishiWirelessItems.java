package com.example.akaishi.item;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * 无线域物品注册：无线能源便捷组件（合成材料）、便捷终端（手持频道遥控面板）与终端身份卡。
 */
public final class AkaishiWirelessItems {

    private AkaishiWirelessItems() {
    }

    public static RegistrySupplier<Item> akaishiWirelessComponent;
    public static RegistrySupplier<Item> akaishiWirelessPortableTerminal;
    public static RegistrySupplier<Item> akaishiWirelessIdentityCard;

    public static void register() {
        // 无线赤能源体系：便捷组件（合成材料）
        akaishiWirelessComponent = item("akaishi_wireless_component");
        // 便捷终端：手持频道遥控面板（查看/切换频道）
        akaishiWirelessPortableTerminal = item("akaishi_wireless_portable_terminal",
                () -> new AkaishiWirelessPortableTerminalItem(new Item.Properties().stacksTo(1)));
        // 终端身份卡：无线网络认证钥匙（唯一 UUID + 等级）
        akaishiWirelessIdentityCard = item("akaishi_wireless_identity_card",
                () -> new AkaishiWirelessIdentityCardItem(new Item.Properties().stacksTo(1)));
    }

    private static RegistrySupplier<Item> item(String id) {
        return item(id, () -> new Item(new Item.Properties()));
    }

    private static RegistrySupplier<Item> item(String id, Supplier<Item> factory) {
        return RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id), factory);
    }
}
