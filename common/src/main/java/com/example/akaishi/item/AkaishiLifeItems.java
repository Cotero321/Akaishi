package com.example.akaishi.item;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.organ.AkaishiOrganItem;
import com.example.akaishi.life.potion.AkaishiPotionItem;
import com.example.akaishi.life.potion.AkaishiRejectionSerumItem;
import com.example.akaishi.life.sample.AkaishiLifeEmbryoItem;
import com.example.akaishi.life.sample.AkaishiLifeSampleItem;
import com.example.akaishi.life.sample.AkaishiSampleCollectorItem;
import com.example.akaishi.life.sequence.AkaishiGeneSequenceItem;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * 生命域物品注册：生命能量固态物、样本/基因/胚胎（生命科技链）、生命的融合锭与融合护甲、
 * 药剂与排异中和剂、器官（9 槽位）、衰竭/活化结晶与活化成分（离心与分馏产物）。
 */
public final class AkaishiLifeItems {

    private AkaishiLifeItems() {
    }

    public static RegistrySupplier<Item> akaishiLifeEssenceSolid;
    public static RegistrySupplier<Item> sampleCollector;
    public static RegistrySupplier<Item> lifeSample;
    public static RegistrySupplier<Item> geneSequence;
    public static RegistrySupplier<Item> lifeEmbryo;
    public static RegistrySupplier<Item> lifeFusionIngot;
    public static RegistrySupplier<Item> lifeFusionHelmet;
    public static RegistrySupplier<Item> lifeFusionChestplate;
    public static RegistrySupplier<Item> lifeFusionLeggings;
    public static RegistrySupplier<Item> lifeFusionBoots;
    public static RegistrySupplier<Item> akaishiPotion;
    public static RegistrySupplier<Item> rejectionSerum;
    public static RegistrySupplier<Item> akaishiOrganEye;
    public static RegistrySupplier<Item> akaishiOrganHeart;
    public static RegistrySupplier<Item> akaishiOrganLungs;
    public static RegistrySupplier<Item> akaishiOrganViscera;
    public static RegistrySupplier<Item> akaishiOrganKidneys;
    public static RegistrySupplier<Item> akaishiOrganLeftArm;
    public static RegistrySupplier<Item> akaishiOrganRightArm;
    public static RegistrySupplier<Item> akaishiOrganLeftLeg;
    public static RegistrySupplier<Item> akaishiOrganRightLeg;
    public static RegistrySupplier<Item> exhaustedCrystal;
    public static RegistrySupplier<Item> activatedSculkCrystal;
    public static RegistrySupplier<Item> activatedNetherCompoundCrystal;
    public static RegistrySupplier<Item> activatedEndMixtureCrystal;
    public static RegistrySupplier<Item> activatedAdvancedMixtureCrystal;
    public static RegistrySupplier<Item> activatedPureCrystal;
    public static RegistrySupplier<Item> activatedDragonCrystal;
    public static RegistrySupplier<Item> activatedUltimateMixtureCrystal;
    public static RegistrySupplier<Item> activatedSculkComponent;
    public static RegistrySupplier<Item> activatedNetherCompoundComponent;
    public static RegistrySupplier<Item> activatedEndMixtureComponent;
    public static RegistrySupplier<Item> activatedAdvancedMixtureComponent;
    public static RegistrySupplier<Item> activatedPureComponent;
    public static RegistrySupplier<Item> activatedDragonComponent;
    public static RegistrySupplier<Item> activatedUltimateMixtureComponent;

