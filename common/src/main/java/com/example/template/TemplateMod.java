package com.example.template;

import com.example.template.api.energy.EnergyTypeRegistry;
import com.example.template.block.ModBlocks;
import com.example.template.block.entity.ModBlockEntities;
import com.example.template.decay.DecayZoneManager;
import com.example.template.effect.ModEffects;
import com.example.template.energy.ChishiEnergyType;
import com.example.template.item.ModCreativeTabs;
import com.example.template.item.ModItems;
import com.example.template.life.body.PlayerBodySync;
import com.example.template.menu.ChishiLifeStructSync;
import com.example.template.menu.ChishiSurgerySync;
import com.example.template.menu.ChishiPotionSync;
import com.example.template.menu.ModMenus;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;

/**
 * 模组通用入口，承载跨平台共享的初始化逻辑。
 */
public final class TemplateMod {
    /** 模组 ID，需与 mods.toml 中的 modId 保持一致 */
    public static final String MOD_ID = "template_mod";

    /** 通用初始化入口，由各平台加载器的入口类调用 */
    public static void init() {
        EnergyTypeRegistry.register(ChishiEnergyType.INSTANCE);
        ModItems.register();
        ModBlocks.register();
        ModBlockEntities.register();
        ModCreativeTabs.register();
        ModMenus.register();
        // 自定义状态效果（衰变）
        ModEffects.register();
        // 衰竭区域：服务端每 tick 结算减益/环境转化/生物转化
        TickEvent.SERVER_LEVEL_POST.register(DecayZoneManager::serverTick);
        // 躯体状态同步包：仅客户端注册接收器（服务端通过 sendToPlayer 主动推送）
        // 用 Platform 判断环境，避免 EnvExecutor 重载签名对 fabric EnvType 的解析依赖
        if (Platform.getEnvironment() == Env.CLIENT) {
            PlayerBodySync.registerClient();
        }
        // 生命结构台目标槽位选择包（C2S 接收器，服务端生效，客户端注册无害）
        ChishiLifeStructSync.register();
        // 手术仓手术开始包（C2S 接收器）
        ChishiSurgerySync.register();
        // 药剂台模板选择包（C2S 接收器）
        ChishiPotionSync.register();
    }
}
