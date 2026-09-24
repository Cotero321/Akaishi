package com.example.akaishi.item;

import com.example.akaishi.AkaishiMod;

import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Supplier;

/**
 * 禁忌秘典域物品注册（本轮只有一件：秘典本体）。
 *
 * <p><b>为什么单独开一个域类</b>：秘典是自研知识/研究系统的入口物品，与理智域（药剂/花环）、
 * 基础域（材料/装备）都不是同一族；按注册域拆分口径，新功能域独立成 {@code AkaishiCodexItems}，
 * 注册 id 常量仍留在门面 {@link ModItems}（id 的唯一来源）。
 *
 * <p><b>配方</b>：本轮用户未指定配方，故秘典<b>仅创造栏可得</b>（展示位见 {@code ModCreativeTabs} 的禁忌栏）。
 *
 * <p>贴图 {@code akaishi:item/forbidden_codex} 由建模侧产出，本类只保证 id 与资源路径严格同名。
 */
public final class AkaishiCodexItems {

    /** 禁忌秘典（禁忌栏）：右键翻开，入知识/研究系统 */
    public static RegistrySupplier<Item> forbiddenCodex;

    private AkaishiCodexItems() {
    }

    public static void register() {
        // 唯一性物品：秘典记载的是"这个玩家读到了哪一页"，同一本多持无意义，故不堆叠
        forbiddenCodex = item(ModItems.FORBIDDEN_CODEX_ID, () -> new AkaishiCodexItem(
                new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    }

    /** 注册物品（generated 模型引用 textures/item/&lt;id&gt;.png，与 id 严格同名） */
    private static RegistrySupplier<Item> item(String id, Supplier<Item> factory) {
        return RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id), factory);
    }
}
