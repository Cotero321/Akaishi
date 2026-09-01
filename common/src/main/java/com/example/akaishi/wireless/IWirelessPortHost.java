package com.example.akaishi.wireless;

import net.minecraft.world.inventory.ContainerData;

/**
 * 无线赤能源端口最小接口（ISP）：输入口/输出口方块实体共用此接口，
 * 使 {@link com.example.akaishi.menu.AkaishiWirelessPortMenu} 不依赖具体实现类。
 */
public interface IWirelessPortHost {

    /** GUI 数据槽（缓冲储能/绑定卡/认证终端/速率） */
    ContainerData data();

    /** 解绑身份卡（GUI 按钮），断开与终端的所有连接 */
    void unbind();

    /** 是否为输出口（输出口纯发电，输入口纯接收，GUI 提示不同） */
    boolean isOutput();
}
