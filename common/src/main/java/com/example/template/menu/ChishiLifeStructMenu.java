package com.example.template.menu;

import com.example.template.block.entity.ChishiLifeStructBlockEntity;
import com.example.template.item.ModItems;
import com.example.template.life.body.BodySlot;
import com.example.template.life.organ.OrganEffectRegistry;
import com.example.template.life.sequence.ChishiGeneSequenceItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 生命结构台菜单：输入槽（基因序列）+ 材料槽（固态物）+ 输出槽（器官）+ 背包。
 * 数据槽：0/1=生命能量/容量 2=构造进度% 3=目标槽位索引（BodySlot.values()）。
 */
public class ChishiLifeStructMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;
    /** 方块坐标：客户端点击槽位按钮时通过 C2S 包选择目标（无方块时为空） */
    @Nullable
    private final BlockPos blockPos;
    /** 存储联动状态（3 格内存在存储库时注入联动槽） */
    @Nullable
    public StorageLinkState linkState;

    public ChishiLifeStructMenu(int id, Inventory inv, ChishiLifeStructBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getBlockPos());
    }

    public ChishiLifeStructMenu(int id, Inventory inv, Container container, ContainerData data) {
        this(id, inv, container, data, null);
    }

    /** 完整构造（包内供 ModMenus 网络工厂调用：附带方块坐标以支持界面选槽位） */
    ChishiLifeStructMenu(int id, Inventory inv, Container container, ContainerData data, @Nullable BlockPos pos) {
        super(ModMenus.CHISHI_LIFE_STRUCT.get(), id);
        this.container = container;
        this.data = data;
        this.blockPos = pos;

        // 输入槽：仅接受基因序列
        addSlot(new OverlayHidingSlot(container, ChishiLifeStructBlockEntity.INPUT_SLOT, 30, 30,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ChishiGeneSequenceItem;
            }
        });
        // 材料槽：仅接受生命固态物
        addSlot(new OverlayHidingSlot(container, ChishiLifeStructBlockEntity.SOLID_SLOT, 56, 30,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.chishiLifeEssenceSolid.get());
            }
        });
        // 输出槽：只出不进
        addSlot(new OverlayHidingSlot(container, ChishiLifeStructBlockEntity.OUTPUT_SLOT, 116, 30,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 124 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 180));
        }
        addDataSlots(data);
        // 存储联动：3 格内存在存储库时注入联动槽
        this.linkState = StorageLink.tryLink(this::addSlot, inv.player.level(), pos);
    }

    public long getLifeEnergy() {
        return data.get(0);
    }

    public long getLifeMax() {
        return data.get(1);
    }

    /** 构造进度（0-100） */
    public int getProgress() {
        return data.get(ChishiLifeStructBlockEntity.DATA_PROGRESS);
    }

    /** 当前目标槽位索引 */
    public int getTargetSlot() {
        return data.get(ChishiLifeStructBlockEntity.DATA_TARGET);
    }

    /** 当前序列生物的可用器官槽位（Screen 高亮用） */
    public List<BodySlot> getAvailableSlots() {
        ItemStack input = container.getItem(ChishiLifeStructBlockEntity.INPUT_SLOT);
        if (!(input.getItem() instanceof ChishiGeneSequenceItem)) {
            return List.of();
        }
        return OrganEffectRegistry.availableSlots(ChishiGeneSequenceItem.getEntityId(input));
    }

    /** 当前序列生物显示名（Screen 标题下提示用），无序列返回 null */
    @Nullable
    public String getSequenceEntityId() {
        ItemStack input = container.getItem(ChishiLifeStructBlockEntity.INPUT_SLOT);
        return input.getItem() instanceof ChishiGeneSequenceItem
                ? ChishiGeneSequenceItem.getEntityId(input) : null;
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
            if (index < ChishiLifeStructBlockEntity.SLOT_COUNT) {
                if (!this.moveItemStackTo(current, ChishiLifeStructBlockEntity.SLOT_COUNT,
                        ChishiLifeStructBlockEntity.SLOT_COUNT + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slot instanceof LinkedVaultSlot) {
                // 联动存储 → 背包
                if (!this.moveItemStackTo(current, ChishiLifeStructBlockEntity.SLOT_COUNT,
                        ChishiLifeStructBlockEntity.SLOT_COUNT + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (current.getItem() instanceof ChishiGeneSequenceItem) {
                if (!this.moveItemStackTo(current, ChishiLifeStructBlockEntity.INPUT_SLOT,
                        ChishiLifeStructBlockEntity.INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (current.is(ModItems.chishiLifeEssenceSolid.get())) {
                if (!this.moveItemStackTo(current, ChishiLifeStructBlockEntity.SOLID_SLOT,
                        ChishiLifeStructBlockEntity.SOLID_SLOT + 1, false)) {
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
