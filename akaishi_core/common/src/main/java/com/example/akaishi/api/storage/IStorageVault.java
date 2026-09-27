package com.example.akaishi.api.storage;

import net.minecraft.world.Container;

/**
 * 可联动存储库：机器（手术仓/结构台/药剂台等）3 格范围内检测到本接口实现时，
 * 机器界面可弹出联动存取面板（见 menu.StorageLink）。
 * 器官储藏库与药剂库实现本接口，分别提供器官仓库与药剂仓库容器。
 */
public interface IStorageVault {

    /** 浮层标题翻译键 */
    String getVaultNameKey();

    /** 存储容器（全量槽位，联动面板按页访问） */
    Container getVaultContainer();
}
