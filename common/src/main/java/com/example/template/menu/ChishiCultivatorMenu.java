package com.example.template.menu;

import com.example.template.block.entity.ChishiCultivatorBlockEntity;
import com.example.template.item.ModItems;
import com.example.template.life.organ.ChishiOrganItem;
import com.example.template.life.organ.QualityTier;
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
 * 部件培养舱菜单：输入槽（样本或器官）+ 材料槽（固态物）+ 背包。
 * 数据槽：0/1=生命能量/容量 2=进度% 3=模式（0 提纯 / 1 升级 / 2 空闲）。
 */
public class ChishiCultivatorMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;
    /** 方块坐标：供存储联动检测与后续 C2S 操作使用（无方块时为空） */
    @Nullable
    private final BlockPos blockPos;
    /** 存储联动状态（3 格内存在存储库时注入联动槽） */
    @Nullable
    public StorageLinkState linkState;

    public ChishiCultivatorMenu(int id, Inventory inv, ChishiCultivatorBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getBlockPos());
    }

    public ChishiCultivatorMenu(int id, Inventory inv, Container container, ContainerData data) {
        this(id, inv, container, data, null);
    }

    /** 完整构造（包内供 ModMenus 网络工厂调用：附带方块坐标以支持存储联动） */
    ChishiCultivatorMenu(int id, Inventory inv, Container container, ContainerData data, @Nullable BlockPos pos) {
        super(ModMenus.CHISHI_CULTIVATOR.get(), id);
        this.container = container;
        this.data = data;
        this.blockPos = pos;

        // 输入槽：生命样本（纯度 <100，满纯度无需再提纯）或器官（品质 <IV）
        addSlot(new OverlayHidingSlot(container, ChishiCultivatorBlockEntity.INPUT_SLOT, 56, 30,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (stack.getItem() instanceof ChishiLifeSampleItem) {
                    return ChishiLifeSampleItem.getPurity(stack) < 100;
                }
                if (stack.getItem() instanceof ChishiOrganItem) {
                    QualityTier tier = ChishiOrganItem.getTier(stack);
                    return tier != null && tier.next() != null;
                }
                return false;
            }
        });
        // 材料槽：仅固态物
        addSlot(new OverlayHidingSlot(container, ChishiCultivatorBlockEntity.SOLID_SLOT, 56, 52,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.chishiLifeEssenceSolid.get());
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

    public int getProgress() {
        return data.get(ChishiCultivatorBlockEntity.DATA_PROGRESS);
    }

    public int getMode() {
        return data.get(ChishiCultivatorBlockEntity.DATA_MODE);
    }

    /** 当前输入物品对应档位的成功率（提纯按纯度区间、升级按品质，无输入返回 0） */
    public int getSuccessRate() {
        ItemStack input = container.getItem(ChishiCultivatorBlockEntity.INPUT_SLOT);
        if (input.getItem() instanceof ChishiLifeSampleItem) {
            return ChishiCultivatorBlockEntity.purifyRate(ChishiLifeSampleItem.getPurity(input));
        }
        if (input.getItem() instanceof ChishiOrganItem) {
            QualityTier tier = ChishiOrganItem.getTier(input);
            if (tier != null && tier.next() != null) {
                return ChishiCultivatorBlockEntity.upgradeRate(tier);
            }
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
            if (index < ChishiCultivatorBlockEntity.SLOT_COUNT) {
                if (!this.moveItemStackTo(current, ChishiCultivatorBlockEntity.SLOT_COUNT,
                        ChishiCultivatorBlockEntity.SLOT_COUNT + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slot instanceof LinkedVaultSlot) {
                // 联动存储 → 背包
                if (!this.moveItemStackTo(current, ChishiCultivatorBlockEntity.SLOT_COUNT,
                        ChishiCultivatorBlockEntity.SLOT_COUNT + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (current.is(ModItems.chishiLifeEssenceSolid.get())) {
                // 固态物 → 材料槽
                if (!this.moveItemStackTo(current, ChishiCultivatorBlockEntity.SOLID_SLOT,
                        ChishiCultivatorBlockEntity.SOLID_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if ((current.getItem() instanceof ChishiLifeSampleItem
                    && ChishiLifeSampleItem.getPurity(current) < 100)
                    || (current.getItem() instanceof ChishiOrganItem
                    && ChishiOrganItem.getTier(current) != null
                    && ChishiOrganItem.getTier(current).next() != null)) {
                // 可培养的样本/器官 → 输入槽
                if (!this.moveItemStackTo(current, ChishiCultivatorBlockEntity.INPUT_SLOT,
                        ChishiCultivatorBlockEntity.INPUT_SLOT + 1, false)) {
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
