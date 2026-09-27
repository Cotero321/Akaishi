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

    /**
     * 只读取槽内容（终端物品库浏览 / 搬运视图，不得用于直写）。
     * <p>
     * <b>一槽多堆口径</b>：本方法返回的是<b>视图堆</b> —— 只承载「物品 + NBT（身份）」与一个
     * <b>夹到单堆上限</b>的展示数量；槽内真实件数可能远超单堆上限（上限由 IP 容量决定），
     * 必须经 {@link #storedCount(int)} 读取，<b>不得</b>用 {@code getItem(slot).getCount()} 当真实件数。
     * 理由：1.20.1 的 {@code ItemStack} 序列化把数量写成 NBT {@code Count}（byte），且原版槽同步
     * 与管道判余量都按单堆上限语义 ⇒ 大数量只有放在独立字段里才不会截断 / 误判。
     */
    ItemStack getItem(int slot);

    /**
     * 单槽<b>真实件数</b>（可远超 {@code getItem(slot).getCount()} 的单堆上限）。
     * <p>
     * 与 {@link #getSlotIp(int)} 一样是单元数据的权威读数：聚合统计、按比例扣账、
     * 搬运夹量一律用它，禁止用视图堆的数量代替。
     * <p>
     * 默认实现退回视图堆数量（外部实现无多堆语义时的保守兜底）。
     */
    default long storedCount(int slot) {
        return getItem(slot).getCount();
    }

    /**
     * 单槽已占用 IP（账本值）。
     * <p>
     * 取出费用必须以<b>账本值</b>而非当前价值表重算 —— 与 {@link #extract} 实际扣减额严格一致，
     * 价值表热重载后不漂移（D10 入账锁定）。
     */
    long getSlotIp(int slot);

    /**
     * 当前还能再存入多少件（容量 + 槽位约束取小），供终端预检与单笔上限计算。
     * <p>
     * 一槽多堆后槽位侧几乎不再是瓶颈（有空槽 / 已有条目即不限），因此本值主要由<b>IP 余量</b>决定。
     *
     * @return 可再存入的件数；{@link #insert} 以此为原子边界
     */
    int acceptable(ItemStack stack);

    /**
     * <b>槽位侧</b>是否还能容纳该物品（<b>不含</b> IP 容量约束）。
     * <p>
     * <b>一槽多堆语义（本轮改定）</b>：一个槽不再只放一堆，件数上限由 IP 容量决定 ⇒ 槽位侧只在
     * 「<b>没有空槽</b>且该 {@code (物品, NBT)} 在库中<b>没有已有条目</b>」时才真正卡死。
     * 因此返回值只有两种：
     * <ul>
     *   <li>{@link Long#MAX_VALUE} —— 有空槽，或该物品（同 NBT）已有条目可并入（件数不限，只受 IP 约束）；</li>
     *   <li>{@code 0} —— 既无空槽、也没有该物品的条目 ⇒ <b>需要新种类槽位</b>但已无槽位。</li>
     * </ul>
     * <p>
     * 为什么保留「返回数量」的形状而不是改布尔：{@link #acceptable} 仍是
     * {@code min(slotRoom, ipRoom)} 的取小结构，失败细分（{@code REJECT_DEPOSIT_SLOTS_FULL}）也仍按
     * {@code slotRoom == 0} 判定；沿用原形可让调用方零改动。注意：本值此后<b>不可跨单元相加</b>
     * （{@code MAX_VALUE} 会溢出），聚合见 {@code TerminalSpaceCheck#slotRoom}。
     *
     * @return {@link Long#MAX_VALUE} = 槽位侧不限（受 IP 约束）；{@code 0} = 无槽位可放该新种类
     */
    default long slotRoom(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0L;
        }
        int slots = slots();
        for (int i = 0; i < slots; i++) {
            ItemStack cur = getItem(i);
            if (cur.isEmpty() || ItemStack.isSameItemSameTags(cur, stack)) {
                return Long.MAX_VALUE;
            }
        }
        return 0L;
    }

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
