package com.example.akaishi;

import com.example.akaishi.api.energy.EnergyTypeRegistry;
import com.example.akaishi.block.AkaishiCrystalBlocks;
import com.example.akaishi.block.AkaishiDecayBlocks;
import com.example.akaishi.block.AkaishiEnergyBlocks;
import com.example.akaishi.block.AkaishiWirelessBlocks;
import com.example.akaishi.block.AkaishiFusionBlocks;
import com.example.akaishi.block.AkaishiItemTerminalBlocks;
import com.example.akaishi.block.AkaishiLifeBlocks;
import com.example.akaishi.block.AkaishiMinerBlocks;
import com.example.akaishi.block.AkaishiMiniMatrixBlocks;
import com.example.akaishi.block.AkaishiMiniatureBlocks;
import com.example.akaishi.block.AkaishiMotherAltarBlocks;
import com.example.akaishi.block.AkaishiMatrixBlocks;
import com.example.akaishi.block.AkaishiReactorBlocks;
import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.block.entity.ModBlockEntities;
import com.example.akaishi.combat.ModCombatAttributes;
import com.example.akaishi.decay.DecayZoneManager;
import com.example.akaishi.decay.DecayZoneSync;
import com.example.akaishi.effect.ModEffects;
import com.example.akaishi.effect.ScreenFlashS2C;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.entity.ModEntities;
import com.example.akaishi.item.AkaishiBannerPatterns;
import com.example.akaishi.item.ModCreativeTabs;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.config.ConfigSyncS2C;
import com.example.akaishi.life.body.PlayerBodySync;
import com.example.akaishi.menu.AkaishiLifeStructSync;
import com.example.akaishi.menu.AkaishiSurgerySync;
import com.example.akaishi.menu.AkaishiPotionSync;
import com.example.akaishi.menu.AkaishiTraitReforgerSync;
import com.example.akaishi.menu.AkaishiGeneManagerSync;
import com.example.akaishi.menu.AkaishiOrganVaultSync;
import com.example.akaishi.menu.AkaishiItemTerminalSync;
import com.example.akaishi.menu.ModMenus;
import com.example.akaishi.miniature.AkaishiMiniatureAdapters;
import com.example.akaishi.life.mechanical.MechanicalDnaProfile;
import com.example.akaishi.life.mechanical.MechanicalMaterial;
import com.example.akaishi.sound.ModSounds;
import com.example.akaishi.value.AkaishiValueService;
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
        // 机械域注册表默认项：材料 / DNA 模板（渲染预合成与部件校验依赖该注册）
        MechanicalMaterial.registerDefaults();
        MechanicalDnaProfile.registerDefaults();
        // 旗帜图案域：须先于物品域注册（山羊头旗帜图案物品引用其标签）
        AkaishiBannerPatterns.register();
        ModItems.register();
        // 衰竭域须先于方块门面注册：ModBlocks 门面会转发其字段引用
        AkaishiDecayBlocks.register();
        ModBlocks.register();
        AkaishiCrystalBlocks.register();
        AkaishiEnergyBlocks.register();
        AkaishiWirelessBlocks.register();
        AkaishiFusionBlocks.register();
        AkaishiMinerBlocks.register();
        AkaishiMotherAltarBlocks.register();
        AkaishiReactorBlocks.register();
        AkaishiLifeBlocks.register();
        AkaishiMatrixBlocks.register();
        // 微缩矩阵域：同样须先于 ModBlockEntities.register() 注册（BE 类型绑定其控制器方块引用）
        AkaishiMiniMatrixBlocks.register();
        // 物品终端域：须先于 ModBlockEntities.register() 注册（BE 类型绑定其方块引用）
        AkaishiItemTerminalBlocks.register();
        // 微缩终端域：同样须先于 ModBlockEntities.register() 注册（BE 类型绑定其方块引用）
        AkaishiMiniatureBlocks.register();
        ModBlockEntities.register();
        // 终端微缩适配器：通用微缩层按 NBT 里的族 id 取适配器还原状态（无顺序约束）
        AkaishiMiniatureAdapters.register();
        // 实体类型：能量弹等（不依赖方块，随注册表事件求值）
        ModEntities.register();
        ModCreativeTabs.register();
        ModMenus.register();
        // 自定义状态效果（衰变）
        ModEffects.register();
        // 底层战斗属性（暴击率/暴击伤害/闪避）：两条数值线共用的属性载体
        ModCombatAttributes.register();
        // 衰竭区域：服务端每 tick 结算减益/环境转化/生物转化
        TickEvent.SERVER_LEVEL_POST.register(DecayZoneManager::serverTick);
        // 衰竭区域污染强度同步：服务端周期推送玩家所在区域强度（伪群系氛围）
        TickEvent.SERVER_LEVEL_POST.register(DecayZoneSync::serverTick);
        // 躯体状态同步包：仅客户端注册接收器（服务端通过 sendToPlayer 主动推送）
        // 用 Platform 判断环境，避免 EnvExecutor 重载签名对 fabric EnvType 的解析依赖
        if (Platform.getEnvironment() == Env.CLIENT) {
            PlayerBodySync.registerClient();
            DecayZoneSync.registerClient();
            // 服务端权威配置值同步（登录/热重载时由 forge 层推送）
            ConfigSyncS2C.registerClient();
            // 屏幕泛红表现（侵蚀跑满 / 吸取被吸，D100/D180）
            ScreenFlashS2C.registerClient();
            // 物品终端库页条目快照（S2C 接收器，仅客户端注册）
            AkaishiItemTerminalSync.registerClient();
            // 无线终端安全页权限表快照（S2C 接收器，仅客户端注册）
            com.example.akaishi.menu.AkaishiTerminalSecuritySync.registerClient();
            // 端口远程绑定清单快照（S2C 接收器，仅客户端注册）
            com.example.akaishi.menu.AkaishiPortBindingSync.registerClient();
            // 储存口远程绑定清单快照（S2C 接收器，仅客户端注册）
            com.example.akaishi.menu.AkaishiItemPortBindingSync.registerClient();
            // 微缩矩阵终端视图快照（S2C 接收器，仅客户端注册）
            com.example.akaishi.menu.AkaishiMiniMatrixSync.registerClient();
            // 微缩矩阵终端加工页视图（S2C 接收器，仅客户端注册）
            com.example.akaishi.menu.AkaishiMatrixCraftSync.registerClient();
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
        // 模板制造厂目标模板选择包（C2S 接收器）
        com.example.akaishi.menu.MechanicalSelectSync.register();
        // 机械三机单次制作请求包（C2S 接收器）
        com.example.akaishi.menu.MechanicalCraftSync.register();
        // 物品终端库页交互包（C2S 接收器：AE2 网格双向的点击动作）
        AkaishiItemTerminalSync.register();
        // 无线终端安全页动作包（C2S 接收器：登记 / 移除 / 勾选权限）
        com.example.akaishi.menu.AkaishiTerminalSecuritySync.register();
        // 端口远程绑定动作包（C2S 接收器：绑定 / 解绑）
        com.example.akaishi.menu.AkaishiPortBindingSync.register();
        // 储存口远程绑定动作包（C2S 接收器：绑定 / 解绑）
        com.example.akaishi.menu.AkaishiItemPortBindingSync.register();
        // 微缩矩阵终端加工页动作包（C2S 接收器：搜索 / 选中 / 开始加工）
        com.example.akaishi.menu.AkaishiMatrixCraftSync.register();
        // 价值分服务：统一存储库排序/统计/筛选与查询指令共用的底层（纯计算，不参与经济兑换）
        AkaishiValueService.install();
        // 强制触发音效注册类加载：SoundEvent 注册需在注册事件前完成
        ModSounds.touch();
    }
}
