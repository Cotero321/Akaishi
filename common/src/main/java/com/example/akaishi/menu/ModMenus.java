package com.example.akaishi.menu;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.inventory.MenuType;

/**
 * 菜单类型注册（门面）。
 * MenuType 必须注册到 Registers.MENU：Forge 发送打开界面数据包时按注册表编码 MenuType，
 * 未注册将无法打开界面。服务端/客户端通过同一工厂创建菜单：从网络缓冲读取方块坐标，再取方块实体数据。
 * <p>
 * 注册逻辑按机器家族/领域拆分到同包的 {@code *MenuRegs} 包私有域类（纯机械搬迁）；
 * {@link #register()} 按原文件的注册顺序逐域委托 —— 各域覆盖原顺序的<b>连续分段</b>，
 * 域内顺序与域间顺序都与原 {@code register()} 逐位一致，故注册表写入顺序不变。
 * <p>
 * 菜单类型字段全部保留在本类（对外 API 不变）。
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
    /** 材料融合器菜单类型（两输入 + 一输出 + 三升级槽） */
    public static RegistrySupplier<MenuType<AkaishiMaterialFuserMenu>> CHISHI_MATERIAL_FUSER;
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

    private ModMenus() {
    }

    /**
     * 注册全部菜单类型。按原 {@code register()} 的注册顺序逐域委托：
     * 基础机械（1~21）→ 反应堆（22~25）→ 生命器官（26~39）→ 矩阵与加工机（40~48）→
     * 矿机与聚变前哨（49~55）→ 无线终端（56~61）→ 聚变堆（62~64）→ 机械改造与祭坛（65~68）→
     * 终端与储存（69~74；禁忌秘典已于 P3a 迁往 akaishi_forbidden）。域内与域间顺序都与原文件逐位一致。
     */
    public static void register() {
        BasicMenuRegs.register();
        ReactorMenuRegs.register();
        LifeMenuRegs.register();
        MatrixMenuRegs.register();
        MinerMenuRegs.register();
        WirelessMenuRegs.register();
        FusionMenuRegs.register();
        MachineMenuRegs.register();
        TerminalMenuRegs.register();
    }
}
