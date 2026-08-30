package com.example.template.menu;

import com.example.template.block.entity.ChishiPotionTableBlockEntity;
import com.example.template.item.ModItems;
import com.example.template.life.potion.ChishiPotionItem;
import com.example.template.life.sample.ChishiLifeSampleItem;
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
 * 药剂台菜单：样本槽 + 固态物槽 + 药剂输出槽 + 背包。
 * 数据槽：0/1=生命能量/容量 2=制作进度% 3=当前模板索引（-1 未选择）。
 * 模板选择经 C2S 包写入服务端（见 ChishiPotionSync）。
 */
public class ChishiPotionTableMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;
    /** 方块坐标：C2S 模板选择包使用 */
    @Nullable
    private final BlockPos blockPos;
    /** 存储联动状态（3 格内存在存储库时注入联动槽） */
    @Nullable
    public StorageLinkState linkState;

    public ChishiPotionTableMenu(int id, Inventory inv, ChishiPotionTableBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getBlockPos());
    }

    /** 完整构造（包内供 ModMenus 网络工厂调用：附带方块坐标） */
    ChishiPotionTableMenu(int id, Inventory inv, Container container, ContainerData data, @Nullable BlockPos pos) {
        super(ModMenus.CHISHI_POTION_TABLE.get(), id);
        this.container = container;
        this.data = data;
        this.blockPos = pos;

        // 样本槽：仅接受纯度 ≥25 的生命样本
        addSlot(new OverlayHidingSlot(container, ChishiPotionTableBlockEntity.SAMPLE_SLOT, 56, 30,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ChishiLifeSampleItem
                        && ChishiLifeSampleItem.getPurity(stack) >= ChishiPotionTableBlockEntity.MIN_PURITY;
            }
        });
        // 固态物槽：仅接受生命固态物
        addSlot(new OverlayHidingSlot(container, ChishiPotionTableBlockEntity.SOLID_SLOT, 56, 56,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.chishiLifeEssenceSolid.get());
            }
        });
        // 输出槽：仅放行药剂（不可放入）
        addSlot(new OverlayHidingSlot(container, ChishiPotionTableBlockEntity.OUTPUT_SLOT, 116, 43,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // 背包主区与快捷栏
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
        // 存储联动：3 格内存在存储库时注入联动槽
        this.linkState = StorageLink.tryLink(this::addSlot, inv.player.level(), pos);
    }

    public long getLifeEnergy() {
        return data.get(ChishiPotionTableBlockEntity.DATA_ENERGY);
    }

    public long getLifeMax() {
        return data.get(ChishiPotionTableBlockEntity.DATA_CAPACITY);
    }

    /** 制作进度（0-100） */
    public int getProgress() {
        return data.get(ChishiPotionTableBlockEntity.DATA_PROGRESS);
    }

    /** 当前模板索引（-1 未选择） */
    public int getSelectedIndex() {
        return data.get(ChishiPotionTableBlockEntity.DATA_TEMPLATE);
    }

    @Nullable
    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < ChishiPotionTableBlockEntity.SLOT_COUNT) {
                if (!this.moveItemStackTo(current, ChishiPotionTableBlockEntity.SLOT_COUNT,
                        ChishiPotionTableBlockEntity.SLOT_COUNT + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slot instanceof LinkedVaultSlot) {
                // 联动存储 → 背包
                if (!this.moveItemStackTo(current, ChishiPotionTableBlockEntity.SLOT_COUNT,
                        ChishiPotionTableBlockEntity.SLOT_COUNT + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (current.getItem() instanceof ChishiLifeSampleItem
                    && ChishiLifeSampleItem.getPurity(current) >= ChishiPotionTableBlockEntity.MIN_PURITY) {
                if (!this.moveItemStackTo(current, ChishiPotionTableBlockEntity.SAMPLE_SLOT,
                        ChishiPotionTableBlockEntity.SAMPLE_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (current.is(ModItems.chishiLifeEssenceSolid.get())) {
                if (!this.moveItemStackTo(current, ChishiPotionTableBlockEntity.SOLID_SLOT,
                        ChishiPotionTableBlockEntity.SOLID_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (current.getItem() instanceof ChishiPotionItem) {
                if (!this.moveItemStackTo(current, ChishiPotionTableBlockEntity.OUTPUT_SLOT,
                        ChishiPotionTableBlockEntity.OUTPUT_SLOT + 1, false)) {
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
        return container.stillValid(player);
    }
}
