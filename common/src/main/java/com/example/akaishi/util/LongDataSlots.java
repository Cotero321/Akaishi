package com.example.akaishi.util;

import net.minecraft.world.inventory.ContainerData;

/**
 * long 值的菜单数据槽同步辅助。
 * <p>
 * {@link net.minecraft.world.inventory.SimpleContainerData} 的槽位只能是 int，
 * 直接 {@code (int) longValue} 会截断并溢出（能量/容量可被配置与能量升级放大到超过 Integer.MAX_VALUE）。
 * 约定：一个 long 占两个槽——低 32 位写入 {@code lowIndex}，高 32 位写入 {@code highIndex}。
 * 两个下标可以相邻（lowIndex+1），也可以把高槽统一追加到数组末尾以保持已有字段下标不变。
 */
public final class LongDataSlots {

    private LongDataSlots() {}

    /** 写入 long：低 32 位 → lowIndex，高 32 位 → highIndex */
    public static void write(ContainerData data, int lowIndex, int highIndex, long value) {
        data.set(lowIndex, (int) value);
        data.set(highIndex, (int) (value >>> 32));
    }

    /** 读取 long：由 lowIndex / highIndex 两槽重组 */
    public static long read(ContainerData data, int lowIndex, int highIndex) {
        return ((long) data.get(highIndex) << 32) | (data.get(lowIndex) & 0xFFFFFFFFL);
    }
}
