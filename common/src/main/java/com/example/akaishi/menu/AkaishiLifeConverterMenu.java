package com.example.akaishi.menu;

import com.example.akaishi.util.LongDataSlots;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 生命转换菜单（生命聚合转换器 / 生命转换架构共用）：
 * 无机器槽位，展示赤能源（输入缓冲）与生命能量（输出缓冲）+ 结构状态。
 * 数据槽：0/1=赤能源低/高，2/3=赤容量低/高，4/5=生命能量低/高，6/7=生命容量低/高，
 * 8=结构状态（1=成型），9=独立转换标记（仅聚合器，可选）。
 * 能量/容量为 long，各占低/高 32 位两槽（SimpleContainerData 仅支持 int，直接强转会溢出）。
 */
public class AkaishiLifeConverterMenu extends AbstractContainerMenu {

    /** 数据槽布局（与两个方块实体写侧保持一致） */
    private static final int SLOT_CHISHI = 0;
    private static final int SLOT_CHISHI_HIGH = 1;
    private static final int SLOT_CHISHI_CAP = 2;
    private static final int SLOT_CHISHI_CAP_HIGH = 3;
    private static final int SLOT_LIFE = 4;
    private static final int SLOT_LIFE_HIGH = 5;
    private static final int SLOT_LIFE_CAP = 6;
    private static final int SLOT_LIFE_CAP_HIGH = 7;
    private static final int SLOT_FORMED = 8;
    private static final int SLOT_STANDALONE = 9;
    /** 聚合器数据槽总数（含独立标记），用于判定是否独立转换 */
    private static final int SLOT_COUNT_STANDALONE = 10;

    private static final Container EMPTY = new SimpleContainer(0);

    private final ContainerData data;

    public AkaishiLifeConverterMenu(int id, Inventory playerInv, ContainerData data) {
        super(ModMenus.CHISHI_LIFE_CONVERTER.get(), id);
        this.data = data;

        // 玩家背包 3 行 × 9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // 快捷栏
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
        this.addDataSlots(data);
    }

    /** 当前赤能源储量 */
    public long getAkaishiEnergy() {
        return LongDataSlots.read(data, SLOT_CHISHI, SLOT_CHISHI_HIGH);
    }

    /** 赤能源容量上限 */
    public long getAkaishiMax() {
        return LongDataSlots.read(data, SLOT_CHISHI_CAP, SLOT_CHISHI_CAP_HIGH);
    }

    /** 当前生命能量储量 */
    public long getLifeEnergy() {
        return LongDataSlots.read(data, SLOT_LIFE, SLOT_LIFE_HIGH);
    }

    /** 生命能量容量上限 */
    public long getLifeMax() {
        return LongDataSlots.read(data, SLOT_LIFE_CAP, SLOT_LIFE_CAP_HIGH);
    }

    /** 结构是否成型（生命转换架构） */
    public boolean isFormed() {
        return data.get(SLOT_FORMED) == 1;
    }

    /** 是否单台独立转换（生命聚合转换器；矩阵则走成型逻辑） */
    public boolean isStandalone() {
        return data.getCount() >= SLOT_COUNT_STANDALONE && data.get(SLOT_STANDALONE) == 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    /** 供无方块实体兜底时使用的空菜单（数据全 0，按矩阵槽位布局） */
    public static AkaishiLifeConverterMenu emptyMenu(int id, Inventory inv) {
        return new AkaishiLifeConverterMenu(id, inv, new SimpleContainerData(SLOT_FORMED + 1));
    }
}
