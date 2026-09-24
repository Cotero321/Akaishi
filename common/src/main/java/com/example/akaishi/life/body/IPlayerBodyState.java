package com.example.akaishi.life.body;

import com.example.akaishi.life.mechanical.MechanicalIntegration;
import com.example.akaishi.sanity.SanityState;
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

    // ===== 机械器官整合度（只涨不降，随躯体持久化）=====

    /** 机械器官整合度数据（每槽位整合值 + 浮点增长余数） */
    MechanicalIntegration getMechanicalIntegration();

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

    // ===== 精神污染（阿盖托洛丝「天魔＊灾」：BOSS 伤害类型改写的玩家侧持久标记）=====

    /**
     * 精神污染窗口的截止游戏刻 —— <b>唯一读法</b>是 {@code AgaitolosPsychic#isConverted}：
     * <ul>
     *   <li>{@code 0} = 从未被污染（也即"BOSS 打你仍是原来的伤害类型"）；</li>
     *   <li>正数 = 30s 改写窗口的截止刻（在此期间 BOSS 的伤害一律改判精神伤害）；</li>
     *   <li>{@link Long#MAX_VALUE} = 窗口已走完，<b>此后永久</b>改写（规格"此技能结束后伤害类型不变"）。</li>
     * </ul>
     * <p><b>为什么落在躯体状态里</b>：它是"玩家侧、跨存档、跨死亡都要留"的标记，
     * 而本 capability 是本项目<b>唯一</b>一条已验证过"落盘 + 死亡快照 + 重生/换维度克隆"的玩家持久化链路
     * （见 forge 侧 {@code PlayerBodyCapability}）。另立一套 capability 只为存一个 long 属于过度设计。
     */
    long getPsychicUntil();

    /** 写入精神污染窗口（0 = 清除；{@code Long.MAX_VALUE} = 转永久） */
    void setPsychicUntil(long untilGameTime);

    // ===== 理智状态（内部数据层 com.example.akaishi.sanity.SanityState）=====

    /**
     * 玩家理智状态（五层数值 + 首见标记 + 各机制运行状态）。
     *
     * <p><b>为什么借住本 capability</b>：与精神污染同一理由——本 capability 是本项目唯一一条
     * 已验证"落盘 + 死亡即时快照 + 重生/换维度克隆"的玩家持久化链路，而理智必须跨存档/跨死亡/跨维度。
     * 另立 capability 只会多出第二套死亡与克隆语义要维护。
     *
     * <p><b>死亡策略的落点</b>：克隆时平台侧对死亡路径调用 {@code SanityState#clearTemporaries()}
     * （临时保护与临时上限削减清除），SAN/SANC/COG 与首见标记随快照整体继承。
     *
     * <p>返回对象永不为 null（capability 挂载即持有），调用方不需要判空。
     */
    SanityState getSanity();

    /** 持久化为 NBT（玩家存档用） */
    CompoundTag save();

    /** 从 NBT 恢复 */
    void load(CompoundTag tag);
}
