package com.example.akaishi.forbidden.forge;

import com.example.akaishi.boss.agaitolos.AgaitolosEntity;
import com.example.akaishi.command.AkaishiSanityCommand;
import com.example.akaishi.entity.AkaishiForbiddenEntities;
import com.example.akaishi.forbidden.AkaishiForbiddenMod;
import com.example.akaishi.forge.AkaishiAltarDrainHandler;
import com.example.akaishi.forge.boss.agaitolos.AgaitolosArenaEvents;
import com.example.akaishi.forge.boss.agaitolos.AgaitolosDoomHandler;
import com.example.akaishi.forge.life.AkaishiForbiddenErosionHandler;
import com.example.akaishi.forge.life.AkaishiForbiddenTooltipHandler;
import com.example.akaishi.forge.life.AkaishiLifeFusionTooltipHandler;
import com.example.akaishi.forge.life.AkaishiSocketEffectHandler;
import com.example.akaishi.forge.sanity.AkaishiSanityCombatHandler;
import com.example.akaishi.forge.sanity.AkaishiSanityDamageHandler;
import com.example.akaishi.forge.sanity.AkaishiSanityDamageSeenHandler;
import com.example.akaishi.forge.sanity.AkaishiSanityFoodHandler;
import com.example.akaishi.forge.sanity.AkaishiSanityKillHandler;
import com.example.akaishi.forge.sanity.AkaishiSanityPotionBrewing;
import com.example.akaishi.forge.sanity.AkaishiSanitySleepHandler;
import com.example.akaishi.forge.sanity.AkaishiSanityUnnameableHandler;
import com.example.akaishi.forge.sound.AkaishiAltarSoundMuter;
import com.example.akaishi.sanity.shadow.ShadowEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * 赤石禁忌的 Forge 平台入口（mods.toml 中 modId = akaishi_forbidden 的实现类）。
 *
 * <p>P3a 仅完成构造与通用初始化转发；P3b 起承接理智系统的平台生效层：
 * 8 个 Forge 事件处理器、酿造配方、影怪属性/渲染器、HUD 元素、深海视野锁定、
 * 低理智视野后处理与 {@code /akaishi sanity} 调试指令；P3c 起承接 BOSS 阿盖托洛丝的平台生效层：
 * 凋亡降低治疗、下界牢狱场地事件、BOSS 属性与渲染器、铭牌血条与战斗音乐 —— 均与迁前逐处一致（只换模块位置）。
 */
@Mod(AkaishiForbiddenMod.MOD_ID)
public final class AkaishiForbiddenModForge {

