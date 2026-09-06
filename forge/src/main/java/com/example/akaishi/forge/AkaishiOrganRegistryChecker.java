package com.example.akaishi.forge;

import com.example.akaishi.life.organ.OrganEffectRegistry;
import com.example.akaishi.life.sample.SampleGroup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 器官登记入库核对器（dev 环境 -Dakaishi.gametest=1 时于服务端启动后同步执行）：
 * 遍历 OrganEffectRegistry.registeredSources()，逐一断言——entityId 有效、为活体实体、
 * SampleGroup.of 能识别出采集分组（孤儿器官兜底，与 AkaishiLifeSystemTests 同规则，
 * 但后者需原版 GameTest 框架/结构，此核对器仅凭注册表 + 注册 id 即出结论，脚本可直接 grep 断言）。
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
        return ok;
    }
}
