package com.example.akaishi.wireless;

import com.example.akaishi.api.storage.IItemStorageUnit;
import com.example.akaishi.api.storage.IItemTerminalHost;
import com.example.akaishi.api.transfer.IItemAccess;
import com.example.akaishi.api.transfer.ItemAccessHolder;
import com.example.akaishi.storage.TerminalSpaceCheck;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;
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
 * <b>物品安全（先判后取）</b>：输入口在 {@code extract} <b>之前</b>先用 {@link IItemAccess#peek}
 * 零副作用探测源容器里的第一个匹配堆，据此算终端可存量 —— 一件也存不下时本周期<b>不做任何位移</b>
 * （不抽、不退、不落地），从根上排除"满了还在从源容器掏东西往外丢"。
 * 只有在确认收得下之后才真正取出，放不回的部分再退回来路（输入口退回源容器、输出口塞回终端，
 * 塞不回则落地——安全网，且记限流 WARN），既不会凭空吞物，也不会凭空复制。
 * <p>
 * <b>过滤网</b>：由调用方以 {@link Predicate} 传入（口方块实体的配置槽）；null = 全通，
 * 仅收窄「抓什么 / 推什么」，不改变上述搬运与退回语义。
 * <p>
 * <b>费用闸门</b>：搬运属于「终端存取」，与 GUI 存取同一套费用口径，故两个方向各有一个回调，
 * 由口方块实体实现（引擎不持有费率 / 价值表口径）。落账顺序照既有存取路径的「预检 → 扣费 → 落账」：
 * 闸门返回 false ⇒ 本批不写单元、不改对面容器，也不扣费。
 * <p>
 * <b>结果回传</b>：两个方向都返回 {@link Result}（件数 + {@link Reason}），把"一件都没搬"的
 * 原因（对面不是容器 / 终端满 / 对面满 / 费用闸门拒绝）显式交给调用方 —— 历史上只返回 {@code int}，
 * 于是这些失败全被压成"0 件"的静默空转，界面上无从区分。
 */
public final class ItemPortTransfer {

    /** 每批上限（件）：默认 1 组 */
    public static final int BATCH_LIMIT = 64;

    /** 排障日志：与端口实体同名（{@code akaishi.itemport}）⇒ 一条 grep 就能收齐搬运与落地两类日志 */
    private static final org.apache.logging.log4j.Logger LOGGER =
            org.apache.logging.log4j.LogManager.getLogger("akaishi.itemport");

    /**
     * 「余量落地」WARN 的限流间隔（tick）；<b>待调手感值</b>（与端口实体的
     * {@code FAIL_LOG_INTERVAL_TICKS} 同值 200 = 10 秒）。落地即代表物品守恒的最后一道被触发，
     * 必须可从日志发现"仍在流失"，但也不能刷屏。
     */
    private static final long LEFTOVER_LOG_INTERVAL_TICKS = 200;

    /** 上次「余量落地」WARN 的游戏时间（限流用；引擎为纯静态 ⇒ 全服共享限流，宁少勿刷） */
    private static long lastLeftoverLogTick = -LEFTOVER_LOG_INTERVAL_TICKS;

    /** 搬运结果原因码：解释本批「为什么一件都没搬」（{@link Result#reason()}） */
    public enum Reason {
        /** 无异常：本批有搬运，或对面/终端只是空转（容器在但没东西可搬，<b>不该报红</b>） */
        NONE,
        /** 面朝处没有可读写的物品容器：没放容器 / 面向不对 / 对面区块未加载 */
        NO_CONTAINER,
        /** 终端一件也存不下（输入口方向：各单元可存量合计为 0） */
        TERMINAL_FULL,
        /** 对面容器一件也塞不进（输出口方向：对面满仓 / 拒收） */
        TARGET_FULL,
        /** 费用闸门拒绝本批（物品与账本零改动；具体口径如"赤能源不足"由调用方给出） */
        FEE_DENIED
    }

    /**
     * 搬运结果：本批实际搬运件数 + 原因码。
     * <p>
     * {@code moved > 0} 时 {@code reason} 表示「停下时又撞上了什么」（例如推了半批才撞到对面满仓）；
     * {@code moved == 0} 时 {@code reason} 才是「为什么一件都没搬」。
     */
    public record Result(int moved, Reason reason) {

        /** 空结果（未绑定 / 参数缺失 / 正常空转）：界面不报红 */
        public static final Result NONE = new Result(0, Reason.NONE);
    }

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
     * @return 搬运件数 + 原因码（{@link Reason#NO_CONTAINER} / {@link Reason#TERMINAL_FULL} /
     *         {@link Reason#FEE_DENIED}）；对面容器在但没东西可搬时返回 {@link Reason#NONE}（正常空转）
     */
    public static Result pullInto(ServerLevel level, BlockPos portPos, Direction facing,
            IItemTerminalHost terminal, Predicate<ItemStack> filter,
            Predicate<ItemStack> feeGate) {
        return pullInto(level, portPos, facing, terminal, filter, feeGate, null);
    }

    /**
     * 输入口（带<b>落点上报</b>的重载）：语义与上面完全一致，只多一条"真正存进了哪个单元"的通知。
     *
     * @param landingSink 落点回调（单元 + 本次存入件数）；null = 不上报。纯通知，
     *                    <b>不参与</b>夹量 / 计费 / 退回任何判定，因此不改变物品守恒口径
     */
    public static Result pullInto(ServerLevel level, BlockPos portPos, Direction facing,
            IItemTerminalHost terminal, Predicate<ItemStack> filter,
            Predicate<ItemStack> feeGate, BiConsumer<IItemStorageUnit, Integer> landingSink) {
        if (level == null || terminal == null) {
            return Result.NONE;
        }
        BlockPos source = portPos.relative(facing);
        Direction accessSide = facing.getOpposite();
        // 先判后取（§21 教训）：据源容器第一个匹配堆零副作用预判终端能否接收。
        // 终端一件也存不下时本周期"零位移"—— 不 extract、不退回、不落地（原先"先抽再判"在这里
        // 每 8 tick 真把物品抽出来、再试图放回；源只出不进时会落地，即玩家看到的"满了还在流失"）。
        // peek 返回空堆有两种：无匹配物（本来就没得搬），或本实现不提供探测（退回原有先取后判路径）。
        ItemStack peeked = ItemAccessHolder.get().peek(level, source, accessSide, filter);
        if (!peeked.isEmpty() && TerminalSpaceCheck.acceptable(terminal, peeked) <= 0) {
            // 终端侧回执：不搬的真正原因是"终端没空间"；细分到槽位满 / IP 不足，与库页同一口径
            terminal.noteReject(TerminalSpaceCheck.classifyDepositBlock(terminal, peeked), 0L);
            return new Result(0, Reason.TERMINAL_FULL);
        }
        ItemStack taken = ItemAccessHolder.get().extract(level, source, accessSide, BATCH_LIMIT, filter);
        if (taken.isEmpty()) {
            // 空手而归有两种：对面根本不是容器（报红指引），或对面在、只是空/无匹配物（正常空转，不报红）。
            // 判据用 hasItemCapability：它与 extract 同源解析（同一 handler 查找），只判"有没有容器"，纯查询无副作用。
            return ItemAccessHolder.get().hasItemCapability(level, source, accessSide)
                    ? Result.NONE : new Result(0, Reason.NO_CONTAINER);
        }
        // 本批实际能搬多少 = 源容器可取量 ∩ 终端各单元可存量（acceptable 即 insert 的原子边界）
        int amount = (int) Math.min(taken.getCount(), TerminalSpaceCheck.acceptable(terminal, taken));
        if (amount <= 0) {
            // 终端一件也存不下：整批退回源容器，物品与账本零改动（不搬也不扣费）
            returnLeftover(level, source, accessSide, portPos, taken);
            // 终端侧回执：不搬的真正原因是"终端没空间"；细分到槽位满 / IP 不足，与库页同一口径
            terminal.noteReject(TerminalSpaceCheck.classifyDepositBlock(terminal, taken), 0L);
            return new Result(0, Reason.TERMINAL_FULL);
        }
        // 费用不足：整批退回源容器，物品与账本零改动（不搬也不扣费）
        if (feeGate != null && !feeGate.test(taken.copyWithCount(amount))) {
            returnLeftover(level, source, accessSide, portPos, taken);
            return new Result(0, Reason.FEE_DENIED);
        }
        // 扣费已过：只落账这批 amount，余量退回源容器
        ItemStack batch = taken.copyWithCount(amount);
        taken.shrink(amount);
        int moved = insertIntoStorage(terminal, batch, landingSink);
        if (!batch.isEmpty()) {
            taken.grow(batch.getCount()); // 理论上不会发生（amount 已按 acceptable 夹量）
        }
        if (!taken.isEmpty()) {
            returnLeftover(level, source, accessSide, portPos, taken);
        }
        if (moved > 0) {
            // 本批真的进库 ⇒ 清空终端侧"最近一次被拒绝"的回执（成功即清空）
            terminal.noteReject(IItemTerminalHost.REJECT_NONE, 0L);
        }
        return new Result(moved, Reason.NONE);
    }

    /**
     * 把物品退回源容器；源容器塞不下（例如只输出、拒收）时<b>原地落地</b>。
     * <p>
     * 这一层是"物品守恒"的最后一道：先前这里直接丢弃 {@code insert} 的返回值，
     * 遇到"源容器只出不进"（熔炉产物槽、MEK/热力的输出槽）时就会把物品静默销毁。
     * <p>
     * 落地是<b>安全网</b>而非正常路径：每触发一次就代表"某条路径仍把物品抽出来后塞不回"，
     * 故按 {@link #LEFTOVER_LOG_INTERVAL_TICKS} 限流打一条 WARN（含端口坐标 / 朝向 / 物品 / 件数），
     * 便于从日志发现"仍在流失"；落点取<b>端口方块上方</b>（可达、不嵌在方块内部）。
     */
    private static void returnLeftover(ServerLevel level, BlockPos source, Direction accessSide,
            BlockPos portPos, ItemStack stack) {
        ItemStack back = ItemAccessHolder.get().insert(level, source, accessSide, stack);
        if (!back.isEmpty()) {
            dropLeftover(level, portPos, accessSide.getOpposite(), back);
        }
    }

    /**
     * 余量兜底落地：限流 WARN + 落点取端口方块<b>上方</b>（避免掉进端口所在方块内部被挤压、不可达）。
     *
     * @param facing 端口朝向（日志用，便于定位是哪一面在往外交付物品）
     */
    private static void dropLeftover(ServerLevel level, BlockPos portPos, Direction facing, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        if (now - lastLeftoverLogTick >= LEFTOVER_LOG_INTERVAL_TICKS) {
            lastLeftoverLogTick = now;
            LOGGER.warn("[akaishi] item port leftover dropped: port={} facing={} item={} count={}",
                    portPos, facing, stack.getItem().builtInRegistryHolder().key().location(), stack.getCount());
        }
        net.minecraft.world.Containers.dropItemStack(level, portPos.getX() + 0.5D,
                portPos.getY() + 1.0D, portPos.getZ() + 0.5D, stack);
    }

    /**
     * 输出口：从终端取一批物品推给面朝方向的容器。
     *
     * @param filter  过滤网（null = 全通）：只推匹配的堆，语义照 AE2 输出总线配置槽
     * @param feeGate 取出费用闸门（null = 不结算）：false = 放弃本批剩余
     * @return 搬运件数 + 原因码（{@link Reason#NO_CONTAINER} / {@link Reason#TARGET_FULL} /
     *         {@link Reason#FEE_DENIED}）；终端在但没东西可推时返回 {@link Reason#NONE}（正常空转）
     */
    public static Result pushOut(ServerLevel level, BlockPos portPos, Direction facing,
            IItemTerminalHost terminal, Predicate<ItemStack> filter,
            WithdrawFeeGate feeGate) {
        if (level == null || terminal == null) {
            return Result.NONE;
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
                    // 对面一件也塞不进：满仓（对面容器在）还是根本不是容器，分别报两种原因，不动账、不扣费
                    return new Result(moved, ItemAccessHolder.get().hasItemCapability(level, target, accessSide)
                            ? Reason.TARGET_FULL : Reason.NO_CONTAINER);
                }
                // 再扣费（取出费率）：不通过即整批中止，物品与账本零改动
                if (feeGate != null && !feeGate.allow(unit, slot, stored, stored.copyWithCount(fit))) {
                    return new Result(moved, Reason.FEE_DENIED);
                }
                ItemStack pulled = unit.extract(slot, fit);
                if (pulled.isEmpty()) {
                    continue;
                }
                ItemStack leftover = ItemAccessHolder.get().insert(level, target, accessSide, pulled);
                moved += pulled.getCount() - leftover.getCount();
                if (!leftover.isEmpty()) {
                    // 对面塞不下：余量放回终端；单元整笔拒绝时落地（安全网，记限流 WARN），避免凭空消失
                    int back = unit.insert(leftover);
                    if (back < leftover.getCount()) {
                        ItemStack dropped = leftover.copy();
                        dropped.shrink(back);
                        dropLeftover(level, portPos, facing, dropped);
                    }
                }
            }
        }
        if (moved > 0) {
            // 本批真的取出 ⇒ 清空终端侧"最近一次被拒绝"的回执（成功即清空）
            terminal.noteReject(IItemTerminalHost.REJECT_NONE, 0L);
        }
        return new Result(moved, Reason.NONE);
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
        return insertIntoTerminal(terminal, stack, filter, feeGate, null);
    }

    /**
     * 外部塞入（带<b>落点上报</b>的重载）：语义与上面完全一致，只多一条"真正存进了哪个单元"的通知。
     *
     * @param landingSink 落点回调（单元 + 本次存入件数）；null = 不上报。纯通知，不参与任何判定
     */
    public static ItemStack insertIntoTerminal(IItemTerminalHost terminal, ItemStack stack,
            Predicate<ItemStack> filter, Predicate<ItemStack> feeGate,
            BiConsumer<IItemStorageUnit, Integer> landingSink) {
        ItemStack leftover = stack.copy();
        if (terminal == null || leftover.isEmpty() || (filter != null && !filter.test(leftover))) {
            return leftover;
        }
        int limit = Math.min(leftover.getCount(), BATCH_LIMIT);
        int amount = (int) Math.min(limit, TerminalSpaceCheck.acceptable(terminal, leftover));
        if (amount <= 0 || (feeGate != null && !feeGate.test(leftover.copyWithCount(amount)))) {
            if (amount <= 0) {
                // 终端侧回执：外部塞入失败的真正原因是"终端没空间"（细分口径同库页）
                terminal.noteReject(TerminalSpaceCheck.classifyDepositBlock(terminal, leftover), 0L);
            }
            return leftover;
        }
        ItemStack batch = leftover.copyWithCount(amount);
        int inserted = insertIntoStorage(terminal, batch, landingSink);
        leftover.shrink(inserted);
        if (inserted > 0) {
            // 本笔真的进库 ⇒ 清空终端侧"最近一次被拒绝"的回执（成功即清空）
            terminal.noteReject(IItemTerminalHost.REJECT_NONE, 0L);
        }
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
        return (int) Math.min(Math.min(stack.getCount(), BATCH_LIMIT), TerminalSpaceCheck.acceptable(terminal, stack));
    }

    /**
     * 把堆尽量塞进终端各单元（按 {@code acceptable} 分片，因 {@code insert} 是整笔原子）。
     * 就地扣减传入的 stack，返回实存件数。
     *
     * @param landingSink 每次真正存入后回调（单元 + 本次件数）；null = 不上报。
     *                    回调只做通知，异常由调用方自负（本引擎不包 try/catch，保持零额外开销语义）
     */
    private static int insertIntoStorage(IItemTerminalHost terminal, ItemStack stack,
            BiConsumer<IItemStorageUnit, Integer> landingSink) {
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
                if (landingSink != null) {
                    landingSink.accept(unit, inserted);
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
