package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiFusionControllerBlockEntity;
import com.example.akaishi.fusion.FusionStructure;
import com.example.akaishi.item.AkaishiFusionHeatSinkItem;
import com.example.akaishi.item.AkaishiPlasmaRodItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 聚变控制器菜单：4 个燃料槽 + 10 个散热片槽 + 13 个数据槽 + 玩家背包。
 * <p>
 * 界面分三页（运行情况/燃料/热量），机器槽位随页签激活：
 * <ul>
 *   <li>运行情况页：成型/宕机/温度/产率/效率/灰烬等状态概览，无槽位</li>
 *   <li>燃料页：4 个燃料棒槽（仅本页激活，数量随结构燃料框架数）</li>
 *   <li>热量页：2×5 散热片槽位网格（存储于控制器散热片容器，热量页直接放入/取出散热片；
 *       散热片仅可放入结构散热框架解锁的前 N 槽，N = 结构内散热框架数量）</li>
 * </ul>
 * 服务端槽位直接绑定控制器 BE 的真实容器；客户端由构造器创建占位容器承接槽同步包。
 */
public class AkaishiFusionControllerMenu extends AbstractContainerMenu {

    /** 燃料槽总数（与结构燃料框架上限一致） */
    public static final int FUEL_SLOT_COUNT = AkaishiFusionControllerBlockEntity.MAX_FUEL_SLOTS;
    /** 散热片槽总数（与结构散热框架上限一致） */
    public static final int COOLER_SLOT_COUNT = FusionStructure.MAX_COOLER_FRAMES;
    /** 散热片槽位 2×5 网格布局（与 Screen 自绘框同源，勿单独改动坐标） */
    public static final int SINK_COLS = 5;
    public static final int SINK_X0 = (176 - SINK_COLS * 18) / 2;
    public static final int SINK_Y0 = 50;
    public static final int SINK_DY = 20;
    /** 燃料槽 2×2 布局 */
    private static final int FUEL_COLS = 2;
    private static final int FUEL_X0 = 70;
    private static final int FUEL_Y0 = 58;

    private final Container container;
    private final ContainerData data;

    /** 当前页签（0=运行情况 1=燃料 2=热量）。仅客户端视图状态。
     *  非 final：{@link OverlayHidingSlot} 的 lambda 需延迟引用。 */
    private int page = 0;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    /** 服务器入口：散热片槽绑定控制器散热片容器 */
    public AkaishiFusionControllerMenu(int id, Inventory inv, AkaishiFusionControllerBlockEntity be) {
        this(id, inv, be.fuelSlots(), be.coolerSlots(), be.data());
    }

    /** 客户端/异常兜底：散热片槽落在占位容器（内容由服务器槽同步包权威写入） */
    public AkaishiFusionControllerMenu(int id, Inventory inv, Container fuelContainer, ContainerData data) {
        this(id, inv, fuelContainer, new SimpleContainer(COOLER_SLOT_COUNT), data);
    }

    private AkaishiFusionControllerMenu(int id, Inventory inv, Container fuelContainer,
                                        Container coolerContainer, ContainerData data) {
        super(ModMenus.CHISHI_FUSION_CONTROLLER.get(), id);
        this.container = fuelContainer;
        this.data = data;

        // 燃料槽 2×2：仅燃料页(page==1)激活
        for (int i = 0; i < FUEL_SLOT_COUNT; i++) {
            int idx = i;
            addSlot(new Slot(fuelContainer, i, FUEL_X0 + (i % FUEL_COLS) * 18, FUEL_Y0 + (i / FUEL_COLS) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof AkaishiPlasmaRodItem;
                }

                @Override
                public boolean isActive() {
                    return page == 1 && idx < getFuelFrames() && super.isActive();
                }
            });
        }

