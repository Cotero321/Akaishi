package com.example.akaishi.menu;

import com.mojang.datafixers.util.Pair;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 储存单元只读槽位：不接受放置也不可被拾取。
 * <p>
 * 注意<b>不覆写</b> {@link #set}：客户端 {@code initializeContents} 正是靠它把服务端下发的槽内容
 * 写进 {@link UnitReadOnlyContainer} 的镜像；服务端的写入由容器自身丢弃，故不构成旁路。
 * <p>
 * 客户端读到的是容器给出的<b>按类合并投影</b>（见 {@link UnitReadOnlyContainer}），不是物理槽内容。
 * 界面自绘这一趟用 {@link #getItem()} 取该投影；原版槽渲染那一趟则伪装成空槽（返回空堆），
 * 由界面自己按"图标 → 合并总量"的顺序绘制（原因见 {@code AkaishiItemStorageUnitMenu#vanillaRenderPass}）。
 */
public class UnitViewSlot extends Slot {

    private final AkaishiItemStorageUnitMenu menu;

    public UnitViewSlot(AkaishiItemStorageUnitMenu menu, Container container, int slot, int x, int y) {
        super(container, slot, x, y);
        this.menu = menu;
    }

    @Override
    public ItemStack getItem() {
        // 原版槽渲染这一趟返回空堆：让原版走 getNoItemIcon 分支（只画全透明图），不画物品，
        // 否则原版那批几何会在帧末 flush，把界面自绘的合并总量盖住
        if (this.menu.vanillaRenderPass()) {
            return ItemStack.EMPTY;
        }
        return super.getItem();
    }

    @Override
    public boolean hasItem() {
        return !this.menu.vanillaRenderPass() && super.hasItem();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        return ItemStack.EMPTY;
    }

    /**
     * 空槽位分支用的「全透明图标」：本槽绘制期靠 {@link #getItem()} 返回空堆伪装成空槽，
     * 这里返回全透明图让原版即使走该分支也画不出东西。
     */
    @Override
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return GuiWidgets.BLANK_SLOT_ICON;
    }
}
