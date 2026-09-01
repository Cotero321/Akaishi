package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiPotionTableBlockEntity;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.sample.AkaishiLifeSampleItem;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
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
 * 模板选择经 C2S 包写入服务端（见 AkaishiPotionSync）。
 */
public class AkaishiPotionTableMenu extends AbstractContainerMenu {

    /** 机器区槽数（升级槽 2 + 样本/固态/输出槽 3），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = MachineUpgradeSlots.SLOT_COUNT
            + AkaishiPotionTableBlockEntity.SLOT_COUNT;

    private final Container container;
    private final ContainerData data;
    private final Container upgrades;
    /** 方块坐标：C2S 模板选择包使用 */
    @Nullable
    private final BlockPos blockPos;
    /** 存储联动状态（3 格内存在存储库时注入联动槽） */
    @Nullable
    public StorageLinkState linkState;

    public AkaishiPotionTableMenu(int id, Inventory inv, AkaishiPotionTableBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getUpgradeSlots(), be.getBlockPos());
    }

    /** 完整构造（包内供 ModMenus 网络工厂调用：附带方块坐标） */
    AkaishiPotionTableMenu(int id, Inventory inv, Container container, ContainerData data,
                          Container upgrades, @Nullable BlockPos pos) {
        super(ModMenus.CHISHI_POTION_TABLE.get(), id);
        this.container = container;
        this.data = data;
        this.upgrades = upgrades;
        this.blockPos = pos;

        // 升级槽（速度/能量各一格，mayPlace 由 MachineUpgradeSlots 按类型互斥过滤；右上能量条下方空位）
        addSlot(new Slot(upgrades, MachineUpgradeSlots.SLOT_SPEED, 134, 30));
        addSlot(new Slot(upgrades, MachineUpgradeSlots.SLOT_ENERGY, 152, 30));

        // 样本槽：仅接受纯度 ≥25 的生命样本
        addSlot(new OverlayHidingSlot(container, AkaishiPotionTableBlockEntity.SAMPLE_SLOT, 56, 30,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof AkaishiLifeSampleItem
                        && AkaishiLifeSampleItem.getPurity(stack) >= AkaishiPotionTableBlockEntity.MIN_PURITY;
            }
        });
        // 固态物槽：仅接受生命固态物
        addSlot(new OverlayHidingSlot(container, AkaishiPotionTableBlockEntity.SOLID_SLOT, 56, 56,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.akaishiLifeEssenceSolid.get());
            }
        });
        // 输出槽：仅放行药剂（不可放入）
        addSlot(new OverlayHidingSlot(container, AkaishiPotionTableBlockEntity.OUTPUT_SLOT, 116, 43,
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
        return data.get(AkaishiPotionTableBlockEntity.DATA_ENERGY);
    }

    public long getLifeMax() {
        return data.get(AkaishiPotionTableBlockEntity.DATA_CAPACITY);
    }

    /** 制作进度（0-100） */
    public int getProgress() {
        return data.get(AkaishiPotionTableBlockEntity.DATA_PROGRESS);
    }

    /** 当前模板索引（-1 未选择） */
    public int getSelectedIndex() {
        return data.get(AkaishiPotionTableBlockEntity.DATA_TEMPLATE);
    }

    /** 速度升级组件数量（0~8） */
    public int getSpeedUpgradeCount() {
        return upgrades.getItem(MachineUpgradeSlots.SLOT_SPEED).getCount();
    }

    /** 能量升级组件数量（0~8） */
    public int getEnergyUpgradeCount() {
        return upgrades.getItem(MachineUpgradeSlots.SLOT_ENERGY).getCount();
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
            if (index < MACHINE_SLOT_END) {
                // 机器区（升级槽 + 样本/固态/输出槽）→ 玩家背包
                if (!this.moveItemStackTo(current, MACHINE_SLOT_END,
                        MACHINE_SLOT_END + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slot instanceof LinkedVaultSlot) {
                // 联动存储 → 背包
                if (!this.moveItemStackTo(current, MACHINE_SLOT_END,
                        MACHINE_SLOT_END + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包：升级组件进升级槽，样本/固态物按 mayPlace 自动进对应槽（输出槽只读跳过）
                if (!this.moveItemStackTo(current, 0, MACHINE_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
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
