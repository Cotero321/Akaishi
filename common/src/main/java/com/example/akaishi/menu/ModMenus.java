package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.AkaishiGenMatrixTier;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import com.example.akaishi.block.entity.AbstractMechanicalMachineBlockEntity;
import com.example.akaishi.block.entity.AkaishiAutoCollectorBlockEntity;
import com.example.akaishi.block.entity.AkaishiCatalystBlockEntity;
import com.example.akaishi.block.entity.AkaishiEnergyAggregatorBlockEntity;
import com.example.akaishi.block.entity.AkaishiEnergyCellBlockEntity;
import com.example.akaishi.block.entity.AkaishiEnergyCellSerializerBlockEntity;
import com.example.akaishi.block.entity.AkaishiExhaustedBarrelBlockEntity;
import com.example.akaishi.block.entity.AkaishiMechanicalTemplateFactoryBlockEntity;
import com.example.akaishi.block.entity.AkaishiMechanicalProcessingFactoryBlockEntity;
import com.example.akaishi.block.entity.AkaishiMechanicalAssemblyStationBlockEntity;
import com.example.akaishi.block.entity.AkaishiEnergyGeneratorBlockEntity;
import com.example.akaishi.block.entity.AkaishiEnergyLiquefierBlockEntity;
import com.example.akaishi.block.entity.AkaishiEnergyProcessorBlockEntity;
import com.example.akaishi.block.entity.AkaishiFuelCannerBlockEntity;
import com.example.akaishi.block.entity.AkaishiFuelMixerBlockEntity;
import com.example.akaishi.block.entity.AkaishiFluidTankBlockEntity;
import com.example.akaishi.block.entity.AkaishiEquipmentForgerBlockEntity;
import com.example.akaishi.block.entity.AkaishiGeneAnalyzerBlockEntity;
import com.example.akaishi.block.entity.AkaishiGenMatrixControllerBlockEntity;
import com.example.akaishi.block.entity.AkaishiCultivatorBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifeStructBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifeBreederBlockEntity;
import com.example.akaishi.block.entity.AkaishiTraitReforgerBlockEntity;
import com.example.akaishi.block.entity.AkaishiTransgeneFactoryBlockEntity;
import com.example.akaishi.block.entity.AkaishiSurgeryBlockEntity;
import com.example.akaishi.block.entity.AkaishiPotionTableBlockEntity;
import com.example.akaishi.block.entity.AkaishiOrganVaultBlockEntity;
import com.example.akaishi.block.entity.AkaishiPotionCabinetBlockEntity;
import com.example.akaishi.block.entity.AkaishiSampleVaultBlockEntity;
import com.example.akaishi.block.entity.AkaishiDecayPurifierBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifeMatrixControllerBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifeActivatorBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifeCentrifugeBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifeEnergyCellBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifeEnergyCellSerializerBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifeEnergyEmitterBlockEntity;
import com.example.akaishi.block.entity.AkaishiMotherAltarBlockEntity;
import com.example.akaishi.block.entity.AkaishiItemReconstructorBlockEntity;
import com.example.akaishi.block.entity.AkaishiSingleSlotMachineBlockEntity;
import com.example.akaishi.block.entity.AkaishiPlantCultivatorBlockEntity;
import com.example.akaishi.block.entity.AkaishiCompressorBlockEntity;
import com.example.akaishi.block.entity.AkaishiPulverizerBlockEntity;
import com.example.akaishi.block.entity.AkaishiTransformerBlockEntity;
import com.example.akaishi.block.entity.AkaishiMinerControllerBlockEntity;
import com.example.akaishi.block.entity.AkaishiMinerPortBlockEntity;
import com.example.akaishi.block.entity.AkaishiMinerEnergyInputBlockEntity;
import com.example.akaishi.block.entity.AkaishiMinerItemOutputBlockEntity;
import com.example.akaishi.block.entity.AkaishiActivatedFractionatorBlockEntity;
import com.example.akaishi.block.entity.AkaishiFusionFuelAggregatorBlockEntity;
import com.example.akaishi.block.entity.AkaishiFusionItemInputPortBlockEntity;
import com.example.akaishi.block.entity.AkaishiFusionItemOutputPortBlockEntity;
import com.example.akaishi.block.entity.AkaishiFusionControllerBlockEntity;
import com.example.akaishi.block.entity.AkaishiFusionEnergyOutputBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifePurifierBlockEntity;
import com.example.akaishi.block.entity.AkaishiPlasmaFillerBlockEntity;
import com.example.akaishi.block.entity.AkaishiPlasmaTankBlockEntity;
import com.example.akaishi.block.entity.AkaishiReactorControllerBlockEntity;
import com.example.akaishi.block.entity.AkaishiReactorEnergyOutputBlockEntity;
import com.example.akaishi.block.entity.AkaishiReactorFuelPortBlockEntity;
import com.example.akaishi.block.entity.AkaishiPurifierBlockEntity;
import com.example.akaishi.block.entity.AkaishiPurifierMatrixControllerBlockEntity;
import com.example.akaishi.block.entity.AkaishiUpgradeStationBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifeFusionAnvilBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifeWirelessTerminalBlockEntity;
import com.example.akaishi.block.entity.AkaishiWirelessTerminalBlockEntity;
import com.example.akaishi.block.entity.AkaishiItemTerminalBlockEntity;
import com.example.akaishi.block.entity.MiniatureTerminalBlockEntity;
import com.example.akaishi.block.entity.AkaishiMiniMatrixTerminalBlockEntity;
import com.example.akaishi.block.entity.AkaishiMiniMatrixNetworkNodeBlockEntity;
import com.example.akaishi.block.entity.AkaishiItemPortBlockEntity;
import com.example.akaishi.block.entity.AkaishiItemStorageUnitBlockEntity;
import com.example.akaishi.wireless.IWirelessPortHost;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 菜单类型注册。
 * MenuType 必须注册到 Registers.MENU：Forge 发送打开界面数据包时按注册表编码 MenuType，
 * 未注册将无法打开界面。服务端/客户端通过同一工厂创建菜单：从网络缓冲读取方块坐标，再取方块实体数据。
 */
public final class ModMenus {

