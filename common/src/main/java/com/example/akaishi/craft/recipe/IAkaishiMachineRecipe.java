package com.example.akaishi.craft.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

/**
 * 赤石机器配方的公共视图：单槽物料变换（{@link AkaishiItemProcessRecipe}）与
 * 能量聚合（{@link AkaishiEnergyProcessRecipe}）共用一套索引与机器读取口径。
 *
 * <p>把"配方类型 / 原料 / 单格消耗 / 赤能源 / 产物"收敛到这里，机器的 tick 逻辑只依赖本接口，
 * 新增配方族时无需改动机器（对扩展开放、对修改关闭）。
 */
public interface IAkaishiMachineRecipe {

    /** 配方类型（索引按它分组） */
    RecipeType<?> getType();

    /** 物品原料；纯能量配方（如生命固态物固化）返回 null */
    @Nullable
    Ingredient ingredient();

    /** 单格物品原料的消耗数量（≥ 1） */
    int inputCount();

    /** 单次加工的赤能源总耗；0 = 未指定，由机器取配置默认值 */
    long energy();

    /** 是否消耗物品输入；false = 输入保留（如植物培养机的种子） */
    boolean consumeInput();

    /** 单次产出（只读语义，实际取用请 {@code copy()}） */
    ItemStack result();

    /** 输入物品能否触发本配方（无物品原料的配方恒为 false） */
    default boolean matchesInput(ItemStack stack) {
        Ingredient ingredient = ingredient();
        return ingredient != null && !stack.isEmpty()
                && stack.getCount() >= inputCount() && ingredient.test(stack);
    }
}
