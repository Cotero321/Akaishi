package com.example.akaishi.forbidden;

import com.example.akaishi.api.life.BodySubStateFactory;
import com.example.akaishi.api.sanity.SanityServices;
import com.example.akaishi.boss.agaitolos.arena.NetherPrisonArena;
import com.example.akaishi.effect.AkaishiForbiddenEffects;
import com.example.akaishi.entity.AkaishiForbiddenEntities;
import com.example.akaishi.item.AkaishiCodexItems;
import com.example.akaishi.item.AkaishiSanityItems;
import com.example.akaishi.life.altar.AltarRitualHooks;
import com.example.akaishi.menu.AkaishiCodexMenuRegs;
import com.example.akaishi.menu.AkaishiCodexSync;
import com.example.akaishi.sanity.SanityEnvironmentSettlement;
import com.example.akaishi.sanity.SanityFirstEncounterSettlement;
import com.example.akaishi.sanity.SanityNaturalRegen;
import com.example.akaishi.sanity.SanityPenaltySettlement;
import com.example.akaishi.sanity.SanityPhantomDive;
import com.example.akaishi.sanity.SanityServiceImpl;
import com.example.akaishi.sanity.SanitySleepDeprivation;
import com.example.akaishi.sanity.SanityState;
import com.example.akaishi.sanity.SanitySyncS2C;
import com.example.akaishi.sanity.content.SanityBuiltinFirstEncounters;
import com.example.akaishi.sanity.content.SanityBuiltinFood;
import com.example.akaishi.sanity.content.SanityBuiltinRestores;
import com.example.akaishi.sanity.content.SanityBuiltinRules;
import com.example.akaishi.sanity.content.SanityBuiltinThresholdCuts;
import com.example.akaishi.sanity.content.SanityFoodSettlement;
import com.example.akaishi.sanity.content.SanityRestorePotions;
import com.example.akaishi.sound.AkaishiForbiddenSounds;

import dev.architectury.event.events.common.TickEvent;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;

/**
 * 赤石禁忌（Akaishi Forbidden）的通用入口。
 *
 * <p><b>P3a</b>：禁忌秘典（物品 / 创造栏「禁忌」/ 菜单与界面 / 网络包）已自本体迁入本模块。
 * <p><b>P3b</b>：理智系统（{@code sanity} 实现包 + {@code api/sanity} 契约 + 影怪实体 + 效果 +
 * 相关物品 + 调试指令 + 服务端结算节拍 + S2C 同步）整体自本体与核心迁入本模块。
 * <p><b>P3c</b>：BOSS「阿盖托洛丝（下界本源）」（{@code boss/agaitolos} 实现包 + 下界牢狱场地 +
 * 召唤仪式 + 凋亡效果 + 凋零头颅实体 + 战斗音乐 SoundEvent 注册）整体自本体迁入本模块。
 * 未安装本模块时这些内容一律不注册（注册命名空间仍为三模块共用的 {@code akaishi:}）；
 * 其余禁忌内容（母神祭坛 / 生命融合）在后续阶段迁入。
 */
public final class AkaishiForbiddenMod {
    /** 模组 ID，需与禁忌模块 mods.toml 中的 modId 保持一致 */
    public static final String MOD_ID = "akaishi_forbidden";

    /** 重复初始化保护（各平台入口只应调一次） */
    private static boolean initialized;

    private AkaishiForbiddenMod() {
    }

    /** 通用初始化入口，由各平台加载器的入口类调用 */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        // ===== 注册内容（先内容、后创造栏，与本体 §1 顺序约定一致）=====
        // 禁忌秘典物品
        AkaishiCodexItems.register();
        // 理智系统：影怪 + 精神弹实体类型；状态效果 akaishi:sanity_restore；物品（两瓶药剂 + 花环）
        // P3c：同一注册方法内追加 BOSS 实体（agaitolos / agaitolos_wither_skull）与效果 akaishi:doom
        AkaishiForbiddenEntities.register();
        AkaishiForbiddenEffects.register();
        AkaishiSanityItems.register();
        // 理智回复药水（原版酿造链路）：须排在效果注册之后（药水实例创建时读该效果）
        SanityRestorePotions.register();
        // 创造栏「禁忌」：栏内条目含本体侧四件禁忌饰品 + 秘典 + 三件理智物品
        AkaishiForbiddenCreativeTabs.register();
        // 秘典菜单类型（+ 客户端 screen factory）
        AkaishiCodexMenuRegs.register();
        // 秘典网络包：C2S 研究请求（服务端注册；客户端注册也无害，与本体同口径）
        AkaishiCodexSync.register();
        // 秘典进度快照接收器：仅客户端注册
        if (Platform.getEnvironment() == Env.CLIENT) {
            AkaishiCodexSync.registerClient();
        }

