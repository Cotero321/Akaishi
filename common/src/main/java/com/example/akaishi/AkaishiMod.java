package com.example.akaishi;

import com.example.akaishi.api.energy.EnergyTypeRegistry;
import com.example.akaishi.block.AkaishiDecayBlocks;
import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.block.entity.ModBlockEntities;
import com.example.akaishi.decay.DecayZoneManager;
import com.example.akaishi.decay.DecayZoneSync;
import com.example.akaishi.effect.ModEffects;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.item.ModCreativeTabs;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.body.PlayerBodySync;
import com.example.akaishi.menu.AkaishiLifeStructSync;
import com.example.akaishi.menu.AkaishiSurgerySync;
import com.example.akaishi.menu.AkaishiPotionSync;
import com.example.akaishi.menu.AkaishiTraitReforgerSync;
import com.example.akaishi.menu.AkaishiGeneManagerSync;
import com.example.akaishi.menu.AkaishiOrganVaultSync;
import com.example.akaishi.menu.ModMenus;
import com.example.akaishi.sound.ModSounds;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;

/**
 * 模组通用入口，承载跨平台共享的初始化逻辑。
 */
public final class AkaishiMod {
    /** 模组 ID，需与 mods.toml 中的 modId 保持一致 */
    public static final String MOD_ID = "akaishi";

    /** 通用初始化入口，由各平台加载器的入口类调用 */
    public static void init() {
        EnergyTypeRegistry.register(AkaishiEnergyType.INSTANCE);
        ModItems.register();
        ModBlocks.register();
        AkaishiDecayBlocks.register();
        ModBlockEntities.register();
        ModCreativeTabs.register();
        ModMenus.register();
        // 自定义状态效果（衰变）
        ModEffects.register();
        // 衰竭区域：服务端每 tick 结算减益/环境转化/生物转化
        TickEvent.SERVER_LEVEL_POST.register(DecayZoneManager::serverTick);
        // 衰竭区域污染强度同步：服务端周期推送玩家所在区域强度（伪群系氛围）
        TickEvent.SERVER_LEVEL_POST.register(DecayZoneSync::serverTick);
        // 躯体状态同步包：仅客户端注册接收器（服务端通过 sendToPlayer 主动推送）
        // 用 Platform 判断环境，避免 EnvExecutor 重载签名对 fabric EnvType 的解析依赖
        if (Platform.getEnvironment() == Env.CLIENT) {
            PlayerBodySync.registerClient();
            DecayZoneSync.registerClient();
        }
        // 生命结构台目标槽位选择包（C2S 接收器，服务端生效，客户端注册无害）
        AkaishiLifeStructSync.register();
        // 手术仓手术开始包（C2S 接收器）
        AkaishiSurgerySync.register();
        // 基因管理器卸载包（C2S 接收器）
        AkaishiGeneManagerSync.register();
        // 药剂台模板选择包（C2S 接收器）
        AkaishiPotionSync.register();
        // 词条重铸仪目标词条选择包（C2S 接收器）
        AkaishiTraitReforgerSync.register();
        // 器官储藏库切页包（C2S 接收器）
        AkaishiOrganVaultSync.register();
        // 强制触发音效注册类加载：SoundEvent 注册需在注册事件前完成
        ModSounds.touch();
    }
}
