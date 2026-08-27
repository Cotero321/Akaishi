package com.example.template.api.item;

import net.minecraft.world.Container;

/**
 * 物品管道设备接口：声明设备的输入/输出槽位，供物品管道精准供料/取料。
 * <p>
 * 聚合器这类"原料进、产物出"的机器通过本接口暴露输入槽与输出槽，
 * 管道只会向输入槽插入原料、从输出槽抽取产物，不会混淆方向。
 * 未实现本接口的普通 {@link Container}（箱子、漏斗等）由管道按"全槽通用"处理。
 */
public interface IItemPipeDevice extends Container {

    /** 物品输入槽（管道向这些槽插入物品）；返回空数组表示该设备不可接收物品 */
    default int[] getPipeInputSlots() {
        return new int[0];
    }

    /** 物品输出槽（管道从这些槽抽取物品）；返回空数组表示该设备不可输出物品 */
    default int[] getPipeOutputSlots() {
        return new int[0];
    }

    /** 是否可接收物品（输入槽非空） */
    default boolean canPipeInput() {
        return getPipeInputSlots().length > 0;
    }

    /** 是否可输出物品（输出槽非空） */
    default boolean canPipeOutput() {
        return getPipeOutputSlots().length > 0;
    }
}
