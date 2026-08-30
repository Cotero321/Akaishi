package com.example.template.menu;

import com.example.template.block.entity.ChishiSurgeryBlockEntity;
import com.example.template.item.ModItems;
import com.example.template.life.body.BodySlot;
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
 * 手术仓菜单：器官输入槽 + 固态物槽 + 背包。
 * 数据槽：0/1=生命能量/容量 2=手术进度% 3=操作类型 4=目标槽位索引 5=固态数量。
 * 玩家躯体 9 槽位状态经 PlayerBodySync（S2C）同步，由 Screen 读取渲染。
 */
public class ChishiSurgeryMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;
    /** 方块坐标：C2S 手术开始包与手术进度绑定检查使用 */
    @Nullable
    private final BlockPos blockPos;
    /** 存储联动状态（3 格内存在存储库时注入联动槽，Screen 弹浮层存取） */
    @Nullable
    public StorageLinkState linkState;

    public ChishiSurgeryMenu(int id, Inventory inv, ChishiSurgeryBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getBlockPos());
    }

    /** 完整构造（包内供 ModMenus 网络工厂调用：附带方块坐标） */
    ChishiSurgeryMenu(int id, Inventory inv, Container container, ContainerData data, @Nullable BlockPos pos) {
        super(ModMenus.CHISHI_SURGERY.get(), id);
        this.container = container;
        this.data = data;
        this.blockPos = pos;

        // 器官输入槽：仅接受器官物品（槽位匹配在服务端手术开始时校验）
        addSlot(new OverlayHidingSlot(container, ChishiSurgeryBlockEntity.ORGAN_SLOT, 120, 28,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ChishiOrganItem;
            }
        });
        // 固态物槽：仅接受生命固态物
        addSlot(new OverlayHidingSlot(container, ChishiSurgeryBlockEntity.SOLID_SLOT, 120, 54,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.chishiLifeEssenceSolid.get());
            }
        });

        // 背包主区（3 行）与快捷栏（界面高 196：背包从 y=118 起）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 118 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 172));
        }
        addDataSlots(data);
        // 存储联动：3 格内存在存储库时注入联动槽（两侧工厂同世界状态，结果一致）
        this.linkState = StorageLink.tryLink(this::addSlot, inv.player.level(), pos);
    }

    public long getLifeEnergy() {
        return data.get(ChishiSurgeryBlockEntity.DATA_ENERGY);
    }

    public long getLifeMax() {
        return data.get(ChishiSurgeryBlockEntity.DATA_CAPACITY);
    }

    /** 手术进度（0-100） */
    public int getProgress() {
        return data.get(ChishiSurgeryBlockEntity.DATA_PROGRESS);
    }

    /** 当前操作类型（0 无 / 1 移植 / 2 摘除） */
    public int getOperation() {
        return data.get(ChishiSurgeryBlockEntity.DATA_OPERATION);
    }

    /** 当前目标槽位索引 */
    public int getTargetSlot() {
        return data.get(ChishiSurgeryBlockEntity.DATA_TARGET);
    }

    /** 固态物数量 */
    public int getSolidCount() {
        return data.get(ChishiSurgeryBlockEntity.DATA_SOLID);
    }

    /** 器官输入槽物品 */
    public ItemStack getOrganInput() {
        return container.getItem(ChishiSurgeryBlockEntity.ORGAN_SLOT);
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
            if (index < ChishiSurgeryBlockEntity.SLOT_COUNT) {
                if (!this.moveItemStackTo(current, ChishiSurgeryBlockEntity.SLOT_COUNT,
                        ChishiSurgeryBlockEntity.SLOT_COUNT + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slot instanceof LinkedVaultSlot) {
                // 联动存储 → 背包
                if (!this.moveItemStackTo(current, ChishiSurgeryBlockEntity.SLOT_COUNT,
                        ChishiSurgeryBlockEntity.SLOT_COUNT + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (current.getItem() instanceof ChishiOrganItem) {
                if (!this.moveItemStackTo(current, ChishiSurgeryBlockEntity.ORGAN_SLOT,
                        ChishiSurgeryBlockEntity.ORGAN_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (current.is(ModItems.chishiLifeEssenceSolid.get())) {
                if (!this.moveItemStackTo(current, ChishiSurgeryBlockEntity.SOLID_SLOT,
                        ChishiSurgeryBlockEntity.SOLID_SLOT + 1, false)) {
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
