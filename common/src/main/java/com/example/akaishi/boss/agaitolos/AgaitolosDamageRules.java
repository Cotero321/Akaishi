package com.example.akaishi.boss.agaitolos;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/**
 * 阿盖托洛丝受击管线的唯一入口（设计文档 §3.1）。
 * <p>
 * 刻意做成<b>无状态纯函数</b>：所有状态（上次受击时间等）由实体持有并传入。
 * 理由：技能里严禁再写第二套减伤/锁伤口径，否则必然出现某招漏套；统一收口后
 * 各技能只负责"如何产生伤害"，不管"伤害如何被 BOSS 吃下"。
 */
public final class AgaitolosDamageRules {

    /** 减伤 60% ⇒ 实际承伤系数 0.4（规格："生命 1444，减伤 60%"） */
    public static final float DAMAGE_MULTIPLIER = 0.4F;

    /** 受击冷却 4 tick = 0.2s（规格："每 0.2s 仅受到一次伤害"） */
    public static final int HURT_COOLDOWN_TICKS = 4;

    /** 锁伤下限 24（规格："伤害限制 24"） */
    public static final float DAMAGE_CAP_MIN = 24.0F;

    /** 锁伤上限比例：最大生命的 5%（§6.4 第 2 条定案；1444 血时上限 = 72.2） */
    public static final float DAMAGE_CAP_HEALTH_RATIO = 0.05F;

    private AgaitolosDamageRules() {
    }

    /**
     * 结算一次受击。<b>所有</b>进入 BOSS 的伤害都必须经此函数。
     *
     * @param gameTime         当前世界游戏刻
     * @param lastHurtGameTime 上次成功受击的游戏刻（未受过击请传远小于 gameTime 的值）
     * @param maxHealth        当前最大生命（多玩家加成抬高后锁伤上限同步抬高）
     * @param respawning       是否处于复活阶段（无敌）；由实体从同步数据读出后传入
     * @param selfDamage       是否为「被玩家反弹回来的凋零头」造成的自伤（规格：无视减伤与锁伤）
     * @param source           伤害来源
     * @param amount           原始伤害量
     * @return 实际生效的伤害量；返回 0 表示本次免伤
     */
    public static float resolve(long gameTime, long lastHurtGameTime, float maxHealth,
                                boolean respawning, boolean selfDamage, DamageSource source, float amount) {
        // ① 非玩家来源免伤（规格："不会受到非玩家的伤害"）。
        // 判据用 getEntity()：玩家射出的箭其 getEntity() 仍是玩家，因此算玩家伤害；陷阱/摔落/其它生物则免伤。
        if (!(source.getEntity() instanceof Player)) {
            return 0.0F;
        }
        // ② 复活阶段免伤（规格："复活阶段 BOSS 不会受到伤害"）。无敌只在这一条口径里判，
        //    严禁另开 isInvulnerableTo（设计文档 §3.1 明令禁止第二套口径）。
        if (respawning) {
            return 0.0F;
        }
        // ③ 0.2s 受击冷却：冷却内伤害记 0 而不是取消事件，保持击退等判定口径统一
        if (gameTime - lastHurtGameTime < HURT_COOLDOWN_TICKS) {
            return 0.0F;
        }
        // ④ 【P6 接入】阶段三免疫远程（届时判 source.is(DamageTypeTags.IS_PROJECTILE)）
        // ⑤ 反弹自伤通道：凋零头被玩家打回 BOSS 时（selfDamage=true）跳过 ⑥⑦，
        //    直接返回原始伤害（规格："BOSS 承受此伤害并且无视自身减伤和锁伤"）。
        //    ③ 的 0.2s 冷却在其之前仍生效：规格只免减伤与锁伤，未免受击冷却。
        if (selfDamage) {
            return amount;
        }
        // ⑥ 减伤 60%
        amount *= DAMAGE_MULTIPLIER;
        // ⑦ 锁伤：上限 = max(24, 最大生命 5%)
        amount = Math.min(amount, Math.max(DAMAGE_CAP_MIN, maxHealth * DAMAGE_CAP_HEALTH_RATIO));
        return amount;
    }
}