    /** 赤石提纯器菜单类型（注册完成前为 null，经 get() 取值） */
    public static RegistrySupplier<MenuType<AkaishiPurifierMenu>> CHISHI_PURIFIER;
    /** 赤能源储存单元菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiEnergyCellMenu>> CHISHI_ENERGY_CELL;
    /** 赤能源发生机菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiEnergyGeneratorMenu>> CHISHI_ENERGY_GENERATOR;
    /** 赤能源储存串联器（多方块主方块）菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiEnergyCellSerializerMenu>> CHISHI_ENERGY_CELL_SERIALIZER;
    /** 生命转换菜单类型（生命转换矩阵控制器） */
    public static RegistrySupplier<MenuType<AkaishiLifeConverterMenu>> CHISHI_LIFE_CONVERTER;
    /** 生命能量储存器（分级电池）菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiLifeEnergyCellMenu>> CHISHI_LIFE_ENERGY_CELL;
    /** 生命储存串联器（3×3×3 多方块主方块）菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiLifeEnergyCellSerializerMenu>> CHISHI_LIFE_ENERGY_CELL_SERIALIZER;
    /** 赤石能量聚合器菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiEnergyAggregatorMenu>> CHISHI_ENERGY_AGGREGATOR;
    /** 赤石装备打造器菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiEquipmentForgerMenu>> CHISHI_EQUIPMENT_FORGER;
    /** 赤红升级台菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiUpgradeStationMenu>> CHISHI_UPGRADE_STATION;
    /** 生命的融合砧菜单类型（赤石护甲 + 融合锭 → 生命融合护甲） */
    public static RegistrySupplier<MenuType<AkaishiLifeFusionAnvilMenu>> CHISHI_LIFE_FUSION_ANVIL;
    /** 自动收集器菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiAutoCollectorMenu>> CHISHI_AUTO_COLLECTOR;
    public static RegistrySupplier<MenuType<AkaishiCatalystMenu>> CHISHI_CATALYST;
    /** 生命能量提纯器菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiLifePurifierMenu>> CHISHI_LIFE_PURIFIER;
    /** 机械改造模板制造厂菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiMechanicalTemplateFactoryMenu>> CHISHI_MECHANICAL_TEMPLATE_FACTORY;
    /** 机械改造加工制作厂菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiMechanicalProcessingFactoryMenu>> CHISHI_MECHANICAL_PROCESSING_FACTORY;
    /** 机械改造组装加工台菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiMechanicalAssemblyStationMenu>> CHISHI_MECHANICAL_ASSEMBLY_STATION;
    /** 能量液化装置菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiEnergyLiquefierMenu>> CHISHI_ENERGY_LIQUEFIER;
    /** 能量加工器菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiEnergyProcessorMenu>> CHISHI_ENERGY_PROCESSOR;
    /** 燃料装罐机菜单 */
    public static RegistrySupplier<MenuType<AkaishiFuelCannerMenu>> CHISHI_FUEL_CANNER;
    /** 燃料混合器菜单 */
    public static RegistrySupplier<MenuType<AkaishiFuelMixerMenu>> CHISHI_FUEL_MIXER;
    /** 液体储罐菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiFluidTankMenu>> CHISHI_FLUID_TANK;
    /** 反应堆控制器菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiReactorControllerMenu>> CHISHI_REACTOR_CONTROLLER;
    /** 衰竭保存桶菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiExhaustedBarrelMenu>> CHISHI_EXHAUSTED_BARREL;
    /** 反应堆燃料投放口菜单类型（27 格燃料罐缓冲） */
    public static RegistrySupplier<MenuType<AkaishiReactorFuelPortMenu>> CHISHI_REACTOR_FUEL_PORT;
    /** 反应堆能量输出口菜单类型（能量缓冲展示） */
    public static RegistrySupplier<MenuType<AkaishiReactorEnergyOutputMenu>> CHISHI_REACTOR_ENERGY_OUTPUT;
    /** 躯体检查仪菜单类型（纯展示面板，无槽位） */
    public static RegistrySupplier<MenuType<AkaishiBodyScannerMenu>> CHISHI_BODY_SCANNER;
    /** 基因管理器菜单类型（纯管理面板，无槽位） */
    public static RegistrySupplier<MenuType<AkaishiGeneManagerMenu>> CHISHI_GENE_MANAGER;
    /** 生命分析台菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiGeneAnalyzerMenu>> CHISHI_GENE_ANALYZER;
    /** 部件培养舱菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiCultivatorMenu>> CHISHI_CULTIVATOR;
    /** 生命结构台菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiLifeStructMenu>> CHISHI_LIFE_STRUCT;
    /** 生命培育器菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiLifeBreederMenu>> CHISHI_LIFE_BREEDER;
    /** 词条重铸仪菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiTraitReforgerMenu>> CHISHI_TRAIT_REFORGER;
    /** 转基因工厂菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiTransgeneFactoryMenu>> CHISHI_TRANSGENE_FACTORY;
    /** 手术仓菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiSurgeryMenu>> CHISHI_SURGERY;
    /** 药剂台菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiPotionTableMenu>> CHISHI_POTION_TABLE;
    /** 器官储藏库菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiOrganVaultMenu>> CHISHI_ORGAN_VAULT;
    /** 药剂库菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiPotionCabinetMenu>> CHISHI_POTION_CABINET;
    /** 样本库菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiSampleVaultMenu>> CHISHI_SAMPLE_VAULT;
    /** 衰变净化塔菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiDecayPurifierMenu>> CHISHI_DECAY_PURIFIER;
    /** 发生器矩阵控制器菜单类型（低级/高级共用，等级由方块实例决定） */
    public static RegistrySupplier<MenuType<AkaishiGenMatrixControllerMenu>> CHISHI_GEN_MATRIX_CONTROLLER;
    /** 提纯矩阵控制器菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiPurifierMatrixControllerMenu>> CHISHI_PURIFIER_MATRIX_CONTROLLER;
    /** 生命活化器菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiLifeActivatorMenu>> CHISHI_LIFE_ACTIVATOR;
    /** 生命离心机菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiLifeCentrifugeMenu>> CHISHI_LIFE_CENTRIFUGE;
    /** 物品重构仪菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiItemReconstructorMenu>> CHISHI_ITEM_RECONSTRUCTOR;
    /** 赤石植物培养机菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiPlantCultivatorMenu>> CHISHI_PLANT_CULTIVATOR;
    /** 赤石压缩机菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiCompressorMenu>> CHISHI_COMPRESSOR;
    /** 赤石打粉机菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiPulverizerMenu>> CHISHI_PULVERIZER;
    /** 赤石变化器菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiTransformerMenu>> CHISHI_TRANSFORMER;
    /** 赤石矿机控制器菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiMinerControllerMenu>> CHISHI_MINER_CONTROLLER;
    /** 矿机转口菜单类型 */
    public static RegistrySupplier<MenuType<AkaishiMinerPortMenu>> CHISHI_MINER_PORT;
    /** 矿机能量输入口菜单（能量缓冲展示） */
    public static RegistrySupplier<MenuType<AkaishiMinerEnergyInputMenu>> CHISHI_MINER_ENERGY_INPUT;
    /** 矿机物品输出口菜单（产物缓冲展示） */
    public static RegistrySupplier<MenuType<AkaishiMinerItemOutputMenu>> CHISHI_MINER_ITEM_OUTPUT;
    /** 活化分馏器（活化结晶深度拆分） */
    public static RegistrySupplier<MenuType<AkaishiActivatedFractionatorMenu>> CHISHI_ACTIVATED_FRACTIONATOR;
    /** 聚变燃料聚合器（活化成分 → 等离子体） */
    public static RegistrySupplier<MenuType<AkaishiFusionFuelAggregatorMenu>> CHISHI_FUSION_FUEL_AGGREGATOR;
    /** 离子体填装器（等离子体 + 反应棒 → 燃料棒） */
    public static RegistrySupplier<MenuType<AkaishiPlasmaFillerMenu>> CHISHI_PLASMA_FILLER;
    /** 等离子体燃料储罐（仅存储等离子体，复用液体储罐界面） */
    public static RegistrySupplier<MenuType<AkaishiFluidTankMenu>> CHISHI_PLASMA_TANK;
    /** 无线赤能源终端菜单类型（终端方块主界面，四页互斥） */
    public static RegistrySupplier<MenuType<AkaishiWirelessTerminalMenu>> CHISHI_WIRELESS_TERMINAL;
    /** 无线赤能源输入口/输出口菜单类型（共用） */
    public static RegistrySupplier<MenuType<AkaishiWirelessPortMenu>> CHISHI_WIRELESS_PORT;
    /** 无线能源便捷终端菜单类型（手持物品，无方块实体） */
    public static RegistrySupplier<MenuType<AkaishiWirelessPortableTerminalMenu>> CHISHI_WIRELESS_PORTABLE_TERMINAL;
    /** 无线生命能量终端菜单类型（终端方块主界面，四页互斥，赤版生命镜像） */
    public static RegistrySupplier<MenuType<AkaishiLifeWirelessTerminalMenu>> CHISHI_LIFE_WIRELESS_TERMINAL;
    /** 无线生命能量输入口/输出口菜单类型（共用，赤版生命镜像） */
    public static RegistrySupplier<MenuType<AkaishiLifeWirelessPortMenu>> CHISHI_LIFE_WIRELESS_PORT;
    /** 无线生命便捷终端菜单类型（手持物品，无方块实体，赤版生命镜像） */
    public static RegistrySupplier<MenuType<AkaishiLifeWirelessPortableTerminalMenu>> CHISHI_LIFE_WIRELESS_PORTABLE_TERMINAL;
    /** 聚变控制器菜单类型（三页：运行情况/燃料/热量） */
    public static RegistrySupplier<MenuType<AkaishiFusionControllerMenu>> CHISHI_FUSION_CONTROLLER;
    /** 聚变物品输入/输出口菜单类型（共用，27 槽缓冲） */
    public static RegistrySupplier<MenuType<AkaishiFusionItemPortMenu>> CHISHI_FUSION_ITEM_PORT;
    /** 聚变能量输出口菜单类型（能量缓冲展示） */
    public static RegistrySupplier<MenuType<AkaishiFusionEnergyOutputMenu>> CHISHI_FUSION_ENERGY_OUTPUT;
    /** 生命能量发射器菜单类型（能量条 + 绑定坐标/射程展示） */
    public static RegistrySupplier<MenuType<AkaishiLifeEnergyEmitterMenu>> CHISHI_LIFE_ENERGY_EMITTER;
    /** 合并母神祭坛菜单类型（结构等级展示 + 单物品供奉槽） */
    public static RegistrySupplier<MenuType<AkaishiMotherAltarMenu>> CHISHI_MOTHER_ALTAR;
    /** 物品终端菜单类型（库页为可滚动聚合列表：4×9 可视区 + 客户端只读虚拟槽） */
    public static RegistrySupplier<MenuType<AkaishiItemTerminalMenu>> CHISHI_ITEM_TERMINAL;
    /** 物品储存单元菜单类型（D18 只读视图：54 槽展示 + 占用/剩余 IP） */
    public static RegistrySupplier<MenuType<AkaishiItemStorageUnitMenu>> CHISHI_ITEM_STORAGE_UNIT;
    /** 储存无线输入口/输出口菜单类型（共用，两页：运行 / 远程绑定） */
    public static RegistrySupplier<MenuType<AkaishiItemPortMenu>> CHISHI_ITEM_PORT;
    /** 微缩矩阵终端菜单类型（三页：芯片列表 / 升级装配 / 安全认证） */
    public static RegistrySupplier<MenuType<AkaishiMiniMatrixTerminalMenu>> CHISHI_MINI_MATRIX_TERMINAL;
    /** 网络节点菜单类型（只读绑定信息 + 本节点屏障开关；176×112，无背包区） */
    public static RegistrySupplier<MenuType<AkaishiMiniMatrixNodeMenu>> CHISHI_MINI_MATRIX_NODE;
    /** 禁忌秘典菜单类型（手持物品界面，无任何槽位；进度与条件由 S2C 快照下发） */
    public static RegistrySupplier<MenuType<AkaishiCodexMenu>> CHISHI_CODEX;

