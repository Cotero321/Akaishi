package com.example.akaishi.boss.agaitolos;

import com.example.akaishi.AkaishiMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * 阿盖托洛丝【下界本源】的<b>输出</b>伤害源工厂（BOSS 打人）。
 * <p>
 * 与 {@link AgaitolosDamageRules} 方向相反，故刻意分成两个类：
 * 后者管 BOSS「挨打」的承伤口径（冷却 / 免伤 / 复活无敌），本类只负责 BOSS「打人」时
 * 构造自定义伤害源。两者混在一起会让"谁是攻方、谁是守方"彻底不可读，也违反单一职责。
 */
public final class AgaitolosCombat {

    /**
     * 自定义伤害类型键：真实伤害（数据层见 {@code data/akaishi/damage_type/true_damage.json}）。
     * <p>
     * 标签口径（{@code data/minecraft/tags/damage_type/}）：
     * <ul>
     *   <li>{@code bypasses_armor} —— 无视护甲</li>
     *   <li>{@code bypasses_resistance} —— 无视抗性提升</li>
     *   <li>{@code bypasses_enchantments} —— 无视保护类附魔</li>
     *   <li>{@code bypasses_cooldown} —— <b>必须</b>。普攻是"物理一击 + 紧接的真实伤害"两段，
     *       物理那一下会把目标的 {@code invulnerableTime} 置为 20；原版 {@code LivingEntity#hurt}
     *       在 {@code invulnerableTime > 10} 且伤害源不在该标签内时走"只结算增量伤害"分支
     *       （{@code amount <= lastHurt} 直接返回 0），导致 4 点真实伤害整段被吞掉。
     *       打上该标签后才会走完整结算分支。</li>
     * </ul>
     */
    public static final ResourceKey<DamageType> TRUE_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation(AkaishiMod.MOD_ID, "true_damage"));

    private AgaitolosCombat() {
    }

    /**
     * 构造真实伤害源（伤害类型注册于数据包，须运行期从注册表取 holder）。
     * <p>客户端 / 非 ServerLevel 环境拿不到注册表，返回 {@code generic()} 兜底，避免 NPE。
     */
    public static DamageSource trueDamage(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            Registry<DamageType> registry = serverLevel.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
            return new DamageSource(registry.getHolderOrThrow(TRUE_DAMAGE));
        }
        return level.damageSources().generic();
    }

    /**
     * 构造<b>携带因果实体</b>的真实伤害源（弹体施加的真实伤害段走这条）。
     * <p>
     * 两个理由必须带 {@code causingEntity}：
     * <ol>
     *   <li>击杀归属与 {@code death.attack.akaishi.true_damage.player} 死亡消息都取自
     *       {@code source.getEntity()}，不带则永远只走无攻击者那条消息键；</li>
     *   <li>BOSS 受击管线第 ① 步按 {@code getEntity()} 判「非玩家来源免伤」，
     *       反弹回来的真实伤害段若不带 causer，会被整段挡掉。</li>
     * </ol>
     *
     * @param directEntity  直接造成伤害的实体（弹体；普攻等无弹体场景传 {@code null}）
     * @param causingEntity 伤害归属实体（施加者）
     */
    public static DamageSource trueDamage(Level level, Entity directEntity, Entity causingEntity) {
        if (level instanceof ServerLevel serverLevel) {
            Registry<DamageType> registry = serverLevel.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
            return new DamageSource(registry.getHolderOrThrow(TRUE_DAMAGE), directEntity, causingEntity);
        }
        return level.damageSources().generic();
    }
}
