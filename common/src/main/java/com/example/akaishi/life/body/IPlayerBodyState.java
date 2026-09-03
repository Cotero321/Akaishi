package com.example.akaishi.life.body;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 玩家躯体状态（capability 接口）：
 * 9 个槽位的器官植入状态 + 每部位排斥值。
 * 移植需槽位为空；摘除造成生命值损失（代价模型：装仿生部件前先承受摘除伤害）。
 * 数据经 NBT 持久化到玩家，排斥值由后续基因系统驱动。
 * common 不依赖 Forge，NBT 序列化方法由平台 capability 包装适配。
 */
public interface IPlayerBodyState {

    /** 当前槽位移植的器官（无则为空物品） */
    ItemStack getOrgan(BodySlot slot);

    /** 槽位是否已被占用 */
    boolean isOccupied(BodySlot slot);

    /** 移植器官：槽位必须为空才可移植，成功返回 true */
    boolean implantOrgan(BodySlot slot, ItemStack organ);

    /**
     * 摘除器官：返回被摘除的器官（空物品表示无）。
     * 摘除瞬间对玩家造成无视护甲的生命值损失（创造模式豁免）。
     */
    ItemStack extractOrgan(Player player, BodySlot slot);

    /** 该部位的排斥值（0-100，越高越危险） */
    int getRejection(BodySlot slot);

    void setRejection(BodySlot slot, int value);

    /** 累加排斥值，超出 0-100 自动钳制 */
    void addRejection(BodySlot slot, int amount);

    // ===== 基因强化（永久药剂吸收，最多 GENE_CAPACITY 种不同来源）=====

    /** 已吸收基因强化条目（来源 → 适配加成），插入序稳定 */
    java.util.Map<String, Integer> getGeneBonuses();

    /** 该生物来源是否已被吸收 */
    boolean hasGene(String entityId);

    /** 该生物来源的适配加成（未吸收返回 0） */
    int getGeneBonus(String entityId);

    /** 是否还能吸收新的基因型（< 4） */
    boolean canAddGene();

    /** 吸收一种基因型（重复/超上限返回 false），加成 = 该瓶药剂纯度档位 */
    boolean addGene(String entityId, int bonus);

    /** 卸载一种基因型（未吸收返回 false），卸载后该来源加成即失效、可再次吸收 */
    boolean removeGene(String entityId);

    // ===== 突破强化（突破药剂：30 分钟临时激活，同一时间最多 1 种，结束后可再次激活）=====

    /** 当前是否有突破激活 */
    boolean hasActiveBreakthrough();

    /** 激活中的突破是否属于该生物来源 */
    boolean isBreakthroughActive(String entityId);

    /** 激活中的突破来源生物 id（无激活返回空串） */
    String getBreakthroughEntity();

    /** 激活中的额外适配度加成（2/4/6/8） */
    int getBreakthroughExtra();

    /** 激活中的基础数值强化百分比（10/20/30/40） */
    int getBreakthroughPct();

    /** 激活截止的游戏时刻（无激活返回 -1） */
    long getBreakthroughUntil();

    /**
     * 启动突破：无其它激活才成功（可重复激活同一来源；到期/卸载后即可再次启动）。
     * 写入激活状态并记录截止时刻；到期自动清除（见 tickBreakthrough）。
     */
    boolean startBreakthrough(String entityId, int extra, int pct, long untilGameTime);

    /** 立即结束激活中的突破（到期/卸载基因时调用），返回是否确有激活被结束 */
    boolean endBreakthrough();

    /** 每 tick 调用：激活到期（到达 untilGameTime）自动结束，刚结束返回 true */
    boolean tickBreakthrough(long gameTime);

    /** 持久化为 NBT（玩家存档用） */
    CompoundTag save();

    /** 从 NBT 恢复 */
    void load(CompoundTag tag);
}
