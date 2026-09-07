package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiEquipmentForgerBlockEntity;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.item.AkaishiUpgradeHelper;
import com.example.akaishi.item.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 赤石装备打造器菜单：输入（下界合金装备 + 赤石锭）+ 输出（赤石装备）+ 能量/进度/升级点数据。
 * 按钮协议：0-4 为属性分配升级点，50-54 撤销对应属性升级点，BUTTON_FORGE 触发锻造。
 */
public class AkaishiEquipmentForgerMenu extends AbstractContainerMenu {

    /** 属性升级按钮基础 id：id 0-N 对应 UpgradeType 顺序 */
    public static final int BTN_ADD_BASE = 0;
    /** 撤销按钮偏移：id 50-(50+N) */
    public static final int BTN_REMOVE_OFFSET = 50;
    /** 锻造按钮 id */
    public static final int BUTTON_FORGE = 100;

    /** 升级类型数量 */
    private static final int UPGRADE_COUNT = AkaishiUpgradeHelper.UpgradeType.values().length;

    private final Container container;
    private final ContainerData data;
    /** 服务端方块实体引用（客户端构造时为 null，点击按钮仅在服务端执行） */
    private final AkaishiEquipmentForgerBlockEntity be;

    public AkaishiEquipmentForgerMenu(int id, Inventory inv, AkaishiEquipmentForgerBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be);
    }

    public AkaishiEquipmentForgerMenu(int id, Inventory inv, Container container, ContainerData data) {
        this(id, inv, container, data, null);
    }

    private AkaishiEquipmentForgerMenu(int id, Inventory inv, Container container, ContainerData data,
                                      AkaishiEquipmentForgerBlockEntity be) {
        super(ModMenus.CHISHI_EQUIPMENT_FORGER.get(), id);
        this.container = container;
        this.data = data;
        this.be = be;

        // 方块槽：装备 / 赤石锭输入左侧，输出右侧
        addSlot(new Slot(container, AkaishiEquipmentForgerBlockEntity.INPUT_GEAR_SLOT, 38, 30));
        addSlot(new Slot(container, AkaishiEquipmentForgerBlockEntity.INPUT_INGOT_SLOT, 62, 30));
        addSlot(new Slot(container, AkaishiEquipmentForgerBlockEntity.OUTPUT_SLOT, 116, 30));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    public int getEnergy() {
        return data.get(0);
    }

    public int getMaxEnergy() {
        return data.get(1);
    }

    /** 锻造充能进度（能量百分比 0-100） */
    public int getProgress() {
        return data.get(2);
    }

    /** 剩余基础升级点 */
    public int getUpgradePoints() {
        return data.get(3);
    }

    /** 指定属性已选升级次数 */
    public int getBaseCount(int typeId) {
        return data.get(4 + typeId);
    }

    /** 输入槽装备（供 Screen 决定是否显示效率按钮等条件选项） */
    public ItemStack getGearItem() {
        return getSlot(AkaishiEquipmentForgerBlockEntity.INPUT_GEAR_SLOT).getItem();
    }

    /** 锻造就绪：配方装备 + 锭足够 + 输出空 + 能量满足（客户端/服务端通用判断，供按钮变色） */
    public boolean isForgeReady() {
        ItemStack gear = getSlot(AkaishiEquipmentForgerBlockEntity.INPUT_GEAR_SLOT).getItem();
        int ingotCost = AkaishiEquipmentForgerBlockEntity.ingotCostFor(gear);
        if (ingotCost <= 0) {
            return false;
        }
        ItemStack ingot = getSlot(AkaishiEquipmentForgerBlockEntity.INPUT_INGOT_SLOT).getItem();
        if (!ingot.is(ModItems.akaishiIngot.get()) || ingot.getCount() < ingotCost) {
            return false;
        }
        if (!getSlot(AkaishiEquipmentForgerBlockEntity.OUTPUT_SLOT).getItem().isEmpty()) {
            return false;
        }
        long cost = ModConfig.equipmentForgerEnergyPerForge
                + totalPoints() * AkaishiUpgradeHelper.ENERGY_PER_BASE_UPGRADE;
        return data.get(0) >= cost;
    }

    /** 当前已选升级点数（data 4-8 求和） */
    private long totalPoints() {
        long points = 0;
        for (int i = 0; i < AkaishiUpgradeHelper.UpgradeType.values().length; i++) {
            points += data.get(4 + i);
        }
        return points;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null) {
            return false;
        }
        if (id == BUTTON_FORGE) {
            // 玩家点击锻造按钮 → 手动触发重铸（服务端校验条件）
            be.tryForge();
            return true;
        }
        if (id >= BTN_ADD_BASE && id < BTN_ADD_BASE + UPGRADE_COUNT) {
            return be.addBaseUpgrade(id - BTN_ADD_BASE);
        }
        if (id >= BTN_REMOVE_OFFSET && id < BTN_REMOVE_OFFSET + UPGRADE_COUNT) {
            return be.removeBaseUpgrade(id - BTN_REMOVE_OFFSET);
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < AkaishiEquipmentForgerBlockEntity.SLOT_COUNT) {
                if (!this.moveItemStackTo(current, AkaishiEquipmentForgerBlockEntity.SLOT_COUNT,
                        AkaishiEquipmentForgerBlockEntity.SLOT_COUNT + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(current, 0, AkaishiEquipmentForgerBlockEntity.SLOT_COUNT, false)) {
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
