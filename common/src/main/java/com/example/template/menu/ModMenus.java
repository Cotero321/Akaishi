package com.example.template.menu;

import com.example.template.TemplateMod;
import com.example.template.block.entity.ChishiEnergyAssemblyBlockEntity;
import com.example.template.block.entity.ChishiAutoCollectorBlockEntity;
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
import com.example.template.block.entity.ChishiLifeAggregationConverterBlockEntity;
import com.example.template.block.entity.ChishiLifeConversionArchitectureBlockEntity;
import com.example.template.block.entity.ChishiLifePurifierBlockEntity;
import com.example.template.block.entity.ChishiReactorControllerBlockEntity;
import com.example.template.block.entity.ChishiSuperGeneratorCoreBlockEntity;
import com.example.template.block.entity.ChishiPurifierBlockEntity;
import com.example.template.block.entity.ChishiUpgradeStationBlockEntity;
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
    /** 小型赤能源组合结构（多方块主方块）菜单类型 */
    public static RegistrySupplier<MenuType<ChishiEnergyAssemblyMenu>> CHISHI_ENERGY_ASSEMBLY;
    /** 赤能源储存串联器（多方块主方块）菜单类型 */
    public static RegistrySupplier<MenuType<ChishiEnergyCellSerializerMenu>> CHISHI_ENERGY_CELL_SERIALIZER;
    /** 超级发生器架构核心（5×5×5 多方块主方块）菜单类型 */
    public static RegistrySupplier<MenuType<ChishiSuperGeneratorMenu>> CHISHI_SUPER_GENERATOR_CORE;
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

        // 小型赤能源组合结构：1 燃料槽 + 能量 + 结构状态同步
        MenuType<ChishiEnergyAssemblyMenu> asmType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiEnergyAssemblyBlockEntity asm) {
                return new ChishiEnergyAssemblyMenu(syncId, inv, asm);
            }
            return new ChishiEnergyAssemblyMenu(syncId, inv,
                    new SimpleContainer(ChishiEnergyAssemblyBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(4));
        });
        CHISHI_ENERGY_ASSEMBLY = (RegistrySupplier<MenuType<ChishiEnergyAssemblyMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_assembly"), () -> asmType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(asmType, ChishiEnergyAssemblyScreen::new));

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

        // 超级发生器架构核心（5×5×5 多方块主方块）：1 个燃料槽 + 玩家背包
        MenuType<ChishiSuperGeneratorMenu> superGenType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiSuperGeneratorCoreBlockEntity core) {
                return new ChishiSuperGeneratorMenu(syncId, inv, core);
            }
            return new ChishiSuperGeneratorMenu(syncId, inv, new SimpleContainer(1), new SimpleContainerData(4));
        });
        CHISHI_SUPER_GENERATOR_CORE = (RegistrySupplier<MenuType<ChishiSuperGeneratorMenu>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_super_generator_core"), () -> superGenType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(superGenType, ChishiSuperGeneratorScreen::new));

        // 生命转换（聚合转换器 / 转换架构共用）：无容器槽位，同步赤能源+生命能量+结构状态
        MenuType<ChishiLifeConverterMenu> lifeType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiLifeAggregationConverterBlockEntity converter) {
                return new ChishiLifeConverterMenu(syncId, inv, converter.data());
            }
            if (be instanceof ChishiLifeConversionArchitectureBlockEntity arch) {
                return new ChishiLifeConverterMenu(syncId, inv, arch.data());
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
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiFuelCannerBlockEntity canner) {
                return new ChishiFuelCannerMenu(syncId, inv, canner);
            }
            return new ChishiFuelCannerMenu(syncId, inv,
                    new SimpleContainer(ChishiFuelCannerBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(ChishiFuelCannerBlockEntity.DATA_SLOTS));
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
    }
}
