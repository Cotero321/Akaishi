package com.example.akaishi.forge.life.trait;

import com.example.akaishi.forge.life.MechanicalAggregation;
import com.example.akaishi.life.body.IPlayerBodyState;
import com.example.akaishi.life.body.PlayerBodyHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 特性处理器的公共查询 / 属性挂载工具。
 * <p>
 * 等级口径统一走 {@link MechanicalAggregation}：每个特性在玩家全身取<b>唯一有效等级</b>
 * （单器官计数 clamp 1~maxLevel，跨器官取最大值），处理器只需读取该等级，不再自行按器官累加。
 */
final class TraitSupport {

    private TraitSupport() {
    }

    /** 指定特性在玩家全身汇总后的「有效等级」（无则 0）。 */
    static int effectiveLevel(Player player, String traitId) {
        IPlayerBodyState state = PlayerBodyHelper.of(player);
        if (state == null) {
            return 0;
        }
        Integer level = MechanicalAggregation.traitLevels(MechanicalAggregation.organs(state)).get(traitId);
        return level == null ? 0 : level;
    }

    /**
     * 设置按「特性 ID」固定的瞬时属性修饰符（{@code amount == 0} 时移除）。
     * <p>
     * 采用瞬时修饰符（{@code addTransientModifier}）——不写入玩家 NBT，避免存档膨胀；
     * 每 tick 由特性处理器重算，摘除义体后由 {@code onUnequip} 清除。
     */
    static void setModifier(LivingEntity entity, Attribute attribute, String traitId,
                            double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        UUID id = modifierId(traitId);
        AttributeModifier existing = instance.getModifier(id);
        if (amount == 0) {
            if (existing != null) {
                instance.removeModifier(id);
            }
            return;
        }
        if (existing != null) {
            if (existing.getAmount() == amount) {
                return;
            }
            instance.removeModifier(id);
        }
        instance.addTransientModifier(new AttributeModifier(id, "akaishi_mech_trait_" + traitId, amount, operation));
    }

    private static UUID modifierId(String traitId) {
        return UUID.nameUUIDFromBytes(("akaishi:mech_trait:" + traitId).getBytes(StandardCharsets.UTF_8));
    }
}
