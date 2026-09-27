package com.example.akaishi.forbidden;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.AkaishiForbiddenBlocks;
import com.example.akaishi.block.AkaishiMotherAltarBlocks;
import com.example.akaishi.item.AkaishiCodexItems;
import com.example.akaishi.item.AkaishiForbiddenItems;
import com.example.akaishi.item.AkaishiSanityItems;

import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * 创造栏 · 禁忌（P3a 随禁忌秘典从本体 {@code ModCreativeTabs} 迁入 akaishi_forbidden）。
 *
 * <p><b>为什么整栏跟着迁</b>：秘典物品迁走后，本体不得再引用它（依赖只能单向 禁忌 → 本体），
 * 而"没装禁忌包就没有禁忌栏"比"留一个空栏"更符合分割语义。
 *
 * <p>P3b 起栏内条目全部来自本模块自身注册（理智三件 / 生命融合 / 母神祭坛 / 禁断四件饰品），
 * 不再经本体 {@code ModItems} 引用。
 *
 * <p>栏 id / 标题 lang 键 / 图标与位置与迁前逐字一致：{@code akaishi:akaishi_forbidden}
 * （命名空间不变）、{@code itemGroup.akaishi.forbidden}、{@code TOP} 行第 4 列。
 */
public final class AkaishiForbiddenCreativeTabs {

    /** 禁忌栏 id（命名空间 akaishi 不变；禁断四件饰品 + 秘典 + 生命融合 + 母神祭坛） */
    public static final String FORBIDDEN_TAB_ID = "akaishi_forbidden";

    private AkaishiForbiddenCreativeTabs() {
    }

    public static void register() {
        var tabs = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.CREATIVE_MODE_TAB);
        tabs.register(new ResourceLocation(AkaishiMod.MOD_ID, FORBIDDEN_TAB_ID),
                () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 4)
                        .title(Component.translatable("itemGroup.akaishi.forbidden"))
                        .icon(() -> new ItemStack(AkaishiForbiddenItems.motherSeal.get()))
                        .displayItems((params, output) -> addForbiddenItems(output))
                        .build());
    }

    private static void addForbiddenItems(CreativeModeTab.Output output) {
        // 禁忌四件培育/生育饰品（P3d 自本体迁入本模块注册，创造栏归属同步改列本栏）
        accept(output, AkaishiForbiddenItems.lifeTouch);
        accept(output, AkaishiForbiddenItems.cubHeart);
        accept(output, AkaishiForbiddenItems.motherSeal);
        accept(output, AkaishiForbiddenItems.fertilityRing);
        // 禁忌秘典：自研知识/研究系统的入口（无配方，仅创造栏可得）
        accept(output, AkaishiCodexItems.forbiddenCodex);
        // 生命融合（P3d 自本体「生命科技」栏改列本禁忌栏）：融合锭 / 融合砧 / 生命融合护甲 4 件
        accept(output, AkaishiForbiddenItems.lifeFusionIngot);
        accept(output, AkaishiForbiddenBlocks.CHISHI_LIFE_FUSION_ANVIL);
        accept(output, AkaishiForbiddenItems.lifeFusionHelmet);
        accept(output, AkaishiForbiddenItems.lifeFusionChestplate);
        accept(output, AkaishiForbiddenItems.lifeFusionLeggings);
        accept(output, AkaishiForbiddenItems.lifeFusionBoots);
        // 母神祭坛体系（P3d 自本体「生命科技」栏改列本禁忌栏）：母神祭坛 / 祭坛石 / 猩红哭泣黑曜石
        accept(output, AkaishiMotherAltarBlocks.CHISHI_MOTHER_ALTAR);
        accept(output, AkaishiMotherAltarBlocks.CHISHI_ALTAR_STONE);
        accept(output, AkaishiMotherAltarBlocks.CRYING_OBSIDIAN_RED);
        // 理智系统·物品与酿造（P3b 自生命科技栏改列本禁忌栏）：两瓶药剂 + 花环饰品
        // （均暂无配方，仅创造栏可得；花环为饰品，药剂走酿造台）
        accept(output, AkaishiSanityItems.sanityTonic);
        accept(output, AkaishiSanityItems.greaterSanityTonic);
        accept(output, AkaishiSanityItems.sanityGarland);
    }

    /** 判空后把注册内容（物品/方块，均实现 ItemLike）放入创造标签（注册完成前为 null，防御性跳过） */
    private static void accept(CreativeModeTab.Output output, RegistrySupplier<? extends ItemLike> supplier) {
        if (supplier != null) {
            output.accept(new ItemStack(supplier.get()));
        }
    }
}
