package com.example.akaishi.life.mechanical;

import com.example.akaishi.life.body.BodySlot;
import net.minecraft.nbt.CompoundTag;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * 机械器官整合度数据：每个可安装机械器官的槽位对应一个 0~100 的整合度。
 * 整合度决定机械器官的实际生效比例，与生物排异不同，整合度只会随时间正向增长。
 * 数据存储在玩家身体状态中，随身体一起保存/恢复。
 *
 * <p>整合度增长采用浮点累加机制：每 tick 计算浮点增长值，累积到 pendingGrowth，
 * 满 1 点才消耗并增加整数整合度。避免 {@code Math.round(growth)} 因增长值太小而始终为 0。</p>
 */
public class MechanicalIntegration {

    public static final int MAX_INTEGRATION = 100;
    /** 最低生效比例：即使整合度为 0，器官也不会完全失效 */
    public static final double MIN_EFFECTIVE_RATIO = 0.2;

    private final Map<BodySlot, Integer> integration = new EnumMap<>(BodySlot.class);
    /** 浮点累积余数：key=槽位，value=未满 1 点的累积值 */
    private final Map<BodySlot, Double> pendingGrowth = new EnumMap<>(BodySlot.class);

    // NBT 键
    private static final String TAG_INTEGRATION = "mechanical_integration";
    private static final String TAG_PENDING = "mechanical_pending";
    private static final String TAG_SLOT = "slot";
    private static final String TAG_VALUE = "value";

    /** 获取某个槽位的整合度，未安装机械器官返回 0 */
    public int get(BodySlot slot) {
        return integration.getOrDefault(slot, 0);
    }

    /** 设置某个槽位的整合度 */
    public void set(BodySlot slot, int value) {
        integration.put(slot, clamp(value));
    }

    /** 增加某个槽位的整合度，返回实际增加的整数点 */
    public int add(BodySlot slot, int delta) {
        if (delta <= 0) return 0;
        int current = get(slot);
        if (current >= MAX_INTEGRATION) return 0;
        int actual = Math.min(delta, MAX_INTEGRATION - current);
        integration.put(slot, current + actual);
        return actual;
    }

    /**
     * 尝试消耗浮点增长值，转换为整数增量。
     * 将 {@code growth} 累加到 pendingGrowth，若累积 ≥ 1 则消耗并返回整数部分。
     *
     * @param slot  槽位
     * @param growth 本次 tick 的浮点增长值（正数）
     * @return 实际增加的整数整合度点数
     */
    public int tryConsumeGrowth(BodySlot slot, double growth) {
        if (growth <= 0) return 0;
        int current = get(slot);
        if (current >= MAX_INTEGRATION) {
            pendingGrowth.remove(slot);
            return 0;
        }
        double accumulated = pendingGrowth.getOrDefault(slot, 0.0) + growth;
        int delta = (int) Math.floor(accumulated);
        if (delta > 0) {
            int actual = Math.min(delta, MAX_INTEGRATION - current);
            integration.put(slot, current + actual);
            double remainder = accumulated - delta;
            if (remainder > 0) {
                pendingGrowth.put(slot, remainder);
            } else {
                pendingGrowth.remove(slot);
            }
            return actual;
        }
        pendingGrowth.put(slot, accumulated);
        return 0;
    }

    /** 重置某个槽位的整合度（器官被摘除时调用） */
    public void reset(BodySlot slot) {
        integration.remove(slot);
        pendingGrowth.remove(slot);
    }

    /** 替换单个部件时保留部分整合度 */
    public void retainOnPartSwap(BodySlot slot) {
        int current = get(slot);
        set(slot, current / 2); // 保留 50%
        // 清除 pendingGrowth，重新累积
        pendingGrowth.remove(slot);
    }

    /**
     * 获取整合度折扣系数。
     * 0% 整合度 = 20% 生效，100% 整合度 = 100% 生效。
     */
    public double getEffectiveMultiplier(BodySlot slot) {
        int value = get(slot);
        return MIN_EFFECTIVE_RATIO + value * (1.0 - MIN_EFFECTIVE_RATIO) / MAX_INTEGRATION;
    }

    /** 获取所有已安装机械器官的槽位及其整合度（不可变视图） */
    public Map<BodySlot, Integer> getAll() {
        return Collections.unmodifiableMap(integration);
    }

    /** 是否有任何机械器官正在整合中 */
    public boolean hasAny() {
        return !integration.isEmpty();
    }

    // ========== NBT 序列化 ==========

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        // 保存整数整合度
        for (Map.Entry<BodySlot, Integer> entry : integration.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putInt(TAG_SLOT, entry.getKey().ordinal());
            entryTag.putInt(TAG_VALUE, entry.getValue());
            tag.put("slot_" + entry.getKey().ordinal(), entryTag);
        }
        // 保存浮点累积余数
        for (Map.Entry<BodySlot, Double> entry : pendingGrowth.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putInt(TAG_SLOT, entry.getKey().ordinal());
            entryTag.putDouble(TAG_VALUE, entry.getValue());
            tag.put("pending_" + entry.getKey().ordinal(), entryTag);
        }
        return tag;
    }

    public static MechanicalIntegration load(CompoundTag tag) {
        MechanicalIntegration result = new MechanicalIntegration();
        BodySlot[] slots = BodySlot.values();
        // 加载整数整合度
        for (String key : tag.getAllKeys()) {
            if (!key.startsWith("slot_")) continue;
            CompoundTag entryTag = tag.getCompound(key);
            int slotOrdinal = entryTag.getInt(TAG_SLOT);
            int value = entryTag.getInt(TAG_VALUE);
            if (slotOrdinal >= 0 && slotOrdinal < slots.length) {
                result.set(slots[slotOrdinal], value);
            }
        }
        // 加载浮点累积余数
        for (String key : tag.getAllKeys()) {
            if (!key.startsWith("pending_")) continue;
            CompoundTag entryTag = tag.getCompound(key);
            int slotOrdinal = entryTag.getInt(TAG_SLOT);
            double value = entryTag.getDouble(TAG_VALUE);
            if (slotOrdinal >= 0 && slotOrdinal < slots.length && value > 0) {
                result.pendingGrowth.put(slots[slotOrdinal], value);
            }
        }
        return result;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(MAX_INTEGRATION, value));
    }
}