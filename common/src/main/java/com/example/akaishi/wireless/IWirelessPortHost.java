package com.example.akaishi.wireless;

import java.util.UUID;

import net.minecraft.world.inventory.ContainerData;

/**
 * 无线端口最小接口（ISP）：输入口/输出口方块实体共用此接口，
 * 使 {@link com.example.akaishi.menu.AkaishiWirelessPortMenu} 不依赖具体实现类。
 */
public interface IWirelessPortHost {

    /** GUI 数据槽（缓冲储能/绑定身份/认证终端/方向） */
    ContainerData data();

    /**
     * 远程绑定终端（GUI）：终端 ID + 绑定身份（调用者 UUID）。
     * 方向权限由具体口决定（输入口 INJECT / 输出口 EXTRACT），绑定动作本身要求 BUILD 权限。
     */
    void bindTerminal(UUID terminalId, UUID identity);

    /** 当前远程绑定的终端短 ID（8 位 hex；未选终端返回空串） */
    String boundTerminalShortId();

    /** 解绑（GUI 按钮），断开与终端的所有连接 */
    void unbind();

    /**
     * 绑定本口的玩家身份（未绑定为 null）。
     * <p>
     * 端口没有独立的"归属者"字段，<b>绑定者本人就是归属凭据</b>：改绑 / 解绑据此判定，
     * 否则任何路人右键一次就能把别人的物流改投到自己终端，或直接解绑让别人的口失效。
     */
    UUID bindingIdentity();

    /** 当前绑定的终端 ID（未绑定为 null）：改绑时用于复核"对原终端是否有布局权限" */
    UUID boundTerminalId();

    /** 宿主所在坐标：界面距离校验用（端口方块实体天然满足） */
    net.minecraft.core.BlockPos getBlockPos();

    /** 是否为输出口（输出口纯发电，输入口纯接收，GUI 提示不同） */
    boolean isOutput();
}
