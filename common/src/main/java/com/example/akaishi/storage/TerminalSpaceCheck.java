package com.example.akaishi.storage;

import java.util.List;

import com.example.akaishi.api.storage.IItemStorageUnit;
import com.example.akaishi.api.storage.IItemTerminalHost;
import com.example.akaishi.value.ItemPoints;

import net.minecraft.world.item.ItemStack;

/**
 * 终端「还能不能再存」的<b>唯一算式源</b>与失败细分。
 * <p>
 * 为什么单独成类：可存量是「槽位余量」与「IP 余量」两个<b>累计型</b>约束的较小者，而这两项分别由
 * 库页动作（{@link com.example.akaishi.menu.TerminalActions}）与搬运引擎
 * （{@link com.example.akaishi.wireless.ItemPortTransfer}）各自预检。若两处各写一份累计逻辑，
 * 迟早出现"一处能存一处不能"；集中到这里后，两条路径与界面展示共用同一份口径。
 * <p>
 * 另：只把 {@code acceptable() == 0} 笼统报成"无空间"，会让"IP 还富余、槽位先满"这种情形完全不可辨
 * —— 玩家只能穷举方向（本项目曾连续两轮误判）。{@link #classifyDepositBlock} 据此把原因细分到
 * {@link IItemTerminalHost#REJECT_DEPOSIT_SLOTS_FULL} 与 {@link IItemTerminalHost#REJECT_DEPOSIT_IP_FULL}。
 * <p>
 * 本类全为纯查询：不落账、不扣费、不改任何状态。
 */
public final class TerminalSpaceCheck {

    private TerminalSpaceCheck() {
    }

    /** 各单元 {@link IItemStorageUnit#acceptable} 的合计（= 本批最多能存几件） */
    public static long acceptable(IItemTerminalHost terminal, ItemStack stack) {
        long space = 0L;
        for (IItemStorageUnit unit : unitsOf(terminal)) {
            space += unit.acceptable(stack);
        }
        return space;
    }

    /**
     * 各单元「槽位侧是否可容纳」的聚合（不含 IP 约束）。
     * <p>
     * <b>不可相加</b>：一槽多堆后单元返回的是 {@link Long#MAX_VALUE} 哨兵（有空槽 / 已有条目即不限），
     * 相加会溢出成负数、把"还有空槽"误判成"槽位已满"。因此这里取<b>任一单元可容纳</b>即视为可容纳。
     *
     * @return {@link Long#MAX_VALUE} = 至少一个单元能放下该新种类；{@code 0} = 所有单元都无空槽且无该条目
     */
    public static long slotRoom(IItemTerminalHost terminal, ItemStack stack) {
        for (IItemStorageUnit unit : unitsOf(terminal)) {
            if (unit.slotRoom(stack) > 0L) {
                return Long.MAX_VALUE;
            }
        }
        return 0L;
    }

    /** 各单元「剩余 IP 能再装几件」的合计 */
    public static long ipRoom(IItemTerminalHost terminal, ItemStack stack) {
        long per = ItemPoints.perItem(stack);
        if (per <= 0L) {
            return 0L;
        }
        long room = 0L;
        for (IItemStorageUnit unit : unitsOf(terminal)) {
            room += Math.max(0L, unit.getIpCapacity() - unit.getStoredIp()) / per;
        }
        return room;
    }

    /** 已占用槽位合计（各单元非空槽计数），供界面展示"槽位：M / N" */
    public static int usedSlots(IItemTerminalHost terminal) {
        int used = 0;
        for (IItemStorageUnit unit : unitsOf(terminal)) {
            int slots = unit.slots();
            for (int i = 0; i < slots; i++) {
                if (!unit.getItem(i).isEmpty()) {
                    used++;
                }
            }
        }
        return used;
    }

    /** 槽位总数合计（各单元 {@link IItemStorageUnit#slots()} 之和） */
    public static int totalSlots(IItemTerminalHost terminal) {
        int total = 0;
        for (IItemStorageUnit unit : unitsOf(terminal)) {
            total += unit.slots();
        }
        return total;
    }

    /**
     * 一件都存不进时，判定到底是哪一项约束触顶（供终端回执与界面文案细分）。
     * <p>
     * 判据（一槽多堆后）：{@code slotRoom == 0} 的含义是「<b>所有单元都没有空槽，且都没有该
     * (物品, NBT) 的条目</b>」—— 这才是真正"需要为新种类腾槽位"的情形；否则就是 IP 余量不足一件。
     * 两个合计都还 &gt; 0 却仍存不进（各单元被不同约束分别卡住，罕见）时回退到笼统的
     * {@link IItemTerminalHost#REJECT_DEPOSIT_NO_SPACE}，不贴可能错的具体原因。
     *
     * @param probe 待存入物品的单件探测堆（{@code acceptable} 与堆量无关，只取物品 + NBT）
     * @return {@link IItemTerminalHost#REJECT_DEPOSIT_SLOTS_FULL} /
     *         {@link IItemTerminalHost#REJECT_DEPOSIT_IP_FULL} /
     *         {@link IItemTerminalHost#REJECT_DEPOSIT_NO_SPACE}
     */
    public static int classifyDepositBlock(IItemTerminalHost terminal, ItemStack probe) {
        if (terminal == null || probe.isEmpty()) {
            return IItemTerminalHost.REJECT_DEPOSIT_NO_SPACE;
        }
        if (slotRoom(terminal, probe) <= 0L) {
            return IItemTerminalHost.REJECT_DEPOSIT_SLOTS_FULL;
        }
        if (ipRoom(terminal, probe) <= 0L) {
            return IItemTerminalHost.REJECT_DEPOSIT_IP_FULL;
        }
        return IItemTerminalHost.REJECT_DEPOSIT_NO_SPACE;
    }

    /** 存活单元视图（宿主为空时给空表，避免调用方各自判空） */
    private static List<IItemStorageUnit> unitsOf(IItemTerminalHost terminal) {
        return terminal == null ? List.of() : terminal.storageUnits();
    }
}
