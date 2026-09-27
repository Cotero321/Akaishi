package com.example.akaishi.forge.life.trait;

import com.example.akaishi.item.MechanicalOrganItem;
import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.body.IPlayerBodyState;
import com.example.akaishi.life.body.PlayerBodyHelper;
import com.example.akaishi.life.mechanical.MechanicalLevels;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 特性处理器的公共查询 / 属性挂载工具。
 * <p>
 * 等级口径统一走 {@link MechanicalLevels#traitLevels(ItemStack)}：单器官内按材料出现次数计（1~4），
 * 跨器官按 M2「各器官各自生效」——{@link #levels} 返回每个器官各自的等级，需要"各器官独立回调"
 * 的处理器直接遍历该列表即可。
 */
final class TraitSupport {

    private TraitSupport() {
    }

    /** 玩家全部机械器官中，携带指定特性的「每器官各自等级」（空列表 = 无器官携带）。 */
    static List<Integer> levels(Player player, String traitId) {
        IPlayerBodyState state = PlayerBodyHelper.of(player);
        if (state == null) {
            return List.of();
        }
        List<Integer> out = new ArrayList<>(BodySlot.values().length);
        for (BodySlot slot : BodySlot.values()) {
            ItemStack organ = state.getOrgan(slot);
            if (!(organ.getItem() instanceof MechanicalOrganItem)) {
                continue;
            }
            Integer level = MechanicalLevels.traitLevels(organ).get(traitId);
            if (level != null) {
                out.add(level);
            }
        }
        return out;
    }

    /** 携带指定特性的器官中最高的等级（无则 0）。 */
    static int maxLevel(Player player, String traitId) {
        int max = 0;
        for (int level : levels(player, traitId)) {
            if (level > max) {
                max = level;
            }
        }
        return max;
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
