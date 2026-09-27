package com.example.akaishi.storage;

import com.example.akaishi.api.storage.IItemTerminalHost;
import com.example.akaishi.util.LongDataSlots;

import net.minecraft.world.inventory.ContainerData;

/**
 * 「最近一次存取被拒绝」的记录体：终端侧的原因回执，供库页界面显式呈现失败原因。
 * <p>
 * <b>为什么单独成类</b>：方块终端（{@link com.example.akaishi.block.entity.AkaishiItemTerminalBlockEntity}）
 * 与微缩芯片（{@link com.example.akaishi.miniature.ItemTerminalMiniatureState}）是两套宿主实现，
 * 但"记录什么、怎么写进数据槽"必须只有一份口径（写入方分散在库页动作 / 搬运引擎 / 储存口三处）。
 * <p>
 * <b>它是纯回执</b>：不参与任何判定、计费与落账；写入方失败时 {@link #note}、成功时 {@link #clear}。
 * <p>
 * 数据槽经 {@code ClientboundContainerSetDataPacket} 以 short 传输，故原因码必然 &lt; 2^15；
 * 「本批所需赤能源」可能到 10^6 量级，用 {@link LongDataSlots#writeInt} 拆 2 槽按完整 32 位同步。
 */
public final class TerminalRejectLog {

    /** 最近一次被拒绝的原因（{@link IItemTerminalHost#REJECT_NONE} = 无失败） */
    private int reason = IItemTerminalHost.REJECT_NONE;
    /** 被拒绝那一笔所需赤能源（无意义时为 0） */
    private long feeNeed;

    /** 记录一次拒绝（原因码 + 本批所需赤能源；无意义传 0） */
    public void note(int rejectReason, long feeNeed) {
        this.reason = rejectReason;
        this.feeNeed = Math.max(0L, feeNeed);
    }

    /** 清空（本笔成功） */
    public void clear() {
        note(IItemTerminalHost.REJECT_NONE, 0L);
    }

    /** 原因码（供服务端逻辑读；界面读同步后的数据槽） */
    public int reason() {
        return reason;
    }

    /** 本批所需赤能源（供服务端逻辑读） */
    public long feeNeed() {
        return feeNeed;
    }

    /**
     * 回放到宿主的数据槽（服务端每 tick 调一次，与其它读数同一节拍）。
     * <p>
     * 值不变时原版数据槽比对不会发包，故稳态零网络开销。
     */
    public void writeTo(ContainerData data) {
        data.set(IItemTerminalHost.DATA_REJECT_REASON, reason);
        LongDataSlots.writeInt(data, IItemTerminalHost.DATA_REJECT_FEE_NEED_LOW,
                IItemTerminalHost.DATA_REJECT_FEE_NEED_HIGH, clampInt(feeNeed));
    }

    /** 钳到 int 范围（费率换算理论上不会溢出，这里只做防御） */
    private static int clampInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, value);
    }
}
