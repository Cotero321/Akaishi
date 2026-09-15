package com.example.akaishi.menu;

import com.example.akaishi.util.LongDataSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 生命能量发射器菜单：无机器槽位，仅展示生命能量/容量与绑定坐标。
 * 能量与容量为 long，拆 4 个 int 数据槽同步（0/1 = 能量低/高位，2/3 = 容量低/高位）。
 * 绑定坐标在开界面时确定，界面存续期间不会变化，直接以字段携带，无需数据槽。
 */
public class AkaishiLifeEnergyEmitterMenu extends AbstractContainerMenu {

    private final ContainerData data;
    private final BlockPos target;

    public AkaishiLifeEnergyEmitterMenu(int id, Inventory playerInv, ContainerData data, BlockPos target) {
        super(ModMenus.CHISHI_LIFE_ENERGY_EMITTER.get(), id);
        this.data = data;
        this.target = target;

        // 玩家背包 3 行 × 9 + 快捷栏
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
        this.addDataSlots(data);
    }

    /** 当前生命能量（槽 0/1 为低/高 16 位段） */
    public long getLifeEnergy() {
        return LongDataSlots.read(data, 0, 1);
    }

    /** 容量上限（槽 2/3 为低/高 16 位段） */
    public long getLifeMax() {
        return LongDataSlots.read(data, 2, 3);
    }

    /** 绑定目标坐标，null 表示未绑定 */
    public BlockPos getTarget() {
        return target;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    /** 供无方块实体兜底时使用的空菜单（数据全 0、未绑定） */
    public static AkaishiLifeEnergyEmitterMenu emptyMenu(int id, Inventory inv) {
        return new AkaishiLifeEnergyEmitterMenu(id, inv,
                new net.minecraft.world.inventory.SimpleContainerData(4), null);
    }
}
