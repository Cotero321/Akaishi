package com.example.akaishi.forge;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.api.fluid.IFluidPipeDevice;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.block.AkaishiCrystalBlocks;
import com.example.akaishi.block.AkaishiDecayBlocks;
import com.example.akaishi.block.AkaishiFoundationBlocks;
import com.example.akaishi.block.AkaishiFusionBlocks;
import com.example.akaishi.block.AkaishiItemTerminalBlocks;
import com.example.akaishi.block.AkaishiLifeBlocks;
import com.example.akaishi.block.AkaishiMatrixBlocks;
import com.example.akaishi.block.AkaishiOreDef;
import com.example.akaishi.block.AkaishiReactorBlocks;
import com.example.akaishi.block.AkaishiTransgeneBlocks;
import com.example.akaishi.block.AkaishiWirelessBlocks;
import com.example.akaishi.block.entity.AkaishiItemPortBlockEntity;
import com.example.akaishi.block.entity.MiniatureTerminalBlockEntity;
import com.example.akaishi.block.entity.AkaishiReactorControllerBlockEntity;
import com.example.akaishi.block.entity.AkaishiFusionControllerBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import com.example.akaishi.boss.agaitolos.AgaitolosEntity;
import com.example.akaishi.command.ModCommands;
import com.example.akaishi.combat.ModCombatAttributes;
import com.example.akaishi.config.ConfigSyncS2C;
import com.example.akaishi.entity.ModEntities;
import com.example.akaishi.forge.boss.agaitolos.AgaitolosBossBarOverlay;
import com.example.akaishi.forge.boss.agaitolos.AgaitolosRenderer;
import com.example.akaishi.forge.client.AkaishiDecayFogHandler;
import com.example.akaishi.forge.client.AkaishiLifeEnergyProjectileRenderer;
import com.example.akaishi.forge.client.armor.AkaishiMekaSuitArmorModel;
import com.example.akaishi.forge.client.AkaishiConfigScreenFactory;
import com.example.akaishi.forge.client.DrillBitBeaconRenderer;
import com.example.akaishi.forge.decay.AkaishiDecaySpawnBlocker;
import com.example.akaishi.forge.client.LifeEnergyEmitterRenderer;
import com.example.akaishi.forge.client.MotherAltarRenderer;
import com.example.akaishi.forge.client.PipeSideOverlayRenderer;
import com.example.akaishi.forge.client.WirelessFieldRenderer;
import com.example.akaishi.forge.client.WirelessNodeFieldRenderer;
import com.example.akaishi.forge.client.AkaishiUnnameableHandler;
import com.example.akaishi.forge.client.AkaishiErosionFlashOverlay;
import com.example.akaishi.forge.client.AkaishiUnnameableOverlay;
import com.example.akaishi.forge.client.AkaishiUnnameablePostHandler;
import com.example.akaishi.api.miniature.IMiniaturizableTerminal;
import com.example.akaishi.miniature.MiniatureCollapse;
import com.example.akaishi.forge.client.mechanical.MechanicalPartRenderer;
import com.example.akaishi.forge.client.model.MechanicalPartGeometryLoader;
import com.example.akaishi.forge.config.AkaishiConfig;
import com.example.akaishi.forge.config.AkaishiConfigSync;
import com.example.akaishi.forge.fluid.ForgeFluidBridge;
import com.example.akaishi.forge.fluid.ModFluidsImpl;
import com.example.akaishi.forge.io.ItemPortExternalItemHandler;
import com.example.akaishi.forge.io.MachineCapabilityProvider;
import com.example.akaishi.forge.io.MiniatureTerminalItemHandler;
import com.example.akaishi.forge.life.AkaishiBodyCombatHandler;
import com.example.akaishi.forge.life.AkaishiBodyPassiveHandler;
import com.example.akaishi.forge.life.AkaishiForbiddenErosionHandler;
import com.example.akaishi.forge.life.AkaishiForbiddenTooltipHandler;
import com.example.akaishi.forge.life.AkaishiLifeFusionTooltipHandler;
import com.example.akaishi.forge.life.AkaishiLifeInteraction;
import com.example.akaishi.forge.life.AkaishiMechanicalEffectHandler;
import com.example.akaishi.forge.life.AkaishiSocketEffectHandler;
import com.example.akaishi.forge.life.PlayerBodyCapability;
import com.example.akaishi.forge.life.WardenBossHandler;
import com.example.akaishi.forge.sound.AkaishiAltarSoundMuter;
import com.example.akaishi.forge.value.AkaishiValueForgeEvents;
import com.example.akaishi.forge.value.ValuePlatformImpl;
import com.example.akaishi.gametest.AkaishiFuelSystemTests;
import com.example.akaishi.gametest.AkaishiLifeSystemTests;
import com.example.akaishi.item.AkaishiLifeFusionSet;
import com.example.akaishi.item.AkaishiPortableEnergyCell;
import com.example.akaishi.item.AkaishiUpgradeHelper;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.wireless.PortableSupplyService;
import com.example.akaishi.wireless.WirelessFieldManager;
import com.example.akaishi.wireless.WirelessNetworkManager;
import com.example.akaishi.wireless.WirelessNodeRegistry;
import dev.architectury.platform.forge.EventBuses;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.WitherSkullRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Forge 平台入口。@Mod 注解将本类注册为 mods.toml 声明的 "akaishi" 的实现类。
 * 同时承载 Forge 专属事件：
 * - 赤石装备升级属性的动态附加（原版 Item 无 getAttributeModifiers(EquipmentSlot, ItemStack)）
 * - 特殊能力战斗效果（吸血 / 火焰抗性）
 */
@Mod(AkaishiMod.MOD_ID)
public final class AkaishiModForge {

