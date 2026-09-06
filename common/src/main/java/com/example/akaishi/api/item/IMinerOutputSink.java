package com.example.akaishi.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * 矿机产物接收端接口：转口 / 物品输出口等可接收控制器推送挖矿产物的方块实体实现。
 * 控制器推送产物时遍历结构端口并对实现本接口的端口执行 {@link #receivePartial}。
 */
public interface IMinerOutputSink {

    /**
     * 尝试接收一叠产物，支持部分合并到同种/空槽。
     *
     * @param incoming 控制器推送的产物
     * @return 未能放入的剩余部分（空表示全部接收）
     */
    ItemStack receivePartial(ItemStack incoming);
}
