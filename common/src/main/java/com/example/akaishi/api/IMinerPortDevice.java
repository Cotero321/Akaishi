package com.example.akaishi.api;

import net.minecraft.core.BlockPos;

/**
 * 矿机可关联端口接口：矿机结构中的设备方块实体（转口/能量输入口/物品输出口）实现此接口。
 * 控制器成型/解散时统一通过本接口建立或解除关联，避免控制器直接依赖各端口具体类型。
 */
public interface IMinerPortDevice {

    /**
     * 建立/解除与矿机控制器的关联。
     *
     * @param pos 控制器位置；null 表示结构解散
     */
    void setControllerPos(BlockPos pos);
}
