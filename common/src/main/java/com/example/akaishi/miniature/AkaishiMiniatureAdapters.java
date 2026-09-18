package com.example.akaishi.miniature;

import com.example.akaishi.api.miniature.MiniatureTerminalRegistry;

/**
 * 终端微缩适配器汇总注册入口。
 * <p>
 * 微缩机制（方块、持久化、能力转发、掉落保留）本身<b>通用</b>，各族只在此登记一支适配器：
 * 用存盘数据还原本族状态、提供本族界面入口。新增终端族只追加一行，通用层零改动（OCP）。
 * <p>
 * <b>只对终端开放</b>：非终端机器不在此注册，即天然不可微缩。
 */
public final class AkaishiMiniatureAdapters {

    private AkaishiMiniatureAdapters() {
    }

    /** 注册全部终端族的微缩适配器（由 AkaishiMod.init 调用） */
    public static void register() {
        MiniatureTerminalRegistry.register(new ItemTerminalMiniatureAdapter());
        // 两个无线能量族：同一支参数化适配器的两个实例（族 + 能量类型 + 显示名不同）
        MiniatureTerminalRegistry.register(WirelessTerminalMiniatureAdapter.CHISHI);
        MiniatureTerminalRegistry.register(WirelessTerminalMiniatureAdapter.LIFE);
    }
}
