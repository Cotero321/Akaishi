package com.example.akaishi.item;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.item.curio.AkaishiAntidoteBracelet;
import com.example.akaishi.item.curio.AkaishiBlastCharm;
import com.example.akaishi.item.curio.AkaishiFireNecklace;
import com.example.akaishi.item.curio.AkaishiGatheringBracelet;
import com.example.akaishi.item.curio.AkaishiHuntingRing;
import com.example.akaishi.item.curio.AkaishiSatiationCharm;
import com.example.akaishi.item.curio.AkaishiWitherCharm;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;

/**
 * 赤石基础域物品注册：材料（晶体/精华/机件/锭）、赤石装备与工具、机器升级件、
 * 粉末与矿石基底（打粉机/压缩机/变化器产物）、赤石饰品（Curios）与手册。
 */
public final class AkaishiBaseItems {

    private AkaishiBaseItems() {
    }

    public static RegistrySupplier<Item> akaishiCrystal;
    public static RegistrySupplier<Item> akaishiEssence;
    public static RegistrySupplier<Item> akaishiEssenceCompressed;
    public static RegistrySupplier<Item> akaishiMachineComponent;
    public static RegistrySupplier<Item> akaishiAdvancedComponent;
    public static RegistrySupplier<Item> akaishiIngot;
    public static RegistrySupplier<Item> akaishiUpgradeTemplate;
    public static RegistrySupplier<Item> akaishiHelmet;
    public static RegistrySupplier<Item> akaishiChestplate;
    public static RegistrySupplier<Item> akaishiLeggings;
    public static RegistrySupplier<Item> akaishiBoots;
    public static RegistrySupplier<Item> akaishiSword;
    public static RegistrySupplier<Item> akaishiPickaxe;
    public static RegistrySupplier<Item> akaishiShovel;
    public static RegistrySupplier<Item> akaishiAxe;
    public static RegistrySupplier<Item> satiationCharm;
    public static RegistrySupplier<Item> huntingRing;
    public static RegistrySupplier<Item> gatheringBracelet;
    public static RegistrySupplier<Item> fireNecklace;
    public static RegistrySupplier<Item> blastCharm;
    public static RegistrySupplier<Item> antidoteBracelet;
    public static RegistrySupplier<Item> witherCharm;
    public static RegistrySupplier<Item> machineSpeedUpgrade;
    public static RegistrySupplier<Item> machineEnergyUpgrade;
    public static RegistrySupplier<Item> akaishiDust;
    public static RegistrySupplier<Item> coalDust;
    public static RegistrySupplier<Item> ironDust;
    public static RegistrySupplier<Item> copperDust;
    public static RegistrySupplier<Item> goldDust;
    public static RegistrySupplier<Item> lapisDust;
    public static RegistrySupplier<Item> diamondDust;
    public static RegistrySupplier<Item> emeraldDust;
    public static RegistrySupplier<Item> quartzDust;
    public static RegistrySupplier<Item> netheriteDust;
    public static RegistrySupplier<Item> obsidianDust;
    public static RegistrySupplier<Item> coolingBase;
    public static RegistrySupplier<Item> coalOreBase;
    public static RegistrySupplier<Item> ironOreBase;
    public static RegistrySupplier<Item> copperOreBase;
    public static RegistrySupplier<Item> goldOreBase;
    public static RegistrySupplier<Item> redstoneOreBase;
    public static RegistrySupplier<Item> lapisOreBase;
    public static RegistrySupplier<Item> diamondOreBase;
    public static RegistrySupplier<Item> emeraldOreBase;
    public static RegistrySupplier<Item> quartzOreBase;
    public static RegistrySupplier<Item> netheriteOreBase;
    public static RegistrySupplier<Item> akaishiOreBase;
    public static RegistrySupplier<Item> akaishiDiary;
    public static RegistrySupplier<Item> lifeBook;
    public static RegistrySupplier<Item> geneBook;

