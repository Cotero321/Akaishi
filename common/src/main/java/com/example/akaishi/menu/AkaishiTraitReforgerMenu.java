package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiTraitReforgerBlockEntity;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.organ.AkaishiOrganItem;
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
 * 词条重铸仪菜单：器官输入 + 衰竭结晶 → 重铸器官输出 + 背包。
 * 数据槽：0/1=生命能量/容量 2=重铸进度% 3=词条总数 4=目标词条序号。
 * 目标词条选择经 C2S 包写入服务端（见 AkaishiTraitReforgerSync）。
 */
public class AkaishiTraitReforgerMenu extends AbstractContainerMenu {

    /** 机器区槽数（升级槽 2 + 器官/结晶/输出槽 3），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = MachineUpgradeSlots.SLOT_COUNT
            + AkaishiTraitReforgerBlockEntity.SLOT_COUNT;

    private final Container container;
    private final ContainerData data;
    private final Container upgrades;
    /** 方块坐标：C2S 目标选择包使用 */
    @Nullable
    private final BlockPos blockPos;
    /** 存储联动状态（3 格内存在存储库时注入联动槽） */
    @Nullable
    public StorageLinkState linkState;

    public AkaishiTraitReforgerMenu(int id, Inventory inv, AkaishiTraitReforgerBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getUpgradeSlots(), be.getBlockPos());
    }

    /** 完整构造（包内供 ModMenus 网络工厂调用：附带方块坐标以支持存储联动） */
    AkaishiTraitReforgerMenu(int id, Inventory inv, Container container, ContainerData data,
                             Container upgrades, @Nullable BlockPos pos) {
        super(ModMenus.CHISHI_TRAIT_REFORGER.get(), id);
        this.container = container;
        this.data = data;
        this.upgrades = upgrades;
        this.blockPos = pos;

        // 升级槽（速度/能量各一格，mayPlace 由 MachineUpgradeSlots 按类型互斥过滤）
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_SPEED, 134, 30));
        addSlot(new MachineUpgradeSlot(upgrades, MachineUpgradeSlots.SLOT_ENERGY, 152, 30));

        // 器官输入槽：仅接受携带 ≥1 条突变词条的非原生器官
        addSlot(new OverlayHidingSlot(container, AkaishiTraitReforgerBlockEntity.ORGAN_SLOT, 30, 30,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof AkaishiOrganItem
                        && !AkaishiOrganItem.isNative(stack)
                        && AkaishiOrganItem.getMutations(stack).size() > 0;
            }
        });
        // 衰竭结晶槽：仅接受衰竭结晶
        addSlot(new OverlayHidingSlot(container, AkaishiTraitReforgerBlockEntity.CRYSTAL_SLOT, 82, 30,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.exhaustedCrystal.get());
            }
        });
        // 产物槽：只出不进（重铸器官 NBT 不可合并）
        addSlot(new OverlayHidingSlot(container, AkaishiTraitReforgerBlockEntity.OUTPUT_SLOT, 116, 30,
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
        // 存储联动：3 格内存在存储库时注入联动槽
        this.linkState = StorageLink.tryLink(this::addSlot, inv.player.level(), pos);
    }

    public long getLifeEnergy() {
        return data.get(0);
    }

    public long getLifeMax() {
        return data.get(1);
    }

    /** 重铸进度（0-100） */
    public int getProgress() {
        return data.get(AkaishiTraitReforgerBlockEntity.DATA_PROGRESS);
    }

    /** 器官词条总数（服务端权威；0 表示无有效器官） */
    public int getTraitCount() {
        return data.get(AkaishiTraitReforgerBlockEntity.DATA_COUNT);
    }

    /** 当前目标词条序号（0-based） */
    public int getTargetIndex() {
        return data.get(AkaishiTraitReforgerBlockEntity.DATA_TARGET);
    }

    /** 目标词条对应稀有度档的结晶消耗（客户端提示用；无器官时按 1 档显示） */
    public int getCrystalCost() {
        ItemStack organ = container.getItem(AkaishiTraitReforgerBlockEntity.ORGAN_SLOT);
        if (organ.getItem() instanceof AkaishiOrganItem) {
            var mutations = AkaishiOrganItem.getMutations(organ);
            int idx = getTargetIndex();
            if (idx >= 0 && idx < mutations.size()) {
                return AkaishiTraitReforgerBlockEntity.crystalCost(mutations.get(idx).getRarity());
            }
        }
        return AkaishiTraitReforgerBlockEntity.crystalCost(1);
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
                // 机器区（升级槽 + 三个机器槽）→ 玩家背包
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
            } else if (linkState == null || !linkState.open) {
                // 玩家背包：升级组件进升级槽，器官/结晶按 mayPlace 自动进对应槽（输出槽只读跳过）
                // 浮层打开时机器槽失活隐藏，moveItemStackTo 不校验 isActive → 禁止 Shift 塞入不可见槽
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
