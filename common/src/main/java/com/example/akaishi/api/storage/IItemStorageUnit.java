package com.example.akaishi.api.storage;

import net.minecraft.world.item.ItemStack;

/**
 * 物品储存单元契约：物品终端外侧贴装（1 格）聚合 IP 容量与占用量的抽象。
 * <p>
 * 终端只经本接口读写（DIP）：聚合统计用只读方法，物品进出用三写入口，
 * 实现方（各阶储存单元）在写入口内同步维护「物品本体 + slotIp 账本」，
 * 因此终端无需、也不得触碰容器本体，从接口面排除绕过账本的可能（D9 / D10 无偏差）。
 */
public interface IItemStorageUnit {

    /** 单元 IP 容量（随等阶恒定） */
    long getIpCapacity();

    /** 单元当前已占用 IP（恒等于账本求和） */
    long getStoredIp();

    /** 单元槽位数（终端按页聚合展示用） */
    int slots();

    /** 只读取槽内容（终端物品库浏览用，不得用于直写） */
    ItemStack getItem(int slot);

    /**
     * 单槽已占用 IP（账本值）。
     * <p>
     * 取出费用必须以<b>账本值</b>而非当前价值表重算 —— 与 {@link #extract} 实际扣减额严格一致，
     * 价值表热重载后不漂移（D10 入账锁定）。
     */
    long getSlotIp(int slot);

    /**
     * 当前还能再存入多少件（容量 + 空位双约束），供终端预检与单笔上限计算。
     *
     * @return 可再存入的件数；{@link #insert} 以此为原子边界
     */
    int acceptable(ItemStack stack);

    /**
     * 存入整堆：容量或空位不足时<b>整笔拒绝</b>，不做部分存入（保证调用方按批结算费用不落空）。
     *
     * @return 实际存入数量（0 表示未改动）
     */
    int insert(ItemStack stack);

    /**
     * 取出：原物取回（含 NBT，D9），账本按比例扣减。
     *
     * @param slot   单元槽位下标
     * @param amount 期望取出件数
     * @return 取出的堆（空表示无货）
     */
    ItemStack extract(int slot, int amount);
}
