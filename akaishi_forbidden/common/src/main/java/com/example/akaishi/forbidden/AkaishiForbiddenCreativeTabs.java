package com.example.akaishi.forbidden;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.item.AkaishiCodexItems;
import com.example.akaishi.item.AkaishiSanityItems;
import com.example.akaishi.item.ModItems;

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
 * <p>栏内<b>仍留在本体的条目</b>（扩展槽四件禁忌饰品）继续经 {@link ModItems} 引用：
 * 它们属后续阶段搬迁，本轮不动其注册归属。
 *
 * <p>栏 id / 标题 lang 键 / 图标与位置与迁前逐字一致：{@code akaishi:akaishi_forbidden}
 * （命名空间不变）、{@code itemGroup.akaishi.forbidden}、{@code TOP} 行第 4 列。
 */
public final class AkaishiForbiddenCreativeTabs {

    /** 禁忌栏 id（命名空间 akaishi 不变；扩展槽四件禁忌饰品 + 禁忌秘典） */
    public static final String FORBIDDEN_TAB_ID = "akaishi_forbidden";

    private AkaishiForbiddenCreativeTabs() {
    }

    public static void register() {
        var tabs = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.CREATIVE_MODE_TAB);
        tabs.register(new ResourceLocation(AkaishiMod.MOD_ID, FORBIDDEN_TAB_ID),
                () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 4)
                        .title(Component.translatable("itemGroup.akaishi.forbidden"))
                        .icon(() -> new ItemStack(ModItems.motherSeal.get()))
                        .displayItems((params, output) -> addForbiddenItems(output))
                        .build());
    }

    private static void addForbiddenItems(CreativeModeTab.Output output) {
        // 仍留在本体的扩展槽四件禁忌饰品（本轮不搬其注册归属）
        accept(output, ModItems.lifeTouch);
        accept(output, ModItems.cubHeart);
        accept(output, ModItems.motherSeal);
        accept(output, ModItems.fertilityRing);
        // 禁忌秘典：自研知识/研究系统的入口（无配方，仅创造栏可得）
        accept(output, AkaishiCodexItems.forbiddenCodex);
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
