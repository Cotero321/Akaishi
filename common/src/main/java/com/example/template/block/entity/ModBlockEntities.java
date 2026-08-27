package com.example.template.block.entity;

import com.example.template.TemplateMod;
import com.example.template.block.ModBlocks;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * 方块实体类型注册。
 * 注意：BlockEntityType 的 Supplier 会创建侵入式 Holder，必须延迟到注册事件求值（与方块一致）。
 * register() 必须在 ModBlocks.register() 之后调用，确保方块已注册。
 */
public final class ModBlockEntities {

    /** 赤石提纯器方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiPurifierBlockEntity>> CHISHI_PURIFIER;
    /** 高级提纯构建方块方块实体类型（提纯矩阵外壳，单放独立提纯） */
    public static RegistrySupplier<BlockEntityType<ChishiAdvancedPurifierBlockEntity>> CHISHI_ADVANCED_PURIFIER;
    /** 赤能源储存单元方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiEnergyCellBlockEntity>> CHISHI_ENERGY_CELL;
    /** 赤能源管道方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiEnergyPipeBlockEntity>> CHISHI_ENERGY_PIPE;
    /** 赤能源发生机方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiEnergyGeneratorBlockEntity>> CHISHI_ENERGY_GENERATOR;
    /** 小型赤能源组合结构（多方块主方块）方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiEnergyAssemblyBlockEntity>> CHISHI_ENERGY_ASSEMBLY;
    /** 赤能源储存串联器（多方块主方块）方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiEnergyCellSerializerBlockEntity>> CHISHI_ENERGY_CELL_SERIALIZER;
    /** 超级发生器架构核心（5×5×5 多方块主方块）方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiSuperGeneratorCoreBlockEntity>> CHISHI_SUPER_GENERATOR_CORE;
    /** 生命能量管道方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiLifeEnergyPipeBlockEntity>> CHISHI_LIFE_ENERGY_PIPE;
    /** 生命聚合转换器方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiLifeAggregationConverterBlockEntity>> CHISHI_LIFE_AGGREGATION_CONVERTER;
    /** 生命转换架构（3×3×3 多方块主方块）方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiLifeConversionArchitectureBlockEntity>> CHISHI_LIFE_CONVERSION_ARCHITECTURE;
    /** 生命能量储存器方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiLifeEnergyCellBlockEntity>> CHISHI_LIFE_ENERGY_CELL;
    /** 赤石能量聚合器方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiEnergyAggregatorBlockEntity>> CHISHI_ENERGY_AGGREGATOR;
    /** 赤石装备打造器方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiEquipmentForgerBlockEntity>> CHISHI_EQUIPMENT_FORGER;
    /** 赤红升级台方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiUpgradeStationBlockEntity>> CHISHI_UPGRADE_STATION;
    /** 创造模式能量源（赤/生命两种方块共用，无限输出） */
    public static RegistrySupplier<BlockEntityType<CreativeEnergySourceBlockEntity>> CREATIVE_ENERGY_SOURCE;
    /** 赤石催化器方块实体类型（4 级共用，等级由方块决定） */
    public static RegistrySupplier<BlockEntityType<ChishiCatalystBlockEntity>> CHISHI_CATALYST;
    /** 自动收集器方块实体类型（4 级共用，等级由方块决定） */
    public static RegistrySupplier<BlockEntityType<ChishiAutoCollectorBlockEntity>> CHISHI_AUTO_COLLECTOR;
    /** 物品管道方块实体类型（4 级共用，等级由方块决定） */
    public static RegistrySupplier<BlockEntityType<ChishiItemPipeBlockEntity>> CHISHI_ITEM_PIPE;
    /** 生命能量提纯器方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiLifePurifierBlockEntity>> CHISHI_LIFE_PURIFIER;
    /** 液体管道方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiFluidPipeBlockEntity>> CHISHI_FLUID_PIPE;
    /** 能量液化装置方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiEnergyLiquefierBlockEntity>> CHISHI_ENERGY_LIQUEFIER;
    /** 能量加工器方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiEnergyProcessorBlockEntity>> CHISHI_ENERGY_PROCESSOR;
    /** 燃料装罐机方块实体 */
    public static RegistrySupplier<BlockEntityType<ChishiFuelCannerBlockEntity>> CHISHI_FUEL_CANNER;
    /** 燃料混合器方块实体 */
    public static RegistrySupplier<BlockEntityType<ChishiFuelMixerBlockEntity>> CHISHI_FUEL_MIXER;
    /** 液体储罐方块实体类型（基础/高级/超级共用） */
    public static RegistrySupplier<BlockEntityType<ChishiFluidTankBlockEntity>> CHISHI_FLUID_TANK;
    /** 反应堆控制器方块实体类型（主方块，持有全部反应堆状态） */
    public static RegistrySupplier<BlockEntityType<ChishiReactorControllerBlockEntity>> CHISHI_REACTOR_CONTROLLER;
    /** 燃料投放口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiReactorFuelPortBlockEntity>> CHISHI_REACTOR_FUEL_PORT;
    /** 能量输出口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiReactorEnergyOutputBlockEntity>> CHISHI_REACTOR_ENERGY_OUTPUT;
    /** 废品输出口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiReactorWastePortBlockEntity>> CHISHI_REACTOR_WASTE_PORT;
    /** 散热组件方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiReactorCoolerBlockEntity>> CHISHI_REACTOR_COOLER;
    /** 衰竭保存桶方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiExhaustedBarrelBlockEntity>> CHISHI_EXHAUSTED_BARREL;

    private ModBlockEntities() {
    }

    @SuppressWarnings("unchecked")
    public static void register() {
        CHISHI_PURIFIER = (RegistrySupplier<BlockEntityType<ChishiPurifierBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_purifier"),
                        () -> BlockEntityType.Builder.of(ChishiPurifierBlockEntity::new, ModBlocks.CHISHI_PURIFIER.get())
                                .build(null));
        CHISHI_ADVANCED_PURIFIER = (RegistrySupplier<BlockEntityType<ChishiAdvancedPurifierBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_advanced_purifier"),
                        () -> BlockEntityType.Builder.of(ChishiAdvancedPurifierBlockEntity::new, ModBlocks.CHISHI_ADVANCED_PURIFIER.get())
                                .build(null));
        // 储存单元：基础/高级/超级三个方块共用一个方块实体类型，等级由方块本身决定
        CHISHI_ENERGY_CELL = (RegistrySupplier<BlockEntityType<ChishiEnergyCellBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_cell"),
                        () -> BlockEntityType.Builder.of(ChishiEnergyCellBlockEntity::new,
                                        ModBlocks.CHISHI_ENERGY_CELL_BASIC.get(),
                                        ModBlocks.CHISHI_ENERGY_CELL_ADVANCED.get(),
                                        ModBlocks.CHISHI_ENERGY_CELL_SUPER.get())
                                .build(null));
        // 管道：基础/高级/精英/终极四个方块共用一个方块实体类型，等级由方块本身决定
        CHISHI_ENERGY_PIPE = (RegistrySupplier<BlockEntityType<ChishiEnergyPipeBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_pipe"),
                        () -> BlockEntityType.Builder.of(ChishiEnergyPipeBlockEntity::new,
                                ModBlocks.CHISHI_ENERGY_PIPE.get(),
                                ModBlocks.CHISHI_ENERGY_PIPE_ADVANCED.get(),
                                ModBlocks.CHISHI_ENERGY_PIPE_ELITE.get(),
                                ModBlocks.CHISHI_ENERGY_PIPE_ULTIMATE.get())
                                .build(null));
        CHISHI_ENERGY_GENERATOR = (RegistrySupplier<BlockEntityType<ChishiEnergyGeneratorBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_generator"),
                        () -> BlockEntityType.Builder.of(ChishiEnergyGeneratorBlockEntity::new, ModBlocks.CHISHI_ENERGY_GENERATOR.get())
                                .build(null));
        CHISHI_ENERGY_ASSEMBLY = (RegistrySupplier<BlockEntityType<ChishiEnergyAssemblyBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_assembly"),
                        () -> BlockEntityType.Builder.of(ChishiEnergyAssemblyBlockEntity::new, ModBlocks.CHISHI_ENERGY_ASSEMBLY.get())
                                .build(null));
        // 赤能源储存串联器（多方块主方块）
        CHISHI_ENERGY_CELL_SERIALIZER = (RegistrySupplier<BlockEntityType<ChishiEnergyCellSerializerBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_cell_serializer"),
                        () -> BlockEntityType.Builder.of(ChishiEnergyCellSerializerBlockEntity::new,
                                ModBlocks.CHISHI_ENERGY_CELL_SERIALIZER.get())
                                .build(null));
        // 超级发生器架构核心（5×5×5 多方块主方块）
        CHISHI_SUPER_GENERATOR_CORE = (RegistrySupplier<BlockEntityType<ChishiSuperGeneratorCoreBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_super_generator_core"),
                        () -> BlockEntityType.Builder.of(ChishiSuperGeneratorCoreBlockEntity::new,
                                ModBlocks.CHISHI_SUPER_GENERATOR_CORE.get())
                                .build(null));
        // 生命能量管道（独立方块实体类型，传输生命能量）
        CHISHI_LIFE_ENERGY_PIPE = (RegistrySupplier<BlockEntityType<ChishiLifeEnergyPipeBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_life_energy_pipe"),
                        () -> BlockEntityType.Builder.of(ChishiLifeEnergyPipeBlockEntity::new,
                                ModBlocks.CHISHI_LIFE_ENERGY_PIPE.get())
                                .build(null));
        // 生命聚合转换器（单方块 / 外壳）
        CHISHI_LIFE_AGGREGATION_CONVERTER = (RegistrySupplier<BlockEntityType<ChishiLifeAggregationConverterBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_life_aggregation_converter"),
                        () -> BlockEntityType.Builder.of(ChishiLifeAggregationConverterBlockEntity::new,
                                ModBlocks.CHISHI_LIFE_AGGREGATION_CONVERTER.get())
                                .build(null));
        // 生命转换架构（3×3×3 多方块主方块）
        CHISHI_LIFE_CONVERSION_ARCHITECTURE = (RegistrySupplier<BlockEntityType<ChishiLifeConversionArchitectureBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_life_conversion_architecture"),
                        () -> BlockEntityType.Builder.of(ChishiLifeConversionArchitectureBlockEntity::new,
                                ModBlocks.CHISHI_LIFE_CONVERSION_ARCHITECTURE.get())
                                .build(null));
        // 生命能量储存器（纯生命能量存储）
        CHISHI_LIFE_ENERGY_CELL = (RegistrySupplier<BlockEntityType<ChishiLifeEnergyCellBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_life_energy_cell"),
                        () -> BlockEntityType.Builder.of(ChishiLifeEnergyCellBlockEntity::new,
                                ModBlocks.CHISHI_LIFE_ENERGY_CELL.get())
                                .build(null));
        // 赤石能量聚合器
        CHISHI_ENERGY_AGGREGATOR = (RegistrySupplier<BlockEntityType<ChishiEnergyAggregatorBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_aggregator"),
                        () -> BlockEntityType.Builder.of(ChishiEnergyAggregatorBlockEntity::new,
                                ModBlocks.CHISHI_ENERGY_AGGREGATOR.get())
                                .build(null));
        // 赤石装备打造器
        CHISHI_EQUIPMENT_FORGER = (RegistrySupplier<BlockEntityType<ChishiEquipmentForgerBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_equipment_forger"),
                        () -> BlockEntityType.Builder.of(ChishiEquipmentForgerBlockEntity::new,
                                ModBlocks.CHISHI_EQUIPMENT_FORGER.get())
                                .build(null));
        // 赤红升级台
        CHISHI_UPGRADE_STATION = (RegistrySupplier<BlockEntityType<ChishiUpgradeStationBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_upgrade_station"),
                        () -> BlockEntityType.Builder.of(ChishiUpgradeStationBlockEntity::new,
                                ModBlocks.CHISHI_UPGRADE_STATION.get())
                                .build(null));
        // 创造模式能量源（赤能源 / 生命能量两种方块共用，无限输出测试）
        CREATIVE_ENERGY_SOURCE = (RegistrySupplier<BlockEntityType<CreativeEnergySourceBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "creative_energy_source"),
                        () -> BlockEntityType.Builder.of(CreativeEnergySourceBlockEntity::new,
                                ModBlocks.CHISHI_CREATIVE_ENERGY_CELL.get(),
                                ModBlocks.CHISHI_CREATIVE_LIFE_CELL.get())
                                .build(null));
        // 赤石催化器：初/中/高/终四个方块共用一个方块实体类型，等级由方块本身决定
        CHISHI_CATALYST = (RegistrySupplier<BlockEntityType<ChishiCatalystBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_catalyst"),
                        () -> BlockEntityType.Builder.of(ChishiCatalystBlockEntity::new,
                                ModBlocks.CHISHI_CATALYST_BASIC.get(),
                                ModBlocks.CHISHI_CATALYST_MEDIUM.get(),
                                ModBlocks.CHISHI_CATALYST_ADVANCED.get(),
                                ModBlocks.CHISHI_CATALYST_ULTIMATE.get())
                                .build(null));
        // 自动收集器：初/中/高/终四个方块共用一个方块实体类型，等级由方块本身决定
        CHISHI_AUTO_COLLECTOR = (RegistrySupplier<BlockEntityType<ChishiAutoCollectorBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_auto_collector"),
                        () -> BlockEntityType.Builder.of(ChishiAutoCollectorBlockEntity::new,
                                ModBlocks.CHISHI_COLLECTOR_BASIC.get(),
                                ModBlocks.CHISHI_COLLECTOR_MEDIUM.get(),
                                ModBlocks.CHISHI_COLLECTOR_ADVANCED.get(),
                                ModBlocks.CHISHI_COLLECTOR_ULTIMATE.get())
                                .build(null));
        // 物品管道：基础/高级/精英/终极四个方块共用一个方块实体类型，等级由方块本身决定
        CHISHI_ITEM_PIPE = (RegistrySupplier<BlockEntityType<ChishiItemPipeBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_item_pipe"),
                        () -> BlockEntityType.Builder.of(ChishiItemPipeBlockEntity::new,
                                ModBlocks.CHISHI_ITEM_PIPE.get(),
                                ModBlocks.CHISHI_ITEM_PIPE_ADVANCED.get(),
                                ModBlocks.CHISHI_ITEM_PIPE_ELITE.get(),
                                ModBlocks.CHISHI_ITEM_PIPE_ULTIMATE.get())
                                .build(null));
        // 生命能量提纯器（赤能源驱动，输出生命能量固态物）
        CHISHI_LIFE_PURIFIER = (RegistrySupplier<BlockEntityType<ChishiLifePurifierBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_life_purifier"),
                        () -> BlockEntityType.Builder.of(ChishiLifePurifierBlockEntity::new,
                                ModBlocks.CHISHI_LIFE_PURIFIER.get())
                                .build(null));
        // 液体管道（传输下界能量/燃料液体）
        CHISHI_FLUID_PIPE = (RegistrySupplier<BlockEntityType<ChishiFluidPipeBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_fluid_pipe"),
                        () -> BlockEntityType.Builder.of(ChishiFluidPipeBlockEntity::new,
                                ModBlocks.CHISHI_FLUID_PIPE.get())
                                .build(null));
        // 能量液化装置（赤能源驱动，产出下界能量液体）
        CHISHI_ENERGY_LIQUEFIER = (RegistrySupplier<BlockEntityType<ChishiEnergyLiquefierBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_liquefier"),
                        () -> BlockEntityType.Builder.of(ChishiEnergyLiquefierBlockEntity::new,
                                ModBlocks.CHISHI_ENERGY_LIQUEFIER.get())
                                .build(null));
        // 能量加工器（赤能源驱动，固态物 + 下界能量液体 → 燃料）
        CHISHI_ENERGY_PROCESSOR = (RegistrySupplier<BlockEntityType<ChishiEnergyProcessorBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_processor"),
                        () -> BlockEntityType.Builder.of(ChishiEnergyProcessorBlockEntity::new,
                                ModBlocks.CHISHI_ENERGY_PROCESSOR.get())
                                .build(null));
        // 燃料装罐机（液体燃料 → 10L 燃料罐）
        CHISHI_FUEL_CANNER = (RegistrySupplier<BlockEntityType<ChishiFuelCannerBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_fuel_canner"),
                        () -> BlockEntityType.Builder.of(ChishiFuelCannerBlockEntity::new,
                                ModBlocks.CHISHI_FUEL_CANNER.get())
                                .build(null));
        // 燃料混合器（燃料液体 1:1:1 调和 → 高级/终极混合燃料）
        CHISHI_FUEL_MIXER = (RegistrySupplier<BlockEntityType<ChishiFuelMixerBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_fuel_mixer"),
                        () -> BlockEntityType.Builder.of(ChishiFuelMixerBlockEntity::new,
                                ModBlocks.CHISHI_FUEL_MIXER.get())
                                .build(null));
        // 液体储罐（一个方块实体类型承载 基础/高级/超级 三个等级）
        CHISHI_FLUID_TANK = (RegistrySupplier<BlockEntityType<ChishiFluidTankBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_fluid_tank"),
                        () -> BlockEntityType.Builder.of(ChishiFluidTankBlockEntity::new,
                                ModBlocks.CHISHI_FLUID_TANK_BASIC.get(),
                                ModBlocks.CHISHI_FLUID_TANK_ADVANCED.get(),
                                ModBlocks.CHISHI_FLUID_TANK_SUPER.get())
                                .build(null));
        // ===== 反应堆体系 =====
        // 控制器（多方块主方块）
        CHISHI_REACTOR_CONTROLLER = (RegistrySupplier<BlockEntityType<ChishiReactorControllerBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_reactor_controller"),
                        () -> BlockEntityType.Builder.of(ChishiReactorControllerBlockEntity::new,
                                ModBlocks.CHISHI_REACTOR_CONTROLLER.get())
                                .build(null));
        // 燃料投放口（燃料罐缓冲槽）
        CHISHI_REACTOR_FUEL_PORT = (RegistrySupplier<BlockEntityType<ChishiReactorFuelPortBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_reactor_fuel_port"),
                        () -> BlockEntityType.Builder.of(ChishiReactorFuelPortBlockEntity::new,
                                ModBlocks.CHISHI_REACTOR_FUEL_PORT.get())
                                .build(null));
        // 能量输出口（赤能源缓冲）
        CHISHI_REACTOR_ENERGY_OUTPUT = (RegistrySupplier<BlockEntityType<ChishiReactorEnergyOutputBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_reactor_energy_output"),
                        () -> BlockEntityType.Builder.of(ChishiReactorEnergyOutputBlockEntity::new,
                                ModBlocks.CHISHI_REACTOR_ENERGY_OUTPUT.get())
                                .build(null));
        // 废品输出口（衰竭燃料缓冲）
        CHISHI_REACTOR_WASTE_PORT = (RegistrySupplier<BlockEntityType<ChishiReactorWastePortBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_reactor_waste_port"),
                        () -> BlockEntityType.Builder.of(ChishiReactorWastePortBlockEntity::new,
                                ModBlocks.CHISHI_REACTOR_WASTE_PORT.get())
                                .build(null));
        // 散热组件（单散热片槽位）
        CHISHI_REACTOR_COOLER = (RegistrySupplier<BlockEntityType<ChishiReactorCoolerBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_reactor_cooler"),
                        () -> BlockEntityType.Builder.of(ChishiReactorCoolerBlockEntity::new,
                                ModBlocks.CHISHI_REACTOR_COOLER.get())
                                .build(null));
        // 衰竭保存桶（专储衰竭燃料）
        CHISHI_EXHAUSTED_BARREL = (RegistrySupplier<BlockEntityType<ChishiExhaustedBarrelBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_exhausted_barrel"),
                        () -> BlockEntityType.Builder.of(ChishiExhaustedBarrelBlockEntity::new,
                                ModBlocks.CHISHI_EXHAUSTED_BARREL.get())
                                .build(null));
    }
}
