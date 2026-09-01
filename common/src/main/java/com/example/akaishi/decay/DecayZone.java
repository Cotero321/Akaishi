package com.example.akaishi.decay;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * 衰竭区域数据：以中心为球心、半径 5 区块（80 格）的持续污染区。
 * 区域内：非亡灵实体每 tick 刷新 1 秒"衰变"效果并每秒受魔法伤害；环境方块逐步转化为砂土/被破坏；
 * 生物按概率转化（骷髅→凋零骷髅、马→僵尸马/骷髅马、村民→僵尸村民）。
 * 支持 NBT 序列化，由 {@link DecayZoneManager} 持久化到世界存档（跨重启存续）。
 */
public final class DecayZone {

    private final BlockPos center;
    private final int radius;
    /** 衰变效果等级（I=0，桶按液量 1-3，爆炸=衰竭五 4） */
    private final int amplifier;
    /** 所在维度 id 字符串 */
    private final String dimension;
    /** 剩余生效 tick（30 小时） */
    private long remainingTicks;

    DecayZone(BlockPos center, int radius, int amplifier, String dimension, long remainingTicks) {
        this.center = center;
        this.radius = radius;
        this.amplifier = amplifier;
        this.dimension = dimension;
        this.remainingTicks = remainingTicks;
    }

    public BlockPos center() {
        return center;
    }

    public int radius() {
        return radius;
    }

    public int amplifier() {
        return amplifier;
    }

    public String dimension() {
        return dimension;
    }

    /** 每秒伤害 = 2 × (等级+1)（I 级 2 点/秒，衰竭五 10 点/秒） */
    public float damagePerSecond() {
        return 2f * (amplifier + 1);
    }

    /** 剩余 tick 递减，返回 true 表示区域到期 */
    boolean tickDown() {
        return --remainingTicks <= 0;
    }

    /**
     * 净化削减剩余生效 tick（最低 0）。
     *
     * @param ticks 削减量（衰减净化塔按距离/倍率累加供给）
     * @return true 表示区域已被净除（剩余时间归零，可由调用方立即移除）
     */
    boolean purify(long ticks) {
        if (ticks <= 0) {
            return false;
        }
        remainingTicks = Math.max(0, remainingTicks - ticks);
        return remainingTicks <= 0;
    }

    CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("X", center.getX());
        tag.putInt("Y", center.getY());
        tag.putInt("Z", center.getZ());
        tag.putInt("Radius", radius);
        tag.putInt("Amplifier", amplifier);
        tag.putString("Dimension", dimension);
        tag.putLong("Remaining", remainingTicks);
        return tag;
    }

    static DecayZone fromNbt(CompoundTag tag) {
        return new DecayZone(
                new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z")),
                tag.getInt("Radius"),
                tag.getInt("Amplifier"),
                tag.getString("Dimension"),
                tag.getLong("Remaining"));
    }
}
