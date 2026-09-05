package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiTransgeneFactoryBlockEntity;
import com.example.akaishi.item.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 转基因工厂菜单。
 * 槽位：0=基因序列（凋零骷髅且纯度≥50）、1=缠怨藤、2=凋零玫瑰、3=生命能量固态物、4=产物（只读）。
 * 玩家背包从索引 5 开始（窗口高 198，背包 y=124/快捷栏 y=180）。
 */
public class AkaishiTransgeneFactoryMenu extends AbstractContainerMenu {

    /** 机器区槽数（5），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = 5;

    private final ContainerData data;

    /** 服务端构造：绑定真实方块实体容器 */
    public AkaishiTransgeneFactoryMenu(int id, Inventory inv, Container inventory, ContainerData data) {
        this(id, inv, data);
        addMachineSlots(inventory, inv);
    }

    /** 客户端回退构造：空容器（槽位数据由服务端同步覆盖） */
    public AkaishiTransgeneFactoryMenu(int id, Inventory inv) {
        this(id, inv, new SimpleContainerData(4));
        addMachineSlots(new SimpleContainer(AkaishiTransgeneFactoryBlockEntity.SLOT_COUNT), inv);
    }

    private AkaishiTransgeneFactoryMenu(int id, Inventory inv, ContainerData data) {
        super(ModMenus.CHISHI_TRANSGENE_FACTORY.get(), id);
        this.data = data;
    }

    private void addMachineSlots(Container inventory, Inventory playerInventory) {
        // 材料槽：0 基因（只收凋零骷髅基因）、1 缠怨藤、2 凋零玫瑰、3 固态物
        addSlot(new Slot(inventory, 0, 26, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AkaishiTransgeneFactoryBlockEntity.isWitherSkeletonGene(stack);
            }
        });
        addSlot(new Slot(inventory, 1, 44, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.TWISTING_VINES);
            }
        });
        addSlot(new Slot(inventory, 2, 62, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.WITHER_ROSE);
            }
        });
        addSlot(new Slot(inventory, 3, 80, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.akaishiLifeEssenceSolid.get());
            }
        });
        // 产物槽只读
        addSlot(new Slot(inventory, 4, 134, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        // 玩家背包 + 快捷栏
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 124 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 180));
        }
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            stack = current.copy();
            if (index < MACHINE_SLOT_END) {
                // 机器区 → 玩家背包
                if (!this.moveItemStackTo(current, MACHINE_SLOT_END, MACHINE_SLOT_END + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包/快捷栏 → 机器区（槽位 mayPlace 决定归属），多余再在背包内周转
                if (!this.moveItemStackTo(current, 0, MACHINE_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(current, MACHINE_SLOT_END + 27, MACHINE_SLOT_END + 36, false)
                        && !this.moveItemStackTo(current, MACHINE_SLOT_END, MACHINE_SLOT_END + 27, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (current.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return stack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public long getLifeEnergy() {
        return data.get(AkaishiTransgeneFactoryBlockEntity.DATA_ENERGY);
    }

    public long getLifeMax() {
        return data.get(AkaishiTransgeneFactoryBlockEntity.DATA_MAX);
    }

    public int getProgressPct() {
        return data.get(AkaishiTransgeneFactoryBlockEntity.DATA_PROGRESS);
    }

    public boolean isWorking() {
        return data.get(AkaishiTransgeneFactoryBlockEntity.DATA_WORKING) != 0;
    }
}
