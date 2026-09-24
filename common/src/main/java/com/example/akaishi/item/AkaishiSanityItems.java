package com.example.akaishi.item;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.item.curio.AkaishiSanityGarland;
import com.example.akaishi.sanity.content.SanityTonicService;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Supplier;

/**
 * 理智域物品注册：理智剂（普通/高级）与花环。
 *
 * <p><b>为什么单独开一个域类</b>：理智系统（P1~P5）此前只有"金西瓜"一件物品借住在生命域；
 * 本期一次落地三件同族物品（两瓶药剂 + 一件饰品），继续往生命域堆会模糊"生命科技链"的边界。
 * 按注册域拆分铁律（§1），新功能域独立成 {@code AkaishiSanityItems}，
 * ID 常量仍留在门面 {@link ModItems}（注册 id 的唯一来源），字段由门面转发。
 *
 * <p><b>数值来源</b>：药剂窗口/冷却取 {@link SanityTonicService} 的常量（唯一口径），
 * 花环耐久上限取 {@link AkaishiSanityGarland#MAX_DURABILITY}，本类只做"装配"，不写数值。
 *
 * <p><b>配方</b>：本轮用户未指定配方，故三件物品<b>仅创造栏可得</b>（配方待指定后另行落地，
 * 见 {@code ModCreativeTabs} 的展示位）。药水走酿造台（见 forge 侧 {@code AkaishiSanityPotionBrewing}）。
 */
public final class AkaishiSanityItems {

    private AkaishiSanityItems() {
    }

    /** 理智回复剂：5s 内共回 30 SAN，冷却 30 分钟 */
    public static RegistrySupplier<Item> sanityTonic;
    /** 高级理智恢复剂：5s 内共回 40 SAN，冷却 20 分钟（见 SanityTonicService 的"疑似写反"标注） */
    public static RegistrySupplier<Item> greaterSanityTonic;
    /** 花环（charm 槽）：每分钟 +1 SAN，每秒 -1 耐久，720 秒后损毁 */
    public static RegistrySupplier<Item> sanityGarland;

    public static void register() {
        // 理智剂：可堆叠（无瓶胆回收语义，故不像药水那样限 1），右键进入 32t 饮用动作；
        // 数值与冷却全在 SanityTonicService 的常量里（待调手感值）
        sanityTonic = item(ModItems.SANITY_TONIC_ID, () -> new SanityTonicItem(
                new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), ModItems.SANITY_TONIC_ID,
                SanityTonicService.TONIC_TOTAL_SAN, SanityTonicService.WINDOW_TICKS,
                SanityTonicService.TONIC_COOLDOWN_TICKS));
        greaterSanityTonic = item(ModItems.GREATER_SANITY_TONIC_ID, () -> new SanityTonicItem(
                new Item.Properties().stacksTo(16).rarity(Rarity.RARE), ModItems.GREATER_SANITY_TONIC_ID,
                SanityTonicService.GREATER_TONIC_TOTAL_SAN, SanityTonicService.WINDOW_TICKS,
                SanityTonicService.GREATER_TONIC_COOLDOWN_TICKS));
        // 花环：durability(720) 会同时把堆叠限为 1（原版 Properties 语义），耐久值即"已佩戴秒数"
        sanityGarland = item(ModItems.SANITY_GARLAND_ID, () -> new AkaishiSanityGarland(
                new Item.Properties().durability(AkaishiSanityGarland.MAX_DURABILITY).rarity(Rarity.UNCOMMON)));
    }

    /** 注册物品（generated 模型引用 textures/item/&lt;id&gt;.png，与 id 严格同名） */
    private static RegistrySupplier<Item> item(String id, Supplier<Item> factory) {
        return RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id), factory);
    }
}
