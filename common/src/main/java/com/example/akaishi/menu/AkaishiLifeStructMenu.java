package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiLifeStructBlockEntity;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.organ.OrganEffectRegistry;
import com.example.akaishi.life.sequence.AkaishiGeneSequenceItem;
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

import java.util.List;

/**
 * 生命结构台菜单：输入槽（基因序列）+ 材料槽（固态物）+ 输出槽（器官）+ 背包。
 * 数据槽：0/1=生命能量/容量 2=构造进度% 3=目标槽位索引（BodySlot.values()）。
 */
public class AkaishiLifeStructMenu extends AbstractContainerMenu {

    /** 机器区槽数（升级槽 2 + 输入/材料/输出槽 3），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = MachineUpgradeSlots.SLOT_COUNT
            + AkaishiLifeStructBlockEntity.SLOT_COUNT;

    private final Container container;
    private final ContainerData data;
    private final Container upgrades;
    /** 方块坐标：客户端点击槽位按钮时通过 C2S 包选择目标（无方块时为空） */
    @Nullable
    private final BlockPos blockPos;
    /** 存储联动状态（3 格内存在存储库时注入联动槽） */
    @Nullable
    public StorageLinkState linkState;

    public AkaishiLifeStructMenu(int id, Inventory inv, AkaishiLifeStructBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getUpgradeSlots(), be.getBlockPos());
    }

    public AkaishiLifeStructMenu(int id, Inventory inv, Container container, ContainerData data) {
        this(id, inv, container, data, new MachineUpgradeSlots(), null);
    }

    /** 完整构造（包内供 ModMenus 网络工厂调用：附带方块坐标以支持界面选槽位） */
    AkaishiLifeStructMenu(int id, Inventory inv, Container container, ContainerData data,
                         Container upgrades, @Nullable BlockPos pos) {
        super(ModMenus.CHISHI_LIFE_STRUCT.get(), id);
        this.container = container;
        this.data = data;
        this.upgrades = upgrades;
        this.blockPos = pos;

        // 升级槽（速度/能量各一格，mayPlace 由 MachineUpgradeSlots 按类型互斥过滤；
        // 与浮层第一行末尾两格坐标重叠，须随浮层开关失活让位）
        addSlot(new MachineUpgradeHidingSlot(upgrades, MachineUpgradeSlots.SLOT_SPEED, 134, 56,
                () -> linkState != null && linkState.open));
        addSlot(new MachineUpgradeHidingSlot(upgrades, MachineUpgradeSlots.SLOT_ENERGY, 152, 56,
                () -> linkState != null && linkState.open));

        // 输入槽：仅接受基因序列
        addSlot(new OverlayHidingSlot(container, AkaishiLifeStructBlockEntity.INPUT_SLOT, 30, 30,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof AkaishiGeneSequenceItem;
            }
        });
        // 材料槽：仅接受生命固态物
        addSlot(new OverlayHidingSlot(container, AkaishiLifeStructBlockEntity.SOLID_SLOT, 56, 30,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.akaishiLifeEssenceSolid.get());
            }
        });
        // 输出槽：只出不进
        addSlot(new OverlayHidingSlot(container, AkaishiLifeStructBlockEntity.OUTPUT_SLOT, 116, 30,
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
        return data.get(AkaishiLifeStructBlockEntity.DATA_PROGRESS);
    }

    /** 当前目标槽位索引 */
    public int getTargetSlot() {
        return data.get(AkaishiLifeStructBlockEntity.DATA_TARGET);
    }

    /** 速度升级组件数量（0~8） */
    public int getSpeedUpgradeCount() {
        return upgrades.getItem(MachineUpgradeSlots.SLOT_SPEED).getCount();
    }

    /** 能量升级组件数量（0~8） */
    public int getEnergyUpgradeCount() {
        return upgrades.getItem(MachineUpgradeSlots.SLOT_ENERGY).getCount();
    }

    /** 当前序列生物的可用器官槽位（Screen 高亮用） */
    public List<BodySlot> getAvailableSlots() {
        ItemStack input = container.getItem(AkaishiLifeStructBlockEntity.INPUT_SLOT);
        if (!(input.getItem() instanceof AkaishiGeneSequenceItem)) {
            return List.of();
        }
        return OrganEffectRegistry.availableSlots(AkaishiGeneSequenceItem.getEntityId(input));
    }

    /** 当前序列生物显示名（Screen 标题下提示用），无序列返回 null */
    @Nullable
    public String getSequenceEntityId() {
        ItemStack input = container.getItem(AkaishiLifeStructBlockEntity.INPUT_SLOT);
        return input.getItem() instanceof AkaishiGeneSequenceItem
                ? AkaishiGeneSequenceItem.getEntityId(input) : null;
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
                // 机器区（升级槽 + 输入/材料/输出槽）→ 玩家背包
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
                // 玩家背包：升级组件进升级槽，序列/固态物按 mayPlace 自动进对应槽（输出槽只读跳过）
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