        // 散热片槽 2×5：仅热量页(page==2)激活；散热片只能放入结构散热框架解锁的前 N 槽
        for (int i = 0; i < COOLER_SLOT_COUNT; i++) {
            final int idx = i;
            addSlot(new Slot(coolerContainer, i,
                    SINK_X0 + (i % SINK_COLS) * 18, SINK_Y0 + (i / SINK_COLS) * SINK_DY) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return idx < getCoolerCount() && !stack.isEmpty()
                            && stack.getItem() instanceof AkaishiFusionHeatSinkItem;
                }

                @Override
                public boolean isActive() {
                    return page == 2 && super.isActive();
                }
            });
        }

        // 玩家背包 3×9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 124 + row * 18));
            }
        }
        // 快捷栏 1×9
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 180));
        }

        addDataSlots(data);
    }

    // ===== 数据读取（供界面显示） =====

    public int getTemp() {
        return data.get(AkaishiFusionControllerBlockEntity.DATA_TEMP);
    }

    public boolean isFormed() {
        return data.get(AkaishiFusionControllerBlockEntity.DATA_FORMED) == 1;
    }

    /** 过热停产剩余秒（0 表示未在停产冷却中） */
    public int getOverheatCooldownSec() {
        return data.get(AkaishiFusionControllerBlockEntity.DATA_OVERHEAT_COOLDOWN);
    }

    public int getFuelFrames() {
        return data.get(AkaishiFusionControllerBlockEntity.DATA_FUEL_FRAMES);
    }

    public int getEfficiencyFrames() {
        return data.get(AkaishiFusionControllerBlockEntity.DATA_EFFICIENCY_FRAMES);
    }

    public int getCoolerCount() {
        return data.get(AkaishiFusionControllerBlockEntity.DATA_COOLER_COUNT);
    }

    public int getCoolingPercent() {
        return data.get(AkaishiFusionControllerBlockEntity.DATA_COOLING_PERCENT);
    }

    public int getActiveSlots() {
        return data.get(AkaishiFusionControllerBlockEntity.DATA_ACTIVE_SLOTS);
    }

    /** 当前产率（赤能源/tick，long 由 7/8 低位/高位重组） */
    public long getYieldPerTick() {
        return ((long) data.get(AkaishiFusionControllerBlockEntity.DATA_YIELD_HIGH) << 32)
                | (data.get(AkaishiFusionControllerBlockEntity.DATA_YIELD_LOW) & 0xFFFFFFFFL);
    }

    public boolean isOverheated() {
        return data.get(AkaishiFusionControllerBlockEntity.DATA_OVERHEATED) == 1;
    }

    /** 散热片槽最低剩余耐久百分比（0-100） */
    public int getCoolerDurability() {
        return data.get(AkaishiFusionControllerBlockEntity.DATA_COOLER_DURABILITY);
    }

    /** 当前效率系数（×100，如 535 = ×5.35） */
    public int getSpeedPercent() {
        return data.get(AkaishiFusionControllerBlockEntity.DATA_SPEED_X100);
    }

    /** 已积累生命灰烬数 */
    public int getAshAmount() {
        return data.get(AkaishiFusionControllerBlockEntity.DATA_ASH_AMOUNT);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            int fuelEnd = FUEL_SLOT_COUNT;
            int sinkEnd = fuelEnd + COOLER_SLOT_COUNT;
            int invStart = sinkEnd;
            if (index < fuelEnd) {
                // 燃料槽 → 玩家背包
                if (!this.moveItemStackTo(current, invStart, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < sinkEnd) {
                // 散热片槽 → 玩家背包
                if (!this.moveItemStackTo(current, invStart, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 燃料页：先尝试背包/快捷栏 → 燃料槽（槽位 mayPlace 过滤，非燃料棒自动跳过）
                if (page == 1) {
                    this.moveItemStackTo(current, 0, fuelEnd, false);
                }
                // 散热片 → 散热片槽（mayPlace 过滤，仅散热片可入且限已解锁槽，不依赖当前页）
                if (!current.isEmpty()) {
                    this.moveItemStackTo(current, fuelEnd, sinkEnd, false);
                }
                // 剩余物品继续执行背包行 ↔ 快捷栏整理
                if (!current.isEmpty()) {
                    if (index < invStart + 27) {
                        if (!this.moveItemStackTo(current, invStart + 27, invStart + 36, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(current, invStart, invStart + 27, false)) {
                        return ItemStack.EMPTY;
                    }
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
