package com.example.akaishi.api.recipe;

/**
 * 机器加工配方：<b>逐格</b>消耗数量声明（{@link IProcessInputCount} 的多格加法式补充）。
 *
 * <p>单格口径（{@code IProcessInputCount#inputCount}）对所有原料格一视同仁，
 * 装不下"9 个 A + 4 个 B"这类<b>每格数量不同</b>的双原料配方 ——
 * 估值 / 虚拟加工侧据本接口按格扣料，否则会把 B 也按 9 个要（多扣材料）。
 *
 * <p>返回值下标 = 原料格序，必须与 {@code Recipe#getIngredients()} 的有效格一一对应；
 * 长度不符（或未实现本接口）时调用方一律回退为 {@code inputCount()} 的均匀口径。
 */
public interface IProcessSlotCounts {

    /** 每个原料格各自消耗的数量（≥ 1，下标 = 原料格序） */
    int[] inputCounts();
}
