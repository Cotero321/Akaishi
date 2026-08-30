package com.example.template.menu;

import com.example.template.block.entity.ChishiGeneAnalyzerBlockEntity;
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
 * 生命分析台菜单：输入槽（纯度 ≥25 样本）+ 输出槽（基因序列片段）+ 背包。
 * 数据槽：0/1=生命能量/容量 2=解构进度百分比。
 */
public class ChishiGeneAnalyzerMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;
    /** 方块坐标：供存储联动检测与后续 C2S 操作使用（无方块时为空） */
    @Nullable
    private final BlockPos blockPos;
    /** 存储联动状态（3 格内存在存储库时注入联动槽） */
    @Nullable
    public StorageLinkState linkState;

    public ChishiGeneAnalyzerMenu(int id, Inventory inv, ChishiGeneAnalyzerBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getBlockPos());
    }

    public ChishiGeneAnalyzerMenu(int id, Inventory inv, Container container, ContainerData data) {
        this(id, inv, container, data, null);
    }

    /** 完整构造（包内供 ModMenus 网络工厂调用：附带方块坐标以支持存储联动） */
    ChishiGeneAnalyzerMenu(int id, Inventory inv, Container container, ContainerData data, @Nullable BlockPos pos) {
        super(ModMenus.CHISHI_GENE_ANALYZER.get(), id);
        this.container = container;
        this.data = data;
        this.blockPos = pos;

        // 输入槽：仅接受纯度 ≥25 的生命样本（未达解构门槛只能用于药剂）
        addSlot(new OverlayHidingSlot(container, ChishiGeneAnalyzerBlockEntity.INPUT_SLOT, 56, 30,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ChishiLifeSampleItem
                        && ChishiLifeSampleItem.getPurity(stack) >= ChishiGeneAnalyzerBlockEntity.MIN_PURITY;
            }
        });
        // 输出槽：只出不进
        addSlot(new OverlayHidingSlot(container, ChishiGeneAnalyzerBlockEntity.OUTPUT_SLOT, 116, 30,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
        // 存储联动：3 格内存在存储库时注入联动槽（两侧工厂同世界状态，结果一致）
        this.linkState = StorageLink.tryLink(this::addSlot, inv.player.level(), pos);
    }

    public long getLifeEnergy() {
        return data.get(0);
    }

    public long getLifeMax() {
        return data.get(1);
    }

    /** 解构进度（0-100） */
    public int getProgress() {
        return data.get(ChishiGeneAnalyzerBlockEntity.DATA_PROGRESS);
    }

    /** 当前输入样本对应纯度的解构成功率（无样本返回 0） */
    public int getSuccessRate() {
        ItemStack input = container.getItem(ChishiGeneAnalyzerBlockEntity.INPUT_SLOT);
        if (input.getItem() instanceof ChishiLifeSampleItem) {
            return Math.round(ChishiGeneAnalyzerBlockEntity.successRate(ChishiLifeSampleItem.getPurity(input)) * 100);
        }
        return 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < ChishiGeneAnalyzerBlockEntity.SLOT_COUNT) {
                if (!this.moveItemStackTo(current, ChishiGeneAnalyzerBlockEntity.SLOT_COUNT,
                        ChishiGeneAnalyzerBlockEntity.SLOT_COUNT + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slot instanceof LinkedVaultSlot) {
                // 联动存储 → 背包
                if (!this.moveItemStackTo(current, ChishiGeneAnalyzerBlockEntity.SLOT_COUNT,
                        ChishiGeneAnalyzerBlockEntity.SLOT_COUNT + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (current.getItem() instanceof ChishiLifeSampleItem
                    && ChishiLifeSampleItem.getPurity(current) >= ChishiGeneAnalyzerBlockEntity.MIN_PURITY) {
                // 纯度 ≥25 的样本移入输入槽（序列片段非样本，走下方 return 不回收）
                if (!this.moveItemStackTo(current, ChishiGeneAnalyzerBlockEntity.INPUT_SLOT,
                        ChishiGeneAnalyzerBlockEntity.INPUT_SLOT + 1, false)) {
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
