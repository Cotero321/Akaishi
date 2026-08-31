package com.example.template.menu;

import com.example.template.block.entity.ChishiReactorControllerBlockEntity;
import com.example.template.block.entity.ChishiReactorCoolerBlockEntity;
import com.example.template.reactor.ReactorStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 反应堆控制器菜单：10 个燃料槽（每根燃料棒解锁 1 槽）+ 20 个散热片槽（温度页审视/更换）
 * + 玩家背包。
 * 数据槽（14 个）：温度/成型/棒数/有效散热/散热%/废品量/废品容量/高温警告/活跃槽/产率/耗速/废品满/散热片耐久/散热组件数。
 * 界面通过 {@link ChishiReactorControllerScreen} 分"燃料/温度/状态"三页展示，机器槽位随页签激活。
 */
public class ChishiReactorControllerMenu extends AbstractContainerMenu {

    /** 燃料槽总数（5×2，界面与容器共用） */
    public static final int FUEL_SLOT_COUNT = 10;
    /** 散热片槽起始索引（紧跟燃料槽） */
    public static final int COOLER_SLOT_START = FUEL_SLOT_COUNT;
    /** 散热片槽总数（5×4，对应结构散热组件上限） */
    public static final int COOLER_SLOT_COUNT = 20;
    private static final int COOLER_COLS = 5;
    private static final int COOLER_X0 = 8, COOLER_Y0 = 42;

    private final Container container;
    private final ContainerData data;

    /** 当前页签（0=燃料 1=温度 2=状态）。仅客户端视图状态，不参与服务端逻辑。
     *  非 final：{@link OverlayHidingSlot} 的 lambda 需延迟引用，final 字段在注入时尚未赋值。 */
    private int page = 0;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public ChishiReactorControllerMenu(int id, Inventory inv, ChishiReactorControllerBlockEntity be) {
        this(id, inv, be.fuelSlots(), be.data(), resolveCoolers(be));
    }

    /** 客户端/异常兜底：无结构信息，散热片槽位使用空容器占位 */
    public ChishiReactorControllerMenu(int id, Inventory inv, Container container, ContainerData data) {
        this(id, inv, container, data, null);
    }

    private ChishiReactorControllerMenu(int id, Inventory inv, Container container, ContainerData data,
                                        List<ChishiReactorCoolerBlockEntity> coolers) {
        super(ModMenus.CHISHI_REACTOR_CONTROLLER.get(), id);
        this.container = container;
        this.data = data;

        // 燃料槽 5×2：x=44 起，对齐标准 18px 网格（8,26,44,62..，与物品栏竖线对齐），
        // 仅燃料页(page==0)激活，且仅显示结构内实际燃料棒数量（rodCount）对应的槽位
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 5; col++) {
                int idx = row * 5 + col;
                addSlot(new FuelCellSlot(container, idx,
                        44 + col * 18, 58 + row * 18,
                        () -> this.page != 0 || idx >= this.data.get(ChishiReactorControllerBlockEntity.DATA_ROD_COUNT)));
            }
        }

        // 散热片槽 5×4（x=8..98, y=42..114），绑定结构内散热组件容器，仅温度页(page==1)激活
        for (int i = 0; i < COOLER_SLOT_COUNT; i++) {
            Container c = (coolers != null && i < coolers.size())
                    ? coolers.get(i) : new SimpleContainer(1);
            addSlot(new HeatSinkSlot(c, ChishiReactorCoolerBlockEntity.SINK_SLOT,
                    COOLER_X0 + (i % COOLER_COLS) * 18, COOLER_Y0 + (i / COOLER_COLS) * 18,
                    () -> this.page != 1));
        }

        // 玩家背包 3×9：18px 格子与燃料槽一致（x=8..170, y=124..178）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 124 + row * 18));
            }
        }
        // 快捷栏 1×9（y=180）
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 180));
        }

        addDataSlots(data);
    }

    /** 从结构扫描结果解析散热组件方块实体列表（未成型返回空表） */
    private static List<ChishiReactorCoolerBlockEntity> resolveCoolers(ChishiReactorControllerBlockEntity be) {
        List<ChishiReactorCoolerBlockEntity> out = new ArrayList<>();
        ReactorStructure.Result s = be.getStructure();
        if (s == null || be.getLevel() == null) {
            return out;
        }
        for (BlockPos p : s.coolers) {
            if (be.getLevel().getBlockEntity(p) instanceof ChishiReactorCoolerBlockEntity c) {
                out.add(c);
            }
        }
        return out;
    }

    // ===== 数据读取（供界面显示） =====

    public int getTemp() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_TEMP);
    }

    public boolean isFormed() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_FORMED) == 1;
    }

    public int getRodCount() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_ROD_COUNT);
    }

    public int getEffectiveCoolers() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_EFFECTIVE_COOLERS);
    }

    public int getCoolingPercent() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_COOLING_PERCENT);
    }

    public int getCoolerDurability() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_COOLER_DURABILITY);
    }

    /** 结构内散热组件总数（客户端绘制散热片槽位用） */
    public int getCoolerCount() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_COOLER_COUNT);
    }

    public long getWasteAmount() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_WASTE_AMOUNT) & 0xFFFFFFFFL;
    }

    public long getWasteMax() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_WASTE_CAPACITY) & 0xFFFFFFFFL;
    }

    public boolean isWarning() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_WARNING) == 1;
    }

    public int getActiveSlots() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_ACTIVE_SLOTS);
    }

    public long getEnergyPerTick() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_ENERGY_PER_TICK) & 0xFFFFFFFFL;
    }

    public double getFuelDrainPerTick() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_FUEL_DRAIN) / 1000.0;
    }

    public boolean isWasteFull() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_WASTE_FULL) == 1;
    }

    /** 废品罐内衰竭燃料种类数（tooltip 展示） */
    public int getWasteTypes() {
        return data.get(ChishiReactorControllerBlockEntity.DATA_WASTE_TYPES);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            int invStart = COOLER_SLOT_START + COOLER_SLOT_COUNT; // 30：玩家背包起点
            if (index < COOLER_SLOT_START) {
                // 燃料槽 → 玩家背包
                if (!this.moveItemStackTo(current, invStart, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < invStart) {
                // 散热片槽 → 玩家背包
                if (!this.moveItemStackTo(current, invStart, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.page == 0
                    && !this.moveItemStackTo(current, 0, COOLER_SLOT_START, false)
                    && !this.moveItemStackTo(current, COOLER_SLOT_START, invStart, false)) {
                // 燃料页：背包 → 燃料槽（后尝试散热片槽，靠槽位 mayPlace 过滤）
                return ItemStack.EMPTY;
            } else if (this.page == 1
                    && !this.moveItemStackTo(current, COOLER_SLOT_START, invStart, false)) {
                // 温度页：背包 → 散热片槽（仅散热片可入）
                return ItemStack.EMPTY;
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