    public static void register() {
        // 生命能量固态物（生命能量提纯器固化，生命能源体系基础材料）
        akaishiLifeEssenceSolid = item(ModItems.LIFE_ESSENCE_SOLID_ID);
        // 生命科技：样本采集器（不可堆叠）+ 生命样本（可堆叠 64）
        sampleCollector = item(ModItems.SAMPLE_COLLECTOR_ID,
                () -> new AkaishiSampleCollectorItem(new Item.Properties().stacksTo(1)));
        lifeSample = item(ModItems.LIFE_SAMPLE_ID, () -> new AkaishiLifeSampleItem(new Item.Properties()));
        geneSequence = item(ModItems.GENE_SEQUENCE_ID, () -> new AkaishiGeneSequenceItem(new Item.Properties()));
        // 生命胚胎（8 生命固态 + 鸡蛋：献给母神祭坛）
        lifeEmbryo = item(ModItems.LIFE_EMBRYO_ID, () -> new AkaishiLifeEmbryoItem(new Item.Properties()));
        // 生命的融合锭（母神祭坛仪式产物）
        lifeFusionIngot = item(ModItems.LIFE_FUSION_INGOT_ID);
        // 生命融合护甲（赤石护甲 2 倍基础数值，融合砧产出，保留升级数据）
        lifeFusionHelmet = item(ModItems.LIFE_FUSION_HELMET_ID,
                () -> new AkaishiLifeFusionArmorItem(ArmorItem.Type.HELMET, new Item.Properties()));
        lifeFusionChestplate = item(ModItems.LIFE_FUSION_CHESTPLATE_ID,
                () -> new AkaishiLifeFusionArmorItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
        lifeFusionLeggings = item(ModItems.LIFE_FUSION_LEGGINGS_ID,
                () -> new AkaishiLifeFusionArmorItem(ArmorItem.Type.LEGGINGS, new Item.Properties()));
        lifeFusionBoots = item(ModItems.LIFE_FUSION_BOOTS_ID,
                () -> new AkaishiLifeFusionArmorItem(ArmorItem.Type.BOOTS, new Item.Properties()));
        // 药剂（永久/突破模板，模板 id + 纯度写 NBT，可堆叠）+ 排异中和剂
        akaishiPotion = item(ModItems.POTION_ID,
                () -> new AkaishiPotionItem(new Item.Properties().stacksTo(16)));
        rejectionSerum = item(ModItems.REJECTION_SERUM_ID,
                () -> new AkaishiRejectionSerumItem(new Item.Properties().stacksTo(16)));
        // 器官物品（9 槽位各一，基因来源/品质存 NBT，不可堆叠）
        akaishiOrganEye = organ(BodySlot.EYE);
        akaishiOrganHeart = organ(BodySlot.HEART);
        akaishiOrganLungs = organ(BodySlot.LUNGS);
        akaishiOrganViscera = organ(BodySlot.VISCERA);
        akaishiOrganKidneys = organ(BodySlot.KIDNEYS);
        akaishiOrganLeftArm = organ(BodySlot.LEFT_ARM);
        akaishiOrganRightArm = organ(BodySlot.RIGHT_ARM);
        akaishiOrganLeftLeg = organ(BodySlot.LEFT_LEG);
        akaishiOrganRightLeg = organ(BodySlot.RIGHT_LEG);
        // 离心结晶：生命离心机分离活化燃料的产物（1 通用衰竭结晶 + 7 对应活化结晶）
        exhaustedCrystal = item("akaishi_exhausted_crystal");
        activatedSculkCrystal = item("akaishi_activated_sculk_crystal");
        activatedNetherCompoundCrystal = item("akaishi_activated_nether_compound_crystal");
        activatedEndMixtureCrystal = item("akaishi_activated_end_mixture_crystal");
        activatedAdvancedMixtureCrystal = item("akaishi_activated_advanced_mixture_crystal");
        activatedPureCrystal = item("akaishi_activated_pure_crystal");
        activatedDragonCrystal = item("akaishi_activated_dragon_crystal");
        activatedUltimateMixtureCrystal = item("akaishi_activated_ultimate_mixture_crystal");
        // 活化成分：活化分馏器将活化结晶深度拆分（1 结晶 → 1 活化成分 + 1 衰竭结晶）
        activatedSculkComponent = item("akaishi_activated_sculk_component");
        activatedNetherCompoundComponent = item("akaishi_activated_nether_compound_component");
        activatedEndMixtureComponent = item("akaishi_activated_end_mixture_component");
        activatedAdvancedMixtureComponent = item("akaishi_activated_advanced_mixture_component");
        activatedPureComponent = item("akaishi_activated_pure_component");
        activatedDragonComponent = item("akaishi_activated_dragon_component");
        activatedUltimateMixtureComponent = item("akaishi_activated_ultimate_mixture_component");
    }

    private static RegistrySupplier<Item> organ(BodySlot slot) {
        return item("akaishi_organ_" + slot.getId(), () -> new AkaishiOrganItem(slot, new Item.Properties().stacksTo(1)));
    }

    private static RegistrySupplier<Item> item(String id) {
        return item(id, () -> new Item(new Item.Properties()));
    }

    private static RegistrySupplier<Item> item(String id, Supplier<Item> factory) {
        return RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id), factory);
    }
}
