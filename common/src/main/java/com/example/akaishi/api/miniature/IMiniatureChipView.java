package com.example.akaishi.api.miniature;

import com.example.akaishi.wireless.TerminalSecurity;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 微缩终端「芯片视图」：宿主（微缩矩阵终端等）读取一枚芯片时所需的<b>全部读数</b>，
 * 与终端族无关 —— 物品族与能量族的差异在实现里被抹平，宿主不必知道对面是哪种终端。
 * <p>
 * 由 {@link com.example.akaishi.block.entity.MiniatureTerminalBlockEntity} 实现：
 * 它已经持有族状态，把两种宿主视图（{@code IItemTerminalHost} / {@code IWirelessTerminalHost}）
 * 归一成下面这组读数即可。
 * <p>
 * 约定：<b>未装载的芯片</b>（空壳）除 {@link #loaded()} 与 {@link #displayName()} 外一律返回 null / 0，
 * 宿主据此显示"空壳"而不是伪造读数。
 */
public interface IMiniatureChipView {

    /** 终端唯一 ID（未装载为 null） */
    @Nullable
    UUID terminalId();

    /** 芯片族 id（未装载为 null） */
    @Nullable
    ResourceLocation chipType();

    /** 是否已装载终端数据（空壳为 false） */
    boolean loaded();

    /** 显示名（已装载取该族终端名；空壳取通用名） */
    Component displayName();

    /** 安全表（未装载为 null）：矩阵据此做统一安全入口的同步 */
    @Nullable
    TerminalSecurity security();

    /**
     * 物品终端宿主视图（非物品族 / 空壳为 null）。
     * <p>
     * <b>为什么要有这个出口</b>：微缩件本身不是 {@link com.example.akaishi.api.storage.IItemTerminalHost}
     * （库与落账能力在族状态里），外部消费者（储存无线输入/输出口按 ID 寻址等）只按
     * {@code blockEntity instanceof IItemTerminalHost} 判就会把微缩后的终端当成"没有终端"，
     * 表现为绑定清单里消失、已绑定的口也搬不动。走这里即可拿到同一份契约视图。
     */
    @Nullable
    default com.example.akaishi.api.storage.IItemTerminalHost itemHost() {
        return null;
    }

    /**
     * 库读数（IP）：仅物品族有；其它族返回 null。
     * <p>
     * 由各储存单元的 IP 账本求和得出，不额外缓存 —— 与库页显示同源。
     */
    @Nullable
    IpReadout itemReadout();

    /** 能量读数：自研能量池（物品族=赤能源缓冲；能量族=储能池）；无能量族返回 null */
    @Nullable
    EnergyReadout energyReadout();

    /** 库读数（占用 / 容量，单位 IP） */
    record IpReadout(long stored, long capacity) {
    }

    /** 能量读数（储量 / 容量） */
    record EnergyReadout(long stored, long capacity) {
    }
}
