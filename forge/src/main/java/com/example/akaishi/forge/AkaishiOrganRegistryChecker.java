package com.example.akaishi.forge;

import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.organ.AttributeWeightRegistry;
import com.example.akaishi.life.organ.MutantTrait;
import com.example.akaishi.life.organ.OrganEffect;
import com.example.akaishi.life.organ.OrganEffectRegistry;
import com.example.akaishi.life.organ.OrganRegistry;
import com.example.akaishi.life.organ.OrganTemplate;
import com.example.akaishi.life.sample.SampleGroup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 器官登记入库核对器（dev 环境 -Dakaishi.gametest=1 时于服务端启动后同步执行）：
 * 遍历 OrganEffectRegistry.registeredSources()，逐一断言——entityId 有效、为活体实体、
 * SampleGroup.of 能识别出采集分组（孤儿器官兜底，与 AkaishiLifeSystemTests 同规则，
 * 但后者需原版 GameTest 框架/结构，此核对器仅凭注册表 + 注册 id 即出结论，脚本可直接 grep 断言）。
 * 另核对「属性↔槽位合法性矩阵」（真源 {@link OrganRegistry#allows}）：槽位模板、生物效果与突变词条的属性
 * 均不得落在非法槽位（正负值同受约束，惩罚代价也无豁免）；并核对槽位轴亲和表，被放大的轴必须是该槽位合法轴。
 */
public final class AkaishiOrganRegistryChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(AkaishiOrganRegistryChecker.class);

    private AkaishiOrganRegistryChecker() {
    }

    /** 同步核对全部已注册器官来源是否可采集（无 tick 依赖，全部 PASS 返回 0） */
    public static int run(MinecraftServer server) {
        ServerLevel level = server.overworld();
        if (level == null) {
            LOGGER.error("[AkaishiReg] 主世界未就绪，器官入库核对跳过");
            return -1;
        }
        int total = 0;
        int passed = 0;
        for (String entityId : OrganEffectRegistry.registeredSources()) {
            total++;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.tryParse(entityId));
            if (type == null) {
                LOGGER.error("[AkaishiReg] [FAIL] 死链：「{}」不是有效实体 ID", entityId);
                continue;
            }
            Entity raw = type.create(level);
            if (!(raw instanceof LivingEntity living)) {
                LOGGER.error("[AkaishiReg] [FAIL] 死链：「{}」不是活体实体", entityId);
                continue;
            }
            if (SampleGroup.of(living) == null) {
                LOGGER.error("[AkaishiReg] [FAIL] 孤儿：「{}」无法被任何样本分组采集", entityId);
                continue;
            }
            passed++;
        }
        int ok = passed == total ? 1 : 0;
        LOGGER.info("[AkaishiReg] 器官入库核对：{} / {} 来源可采集（注册 {} 生物 × {} 条目）{}",
                passed, total,
                OrganEffectRegistry.entityCount(), OrganEffectRegistry.entryCount(),
                ok == 1 ? "[PASS] 全部入库，无孤儿" : "[存在死注册/孤儿]");

        int violations = checkMatrix() + checkTraitSlots() + checkSlotAffinity();
        LOGGER.info("[AkaishiReg] 属性↔槽位矩阵核对：{} {}",
                violations == 0 ? "[PASS] 无属性错位" : "[" + violations + " 处属性错位]",
                violations == 0 ? "" : "（正属性落在非法槽位，详见上方 FAIL 行）");
        return ok == 1 && violations == 0 ? 1 : 0;
    }

    /** 核对槽位模板与全部生物效果是否越界（返回违例数，0 = 全部合法） */
    private static int checkMatrix() {
        int violations = 0;
        for (BodySlot slot : BodySlot.values()) {
            OrganTemplate template = OrganRegistry.get(slot);
            if (template != null) {
                violations += checkMatrix("槽位模板", slot, template.bonuses());
            }
        }
        for (OrganEffect effect : OrganEffectRegistry.allEffects()) {
            if (effect.attributes() != null) {
                violations += checkMatrix(effect.entityId(), effect.slot(), effect.attributes());
            }
        }
        return violations;
    }

    /**
     * 核对突变词条的部位约束：appliesTo 命中的每个槽位都必须接受该词条的全部属性（含代价）。
     * 拦下「多轴词条并集退化」导致的跨轴错位（如护甲词条落到肺、移速词条落到臂）。
     */
    private static int checkTraitSlots() {
        int violations = 0;
        for (MutantTrait trait : MutantTrait.values()) {
            for (BodySlot slot : BodySlot.values()) {
                if (trait.appliesTo(slot)) {
                    violations += checkMatrix(trait.getId(), slot, trait.attributes());
                }
            }
        }
        return violations;
    }

    /** 逐项判定属性加成是否落在合法槽位（正负值同受约束） */
    private static int checkMatrix(String owner, BodySlot slot, List<OrganTemplate.AttributeBonus> bonuses) {
        int violations = 0;
        for (OrganTemplate.AttributeBonus bonus : bonuses) {
            if (!OrganRegistry.allows(slot, bonus.attribute())) {
                violations++;
                LOGGER.error("[AkaishiReg] [FAIL] 属性错位：「{}」{} 槽非法属性 {} = {}",
                        owner, slot, bonus.attribute().getDescriptionId(), bonus.base());
            }
        }
        return violations;
    }

    /**
     * 核对槽位轴亲和表：被放大的轴（亲和 &gt;1.0）必须是该槽位的合法轴，
     * 否则等于给非法组合开绿灯——与矩阵真源脱节的隐性错位。
     */
    private static int checkSlotAffinity() {
        int violations = 0;
        for (BodySlot slot : BodySlot.values()) {
            for (AttributeWeightRegistry.Axis axis : AttributeWeightRegistry.Axis.values()) {
                if (AttributeWeightRegistry.slotAffinity(slot, axis) <= 1.0) {
                    continue;
                }
                if (!isAxisLegalOn(slot, axis)) {
                    violations++;
                    LOGGER.error("[AkaishiReg] [FAIL] 亲和错位：{} 槽放大非法轴 {}", slot, axis);
                }
            }
        }
        return violations;
    }

    /** 该轴是否在该槽位合法：归并轴取全部成员，任一成员被矩阵接受即视为合法 */
    private static boolean isAxisLegalOn(BodySlot slot, AttributeWeightRegistry.Axis axis) {
        for (Attribute member : AttributeWeightRegistry.members(axis)) {
            if (OrganRegistry.allows(slot, member)) {
                return true;
            }
        }
        return false;
    }
}
