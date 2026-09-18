package com.example.akaishi.craft;

import com.example.akaishi.api.storage.IItemStorageUnit;
import com.example.akaishi.api.storage.IItemTerminalHost;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 虚拟加工任务：一次「<b>锁定材料 → 预扣手续费 → 倒计时 → 产物入库</b>」的服务端状态机。
 * <p>
 * <b>为什么启动就扣料</b>（借 AE2 的「在途库存锁定」）：材料必须离开可用库存，
 * 否则同一批材料会被并发的两单重复认领。锁定期间材料只存在于本任务的 {@code reserved} 列表，
 * 任务失败一律<b>原样回填</b>（回填不下才落地成掉落物，保证物品守恒，不吞不复制）。
 * <p>
 * <b>为什么两笔手续费都在启动时预扣</b>：取出费与产物入库费一起收，避免"等了半天，产出时能量不足"
 * 这种白等情形；能量不足时启动阶段就直接失败，材料立刻回填。
 * <p>
 * <b>宿主</b>：{@link IItemTerminalHost}（物品族微缩芯片）——矩阵本身没有能量也没有库，
 * 库与赤能源缓冲都在芯片上，见设计记忆 §15.9。
 */
public final class VirtualCraftTask {

    /** 任务状态 */
    public enum State {
        /** 倒计时中 */
        RUNNING,
        /** 已产出 */
        DONE,
        /** 启动失败或中途结构失效 */
        FAILED
    }

    private final IItemTerminalHost host;
    private final VirtualCraftPlanner.Plan plan;
    /** 已锁定的材料（失败时原样回填） */
    private final List<ItemStack> reserved = new ArrayList<>();
    /** 剩余倒计时（手续费已在启动时一次性结清，见 {@code start}） */
    private int remainingTicks;
    private State state = State.RUNNING;

    private VirtualCraftTask(IItemTerminalHost host, VirtualCraftPlanner.Plan plan, int ticks) {
        this.host = host;
        this.plan = plan;
        this.remainingTicks = ticks;
    }

    /**
     * 启动一次虚拟加工；任一步骤不满足即返回 null 且<b>不留下任何副作用</b>。
     * <p>
     * 顺序：核对材料 → 扣料（锁定）→ 预扣两笔手续费 → 起倒计时。
     * 扣料后任一步失败都会把已扣材料回填，因此调用方无需做补偿。
     */
    @Nullable
    public static VirtualCraftTask start(ServerLevel level, IItemTerminalHost host, VirtualCraftPlanner.Plan plan) {
        List<IItemStorageUnit> units = host.storageUnits();
        if (units.isEmpty()) {
            return null;
        }
        // 要锁定的东西 = 现采的基础材料 + 规划时决定"直接从库存吃掉"的中间产物（见 Plan.taken）
        List<VirtualCraftPlanner.Leaf> consumables = plan.consumables();
        // 1) 核对材料（先查后取：避免取了一半才发现不够，虽然下面也会回填，但少一次写操作）
        if (!CraftLibrary.hasAll(units, consumables)) {
            return null;
        }
        VirtualCraftTask task = new VirtualCraftTask(host, plan, (int) Math.min(plan.totalTicks(), Integer.MAX_VALUE));
        // 2) 扣料（锁定）：一次扣完，避免逐叶重建库快照
        task.reserved.addAll(CraftLibrary.takeAll(units, consumables));
        long locked = task.reserved.stream().mapToLong(ItemStack::getCount).sum();
        long wanted = consumables.stream().mapToLong(VirtualCraftPlanner.Leaf::count).sum();
        if (locked < wanted) {
            // 竞态：核对与扣料之间被别的路径取走了 —— 回填已扣部分后失败
            task.refundAll(level);
            return null;
        }
        // 3) 结算两笔手续费（取出费按材料 IP、入库费按产物 IP）
        // <b>必须先按"两笔之和"预检</b>：接口没有退费能力，若第一笔扣完才发现第二笔付不起，
        // 那笔能量就凭空消失了。用存入费率评两笔之和偏保守（存入费率 ≥ 取出费率），
        // 预检通过时两笔必然都付得起（极端取整下至多差 2 点，仍由下面的逐笔结算兜底）
        if (!host.canAffordFee(plan.materialIp() + plan.resultIp(), true)
                || !host.tryChargeFee(plan.materialIp(), false)
                || !host.tryChargeFee(plan.resultIp(), true)) {
            task.refundAll(level);
            return null;
        }
        return task;
    }

    /** 推进一 tick；返回 true 表示任务已结束（成功或失败） */
    public boolean tick(ServerLevel level) {
        if (state != State.RUNNING) {
            return true;
        }
        if (host.isRemoved()) {
            fail(level);
            return true;
        }
        if (--remainingTicks > 0) {
            return false;
        }
        // 产物入库：整批执行的<b>全部净产出</b>（目标 + 富余的中间产物）都进库，塞不下则落地（不吞）
        for (ItemStack output : plan.outputs()) {
            ItemStack leftover = CraftLibrary.insertAll(host.storageUnits(), output);
            if (!leftover.isEmpty()) {
                BlockPos pos = host.getBlockPos();
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, leftover);
            }
        }
        state = State.DONE;
        reserved.clear();
        return true;
    }

    /** 主动失败（结构被拆 / 终端失效）并回填材料 */
    public void fail(ServerLevel level) {
        if (state != State.RUNNING) {
            return;
        }
        state = State.FAILED;
        refundAll(level);
    }

    public State state() {
        return state;
    }

    public VirtualCraftPlanner.Plan plan() {
        return plan;
    }

    /** 剩余 tick（界面进度条用） */
    public int remainingTicks() {
        return Math.max(0, remainingTicks);
    }

    // ===== 库操作（统一走 CraftLibrary，账本与失败纪律只有一处实现） =====

    /** 回填全部锁定材料；回填不下的落地（物品守恒，不吞不复制） */
    private void refundAll(ServerLevel level) {
        if (reserved.isEmpty()) {
            return;
        }
        if (host.isRemoved()) {
            // 宿主已不在世界里（如芯片被拆）：回填进它的内部数据等于凭空蒸发（掉落物不含这批料），
            // 因此改为就地落地，让玩家还能捡回来
            BlockPos pos = host.getBlockPos();
            for (ItemStack stack : reserved) {
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, stack);
            }
        } else {
            CraftLibrary.refund(level, host.getBlockPos(), host.storageUnits(), reserved);
        }
        reserved.clear();
    }
}
