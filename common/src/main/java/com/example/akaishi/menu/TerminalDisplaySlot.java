package com.example.akaishi.menu;

import com.mojang.datafixers.util.Pair;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 库页客户端只读虚拟槽（对齐 AE2 {@code ClientReadOnlySlot} + {@code RepoSlot}）。
 * <p>
 * 服务端<b>没有</b>对应槽位：本槽只在本机渲染与命中判定中被使用，内容取自客户端条目仓库，
 * 因此原版点击管线永远不会改写它。所有写操作一律由 {@link AkaishiItemTerminalSync} 送到服务端落账，
 * 从结构上排除「客户端本地改内容造成与服务端脱节」。
 * <p>
 * 用一个 0 容量的假容器兜底：其它模组若按「容器 + 槽下标」反查本槽，只会拿到空容器而非真实存储。
 */
public class TerminalDisplaySlot extends Slot {

    private static final Container EMPTY_INVENTORY = new SimpleContainer(0);

    private final AkaishiItemTerminalMenu menu;
    /** 可视区固定下标（0 ~ VIEW_SLOTS-1）：实际条目 = 滚动行 × 列数 + 本下标 */
    private final int viewIndex;

    public TerminalDisplaySlot(AkaishiItemTerminalMenu menu, int viewIndex, int x, int y) {
        super(EMPTY_INVENTORY, 0, x, y);
        this.menu = menu;
        this.viewIndex = viewIndex;
    }

    /**
     * 是否参与原版 hover 高亮 / 悬浮文本 / 快速搬运。
     * <p>
     * 安全页等「覆盖式页面」在库区之上绘制时置 false：否则这些看不见的格子仍会被
     * {@code AbstractContainerScreen.getHoveredSlot} 命中，导致上一页的槽位高亮与悬浮文本
     * 穿透到当前页（"隐藏的上一页格子"）。
     */
    private boolean active = true;

    /** 由界面按页面切换 */
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean isActive() {
        return this.active;
    }

    /** 当前应显示的条目（滚动行参与换算），越界为 null */
    public AkaishiItemTerminalSync.Entry entry() {
        return this.menu.entryAt(this.viewIndex);
    }

    @Override
    public ItemStack getItem() {
        // 原版槽渲染这一趟返回空堆：让原版走 getNoItemIcon 分支（只画全透明图），不画物品与数量，
        // 从而保证"图标 → 数字"完全由界面自绘、顺序自控（详见菜单里 vanillaRenderPass 的说明）
        if (this.menu.vanillaRenderPass()) {
            return ItemStack.EMPTY;
        }
        AkaishiItemTerminalSync.Entry entry = entry();
        return entry == null ? ItemStack.EMPTY : entry.display();
    }

    @Override
    public boolean hasItem() {
        // 同 getItem()：这一趟要对原版伪装成空槽，否则原版会基于真实物品走 tooltip/高亮逻辑
        return !this.menu.vanillaRenderPass() && entry() != null;
    }

    // 只读：原版任何写入/取出路径都被挡在门外（AE2 同款做法）
    @Override
    public final boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public final void set(ItemStack stack) {
    }

    @Override
    public final int getMaxStackSize() {
        return 0;
    }

    @Override
    public final ItemStack remove(int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public final boolean mayPickup(Player player) {
        return false;
    }

    /**
     * 空槽位分支用的「全透明图标」。
     * <p>
     * 注意 1.20.1 的 {@code AbstractContainerScreen.renderSlot}（已反汇编核对）只在
     * <b>槽里没有物品</b>时才会走这一分支；槽里有物品时原版一定会 {@code renderItem}。
     * 所以本槽绘制期靠 {@link #getItem()} 返回空堆来"伪装成空槽"（见 {@code vanillaRenderPass}），
     * 这里返回全透明图让原版即使走该分支也画不出东西。
     */
    @Override
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return GuiWidgets.BLANK_SLOT_ICON;
    }
}