    /** 护甲升级修饰符 UUID（固定 ID，属性以 UUID 去重） */
    private static final UUID ARMOR_HEALTH_UUID = UUID.fromString("0a1b2c3d-1001-4000-8000-000000000001");
    private static final UUID ARMOR_DAMAGE_UUID = UUID.fromString("0a1b2c3d-1001-4000-8000-000000000002");
    private static final UUID ARMOR_SPEED_UUID = UUID.fromString("0a1b2c3d-1001-4000-8000-000000000003");
    private static final UUID ARMOR_DEFENSE_UUID = UUID.fromString("0a1b2c3d-1001-4000-8000-000000000004");
    private static final UUID ARMOR_TOUGHNESS_UUID = UUID.fromString("0a1b2c3d-1001-4000-8000-000000000005");
    private static final UUID ARMOR_KNOCKBACK_UUID = UUID.fromString("0a1b2c3d-1001-4000-8000-000000000006");
    private static final UUID ARMOR_MOVE_SPEED_UUID = UUID.fromString("0a1b2c3d-1001-4000-8000-000000000007");
    /** 靴子速度提升能力修饰符 UUID（与通用移速能力区分） */
    private static final UUID ARMOR_BOOTS_SPEED_UUID = UUID.fromString("0a1b2c3d-1001-4000-8000-000000000008");
    /** 剑升级修饰符 UUID（与护甲区分，避免同属性冲突覆盖） */
    private static final UUID SWORD_DAMAGE_UUID = UUID.fromString("0a1b2c3d-2001-4000-8000-000000000001");
    private static final UUID SWORD_SPEED_UUID = UUID.fromString("0a1b2c3d-2001-4000-8000-000000000002");

    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADDITION;
    private static final AttributeModifier.Operation MULTIPLY_BASE = AttributeModifier.Operation.MULTIPLY_BASE;

