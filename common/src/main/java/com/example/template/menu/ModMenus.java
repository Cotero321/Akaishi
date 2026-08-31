package com.example.template.menu;

import com.example.template.TemplateMod;
import com.example.template.block.ChishiGenMatrixTier;
import com.example.template.block.entity.ChishiAutoCollectorBlockEntity;
import com.example.template.block.entity.ChishiCatalystBlockEntity;
import com.example.template.block.entity.ChishiEnergyAggregatorBlockEntity;
import com.example.template.block.entity.ChishiEnergyCellBlockEntity;
import com.example.template.block.entity.ChishiEnergyCellSerializerBlockEntity;
import com.example.template.block.entity.ChishiExhaustedBarrelBlockEntity;
import com.example.template.block.entity.ChishiEnergyGeneratorBlockEntity;
import com.example.template.block.entity.ChishiEnergyLiquefierBlockEntity;
import com.example.template.block.entity.ChishiEnergyProcessorBlockEntity;
import com.example.template.block.entity.ChishiFuelCannerBlockEntity;
import com.example.template.block.entity.ChishiFuelMixerBlockEntity;
import com.example.template.block.entity.ChishiFluidTankBlockEntity;
import com.example.template.block.entity.ChishiEquipmentForgerBlockEntity;
import com.example.template.block.entity.ChishiGeneAnalyzerBlockEntity;
import com.example.template.block.entity.ChishiGenMatrixControllerBlockEntity;
import com.example.template.block.entity.ChishiCultivatorBlockEntity;
import com.example.template.block.entity.ChishiLifeStructBlockEntity;
import com.example.template.block.entity.ChishiSurgeryBlockEntity;
import com.example.template.block.entity.ChishiPotionTableBlockEntity;
import com.example.template.block.entity.ChishiOrganVaultBlockEntity;
import com.example.template.block.entity.ChishiPotionCabinetBlockEntity;
import com.example.template.block.entity.ChishiSampleVaultBlockEntity;
import com.example.template.block.entity.ChishiLifeMatrixControllerBlockEntity;
import com.example.template.block.entity.ChishiLifeActivatorBlockEntity;
import com.example.template.block.entity.ChishiLifePurifierBlockEntity;
import com.example.template.block.entity.ChishiReactorControllerBlockEntity;
import com.example.template.block.entity.ChishiReactorEnergyOutputBlockEntity;
import com.example.template.block.entity.ChishiReactorFuelPortBlockEntity;
import com.example.template.block.entity.ChishiPurifierBlockEntity;
import com.example.template.block.entity.ChishiPurifierMatrixControllerBlockEntity;
import com.example.template.block.entity.ChishiUpgradeStationBlockEntity;
import com.example.template.block.entity.ChishiWirelessTerminalBlockEntity;
import com.example.template.wireless.IWirelessPortHost;
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
    public static RegistrySupplier<MenuType<ChishiPurifierMenu>> CHISHI_PURIFIER;
    /** 赤能源储存单元菜单类型 */
    public static RegistrySupplier<MenuType<ChishiEnergyCellMenu>> CHISHI_ENERGY_CELL;
    /** 赤能源发生机菜单类型 */
    public static RegistrySupplier<MenuType<ChishiEnergyGeneratorMenu>> CHISHI_ENERGY_GENERATOR;
    /** 赤能源储存串联器（多方块主方块）菜单类型 */
    public static RegistrySupplier<MenuType<ChishiEnergyCellSerializerMenu>> CHISHI_ENERGY_CELL_SERIALIZER;
    /** 生命转换菜单类型（生命聚合转换器 / 生命转换架构共用） */
    public static RegistrySupplier<MenuType<ChishiLifeConverterMenu>> CHISHI_LIFE_CONVERTER;
    /** 赤石能量聚合器菜单类型 */
    public static RegistrySupplier<MenuType<ChishiEnergyAggregatorMenu>> CHISHI_ENERGY_AGGREGATOR;
    /** 赤石装备打造器菜单类型 */
    public static RegistrySupplier<MenuType<ChishiEquipmentForgerMenu>> CHISHI_EQUIPMENT_FORGER;
    /** 赤红升级台菜单类型 */
    public static RegistrySupplier<MenuType<ChishiUpgradeStationMenu>> CHISHI_UPGRADE_STATION;
    /** 自动收集器菜单类型 */
    public static RegistrySupplier<MenuType<ChishiAutoCollectorMenu>> CHISHI_AUTO_COLLECTOR;
    public static RegistrySupplier<MenuType<ChishiCatalystMenu>> CHISHI_CATALYST;
    /** 生命能量提纯器菜单类型 */
    public static RegistrySupplier<MenuType<ChishiLifePurifierMenu>> CHISHI_LIFE_PURIFIER;
    /** 能量液化装置菜单类型 */
    public static RegistrySupplier<MenuType<ChishiEnergyLiquefierMenu>> CHISHI_ENERGY_LIQUEFIER;
    /** 能量加工器菜单类型 */
    public static RegistrySupplier<MenuType<ChishiEnergyProcessorMenu>> CHISHI_ENERGY_PROCESSOR;
    /** 燃料装罐机菜单 */
    public static RegistrySupplier<MenuType<ChishiFuelCannerMenu>> CHISHI_FUEL_CANNER;
    /** 燃料混合器菜单 */
    public static RegistrySupplier<MenuType<ChishiFuelMixerMenu>> CHISHI_FUEL_MIXER;
    /** 液体储罐菜单类型 */
    public static RegistrySupplier<MenuType<ChishiFluidTankMenu>> CHISHI_FLUID_TANK;
    /** 反应堆控制器菜单类型 */
    public static RegistrySupplier<MenuType<ChishiReactorControllerMenu>> CHISHI_REACTOR_CONTROLLER;
    /** 衰竭保存桶菜单类型 */
    public static RegistrySupplier<MenuType<ChishiExhaustedBarrelMenu>> CHISHI_EXHAUSTED_BARREL;
    /** 反应堆燃料投放口菜单类型（27 格燃料罐缓冲） */
    public static RegistrySupplier<MenuType<ChishiReactorFuelPortMenu>> CHISHI_REACTOR_FUEL_PORT;
    /** 反应堆能量输出口菜单类型（能量缓冲展示） */
    public static RegistrySupplier<MenuType<ChishiReactorEnergyOutputMenu>> CHISHI_REACTOR_ENERGY_OUTPUT;
    /** 躯体检查仪菜单类型（纯展示面板，无槽位） */
    public static RegistrySupplier<MenuType<ChishiBodyScannerMenu>> CHISHI_BODY_SCANNER;
    /** 生命分析台菜单类型 */
    public static RegistrySupplier<MenuType<ChishiGeneAnalyzerMenu>> CHISHI_GENE_ANALYZER;
    /** 部件培养舱菜单类型 */
    public static RegistrySupplier<MenuType<ChishiCultivatorMenu>> CHISHI_CULTIVATOR;
    /** 生命结构台菜单类型 */
    public static RegistrySupplier<MenuType<ChishiLifeStructMenu>> CHISHI_LIFE_STRUCT;
    /** 手术仓菜单类型 */
    public static RegistrySupplier<MenuType<ChishiSurgeryMenu>> CHISHI_SURGERY;
    /** 药剂台菜单类型 */
    public static RegistrySupplier<MenuType<ChishiPotionTableMenu>> CHISHI_POTION_TABLE;
    /** 器官储藏库菜单类型 */
    public static RegistrySupplier<MenuType<ChishiOrganVaultMenu>> CHISHI_ORGAN_VAULT;
    /** 药剂库菜单类型 */
    public static RegistrySupplier<MenuType<ChishiPotionCabinetMenu>> CHISHI_POTION_CABINET;
    /** 样本库菜单类型 */
    public static RegistrySupplier<MenuType<ChishiSampleVaultMenu>> CHISHI_SAMPLE_VAULT;
    /** 发生器矩阵控制器菜单类型（低级/高级共用，等级由方块实例决定） */
    public static RegistrySupplier<MenuType<ChishiGenMatrixControllerMenu>> CHISHI_GEN_MATRIX_CONTROLLER;
    /** 提纯矩阵控制器菜单类型 */
    public static RegistrySupplier<MenuType<ChishiPurifierMatrixControllerMenu>> CHISHI_PURIFIER_MATRIX_CONTROLLER;
    /** 生命活化器菜单类型 */
    public static RegistrySupplier<MenuType<ChishiLifeActivatorMenu>> CHISHI_LIFE_ACTIVATOR;
    /** 无线赤能源终端菜单类型（终端方块主界面，四页互斥） */
    public static RegistrySupplier<MenuType<ChishiWirelessTerminalMenu>> CHISHI_WIRELESS_TERMINAL;
    /** 无线赤能源输入口/输出口菜单类型（共用） */
    public static RegistrySupplier<MenuType<ChishiWirelessPortMenu>> CHISHI_WIRELESS_PORT;
    /** 无线能源便捷终端菜单类型（手持物品，无方块实体） */
    public static RegistrySupplier<MenuType<ChishiWirelessPortableTerminalMenu>> CHISHI_WIRELESS_PORTABLE_TERMINAL;

    private ModMenus() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register() {
        // MenuType 实例可在构造期直接创建（仅封装容器工厂，不触碰注册表，此时注册表未冻结）
        MenuType<ChishiPurifierMenu> type = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiPurifierBlockEntity purifier) {
                return new ChishiPurifierMenu(syncId, inv, purifier.inventory(), purifier.data());
            }
            // 方块实体缺失（如跨维度/距离过远）时使用空数据兜底，避免崩溃
            return new ChishiPurifierMenu(syncId, inv,
                    new SimpleContainer(ChishiPurifierBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(ChishiPurifierBlockEntity.DATA_SLOTS));
        });
        // 注册表延迟注册同一实例（supplier 在 RegisterEvent 时才求值）：
        // Forge 发送打开界面数据包时按注册表编码 MenuType，未注册将无法打开界面
        CHISHI_PURIFIER = (RegistrySupplier<MenuType<ChishiPurifierMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_purifier"), () -> type);
        // 客户端注册界面工厂：实例已就绪，无需等待注册求值，避免构造期 get() 取值 NPE
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(type, ChishiPurifierScreen::new));

        // 赤能源储存单元：1 便携单元充能槽 + 能量数据同步
        MenuType<ChishiEnergyCellMenu> cellType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiEnergyCellBlockEntity cell) {
                return new ChishiEnergyCellMenu(syncId, inv, cell.cellSlot(), cell.data());
            }
            return ChishiEnergyCellMenu.emptyMenu(syncId, inv);
        });
        CHISHI_ENERGY_CELL = (RegistrySupplier<MenuType<ChishiEnergyCellMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_cell"), () -> cellType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(cellType, ChishiEnergyCellScreen::new));

        // 赤能源发生机：1 燃料槽 + 能量同步
        MenuType<ChishiEnergyGeneratorMenu> genType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiEnergyGeneratorBlockEntity gen) {
                return new ChishiEnergyGeneratorMenu(syncId, inv, gen);
            }
            return new ChishiEnergyGeneratorMenu(syncId, inv,
                    new SimpleContainer(ChishiEnergyGeneratorBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(3));
        });
        CHISHI_ENERGY_GENERATOR = (RegistrySupplier<MenuType<ChishiEnergyGeneratorMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_generator"), () -> genType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(genType, ChishiEnergyGeneratorScreen::new));

        // 赤能源储存串联器：无容器槽位，同步总能量/总容量（long 4 槽）+ 结构状态
        MenuType<ChishiEnergyCellSerializerMenu> serializerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiEnergyCellSerializerBlockEntity serializer) {
                return new ChishiEnergyCellSerializerMenu(syncId, inv, serializer.data());
            }
            return ChishiEnergyCellSerializerMenu.emptyMenu(syncId, inv);
        });
        CHISHI_ENERGY_CELL_SERIALIZER = (RegistrySupplier<MenuType<ChishiEnergyCellSerializerMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_cell_serializer"), () -> serializerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(serializerType, ChishiEnergyCellSerializerScreen::new));

        // 生命转换（生命转换矩阵控制器）：无容器槽位，同步赤能源+生命能量+结构状态
        MenuType<ChishiLifeConverterMenu> lifeType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiLifeMatrixControllerBlockEntity controller) {
                return new ChishiLifeConverterMenu(syncId, inv, controller.data());
            }
            return ChishiLifeConverterMenu.emptyMenu(syncId, inv);
        });
        CHISHI_LIFE_CONVERTER = (RegistrySupplier<MenuType<ChishiLifeConverterMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_life_converter"), () -> lifeType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(lifeType, ChishiLifeConverterScreen::new));

        // 赤石能量聚合器
        MenuType<ChishiEnergyAggregatorMenu> aggregatorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            return level.getBlockEntity(pos) instanceof ChishiEnergyAggregatorBlockEntity be
                    ? new ChishiEnergyAggregatorMenu(syncId, inv, be)
                    : new ChishiEnergyAggregatorMenu(syncId, inv, new net.minecraft.world.SimpleContainer(2), new net.minecraft.world.inventory.SimpleContainerData(3));
        });
        CHISHI_ENERGY_AGGREGATOR = (RegistrySupplier<MenuType<ChishiEnergyAggregatorMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_aggregator"), () -> aggregatorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(aggregatorType, ChishiEnergyAggregatorScreen::new));

        // 赤石装备打造器
        MenuType<ChishiEquipmentForgerMenu> forgerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            return level.getBlockEntity(pos) instanceof ChishiEquipmentForgerBlockEntity be
                    ? new ChishiEquipmentForgerMenu(syncId, inv, be)
                    : new ChishiEquipmentForgerMenu(syncId, inv, new net.minecraft.world.SimpleContainer(3), new net.minecraft.world.inventory.SimpleContainerData(3));
        });
        CHISHI_EQUIPMENT_FORGER = (RegistrySupplier<MenuType<ChishiEquipmentForgerMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_equipment_forger"), () -> forgerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(forgerType, ChishiEquipmentForgerScreen::new));

        // 赤红升级台
        MenuType<ChishiUpgradeStationMenu> upgradeType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            return level.getBlockEntity(pos) instanceof ChishiUpgradeStationBlockEntity be
                    ? new ChishiUpgradeStationMenu(syncId, inv, be)
                    : new ChishiUpgradeStationMenu(syncId, inv, null);
        });
        CHISHI_UPGRADE_STATION = (RegistrySupplier<MenuType<ChishiUpgradeStationMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_upgrade_station"), () -> upgradeType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(upgradeType, ChishiUpgradeStationScreen::new));

        // 自动收集器：27 槽存储 + 能量/进度同步
        MenuType<ChishiAutoCollectorMenu> collectorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            return level.getBlockEntity(pos) instanceof ChishiAutoCollectorBlockEntity be
                    ? new ChishiAutoCollectorMenu(syncId, inv, be)
                    : new ChishiAutoCollectorMenu(syncId, inv,
                            new net.minecraft.world.SimpleContainer(ChishiAutoCollectorBlockEntity.STORAGE_SIZE),
                            new net.minecraft.world.inventory.SimpleContainerData(ChishiAutoCollectorBlockEntity.DATA_SLOTS));
        });
        CHISHI_AUTO_COLLECTOR = (RegistrySupplier<MenuType<ChishiAutoCollectorMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_auto_collector"), () -> collectorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(collectorType, ChishiAutoCollectorScreen::new));

        // 赤石催化器：无机器槽，仅玩家背包 + 能量/工作状态数据同步
        MenuType<ChishiCatalystMenu> catalystType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            return level.getBlockEntity(pos) instanceof ChishiCatalystBlockEntity be
                    ? new ChishiCatalystMenu(syncId, inv, be.data())
                    : new ChishiCatalystMenu(syncId, inv,
                            new net.minecraft.world.inventory.SimpleContainerData(ChishiCatalystBlockEntity.DATA_SLOTS));
        });
        CHISHI_CATALYST = (RegistrySupplier<MenuType<ChishiCatalystMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_catalyst"), () -> catalystType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(catalystType, ChishiCatalystScreen::new));

        // 生命能量提纯器：1 输出槽 + 赤能源/生命能量/进度数据同步
        MenuType<ChishiLifePurifierMenu> lifePurifierType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiLifePurifierBlockEntity purifier) {
                return new ChishiLifePurifierMenu(syncId, inv, purifier.inventory(), purifier.data());
            }
            return new ChishiLifePurifierMenu(syncId, inv,
                    new SimpleContainer(ChishiLifePurifierBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(ChishiLifePurifierBlockEntity.DATA_SLOTS));
        });
        CHISHI_LIFE_PURIFIER = (RegistrySupplier<MenuType<ChishiLifePurifierMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_life_purifier"), () -> lifePurifierType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(lifePurifierType, ChishiLifePurifierScreen::new));

        // 能量液化装置：1 输入槽（下界之星/凋零玫瑰）+ 赤能源/双液体罐/进度数据同步
        MenuType<ChishiEnergyLiquefierMenu> liquefierType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiEnergyLiquefierBlockEntity liquefier) {
                return new ChishiEnergyLiquefierMenu(syncId, inv, liquefier);
            }
            return new ChishiEnergyLiquefierMenu(syncId, inv,
                    new SimpleContainer(ChishiEnergyLiquefierBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(ChishiEnergyLiquefierBlockEntity.DATA_SLOTS));
        });
        CHISHI_ENERGY_LIQUEFIER = (RegistrySupplier<MenuType<ChishiEnergyLiquefierMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_liquefier"), () -> liquefierType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(liquefierType, ChishiEnergyLiquefierScreen::new));

        // 能量加工器：1 输入槽（生命固态物）+ 赤能源/双输入罐/双输出罐/进度数据同步
        MenuType<ChishiEnergyProcessorMenu> processorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiEnergyProcessorBlockEntity processor) {
                return new ChishiEnergyProcessorMenu(syncId, inv, processor);
            }
            return new ChishiEnergyProcessorMenu(syncId, inv,
                    new SimpleContainer(ChishiEnergyProcessorBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(ChishiEnergyProcessorBlockEntity.DATA_SLOTS));
        });
        CHISHI_ENERGY_PROCESSOR = (RegistrySupplier<MenuType<ChishiEnergyProcessorMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_processor"), () -> processorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(processorType, ChishiEnergyProcessorScreen::new));

        // 燃料装罐机：1 空罐输入槽 + 1 满罐输出槽 + 输入液体量数据
        MenuType<ChishiFuelCannerMenu> cannerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            String fuelId = buf.readUtf();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiFuelCannerBlockEntity canner) {
                return new ChishiFuelCannerMenu(syncId, inv, canner, fuelId);
            }
            return new ChishiFuelCannerMenu(syncId, inv,
                    new SimpleContainer(ChishiFuelCannerBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(ChishiFuelCannerBlockEntity.DATA_SLOTS), fuelId);
        });
        CHISHI_FUEL_CANNER = (RegistrySupplier<MenuType<ChishiFuelCannerMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_fuel_canner"), () -> cannerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(cannerType, ChishiFuelCannerScreen::new));

        // 燃料混合器：无机器槽位（纯液体调和），9 数据槽（能量/双输入/输出/进度）
        MenuType<ChishiFuelMixerMenu> mixerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiFuelMixerBlockEntity mixer) {
                return new ChishiFuelMixerMenu(syncId, inv, mixer);
            }
            return new ChishiFuelMixerMenu(syncId, inv,
                    new SimpleContainerData(ChishiFuelMixerBlockEntity.DATA_SLOTS));
        });
        CHISHI_FUEL_MIXER = (RegistrySupplier<MenuType<ChishiFuelMixerMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_fuel_mixer"), () -> mixerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(mixerType, ChishiFuelMixerScreen::new));

        // 液体储罐：无机器槽位，仅液体量/容量数据展示
        MenuType<ChishiFluidTankMenu> tankType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiFluidTankBlockEntity tank) {
                return new ChishiFluidTankMenu(syncId, inv, tank.data());
            }
            return ChishiFluidTankMenu.emptyMenu(syncId, inv);
        });
        CHISHI_FLUID_TANK = (RegistrySupplier<MenuType<ChishiFluidTankMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_fluid_tank"), () -> tankType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(tankType, ChishiFluidTankScreen::new));

        // 反应堆控制器：10 燃料槽 + 13 数据槽（温度/成型/散热/废品/熔毁等）
        MenuType<ChishiReactorControllerMenu> reactorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiReactorControllerBlockEntity controller) {
                return new ChishiReactorControllerMenu(syncId, inv, controller);
            }
            return new ChishiReactorControllerMenu(syncId, inv,
                    new SimpleContainer(ChishiReactorControllerBlockEntity.MAX_FUEL_SLOTS),
                    new SimpleContainerData(ChishiReactorControllerBlockEntity.DATA_SLOTS));
        });
        CHISHI_REACTOR_CONTROLLER = (RegistrySupplier<MenuType<ChishiReactorControllerMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_reactor_controller"), () -> reactorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(reactorType, ChishiReactorControllerScreen::new));

        // 衰竭保存桶：无机器槽位，仅液体量/容量数据展示
        MenuType<ChishiExhaustedBarrelMenu> barrelType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiExhaustedBarrelBlockEntity barrel) {
                return new ChishiExhaustedBarrelMenu(syncId, inv, barrel.data());
            }
            return ChishiExhaustedBarrelMenu.emptyMenu(syncId, inv);
        });
        CHISHI_EXHAUSTED_BARREL = (RegistrySupplier<MenuType<ChishiExhaustedBarrelMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_exhausted_barrel"), () -> barrelType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(barrelType, ChishiExhaustedBarrelScreen::new));

        // 反应堆燃料投放口：27 格燃料罐缓冲槽 + 玩家背包
        MenuType<ChishiReactorFuelPortMenu> fuelPortType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            if (level.getBlockEntity(pos) instanceof ChishiReactorFuelPortBlockEntity port) {
                return new ChishiReactorFuelPortMenu(syncId, inv, port.buffer());
            }
            return ChishiReactorFuelPortMenu.emptyMenu(syncId, inv);
        });
        CHISHI_REACTOR_FUEL_PORT = (RegistrySupplier<MenuType<ChishiReactorFuelPortMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_reactor_fuel_port"), () -> fuelPortType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(fuelPortType, ChishiReactorFuelPortScreen::new));

        // 反应堆能量输出口：无机器槽位，能量/容量数据展示
        MenuType<ChishiReactorEnergyOutputMenu> energyOutputType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            if (level.getBlockEntity(pos) instanceof ChishiReactorEnergyOutputBlockEntity output) {
                return new ChishiReactorEnergyOutputMenu(syncId, inv, output.data());
            }
            return ChishiReactorEnergyOutputMenu.emptyMenu(syncId, inv);
        });
        CHISHI_REACTOR_ENERGY_OUTPUT = (RegistrySupplier<MenuType<ChishiReactorEnergyOutputMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_reactor_energy_output"), () -> energyOutputType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(energyOutputType, ChishiReactorEnergyOutputScreen::new));

        // 躯体检查仪：无机器槽位，纯展示玩家躯体状态（数据由 S2C 同步包推送）
        MenuType<ChishiBodyScannerMenu> bodyScannerType = MenuRegistry.ofExtended((syncId, inv, buf) ->
                new ChishiBodyScannerMenu(syncId, inv));
        CHISHI_BODY_SCANNER = (RegistrySupplier<MenuType<ChishiBodyScannerMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_body_scanner"), () -> bodyScannerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(bodyScannerType, ChishiBodyScannerScreen::new));

        // 生命分析台：输入（纯度 100 样本）+ 输出（基因序列片段）+ 生命能量/进度数据
        MenuType<ChishiGeneAnalyzerMenu> geneAnalyzerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiGeneAnalyzerBlockEntity analyzer) {
                return new ChishiGeneAnalyzerMenu(syncId, inv, analyzer.inventory(), analyzer.data(), pos);
            }
            return new ChishiGeneAnalyzerMenu(syncId, inv,
                    new SimpleContainer(ChishiGeneAnalyzerBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(ChishiGeneAnalyzerBlockEntity.DATA_SLOTS), pos);
        });
        CHISHI_GENE_ANALYZER = (RegistrySupplier<MenuType<ChishiGeneAnalyzerMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_gene_analyzer"), () -> geneAnalyzerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(geneAnalyzerType, ChishiGeneAnalyzerScreen::new));

        // 部件培养舱：输入（样本/器官）+ 材料（固态物）+ 生命能量/进度/模式数据
        MenuType<ChishiCultivatorMenu> cultivatorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiCultivatorBlockEntity cultivator) {
                return new ChishiCultivatorMenu(syncId, inv, cultivator.inventory(), cultivator.data(), pos);
            }
            return new ChishiCultivatorMenu(syncId, inv,
                    new SimpleContainer(ChishiCultivatorBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(ChishiCultivatorBlockEntity.DATA_SLOTS), pos);
        });
        CHISHI_CULTIVATOR = (RegistrySupplier<MenuType<ChishiCultivatorMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_cultivator"), () -> cultivatorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(cultivatorType, ChishiCultivatorScreen::new));

        // 生命结构台：基因序列 + 固态物 → 器官（目标槽位界面选择）
        MenuType<ChishiLifeStructMenu> lifeStructType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiLifeStructBlockEntity struct) {
                return new ChishiLifeStructMenu(syncId, inv, struct.inventory(), struct.data(), struct.getBlockPos());
            }
            return new ChishiLifeStructMenu(syncId, inv,
                    new SimpleContainer(ChishiLifeStructBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(ChishiLifeStructBlockEntity.DATA_SLOTS));
        });
        CHISHI_LIFE_STRUCT = (RegistrySupplier<MenuType<ChishiLifeStructMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_life_struct"), () -> lifeStructType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(lifeStructType, ChishiLifeStructScreen::new));

        // 手术仓：器官移植/摘除（消耗固态 + 生命能量，带进度）
        MenuType<ChishiSurgeryMenu> surgeryType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiSurgeryBlockEntity surgery) {
                return new ChishiSurgeryMenu(syncId, inv, surgery.inventory(), surgery.data(), surgery.getBlockPos());
            }
            return new ChishiSurgeryMenu(syncId, inv,
                    new SimpleContainer(ChishiSurgeryBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(ChishiSurgeryBlockEntity.DATA_SLOTS), pos);
        });
        CHISHI_SURGERY = (RegistrySupplier<MenuType<ChishiSurgeryMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_surgery"), () -> surgeryType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(surgeryType, ChishiSurgeryScreen::new));

        // 药剂台：样本+固态+生命能量 → 永久/突破药剂（模板选择走 C2S 包）
        MenuType<ChishiPotionTableMenu> potionTableType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiPotionTableBlockEntity potionTable) {
                return new ChishiPotionTableMenu(syncId, inv, potionTable.inventory(), potionTable.data(), potionTable.getBlockPos());
            }
            return new ChishiPotionTableMenu(syncId, inv,
                    new SimpleContainer(ChishiPotionTableBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(ChishiPotionTableBlockEntity.DATA_SLOTS), pos);
        });
        CHISHI_POTION_TABLE = (RegistrySupplier<MenuType<ChishiPotionTableMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_potion_table"), () -> potionTableType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(potionTableType, ChishiPotionTableScreen::new));

        // 器官储藏库：按躯体槽位分页的器官仓库（选页为客户端本地状态，无需网络包）
        MenuType<ChishiOrganVaultMenu> organVaultType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiOrganVaultBlockEntity vault) {
                return new ChishiOrganVaultMenu(syncId, inv, vault);
            }
            return new ChishiOrganVaultMenu(syncId, inv,
                    new SimpleContainer(ChishiOrganVaultBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(ChishiOrganVaultBlockEntity.DATA_SLOTS), pos);
        });
        CHISHI_ORGAN_VAULT = (RegistrySupplier<MenuType<ChishiOrganVaultMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_organ_vault"), () -> organVaultType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(organVaultType, ChishiOrganVaultScreen::new));

        // 药剂库：大容量药剂仓库（筛选为客户端本地状态，无需网络包）
        MenuType<ChishiPotionCabinetMenu> potionCabinetType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiPotionCabinetBlockEntity cabinet) {
                return new ChishiPotionCabinetMenu(syncId, inv, cabinet);
            }
            return new ChishiPotionCabinetMenu(syncId, inv,
                    new SimpleContainer(ChishiPotionCabinetBlockEntity.CABINET_SLOTS));
        });
        CHISHI_POTION_CABINET = (RegistrySupplier<MenuType<ChishiPotionCabinetMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_potion_cabinet"), () -> potionCabinetType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(potionCabinetType, ChishiPotionCabinetScreen::new));

        // 样本库：大容量样本仓库（同 NBT 自动合并）
        MenuType<ChishiSampleVaultMenu> sampleVaultType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiSampleVaultBlockEntity vault) {
                return new ChishiSampleVaultMenu(syncId, inv, vault);
            }
            return new ChishiSampleVaultMenu(syncId, inv,
                    new SimpleContainer(ChishiSampleVaultBlockEntity.SAMPLE_SLOTS));
        });
        CHISHI_SAMPLE_VAULT = (RegistrySupplier<MenuType<ChishiSampleVaultMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_sample_vault"), () -> sampleVaultType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(sampleVaultType, ChishiSampleVaultScreen::new));

        // 发生器矩阵控制器：1 燃料槽 + 10 升级槽 + 5 数据槽（能量/燃烧/总量/成型/升级数）
        MenuType<ChishiGenMatrixControllerMenu> genMatrixType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiGenMatrixControllerBlockEntity controller) {
                return new ChishiGenMatrixControllerMenu(syncId, inv, controller);
            }
            return new ChishiGenMatrixControllerMenu(syncId, inv,
                    new SimpleContainer(ChishiGenMatrixControllerBlockEntity.TOTAL_SLOTS),
                    new SimpleContainerData(5), ChishiGenMatrixTier.BASIC);
        });
        CHISHI_GEN_MATRIX_CONTROLLER = (RegistrySupplier<MenuType<ChishiGenMatrixControllerMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_gen_matrix_controller"), () -> genMatrixType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(genMatrixType, ChishiGenMatrixControllerScreen::new));

        // 提纯矩阵控制器：2 槽（输入/输出）+ 3 数据槽（能量/进度/成型）
        MenuType<ChishiPurifierMatrixControllerMenu> purifierMatrixType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiPurifierMatrixControllerBlockEntity controller) {
                return new ChishiPurifierMatrixControllerMenu(syncId, inv, controller);
            }
            return new ChishiPurifierMatrixControllerMenu(syncId, inv,
                    new SimpleContainer(ChishiPurifierMatrixControllerBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(ChishiPurifierMatrixControllerBlockEntity.DATA_SLOTS));
        });
        CHISHI_PURIFIER_MATRIX_CONTROLLER = (RegistrySupplier<MenuType<ChishiPurifierMatrixControllerMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_purifier_matrix_controller"), () -> purifierMatrixType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(purifierMatrixType, ChishiPurifierMatrixControllerScreen::new));

        // 生命活化器：无机器槽位（纯液体无害化），7 数据槽（生命能量/输入/输出/累计活化量）
        MenuType<ChishiLifeActivatorMenu> activatorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiLifeActivatorBlockEntity activator) {
                return new ChishiLifeActivatorMenu(syncId, inv, activator);
            }
            return new ChishiLifeActivatorMenu(syncId, inv,
                    new SimpleContainerData(ChishiLifeActivatorBlockEntity.DATA_SLOTS));
        });
        CHISHI_LIFE_ACTIVATOR = (RegistrySupplier<MenuType<ChishiLifeActivatorMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_life_activator"), () -> activatorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(activatorType, ChishiLifeActivatorScreen::new));

        // ===== 无线赤能源 =====
        // 终端（外墙主方块）：15 数据槽（储能 long/成型/口统计/授权卡数/组件状态/终端ID），
        // 1 授权槽（仅安全页显示）；网络缓冲 = 方块坐标 + 初始页（安全方块直达安全卡认证页）
        MenuType<ChishiWirelessTerminalMenu> terminalType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            int page = buf.readInt();
            Level level = inv.player.level();
            ChishiWirelessTerminalMenu menu = level.getBlockEntity(pos) instanceof ChishiWirelessTerminalBlockEntity t
                    ? new ChishiWirelessTerminalMenu(syncId, inv, t)
                    : ChishiWirelessTerminalMenu.emptyMenu(syncId, inv);
            menu.setInitialPage(page);
            return menu;
        });
        CHISHI_WIRELESS_TERMINAL = (RegistrySupplier<MenuType<ChishiWirelessTerminalMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_wireless_terminal"), () -> terminalType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(terminalType, ChishiWirelessTerminalScreen::new));

        // 端口（输入口/输出口共用）：9 数据槽（缓冲储能 long + 卡/终端短 ID + 认证态 + 速率 long），无机器槽
        MenuType<ChishiWirelessPortMenu> wirelessPortType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IWirelessPortHost host) {
                return new ChishiWirelessPortMenu(syncId, inv, host);
            }
            return new ChishiWirelessPortMenu(syncId, inv,
                    new SimpleContainerData(com.example.template.block.entity.ChishiWirelessInputPortBlockEntity.DATA_SLOTS));
        });
        CHISHI_WIRELESS_PORT = (RegistrySupplier<MenuType<ChishiWirelessPortMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_wireless_port"), () -> wirelessPortType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(wirelessPortType, ChishiWirelessPortScreen::new));

        // 便捷终端（手持物品）：无方块实体，服务端每 tick broadcastChanges 扫背包身份卡刷新数据槽
        MenuType<ChishiWirelessPortableTerminalMenu> portableType = MenuRegistry.ofExtended((syncId, inv, buf) ->
                new ChishiWirelessPortableTerminalMenu(syncId, inv, inv.player));
        CHISHI_WIRELESS_PORTABLE_TERMINAL = (RegistrySupplier<MenuType<ChishiWirelessPortableTerminalMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_wireless_portable_terminal"), () -> portableType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(portableType, ChishiWirelessPortableTerminalScreen::new));
    }
}
