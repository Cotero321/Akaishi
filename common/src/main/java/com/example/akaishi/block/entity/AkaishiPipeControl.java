package com.example.akaishi.block.entity;

import net.minecraft.core.Direction;

/**
 * 管道通用控制接口：物品/赤能源/液体三类管道均实现，供配置器统一操作。
 * 方向类型按「面」独立（对齐 Mekanism 的 ConnectionType），存于方块实体而非方块状态，
 * 避免方块状态变体随方向组合爆炸。
 * 类型语义：0=正常（按设备能力双向判定） 1=输出（管道→设备） 2=输入（设备→管道）。
 */
public interface AkaishiPipeControl {

    /** 正常：按设备能力双向判定 */
    int MODE_NORMAL = 0;
    /** 输出：管道 → 设备（相连设备只作汇） */
    int MODE_OUTPUT = 1;
    /** 输入：设备 → 管道（相连设备只作源） */
    int MODE_INPUT = 2;

    /** 读取某方向的方向类型 */
    int getSideMode(Direction dir);

    /** 设置某方向的方向类型（越界回退为正常） */
    void setSideMode(Direction dir, int mode);

    /** 该方向是否被配置器断开连接 */
    boolean isDisconnected(Direction dir);

    /** 切换某方向的连接（断开↔恢复），返回切换后是否处于断开状态 */
    boolean toggleDisconnected(Direction dir);
}
