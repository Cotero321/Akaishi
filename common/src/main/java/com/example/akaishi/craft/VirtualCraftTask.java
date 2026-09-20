package com.example.akaishi.craft;

import com.example.akaishi.api.storage.IItemStorageUnit;
import com.example.akaishi.api.storage.IItemTerminalHost;
import com.example.akaishi.craft.exec.EndpointFinder;
import com.example.akaishi.craft.exec.MachineEndpoint;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.energy.LifeEnergyType;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 真机加工任务：<b>逐节点投料 → 等机台自己加工 → 按期望对账收取</b>的服务端状态机。
 *
 * <p><b>执行序来自规划器</b>（{@link VirtualCraftPlanner.Plan#order()}，依赖优先：先做下级材料再喂上一级）。
 * 本类<b>不自己推账</b> —— 每个节点吃多少料、该收多少货全部照 {@link VirtualCraftPlanner.Step} 执行，
 * 否则就是第二套口径（"报价 ≠ 实收"）。
 *
 * <p><b>三种节点的处理</b>：
 * <ul>
 *   <li><b>机台节点</b>（自研机台 / 经接入器认可的第三方机器）：投料进机器的输入侧，机台按自己的配方跑，
 *       我们从输出侧按"期望的那件产物 + 期望件数"取回。机台是它自己的主人：能量、耗时、并行度都不归我们管；</li>
 *   <li><b>免机台节点</b>（原版工作台/熔炉那类）：场域里本来就没有对应机台，由终端按规划步时代劳
 *       （材料真扣、产出入库，与旧口径一致）；</li>
 *   <li>两者都不做"锁"：材料逐节点从库里取（不预锁全单），机台也不申请占用。</li>
 * </ul>
 *
 * <p><b>对账纪律（不锁机台的代价与对策）</b>：同一台机器可能同时在跑别人的活，输出槽里有别人的东西。
 * 因此收货只按本节点的期望清单与件数取，<b>多一件都不取</b>；中断时只尽力撤回"已投进去但机器还没吃掉的料"，
 * 机器已经消耗掉的料要不回来（真机加工的固有代价，如实记录，不假装能退）。
 *
 * <p><b>终端不代扣能量</b>（用户口径）：手续费与机器能耗只做"够不够"的门槛判断，真实能量由机台自己消耗
 * （自研机台的赤能源来自矩阵的无线能源直供，第三方机器得自己有电）。
 */
public final class VirtualCraftTask {

    /** 任务状态 */
    public enum State {
        /** 执行中 */
        RUNNING,
        /** 全部节点完成 */
        DONE,
        /** 启动失败或中途超时/结构失效 */
        FAILED
    }

    /** 单节点等待上限倍率：预计墙钟 × 本倍率 + {@link #WAIT_GRACE} */
    private static final int WAIT_FACTOR = 4;
    /** 单节点等待宽限（tick）：≥20 秒，覆盖"机器刚被放下/能量刚补上"这类瞬时状态 */
    private static final int WAIT_GRACE = 400;

    private final IItemTerminalHost host;
    private final VirtualCraftPlanner.Plan plan;
    /** 场域半径（区块）：执行器按它与准入同一片场域找机台 */
    private final int radiusChunks;
    /** 执行序（依赖优先），直接取自规划 */
    private final List<VirtualCraftPlanner.Step> order;

    /** 本节点已从库里取出、尚未入机的料：失败时原样回填 */
    private final List<ItemStack> reserved = new ArrayList<>();
    /** 本节点已入机的料（物品 → 件数）：失败时尽力撤回 */
    private final Map<Item, Long> inserted = new LinkedHashMap<>();
    /** 本节点该收的货（物品 → 件数） */
    private Map<Item, Long> expected = Map.of();
    /** 本节点已收的货 */
    private final Map<Item, Long> collected = new LinkedHashMap<>();

    private int cursor;
    @Nullable
    private MachineEndpoint endpoint;
    private boolean inputsReady;
    /** 本节点是否免机台（终端按步时代劳） */
    private boolean handsFree;
    /** 免机台节点剩余步时 */
    private int handsFreeTicks;
    /** 本节点已等待 tick（超时判据） */
    private int waitedTicks;
    /** 已消耗的总 tick（进度条用） */
    private long elapsedTicks;
    private State state = State.RUNNING;
    @Nullable
    private String failReason;

    private VirtualCraftTask(IItemTerminalHost host, VirtualCraftPlanner.Plan plan, int radiusChunks) {
        this.host = host;
        this.plan = plan;
        this.radiusChunks = radiusChunks;
        this.order = plan.order();
    }

    /**
     * 启动一次真机加工；任一步骤不满足即返回 null 且<b>不留下任何副作用</b>。
     *
     * <p><b>为什么这里只做预检、不预先扣料</b>：材料改为<b>逐节点</b>从库里取（节点要跑了才取），
     * 机台不锁、材料也不预锁 —— 预锁全单会让"部分完成"的账变得极其复杂。预检的意义是
     * "按钮亮着点下去必然开得了工"（与 {@code CraftReadiness} 同一口径），不是扣款。
     */
    @Nullable
    public static VirtualCraftTask start(ServerLevel level, IItemTerminalHost host, ICraftEnergyPool pool,
            VirtualCraftPlanner.Plan plan, int radiusChunks) {
        List<IItemStorageUnit> units = host.storageUnits();
        if (units.isEmpty()) {
            return null;
        }
        // 1) 材料齐备（与界面同一份账：现采基础材料 + 直接从库存吃掉的中间产物）
        if (!CraftLibrary.hasAll(units, plan.consumables())) {
            return null;
        }
        // 2) 手续费与机器能量：只做门槛判断，不真扣（真消耗发生在机台那一侧）
        MachineProcessEnergy.Cost machine = plan.machineCost();
        if (!host.canAffordFee(plan.materialIp(), false)
                || !host.canAffordFee(plan.resultIp(), true)
                || pool.availableEnergy(AkaishiEnergyType.INSTANCE) < machine.chishi()
                || pool.availableEnergy(LifeEnergyType.INSTANCE) < machine.life()) {
            return null;
        }
        return new VirtualCraftTask(host, plan, radiusChunks);
    }

    /**
     * 推进一 tick；返回 true 表示任务已结束（成功或失败）。
     * <p>一 tick 内可以连续推进多个节点：免机台节点可能步时为 0，逐个等一 tick 会让这类订单平白变慢。
     */
    public boolean tick(ServerLevel level) {
        if (state != State.RUNNING) {
            return true;
        }
        if (host.isRemoved()) {
            fail(level, "host_gone");
            return true;
        }
        elapsedTicks++;
        while (state == State.RUNNING && cursor < order.size()) {
            if (!stepTick(level)) {
                break; // 本节点还没完成，下一 tick 继续
            }
        }
        if (state == State.RUNNING && cursor >= order.size()) {
            state = State.DONE;
        }
        return state != State.RUNNING;
    }

    /**
     * 推进当前节点一步。
     *
     * @return true = 本节点已完成（调用方继续下一个节点）
     */
    private boolean stepTick(ServerLevel level) {
        VirtualCraftPlanner.Step step = order.get(cursor);
        // ① 备料：本节点的输入从库里真扣（一个节点只扣一次）
        if (!inputsReady) {
            if (!withdraw(step)) {
                fail(level, "not_enough");
                return false;
            }
            inputsReady = true;
            handsFree = step.handsFree();
            handsFreeTicks = (int) Math.min(Math.max(1L, step.wallTicks()), Integer.MAX_VALUE);
            expected = step.expected();
        }
        // ② 免机台节点：终端按步时代劳（材料已真扣，产出入库）
        if (handsFree) {
            if (--handsFreeTicks > 0) {
                return false;
            }
            depositExpected(level);
            return advance();
        }
        // ③ 选机台：每 tick 重试（机器可能刚被放下，或输入位刚腾出来）
        if (endpoint == null) {
            endpoint = EndpointFinder.pick(level, host.getBlockPos(), radiusChunks, step);
            if (endpoint == null) {
                waited(level, step, "no_machine");
                return false;
            }
            waitedTicks = 0;
        }
        // ④ 投料：塞不下的余量留着下一 tick 继续塞（等机器把输入槽吃掉）
        if (!reserved.isEmpty()) {
            insertPending();
            if (!reserved.isEmpty()) {
                waited(level, step, "machine_full");
                return false;
            }
        }
        // ⑤ 收货：只按期望清单与件数取（机台共用，多取就是偷别人的）
        harvest(level);
        if (!collectedAll()) {
            waited(level, step, "timeout");
            return false;
        }
        // 输入保留型（培养机）：机器从不扣料，收完产物要把料取回库，账才对得上
        if (step.inputKept()) {
            reclaimInserted(level);
        }
        return advance();
    }

    // ===== 库操作 =====

    /** 按本节点输入清单从库里取料；中途取不够（被并发订单先取走）即失败 */
    private boolean withdraw(VirtualCraftPlanner.Step step) {
        List<IItemStorageUnit> units = host.storageUnits();
        for (Map.Entry<Item, Long> need : step.inputs().entrySet()) {
            if (need.getValue() <= 0L) {
                continue;
            }
            List<ItemStack> got = CraftLibrary.take(units, need.getKey(), need.getValue());
            reserved.addAll(got);
            long total = 0L;
            for (ItemStack stack : got) {
                total += stack.getCount();
            }
            if (total < need.getValue()) {
                return false;
            }
        }
        return true;
    }

    /** 把待投的料塞进机台输入侧（塞不进的留在 {@code reserved} 里下一 tick 再试） */
    private void insertPending() {
        if (endpoint == null) {
            return;
        }
        for (int i = reserved.size() - 1; i >= 0; i--) {
            ItemStack stack = reserved.get(i);
            if (stack.isEmpty()) {
                reserved.remove(i);
                continue;
            }
            ItemStack left = endpoint.insert(stack);
            int moved = stack.getCount() - left.getCount();
            if (moved > 0) {
                inserted.merge(stack.getItem(), (long) moved, Long::sum);
            }
            if (left.isEmpty()) {
                reserved.remove(i);
            } else {
                reserved.set(i, left);
            }
        }
    }

    /** 从机台输出侧按期望清单收货（取到就立即入库，不留中间态） */
    private void harvest(ServerLevel level) {
        if (endpoint == null) {
            return;
        }
        for (Map.Entry<Item, Long> entry : expected.entrySet()) {
            long want = entry.getValue() - collected.getOrDefault(entry.getKey(), 0L);
            while (want > 0L) {
                ItemStack got = endpoint.extract(entry.getKey(), (int) Math.min(want, Integer.MAX_VALUE));
                if (got.isEmpty()) {
                    break;
                }
                want -= got.getCount();
                collected.merge(entry.getKey(), (long) got.getCount(), Long::sum);
                deposit(level, got);
            }
        }
    }

    /** 中断/失败时尽力从机台输入侧撤回已投的料（机器已吃掉的部分要不回来） */
    private void reclaimInserted(ServerLevel level) {
        if (endpoint == null || inserted.isEmpty()) {
            return;
        }
        for (Map.Entry<Item, Long> entry : new LinkedHashMap<>(inserted).entrySet()) {
            long left = entry.getValue();
            while (left > 0L) {
                ItemStack got = endpoint.reclaim(entry.getKey(), (int) Math.min(left, Integer.MAX_VALUE));
                if (got.isEmpty()) {
                    break;
                }
                left -= got.getCount();
                deposit(level, got);
            }
        }
        inserted.clear();
    }

    /** 免机台节点的产出（材料已真扣，这里把规划好的产物入库） */
    private void depositExpected(ServerLevel level) {
        for (Map.Entry<Item, Long> entry : expected.entrySet()) {
            long left = entry.getValue();
            int per = Math.max(1, entry.getKey().getMaxStackSize());
            while (left > 0L) {
                int chunk = (int) Math.min(left, per);
                deposit(level, new ItemStack(entry.getKey(), chunk));
                left -= chunk;
            }
        }
    }

    /** 入库；塞不下的落地（物品守恒，不吞不复制） */
    private void deposit(ServerLevel level, ItemStack stack) {
        ItemStack leftover = CraftLibrary.insertAll(host.storageUnits(), stack);
        if (!leftover.isEmpty()) {
            dropAt(level, leftover);
        }
    }

    /** 回填全部未入机的料；回填不下的落地（物品守恒） */
    private void refundAll(ServerLevel level) {
        if (reserved.isEmpty()) {
            return;
        }
        if (host.isRemoved()) {
            // 宿主已不在世界里（如芯片被拆）：回填进它的内部数据等于凭空蒸发，改为就地落地
            for (ItemStack stack : reserved) {
                dropAt(level, stack);
            }
        } else {
            CraftLibrary.refund(level, host.getBlockPos(), host.storageUnits(), reserved);
        }
        reserved.clear();
    }

    private void dropAt(ServerLevel level, ItemStack stack) {
        BlockPos pos = host.getBlockPos();
        Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, stack);
    }

    // ===== 节点推进与状态 =====

    private boolean advance() {
        cursor++;
        endpoint = null;
        inputsReady = false;
        handsFree = false;
        handsFreeTicks = 0;
        expected = Map.of();
        collected.clear();
        reserved.clear();
        inserted.clear();
        waitedTicks = 0;
        return true;
    }

    private boolean collectedAll() {
        for (Map.Entry<Item, Long> entry : expected.entrySet()) {
            if (collected.getOrDefault(entry.getKey(), 0L) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    /** 记一 tick 等待；超过"预计墙钟 × 倍率 + 宽限"即判失败（原因区分"没机器/机器满/超时"） */
    private void waited(ServerLevel level, VirtualCraftPlanner.Step step, String reason) {
        long limit = MachineProcessEnergy.saturatingMultiply(Math.max(0L, step.wallTicks()), WAIT_FACTOR)
                + WAIT_GRACE;
        if (++waitedTicks > limit) {
            fail(level, reason);
        }
    }

    /** 主动失败（结构被拆 / 中断）并尽力挽回材料 */
    public void fail(ServerLevel level) {
        fail(level, "aborted");
    }

    private void fail(ServerLevel level, String reason) {
        if (state != State.RUNNING) {
            return;
        }
        state = State.FAILED;
        this.failReason = reason;
        reclaimInserted(level); // 已投进机器的：尽力撤回
        refundAll(level);       // 还没入机的：原样回填
    }

    public State state() {
        return state;
    }

    public VirtualCraftPlanner.Plan plan() {
        return plan;
    }

    /** 失败原因（诊断指令用；成功为 null）：{@code host_gone / not_enough / no_machine / machine_full / timeout / aborted} */
    @Nullable
    public String failReason() {
        return failReason;
    }

    /** 已完成的节点数 / 总节点数（进度用） */
    public int completedSteps() {
        return cursor;
    }

    public int totalSteps() {
        return order.size();
    }

    /**
     * 剩余 tick（界面进度条用）。
     * <p><b>只是预估</b>：真机加工由机台决定实际耗时（第三方机器我们连它的速度都读不到），
     * 故这里按规划的预计总时长线性扣减，不做任何"保证按时完成"的暗示。
     */
    public int remainingTicks() {
        long left = plan.totalTicks() - elapsedTicks;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, left));
    }
}