    public static void register() {
        akaishiCrystal = item(ModItems.CHISHI_CRYSTAL_ID);
        akaishiEssence = item(ModItems.CHISHI_ESSENCE_ID);
        akaishiEssenceCompressed = item(ModItems.ESSENCE_COMPRESSED_ID);
        akaishiMachineComponent = item(ModItems.MACHINE_COMPONENT_ID);
        akaishiAdvancedComponent = item(ModItems.ADVANCED_COMPONENT_ID);
        akaishiIngot = item(ModItems.CHISHI_INGOT_ID);
        akaishiUpgradeTemplate = item(ModItems.UPGRADE_TEMPLATE_ID);
        // 赤石装备（基础属性 = 下界合金 × 1.25，可升级）
        akaishiHelmet = item(ModItems.HELMET_ID, () -> new AkaishiArmorItem(ArmorItem.Type.HELMET, new Item.Properties()));
        akaishiChestplate = item(ModItems.CHESTPLATE_ID, () -> new AkaishiArmorItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
        akaishiLeggings = item(ModItems.LEGGINGS_ID, () -> new AkaishiArmorItem(ArmorItem.Type.LEGGINGS, new Item.Properties()));
        akaishiBoots = item(ModItems.BOOTS_ID, () -> new AkaishiArmorItem(ArmorItem.Type.BOOTS, new Item.Properties()));
        // 攻击伤害修饰符 7：4 + 7 = 11 = 下界合金剑 9 × 1.25
        akaishiSword = item(ModItems.SWORD_ID, () -> new AkaishiSwordItem(Tiers.NETHERITE, 7, -2.0F, new Item.Properties()));
        // 赤石工具（属性 = 下界合金对应工具 × 1.25，见各类注释）
        akaishiPickaxe = item(ModItems.PICKAXE_ID, () -> new AkaishiPickaxeItem(new Item.Properties()));
        akaishiShovel = item(ModItems.SHOVEL_ID, () -> new AkaishiShovelItem(new Item.Properties()));
        akaishiAxe = item(ModItems.AXE_ID, () -> new AkaishiAxeItem(new Item.Properties()));
        // 机器升级组件（速度/能量）：装入用电器升级槽，单格堆叠 8 封顶
        machineSpeedUpgrade = item(ModItems.MACHINE_SPEED_UPGRADE_ID,
                () -> new AkaishiMachineUpgradeItem(MachineUpgradeType.SPEED));
        machineEnergyUpgrade = item(ModItems.MACHINE_ENERGY_UPGRADE_ID,
                () -> new AkaishiMachineUpgradeItem(MachineUpgradeType.ENERGY));
        // 粉末（打粉机产物 / 压缩机原料）
        akaishiDust = item(ModItems.CHISHI_DUST_ID);
        coalDust = item(ModItems.COAL_DUST_ID);
        ironDust = item(ModItems.IRON_DUST_ID);
        copperDust = item(ModItems.COPPER_DUST_ID);
        goldDust = item(ModItems.GOLD_DUST_ID);
        lapisDust = item(ModItems.LAPIS_DUST_ID);
        diamondDust = item(ModItems.DIAMOND_DUST_ID);
        emeraldDust = item(ModItems.EMERALD_DUST_ID);
        quartzDust = item(ModItems.QUARTZ_DUST_ID);
        netheriteDust = item(ModItems.NETHERITE_DUST_ID);
        obsidianDust = item(ModItems.OBSIDIAN_DUST_ID);
        // 基底（变化器产物）
        coolingBase = item(ModItems.COOLING_BASE_ID);
        coalOreBase = item(ModItems.COAL_ORE_BASE_ID);
        ironOreBase = item(ModItems.IRON_ORE_BASE_ID);
        copperOreBase = item(ModItems.COPPER_ORE_BASE_ID);
        goldOreBase = item(ModItems.GOLD_ORE_BASE_ID);
        redstoneOreBase = item(ModItems.REDSTONE_ORE_BASE_ID);
        lapisOreBase = item(ModItems.LAPIS_ORE_BASE_ID);
        diamondOreBase = item(ModItems.DIAMOND_ORE_BASE_ID);
        emeraldOreBase = item(ModItems.EMERALD_ORE_BASE_ID);
        quartzOreBase = item(ModItems.QUARTZ_ORE_BASE_ID);
        netheriteOreBase = item(ModItems.NETHERITE_ORE_BASE_ID);
        akaishiOreBase = item(ModItems.CHISHI_ORE_BASE_ID);
        // 赤石饰品（Curios 槽位：charm/ring/hands/necklace/body/bracelet/belt）
        satiationCharm = item(ModItems.SATIATION_CHARM_ID, () -> new AkaishiSatiationCharm(new Item.Properties()));
        huntingRing = item(ModItems.HUNTING_RING_ID, () -> new AkaishiHuntingRing(new Item.Properties()));
        gatheringBracelet = item(ModItems.GATHERING_BRACELET_ID, () -> new AkaishiGatheringBracelet(new Item.Properties()));
        fireNecklace = item(ModItems.FIRE_NECKLACE_ID, () -> new AkaishiFireNecklace(new Item.Properties()));
        blastCharm = item(ModItems.BLAST_CHARM_ID, () -> new AkaishiBlastCharm(new Item.Properties()));
        antidoteBracelet = item(ModItems.ANTIDOTE_BRACELET_ID, () -> new AkaishiAntidoteBracelet(new Item.Properties()));
        witherCharm = item(ModItems.WITHER_CHARM_ID, () -> new AkaishiWitherCharm(new Item.Properties()));
        // Patchouli 手册物品（右键打开对应书籍）
        akaishiDiary = item(ModItems.AKAISHI_DIARY_ID,
                () -> new AkaishiBookItem(new ResourceLocation(AkaishiMod.MOD_ID, ModItems.AKAISHI_DIARY_ID), new Item.Properties()));
        lifeBook = item(ModItems.LIFE_BOOK_ID,
                () -> new AkaishiBookItem(new ResourceLocation(AkaishiMod.MOD_ID, "life_secrets"), new Item.Properties()));
        // 基因详解：独立手册，按基因组/基因型/词条/序列分区整理生命体系的全部数值
        geneBook = item(ModItems.GENE_BOOK_ID,
                () -> new AkaishiBookItem(new ResourceLocation(AkaishiMod.MOD_ID, "gene_detail"), new Item.Properties()));
    }

    /** 注册普通物品（generated 模型引用 textures/item/&lt;id&gt;.png） */
    private static RegistrySupplier<Item> item(String id) {
        return item(id, () -> new Item(new Item.Properties()));
    }

    /** 注册带自定义实现的物品（Supplier<Item> 保持 register 目标类型推导） */
    private static RegistrySupplier<Item> item(String id, java.util.function.Supplier<Item> factory) {
        return RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id), factory);
    }
}
