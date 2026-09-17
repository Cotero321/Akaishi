package com.example.akaishi.forge.life;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * 「禁忌」饰品的属性修饰管理：非 buff 增益一律走 AttributeModifier（无 HUD 图标，D32）。
 *
 * <p>两类生命周期：
 * <ul>
 *   <li><b>常驻型</b> {@link #cache}：由佩戴/生效条件自身驱动，值无变化直接跳过，避免每 tick 触发属性重算；</li>
 *   <li><b>限时型</b> {@link #timed}：登记到期 tick，由实体 tick 调 {@link #tickTimed} 精确回收。</li>
 * </ul>
 *
 * <p>两条铁律：同 UUID 重复 {@code addTransientModifier} 会抛 "Modifier is already applied"，
 * 故一律「先移后挂」；回收必须按 UUID 精确移除，否则属性残留。</p>
 */
final class AkaishiForbiddenAttrs {

    /** 常驻型登记：实体 → (UUID → 当前值) */
    private static final Map<LivingEntity, Map<UUID, Double>> CACHED = new WeakHashMap<>();
    /** 限时型登记：实体 → (UUID → 到期信息) */
    private static final Map<LivingEntity, Map<UUID, Timed>> TIMED = new WeakHashMap<>();

    private record Timed(UUID uuid, Attribute attribute, long expireTick) {
    }

    private AkaishiForbiddenAttrs() {
    }

    /**
     * 常驻型：目标值无变化则跳过；变化时先移后挂（amount 为 0 只移除）。
     *
     * <p>移速／攻速等基础值属性必须传 {@code MULTIPLY_BASE}（+10% 即 0.10），
     * 传 {@code ADDITION} 会被当作绝对值加到基础值上（+0.10 → +100%）。</p>
     */
    static void cache(LivingEntity entity, UUID uuid, Attribute attribute, double amount,
                      AttributeModifier.Operation operation) {
        Map<UUID, Double> applied = CACHED.computeIfAbsent(entity, e -> new HashMap<>(4));
        Double last = applied.get(uuid);
        if (last != null && Math.abs(last - amount) < 1e-9) {
            return;
        }
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            if (last != null) {
                instance.removeModifier(uuid);
            }
            if (amount != 0.0) {
                instance.addTransientModifier(new AttributeModifier(uuid,
                        "Akaishi forbidden", amount, operation));
            }
        }
        applied.put(uuid, amount);
    }

    /** 限时型：挂载并登记到期 tick（重复触发即刷新剩余时长） */
    static void timed(LivingEntity entity, UUID uuid, Attribute attribute, double amount,
                      AttributeModifier.Operation operation, long expireTick) {
        Map<UUID, Timed> applied = TIMED.computeIfAbsent(entity, e -> new HashMap<>(4));
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(uuid); // 先移后挂
            instance.addTransientModifier(new AttributeModifier(uuid, "Akaishi forbidden", amount, operation));
        }
        applied.put(uuid, new Timed(uuid, attribute, expireTick));
    }

    /** 实体 tick 调用：回收已到期的限时修饰符（无登记时开销仅一次空表判断） */
    static void tickTimed(LivingEntity entity, long gameTime) {
        if (TIMED.isEmpty()) {
            return;
        }
        Map<UUID, Timed> applied = TIMED.get(entity);
        if (applied == null) {
            return;
        }
        applied.values().removeIf(timed -> {
            if (timed.expireTick() > gameTime) {
                return false;
            }
            AttributeInstance instance = entity.getAttribute(timed.attribute());
            if (instance != null) {
                instance.removeModifier(timed.uuid());
            }
            return true;
        });
        if (applied.isEmpty()) {
            TIMED.remove(entity);
        }
    }
}
