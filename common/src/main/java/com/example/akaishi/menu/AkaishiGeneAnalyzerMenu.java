package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiGeneAnalyzerBlockEntity;
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
 * 生命分析台菜单：输入槽（纯度 ≥25 样本）+ 输出槽（基因序列片段）+ 背包。
 * 数据槽：0/1=生命能量/容量 2=解构进度百分比。
 */
public class AkaishiGeneAnalyzerMenu extends AbstractContainerMenu {

    /** 机器区槽数（升级槽 2 + 输入/输出槽 2），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = MachineUpgradeSlots.SLOT_COUNT + AkaishiGeneAnalyzerBlockEntity.SLOT_COUNT;

    private final Container container;
    private final ContainerData data;
    private final Container upgrades;
    /** 方块坐标：供存储联动检测与后续 C2S 操作使用（无方块时为空） */
    @Nullable
    private final BlockPos blockPos;
    /** 存储联动状态（3 格内存在存储库时注入联动槽） */
    @Nullable
    public StorageLinkState linkState;

    public AkaishiGeneAnalyzerMenu(int id, Inventory inv, AkaishiGeneAnalyzerBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getUpgradeSlots(), be.getBlockPos());
    }

    public AkaishiGeneAnalyzerMenu(int id, Inventory inv, Container container, ContainerData data) {
        this(id, inv, container, data, new MachineUpgradeSlots(), null);
    }

    /** 完整构造（包内供 ModMenus 网络工厂调用：附带方块坐标以支持存储联动） */
    AkaishiGeneAnalyzerMenu(int id, Inventory inv, Container container, ContainerData data, Container upgrades, @Nullable BlockPos pos) {
        super(ModMenus.CHISHI_GENE_ANALYZER.get(), id);
        this.container = container;
        this.data = data;
        this.upgrades = upgrades;
        this.blockPos = pos;

        // 升级槽（速度/能量各一格，mayPlace 由 MachineUpgradeSlots 按类型互斥过滤；
        // 置于输出槽右侧同行，避开右上角存储按钮；浮层打开时失活让位）
        addSlot(new OverlayHidingSlot(upgrades, MachineUpgradeSlots.SLOT_SPEED, 134, 30,
                () -> linkState != null && linkState.open));
        addSlot(new OverlayHidingSlot(upgrades, MachineUpgradeSlots.SLOT_ENERGY, 152, 30,
                () -> linkState != null && linkState.open));

        // 输入槽：仅接受纯度 ≥25 的生命样本（未达解构门槛只能用于药剂）
        addSlot(new OverlayHidingSlot(container, AkaishiGeneAnalyzerBlockEntity.INPUT_SLOT, 56, 30,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof AkaishiLifeSampleItem
                        && AkaishiLifeSampleItem.getPurity(stack) >= AkaishiGeneAnalyzerBlockEntity.MIN_PURITY;
            }
        });
        // 输出槽：只出不进
        addSlot(new OverlayHidingSlot(container, AkaishiGeneAnalyzerBlockEntity.OUTPUT_SLOT, 116, 30,
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
        return data.get(AkaishiGeneAnalyzerBlockEntity.DATA_PROGRESS);
    }

    /** 当前输入样本对应纯度的解构成功率（无样本返回 0） */
    public int getSuccessRate() {
        ItemStack input = container.getItem(AkaishiGeneAnalyzerBlockEntity.INPUT_SLOT);
        if (input.getItem() instanceof AkaishiLifeSampleItem) {
            return Math.round(AkaishiGeneAnalyzerBlockEntity.successRate(AkaishiLifeSampleItem.getPurity(input)) * 100);
        }
        return 0;
    }

    /** 速度升级组件数量（0~8） */
    public int getSpeedUpgradeCount() {
        return upgrades.getItem(MachineUpgradeSlots.SLOT_SPEED).getCount();
    }

    /** 能量升级组件数量（0~8） */
    public int getEnergyUpgradeCount() {
        return upgrades.getItem(MachineUpgradeSlots.SLOT_ENERGY).getCount();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < MACHINE_SLOT_END) {
                // 机器区（升级槽 + 输入/输出槽）→ 背包
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
                // 玩家背包：升级组件按槽位类型自动过滤入升级槽，合格样本入输入槽（其余物品进不去不回收）
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
