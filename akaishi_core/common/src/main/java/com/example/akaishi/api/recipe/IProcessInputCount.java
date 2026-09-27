package com.example.akaishi.api.recipe;

/**
 * 机器加工配方：单格原料的<b>消耗数量</b>声明。
 *
 * <p>原版 {@code Ingredient} 无法表达"这一格要 9 个"（{@code matches} 只看物品、不看数量），
 * 因此凡"一格吃多个"的配方实现本接口，估值 / 虚拟加工侧据此按倍率扣料 ——
 * 否则会出现"1 粉压 1 块"这类凭空放大产物的漏洞。
 */
public interface IProcessInputCount {

    /** 单次加工、每个原料格消耗的数量（≥ 1） */
    int inputCount();
}
