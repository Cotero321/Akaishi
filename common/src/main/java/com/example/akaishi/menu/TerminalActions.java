package com.example.akaishi.menu;

import java.util.List;

import com.example.akaishi.api.storage.IItemStorageUnit;
import com.example.akaishi.block.entity.AkaishiItemTerminalBlockEntity;
import com.example.akaishi.value.ItemPoints;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 物品终端库页的落账层：AE2「网格双向」交互的服务端实现，也是库内容唯一的外部写入路径。
 * <p>
 * 交互语义对齐 AE2 {@code MEStorageScreen#handleGridInventoryEntryMouseClick} + {@code InventoryAction}：
 * <ul>
 *   <li>空白格 + 手持物 → 放入（左键整手 / 右键单件），对应 AE2 的 serial = -1；</li>
 *   <li>条目 + 空手 → 取出（左键一整组 / 右键单件 / Shift+左键整条进背包）；</li>
 *   <li>条目 + 同名手持物 → 放入；手持异类物品一律忽略（不猜意图，避免误存）；</li>
 * </ul>
 * 写入一律经 {@link IItemStorageUnit} 的三写入口，绝不触碰容器本体，故不存在绕过账本的可能（§2.5 防线）。
 * <p>
 * 本类<b>只在服务端调用</b>：客户端仅发动作与「选中物」，数量与费用全部由服务端重新推导。
 * <p>
 * <b>失败必须可见</b>：本界面是纯点击驱动，静默 no-op 会让玩家完全无法判断问题在哪，
 * 因此每条拒绝路径都会经动作栏回一条具体原因（见 {@link Outcome}）。
 */
public final class TerminalActions {

    // ===== 动作集（AE2 InventoryAction 的裁剪子集；无合成 / 无创造复制场景） =====

    /** 左键：取出一整组 / 放下整手 */
    public static final byte PICKUP_OR_SET_DOWN = 0;
    /** 右键：取出单件 / 放下单件 */
    public static final byte SPLIT_OR_PLACE_SINGLE = 1;
    /** Shift + 左键：整条取出进背包 / 存入整手 */
    public static final byte SHIFT_CLICK = 2;
    /** Shift + 右键：取出单件（与右键同效，保留 AE2 动作位） */
    public static final byte PICKUP_SINGLE = 3;

    /** 单笔结果原因（仅用于给玩家反馈；OK 之外的都配一条动作栏文本） */
    private enum Outcome {
        OK(null),
        /** 多方块未成型 */
        UNFORMED("gui.akaishi.item_terminal.fail.unformed"),
        /** 结构外侧 1 格没有贴装任何储存单元 */
        NO_UNIT("gui.akaishi.item_terminal.fail.no_unit"),
        /** 赤能源不足（缓冲为空或不够付这一笔） */
        NO_ENERGY("gui.akaishi.item_terminal.fail.no_energy"),
        /** 储存单元已满（IP 容量或空位不足） */
        NO_SPACE("gui.akaishi.item_terminal.fail.no_space"),
        /** 库里没有这件物品 */
        NO_ITEM("gui.akaishi.item_terminal.fail.no_item"),
        /** 背包放不下 */
        NO_INV_ROOM("gui.akaishi.item_terminal.fail.no_room");

        private final String langKey;

        Outcome(String langKey) {
            this.langKey = langKey;
        }
    }

    /** 单笔结果：成功时带光标结果堆（null = 不改写光标），失败时带原因 */
    private record Result(ItemStack carried, Outcome outcome) {
        static Result ok(ItemStack carried) {
            return new Result(carried, Outcome.OK);
        }

        static Result okNoCursor() {
            return new Result(null, Outcome.OK);
        }

        static Result fail(Outcome outcome) {
            return new Result(null, outcome);
        }
    }

    private TerminalActions() {
    }

    /**
     * 执行一次库页交互。
     *
     * @param terminal 终端方块实体（提供单元视图 + 一次性费用结算）
     * @param menu     玩家的当前菜单（光标持有物的读 / 写入口）
     * @param player   操作玩家
     * @param action   {@link #PICKUP_OR_SET_DOWN} 等动作字节
     * @param key      点击的条目（空堆 = 点击空白格，仅允许放入）
     */
    public static void perform(AkaishiItemTerminalBlockEntity terminal, AbstractContainerMenu menu,
            Player player, byte action, ItemStack key) {
        if (terminal == null || !terminal.isFormed()) {
            notify(player, Outcome.UNFORMED);
            return;
        }
        List<IItemStorageUnit> units = terminal.storageUnits();
        if (units.isEmpty()) {
            notify(player, Outcome.NO_UNIT);
            return;
        }
        ItemStack carried = menu.getCarried();
        Result result;
        if (key.isEmpty()) {
            result = deposit(terminal, units, carried, single(action));
        } else if (carried.isEmpty()) {
            result = action == SHIFT_CLICK
                    ? withdrawToInventory(terminal, units, player, key)
                    : withdrawToCursor(terminal, units, key, single(action) ? 1 : key.getMaxStackSize());
        } else if (ItemStack.isSameItemSameTags(carried, key)) {
            result = deposit(terminal, units, carried, single(action));
        } else {
            // 手持异类物品：不猜意图，静默忽略（不算失败，不打扰玩家）
            result = Result.okNoCursor();
        }
        apply(menu, result.carried());
        notify(player, result.outcome());
    }

    /** 单件动作（右键族）：只处理 1 件 */
    private static boolean single(byte action) {
        return action == SPLIT_OR_PLACE_SINGLE || action == PICKUP_SINGLE;
    }

    /** 光标结果回写：null 表示本笔未发生，不得回写（避免凭空生成） */
    private static void apply(AbstractContainerMenu menu, ItemStack newCarried) {
        if (newCarried == null) {
            return;
        }
        menu.setCarried(newCarried);
        menu.broadcastChanges();
    }

    /** 失败原因经动作栏回给玩家：点击界面必须"有回应"，否则玩家无从判断 */
    private static void notify(Player player, Outcome outcome) {
        if (outcome.langKey != null) {
            player.displayClientMessage(Component.translatable(outcome.langKey), true);
        }
    }

    // ===== 存入 =====

    /**
     * 存入：先按「各单元容量 / 空位 + 单笔能量上限」夹出可存件数，再扣费，最后分发入库。
     * <p>
     * 顺序固定为「预检 → 扣费 → 入库」，因此扣费失败或无可存空间时，物品与账本零改动（D13 约束 3）。
     */
    private static Result deposit(AkaishiItemTerminalBlockEntity terminal, List<IItemStorageUnit> units,
            ItemStack carried, boolean single) {
        if (carried.isEmpty()) {
            return Result.okNoCursor(); // 空手点空白格：无事发生
        }
        // acceptable 与传入堆量无关（只看物品 + NBT），故用单件探测即可
        ItemStack probe = carried.copyWithCount(1);
        long per = ItemPoints.perItem(probe);
        long space = 0L;
        for (IItemStorageUnit unit : units) {
            space += unit.acceptable(probe);
        }
        if (space <= 0L) {
            return Result.fail(Outcome.NO_SPACE);
        }
        // 单笔上限：缓冲能承担的费用对应的 IP（缓冲为 0 或减免后仍不够一件 ⇒ 属于能量不足）
        long byFee = terminal.maxBatchIp(true) / per;
        if (byFee <= 0L) {
            return Result.fail(Outcome.NO_ENERGY);
        }
        int amount = (int) Math.min(single ? 1L : carried.getCount(),
                Math.min(Math.min(space, byFee), Integer.MAX_VALUE));
        if (amount <= 0) {
            return Result.fail(Outcome.NO_SPACE);
        }
        if (!terminal.tryChargeFee(per * amount, true)) {
            return Result.fail(Outcome.NO_ENERGY);
        }
        int moved = 0;
        for (IItemStorageUnit unit : units) {
            if (moved >= amount) {
                break;
            }
            int accept = unit.acceptable(probe);
            if (accept <= 0) {
                continue;
            }
            // insert 为整堆语义（超出 acceptable 即整笔拒绝），故按单元上限切块投喂
            moved += unit.insert(probe.copyWithCount(Math.min(accept, amount - moved)));
        }
        if (moved <= 0) {
            return Result.fail(Outcome.NO_SPACE);
        }
        int left = carried.getCount() - moved;
        return Result.ok(left <= 0 ? ItemStack.EMPTY : carried.copyWithCount(left));
    }

    // ===== 取出 =====

    /**
     * 取出到光标：先按分片模拟本次要消费的<b>账本 IP</b> 并扣费，成功后再按同一顺序真实取出。
     * <p>
     * 账本 IP 用 {@code slotIp * take / count} 复刻单元 {@code extract} 的扣减公式，
     * 全程不重算价值表 ⇒ 价值表热重载后费用不漂移（D10）。
     */
    private static Result withdrawToCursor(AkaishiItemTerminalBlockEntity terminal,
            List<IItemStorageUnit> units, ItemStack key, int requested) {
        TerminalEntry entry = find(units, key);
        if (entry == null) {
            return Result.fail(Outcome.NO_ITEM);
        }
        int amount = (int) Math.min(entry.amount(), Math.max(0, requested));
        if (amount <= 0) {
            return Result.fail(Outcome.NO_ITEM);
        }
        if (!chargeFor(terminal, entry, amount)) {
            return Result.fail(Outcome.NO_ENERGY);
        }
        ItemStack out = take(entry, amount);
        return out == null ? Result.fail(Outcome.NO_ITEM) : Result.ok(out);
    }

    /** 整条取出进背包（AE2 SHIFT_CLICK）：按背包剩余空位夹量，不经光标 */
    private static Result withdrawToInventory(AkaishiItemTerminalBlockEntity terminal,
            List<IItemStorageUnit> units, Player player, ItemStack key) {
        TerminalEntry entry = find(units, key);
        if (entry == null) {
            return Result.fail(Outcome.NO_ITEM);
        }
        int room = freeSpace(player.getInventory(), entry.display());
        if (room <= 0) {
            return Result.fail(Outcome.NO_INV_ROOM);
        }
        int amount = (int) Math.min(entry.amount(), room);
        if (amount <= 0 || !chargeFor(terminal, entry, amount)) {
            return Result.fail(amount <= 0 ? Outcome.NO_ITEM : Outcome.NO_ENERGY);
        }
        ItemStack out = take(entry, amount);
        if (out == null) {
            return Result.fail(Outcome.NO_ITEM);
        }
        // 背包放不下就落地，避免物品凭空消失（夹量已保证正常情况装得下）
        if (!player.getInventory().add(out)) {
            player.drop(out, false);
        }
        return Result.okNoCursor();
    }

    /** 模拟取出 amount 件的账本 IP 并扣费（不改动任何物品） */
    private static boolean chargeFor(AkaishiItemTerminalBlockEntity terminal, TerminalEntry entry, int amount) {
        long ip = 0L;
        int need = amount;
        for (TerminalEntry.Slice slice : entry.slices()) {
            if (need <= 0) {
                break;
            }
            int take = Math.min(need, slice.count());
            long slotIp = slice.unit().getSlotIp(slice.slot());
            ip += take >= slice.count() ? slotIp : slotIp * take / slice.count();
            need -= take;
        }
        return ip <= 0L || terminal.tryChargeFee(ip, false);
    }

    /** 按分片顺序真实取出；分片取自本笔开头即时聚合的结果，单线程下不会中途变化 */
    private static ItemStack take(TerminalEntry entry, int amount) {
        ItemStack out = entry.display().copy();
        out.setCount(0);
        int need = amount;
        for (TerminalEntry.Slice slice : entry.slices()) {
            if (need <= 0) {
                break;
            }
            ItemStack taken = slice.unit().extract(slice.slot(), Math.min(need, slice.count()));
            if (taken.isEmpty()) {
                continue;
            }
            out.grow(taken.getCount());
            need -= taken.getCount();
        }
        return out.isEmpty() ? null : out;
    }

    /** 在即时聚合结果中定位点击的条目（客户端仅作选择器，数量一律以服务端为准） */
    private static TerminalEntry find(List<IItemStorageUnit> units, ItemStack key) {
        for (TerminalEntry entry : TerminalInventory.snapshot(units)) {
            if (ItemStack.isSameItemSameTags(entry.display(), key)) {
                return entry;
            }
        }
        return null;
    }

    /** 背包还能装下多少件同物品（含可并入的未满堆），用于跨背包取出的夹量 */
    private static int freeSpace(Inventory inventory, ItemStack prototype) {
        int max = prototype.getMaxStackSize();
        int space = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot.isEmpty()) {
                space += max;
            } else if (ItemStack.isSameItemSameTags(slot, prototype)) {
                space += Math.max(0, max - slot.getCount());
            }
        }
        return space;
    }
}
