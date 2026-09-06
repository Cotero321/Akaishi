package com.example.akaishi.item;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.energy.PortableCellTier;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * 能源域物品注册：管道配置器、便携赤能源储存单元（3 级）、发生器产能升级、
 * 液体燃料前体（末地/巨龙/幽匿混合物，液化装置原料）。
 */
public final class AkaishiEnergyItems {

    private AkaishiEnergyItems() {
    }

    public static RegistrySupplier<Item> akaishiDebugTool;
    public static RegistrySupplier<Item> portableCellBasic;
    public static RegistrySupplier<Item> portableCellAdvanced;
    public static RegistrySupplier<Item> portableCellSuper;
    public static RegistrySupplier<Item> akaishiSpeedUpgrade;
    public static RegistrySupplier<Item> endMixture;
    public static RegistrySupplier<Item> dragonMixture;
    public static RegistrySupplier<Item> sculkLifeform;

    public static void register() {
        akaishiDebugTool = item(ModItems.DEBUG_TOOL_ID, () -> new AkaishiDebugTool());
        // 便携赤能源储存单元（初级/中级/高级）
        portableCellBasic = item(ModItems.PORTABLE_CELL_BASIC_ID,
                () -> new AkaishiPortableEnergyCell(PortableCellTier.BASIC, new Item.Properties()));
        portableCellAdvanced = item(ModItems.PORTABLE_CELL_ADVANCED_ID,
                () -> new AkaishiPortableEnergyCell(PortableCellTier.ADVANCED, new Item.Properties()));
        portableCellSuper = item(ModItems.PORTABLE_CELL_SUPER_ID,
                () -> new AkaishiPortableEnergyCell(PortableCellTier.SUPER, new Item.Properties()));
        // 能源产生升级组件：装配到发生器（单块/多方块中心），每个提升 1.75 倍产能速度、减少 1% 产出，最多 10 个
        akaishiSpeedUpgrade = item(ModItems.SPEED_UPGRADE_ID);
        // 液体燃料前体（液化装置 → 各等级混合燃料）
        endMixture = item(ModItems.END_MIXTURE_ID);
        dragonMixture = item(ModItems.DRAGON_MIXTURE_ID);
        sculkLifeform = item(ModItems.SCULK_LIFEFORM_ID);
    }

    private static RegistrySupplier<Item> item(String id) {
        return item(id, () -> new Item(new Item.Properties()));
    }

    private static RegistrySupplier<Item> item(String id, Supplier<Item> factory) {
        return RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id), factory);
    }
}
