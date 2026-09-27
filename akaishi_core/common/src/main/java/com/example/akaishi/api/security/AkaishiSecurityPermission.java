package com.example.akaishi.api.security;

import java.util.Locale;

import net.minecraft.network.chat.Component;

/**
 * 终端安全权限位（对齐 AE2 {@code SecurityPermissions} 的五档）。
 * <p>
 * 语义映射到本项目：
 * <ul>
 *   <li>{@link #INJECT} 存入 —— 物品终端存入物品、无线输入口向终端推送能量；</li>
 *   <li>{@link #EXTRACT} 取出 —— 物品终端取出物品、无线输出口从终端抽取能量；</li>
 *   <li>{@link #CRAFT} 请求 —— 请求式取用（便携终端「随身供能」）；</li>
 *   <li>{@link #BUILD} 布局 —— 改动网络物理布局（无线输入/输出口的远程绑定与解绑）；</li>
 *   <li>{@link #SECURITY} 安全 —— 修改终端安全设置（登记/移除身份卡、勾选权限）。</li>
 * </ul>
 * 权限按序号取比特，整表以 {@code int} 位掩码持久化与同步；归属者（放置者）恒为 {@link #ALL}。
 * 权限位本身只是数据，实际放行一律由各入口调用判定函数（见 {@code WirelessNetworkManager}）。
 */
public enum AkaishiSecurityPermission {

    INJECT,
    EXTRACT,
    CRAFT,
    BUILD,
    SECURITY;

    /** 全权限掩码（归属者恒为全权限） */
    public static final int ALL = (1 << 5) - 1;

    /** 无权限掩码 */
    public static final int NONE = 0;

    /** 本权限对应的位 */
    public int bit() {
        return 1 << ordinal();
    }

    /** 掩码是否含本权限 */
    public boolean granted(int mask) {
        return (mask & bit()) != 0;
    }

    /** 按 granted 置位/清位后返回新掩码 */
    public int apply(int mask, boolean granted) {
        return granted ? (mask | bit()) : (mask & ~bit());
    }

    /** 双语键前缀：{@code gui.akaishi.security.<小写名>} */
    public String langKey() {
        return "gui.akaishi.security." + name().toLowerCase(Locale.ROOT);
    }

    /** 权限名（GUI 勾选项 / 悬浮文本） */
    public Component displayName() {
        return Component.translatable(langKey() + ".name");
    }

    /** 权限说明（悬浮文本） */
    public Component displayHint() {
        return Component.translatable(langKey() + ".tip");
    }

    /** 权限缩写（安全页勾选框列头：中文单字 / 英文单字母） */
    public Component shortName() {
        return Component.translatable(langKey() + ".short");
    }
}
