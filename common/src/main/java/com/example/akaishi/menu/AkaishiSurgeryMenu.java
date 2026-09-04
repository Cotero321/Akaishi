package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiSurgeryBlockEntity;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.body.BodySlot;
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
 * 手术仓菜单：器官输入槽 + 固态物槽 + 背包。
 * 数据槽：0/1=生命能量/容量 2=手术进度% 3=操作类型 4=目标槽位索引 5=固态数量。
 * 玩家躯体 9 槽位状态经 PlayerBodySync（S2C）同步，由 Screen 读取渲染。
 */
public class AkaishiSurgeryMenu extends AbstractContainerMenu {

    /** 机器区槽数（升级槽 2 + 器官/固态槽 2），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = MachineUpgradeSlots.SLOT_COUNT
            + AkaishiSurgeryBlockEntity.SLOT_COUNT;

    private final Container container;
    private final ContainerData data;
    private final Container upgrades;
    /** 方块坐标：C2S 手术开始包与手术进度绑定检查使用 */
    @Nullable
    private final BlockPos blockPos;
    /** 存储联动状态（3 格内存在存储库时注入联动槽，Screen 弹浮层存取） */
    @Nullable
    public StorageLinkState linkState;

    public AkaishiSurgeryMenu(int id, Inventory inv, AkaishiSurgeryBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getUpgradeSlots(), be.getBlockPos());
    }

    /** 完整构造（包内供 ModMenus 网络工厂调用：附带方块坐标） */
    AkaishiSurgeryMenu(int id, Inventory inv, Container container, ContainerData data,
                      Container upgrades, @Nullable BlockPos pos) {
        super(ModMenus.CHISHI_SURGERY.get(), id);
        this.container = container;
        this.data = data;
        this.upgrades = upgrades;
        this.blockPos = pos;

        // 升级槽（速度/能量各一格，mayPlace 由 MachineUpgradeSlots 按类型互斥过滤；
        // 坐标落在存储浮层面板带内（x80..98 / y24..42），浮层打开时必须失活让位 → OverlayHidingSlot）
        addSlot(new MachineUpgradeHidingSlot(upgrades, MachineUpgradeSlots.SLOT_SPEED, 86, 30,
                () -> linkState != null && linkState.open));
        addSlot(new MachineUpgradeHidingSlot(upgrades, MachineUpgradeSlots.SLOT_ENERGY, 104, 30,
                () -> linkState != null && linkState.open));

        // 器官输入槽：仅接受器官物品（槽位匹配在服务端手术开始时校验）
        addSlot(new OverlayHidingSlot(container, AkaishiSurgeryBlockEntity.ORGAN_SLOT, 120, 28,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof AkaishiOrganItem;
            }
        });
        // 固态物槽：仅接受生命固态物
        addSlot(new OverlayHidingSlot(container, AkaishiSurgeryBlockEntity.SOLID_SLOT, 120, 54,
                () -> linkState != null && linkState.open) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.akaishiLifeEssenceSolid.get());
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
        return data.get(AkaishiSurgeryBlockEntity.DATA_ENERGY);
    }

    public long getLifeMax() {
        return data.get(AkaishiSurgeryBlockEntity.DATA_CAPACITY);
    }

    /** 手术进度（0-100） */
    public int getProgress() {
        return data.get(AkaishiSurgeryBlockEntity.DATA_PROGRESS);
    }

    /** 当前操作类型（0 无 / 1 移植 / 2 摘除） */
    public int getOperation() {
        return data.get(AkaishiSurgeryBlockEntity.DATA_OPERATION);
    }

    /** 当前目标槽位索引 */
    public int getTargetSlot() {
        return data.get(AkaishiSurgeryBlockEntity.DATA_TARGET);
    }

    /** 固态物数量 */
    public int getSolidCount() {
        return data.get(AkaishiSurgeryBlockEntity.DATA_SOLID);
    }

    /** 速度升级组件数量（0~8） */
    public int getSpeedUpgradeCount() {
        return upgrades.getItem(MachineUpgradeSlots.SLOT_SPEED).getCount();
    }

    /** 能量升级组件数量（0~8） */
    public int getEnergyUpgradeCount() {
        return upgrades.getItem(MachineUpgradeSlots.SLOT_ENERGY).getCount();
    }

    /** 器官输入槽物品 */
    public ItemStack getOrganInput() {
        return container.getItem(AkaishiSurgeryBlockEntity.ORGAN_SLOT);
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
                // 机器区（升级槽 + 器官/固态槽）→ 玩家背包
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
                // 玩家背包：升级组件进升级槽，器官/固态物按 mayPlace 自动进对应槽
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
