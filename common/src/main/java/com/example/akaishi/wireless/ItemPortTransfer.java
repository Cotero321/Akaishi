package com.example.akaishi.wireless;

import com.example.akaishi.api.storage.IItemStorageUnit;
import com.example.akaishi.api.storage.IItemTerminalHost;
import com.example.akaishi.api.transfer.ItemAccessHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * 储存无线输入/输出口的物品搬运引擎（照 AE2 输入总线 / 输出总线的<b>方向语义</b>）。
 * <ul>
 *   <li><b>输入口</b> = AE2 {@code Import Bus}：对面容器的物品 → <b>抓进</b>绑定的物品终端储存；</li>
 *   <li><b>输出口</b> = AE2 {@code Export Bus}：绑定的物品终端储存 → <b>推给</b>对面容器。</li>
 * </ul>
 * 只对面朝方向的容器操作（同 AE2 总线）；对面容器经 {@link ItemAccessHolder} 取（加载器未注册实现时
 * 退回原版 Container）。单元写入一律走 {@link IItemStorageUnit#acceptable}/{@code insert}/{@code extract}，
 * 不直写内部账本（D9/D10 口径：取出按账本值）。
 * <p>
 * <b>物品安全</b>：先取后放；放不回的部分退回来路（输入口退回源容器、输出口塞回终端，塞不回则落地），
 * 既不会凭空吞物，也不会凭空复制。
 * <p>
 * <b>过滤网</b>：由调用方以 {@link Predicate} 传入（口方块实体的配置槽）；null = 全通，
 * 仅收窄「抓什么 / 推什么」，不改变上述搬运与退回语义。
 * <p>
 * <b>费用闸门</b>：搬运属于「终端存取」，与 GUI 存取同一套费用口径，故两个方向各有一个回调，
 * 由口方块实体实现（引擎不持有费率 / 价值表口径）。落账顺序照既有存取路径的「预检 → 扣费 → 落账」：
 * 闸门返回 false ⇒ 本批不写单元、不改对面容器，也不扣费。
 */
public final class ItemPortTransfer {

    /** 每批上限（件）：默认 1 组 */
    public static final int BATCH_LIMIT = 64;

    /**
     * 输出口费用闸门（取出费率）：单槽真正取出<b>之前</b>调用。
     * <p>
     * 取出费用必须按单元<b>账本</b>结算（{@code IItemStorageUnit#getSlotIp}），因此入参给出来源
     * 单元 / 槽位 / 取出前槽内容，由实现方照库页取出那一套算法换算，不在引擎里塞费率细节。
     *
     * @param stored 该槽取出前的内容（count = 取出前件数）
     * @param batch  本批实际要取出的堆（count = 实际搬运量）
     * @return false ⇒ 放弃本批剩余（已落账的前几批保留）
     */
    @FunctionalInterface
    public interface WithdrawFeeGate {
        boolean allow(IItemStorageUnit unit, int slot, ItemStack stored, ItemStack batch);
    }

    private ItemPortTransfer() {
    }

    /**
     * 输入口：从面朝方向的容器抓一批物品存进终端。
     *
     * @param filter  过滤网（null = 全通）：只抓匹配的堆，语义照 AE2 输入总线配置槽
     * @param feeGate 存入费用闸门（null = 不结算）：入参为本批实际要存入的堆，false = 放弃本批
     * @return 实际存入件数
     */
    public static int pullInto(ServerLevel level, BlockPos portPos, Direction facing,
            IItemTerminalHost terminal, Predicate<ItemStack> filter,
            Predicate<ItemStack> feeGate) {
        if (level == null || terminal == null) {
            return 0;
        }
        BlockPos source = portPos.relative(facing);
        Direction accessSide = facing.getOpposite();
        ItemStack taken = ItemAccessHolder.get().extract(level, source, accessSide, BATCH_LIMIT, filter);
        if (taken.isEmpty()) {
            return 0;
        }
        // 本批实际能搬多少 = 源容器可取量 ∩ 终端各单元可存量（acceptable 即 insert 的原子边界）
        int amount = (int) Math.min(taken.getCount(), acceptableSpace(terminal, taken));
        // 预检不过 / 费用不足：整批退回源容器，物品与账本零改动（不搬也不扣费）
        if (amount <= 0 || (feeGate != null && !feeGate.test(taken.copyWithCount(amount)))) {
            returnLeftover(level, source, accessSide, portPos, taken);
            return 0;
        }
        // 扣费已过：只落账这批 amount，余量退回源容器
        ItemStack batch = taken.copyWithCount(amount);
        taken.shrink(amount);
        int moved = insertIntoStorage(terminal, batch);
        if (!batch.isEmpty()) {
            taken.grow(batch.getCount()); // 理论上不会发生（amount 已按 acceptable 夹量）
        }
        if (!taken.isEmpty()) {
            returnLeftover(level, source, accessSide, portPos, taken);
        }
        return moved;
    }

    /**
     * 把物品退回源容器；源容器塞不下（例如只输出、拒收）时<b>原地落地</b>。
     * <p>
     * 这一层是"物品守恒"的最后一道：先前这里直接丢弃 {@code insert} 的返回值，
     * 遇到"源容器只出不进"（熔炉产物槽、MEK/热力的输出槽）时就会把物品静默销毁。
     */
    private static void returnLeftover(ServerLevel level, BlockPos source, Direction accessSide,
            BlockPos portPos, ItemStack stack) {
        ItemStack back = ItemAccessHolder.get().insert(level, source, accessSide, stack);
        if (!back.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(level, portPos.getX() + 0.5D,
                    portPos.getY() + 0.5D, portPos.getZ() + 0.5D, back);
        }
    }

    /**
     * 输出口：从终端取一批物品推给面朝方向的容器。
     *
     * @param filter  过滤网（null = 全通）：只推匹配的堆，语义照 AE2 输出总线配置槽
     * @param feeGate 取出费用闸门（null = 不结算）：false = 放弃本批剩余
     * @return 实际推出件数
     */
    public static int pushOut(ServerLevel level, BlockPos portPos, Direction facing,
            IItemTerminalHost terminal, Predicate<ItemStack> filter,
            WithdrawFeeGate feeGate) {
        if (level == null || terminal == null) {
            return 0;
        }
        BlockPos target = portPos.relative(facing);
        Direction accessSide = facing.getOpposite();
        int moved = 0;
        for (IItemStorageUnit unit : terminal.storageUnits()) {
            if (moved >= BATCH_LIMIT) {
                break;
            }
            int slots = unit.slots();
            for (int slot = 0; slot < slots && moved < BATCH_LIMIT; slot++) {
                ItemStack stored = unit.getItem(slot);
                if (stored.isEmpty() || (filter != null && !filter.test(stored))) {
                    continue;
                }
                int want = Math.min(BATCH_LIMIT - moved, stored.getCount());
                // 先按"对面真正塞得下多少"夹量再计费：否则对面满仓时物品会整批塞回终端、费却照扣
                int fit = Math.min(want,
                        ItemAccessHolder.get().acceptable(level, target, accessSide, stored.copyWithCount(want)));
                if (fit <= 0) {
                    return moved; // 对面已满：不动账、不扣费
                }
                // 再扣费（取出费率）：不通过即整批中止，物品与账本零改动
                if (feeGate != null && !feeGate.allow(unit, slot, stored, stored.copyWithCount(fit))) {
                    return moved;
                }
                ItemStack pulled = unit.extract(slot, fit);
                if (pulled.isEmpty()) {
                    continue;
                }
                ItemStack leftover = ItemAccessHolder.get().insert(level, target, accessSide, pulled);
                moved += pulled.getCount() - leftover.getCount();
                if (!leftover.isEmpty()) {
                    // 对面塞不下：余量放回终端；单元整笔拒绝时落地，避免凭空消失
                    int back = unit.insert(leftover);
                    if (back < leftover.getCount()) {
                        ItemStack dropped = leftover.copy();
                        dropped.shrink(back);
                        net.minecraft.world.Containers.dropItemStack(level, portPos.getX() + 0.5,
                                portPos.getY() + 0.5, portPos.getZ() + 0.5, dropped);
                    }
                }
            }
        }
        return moved;
    }

    /**
     * 外部物流塞入（端口作为「终端储存远程接口面」时的写入段）：把 stack 尽量存进终端，
     * 返回<b>未存入的余量</b>（调用方的堆不被修改）。
     * <p>
     * 落账顺序与 {@link #pullInto} 完全一致：按 {@code acceptable} 夹量 → 费用闸门 → 落账；
     * 闸门不过 ⇒ 一件未存、一件未扣，原堆整笔退回。单次上限 {@link #BATCH_LIMIT}：
     * 超出部分直接作为余量退回，不会被"顺手"存进去而绕过节流。
     *
     * @param filter  过滤网（null = 全通；端口实体已在调用前判过，这里兜底）
     * @param feeGate 存入费用闸门（null = 不结算）：false = 放弃本笔
     */
    public static ItemStack insertIntoTerminal(IItemTerminalHost terminal, ItemStack stack,
            Predicate<ItemStack> filter, Predicate<ItemStack> feeGate) {
        ItemStack leftover = stack.copy();
        if (terminal == null || leftover.isEmpty() || (filter != null && !filter.test(leftover))) {
            return leftover;
        }
        int limit = Math.min(leftover.getCount(), BATCH_LIMIT);
        int amount = (int) Math.min(limit, acceptableSpace(terminal, leftover));
        if (amount <= 0 || (feeGate != null && !feeGate.test(leftover.copyWithCount(amount)))) {
            return leftover;
        }
        ItemStack batch = leftover.copyWithCount(amount);
        leftover.shrink(insertIntoStorage(terminal, batch));
        return leftover;
    }

    /**
     * 外部物流可接收量上界（<b>零副作用</b>预检）：≤ 堆量、≤ {@link #BATCH_LIMIT}、≤ 终端空余。
     * 供外部能力在 {@code simulate} / {@code canPlaceItem} 阶段先给出"最多能收多少"的承诺，
     * 不落账也不扣费，因此可安全地被反复探测。
     */
    public static int acceptLimit(IItemTerminalHost terminal, ItemStack stack,
            Predicate<ItemStack> filter) {
        if (terminal == null || stack.isEmpty() || (filter != null && !filter.test(stack))) {
            return 0;
        }
        return (int) Math.min(Math.min(stack.getCount(), BATCH_LIMIT), acceptableSpace(terminal, stack));
    }

    /**
     * 终端各单元合计还能再存入多少件（口径同库页存入的 {@code space} 预检：
     * {@link IItemStorageUnit#acceptable} 与传入堆量无关，故用本批实物探测即可）。
     */
    private static long acceptableSpace(IItemTerminalHost terminal, ItemStack stack) {
        long space = 0L;
        for (IItemStorageUnit unit : terminal.storageUnits()) {
            space += unit.acceptable(stack);
        }
        return space;
    }

    /**
     * 把堆尽量塞进终端各单元（按 {@code acceptable} 分片，因 {@code insert} 是整笔原子）。
     * 就地扣减传入的 stack，返回实存件数。
     */
    private static int insertIntoStorage(IItemTerminalHost terminal, ItemStack stack) {
        int moved = 0;
        for (IItemStorageUnit unit : terminal.storageUnits()) {
            while (!stack.isEmpty()) {
                int acceptable = Math.min(stack.getCount(), unit.acceptable(stack));
                if (acceptable <= 0) {
                    break;
                }
                ItemStack portion = stack.copy();
                portion.setCount(acceptable);
                int inserted = unit.insert(portion);
                if (inserted <= 0) {
                    break;
                }
                stack.shrink(inserted);
                moved += inserted;
            }
            if (stack.isEmpty()) {
                break;
            }
        }
        return moved;
    }
}