    public AkaishiModForge() {
        // 将 Forge 的 Mod 事件总线交给 Architectury，使内容在正确的时机加载
        EventBuses.registerModEventBus(AkaishiMod.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // 注册数值配置（common.toml）：加载/重载时同步到 common ModConfig
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AkaishiConfig.SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(AkaishiConfigSync::onModConfig);
        // 玩家登录时推送服务端权威配置值（客户端界面标尺/成功率与服务器一致）
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                ConfigSyncS2C.sendToPlayer(serverPlayer);
                // 登录即按已有赤石进度补齐饰品扩展槽（永久修饰符随 Curios NBT 持久化）
                AkaishiCurioSlotUnlocker.sync(serverPlayer);
            }
        });
        // 获得进度后即时判定开槽（事件驱动，不做逐 tick 轮询）
        MinecraftForge.EVENT_BUS.addListener((AdvancementEvent.AdvancementEarnEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                AkaishiCurioSlotUnlocker.sync(serverPlayer);
            }
        });

        // 注册 4 种液体（下界至纯/复合能量 + 至纯/复合燃料）到 Forge 注册表
        ModFluidsImpl.register(FMLJavaModLoadingContext.get().getModEventBus());
        // 注入外部液体访问桥（MEK 等第三方液体能力对接）
        ForgeFluidBridge.init();
        // 第三方物流能力（能量除外）：任何实现 IItemPipeDevice / IFluidPipeDevice 的机器方块自动
        // 暴露 Forge ITEM_HANDLER / FLUID_HANDLER，原版漏斗、MEK 管道、AE2/RS 等可直接对接；
        // 方向（仅输出/仅输入）与废料/等离子家族过滤由 forge.io 适配器逐槽/逐罐遵守。
        // 液体管道为直连模式，自身无缓冲，故不额外暴露 FLUID_HANDLER。
        // 例外：储存无线输入/输出口是"无内部容器、纯转发"的终端远程接口面，通用适配层按真实槽位
        // 记账（插入乐观扣减、抽取乐观回报），会对它丢物/复制，故改挂专用转发实现（见该类注释）。
        // 同理，微缩终端也是虚拟槽纯转发（且费用不足整笔拒绝），须排在通用分支之前单独处理；
        // 仅在其确实声明了物品槽（getContainerSize > 0）时才挂能力，无物品能力的族不暴露槽位。
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, (AttachCapabilitiesEvent<BlockEntity> event) -> {
            BlockEntity be = event.getObject();
            if (be instanceof AkaishiItemPortBlockEntity port) {
                event.addCapability(new ResourceLocation(AkaishiMod.MOD_ID, "item_port_external"),
                        new ItemPortExternalItemHandler(port));
            } else if (be instanceof MiniatureTerminalBlockEntity miniature) {
                // 微缩终端一律由本分支处理，**绝不落到下面的通用分支**：通用适配层按"真实槽位"记账，
                // 对虚拟槽纯转发件会丢物/复制。无物品能力的族（containerSize == 0，纯能量族）直接不挂物品能力；
                // 能量亦不外转第三方（赤能源/生命能量自研，见项目铁律）。
                if (miniature.getContainerSize() > 0) {
                    event.addCapability(new ResourceLocation(AkaishiMod.MOD_ID, "miniature_terminal_external"),
                            new MiniatureTerminalItemHandler(miniature));
                }
            } else if (be instanceof IItemPipeDevice || be instanceof IFluidPipeDevice) {
                event.addCapability(new ResourceLocation(AkaishiMod.MOD_ID, "external_logistics"),
                        new MachineCapabilityProvider(be));
            }
        });

        // 注册 GameTest（dev 环境 -Dakaishi.gametest=1 自动运行）
        FMLJavaModLoadingContext.get().getModEventBus()
                .addListener((RegisterGameTestsEvent event) -> {
                    event.register(AkaishiFuelSystemTests.class);
                    event.register(AkaishiLifeSystemTests.class);
                });

        // 底层战斗属性（暴击率/暴击伤害/闪避）挂到玩家：
        // 必须走 EntityAttributeModificationEvent 增量追加，不可用 EntityAttributeCreationEvent.put 覆盖
        // 玩家属性供应商，否则会丢失全部原版属性并触发 "Registry Object not present" 崩溃。
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onEntityAttributeModification);

        // 阿盖托洛丝 BOSS 属性：自定义生物必须在此注册属性供应商，否则实体生成即崩
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onEntityAttributes);

        // 原生动力护甲层：分件几何绑定到人形骨骼，不依赖 Geo/GeckoLib。
        FMLJavaModLoadingContext.get().getModEventBus().addListener(
                (EntityRenderersEvent.RegisterLayerDefinitions event) -> event.registerLayerDefinition(
                        AkaishiMekaSuitArmorModel.LAYER, AkaishiMekaSuitArmorModel::createLayer));

        // 注册机械部件模型加载器（IGeometryLoader：方案4）
        FMLJavaModLoadingContext.get().getModEventBus()
                .addListener((ModelEvent.RegisterGeometryLoaders event) -> {
                    event.register("mechanical_part", new MechanicalPartGeometryLoader());
                });

        // Curios 饰品集成：通用事件（击杀/挖掘/受伤）走游戏总线；装备与每 tick 由 Curios 自动驱动
        MinecraftForge.EVENT_BUS.register(AkaishiCurioIntegration.INSTANCE);
        // 便携终端「随身供能」：注册 Curios 饰品充能承接器（common 侧不可见 Curios API）
        AkaishiCurioIntegration.installSupplySink();

        // 玩家躯体状态（9 槽位器官/肢体）：capability 挂载 + 向 common 注入访问器
        PlayerBodyCapability.init();
        MinecraftForge.EVENT_BUS.register(PlayerBodyCapability.INSTANCE);

        // 生命科技交互：手持样本采集器右键生物抽取样本
        MinecraftForge.EVENT_BUS.register(AkaishiLifeInteraction.INSTANCE);

        // 器官运行时处理器：属性/被动/排斥/冲突 tick + 战斗效果（移植系统核心，必须注册才生效）
        MinecraftForge.EVENT_BUS.register(AkaishiBodyPassiveHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(AkaishiBodyCombatHandler.INSTANCE);
        // 机械义体特殊效果（DNA 授予）：tick / 受击 / 击退消费，缺注册则义体效果不可见亦无效
        MinecraftForge.EVENT_BUS.register(AkaishiMechanicalEffectHandler.INSTANCE);

        // 生命融合护甲实时状态 tooltip（已穿件数/激活情况，仅客户端渲染触发）
        MinecraftForge.EVENT_BUS.register(AkaishiLifeFusionTooltipHandler.INSTANCE);

        // 禁忌四件饰品统一生效层（生命之触/幼崽之心/母神之印/孕育之环 + 套装）
        // 构造器内完成 common↔forge 接口注入（套装等级加成 / 扭曲屏蔽），注册即生效
        MinecraftForge.EVENT_BUS.register(AkaishiSocketEffectHandler.INSTANCE);
        // 禁忌套装实时状态 tooltip（已集齐件数，仅客户端渲染触发）
        MinecraftForge.EVENT_BUS.register(AkaishiForbiddenTooltipHandler.INSTANCE);
        // 禁忌侵蚀推进层：阈值低语提示 + 跑满结算（9 槽乱码 + 数值重抽，幂等一次）
        MinecraftForge.EVENT_BUS.register(AkaishiForbiddenErosionHandler.INSTANCE);

        // 监守者 Boss 化：紫色 Boss 血条 + Boss 保护（免疫击退/免疫负面）
        MinecraftForge.EVENT_BUS.register(WardenBossHandler.INSTANCE);

        // 衰竭区域死寂：区域内禁止生物自然生成
        MinecraftForge.EVENT_BUS.register(AkaishiDecaySpawnBlocker.INSTANCE);

        // 祭坛成型静音：屏蔽四个结构信标的环境音/激活音（取消位置音事件，无需 Mixin）
        MinecraftForge.EVENT_BUS.register(AkaishiAltarSoundMuter.INSTANCE);

        // 仪式吸取掉落豁免：被吸死无掉落/无经验（common 定钩子，此处注入实现并消费事件，D158/D257）
        AkaishiAltarDrainHandler.install();
        MinecraftForge.EVENT_BUS.register(AkaishiAltarDrainHandler.INSTANCE);

        // 估值内核平台桥接：注入「当前服务端」获取方式（common 层通过 ValuePlatform 读取）
        ValuePlatformImpl.install();
        // 掉落来源索引：数据包重载标脏 + tick 分帧扫表 + 服务端停止清索引
        MinecraftForge.EVENT_BUS.register(AkaishiValueForgeEvents.INSTANCE);

        // 无线场域 / 节点登记表 / 终端注册表按 server 实例分表，需在服务器停止时显式整组丢弃：
        // 否则静态表会长期钉住已结束的 ServerLevel，且条目随"建过多少终端"单调增长永不回收
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            WirelessFieldManager.clearServer(event.getServer());
            WirelessNodeRegistry.clearServer(event.getServer());
            WirelessNetworkManager.clear();
        });

        // 调用通用初始化逻辑
        AkaishiMod.init();
        // 相邻容器物品访问：装上 forge 物品能力实现（common 侧默认只有原版容器兜底）
        com.example.akaishi.api.transfer.ItemAccessHolder.install(new com.example.akaishi.forge.transfer.ForgeItemAccess());

        // 游戏事件总线：动态属性修饰符事件 + 特殊能力战斗事件 + 测试指令
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> ModCommands.build(event.getDispatcher()));
        // 终端微缩（潜行右键）：必须走 RightClickBlock —— 原版在"潜行且手上有物品"时会跳过
        // 方块自身的 use()，直接用物品放置，方块侧钩子根本收不到；本事件不受该门控影响
        MinecraftForge.EVENT_BUS.addListener(this::onRightClickBlock);

        // Mods 菜单在模组构造阶段收集配置扩展点，必须此时注册才能显示配置按钮。
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> AkaishiConfigScreenFactory::register);

        // 客户端：赤石水晶簇贴图含透明像素，须注册 cutout 渲染，否则透明区域渲染成黑色块
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);

        // 「不可名状」HUD 叠加层：RegisterGuiOverlaysEvent 属于 IModBusEvent，必须走 mod 事件总线
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onRegisterOverlays);
    }

    /**
     * 终端微缩：潜行右键已成型终端 ⇒ 坍缩为单方块。
     * <p>
     * <b>为什么走事件而不是方块 {@code use()}</b>：原版在「潜行且手上拿着物品」时，
     * 客户端会跳过 {@code BlockState#use}，直接用物品去放置方块 —— 于是方块侧的钩子<b>永远收不到</b>
     * 这次交互（表现就是"潜行右键照样放方块、毫无反应"）。{@code RightClickBlock} 不受该门控影响，
     * 是扳手类交互的标准落点。
     * <p>
     * 只在<b>已成型终端</b>上才拦截：其它方块（含本族外壳）保持原版行为，不影响建造。
     */
    private void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        if (!(level.getBlockEntity(pos) instanceof IMiniaturizableTerminal)) {
            return;
        }
        if (!player.isShiftKeyDown()) {
            return; // 不潜行：交给方块自身逻辑（开界面）
        }
        if (MiniatureCollapse.collapse(level, pos, player)) {
            event.setCanceled(true);
            event.setUseBlock(Event.Result.DENY);
            event.setUseItem(Event.Result.DENY);
        } else {
            // 未成型 / 无权限：同样拦下，避免"想微缩却把方块放出去"
            event.setCanceled(true);
            event.setUseItem(Event.Result.DENY);
        }
    }

    /** 注册客户端 HUD 叠加层：「不可名状」的边缘粗线 + 噪点 + 低语文字；侵蚀泛红的血色边缘；阿盖托洛丝铭牌血条 */
    private void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        // 泛红先注册（位于下层），避免盖住低语文字
        event.registerAboveAll("erosion_flash_overlay", new AkaishiErosionFlashOverlay());
        event.registerAboveAll("unnameable_overlay", new AkaishiUnnameableOverlay());
        // 阿盖托洛丝铭牌血条：锚在原版 boss 血条层（该层已空 —— 本 BOSS 不再用 ServerBossEvent），
        // 挂上去即占据"原版血条的位置"，且不会与任何原版血条重叠
        event.registerAbove(VanillaGuiOverlay.BOSS_EVENT_PROGRESS.id(), "agaitolos_boss_bar",
                new AgaitolosBossBarOverlay());
    }

    /** 方块渲染类型（仅客户端触发）：透明贴图方块必须显式指定渲染层（水晶簇 cutout / 结构玻璃 translucent） */
    private void onClientSetup(FMLClientSetupEvent event) {
        RenderTypeRegistry.register(RenderType.cutout(), AkaishiCrystalBlocks.CHISHI_CRYSTAL_CLUSTER.get());
        // 凋零藤和烈焰花为带透明像素的十字植物，须注册 cutout 否则透明区呈黑色
        RenderTypeRegistry.register(RenderType.cutout(),
                AkaishiTransgeneBlocks.CHISHI_WITHER_ROOT.get(),
                AkaishiTransgeneBlocks.CHISHI_WITHER_STEM.get(),
                AkaishiTransgeneBlocks.CHISHI_BLAZE_FLOWER_ROOT.get(),
                AkaishiTransgeneBlocks.CHISHI_BLAZE_BLOOM.get());
        // 衰竭木门/活板门贴图含镂空透明区，须注册 cutout，否则透明部分渲染为黑色
        RenderTypeRegistry.register(RenderType.cutout(),
                AkaishiDecayBlocks.CHISHI_DECAY_DOOR.get(),
                AkaishiDecayBlocks.CHISHI_DECAY_TRAPDOOR.get());
        // 16 种矿石为「底材 cube_all + 外凸 1/16 格矿斑层」两层：矿斑层贴图透明底，
        // 必须注册 cutout，否则透明区渲染成黑色；正面两层重合、斜看产生视差立体感
        List<Block> oreBlocks = new ArrayList<>(AkaishiFoundationBlocks.ALL_ORES.size());
        for (AkaishiOreDef def : AkaishiFoundationBlocks.ALL_ORES) {
            oreBlocks.add(AkaishiFoundationBlocks.get(def));
        }
        RenderTypeRegistry.register(RenderType.cutout(), oreBlocks.toArray(new Block[0]));
        // 结构玻璃为半透明材质，注册 translucent 才能正确混合显示内部结构
        RenderTypeRegistry.register(RenderType.translucent(),
                AkaishiReactorBlocks.CHISHI_REACTOR_STRUCTURE_GLASS.get(),
                AkaishiFusionBlocks.CHISHI_FUSION_STRUCTURE_GLASS.get(),
                AkaishiMatrixBlocks.CHISHI_GEN_MATRIX_STRUCTURE_GLASS.get(),
                AkaishiMatrixBlocks.CHISHI_PURIFIER_MATRIX_STRUCTURE_GLASS.get(),
                AkaishiMatrixBlocks.CHISHI_LIFE_MATRIX_STRUCTURE_GLASS.get(),
                AkaishiWirelessBlocks.CHISHI_WIRELESS_STRUCTURE_GLASS.get(),
                AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_STRUCTURE_GLASS.get(),
                AkaishiItemTerminalBlocks.CHISHI_ITEM_TERMINAL_STRUCTURE_GLASS.get());
        // 母神祭坛：注册方块实体渲染器（供奉物悬浮展示）
        BlockEntityRenderers.register(ModBlockEntities.CHISHI_MOTHER_ALTAR.get(), MotherAltarRenderer::new);
        // 钻机钻头：结构成型时从钻头底面打出向下的信标光束（客户端渲染器）
        BlockEntityRenderers.register(ModBlockEntities.CHISHI_MINER_DRILL_BIT.get(), DrillBitBeaconRenderer::new);
        // 能量发射器：头部朝向与蓄能光效（方块实体渲染器）
        BlockEntityRenderers.register(ModBlockEntities.CHISHI_LIFE_ENERGY_EMITTER.get(), LifeEnergyEmitterRenderer::new);
        // 生命能量弹：相机朝向的发光公告板
        EntityRenderers.register(ModEntities.LIFE_ENERGY_PROJECTILE.get(), AkaishiLifeEnergyProjectileRenderer::new);
        // 阿盖托洛丝：GeckoLib 几何动画渲染（阶段一模型/动画，P7 再接三阶段换模）
        EntityRenderers.register(ModEntities.AGAITOLOS.get(), AgaitolosRenderer::new);
        // 阿盖托洛丝的远程弹体：复用原版凋零头渲染器（子类可被 EntityRenderer<WitherSkull> 直接渲染）
        EntityRenderers.register(ModEntities.AGAITOLOS_WITHER_SKULL.get(), WitherSkullRenderer::new);
        // 管道方向标识：输出=臂端收窄尖口，输入=臂端外扩喇叭口（物品/赤能源/生命能量/液体/废料/等离子全族）
        BlockEntityRenderers.<BlockEntity>register(ModBlockEntities.CHISHI_ITEM_PIPE.get(), PipeSideOverlayRenderer::new);
        BlockEntityRenderers.<BlockEntity>register(ModBlockEntities.CHISHI_ENERGY_PIPE.get(), PipeSideOverlayRenderer::new);
        BlockEntityRenderers.<BlockEntity>register(ModBlockEntities.CHISHI_LIFE_ENERGY_PIPE.get(), PipeSideOverlayRenderer::new);
        BlockEntityRenderers.<BlockEntity>register(ModBlockEntities.CHISHI_FLUID_PIPE.get(), PipeSideOverlayRenderer::new);
        BlockEntityRenderers.<BlockEntity>register(ModBlockEntities.CHISHI_EXHAUSTED_PIPE.get(), PipeSideOverlayRenderer::new);
        BlockEntityRenderers.<BlockEntity>register(ModBlockEntities.CHISHI_MULTI_FLUID_WASTE_PIPE.get(), PipeSideOverlayRenderer::new);
        BlockEntityRenderers.<BlockEntity>register(ModBlockEntities.CHISHI_PLASMA_PIPE.get(), PipeSideOverlayRenderer::new);
        // 无线场域屏障：微缩矩阵终端的场域范围画成四面透明蓝光墙（客户端只读重扫取半径，无需同步包）
        BlockEntityRenderers.register(ModBlockEntities.CHISHI_MINI_MATRIX_TERMINAL.get(), WirelessFieldRenderer::new);
        // 网络节点子场域：节点被申领时画它自己的 1 区块场域（不限距离 ⇒ 必须由节点所在区块渲染）
        BlockEntityRenderers.register(ModBlockEntities.CHISHI_MINI_MATRIX_NETWORK_NODE.get(), WirelessNodeFieldRenderer::new);
        // 衰竭区域氛围：玩家身处区域时染污雾色并收拢雾距（伪群系渲染）
        MinecraftForge.EVENT_BUS.register(AkaishiDecayFogHandler.INSTANCE);
        // 「不可名状」视野扭曲：相机滚转/抖动与 FOV 脉动
        MinecraftForge.EVENT_BUS.register(AkaishiUnnameableHandler.INSTANCE);
        // 「不可名状」后处理：整帧对比度提升 + 电视机花白
        MinecraftForge.EVENT_BUS.register(AkaishiUnnameablePostHandler.INSTANCE);

        // 初始化机械部件纹理合成缓存（BEWLR 渲染准备）
        MechanicalPartRenderer.initialize();
    }

    /**
     * 把底层战斗属性（暴击率/暴击伤害/闪避）挂到玩家。
     * 用 ModificationEvent 在原版属性供应商上增量追加，保留全部原版属性。
     */
    private void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, ModCombatAttributes.CRIT_CHANCE.get());
        event.add(EntityType.PLAYER, ModCombatAttributes.CRIT_DAMAGE.get());
        event.add(EntityType.PLAYER, ModCombatAttributes.DODGE_CHANCE.get());
    }

    /** 自定义生物的属性供应商（每个自定义 LivingEntity 类型必须注册一次） */
    private void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.AGAITOLOS.get(), AgaitolosEntity.createAttributes().build());
    }

    /**
     * 为携带升级标签的赤石装备附加升级属性。
     * 事件在 Forge 收集基础属性后触发，addModifier 追加到对应槽位属性集合。
     */
    @SubscribeEvent
    public void onAttributeModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!AkaishiUpgradeHelper.isAkaishiGear(stack)) {
            return;
        }
        EquipmentSlot slot = event.getSlotType();
        int damage = AkaishiUpgradeHelper.getCount(stack, AkaishiUpgradeHelper.TAG_ATTACK_DAMAGE);
        int speed = AkaishiUpgradeHelper.getCount(stack, AkaishiUpgradeHelper.TAG_ATTACK_SPEED);

        if (stack.getItem() instanceof ArmorItem armorItem) {
            // 护甲：仅当事件槽位与装备实际穿戴槽位一致时生效
            if (slot != armorItem.getEquipmentSlot()) {
                return;
            }
            add(event, slot, Attributes.MAX_HEALTH, ARMOR_HEALTH_UUID,
                    AkaishiUpgradeHelper.getCount(stack, AkaishiUpgradeHelper.TAG_MAX_HEALTH) * 2.0);
            add(event, slot, Attributes.ATTACK_DAMAGE, ARMOR_DAMAGE_UUID, damage);
            add(event, slot, Attributes.ATTACK_SPEED, ARMOR_SPEED_UUID, speed * 0.15);
            add(event, slot, Attributes.ARMOR, ARMOR_DEFENSE_UUID,
                    AkaishiUpgradeHelper.getCount(stack, AkaishiUpgradeHelper.TAG_ARMOR));
            add(event, slot, Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS_UUID,
                    AkaishiUpgradeHelper.getCount(stack, AkaishiUpgradeHelper.TAG_TOUGHNESS));
            // 特殊能力：击退抗性 +0.1/级，移速 +2%/级（仅护甲穿戴时）
            int knockback = AkaishiUpgradeHelper.getCount(stack, AkaishiUpgradeHelper.TAG_ABILITY_KNOCKBACK);
            add(event, slot, Attributes.KNOCKBACK_RESISTANCE, ARMOR_KNOCKBACK_UUID, knockback * 0.1);
            int moveSpeed = AkaishiUpgradeHelper.getCount(stack, AkaishiUpgradeHelper.TAG_ABILITY_SPEED);
            add(event, slot, Attributes.MOVEMENT_SPEED, ARMOR_MOVE_SPEED_UUID, moveSpeed * 0.02, MULTIPLY_BASE);
            // 靴子专属：速度提升（+10%，仅 1 级）
            int bootsSpeed = AkaishiUpgradeHelper.getCount(stack, AkaishiUpgradeHelper.TAG_ABILITY_BOOTS_SPEED);
            add(event, slot, Attributes.MOVEMENT_SPEED, ARMOR_BOOTS_SPEED_UUID, bootsSpeed * 0.10, MULTIPLY_BASE);
        } else if (slot == EquipmentSlot.MAINHAND) {
            // 剑：仅主手
            add(event, slot, Attributes.ATTACK_DAMAGE, SWORD_DAMAGE_UUID, damage);
            add(event, slot, Attributes.ATTACK_SPEED, SWORD_SPEED_UUID, speed * 0.15);
        }
    }

    /**
     * 特殊能力战斗效果：
     * - 吸血：攻击者持赤石装备（主/副手）时，命中回复 1 生命/级
     * - 火焰/爆炸/摔落保护：受伤方穿戴赤石装备时，对应伤害减免 15%/级（最多 90%）
     */
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        // 吸血：攻击者为玩家
        if (event.getSource().getEntity() instanceof Player attacker) {
            int lifesteal = totalAbility(attacker, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                    AkaishiUpgradeHelper.TAG_ABILITY_LIFESTEAL);
            if (lifesteal > 0) {
                attacker.heal(lifesteal);
            }
        }
        // 伤害保护：受伤者为玩家，按来源类型分别减免
        if (event.getEntity() instanceof Player victim) {
            EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            float amount = event.getAmount();
            // 1.20.1 的 DamageSource 无 isFire/isExplosion，统一用伤害类型 tag 判断
            if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
                amount = applyProtection(amount, totalAbility(victim, armorSlots, AkaishiUpgradeHelper.TAG_ABILITY_FIRE_RESIST));
            }
            if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
                amount = applyProtection(amount, totalAbility(victim, armorSlots, AkaishiUpgradeHelper.TAG_ABILITY_BLAST_PROTECT));
            }
            if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
                amount = applyProtection(amount, totalAbility(victim, armorSlots, AkaishiUpgradeHelper.TAG_ABILITY_FALL_PROTECT));
            }
            event.setAmount(amount);
            // 胸甲专属：受击时获得生命回复 I（5 秒）
            int hitRegen = totalAbility(victim, new EquipmentSlot[]{EquipmentSlot.CHEST},
                    AkaishiUpgradeHelper.TAG_ABILITY_HIT_REGEN);
            if (hitRegen > 0) {
                victim.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false));
            }
        }
    }

    /**
     * 药水类特殊能力（部位专属）：穿戴对应护甲时持续获得原版药水效果（隐藏粒子，2 秒刷新）。
     * 急迫 / 水下呼吸 / 抗性提升 / 跳跃提升 / 水中加速（海豚的恩惠）/ 缓降。
     */
    @SubscribeEvent
    public void onAbilityTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) {
            return;
        }
        Player player = event.player;
        if (player.isDeadOrDying()) {
            return;
        }
        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : armorSlots) {
            ItemStack gear = player.getItemBySlot(slot);
            if (!AkaishiUpgradeHelper.isAkaishiGear(gear)) {
                continue;
            }
            applyPotionAbility(player, gear, AkaishiUpgradeHelper.TAG_ABILITY_HASTE, MobEffects.DIG_SPEED);
            applyPotionAbility(player, gear, AkaishiUpgradeHelper.TAG_ABILITY_WATER_BREATHING, MobEffects.WATER_BREATHING);
            applyPotionAbility(player, gear, AkaishiUpgradeHelper.TAG_ABILITY_RESISTANCE, MobEffects.DAMAGE_RESISTANCE);
            applyPotionAbility(player, gear, AkaishiUpgradeHelper.TAG_ABILITY_JUMP_BOOST, MobEffects.JUMP);
            applyPotionAbility(player, gear, AkaishiUpgradeHelper.TAG_ABILITY_SWIM_SPEED, MobEffects.DOLPHINS_GRACE);
            applyPotionAbility(player, gear, AkaishiUpgradeHelper.TAG_ABILITY_SLOW_FALLING, MobEffects.SLOW_FALLING);
        }
    }

    /** 若装备携带该能力且玩家当前无对应效果，则补充 2 秒效果（脱下装备后自然消退） */
    private static void applyPotionAbility(Player player, ItemStack gear, String tagKey, MobEffect effect) {
        if (AkaishiUpgradeHelper.getCount(gear, tagKey) > 0 && !player.hasEffect(effect)) {
            player.addEffect(new MobEffectInstance(effect, 40, 0, false, false));
        }
    }

    /**
     * 玩家 tick：背包中任意位置的便捷赤能源储存单元都会自动为玩家身上（护甲 + 主副手）
     * 的赤石装备补充耐久，每 1 点耐久消耗 ENERGY_PER_DURABILITY 赤能源，速率受单元等级限制；
     * 生命融合护甲同样吸收赤能源修复，速率为普通赤石装备的 LIFE_FUSION_REPAIR_MULTIPLIER 倍。
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) {
            return;
        }
        Player player = event.player;
        if (player.isDeadOrDying()) {
            return;
        }
        // 便携终端「随身供能」：若已开启且终端内腔含便捷传输构架，从绑定终端抽能注入背包单元/已装备饰品（限速 10M/tick）
        PortableSupplyService.tick(player);
        // 收集背包中全部便携单元（物品栏 36 格 + 副手；护甲槽无法放入非护甲）
        List<ItemStack> cells = new ArrayList<>();
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof AkaishiPortableEnergyCell) {
                cells.add(s);
            }
        }
        ItemStack offhand = inv.offhand.get(0);
        if (offhand.getItem() instanceof AkaishiPortableEnergyCell) {
            cells.add(offhand);
        }
        if (cells.isEmpty()) {
            return;
        }
        // 修复目标：护甲 4 件 + 主副手（便携单元放不进护甲槽，无需跳过）
        EquipmentSlot[] gearSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
                EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        // 外层遍历单元：每个单元每 tick 有独立修复额度，多个单元可叠加供能
        for (ItemStack cellStack : cells) {
            AkaishiPortableEnergyCell portable = (AkaishiPortableEnergyCell) cellStack.getItem();
            if (portable.getEnergyStored(cellStack) < AkaishiPortableEnergyCell.ENERGY_PER_DURABILITY) {
                continue; // 该单元能量不足，换下一个
            }
            int repairLimit = portable.tier.repairPerTick;
            // 1) 普通赤石装备（护甲/剑/工具）原速率修复；生命融合护甲交给下方专用快速循环
            for (EquipmentSlot slot : gearSlots) {
                if (repairLimit <= 0) {
                    break; // 本单元本 tick 额度用完
                }
                ItemStack gear = player.getItemBySlot(slot);
                if (gear.isEmpty() || !gear.isDamaged() || AkaishiLifeFusionSet.isLifeFusionArmor(gear)) {
                    continue;
                }
                // 兼容创造模式直接取用的无标签装备，并过滤非赤石装备
                AkaishiUpgradeHelper.ensureGear(gear);
                if (!AkaishiUpgradeHelper.isAkaishiGear(gear)) {
                    continue;
                }
                int toRepair = (int) Math.min(repairLimit, Math.min(gear.getDamageValue(),
                        portable.getEnergyStored(cellStack) / AkaishiPortableEnergyCell.ENERGY_PER_DURABILITY));
                if (toRepair <= 0) {
                    continue;
                }
                portable.extractEnergy(cellStack, toRepair * AkaishiPortableEnergyCell.ENERGY_PER_DURABILITY, false);
                gear.setDamageValue(gear.getDamageValue() - toRepair);
                repairLimit -= toRepair;
            }
            // 2) 生命融合护甲：同样消耗赤能源，但修复速率 ×4，恢复显著快于普通赤石装备
            int lifeBudget = portable.tier.repairPerTick * AkaishiLifeFusionSet.LIFE_FUSION_REPAIR_MULTIPLIER;
            for (EquipmentSlot slot : armorSlots) {
                if (lifeBudget <= 0) {
                    break; // 本单元本 tick 的生命护甲修复额度用完
                }
                ItemStack gear = player.getItemBySlot(slot);
                if (gear.isEmpty() || !gear.isDamaged() || !AkaishiLifeFusionSet.isLifeFusionArmor(gear)) {
                    continue;
                }
                int toRepair = (int) Math.min(lifeBudget, Math.min(gear.getDamageValue(),
                        portable.getEnergyStored(cellStack) / AkaishiPortableEnergyCell.ENERGY_PER_DURABILITY));
                if (toRepair <= 0) {
                    continue;
                }
                portable.extractEnergy(cellStack, toRepair * AkaishiPortableEnergyCell.ENERGY_PER_DURABILITY, false);
                gear.setDamageValue(gear.getDamageValue() - toRepair);
                lifeBudget -= toRepair;
            }
        }
    }

    /**
     * 效率升级：挖掘速度每级 +20%（挖掘速度无实体属性，用方块破坏速度事件实现）。
     * 仅主手赤石挖掘工具（铲/斧/镐）生效。
     */
    @SubscribeEvent
    public void onBlockBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        ItemStack main = player.getMainHandItem();
        if (AkaishiUpgradeHelper.isAkaishiGear(main)) {
            int eff = AkaishiUpgradeHelper.getCount(main, AkaishiUpgradeHelper.TAG_EFFICIENCY);
            if (eff > 0) {
                event.setNewSpeed(event.getNewSpeed() * (1.0F + 0.2F * eff));
            }
        }
    }

    /**
     * 工具专属能力：区域破坏。破坏方块时同时破坏以目标为中心的 3×3×3 区域
     * （跳过空气与不可破坏方块），每次区域破坏额外消耗 1 + 实际破坏数/4 点耐久。
     */
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        // 反应堆/聚变结构扫描缓存失效：结构内方块被玩家破坏 → 立即重扫（成型后另有 20 tick 兜底）
        AkaishiReactorControllerBlockEntity.invalidateNearby((Level) event.getLevel(), event.getPos());
        AkaishiFusionControllerBlockEntity.invalidateNearby((Level) event.getLevel(), event.getPos());
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide) {
            return;
        }
        ItemStack main = player.getMainHandItem();
        if (!AkaishiUpgradeHelper.isAkaishiGear(main)) {
            return;
        }
        if (AkaishiUpgradeHelper.getCount(main, AkaishiUpgradeHelper.TAG_ABILITY_AREA_BREAK) <= 0) {
            return;
        }
        event.setCanceled(true); // 取消原破坏，由下方统一处理 3×3×3
        BlockPos center = event.getPos();
        Level level = player.level();
        boolean drop = !player.isCreative();
        int broken = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.getX() - 1, center.getY() - 1, center.getZ() - 1,
                center.getX() + 1, center.getY() + 1, center.getZ() + 1)) {
            BlockState state = level.getBlockState(p);
            if (state.isAir() || state.getDestroySpeed(level, p) < 0) {
                continue; // 空气与基岩等不可破坏方块跳过
            }
            if (level.destroyBlock(p, drop)) {
                broken++;
            }
        }
        if (broken > 0) {
            main.hurtAndBreak(broken / 4 + 1, player, e -> {
            });
        }
    }

    /**
     * 反应堆/聚变结构扫描缓存失效：结构内方块被放置 → 立即重扫（搭建时快速成型反馈）。
     */
    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.getLevel().isClientSide()) {
            AkaishiReactorControllerBlockEntity.invalidateNearby((Level) event.getLevel(), event.getPos());
            AkaishiFusionControllerBlockEntity.invalidateNearby((Level) event.getLevel(), event.getPos());
        }
    }

    /**
     * 剑专属能力：击杀掉落赤石精华。每级 15% 概率（3 级最多 45%），掉落 1 个赤石精华。
     */
    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (event.getSource().getEntity() instanceof Player attacker) {
            int level = totalAbility(attacker, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                    AkaishiUpgradeHelper.TAG_ABILITY_DROP_ESSENCE);
            if (level > 0 && attacker.getRandom().nextFloat() < 0.15F * level) {
                Level world = attacker.level();
                net.minecraft.world.phys.Vec3 pos = event.getEntity().position();
                event.getDrops().add(new ItemEntity(world, pos.x, pos.y, pos.z,
                        new ItemStack(ModItems.akaishiEssence.get(), 1)));
            }
        }
    }

    /** 按能力等级减免伤害：15%/级，最多 90% */
    private static float applyProtection(float amount, int level) {
        if (level <= 0) {
            return amount;
        }
        float reduction = Math.min(0.9F, 0.15F * level);
        return amount * (1.0F - reduction);
    }

    /** 统计指定槽位赤石装备上某能力的总等级 */
    private static int totalAbility(LivingEntity entity, EquipmentSlot[] slots, String tagKey) {
        int total = 0;
        for (EquipmentSlot slot : slots) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (AkaishiUpgradeHelper.isAkaishiGear(stack)) {
                total += AkaishiUpgradeHelper.getCount(stack, tagKey);
            }
        }
        return total;
    }

    private static int totalAbility(LivingEntity entity, EquipmentSlot slot1, EquipmentSlot slot2, String tagKey) {
        return totalAbility(entity, new EquipmentSlot[]{slot1, slot2}, tagKey);
    }

    private static void add(ItemAttributeModifierEvent event, EquipmentSlot slot,
                            net.minecraft.world.entity.ai.attributes.Attribute attribute,
                            UUID id, double amount) {
        if (amount != 0.0) {
            event.addModifier(attribute, new AttributeModifier(id, "Akaishi upgrade", amount, ADD));
        }
    }

    private static void add(ItemAttributeModifierEvent event, EquipmentSlot slot,
                            net.minecraft.world.entity.ai.attributes.Attribute attribute,
                            UUID id, double amount, AttributeModifier.Operation operation) {
        if (amount != 0.0) {
            event.addModifier(attribute, new AttributeModifier(id, "Akaishi ability", amount, operation));
        }
    }

    /** dev 环境燃料系统/反应堆系统 GameTest 入口（-Dakaishi.gametest=1 / -Dakaishi.gametest.reactor=1 时服务端启动后自动运行） */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        if (System.getProperty("akaishi.gametest") != null) {
            // 先做器官登记入库核对（同步、无 tick 依赖），再启动燃料端到端测试
            AkaishiOrganRegistryChecker.run(event.getServer());
            AkaishiGameTestAutoRunner.start(event.getServer());
        }
        if (System.getProperty("akaishi.gametest.reactor") != null) {
            AkaishiReactorGameTestAutoRunner.start(event.getServer());
        }
    }

    /** 测试运行期间每 tick 检查进度，全部完成后自动关闭服务端 */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            AkaishiGameTestAutoRunner.tick();
            AkaishiReactorGameTestAutoRunner.tick();
        }
    }
}