        // ===== 理智系统·服务与节拍（P3b 自本体迁入）=====
        // 躯体 capability 的状态段实现注入：本体只按段名原样存取（见 api.life.IBodySubState）
        BodySubStateFactory.install(SanityState::new);
        // 母神祭坛·仪式完成回调：把"首见 mother_altar"上报接回理智系统（本体只广播事件，不依赖本模块）
        AltarRitualHooks.install(player ->
                SanityServices.get().reportFirstEncounter(player, SanityBuiltinFirstEncounters.MOTHER_ALTAR));

        // 理智系统：内部实现注册（对外契约只在 api.sanity）+ 环境结算与数值同步两条服务端 tick
        SanityServices.register(SanityServiceImpl.instance());
        // 内置内容：环境规则与食补档位一律走对外 API 注册（与附属同权，否决回调/豁免对它同样生效）
        SanityBuiltinRules.register();
        SanityBuiltinFood.register();
        // 首见全表（声明式条目由环境节拍轮询，行为类条目由对应钩子上报）+ 首见聊天提示
        SanityBuiltinFirstEncounters.register();
        SanityFirstEncounterSettlement.registerNotifier();
        // SANC 恢复来源（首用类）：消费点在 SanityRestoreService，与食补挂同一个"用完一口"事件
        SanityBuiltinRestores.register();
        // 阈值惩罚·上限削减账本：跨档建账/取消走对外 API 的阈值钩子（内置内容同样经注册表，与附属同权）
        SanityBuiltinThresholdCuts.register();
        TickEvent.SERVER_LEVEL_POST.register(SanityEnvironmentSettlement::serverTick);
        // 阈值惩罚·持续类落地（tempCut 账本、60/20/0 档效果、攻速与精神化标记）：与上面同一 1s 节拍
        TickEvent.SERVER_LEVEL_POST.register(SanityPenaltySettlement::serverTick);
        // 0% 档「幻翼自杀式袭击」的逐 tick 驱动：幻翼的"俯冲助推/撞击自毁"需要逐 tick 推进，
        // 而"征用哪几只幻翼"由上面 1s 节拍的 0% 档分支标记（见 SanityPhantomDive）
        TickEvent.SERVER_LEVEL_POST.register(SanityPhantomDive::serverTick);
        // 食补窗口逐 tick 分摊（窗口总量要摊到窗口内每个 tick，不能并入 1s 的环境节拍）
        TickEvent.SERVER_LEVEL_POST.register(SanityFoodSettlement::serverTick);
        // 自然恢复（P6）：有顶 + 在地面 + 方块光 > 10 时按进度累积器缓慢回理智（1s 节拍）
        TickEvent.SERVER_LEVEL_POST.register(SanityNaturalRegen::serverTick);
        // 睡眠剥夺（P6）：睡醒奖励 / 每日扣减（1s 节拍，只在主世界推日）+ 幻翼附加精神伤害投递（逐 tick）
        TickEvent.SERVER_LEVEL_POST.register(SanitySleepDeprivation::serverTick);
        TickEvent.SERVER_LEVEL_POST.register(SanitySyncS2C::serverTick);
        // 理智数值快照接收器：仅客户端注册（供 HUD 读取只读镜像）
        if (Platform.getEnvironment() == Env.CLIENT) {
            SanitySyncS2C.registerClient();
        }

        // ===== BOSS 阿盖托洛丝（P3c 自本体迁入）=====
        // 下界牢狱场地：分批施工 / 分批还原 / 重启自愈 + 复活期凋零，每维度每 tick 驱动
        TickEvent.SERVER_LEVEL_POST.register(NetherPrisonArena::serverTick);
        // 强制触发音效注册类加载：SoundEvent 注册需在注册事件前完成（BOSS 战斗音乐 akaishi:agaitolos_theme）
        AkaishiForbiddenSounds.touch();
    }
}
