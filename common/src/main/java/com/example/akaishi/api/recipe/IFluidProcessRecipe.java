package com.example.akaishi.api.recipe;

/**
 * 需要<b>流体输入</b>才能加工的配方（如生命离心机：活化燃料 → 活化结晶）。
 *
 * <p><b>为什么要有这个接口</b>：虚拟加工（{@code VirtualCraftPlanner}）的物料模型<b>只有物品</b> ——
 * 它按物品库存扣料、按产物计数，完全不认识流体库存。若这类配方进了加工页索引，
 * 就会出现"配方有物品产物、但物品原料格是空的"⇒ 被当成<b>不用材料白拿产物</b>（凭空造物）。
 * 因此 {@code RecipeIngredients} 会在建索引时按本接口把它们排除掉。
 *
 * <p>接口只暴露一个判据（而非完整流体视图）：调用方只需要知道"要不要流体"，
 * 具体流体规格由各实现类自己保留（接口隔离原则）。
 */
public interface IFluidProcessRecipe {

    /** 本配方是否必须消耗流体输入；true = 不参与虚拟加工 */
    boolean requiresFluidInput();
}
