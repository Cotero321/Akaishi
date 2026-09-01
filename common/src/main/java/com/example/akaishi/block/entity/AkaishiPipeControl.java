package com.example.akaishi.block.entity;

import net.minecraft.core.Direction;

/**
 * 管道通用控制接口：赤能源管道与物品管道均实现，供配置器统一操作
 * 方向模式（正常/推/拉）与单侧连接断开/恢复。
 */
public interface AkaishiPipeControl {

    int getMode();

    void setMode(int mode);

    /** 该方向是否被配置器断开连接 */
    boolean isDisconnected(Direction dir);

    /** 切换某方向的连接（断开↔恢复），返回切换后是否处于断开状态 */
    boolean toggleDisconnected(Direction dir);
}
