package com.example.akaishi.gametest;

import com.example.akaishi.life.organ.OrganEffectRegistry;
import com.example.akaishi.life.sample.SampleGroup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * 生命器官注册一致性测试（GameTest，dev 环境以 -Dakaishi.gametest=1 触发）。
 * 守护规则：OrganEffectRegistry 的每个注册生物必须满足——
 * 1. entityId 是有效实体注册项（防拼写死链）
 * 2. 能被 SampleGroup 识别出采集分组（防"注册了特色器官却采不到样本"的死注册回归）
 */
public final class AkaishiLifeSystemTests {

    private AkaishiLifeSystemTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void organSourcesAreAllCollectable(GameTestHelper helper) {
        for (String entityId : OrganEffectRegistry.registeredSources()) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE
                    .get(ResourceLocation.tryParse(entityId));
            if (type == null) {
                helper.fail("器官来源「" + entityId + "」不是有效实体 ID");
                return;
            }
            Entity raw = type.create(helper.getLevel());
            if (!(raw instanceof LivingEntity living)) {
                helper.fail("器官来源「" + entityId + "」不是活体实体");
                return;
            }
            if (SampleGroup.of(living) == null) {
                helper.fail("死注册：「" + entityId + "」无法被任何样本分组采集");
                return;
            }
        }
        helper.succeed();
    }
}
