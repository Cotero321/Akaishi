package com.example.akaishi.forbidden.item;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.forbidden.item.curio.AkaishiCubHeart;
import com.example.akaishi.forbidden.item.curio.AkaishiFertilityRing;
import com.example.akaishi.forbidden.item.curio.AkaishiLifeTouch;
import com.example.akaishi.forbidden.item.curio.AkaishiMotherSeal;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Supplier;

/**
 * 禁忌模块·物品注册（P3d 随生命融合与禁忌四件饰品从本体迁入，注册 id 与迁前逐字一致）。
 *
 * <p>ID 常量随域类迁入本类（对本体不再是字段，符合「搬走即本体无引用」）。
 * 未安装本模块时这些物品根本不注册（注册命名空间仍为三模块共用的 {@code akaishi:}）。
 */
public final class AkaishiForbiddenItems {

    /** 生命的融合锭：母神祭坛仪式的产物 */
    public static final String LIFE_FUSION_INGOT_ID = "akaishi_life_fusion_ingot";
    /** 生命融合护甲（4 件）：赤石护甲 2 倍基础数值，保留升级数据，穿齐触发套装效果 */
    public static final String LIFE_FUSION_HELMET_ID = "akaishi_life_fusion_helmet";
    public static final String LIFE_FUSION_CHESTPLATE_ID = "akaishi_life_fusion_chestplate";
    public static final String LIFE_FUSION_LEGGINGS_ID = "akaishi_life_fusion_leggings";
    public static final String LIFE_FUSION_BOOTS_ID = "akaishi_life_fusion_boots";
    /** 禁忌·生命之触（akaishi_socket_1）：攻击距离 +2、概率双击、概率自伤与饥饿代价 */
    public static final String LIFE_TOUCH_ID = "akaishi_life_touch";
    /** 禁忌·幼崽之心（akaishi_socket_2）：黄心、微量增益、伤害转移、不可名状、兴奋攻速 */
    public static final String CUB_HEART_ID = "akaishi_cub_heart";
    /** 禁忌·母神之印（akaishi_socket_3）：伤害/生命/移速/攻速加成，素食代价与抗性 */
    public static final String MOTHER_SEAL_ID = "akaishi_mother_seal";
    /** 禁忌·孕育之环（akaishi_socket_4）：受击治疗、攻速爆发、肉食治疗、饥饿惩罚 */
    public static final String FERTILITY_RING_ID = "akaishi_fertility_ring";

    /** 生命的融合锭（母神祭坛仪式产物） */
    public static RegistrySupplier<Item> lifeFusionIngot;
    /** 生命融合护甲（赤石护甲 2 倍基础数值，融合砧产出，保留升级数据） */
    public static RegistrySupplier<Item> lifeFusionHelmet;
    public static RegistrySupplier<Item> lifeFusionChestplate;
    public static RegistrySupplier<Item> lifeFusionLeggings;
    public static RegistrySupplier<Item> lifeFusionBoots;
    /** 禁忌四件（Curios 扩展槽 akaishi_socket_1..4）：纯被动零消耗，各占一槽，集齐触发套装 */
    public static RegistrySupplier<Item> lifeTouch;
    public static RegistrySupplier<Item> cubHeart;
    public static RegistrySupplier<Item> motherSeal;
    public static RegistrySupplier<Item> fertilityRing;

    private AkaishiForbiddenItems() {
    }

    /** 由 {@code AkaishiForbiddenMod.init()} 调用（注册表冻结前） */
    public static void register() {
        lifeFusionIngot = item(LIFE_FUSION_INGOT_ID);
        lifeFusionHelmet = item(LIFE_FUSION_HELMET_ID,
                () -> new AkaishiLifeFusionArmorItem(ArmorItem.Type.HELMET, new Item.Properties()));
        lifeFusionChestplate = item(LIFE_FUSION_CHESTPLATE_ID,
                () -> new AkaishiLifeFusionArmorItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
        lifeFusionLeggings = item(LIFE_FUSION_LEGGINGS_ID,
                () -> new AkaishiLifeFusionArmorItem(ArmorItem.Type.LEGGINGS, new Item.Properties()));
        lifeFusionBoots = item(LIFE_FUSION_BOOTS_ID,
                () -> new AkaishiLifeFusionArmorItem(ArmorItem.Type.BOOTS, new Item.Properties()));
        // 禁忌四件（Curios 扩展槽 akaishi_socket_1..4）：纯被动零消耗，各占一槽，集齐触发套装
        Item.Properties forbiddenProps = new Item.Properties().stacksTo(1).rarity(Rarity.EPIC);
        lifeTouch = item(LIFE_TOUCH_ID, () -> new AkaishiLifeTouch(forbiddenProps));
        cubHeart = item(CUB_HEART_ID, () -> new AkaishiCubHeart(forbiddenProps));
        motherSeal = item(MOTHER_SEAL_ID, () -> new AkaishiMotherSeal(forbiddenProps));
        fertilityRing = item(FERTILITY_RING_ID, () -> new AkaishiFertilityRing(forbiddenProps));
    }

    private static RegistrySupplier<Item> item(String id) {
        return item(id, () -> new Item(new Item.Properties()));
    }

    private static RegistrySupplier<Item> item(String id, Supplier<Item> factory) {
        return RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id), factory);
    }
}
