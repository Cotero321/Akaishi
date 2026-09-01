package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiCultivatorBlockEntity;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.organ.AkaishiOrganItem;
import com.example.akaishi.life.organ.QualityTier;
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
 * 部件培养舱菜单：输入槽（样本或器官）+ 材料槽（固态物）+ 背包。
 * 数据槽：0/1=生命能量/容量 2=进度% 3=模式（0 提纯 / 1 升级 / 2 空闲）。
 */
public class AkaishiCultivatorMenu extends AbstractContainerMenu {

    /** 机器区槽数（升级槽 2 + 输入/材料槽 2），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = MachineUpgradeSlots.SLOT_COUNT
            + AkaishiCultivatorBlockEntity.SLOT_COUNT;

    private final Container container;
    private final ContainerData data;
    private final Container upgrades;
    /** 方块坐标：供存储联动检测与后续 C2S 操作使用（无方块时为空） */
    @Nullable
    private final BlockPos blockPos;
    /** 存储联动状态（3 格内存在存储库时注入联动槽） */
    @Nullable
    public StorageLinkState linkState;

    public AkaishiCultivatorMenu(int id, Inventory inv, AkaishiCultivatorBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getUpgradeSlots(), be.getBlockPos());
    }

    public AkaishiCultivatorMenu(int id, Inventory inv, Container container, ContainerData data) {
        this(id, inv, container, data, new MachineUpgradeSlots(), null);
    }

    /** 完整构造（包内供 ModMenus 网络工厂调用：附带方块坐标以支持存储联动） */
    AkaishiCultivatorMenu(int id, Inventory inv, Container container, ContainerData data,
                         Container upgrades, @Nullable BlockPos pos) {
        super(ModMenus.CHISHI_CULTIVATOR.get(), id);
        this.container = container;
        this.data = data;
        this.upgrades = upgrades;
        this.blockPos = pos;

        // 升级槽（速度/能量各一格，mayPlace 由 MachineUpgradeSlots 按类型互斥过滤；右侧空位）
        addSlot(new Slot(upgrades, MachineUpgradeSlots.SLOT_SPEED, 134, 30));
        addSlot(new Slot(upgrades, MachineUpgradeSlots.SLOT_ENERGY, 152, 30));

        // 输入槽：生命样本（纯度 <100，满纯度无需再提纯）或器官（品质 <IV）
        addSlot(new OverlayHidingSlot(container, AkaishiCultivatorBlockEntity.INPUT_SLOT, 56, 30,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (stack.getItem() instanceof AkaishiLifeSampleItem) {
                    return AkaishiLifeSampleItem.getPurity(stack) < 100;
                }
                if (stack.getItem() instanceof AkaishiOrganItem) {
                    QualityTier tier = AkaishiOrganItem.getTier(stack);
                    return tier != null && tier.next() != null;
                }
                return false;
            }
        });
        // 材料槽：仅固态物
        addSlot(new OverlayHidingSlot(container, AkaishiCultivatorBlockEntity.SOLID_SLOT, 56, 52,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.akaishiLifeEssenceSolid.get());
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
        return data.get(AkaishiCultivatorBlockEntity.DATA_PROGRESS);
    }

    public int getMode() {
        return data.get(AkaishiCultivatorBlockEntity.DATA_MODE);
    }

    /** 速度升级组件数量（0~8） */
    public int getSpeedUpgradeCount() {
        return upgrades.getItem(MachineUpgradeSlots.SLOT_SPEED).getCount();
    }

    /** 能量升级组件数量（0~8） */
    public int getEnergyUpgradeCount() {
        return upgrades.getItem(MachineUpgradeSlots.SLOT_ENERGY).getCount();
    }

    /** 当前输入物品对应档位的成功率（提纯按纯度区间、升级按品质，无输入返回 0） */
    public int getSuccessRate() {
        ItemStack input = container.getItem(AkaishiCultivatorBlockEntity.INPUT_SLOT);
        if (input.getItem() instanceof AkaishiLifeSampleItem) {
            return AkaishiCultivatorBlockEntity.purifyRate(AkaishiLifeSampleItem.getPurity(input));
        }
        if (input.getItem() instanceof AkaishiOrganItem) {
            QualityTier tier = AkaishiOrganItem.getTier(input);
            if (tier != null && tier.next() != null) {
                return AkaishiCultivatorBlockEntity.upgradeRate(tier);
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
            if (index < MACHINE_SLOT_END) {
                // 机器区（升级槽 + 输入/材料槽）→ 玩家背包
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
                // 玩家背包：升级组件进升级槽，样本/器官/固态物按 mayPlace 自动进对应槽
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