    private ModMenus() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register() {
        // MenuType 实例可在构造期直接创建（仅封装容器工厂，不触碰注册表，此时注册表未冻结）
        MenuType<AkaishiPurifierMenu> type = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiPurifierBlockEntity purifier) {
                return new AkaishiPurifierMenu(syncId, inv, purifier.inventory(), purifier.data());
            }
            // 方块实体缺失（如跨维度/距离过远）时使用空数据兜底，避免崩溃
            return new AkaishiPurifierMenu(syncId, inv,
                    new SimpleContainer(AkaishiPurifierBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiPurifierBlockEntity.DATA_SLOTS));
        });
        // 注册表延迟注册同一实例（supplier 在 RegisterEvent 时才求值）：
        // Forge 发送打开界面数据包时按注册表编码 MenuType，未注册将无法打开界面
        CHISHI_PURIFIER = (RegistrySupplier<MenuType<AkaishiPurifierMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_purifier"), () -> type);
        // 客户端注册界面工厂：实例已就绪，无需等待注册求值，避免构造期 get() 取值 NPE
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(type, AkaishiPurifierScreen::new));

        // 赤能源储存单元：1 便携单元充能槽 + 能量数据同步
        MenuType<AkaishiEnergyCellMenu> cellType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiEnergyCellBlockEntity cell) {
                return new AkaishiEnergyCellMenu(syncId, inv, cell.cellSlot(), cell.data());
            }
            return AkaishiEnergyCellMenu.emptyMenu(syncId, inv);
        });
        CHISHI_ENERGY_CELL = (RegistrySupplier<MenuType<AkaishiEnergyCellMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_cell"), () -> cellType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(cellType, AkaishiEnergyCellScreen::new));

        // 赤能源发生机：1 燃料槽 + 能量同步
        MenuType<AkaishiEnergyGeneratorMenu> genType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiEnergyGeneratorBlockEntity gen) {
                return new AkaishiEnergyGeneratorMenu(syncId, inv, gen);
            }
            return new AkaishiEnergyGeneratorMenu(syncId, inv,
                    new SimpleContainer(AkaishiEnergyGeneratorBlockEntity.SLOT_COUNT),
                    // 占位尺寸需与服务端一致：能量/燃烧/总燃烧（各 2 槽）+ 升级数 共 7 槽
                    new SimpleContainerData(AkaishiEnergyGeneratorBlockEntity.DATA_SLOTS));
        });
        CHISHI_ENERGY_GENERATOR = (RegistrySupplier<MenuType<AkaishiEnergyGeneratorMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_generator"), () -> genType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(genType, AkaishiEnergyGeneratorScreen::new));

        // 赤能源储存串联器：无容器槽位，同步总能量/总容量（long 4 槽）+ 结构状态
        MenuType<AkaishiEnergyCellSerializerMenu> serializerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiEnergyCellSerializerBlockEntity serializer) {
                return new AkaishiEnergyCellSerializerMenu(syncId, inv, serializer.data());
            }
            return AkaishiEnergyCellSerializerMenu.emptyMenu(syncId, inv);
        });
        CHISHI_ENERGY_CELL_SERIALIZER = (RegistrySupplier<MenuType<AkaishiEnergyCellSerializerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_cell_serializer"), () -> serializerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(serializerType, AkaishiEnergyCellSerializerScreen::new));

        // 生命转换（生命转换矩阵控制器）：无容器槽位，同步赤能源+生命能量+结构状态
        MenuType<AkaishiLifeConverterMenu> lifeType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiLifeMatrixControllerBlockEntity controller) {
                return new AkaishiLifeConverterMenu(syncId, inv, controller.data());
            }
            return AkaishiLifeConverterMenu.emptyMenu(syncId, inv);
        });
        CHISHI_LIFE_CONVERTER = (RegistrySupplier<MenuType<AkaishiLifeConverterMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_converter"), () -> lifeType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(lifeType, AkaishiLifeConverterScreen::new));

        // 生命能量储存器（单方块电池）：无机器槽位，同步生命能量/容量（long 4 槽）
        MenuType<AkaishiLifeEnergyCellMenu> lifeCellType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiLifeEnergyCellBlockEntity cell) {
                return new AkaishiLifeEnergyCellMenu(syncId, inv, cell.data());
            }
            return AkaishiLifeEnergyCellMenu.emptyMenu(syncId, inv);
        });
        CHISHI_LIFE_ENERGY_CELL = (RegistrySupplier<MenuType<AkaishiLifeEnergyCellMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_energy_cell"), () -> lifeCellType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(lifeCellType, AkaishiLifeEnergyCellScreen::new));

        // 生命储存串联器：无容器槽位，同步总生命能量/总容量（long 4 槽）+ 结构状态
        MenuType<AkaishiLifeEnergyCellSerializerMenu> lifeSerializerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiLifeEnergyCellSerializerBlockEntity serializer) {
                return new AkaishiLifeEnergyCellSerializerMenu(syncId, inv, serializer.data());
            }
            return AkaishiLifeEnergyCellSerializerMenu.emptyMenu(syncId, inv);
        });
        CHISHI_LIFE_ENERGY_CELL_SERIALIZER = (RegistrySupplier<MenuType<AkaishiLifeEnergyCellSerializerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_energy_cell_serializer"), () -> lifeSerializerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(lifeSerializerType, AkaishiLifeEnergyCellSerializerScreen::new));

        // 生命能量发射器：无机器槽位，同步生命能量/容量（long 4 槽）+ 绑定坐标
        MenuType<AkaishiLifeEnergyEmitterMenu> lifeEmitterType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiLifeEnergyEmitterBlockEntity emitter) {
                return new AkaishiLifeEnergyEmitterMenu(syncId, inv, emitter.data(), emitter.getTarget());
            }
            return AkaishiLifeEnergyEmitterMenu.emptyMenu(syncId, inv);
        });
        CHISHI_LIFE_ENERGY_EMITTER = (RegistrySupplier<MenuType<AkaishiLifeEnergyEmitterMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_energy_emitter"), () -> lifeEmitterType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(lifeEmitterType, AkaishiLifeEnergyEmitterScreen::new));

        // 赤石能量聚合器
        MenuType<AkaishiEnergyAggregatorMenu> aggregatorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            return level.getBlockEntity(pos) instanceof AkaishiEnergyAggregatorBlockEntity be
                    ? new AkaishiEnergyAggregatorMenu(syncId, inv, be)
                    : new AkaishiEnergyAggregatorMenu(syncId, inv, new net.minecraft.world.SimpleContainer(2),
                            new net.minecraft.world.inventory.SimpleContainerData(AkaishiEnergyAggregatorBlockEntity.DATA_SLOTS));
        });
        CHISHI_ENERGY_AGGREGATOR = (RegistrySupplier<MenuType<AkaishiEnergyAggregatorMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_aggregator"), () -> aggregatorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(aggregatorType, AkaishiEnergyAggregatorScreen::new));

        // 赤石装备打造器
        MenuType<AkaishiEquipmentForgerMenu> forgerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            return level.getBlockEntity(pos) instanceof AkaishiEquipmentForgerBlockEntity be
                    ? new AkaishiEquipmentForgerMenu(syncId, inv, be)
                    // 占位尺寸需与服务端一致：能量/容量各占高低两槽，共 DATA_SIZE=12 槽
                    : new AkaishiEquipmentForgerMenu(syncId, inv,
                            new net.minecraft.world.SimpleContainer(AkaishiEquipmentForgerBlockEntity.SLOT_COUNT),
                            new net.minecraft.world.inventory.SimpleContainerData(AkaishiEquipmentForgerBlockEntity.DATA_SIZE));
        });
        CHISHI_EQUIPMENT_FORGER = (RegistrySupplier<MenuType<AkaishiEquipmentForgerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_equipment_forger"), () -> forgerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(forgerType, AkaishiEquipmentForgerScreen::new));

        // 赤红升级台
        MenuType<AkaishiUpgradeStationMenu> upgradeType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            return level.getBlockEntity(pos) instanceof AkaishiUpgradeStationBlockEntity be
                    ? new AkaishiUpgradeStationMenu(syncId, inv, be)
                    : new AkaishiUpgradeStationMenu(syncId, inv, null);
        });
        CHISHI_UPGRADE_STATION = (RegistrySupplier<MenuType<AkaishiUpgradeStationMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_upgrade_station"), () -> upgradeType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(upgradeType, AkaishiUpgradeStationScreen::new));

        // 生命的融合砧：赤石护甲 + 融合锭 → 生命融合护甲（无能量/进度数据，纯槽位合成）
        MenuType<AkaishiLifeFusionAnvilMenu> fusionAnvilType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            return level.getBlockEntity(pos) instanceof AkaishiLifeFusionAnvilBlockEntity anvil
                    ? new AkaishiLifeFusionAnvilMenu(syncId, inv, anvil)
                    : new AkaishiLifeFusionAnvilMenu(syncId, inv, null);
        });
        CHISHI_LIFE_FUSION_ANVIL = (RegistrySupplier<MenuType<AkaishiLifeFusionAnvilMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_fusion_anvil"), () -> fusionAnvilType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(fusionAnvilType, AkaishiLifeFusionAnvilScreen::new));

        // 自动收集器：27 槽存储 + 能量/进度同步
        MenuType<AkaishiAutoCollectorMenu> collectorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            return level.getBlockEntity(pos) instanceof AkaishiAutoCollectorBlockEntity be
                    ? new AkaishiAutoCollectorMenu(syncId, inv, be)
                    : new AkaishiAutoCollectorMenu(syncId, inv,
                            new net.minecraft.world.SimpleContainer(AkaishiAutoCollectorBlockEntity.STORAGE_SIZE),
                            new net.minecraft.world.inventory.SimpleContainerData(AkaishiAutoCollectorBlockEntity.DATA_SLOTS));
        });
        CHISHI_AUTO_COLLECTOR = (RegistrySupplier<MenuType<AkaishiAutoCollectorMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_auto_collector"), () -> collectorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(collectorType, AkaishiAutoCollectorScreen::new));

        // 赤石催化器：无机器槽，仅玩家背包 + 能量/工作状态数据同步
        MenuType<AkaishiCatalystMenu> catalystType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            return level.getBlockEntity(pos) instanceof AkaishiCatalystBlockEntity be
                    ? new AkaishiCatalystMenu(syncId, inv, be.data())
                    : new AkaishiCatalystMenu(syncId, inv,
                            new net.minecraft.world.inventory.SimpleContainerData(AkaishiCatalystBlockEntity.DATA_SLOTS));
        });
        CHISHI_CATALYST = (RegistrySupplier<MenuType<AkaishiCatalystMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_catalyst"), () -> catalystType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(catalystType, AkaishiCatalystScreen::new));

        // 生命能量提纯器：1 输出槽 + 赤能源/生命能量/进度数据同步
        MenuType<AkaishiLifePurifierMenu> lifePurifierType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiLifePurifierBlockEntity purifier) {
                return new AkaishiLifePurifierMenu(syncId, inv, purifier.inventory(), purifier.data(),
                        purifier.getUpgradeSlots());
            }
            return new AkaishiLifePurifierMenu(syncId, inv,
                    new SimpleContainer(AkaishiLifePurifierBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiLifePurifierBlockEntity.DATA_SLOTS));
        });
        CHISHI_LIFE_PURIFIER = (RegistrySupplier<MenuType<AkaishiLifePurifierMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_purifier"), () -> lifePurifierType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(lifePurifierType, AkaishiLifePurifierScreen::new));

        // 能量液化装置：1 输入槽（下界之星/凋零玫瑰）+ 赤能源/双液体罐/进度数据同步
        MenuType<AkaishiEnergyLiquefierMenu> liquefierType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiEnergyLiquefierBlockEntity liquefier) {
                return new AkaishiEnergyLiquefierMenu(syncId, inv, liquefier);
            }
            return new AkaishiEnergyLiquefierMenu(syncId, inv,
                    new SimpleContainer(AkaishiEnergyLiquefierBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiEnergyLiquefierBlockEntity.DATA_SLOTS),
                    new MachineUpgradeSlots());
        });
        CHISHI_ENERGY_LIQUEFIER = (RegistrySupplier<MenuType<AkaishiEnergyLiquefierMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_liquefier"), () -> liquefierType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(liquefierType, AkaishiEnergyLiquefierScreen::new));

        // 能量加工器：1 输入槽（生命固态物）+ 赤能源/双输入罐/双输出罐/进度数据同步
        MenuType<AkaishiEnergyProcessorMenu> processorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiEnergyProcessorBlockEntity processor) {
                return new AkaishiEnergyProcessorMenu(syncId, inv, processor);
            }
            return new AkaishiEnergyProcessorMenu(syncId, inv,
                    new SimpleContainer(AkaishiEnergyProcessorBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiEnergyProcessorBlockEntity.DATA_SLOTS),
                    new MachineUpgradeSlots());
        });
        CHISHI_ENERGY_PROCESSOR = (RegistrySupplier<MenuType<AkaishiEnergyProcessorMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_processor"), () -> processorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(processorType, AkaishiEnergyProcessorScreen::new));

        // 燃料装罐机：1 空罐输入槽 + 1 满罐输出槽 + 输入液体量数据
        MenuType<AkaishiFuelCannerMenu> cannerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            String fuelId = buf.readUtf();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiFuelCannerBlockEntity canner) {
                return new AkaishiFuelCannerMenu(syncId, inv, canner, fuelId);
            }
            return new AkaishiFuelCannerMenu(syncId, inv,
                    new SimpleContainer(AkaishiFuelCannerBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiFuelCannerBlockEntity.DATA_SLOTS), fuelId);
        });
        CHISHI_FUEL_CANNER = (RegistrySupplier<MenuType<AkaishiFuelCannerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fuel_canner"), () -> cannerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(cannerType, AkaishiFuelCannerScreen::new));

        // 燃料混合器：无机器槽位（纯液体调和），9 数据槽（能量/双输入/输出/进度）
        MenuType<AkaishiFuelMixerMenu> mixerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiFuelMixerBlockEntity mixer) {
                return new AkaishiFuelMixerMenu(syncId, inv, mixer);
            }
            return new AkaishiFuelMixerMenu(syncId, inv,
                    new SimpleContainerData(AkaishiFuelMixerBlockEntity.DATA_SLOTS),
                    new MachineUpgradeSlots());
        });
        CHISHI_FUEL_MIXER = (RegistrySupplier<MenuType<AkaishiFuelMixerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fuel_mixer"), () -> mixerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(mixerType, AkaishiFuelMixerScreen::new));

        // 液体储罐：无机器槽位，仅液体量/容量数据展示
        MenuType<AkaishiFluidTankMenu> tankType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiFluidTankBlockEntity tank) {
                return new AkaishiFluidTankMenu(syncId, inv, tank.data());
            }
            return AkaishiFluidTankMenu.emptyMenu(syncId, inv);
        });
        CHISHI_FLUID_TANK = (RegistrySupplier<MenuType<AkaishiFluidTankMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fluid_tank"), () -> tankType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(tankType, AkaishiFluidTankScreen::new));

        // 等离子体燃料储罐：无机器槽位，复用液体储罐菜单与界面（独立菜单类型）
        MenuType<AkaishiFluidTankMenu> plasmaTankType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiPlasmaTankBlockEntity tank) {
                return new AkaishiFluidTankMenu(ModMenus.CHISHI_PLASMA_TANK.get(), syncId, inv, tank.data());
            }
            return new AkaishiFluidTankMenu(ModMenus.CHISHI_PLASMA_TANK.get(), syncId, inv,
                    new SimpleContainerData(AkaishiPlasmaTankBlockEntity.DATA_SLOTS));
        });
        CHISHI_PLASMA_TANK = (RegistrySupplier<MenuType<AkaishiFluidTankMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_plasma_tank"), () -> plasmaTankType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(plasmaTankType, AkaishiFluidTankScreen::new));

        // 反应堆控制器：10 燃料槽 + 13 数据槽（温度/成型/散热/废品/熔毁等）
        MenuType<AkaishiReactorControllerMenu> reactorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiReactorControllerBlockEntity controller) {
                return new AkaishiReactorControllerMenu(syncId, inv, controller);
            }
            return new AkaishiReactorControllerMenu(syncId, inv,
                    new SimpleContainer(AkaishiReactorControllerBlockEntity.MAX_FUEL_SLOTS),
                    new SimpleContainerData(AkaishiReactorControllerBlockEntity.DATA_SLOTS));
        });
        CHISHI_REACTOR_CONTROLLER = (RegistrySupplier<MenuType<AkaishiReactorControllerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_reactor_controller"), () -> reactorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(reactorType, AkaishiReactorControllerScreen::new));

        // 衰竭保存桶：无机器槽位，仅液体量/容量数据展示
        MenuType<AkaishiExhaustedBarrelMenu> barrelType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiExhaustedBarrelBlockEntity barrel) {
                return new AkaishiExhaustedBarrelMenu(syncId, inv, barrel.data());
            }
            return AkaishiExhaustedBarrelMenu.emptyMenu(syncId, inv);
        });
        CHISHI_EXHAUSTED_BARREL = (RegistrySupplier<MenuType<AkaishiExhaustedBarrelMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_exhausted_barrel"), () -> barrelType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(barrelType, AkaishiExhaustedBarrelScreen::new));

        // 反应堆燃料投放口：27 格燃料罐缓冲槽 + 玩家背包
        MenuType<AkaishiReactorFuelPortMenu> fuelPortType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            if (level.getBlockEntity(pos) instanceof AkaishiReactorFuelPortBlockEntity port) {
                return new AkaishiReactorFuelPortMenu(syncId, inv, port.buffer());
            }
            return AkaishiReactorFuelPortMenu.emptyMenu(syncId, inv);
        });
        CHISHI_REACTOR_FUEL_PORT = (RegistrySupplier<MenuType<AkaishiReactorFuelPortMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_reactor_fuel_port"), () -> fuelPortType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(fuelPortType, AkaishiReactorFuelPortScreen::new));

        // 反应堆能量输出口：无机器槽位，能量/容量数据展示
        MenuType<AkaishiReactorEnergyOutputMenu> energyOutputType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            if (level.getBlockEntity(pos) instanceof AkaishiReactorEnergyOutputBlockEntity output) {
                return new AkaishiReactorEnergyOutputMenu(syncId, inv, output.data());
            }
            return AkaishiReactorEnergyOutputMenu.emptyMenu(syncId, inv);
        });
        CHISHI_REACTOR_ENERGY_OUTPUT = (RegistrySupplier<MenuType<AkaishiReactorEnergyOutputMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_reactor_energy_output"), () -> energyOutputType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(energyOutputType, AkaishiReactorEnergyOutputScreen::new));

        // 躯体检查仪：无机器槽位，纯展示玩家躯体状态（数据由 S2C 同步包推送）
        MenuType<AkaishiBodyScannerMenu> bodyScannerType = MenuRegistry.ofExtended((syncId, inv, buf) ->
                new AkaishiBodyScannerMenu(syncId, inv));
        CHISHI_BODY_SCANNER = (RegistrySupplier<MenuType<AkaishiBodyScannerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_body_scanner"), () -> bodyScannerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(bodyScannerType, AkaishiBodyScannerScreen::new));

        // 基因管理器：无机器槽位，展示/卸载已吸收基因强化（数据 S2C 推送，卸载走 C2S）
        // extraData 携带方块坐标（BE.saveExtraData 写入），客户端据此还原菜单绑定方块
        MenuType<AkaishiGeneManagerMenu> geneManagerType = MenuRegistry.ofExtended((syncId, inv, buf) ->
                new AkaishiGeneManagerMenu(syncId, inv, buf.readBlockPos()));
        CHISHI_GENE_MANAGER = (RegistrySupplier<MenuType<AkaishiGeneManagerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_gene_manager"), () -> geneManagerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(geneManagerType, AkaishiGeneManagerScreen::new));

        // 生命分析台：输入（纯度 100 样本）+ 输出（基因序列片段）+ 生命能量/进度数据
        MenuType<AkaishiGeneAnalyzerMenu> geneAnalyzerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiGeneAnalyzerBlockEntity analyzer) {
                return new AkaishiGeneAnalyzerMenu(syncId, inv, analyzer.inventory(), analyzer.data(), analyzer.getUpgradeSlots(), pos);
            }
            return new AkaishiGeneAnalyzerMenu(syncId, inv,
                    new SimpleContainer(AkaishiGeneAnalyzerBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiGeneAnalyzerBlockEntity.DATA_SLOTS),
                    new MachineUpgradeSlots(), pos);
        });
        CHISHI_GENE_ANALYZER = (RegistrySupplier<MenuType<AkaishiGeneAnalyzerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_gene_analyzer"), () -> geneAnalyzerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(geneAnalyzerType, AkaishiGeneAnalyzerScreen::new));

        // 部件培养舱：输入（样本/器官）+ 材料（固态物）+ 生命能量/进度/模式数据
        MenuType<AkaishiCultivatorMenu> cultivatorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiCultivatorBlockEntity cultivator) {
                return new AkaishiCultivatorMenu(syncId, inv, cultivator.inventory(), cultivator.data(),
                        cultivator.getUpgradeSlots(), pos);
            }
            return new AkaishiCultivatorMenu(syncId, inv,
                    new SimpleContainer(AkaishiCultivatorBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiCultivatorBlockEntity.DATA_SLOTS),
                    new MachineUpgradeSlots(), pos);
        });
        CHISHI_CULTIVATOR = (RegistrySupplier<MenuType<AkaishiCultivatorMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_cultivator"), () -> cultivatorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(cultivatorType, AkaishiCultivatorScreen::new));

        // 生命结构台：基因序列 + 固态物 → 器官（目标槽位界面选择）
        MenuType<AkaishiLifeStructMenu> lifeStructType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiLifeStructBlockEntity struct) {
                return new AkaishiLifeStructMenu(syncId, inv, struct.inventory(), struct.data(),
                        struct.getUpgradeSlots(), struct.getBlockPos());
            }
            return new AkaishiLifeStructMenu(syncId, inv,
                    new SimpleContainer(AkaishiLifeStructBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiLifeStructBlockEntity.DATA_SLOTS),
                    new MachineUpgradeSlots(), null);
        });
        CHISHI_LIFE_STRUCT = (RegistrySupplier<MenuType<AkaishiLifeStructMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_struct"), () -> lifeStructType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(lifeStructType, AkaishiLifeStructScreen::new));

        // 生命培育器：器官 + 同源基因序列 + 衰竭结晶 → 突变器官（纯度决定成功率）
        MenuType<AkaishiLifeBreederMenu> breederType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiLifeBreederBlockEntity breeder) {
                return new AkaishiLifeBreederMenu(syncId, inv, breeder.inventory(), breeder.data(),
                        breeder.getUpgradeSlots(), breeder.getBlockPos());
            }
            return new AkaishiLifeBreederMenu(syncId, inv,
                    new SimpleContainer(AkaishiLifeBreederBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiLifeBreederBlockEntity.DATA_SLOTS),
                    new MachineUpgradeSlots(), pos);
        });
        CHISHI_LIFE_BREEDER = (RegistrySupplier<MenuType<AkaishiLifeBreederMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_breeder"), () -> breederType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(breederType, AkaishiLifeBreederScreen::new));

        // 词条重铸仪：器官 + 衰竭结晶 → 原位替换指定第 N 条突变词条（确定性必成）
        MenuType<AkaishiTraitReforgerMenu> traitReforgerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiTraitReforgerBlockEntity reforger) {
                return new AkaishiTraitReforgerMenu(syncId, inv, reforger.inventory(), reforger.data(),
                        reforger.getUpgradeSlots(), reforger.getBlockPos());
            }
            return new AkaishiTraitReforgerMenu(syncId, inv,
                    new SimpleContainer(AkaishiTraitReforgerBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiTraitReforgerBlockEntity.DATA_SLOTS),
                    new MachineUpgradeSlots(), pos);
        });
        CHISHI_TRAIT_REFORGER = (RegistrySupplier<MenuType<AkaishiTraitReforgerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_trait_reforger"), () -> traitReforgerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(traitReforgerType, AkaishiTraitReforgerScreen::new));

        // 转基因工厂：凋零骷髅基因（纯度≥50）+ 缠怨藤 + 凋零玫瑰 + 固态物 → 凋零藤
        MenuType<AkaishiTransgeneFactoryMenu> transgeneFactoryType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiTransgeneFactoryBlockEntity factory) {
                return new AkaishiTransgeneFactoryMenu(syncId, inv, factory.inventory(), factory.data());
            }
            return new AkaishiTransgeneFactoryMenu(syncId, inv);
        });
        CHISHI_TRANSGENE_FACTORY = (RegistrySupplier<MenuType<AkaishiTransgeneFactoryMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_transgene_factory"), () -> transgeneFactoryType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(transgeneFactoryType, AkaishiTransgeneFactoryScreen::new));

        // 手术仓：器官移植/摘除（消耗固态 + 生命能量，带进度）
        MenuType<AkaishiSurgeryMenu> surgeryType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiSurgeryBlockEntity surgery) {
                return new AkaishiSurgeryMenu(syncId, inv, surgery.inventory(), surgery.data(),
                        surgery.getUpgradeSlots(), surgery.getBlockPos());
            }
            return new AkaishiSurgeryMenu(syncId, inv,
                    new SimpleContainer(AkaishiSurgeryBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiSurgeryBlockEntity.DATA_SLOTS),
                    new MachineUpgradeSlots(), pos);
        });
        CHISHI_SURGERY = (RegistrySupplier<MenuType<AkaishiSurgeryMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_surgery"), () -> surgeryType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(surgeryType, AkaishiSurgeryScreen::new));

        // 药剂台：样本+固态+生命能量 → 永久/突破药剂（模板选择走 C2S 包）
        MenuType<AkaishiPotionTableMenu> potionTableType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiPotionTableBlockEntity potionTable) {
                return new AkaishiPotionTableMenu(syncId, inv, potionTable.inventory(), potionTable.data(),
                        potionTable.getUpgradeSlots(), potionTable.getBlockPos());
            }
            return new AkaishiPotionTableMenu(syncId, inv,
                    new SimpleContainer(AkaishiPotionTableBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiPotionTableBlockEntity.DATA_SLOTS),
                    new MachineUpgradeSlots(), pos);
        });
        CHISHI_POTION_TABLE = (RegistrySupplier<MenuType<AkaishiPotionTableMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_potion_table"), () -> potionTableType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(potionTableType, AkaishiPotionTableScreen::new));

        // 器官储藏库：按躯体槽位分页的器官仓库（选页为客户端本地状态，无需网络包）
        MenuType<AkaishiOrganVaultMenu> organVaultType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiOrganVaultBlockEntity vault) {
                return new AkaishiOrganVaultMenu(syncId, inv, vault);
            }
            return new AkaishiOrganVaultMenu(syncId, inv,
                    new SimpleContainer(AkaishiOrganVaultBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiOrganVaultBlockEntity.DATA_SLOTS), pos);
        });
        CHISHI_ORGAN_VAULT = (RegistrySupplier<MenuType<AkaishiOrganVaultMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_organ_vault"), () -> organVaultType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(organVaultType, AkaishiOrganVaultScreen::new));

        // 药剂库：大容量药剂仓库（筛选为客户端本地状态，无需网络包）
        MenuType<AkaishiPotionCabinetMenu> potionCabinetType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiPotionCabinetBlockEntity cabinet) {
                return new AkaishiPotionCabinetMenu(syncId, inv, cabinet);
            }
            return new AkaishiPotionCabinetMenu(syncId, inv,
                    new SimpleContainer(AkaishiPotionCabinetBlockEntity.CABINET_SLOTS));
        });
        CHISHI_POTION_CABINET = (RegistrySupplier<MenuType<AkaishiPotionCabinetMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_potion_cabinet"), () -> potionCabinetType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(potionCabinetType, AkaishiPotionCabinetScreen::new));

        // 样本库：大容量样本仓库（同 NBT 自动合并）
        MenuType<AkaishiSampleVaultMenu> sampleVaultType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiSampleVaultBlockEntity vault) {
                return new AkaishiSampleVaultMenu(syncId, inv, vault);
            }
            return new AkaishiSampleVaultMenu(syncId, inv,
                    new SimpleContainer(AkaishiSampleVaultBlockEntity.SAMPLE_SLOTS));
        });
        CHISHI_SAMPLE_VAULT = (RegistrySupplier<MenuType<AkaishiSampleVaultMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_sample_vault"), () -> sampleVaultType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(sampleVaultType, AkaishiSampleVaultScreen::new));

        // 衰变净化塔：无机器槽位，升级槽（速度/能量）+ 4 数据槽（能量/容量/净化中/区域数）
        MenuType<AkaishiDecayPurifierMenu> decayPurifierType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiDecayPurifierBlockEntity purifier) {
                return new AkaishiDecayPurifierMenu(syncId, inv, purifier.data(), purifier.getUpgradeSlots());
            }
            return new AkaishiDecayPurifierMenu(syncId, inv,
                    new SimpleContainerData(AkaishiDecayPurifierBlockEntity.DATA_SLOTS),
                    new MachineUpgradeSlots());
        });
        CHISHI_DECAY_PURIFIER = (RegistrySupplier<MenuType<AkaishiDecayPurifierMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_decay_purifier"), () -> decayPurifierType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(decayPurifierType, AkaishiDecayPurifierScreen::new));

        // 发生器矩阵控制器：1 燃料槽 + 10 升级槽 + 8 数据槽（能量/燃烧/总量各低高两槽、成型、升级数）
        MenuType<AkaishiGenMatrixControllerMenu> genMatrixType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiGenMatrixControllerBlockEntity controller) {
                return new AkaishiGenMatrixControllerMenu(syncId, inv, controller);
            }
            return new AkaishiGenMatrixControllerMenu(syncId, inv,
                    new SimpleContainer(AkaishiGenMatrixControllerBlockEntity.TOTAL_SLOTS),
                    new SimpleContainerData(AkaishiGenMatrixControllerBlockEntity.DATA_SLOTS), AkaishiGenMatrixTier.BASIC);
        });
        CHISHI_GEN_MATRIX_CONTROLLER = (RegistrySupplier<MenuType<AkaishiGenMatrixControllerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_gen_matrix_controller"), () -> genMatrixType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(genMatrixType, AkaishiGenMatrixControllerScreen::new));

        // 提纯矩阵控制器：2 槽（输入/输出）+ 3 数据槽（能量/进度/成型）
        MenuType<AkaishiPurifierMatrixControllerMenu> purifierMatrixType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiPurifierMatrixControllerBlockEntity controller) {
                return new AkaishiPurifierMatrixControllerMenu(syncId, inv, controller);
            }
            return new AkaishiPurifierMatrixControllerMenu(syncId, inv,
                    new SimpleContainer(AkaishiPurifierMatrixControllerBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiPurifierMatrixControllerBlockEntity.DATA_SLOTS));
        });
        CHISHI_PURIFIER_MATRIX_CONTROLLER = (RegistrySupplier<MenuType<AkaishiPurifierMatrixControllerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_purifier_matrix_controller"), () -> purifierMatrixType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(purifierMatrixType, AkaishiPurifierMatrixControllerScreen::new));

        // 生命活化器：无机器槽位（纯液体无害化），7 数据槽（生命能量/输入/输出/累计活化量）
        MenuType<AkaishiLifeActivatorMenu> activatorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiLifeActivatorBlockEntity activator) {
                return new AkaishiLifeActivatorMenu(syncId, inv, activator);
            }
            return new AkaishiLifeActivatorMenu(syncId, inv,
                    new SimpleContainerData(AkaishiLifeActivatorBlockEntity.DATA_SLOTS));
        });
        CHISHI_LIFE_ACTIVATOR = (RegistrySupplier<MenuType<AkaishiLifeActivatorMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_activator"), () -> activatorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(activatorType, AkaishiLifeActivatorScreen::new));

        // 生命离心机：2 机器输出槽 + 5 数据槽（能量/罐量/进度）
        MenuType<AkaishiLifeCentrifugeMenu> centrifugeType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiLifeCentrifugeBlockEntity centrifuge) {
                return new AkaishiLifeCentrifugeMenu(syncId, inv, centrifuge);
            }
            return new AkaishiLifeCentrifugeMenu(syncId, inv, new SimpleContainer(2),
                    new SimpleContainerData(AkaishiLifeCentrifugeBlockEntity.DATA_SLOTS));
        });
        CHISHI_LIFE_CENTRIFUGE = (RegistrySupplier<MenuType<AkaishiLifeCentrifugeMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_centrifuge"), () -> centrifugeType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(centrifugeType, AkaishiLifeCentrifugeScreen::new));

        // 物品重构仪：3 机器槽（原料/结晶/产物）+ 5 数据槽（能量/进度/所需/结晶数）
        MenuType<AkaishiItemReconstructorMenu> reconstructorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiItemReconstructorBlockEntity reconstructor) {
                return new AkaishiItemReconstructorMenu(syncId, inv, reconstructor);
            }
            return new AkaishiItemReconstructorMenu(syncId, inv, new SimpleContainer(3),
                    new SimpleContainerData(AkaishiItemReconstructorBlockEntity.DATA_SLOTS));
        });
        CHISHI_ITEM_RECONSTRUCTOR = (RegistrySupplier<MenuType<AkaishiItemReconstructorMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_item_reconstructor"), () -> reconstructorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(reconstructorType, AkaishiItemReconstructorScreen::new));

        // 赤石植物培养机：2 机器槽（输入/输出）+ 4 数据槽（能量/容量/进度/总耗时）
        MenuType<AkaishiPlantCultivatorMenu> plantCultivatorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiPlantCultivatorBlockEntity cultivator) {
                return new AkaishiPlantCultivatorMenu(syncId, inv, cultivator);
            }
            return new AkaishiPlantCultivatorMenu(syncId, inv, new SimpleContainer(AkaishiSingleSlotMachineBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiSingleSlotMachineBlockEntity.DATA_SLOTS));
        });
        CHISHI_PLANT_CULTIVATOR = (RegistrySupplier<MenuType<AkaishiPlantCultivatorMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_plant_cultivator"), () -> plantCultivatorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(plantCultivatorType, AkaishiPlantCultivatorScreen::new));

        // 赤石压缩机：2 机器槽（输入/输出）+ 4 数据槽（能量/容量/进度/总耗时）
        MenuType<AkaishiCompressorMenu> compressorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiCompressorBlockEntity compressor) {
                return new AkaishiCompressorMenu(syncId, inv, compressor);
            }
            return new AkaishiCompressorMenu(syncId, inv, new SimpleContainer(AkaishiSingleSlotMachineBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiSingleSlotMachineBlockEntity.DATA_SLOTS));
        });
        CHISHI_COMPRESSOR = (RegistrySupplier<MenuType<AkaishiCompressorMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_compressor"), () -> compressorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(compressorType, AkaishiCompressorScreen::new));

        // 赤石打粉机：2 机器槽（输入/输出）+ 4 数据槽（能量/容量/进度/总耗时）
        MenuType<AkaishiPulverizerMenu> pulverizerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiPulverizerBlockEntity pulverizer) {
                return new AkaishiPulverizerMenu(syncId, inv, pulverizer);
            }
            return new AkaishiPulverizerMenu(syncId, inv, new SimpleContainer(AkaishiSingleSlotMachineBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiSingleSlotMachineBlockEntity.DATA_SLOTS));
        });
        CHISHI_PULVERIZER = (RegistrySupplier<MenuType<AkaishiPulverizerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_pulverizer"), () -> pulverizerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(pulverizerType, AkaishiPulverizerScreen::new));

        // 赤石变化器：2 机器槽（输入/输出）+ 4 数据槽（能量/容量/进度/总耗时）
        MenuType<AkaishiTransformerMenu> transformerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiTransformerBlockEntity transformer) {
                return new AkaishiTransformerMenu(syncId, inv, transformer);
            }
            return new AkaishiTransformerMenu(syncId, inv, new SimpleContainer(AkaishiSingleSlotMachineBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiSingleSlotMachineBlockEntity.DATA_SLOTS));
        });
        CHISHI_TRANSFORMER = (RegistrySupplier<MenuType<AkaishiTransformerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_transformer"), () -> transformerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(transformerType, AkaishiTransformerScreen::new));

        // 赤石矿机控制器：产物暂存 6 槽 + 8 数据槽（能量/容量/进度/总耗时/成型/3 类升级）
        MenuType<AkaishiMinerControllerMenu> minerControllerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiMinerControllerBlockEntity controller) {
                return new AkaishiMinerControllerMenu(syncId, inv, controller);
            }
            return new AkaishiMinerControllerMenu(syncId, inv,
                    new SimpleContainer(AkaishiMinerControllerBlockEntity.OUTPUT_SLOTS),
                    new SimpleContainerData(AkaishiMinerControllerBlockEntity.DATA_COUNT));
        });
        CHISHI_MINER_CONTROLLER = (RegistrySupplier<MenuType<AkaishiMinerControllerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_miner_controller"), () -> minerControllerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(minerControllerType, AkaishiMinerControllerScreen::new));

        // 矿机转口：产物缓冲 27 槽 + 5 数据槽（能量/容量高低两槽 + 成型）
        MenuType<AkaishiMinerPortMenu> minerPortType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiMinerPortBlockEntity port) {
                return new AkaishiMinerPortMenu(syncId, inv, port);
            }
            return new AkaishiMinerPortMenu(syncId, inv,
                    new SimpleContainer(AkaishiMinerPortBlockEntity.BUFFER_SLOTS),
                    new SimpleContainerData(AkaishiMinerPortBlockEntity.DATA_SLOTS));
        });
        CHISHI_MINER_PORT = (RegistrySupplier<MenuType<AkaishiMinerPortMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_miner_port"), () -> minerPortType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(minerPortType, AkaishiMinerPortScreen::new));

        // 矿机能量输入口：纯能量缓冲展示（无机器槽）+ 3 数据槽（能量/容量/成型）
        MenuType<AkaishiMinerEnergyInputMenu> energyInputType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiMinerEnergyInputBlockEntity port) {
                return new AkaishiMinerEnergyInputMenu(syncId, inv, port.data());
            }
            return AkaishiMinerEnergyInputMenu.emptyMenu(syncId, inv);
        });
        CHISHI_MINER_ENERGY_INPUT = (RegistrySupplier<MenuType<AkaishiMinerEnergyInputMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_miner_energy_input"), () -> energyInputType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(energyInputType, AkaishiMinerEnergyInputScreen::new));

        // 矿机物品输出口：产物缓冲 27 槽（只读）+ 3 数据槽（能量/容量/成型）
        MenuType<AkaishiMinerItemOutputMenu> itemOutputType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiMinerItemOutputBlockEntity port) {
                return new AkaishiMinerItemOutputMenu(syncId, inv, port);
            }
            return new AkaishiMinerItemOutputMenu(syncId, inv,
                    new SimpleContainer(AkaishiMinerItemOutputBlockEntity.BUFFER_SLOTS),
                    new SimpleContainerData(3));
        });
        CHISHI_MINER_ITEM_OUTPUT = (RegistrySupplier<MenuType<AkaishiMinerItemOutputMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_miner_item_output"), () -> itemOutputType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(itemOutputType, AkaishiMinerItemOutputScreen::new));

        // 活化分馏器：3 机器槽（输入活化结晶 + 2 只读输出槽）+ 3 数据槽（能量/进度）
        MenuType<AkaishiActivatedFractionatorMenu> fractionatorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiActivatedFractionatorBlockEntity fractionator) {
                return new AkaishiActivatedFractionatorMenu(syncId, inv, fractionator);
            }
            return new AkaishiActivatedFractionatorMenu(syncId, inv, new SimpleContainer(1),
                    new SimpleContainer(2), new SimpleContainerData(AkaishiActivatedFractionatorBlockEntity.DATA_SLOTS));
        });
        CHISHI_ACTIVATED_FRACTIONATOR = (RegistrySupplier<MenuType<AkaishiActivatedFractionatorMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_activated_fractionator"), () -> fractionatorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(fractionatorType, AkaishiActivatedFractionatorScreen::new));

        // 聚变燃料聚合器：1 活化成分输入槽 + 9 数据槽（能量/进度/3 等离子体罐量）
        MenuType<AkaishiFusionFuelAggregatorMenu> fusionAggregatorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiFusionFuelAggregatorBlockEntity aggregator) {
                return new AkaishiFusionFuelAggregatorMenu(syncId, inv, aggregator);
            }
            return new AkaishiFusionFuelAggregatorMenu(syncId, inv, new SimpleContainer(1),
                    new SimpleContainerData(AkaishiFusionFuelAggregatorBlockEntity.DATA_SLOTS));
        });
        CHISHI_FUSION_FUEL_AGGREGATOR = (RegistrySupplier<MenuType<AkaishiFusionFuelAggregatorMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fusion_fuel_aggregator"), () -> fusionAggregatorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(fusionAggregatorType, AkaishiFusionFuelAggregatorScreen::new));

        // 离子体填装器：1 反应棒槽 + 3 只读燃料棒输出槽 + 7 数据槽（3 等离子体罐量/进度）
        MenuType<AkaishiPlasmaFillerMenu> fillerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiPlasmaFillerBlockEntity filler) {
                return new AkaishiPlasmaFillerMenu(syncId, inv, filler);
            }
            return new AkaishiPlasmaFillerMenu(syncId, inv, new SimpleContainer(1), new SimpleContainer(3),
                    new SimpleContainerData(AkaishiPlasmaFillerBlockEntity.DATA_SLOTS));
        });
        CHISHI_PLASMA_FILLER = (RegistrySupplier<MenuType<AkaishiPlasmaFillerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_plasma_filler"), () -> fillerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(fillerType, AkaishiPlasmaFillerScreen::new));

        // ===== 无线赤能源 =====
        // 终端（外墙主方块）：15 数据槽（储能 long/成型/口统计/授权卡数/组件状态/终端ID），
        // 1 授权槽（仅安全页显示）；网络缓冲 = 方块坐标 + 初始页（安全方块直达安全卡认证页）
        MenuType<AkaishiWirelessTerminalMenu> terminalType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            // 微缩终端的扩展数据只写坐标（无页号）⇒ 缺页号时回退运行页，避免缓冲读越界
            int page = buf.isReadable() ? buf.readInt() : 0;
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            AkaishiWirelessTerminalMenu menu = be instanceof AkaishiWirelessTerminalBlockEntity t
                    ? new AkaishiWirelessTerminalMenu(syncId, inv, t)
                    : be instanceof MiniatureTerminalBlockEntity miniature && miniature.wirelessHost() != null
                            ? new AkaishiWirelessTerminalMenu(syncId, inv, miniature.wirelessHost())
                            : AkaishiWirelessTerminalMenu.emptyMenu(syncId, inv);
            menu.setInitialPage(page);
            return menu;
        });
        CHISHI_WIRELESS_TERMINAL = (RegistrySupplier<MenuType<AkaishiWirelessTerminalMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_wireless_terminal"), () -> terminalType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(terminalType, AkaishiWirelessTerminalScreen::new));

        // 端口（输入口/输出口共用）：10 数据槽（缓冲储能 long + 卡/终端短 ID 各拆 2 槽 + 认证态 + 方向），无机器槽
        MenuType<AkaishiWirelessPortMenu> wirelessPortType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IWirelessPortHost host) {
                return new AkaishiWirelessPortMenu(syncId, inv, host);
            }
            return new AkaishiWirelessPortMenu(syncId, inv,
                    new SimpleContainerData(com.example.akaishi.block.entity.AkaishiWirelessInputPortBlockEntity.DATA_SLOTS));
        });
        CHISHI_WIRELESS_PORT = (RegistrySupplier<MenuType<AkaishiWirelessPortMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_wireless_port"), () -> wirelessPortType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(wirelessPortType, AkaishiWirelessPortScreen::new));

        // ===== 无线生命能量（赤版生命镜像，布局/槽位与赤版无线一致） =====
        // 终端（外墙主方块）：数据槽布局与赤版无线终端相同；网络缓冲 = 方块坐标 + 初始页
        MenuType<AkaishiLifeWirelessTerminalMenu> lifeTerminalType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            // 微缩终端的扩展数据只写坐标（无页号）⇒ 缺页号时回退运行页，避免缓冲读越界
            int page = buf.isReadable() ? buf.readInt() : 0;
            Level level = inv.player.level();
            BlockEntity lifeBe = level.getBlockEntity(pos);
            AkaishiLifeWirelessTerminalMenu menu = lifeBe instanceof AkaishiLifeWirelessTerminalBlockEntity t
                    ? new AkaishiLifeWirelessTerminalMenu(syncId, inv, t)
                    : lifeBe instanceof MiniatureTerminalBlockEntity miniature && miniature.wirelessHost() != null
                            ? new AkaishiLifeWirelessTerminalMenu(syncId, inv, miniature.wirelessHost())
                            : AkaishiLifeWirelessTerminalMenu.emptyMenu(syncId, inv);
            menu.setInitialPage(page);
            return menu;
        });
        CHISHI_LIFE_WIRELESS_TERMINAL = (RegistrySupplier<MenuType<AkaishiLifeWirelessTerminalMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_wireless_terminal"), () -> lifeTerminalType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(lifeTerminalType, AkaishiLifeWirelessTerminalScreen::new));

        // 端口（输入口/输出口共用）：数据槽/无机器槽布局与赤版口一致，无额外缓冲
        MenuType<AkaishiLifeWirelessPortMenu> lifeWirelessPortType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IWirelessPortHost host) {
                return new AkaishiLifeWirelessPortMenu(syncId, inv, host);
            }
            return new AkaishiLifeWirelessPortMenu(syncId, inv,
                    new SimpleContainerData(com.example.akaishi.block.entity.AkaishiLifeWirelessInputPortBlockEntity.DATA_SLOTS));
        });
        CHISHI_LIFE_WIRELESS_PORT = (RegistrySupplier<MenuType<AkaishiLifeWirelessPortMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_wireless_port"), () -> lifeWirelessPortType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(lifeWirelessPortType, AkaishiLifeWirelessPortScreen::new));

        // 便捷终端（手持物品）：无方块实体，服务端每 tick broadcastChanges 扫背包身份卡刷新数据槽
        MenuType<AkaishiWirelessPortableTerminalMenu> portableType = MenuRegistry.ofExtended((syncId, inv, buf) ->
                new AkaishiWirelessPortableTerminalMenu(syncId, inv, inv.player));
        CHISHI_WIRELESS_PORTABLE_TERMINAL = (RegistrySupplier<MenuType<AkaishiWirelessPortableTerminalMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_wireless_portable_terminal"), () -> portableType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(portableType, AkaishiWirelessPortableTerminalScreen::new));

        // 生命便捷终端（手持物品，赤版生命镜像）：无方块实体，服务端每 tick 按生命族扫背包身份卡刷新数据槽
        MenuType<AkaishiLifeWirelessPortableTerminalMenu> lifePortableType = MenuRegistry.ofExtended((syncId, inv, buf) ->
                new AkaishiLifeWirelessPortableTerminalMenu(syncId, inv, inv.player));
        CHISHI_LIFE_WIRELESS_PORTABLE_TERMINAL = (RegistrySupplier<MenuType<AkaishiLifeWirelessPortableTerminalMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_wireless_portable_terminal"), () -> lifePortableType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(lifePortableType, AkaishiLifeWirelessPortableTerminalScreen::new));

        // ===== 聚变堆 =====
        // 控制器：4 燃料槽 + 10 散热片槽 + 16 数据槽，三页界面（散热片槽直连控制器容器）
        MenuType<AkaishiFusionControllerMenu> fusionType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            if (level.getBlockEntity(pos) instanceof AkaishiFusionControllerBlockEntity controller) {
                return new AkaishiFusionControllerMenu(syncId, inv, controller);
            }
            return new AkaishiFusionControllerMenu(syncId, inv,
                    new SimpleContainer(AkaishiFusionControllerBlockEntity.MAX_FUEL_SLOTS),
                    new SimpleContainerData(AkaishiFusionControllerBlockEntity.DATA_SLOTS));
        });
        CHISHI_FUSION_CONTROLLER = (RegistrySupplier<MenuType<AkaishiFusionControllerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fusion_controller"), () -> fusionType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(fusionType, AkaishiFusionControllerScreen::new));

        // 物品输入/输出口（共用菜单类型）：27 格缓冲槽，缓冲类型由方块实体传入
        MenuType<AkaishiFusionItemPortMenu> fusionPortType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            if (level.getBlockEntity(pos) instanceof AkaishiFusionItemInputPortBlockEntity port) {
                return new AkaishiFusionItemPortMenu(syncId, inv, port.buffer(), AkaishiFusionItemPortMenu.BufferKind.INPUT_RODS);
            }
            if (level.getBlockEntity(pos) instanceof AkaishiFusionItemOutputPortBlockEntity port) {
                return new AkaishiFusionItemPortMenu(syncId, inv, port.buffer(), AkaishiFusionItemPortMenu.BufferKind.OUTPUT_ASH);
            }
            return AkaishiFusionItemPortMenu.emptyMenu(syncId, inv);
        });
        CHISHI_FUSION_ITEM_PORT = (RegistrySupplier<MenuType<AkaishiFusionItemPortMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fusion_item_port"), () -> fusionPortType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(fusionPortType, AkaishiFusionItemPortScreen::new));

        // 能量输出口：无机器槽位，能量/容量数据展示
        MenuType<AkaishiFusionEnergyOutputMenu> fusionEnergyType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            if (level.getBlockEntity(pos) instanceof AkaishiFusionEnergyOutputBlockEntity output) {
                return new AkaishiFusionEnergyOutputMenu(syncId, inv, output.data());
            }
            return AkaishiFusionEnergyOutputMenu.emptyMenu(syncId, inv);
        });
        CHISHI_FUSION_ENERGY_OUTPUT = (RegistrySupplier<MenuType<AkaishiFusionEnergyOutputMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fusion_energy_output"), () -> fusionEnergyType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(fusionEnergyType, AkaishiFusionEnergyOutputScreen::new));

        // ===== 机械改造三机 =====
        // 模板制造厂
        MenuType<AkaishiMechanicalTemplateFactoryMenu> mechTemplateType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            Level level = inv.player.level();
            if (level.getBlockEntity(buf.readBlockPos()) instanceof AkaishiMechanicalTemplateFactoryBlockEntity be) {
                return new AkaishiMechanicalTemplateFactoryMenu(syncId, inv, be);
            }
            return new AkaishiMechanicalTemplateFactoryMenu(syncId, inv,
                    new SimpleContainer(AkaishiMechanicalTemplateFactoryBlockEntity.SLOT_COUNT),
                    // 占位尺寸需与服务端一致：基类 10 槽 + 子类 2 槽
                    new SimpleContainerData(12), new MachineUpgradeSlots());
        });
        CHISHI_MECHANICAL_TEMPLATE_FACTORY = (RegistrySupplier<MenuType<AkaishiMechanicalTemplateFactoryMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_mechanical_template_factory"), () -> mechTemplateType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(mechTemplateType, AkaishiMechanicalTemplateFactoryScreen::new));
        // 加工制作厂
        MenuType<AkaishiMechanicalProcessingFactoryMenu> mechProcessType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            Level level = inv.player.level();
            if (level.getBlockEntity(buf.readBlockPos()) instanceof AkaishiMechanicalProcessingFactoryBlockEntity be) {
                return new AkaishiMechanicalProcessingFactoryMenu(syncId, inv, be);
            }
            return new AkaishiMechanicalProcessingFactoryMenu(syncId, inv,
                    new SimpleContainer(AkaishiMechanicalProcessingFactoryBlockEntity.SLOT_COUNT),
                    // 占位尺寸需与服务端一致：基类 10 槽（子类无额外槽）
                    new SimpleContainerData(AbstractMechanicalMachineBlockEntity.DATA_EXTRA_BASE), new MachineUpgradeSlots());
        });
        CHISHI_MECHANICAL_PROCESSING_FACTORY = (RegistrySupplier<MenuType<AkaishiMechanicalProcessingFactoryMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_mechanical_processing_factory"), () -> mechProcessType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(mechProcessType, AkaishiMechanicalProcessingFactoryScreen::new));
        // 组装加工台
        MenuType<AkaishiMechanicalAssemblyStationMenu> mechAssemblyType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            Level level = inv.player.level();
            if (level.getBlockEntity(buf.readBlockPos()) instanceof AkaishiMechanicalAssemblyStationBlockEntity be) {
                return new AkaishiMechanicalAssemblyStationMenu(syncId, inv, be);
            }
            return new AkaishiMechanicalAssemblyStationMenu(syncId, inv,
                    new SimpleContainer(AkaishiMechanicalAssemblyStationBlockEntity.SLOT_COUNT),
                    // 占位尺寸需与服务端一致：基类 10 槽（子类无额外槽）
                    new SimpleContainerData(10), new MachineUpgradeSlots());
        });
        CHISHI_MECHANICAL_ASSEMBLY_STATION = (RegistrySupplier<MenuType<AkaishiMechanicalAssemblyStationMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_mechanical_assembly_station"), () -> mechAssemblyType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(mechAssemblyType, AkaishiMechanicalAssemblyStationScreen::new));

        // 合并母神祭坛：同步结构等级（1 槽）+ 单物品供奉槽
        MenuType<AkaishiMotherAltarMenu> motherAltarType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiMotherAltarBlockEntity altar) {
                return new AkaishiMotherAltarMenu(syncId, inv, altar.altarSlot(), altar.data());
            }
            return AkaishiMotherAltarMenu.emptyMenu(syncId, inv);
        });
        CHISHI_MOTHER_ALTAR = (RegistrySupplier<MenuType<AkaishiMotherAltarMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_mother_altar"), () -> motherAltarType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(motherAltarType, AkaishiMotherAltarScreen::new));

        // 物品终端：库页 4×9 可视区（客户端只读虚拟槽）+ IP/单笔上限展示，服务端仅玩家背包槽
        MenuType<AkaishiItemTerminalMenu> itemTerminalType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiItemTerminalBlockEntity terminal) {
                return new AkaishiItemTerminalMenu(syncId, inv, terminal);
            }
            // 微缩件：同一套库页（宿主是微缩状态，不是终端方块实体）
            if (be instanceof MiniatureTerminalBlockEntity miniature && miniature.itemHost() != null) {
                return new AkaishiItemTerminalMenu(syncId, inv, miniature.itemHost());
            }
            // 方块实体缺失（跨维度/距离过远）时用空数据兜底：槽位与数据槽数量不变，避免索引错位
            return new AkaishiItemTerminalMenu(syncId, inv, null);
        });
        CHISHI_ITEM_TERMINAL = (RegistrySupplier<MenuType<AkaishiItemTerminalMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_item_terminal"), () -> itemTerminalType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(itemTerminalType, AkaishiItemTerminalScreen::new));

        // 微缩矩阵终端：芯片列表 / 升级装配 / 安全认证三页（芯片读数经 AkaishiMiniMatrixSync 快照）
        MenuType<AkaishiMiniMatrixTerminalMenu> miniMatrixType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiMiniMatrixTerminalBlockEntity matrix) {
                return new AkaishiMiniMatrixTerminalMenu(syncId, inv, matrix);
            }
            // 方块实体缺失（跨维度/距离过远）时用空数据兜底：槽位数量不变，避免索引错位
            return new AkaishiMiniMatrixTerminalMenu(syncId, inv, null);
        });
        CHISHI_MINI_MATRIX_TERMINAL = (RegistrySupplier<MenuType<AkaishiMiniMatrixTerminalMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_mini_matrix_terminal"), () -> miniMatrixType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(miniMatrixType, AkaishiMiniMatrixTerminalScreen::new));

        // 网络节点：只读展示绑定终端 / 归属者 / 子场域，外加本节点独立的「节点屏障」开关
        MenuType<AkaishiMiniMatrixNodeMenu> miniMatrixNodeType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiMiniMatrixNetworkNodeBlockEntity node) {
                return new AkaishiMiniMatrixNodeMenu(syncId, inv, node);
            }
            // 方块实体缺失（跨维度/距离过远）时用空数据兜底：本界面没有槽位，不存在索引错位
            return AkaishiMiniMatrixNodeMenu.empty(syncId, inv);
        });
        CHISHI_MINI_MATRIX_NODE = (RegistrySupplier<MenuType<AkaishiMiniMatrixNodeMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_mini_matrix_node"), () -> miniMatrixNodeType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(miniMatrixNodeType, AkaishiMiniMatrixNodeScreen::new));

        // 禁忌秘典：手持物品界面（无槽位、无方块实体），进度快照与条件求值由服务端下发
        MenuType<AkaishiCodexMenu> codexType = MenuRegistry.ofExtended(
                (syncId, inv, buf) -> new AkaishiCodexMenu(syncId, inv));
        CHISHI_CODEX = (RegistrySupplier<MenuType<AkaishiCodexMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_forbidden_codex"), () -> codexType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(codexType, AkaishiCodexScreen::new));

        // 物品储存单元：D18 只读视图（54 槽 + 占用/剩余 IP），无任何写入路径
        MenuType<AkaishiItemStorageUnitMenu> itemStorageUnitType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiItemStorageUnitBlockEntity unit) {
                return new AkaishiItemStorageUnitMenu(syncId, inv, unit);
            }
            return new AkaishiItemStorageUnitMenu(syncId, inv, null);
        });
        CHISHI_ITEM_STORAGE_UNIT = (RegistrySupplier<MenuType<AkaishiItemStorageUnitMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_item_storage_unit"),
                        () -> itemStorageUnitType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(itemStorageUnitType, AkaishiItemStorageUnitScreen::new));

        // 储存无线输入口/输出口（共用菜单类型）：5 数据槽（方向/绑定态/上次搬运/绑定身份短号拆 2 槽），无机器槽；
        // 远程绑定走 AkaishiItemPortBindingSync（S2C 清单快照 + C2S 动作包）
        MenuType<AkaishiItemPortMenu> itemPortType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiItemPortBlockEntity port) {
                return new AkaishiItemPortMenu(syncId, inv, port);
            }
            // 方块实体缺失（如跨维度/距离过远）时用同尺寸空数据兜底，避免索引错位
            return new AkaishiItemPortMenu(syncId, inv,
                    new SimpleContainerData(AkaishiItemPortBlockEntity.DATA_SLOTS));
        });
        CHISHI_ITEM_PORT = (RegistrySupplier<MenuType<AkaishiItemPortMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_item_port"), () -> itemPortType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(itemPortType, AkaishiItemPortScreen::new));
    }
}