    public AkaishiForbiddenModForge() {
        // 通用初始化（物品/效果/实体/创造栏/菜单/网络包/理智服务与节拍）
        AkaishiForbiddenMod.init();

        // 理智回复药水的酿造配方：addMix 是 Forge 补进原版 PotionBrewing 的 API（common 不可见），
        // 且必须等药水注册完成，故放在 common setup（enqueueWork 内执行）
        FMLJavaModLoadingContext.get().getModEventBus().addListener(
                (FMLCommonSetupEvent event) -> event.enqueueWork(AkaishiSanityPotionBrewing::register));
        // 影怪属性供应商：非 Mob 的 LivingEntity 同样必须在此登记，否则实体构造时读不到 MAX_HEALTH 即崩
        FMLJavaModLoadingContext.get().getModEventBus().addListener(
                (EntityAttributeCreationEvent event) ->
                        event.put(AkaishiForbiddenEntities.SHADOW.get(), ShadowEntity.createAttributes().build()));
        // 阿盖托洛丝 BOSS 属性：自定义生物必须在此注册属性供应商，否则实体生成即崩（P3c 自本体迁入）
        FMLJavaModLoadingContext.get().getModEventBus().addListener(
                (EntityAttributeCreationEvent event) ->
                        event.put(AkaishiForbiddenEntities.AGAITOLOS.get(), AgaitolosEntity.createAttributes().build()));

        // 理智系统·精神伤害减免：只处理 akaishi:psychic 伤害（减伤口径在 common 的 SanityDamageGuard 内）
        MinecraftForge.EVENT_BUS.register(AkaishiSanityDamageHandler.INSTANCE);
        // 禁忌秘典·"挨过的伤害类型"记档：LivingHurtEvent 里把玩家被打中的伤害类型记进 SanityState
        MinecraftForge.EVENT_BUS.register(AkaishiSanityDamageSeenHandler.INSTANCE);
        // 理智系统·阈值惩罚（伤害侧）：攻击效能 / 易伤 / 护甲效能 / 0% 档精神化 / 骷髅凋零
        MinecraftForge.EVENT_BUS.register(AkaishiSanityCombatHandler.INSTANCE);
        // 理智系统·食补触发：原版进食 Finish 事件 → common 的食补入口
        MinecraftForge.EVENT_BUS.register(AkaishiSanityFoodHandler.INSTANCE);
        // 理智系统·遭遇不可名状的起手扣减：效果被施加到玩家身上时结算一次（MobEffectEvent.Added）
        MinecraftForge.EVENT_BUS.register(AkaishiSanityUnnameableHandler.INSTANCE);
        // 理智系统·击杀类首见上报：LivingDeathEvent 里按"责任实体是玩家"归因
        MinecraftForge.EVENT_BUS.register(AkaishiSanityKillHandler.INSTANCE);
        // 理智系统·睡眠（P6）：睡醒奖励 + 幻翼附加精神伤害投递
        MinecraftForge.EVENT_BUS.register(AkaishiSanitySleepHandler.INSTANCE);

        // BOSS 阿盖托洛丝（P3c 自本体迁入）：
        // 阶段三「凋亡」的降低治疗层：common 无治疗钩子，故在此消费 Forge 的 LivingHealEvent
        MinecraftForge.EVENT_BUS.register(AgaitolosDoomHandler.INSTANCE);
        // 下界牢狱：召唤仪式右键入口（RightClickBlock）+ 场地方块不可破坏（BreakEvent / ExplosionEvent.Detonate）
        MinecraftForge.EVENT_BUS.register(AgaitolosArenaEvents.INSTANCE);

        // 生命融合护甲实时状态 tooltip（已穿件数/激活情况，仅客户端渲染触发；P3d 自本体迁入）
        MinecraftForge.EVENT_BUS.register(AkaishiLifeFusionTooltipHandler.INSTANCE);

        // 禁忌四件饰品（P3d 自本体迁入）：
        // 统一生效层（生命之触/幼崽之心/母神之印/孕育之环 + 套装）
        // 构造器内完成 common↔forge 接口注入（套装等级加成 / 扭曲屏蔽），注册即生效
        MinecraftForge.EVENT_BUS.register(AkaishiSocketEffectHandler.INSTANCE);
        // 禁忌套装实时状态 tooltip（已集齐件数，仅客户端渲染触发）
        MinecraftForge.EVENT_BUS.register(AkaishiForbiddenTooltipHandler.INSTANCE);
        // 禁忌侵蚀推进层：阈值低语提示 + 跑满结算（9 槽乱码 + 数值重抽，幂等一次）
        MinecraftForge.EVENT_BUS.register(AkaishiForbiddenErosionHandler.INSTANCE);

        // 母神祭坛（P3d 自本体迁入）：
        // 祭坛成型静音：屏蔽四个结构信标的环境音/激活音（取消位置音事件，无需 Mixin）
        MinecraftForge.EVENT_BUS.register(AkaishiAltarSoundMuter.INSTANCE);
        // 仪式吸取掉落豁免：被吸死无掉落/无经验（common 定钩子，此处注入实现并消费事件，D158/D257）
        AkaishiAltarDrainHandler.install();
        MinecraftForge.EVENT_BUS.register(AkaishiAltarDrainHandler.INSTANCE);

        // 理智系统调试指令：/akaishi sanity get|set|env|food
        MinecraftForge.EVENT_BUS.addListener(
                (RegisterCommandsEvent event) -> AkaishiSanityCommand.register(event.getDispatcher()));

        // 客户端专属注册（渲染器 / HUD 元素 / 雾效 / 后处理）：延时到客户端求值，服务端不加载客户端类型
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> AkaishiForbiddenClientSetup::register);
    }
}
