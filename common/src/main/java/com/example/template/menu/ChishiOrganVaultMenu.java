package com.example.template.menu;

import com.example.template.block.entity.ChishiOrganVaultBlockEntity;
import com.example.template.life.organ.ChishiOrganItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 器官储藏库菜单：
 * - 页槽 9 个：映射"当前选中页"的容器槽位（Slots 通过 BE 页映射访问，切页仅本地状态）
 * - 暂存槽 9 个：输出缓冲（任意器官）
 * - 归类由 canPlaceItem 保证：器官只能放入所属槽位页
 * 选页纯客户端本地状态（ChishiOrganVaultScreen 切换），无需网络包。
 */
public class ChishiOrganVaultMenu extends AbstractContainerMenu {

    /** 当前选中页（客户端本地状态，服务端无意义） */
    private int currentPage;
    @Nullable
    private final ChishiOrganVaultBlockEntity vault;
    private final ContainerData data;

    public ChishiOrganVaultMenu(int id, Inventory inv, ChishiOrganVaultBlockEntity vault) {
        super(ModMenus.CHISHI_ORGAN_VAULT.get(), id);
        this.vault = vault;
        this.data = vault.data();

        // 页槽 3×3（映射当前页）
        for (int i = 0; i < ChishiOrganVaultBlockEntity.PER_PAGE; i++) {
            addSlot(new VaultSlot(vault, 0, i,
                    66 + (i % 3) * 18, 22 + (i / 3) * 18));
        }
        // 暂存槽 3×3（输出缓冲）
        for (int i = 0; i < ChishiOrganVaultBlockEntity.TEMP_SIZE; i++) {
            addSlot(new VaultSlot(vault, -1, i,
                    124 + (i % 3) * 18, 22 + (i / 3) * 18));
        }
        // 背包 3 行 + 快捷栏
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 96 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 154));
        }
        addDataSlots(data);
    }

    /** 网络工厂退化构造（BE 丢失兜底，页槽直连 Container） */
    public ChishiOrganVaultMenu(int id, Inventory inv, Container container, ContainerData data, BlockPos pos) {
        super(ModMenus.CHISHI_ORGAN_VAULT.get(), id);
        this.vault = null;
        this.data = data;
        for (int i = 0; i < ChishiOrganVaultBlockEntity.PER_PAGE; i++) {
            addSlot(new Slot(container, i, 66 + (i % 3) * 18, 22 + (i / 3) * 18));
        }
        for (int i = 0; i < ChishiOrganVaultBlockEntity.TEMP_SIZE; i++) {
            addSlot(new Slot(container, ChishiOrganVaultBlockEntity.TEMP_START + i,
                    124 + (i % 3) * 18, 22 + (i / 3) * 18));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 96 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 154));
        }
        addDataSlots(data);
    }

    /** 客户端切换选中页（纯本地） */
    public void setCurrentPage(int page) {
        this.currentPage = Math.max(0, Math.min(ChishiOrganVaultBlockEntity.PAGE_COUNT - 1, page));
        // 页槽映射随页变化，直接重绘
        for (int i = 0; i < ChishiOrganVaultBlockEntity.PER_PAGE; i++) {
            Slot slot = this.slots.get(i);
            if (slot instanceof VaultSlot vaultSlot) {
                vaultSlot.page = this.currentPage;
            }
        }
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public long getLifeEnergy() {
        return data.get(ChishiOrganVaultBlockEntity.DATA_ENERGY);
    }

    public long getLifeMax() {
        return data.get(ChishiOrganVaultBlockEntity.DATA_CAPACITY);
    }

    /** 活性状态（1 活性 0 休眠） */
    public boolean isActive() {
        return data.get(ChishiOrganVaultBlockEntity.DATA_ACTIVE) > 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            int machineEnd = ChishiOrganVaultBlockEntity.PER_PAGE + ChishiOrganVaultBlockEntity.TEMP_SIZE;
            if (index < machineEnd) {
                // 库内 → 背包
                if (!this.moveItemStackTo(current, machineEnd, machineEnd + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (current.getItem() instanceof ChishiOrganItem) {
                // 背包 → 当前显示页槽（器官页匹配时）或暂存区（canPlaceItem 裁决归类）
                if (!this.moveItemStackTo(current, 0, machineEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
            if (current.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (current.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, current);
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        if (vault != null) {
            return vault.stillValid(player);
        }
        return true;
    }

    /** 页映射槽：按选中页访问 BE 容器槽位（切页时 page 被 Menu 重写） */
    static class VaultSlot extends Slot {
        private final ChishiOrganVaultBlockEntity vault;
        private int page;          // -1 = 暂存区
        private final int indexInPage;

        VaultSlot(ChishiOrganVaultBlockEntity vault, int page, int indexInPage, int x, int y) {
            super(vault, indexInPage, x, y);
            this.vault = vault;
            this.page = page;
            this.indexInPage = indexInPage;
        }

        @Override
        public ItemStack getItem() {
            if (page < 0) {
                return vault.getTempItem(indexInPage);
            }
            return vault.getItemAt(page, indexInPage);
        }

        @Override
        public void set(ItemStack stack) {
            if (page < 0) {
                vault.setTempItem(indexInPage, stack);
            } else {
                vault.setItemAt(page, indexInPage, stack);
            }
            setChanged();
        }

        @Override
        public ItemStack remove(int amount) {
            ItemStack current = getItem();
            ItemStack split = current.split(amount);
            set(current);
            return split;
        }

        @Override
        public int getMaxStackSize() {
            return 1; // 器官个体不可堆叠
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return vault.canPlaceItem(actualIndex(), stack);
        }

        /** 真实容器索引（canPlaceItem 校验用） */
        private int actualIndex() {
            if (page < 0) {
                return ChishiOrganVaultBlockEntity.TEMP_START + indexInPage;
            }
            return page * ChishiOrganVaultBlockEntity.PER_PAGE + indexInPage;
        }
    }
}
