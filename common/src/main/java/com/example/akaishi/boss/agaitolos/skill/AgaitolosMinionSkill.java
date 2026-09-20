package com.example.akaishi.boss.agaitolos.skill;

import com.example.akaishi.boss.agaitolos.AgaitolosEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 一阶段技能「恶怨倒转」的<b>召唤面</b>（设计文档 §0 一阶段 / §1 第 22 条）。
 * <p>
 * 规格：生成凋零骷髅分队（5 近战 5 远程，拥有增强过的属性和装备）；BOSS 在此期间为蓄力模式。
 * <p>
 * 本类只负责"把分队铺出来"与"把它们认回来"两件事：
 * <ul>
 *   <li>{@link #summon(AgaitolosEntity)} —— 生成整支分队；</li>
 *   <li>{@link #findLivingMinions(AgaitolosEntity, double)} —— 按归属标记筛出<b>本 BOSS 召唤的</b>存活成员
 *       （本轮先建好这个能力，供下一轮"吸血结算只吸自己召唤的、不吸自然生成的凋零骷髅"使用）。</li>
 * </ul>
 * 蓄力状态、时长与取消条件归实体自己持有（见 {@code AgaitolosEntity#tickCharge}），本类不碰状态计时。
 * <p>
 * <b>为什么"不攻击 BOSS / BOSS 不误伤它们"不需要额外代码</b>：
 * <ol>
 *   <li>BOSS → 召唤物：BOSS 的三条输出路径都只针对玩家 —— 近战经 {@code MeleeAttackGoal} 打
 *       {@code getTarget()}，而目标由 {@code NearestAttackableTargetGoal<Player>} 唯一选定；
 *       俯冲镰扫的筛选是 {@code getEntitiesOfClass(Player.class, ...)}；凋零头的 {@code owner} 是 BOSS、
 *       发射方向朝玩家。故 BOSS 不会把召唤物当目标，也没有 AoE 会把它们卷进去（见下方"已知缺口"）。</li>
 *   <li>召唤物 → BOSS：凋零骷髅的目标选择器只有 {@code HurtByTargetGoal}（仅反击"打过自己的"）+
 *       {@code NearestAttackableTargetGoal}(Player / IronGolem / Turtle / AbstractPiglin)。
 *       BOSS 既不打它们、也不是以上任何一类，二者天然不交叉。</li>
 * </ol>
 * <b>已知缺口（本轮不改，需后续注意）</b>：{@code AgaitolosWitherSkull} 的 {@code onHitEntity} 结算的是
 * "任何被碰撞到的 LivingEntity"（弹体共有逻辑，原版 {@code Projectile#canHitEntity} 也没有友军过滤），
 * 因此当召唤物恰好挡在 BOSS 与玩家之间的弹道上时，会被自己的弹体擦中。
 * <b>⚠ 将来若给 BOSS 加任何"误伤召唤物"的招（环形 AoE、牵引、自爆等），必须回到这里加闸</b>。
 */
public final class AgaitolosMinionSkill {

    // ---------------------------------------------------------------- 手感常量（待调手感值 / P8 转配置项）

    /** 近战召唤数：5（规格明确）。待调手感值 / P8 转配置项 */
    public static final int MELEE_COUNT = 5;

    /** 远程召唤数：5（规格明确）。待调手感值 / P8 转配置项 */
    public static final int RANGED_COUNT = 5;

    /**
     * 近战召唤环半径（格）：4.0。
     * <p>取值依据：① 与 BOSS 自身横扫半径（{@code AgaitolosDiveSweepSkill.SWEEP_RADIUS} = 4.0）同量级，
     * 召唤物不会穿进 BOSS 模型里；② 5 只均分圆周时相邻弧长 = 2π×4/5 ≈ 5.0 格，
     * 远大于凋零骷髅 0.7 的碰撞宽度，落位不互相挤压。待调手感值 / P8 转配置项
     */
    public static final double RING_RADIUS = 4.0D;

    /**
     * 远程召唤环半径（格）：6.0 —— 与近战环径向差 2 格，形成前后两层（"前后错开"）。
     * <p>外环在 {@link #summon(AgaitolosEntity)} 里再额外转半个角步，故 10 只中任意两只都不会叠在同一落点。
     * 待调手感值 / P8 转配置项
     */
    public static final double RANGED_RING_RADIUS = 6.0D;

    /**
     * 召唤点相对 BOSS 脚部的竖直偏移（格）：0，即直接取 BOSS 当前高度。
     * <p>不做贴地探测的理由：凋零骷髅是地面生物，而本 BOSS 常态悬停高度只有
     * {@code AgaitolosMoveControl.HOVER_HEIGHT}（2 格），低于原版 3 格摔落伤害阈值，
     * 落体自然着地且不吃摔落伤害。待调手感值 / P8 转配置项
     */
    public static final double SPAWN_VERTICAL_OFFSET = 0.0D;

    /** 最大生命加成：+20（原版凋零骷髅 20 ⇒ 40）。待调手感值 / P8 转配置项 */
    public static final double BONUS_MAX_HEALTH = 20.0D;

    /** 攻击力加成：+4（原版凋零骷髅 4.0 ⇒ 8.0，约等于铁剑一击）。待调手感值 / P8 转配置项 */
    public static final double BONUS_ATTACK_DAMAGE = 4.0D;

    /** 移动速度加成：+0.05（原版 0.25 ⇒ 0.30，追击速度 +20%）。待调手感值 / P8 转配置项 */
    public static final double BONUS_MOVEMENT_SPEED = 0.05D;

    /**
     * 「召唤物是否全部阵亡」的搜索半径（格）：64。
     * <p>刻意<b>不</b>用 20（下一轮吸血结算的规格半径）：召唤物会追着玩家跑出很远，
     * 若沿用 20 格，追出去的召唤物会被误判成"已阵亡"、蓄力被提前打断。
     * 64 也 ≥ 召唤物的实际追击上限（其 FOLLOW_RANGE 远小于此），不会漏判。待调手感值 / P8 转配置项
     */
    public static final double ALIVE_CHECK_RADIUS = 64.0D;

    /**
     * 归属标记的前缀（完整标记 = {@code "AgaitolosMinionOwner:<召唤者UUID>"}），写在<b>凋零骷髅自己的
     * 原版实体标签</b>上（{@code Entity#addTag(String)} / {@code Entity#getTags()}）。
     * <p><b>为什么不用 Forge 的 {@code Entity#getPersistentData()}</b>：那是 Forge 补丁进 {@code Entity} 的
     * API，实测只在 forge 侧 jar 里存在；本类位于 {@code common}（Architectury 的 common 编译面 = 原版 +
     * Architectury API），引用它会直接编译失败。原版实体标签是两端都有的等价物，且同样会落盘
     * （{@code Entity#saveWithoutId} 写 {@code "Tags"}、{@code Entity#load} 读回），
     * 故区块卸载/读档后归属不丢。
     * <p><b>为什么用标记而不是新建 EntityType</b>：新建一整套自定义实体意味着实体注册 + 属性 + 渲染器 +
     * 模型贴图 + 语言键等一长串落地项；而本招要的只是"给原版凋零骷髅换个归属"，直接用原版
     * {@link WitherSkeleton} 就能白拿它的 AI / 掉落 / 渲染 / 音效，再打一个标记即可满足
     * "吸血只吸自己召唤的、不吸自然生成的凋零骷髅"。
     * <p>⚠ 已知副作用：实体标签是原版 {@code /tag} 命令的操作对象，有 OP 权限的玩家理论上能给自然生成的
     * 凋零骷髅补一条同样的标记，把它伪装成召唤物。本轮只影响"是否算本 BOSS 分队"，不产生额外收益，
     * 故不额外设防。
     */
    public static final String OWNER_TAG_PREFIX = "AgaitolosMinionOwner";

    /**
     * 原版凋零骷髅的攻击力基础值：4.0。
     * <p>该值由 {@code WitherSkeleton#finalizeSpawn} 补设，而本招用 {@code new WitherSkeleton(...)}
     * 直接生成、<b>不走</b> finalizeSpawn，故必须自己把基础值补上，否则召唤物的基础攻击力只有 2.0
     * （{@code Monster.createMonsterAttributes} 的默认值），比自然生成的同类还弱。
     */
    private static final double VANILLA_ATTACK_DAMAGE = 4.0D;

    // 属性修饰符 UUID：固定值即可——transient 修饰符挂在"每个实体自己的 AttributeInstance"上，
    // 不同召唤物之间不共享实例，故不存在 UUID 碰撞。（同一实例上重复添加同 UUID 才会抛异常）
    private static final UUID UUID_MAX_HEALTH = UUID.fromString("5a7e6c1e-1f3b-4d2a-9c60-7b1a2f4d8e01");
    private static final UUID UUID_ATTACK_DAMAGE = UUID.fromString("5a7e6c1e-1f3b-4d2a-9c60-7b1a2f4d8e02");
    private static final UUID UUID_MOVEMENT_SPEED = UUID.fromString("5a7e6c1e-1f3b-4d2a-9c60-7b1a2f4d8e03");

    private AgaitolosMinionSkill() {
    }

    // ---------------------------------------------------------------- 召唤

    /**
     * 铺开整支凋零骷髅分队（仅服务端；由 {@code AgaitolosEntity#tickCharge} 起手时调用）。
     * <p>
     * 阵型：近战占内环、远程占外环，外环再错开半个角步，使 10 只形成"两层交错"的包围圈
     * （即规格要的"前后错开，避免叠在一起"）。
     * <p>所有召唤物共享 BOSS 当前目标，避免小怪到场后站在原地发愣。
     */
    public static void summon(AgaitolosEntity boss) {
        if (boss.level().isClientSide()) {
            // 仅服务端生成：两端各自生成会刷出双份，且客户端实体随即被服务端同步覆盖
            return;
        }
        Level level = boss.level();
        String ownerId = boss.getUUID().toString();
        LivingEntity sharedTarget = boss.getTarget();
        summonRing(boss, level, ownerId, sharedTarget, MELEE_COUNT, RING_RADIUS, 0.0D, false);
        summonRing(boss, level, ownerId, sharedTarget, RANGED_COUNT, RANGED_RING_RADIUS,
                Math.PI / RANGED_COUNT, true);
    }

    /**
     * 在一条环上均分生成 {@code count} 只凋零骷髅。
     *
     * @param angleOffset 起始角（弧度）：外环传半个角步，即可与内环交叉排布
     * @param ranged      true = 主手给弓（自动切远程 Goal），false = 主手给剑
     */
    private static void summonRing(AgaitolosEntity boss, Level level, String ownerId, LivingEntity sharedTarget,
                                   int count, double radius, double angleOffset, boolean ranged) {
        for (int i = 0; i < count; ++i) {
            double angle = angleOffset + Math.PI * 2.0D * i / count;
            double x = boss.getX() + Math.cos(angle) * radius;
            double z = boss.getZ() + Math.sin(angle) * radius;
            // 朝向圆心（BOSS）：新构造的实体 yaw 默认为 0（全部朝南），不设的话 10 只整齐朝同一方向很出戏
            float yaw = (float) Math.toDegrees(Mth.atan2(-Math.sin(angle), -Math.cos(angle))) - 90.0F;

            WitherSkeleton minion = new WitherSkeleton(EntityType.WITHER_SKELETON, level);
            minion.moveTo(x, boss.getY() + SPAWN_VERTICAL_OFFSET, z, yaw, 0.0F);
            minion.setYHeadRot(yaw);
            equip(minion, ranged);
            buff(minion);
            markOwner(minion, ownerId);
            // 召唤物不得自然清除：否则分队会在玩家视线外悄悄消失，蓄力变成"招完就散"
            minion.setPersistenceRequired();
            if (sharedTarget != null && sharedTarget.isAlive()) {
                minion.setTarget(sharedTarget);
            }
            // 最后才入世界：装备/属性/标记全部就位后再生成，客户端首帧看到的就是成品
            level.addFreshEntity(minion);
        }
    }

    /**
     * 给召唤物上装备。
     * <p><b>远程那 5 只"只要主手给弓"就够了</b> —— 这是 1.20.1 源码实测出的原版链路，无需手动补 Goal：
     * <pre>
     *   AbstractSkeleton#setItemSlot(slot, stack):
     *       super.setItemSlot(slot, stack);
     *       if (!this.level().isClientSide) { this.reassessWeaponGoal(); }   // ← 触发点在这里
     *
     *   AbstractSkeleton#reassessWeaponGoal():
     *       goalSelector.removeGoal(meleeGoal);
     *       goalSelector.removeGoal(bowGoal);
     *       itemstack = getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, it -> it instanceof BowItem));
     *       if (itemstack.is(Items.BOW)) { bowGoal.setMinAttackInterval(...); addGoal(4, bowGoal); }
     *       else                        { addGoal(4, meleeGoal); }
     * </pre>
     * 即：<b>给骷髅主手放弓 → {@code setItemSlot} 覆写 → {@code reassessWeaponGoal} → 换上
     * {@code RangedBowAttackGoal}</b>。弓是无限箭（{@code AbstractSkeleton#performRangedAttack} 走
     * {@code getProjectile} → 缺箭时补 {@code Items.ARROW}），故不需要给弹药。
     * 近战那只同样必须走 {@code setItemSlot}：主手不是弓 ⇒ 落到 {@code meleeGoal} 分支，正好是近战。
     * <p>护甲（头盔 + 胸甲）的两个理由：① 规格要求"装备增强过"；
     * ② {@code AbstractSkeleton#aiStep} 白天会给自己点火，而"头盔格非空"即可免灼烧 ——
     * 本 BOSS 按设计在下界作战，但顺手把这条原版坑堵掉，免得日后场地改到主世界时召唤物集体自燃。
     * <p>近战选 {@code IRON_SWORD} 的理由：原版凋零骷髅默认石剑
     * （{@code WitherSkeleton#populateDefaultEquipmentSlots}），铁剑刚好高一级，是"增强过"的最低成本表达；
     * 不用钻石/下界合金，避免 5 只小怪的输出盖过 BOSS 本体。
     */
    private static void equip(WitherSkeleton minion, boolean ranged) {
        minion.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ranged ? Items.BOW : Items.IRON_SWORD));
        minion.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        minion.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
    }

    /**
     * 属性增强。三处都用 {@code Operation.ADDITION}，故上面三个 BONUS 常量的语义就是
     * "<b>相对原版凋零骷髅的加成量</b>"。
     * <p><b>⚠ 已知副作用</b>：{@code AttributeInstance#save()} 只序列化 {@code permanentModifiers}，
     * 因此 {@code addTransientModifier} 的加成<b>不会</b>写进存档 —— 区块卸载重载或重启后，
     * 召唤物会退回"原版基础值 + 我们的装备"。若将来要跨存档保住加成，把这里换成
     * {@code addPermanentModifier} 即可（原版 {@code Mob#finalizeSpawn} 的刷怪加成正是这么做的）。
     */
    private static void buff(WitherSkeleton minion) {
        AttributeInstance attack = minion.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(VANILLA_ATTACK_DAMAGE);
        }
        applyBonus(minion, Attributes.MAX_HEALTH, UUID_MAX_HEALTH, "Agaitolos minion health", BONUS_MAX_HEALTH);
        applyBonus(minion, Attributes.ATTACK_DAMAGE, UUID_ATTACK_DAMAGE, "Agaitolos minion damage",
                BONUS_ATTACK_DAMAGE);
        applyBonus(minion, Attributes.MOVEMENT_SPEED, UUID_MOVEMENT_SPEED, "Agaitolos minion speed",
                BONUS_MOVEMENT_SPEED);
        // 抬过 MAX_HEALTH 之后必须补满，否则召唤物是"上限 40 却只有 20 血"的半血出场
        minion.setHealth(minion.getMaxHealth());
    }

    /** 单个属性的加成；属性缺失时静默跳过，不让一条缺失拖垮整次召唤 */
    private static void applyBonus(WitherSkeleton minion, Attribute attribute, UUID id, String name, double amount) {
        AttributeInstance instance = minion.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
    }

    /** 打归属标记：完整标记 = 前缀 + ":" + 召唤者 BOSS 的 UUID，见 {@link #OWNER_TAG_PREFIX} */
    private static void markOwner(WitherSkeleton minion, String ownerId) {
        minion.addTag(ownerTag(ownerId));
    }

    /** 归属标记的完整字符串。用 UUID 区分召唤者：场上若同时存在多个 BOSS，各自的结算只认自己的分队 */
    private static String ownerTag(String ownerId) {
        return OWNER_TAG_PREFIX + ":" + ownerId;
    }

    // ---------------------------------------------------------------- 归属查询

    /**
     * 筛出<b>本 BOSS 召唤的、且存活</b>的分队成员（仅服务端会返回结果）。
     * <p>
     * 本轮只有一个调用点：{@code AgaitolosEntity#tickCharge} 用它判"小怪全部阵亡即取消蓄力"。
     * <b>下一轮的吸血结算会复用本方法</b>（规格："吸取周围小怪剩余血量"）——
     * 归属标记正是为了让那次结算只吸自己召唤的、不吸自然生成的凋零骷髅。
     * <p>筛选口径：包围盒粗筛 → 球半径二次筛 → 归属标记相等。BOSS 自身被显式排除。
     *
     * @param radius 搜索半径（格）；判"是否全灭"请用 {@link #ALIVE_CHECK_RADIUS}，
     *               下一轮吸血结算按规格用 20
     */
    public static List<LivingEntity> findLivingMinions(AgaitolosEntity boss, double radius) {
        List<LivingEntity> minions = new ArrayList<>();
        if (boss.level().isClientSide()) {
            return minions;
        }
        String ownerTag = ownerTag(boss.getUUID().toString());
        double radiusSqr = radius * radius;
        for (LivingEntity candidate : boss.level().getEntitiesOfClass(LivingEntity.class,
                boss.getBoundingBox().inflate(radius))) {
            // 包围盒是方盒，按球半径再筛一遍；BOSS 自己不是召唤物
            if (candidate == boss || !candidate.isAlive() || candidate.distanceToSqr(boss) > radiusSqr) {
                continue;
            }
            // 自然生成的凋零骷髅没有这条标记，自然落选
            if (candidate.getTags().contains(ownerTag)) {
                minions.add(candidate);
            }
        }
        return minions;
    }
}
