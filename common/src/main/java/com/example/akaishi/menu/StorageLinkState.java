package com.example.akaishi.menu;

import net.minecraft.world.Container;
import org.jetbrains.annotations.Nullable;

/**
 * 机器菜单的存储联动状态（客户端本地交互状态 + 联动容器引用）。
 * 由 StorageLink.tryLink 创建并注入菜单；Screen 读取并切换 open/page。
 */
public class StorageLinkState {

    /** 联动存储容器（null = 无联动，槽位不可用） */
    @Nullable
    public Container storage;
    /** 存储库名称翻译键（浮层标题） */
    public String nameKey = "";
    /** 面板是否打开 */
    public boolean open;
    /** 当前分页 */
    public int page;

    public boolean canPagePrev() {
        return page > 0;
    }

    public boolean canPageNext() {
        return storage != null && page < (storage.getContainerSize() + StorageLink.PAGE_SLOTS - 1) / StorageLink.PAGE_SLOTS - 1;
    }

    /** 翻页并钳制范围 */
    public void flip(int delta) {
        if (storage == null) {
            return;
        }
        int max = (storage.getContainerSize() + StorageLink.PAGE_SLOTS - 1) / StorageLink.PAGE_SLOTS - 1;
        page = Math.max(0, Math.min(max, page + delta));
    }
}
