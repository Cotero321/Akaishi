package com.example.template.block.entity;

import com.example.template.TemplateMod;
import com.example.template.block.ModBlocks;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Arrays;

/**
 * 方块实体类型注册。
 * 注意：BlockEntityType 的 Supplier 会创建侵入式 Holder，必须延迟到注册事件求值（与方块一致）。
 * register() 必须在 ModBlocks.register() 之后调用，确保方块已注册。
 * 所有注册统一走 {@link #be(String, BlockEntityType.BlockEntitySupplier, RegistrySupplier...)} helper，
 * 侵入式强转只收敛在 helper 一处。
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
    /** 封闭性衰竭管道方块实体类型（废料专用，单缓冲） */
    public static RegistrySupplier<BlockEntityType<ChishiExhaustedPipeBlockEntity>> CHISHI_EXHAUSTED_PIPE;
    /** 多流体废料管道方块实体类型（废料专用，多缓冲） */
    public static RegistrySupplier<BlockEntityType<ChishiMultiFluidWastePipeBlockEntity>> CHISHI_MULTI_FLUID_WASTE_PIPE;
    /** 能量液化装置方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiEnergyLiquefierBlockEntity>> CHISHI_ENERGY_LIQUEFIER;
    /** 能量加工器方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiEnergyProcessorBlockEntity>> CHISHI_ENERGY_PROCESSOR;
    /** 燃料装罐机方块实体 */
    public static RegistrySupplier<BlockEntityType<ChishiFuelCannerBlockEntity>> CHISHI_FUEL_CANNER;
    /** 燃料混合器方块实体 */
    public static RegistrySupplier<BlockEntityType<ChishiFuelMixerBlockEntity>> CHISHI_FUEL_MIXER;
    /** 生命活化器方块实体 */
    public static RegistrySupplier<BlockEntityType<ChishiLifeActivatorBlockEntity>> CHISHI_LIFE_ACTIVATOR;
    /** 生命离心机方块实体 */
    public static RegistrySupplier<BlockEntityType<ChishiLifeCentrifugeBlockEntity>> CHISHI_LIFE_CENTRIFUGE;
    /** 物品重构仪方块实体 */
    public static RegistrySupplier<BlockEntityType<ChishiItemReconstructorBlockEntity>> CHISHI_ITEM_RECONSTRUCTOR;
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
    /** 躯体检查仪方块实体类型（无 tick 纯展示） */
    public static RegistrySupplier<BlockEntityType<ChishiBodyScannerBlockEntity>> CHISHI_BODY_SCANNER;
    /** 生命分析台方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiGeneAnalyzerBlockEntity>> CHISHI_GENE_ANALYZER;
    /** 部件培养舱方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiCultivatorBlockEntity>> CHISHI_CULTIVATOR;
    /** 生命结构台方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiLifeStructBlockEntity>> CHISHI_LIFE_STRUCT;
    /** 手术仓方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiSurgeryBlockEntity>> CHISHI_SURGERY;
    /** 药剂台方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiPotionTableBlockEntity>> CHISHI_POTION_TABLE;
    /** 器官储藏库方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiOrganVaultBlockEntity>> CHISHI_ORGAN_VAULT;
    /** 药剂库方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiPotionCabinetBlockEntity>> CHISHI_POTION_CABINET;
    /** 样本库方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiSampleVaultBlockEntity>> CHISHI_SAMPLE_VAULT;
    /** 发生器矩阵控制器方块实体类型（低级/高级共用，等级由方块决定） */
    public static RegistrySupplier<BlockEntityType<ChishiGenMatrixControllerBlockEntity>> CHISHI_GEN_MATRIX_CONTROLLER;
    /** 发生器矩阵能量输出口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiGenEnergyOutputPortBlockEntity>> CHISHI_GEN_ENERGY_OUTPUT;
    /** 发生器矩阵燃料输入口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiGenFuelInputPortBlockEntity>> CHISHI_GEN_FUEL_INPUT;
    /** 提纯矩阵控制器方块实体类型（主方块，持有提纯状态） */
    public static RegistrySupplier<BlockEntityType<ChishiPurifierMatrixControllerBlockEntity>> CHISHI_PURIFIER_MATRIX_CONTROLLER;
    /** 提纯矩阵物品输入口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiPurifierItemInputPortBlockEntity>> CHISHI_PURIFIER_ITEM_INPUT;
    /** 提纯矩阵物品输出口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiPurifierItemOutputPortBlockEntity>> CHISHI_PURIFIER_ITEM_OUTPUT;
    /** 提纯矩阵能量输入口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiPurifierEnergyInputPortBlockEntity>> CHISHI_PURIFIER_ENERGY_INPUT;
    /** 生命转换矩阵控制器方块实体类型（主方块，持有转换状态） */
    public static RegistrySupplier<BlockEntityType<ChishiLifeMatrixControllerBlockEntity>> CHISHI_LIFE_MATRIX_CONTROLLER;
    /** 生命转换矩阵能量输入口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiLifeMatrixEnergyInputPortBlockEntity>> CHISHI_LIFE_MATRIX_ENERGY_INPUT;
    /** 生命转换矩阵能量输出口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiLifeMatrixEnergyOutputPortBlockEntity>> CHISHI_LIFE_MATRIX_ENERGY_OUTPUT;
    /** 无线赤能源终端方块实体类型（外墙主方块，网络能量中枢） */
    public static RegistrySupplier<BlockEntityType<ChishiWirelessTerminalBlockEntity>> CHISHI_WIRELESS_TERMINAL;
    /** 无线赤能源输入口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiWirelessInputPortBlockEntity>> CHISHI_WIRELESS_INPUT_PORT;
    /** 无线赤能源输出口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<ChishiWirelessOutputPortBlockEntity>> CHISHI_WIRELESS_OUTPUT_PORT;
    /** 区块加载构架方块实体：锁所属终端结构控制器区块（弱加载） */
    public static RegistrySupplier<BlockEntityType<ChishiWirelessChunkLoaderBlockEntity>> CHISHI_WIRELESS_CHUNK_LOADER;

    private ModBlockEntities() {
    }

    /**
     * 注册方块实体类型（唯一强转点）。
     * Builder 的 Supplier 延迟到注册事件求值，且方块引用同样延迟解引用，
     * 避免方块注册事件（BLOCK）尚未 fire 时提前取方块实例导致 Value missing。
     *
     * @param id      注册 id
     * @param factory 方块实体工厂（方法引用，延迟创建）
     * @param blocks  该类型绑定的方块引用（可多个方块共用一个类型，事件内解引用）
     */
    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> RegistrySupplier<BlockEntityType<T>> be(
            String id, BlockEntityType.BlockEntitySupplier<T> factory, RegistrySupplier<Block>... blocks) {
        return (RegistrySupplier<BlockEntityType<T>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, id),
                        () -> BlockEntityType.Builder.of(factory,
                                Arrays.stream(blocks).map(RegistrySupplier::get).toArray(Block[]::new)).build(null));
    }

    public static void register() {
        CHISHI_PURIFIER = be("chishi_purifier", ChishiPurifierBlockEntity::new, ModBlocks.CHISHI_PURIFIER);
        CHISHI_ADVANCED_PURIFIER = be("chishi_advanced_purifier", ChishiAdvancedPurifierBlockEntity::new,
                ModBlocks.CHISHI_ADVANCED_PURIFIER);
        // 储存单元：基础/高级/超级三个方块共用一个方块实体类型，等级由方块本身决定
        CHISHI_ENERGY_CELL = be("chishi_energy_cell", ChishiEnergyCellBlockEntity::new,
                ModBlocks.CHISHI_ENERGY_CELL_BASIC, ModBlocks.CHISHI_ENERGY_CELL_ADVANCED,
                ModBlocks.CHISHI_ENERGY_CELL_SUPER);
        // 管道：基础/高级/精英/终极四个方块共用一个方块实体类型，等级由方块本身决定
        CHISHI_ENERGY_PIPE = be("chishi_energy_pipe", ChishiEnergyPipeBlockEntity::new,
                ModBlocks.CHISHI_ENERGY_PIPE, ModBlocks.CHISHI_ENERGY_PIPE_ADVANCED,
                ModBlocks.CHISHI_ENERGY_PIPE_ELITE, ModBlocks.CHISHI_ENERGY_PIPE_ULTIMATE);
        CHISHI_ENERGY_GENERATOR = be("chishi_energy_generator", ChishiEnergyGeneratorBlockEntity::new,
                ModBlocks.CHISHI_ENERGY_GENERATOR);
        CHISHI_ENERGY_ASSEMBLY = be("chishi_energy_assembly", ChishiEnergyAssemblyBlockEntity::new,
                ModBlocks.CHISHI_ENERGY_ASSEMBLY);
        // 赤能源储存串联器（多方块主方块）
        CHISHI_ENERGY_CELL_SERIALIZER = be("chishi_energy_cell_serializer", ChishiEnergyCellSerializerBlockEntity::new,
                ModBlocks.CHISHI_ENERGY_CELL_SERIALIZER);
        // 超级发生器架构核心（5×5×5 多方块主方块）
        CHISHI_SUPER_GENERATOR_CORE = be("chishi_super_generator_core", ChishiSuperGeneratorCoreBlockEntity::new,
                ModBlocks.CHISHI_SUPER_GENERATOR_CORE);
        // 生命能量管道（独立方块实体类型，传输生命能量）
        CHISHI_LIFE_ENERGY_PIPE = be("chishi_life_energy_pipe", ChishiLifeEnergyPipeBlockEntity::new,
                ModBlocks.CHISHI_LIFE_ENERGY_PIPE);
        // 生命聚合转换器（单方块 / 外壳）
        CHISHI_LIFE_AGGREGATION_CONVERTER = be("chishi_life_aggregation_converter",
                ChishiLifeAggregationConverterBlockEntity::new, ModBlocks.CHISHI_LIFE_AGGREGATION_CONVERTER);
        // 生命转换架构（3×3×3 多方块主方块）
        CHISHI_LIFE_CONVERSION_ARCHITECTURE = be("chishi_life_conversion_architecture",
                ChishiLifeConversionArchitectureBlockEntity::new, ModBlocks.CHISHI_LIFE_CONVERSION_ARCHITECTURE);
        // 生命能量储存器（纯生命能量存储）
        CHISHI_LIFE_ENERGY_CELL = be("chishi_life_energy_cell", ChishiLifeEnergyCellBlockEntity::new,
                ModBlocks.CHISHI_LIFE_ENERGY_CELL);
        // 赤石能量聚合器
        CHISHI_ENERGY_AGGREGATOR = be("chishi_energy_aggregator", ChishiEnergyAggregatorBlockEntity::new,
                ModBlocks.CHISHI_ENERGY_AGGREGATOR);
        // 赤石装备打造器
        CHISHI_EQUIPMENT_FORGER = be("chishi_equipment_forger", ChishiEquipmentForgerBlockEntity::new,
                ModBlocks.CHISHI_EQUIPMENT_FORGER);
        // 赤红升级台
        CHISHI_UPGRADE_STATION = be("chishi_upgrade_station", ChishiUpgradeStationBlockEntity::new,
                ModBlocks.CHISHI_UPGRADE_STATION);
        // 创造模式能量源（赤能源 / 生命能量两种方块共用，无限输出测试）
        CREATIVE_ENERGY_SOURCE = be("creative_energy_source", CreativeEnergySourceBlockEntity::new,
                ModBlocks.CHISHI_CREATIVE_ENERGY_CELL, ModBlocks.CHISHI_CREATIVE_LIFE_CELL);
        // 赤石催化器：初/中/高/终四个方块共用一个方块实体类型，等级由方块本身决定
        CHISHI_CATALYST = be("chishi_catalyst", ChishiCatalystBlockEntity::new,
                ModBlocks.CHISHI_CATALYST_BASIC, ModBlocks.CHISHI_CATALYST_MEDIUM,
                ModBlocks.CHISHI_CATALYST_ADVANCED, ModBlocks.CHISHI_CATALYST_ULTIMATE);
        // 自动收集器：初/中/高/终四个方块共用一个方块实体类型，等级由方块本身决定
        CHISHI_AUTO_COLLECTOR = be("chishi_auto_collector", ChishiAutoCollectorBlockEntity::new,
                ModBlocks.CHISHI_COLLECTOR_BASIC, ModBlocks.CHISHI_COLLECTOR_MEDIUM,
                ModBlocks.CHISHI_COLLECTOR_ADVANCED, ModBlocks.CHISHI_COLLECTOR_ULTIMATE);
        // 物品管道：基础/高级/精英/终极四个方块共用一个方块实体类型，等级由方块本身决定
        CHISHI_ITEM_PIPE = be("chishi_item_pipe", ChishiItemPipeBlockEntity::new,
                ModBlocks.CHISHI_ITEM_PIPE, ModBlocks.CHISHI_ITEM_PIPE_ADVANCED,
                ModBlocks.CHISHI_ITEM_PIPE_ELITE, ModBlocks.CHISHI_ITEM_PIPE_ULTIMATE);
        // 生命能量提纯器（赤能源驱动，输出生命能量固态物）
        CHISHI_LIFE_PURIFIER = be("chishi_life_purifier", ChishiLifePurifierBlockEntity::new,
                ModBlocks.CHISHI_LIFE_PURIFIER);
        // 液体管道（传输下界能量/燃料液体）
        CHISHI_FLUID_PIPE = be("chishi_fluid_pipe", ChishiFluidPipeBlockEntity::new,
                ModBlocks.CHISHI_FLUID_PIPE);
        // 封闭性衰竭管道（废料专用，单缓冲）
        CHISHI_EXHAUSTED_PIPE = be("chishi_exhausted_pipe", ChishiExhaustedPipeBlockEntity::new,
                ModBlocks.CHISHI_EXHAUSTED_PIPE);
        // 多流体废料管道（废料专用，多缓冲）
        CHISHI_MULTI_FLUID_WASTE_PIPE = be("chishi_multi_fluid_waste_pipe", ChishiMultiFluidWastePipeBlockEntity::new,
                ModBlocks.CHISHI_MULTI_FLUID_WASTE_PIPE);
        // 能量液化装置（赤能源驱动，产出下界能量液体）
        CHISHI_ENERGY_LIQUEFIER = be("chishi_energy_liquefier", ChishiEnergyLiquefierBlockEntity::new,
                ModBlocks.CHISHI_ENERGY_LIQUEFIER);
        // 能量加工器（赤能源驱动，固态物 + 下界能量液体 → 燃料）
        CHISHI_ENERGY_PROCESSOR = be("chishi_energy_processor", ChishiEnergyProcessorBlockEntity::new,
                ModBlocks.CHISHI_ENERGY_PROCESSOR);
        // 燃料装罐机（液体燃料 → 10L 燃料罐）
        CHISHI_FUEL_CANNER = be("chishi_fuel_canner", ChishiFuelCannerBlockEntity::new,
                ModBlocks.CHISHI_FUEL_CANNER);
        // 燃料混合器（燃料液体 1:1:1 调和 → 高级/终极混合燃料）
        CHISHI_FUEL_MIXER = be("chishi_fuel_mixer", ChishiFuelMixerBlockEntity::new,
                ModBlocks.CHISHI_FUEL_MIXER);
        // 生命活化器（生命能量无害化衰竭燃料）
        CHISHI_LIFE_ACTIVATOR = be("chishi_life_activator", ChishiLifeActivatorBlockEntity::new,
                ModBlocks.CHISHI_LIFE_ACTIVATOR);
        // 生命离心机（赤能源分离活化燃料为结晶产物）
        CHISHI_LIFE_CENTRIFUGE = be("chishi_life_centrifuge", ChishiLifeCentrifugeBlockEntity::new,
                ModBlocks.CHISHI_LIFE_CENTRIFUGE);
        // 物品重构仪（以衰竭结晶为代价嬗变物品）
        CHISHI_ITEM_RECONSTRUCTOR = be("chishi_item_reconstructor", ChishiItemReconstructorBlockEntity::new,
                ModBlocks.CHISHI_ITEM_RECONSTRUCTOR);
        // 液体储罐（一个方块实体类型承载 基础/高级/超级 三个等级）
        CHISHI_FLUID_TANK = be("chishi_fluid_tank", ChishiFluidTankBlockEntity::new,
                ModBlocks.CHISHI_FLUID_TANK_BASIC, ModBlocks.CHISHI_FLUID_TANK_ADVANCED,
                ModBlocks.CHISHI_FLUID_TANK_SUPER);
        // ===== 反应堆体系 =====
        // 控制器（多方块主方块）
        CHISHI_REACTOR_CONTROLLER = be("chishi_reactor_controller", ChishiReactorControllerBlockEntity::new,
                ModBlocks.CHISHI_REACTOR_CONTROLLER);
        // 燃料投放口（燃料罐缓冲槽）
        CHISHI_REACTOR_FUEL_PORT = be("chishi_reactor_fuel_port", ChishiReactorFuelPortBlockEntity::new,
                ModBlocks.CHISHI_REACTOR_FUEL_PORT);
        // 能量输出口（赤能源缓冲）
        CHISHI_REACTOR_ENERGY_OUTPUT = be("chishi_reactor_energy_output", ChishiReactorEnergyOutputBlockEntity::new,
                ModBlocks.CHISHI_REACTOR_ENERGY_OUTPUT);
        // 废品输出口（衰竭燃料缓冲）
        CHISHI_REACTOR_WASTE_PORT = be("chishi_reactor_waste_port", ChishiReactorWastePortBlockEntity::new,
                ModBlocks.CHISHI_REACTOR_WASTE_PORT);
        // 散热组件（单散热片槽位）
        CHISHI_REACTOR_COOLER = be("chishi_reactor_cooler", ChishiReactorCoolerBlockEntity::new,
                ModBlocks.CHISHI_REACTOR_COOLER);
        // 衰竭保存桶（专储衰竭燃料）
        CHISHI_EXHAUSTED_BARREL = be("chishi_exhausted_barrel", ChishiExhaustedBarrelBlockEntity::new,
                ModBlocks.CHISHI_EXHAUSTED_BARREL);
        // 躯体检查仪（纯展示，无 tick）
        CHISHI_BODY_SCANNER = be("chishi_body_scanner", ChishiBodyScannerBlockEntity::new,
                ModBlocks.CHISHI_BODY_SCANNER);
        // 生命分析台（纯度 100 样本解构）
        CHISHI_GENE_ANALYZER = be("chishi_gene_analyzer", ChishiGeneAnalyzerBlockEntity::new,
                ModBlocks.CHISHI_GENE_ANALYZER);
        // 部件培养舱（样本提纯 + 器官升级）
        CHISHI_CULTIVATOR = be("chishi_cultivator", ChishiCultivatorBlockEntity::new,
                ModBlocks.CHISHI_CULTIVATOR);
        // 生命结构台（基因序列 → 器官）
        CHISHI_LIFE_STRUCT = be("chishi_life_struct", ChishiLifeStructBlockEntity::new,
                ModBlocks.CHISHI_LIFE_STRUCT);
        // 手术仓（器官移植/摘除）
        CHISHI_SURGERY = be("chishi_surgery", ChishiSurgeryBlockEntity::new, ModBlocks.CHISHI_SURGERY);
        // 药剂台（永久/突破药剂制作）
        CHISHI_POTION_TABLE = be("chishi_potion_table", ChishiPotionTableBlockEntity::new,
                ModBlocks.CHISHI_POTION_TABLE);
        // 器官储藏库（按躯体槽位分页存储，生命能量维持）
        CHISHI_ORGAN_VAULT = be("chishi_organ_vault", ChishiOrganVaultBlockEntity::new,
                ModBlocks.CHISHI_ORGAN_VAULT);
        // 药剂库（大容量药剂仓库，同 NBT 自动合并）
        CHISHI_POTION_CABINET = be("chishi_potion_cabinet", ChishiPotionCabinetBlockEntity::new,
                ModBlocks.CHISHI_POTION_CABINET);
        // 样本库（大容量样本仓库，同 NBT 自动合并）
        CHISHI_SAMPLE_VAULT = be("chishi_sample_vault", ChishiSampleVaultBlockEntity::new,
                ModBlocks.CHISHI_SAMPLE_VAULT);
        // ===== 发生器矩阵 =====
        // 控制器（低级/高级两个方块共用一个方块实体类型，等级由方块实例决定）
        CHISHI_GEN_MATRIX_CONTROLLER = be("chishi_gen_matrix_controller", ChishiGenMatrixControllerBlockEntity::new,
                ModBlocks.CHISHI_GEN_MATRIX_CONTROLLER_BASIC, ModBlocks.CHISHI_GEN_MATRIX_CONTROLLER_ADVANCED);
        // 能量输出口（赤能源缓冲）
        CHISHI_GEN_ENERGY_OUTPUT = be("chishi_gen_energy_output", ChishiGenEnergyOutputPortBlockEntity::new,
                ModBlocks.CHISHI_GEN_ENERGY_OUTPUT);
        // 燃料输入口（燃料缓冲槽）
        CHISHI_GEN_FUEL_INPUT = be("chishi_gen_fuel_input", ChishiGenFuelInputPortBlockEntity::new,
                ModBlocks.CHISHI_GEN_FUEL_INPUT);
        // ===== 提纯矩阵 =====
        // 控制器（主方块，持有提纯状态）
        CHISHI_PURIFIER_MATRIX_CONTROLLER = be("chishi_purifier_matrix_controller",
                ChishiPurifierMatrixControllerBlockEntity::new, ModBlocks.CHISHI_PURIFIER_MATRIX_CONTROLLER);
        // 物品输入口（原料缓冲槽）
        CHISHI_PURIFIER_ITEM_INPUT = be("chishi_purifier_item_input", ChishiPurifierItemInputPortBlockEntity::new,
                ModBlocks.CHISHI_PURIFIER_ITEM_INPUT);
        // 物品输出口（产物缓冲槽）
        CHISHI_PURIFIER_ITEM_OUTPUT = be("chishi_purifier_item_output", ChishiPurifierItemOutputPortBlockEntity::new,
                ModBlocks.CHISHI_PURIFIER_ITEM_OUTPUT);
        // 能量输入口（赤能源缓冲，仅管道供能）
        CHISHI_PURIFIER_ENERGY_INPUT = be("chishi_purifier_energy_input", ChishiPurifierEnergyInputPortBlockEntity::new,
                ModBlocks.CHISHI_PURIFIER_ENERGY_INPUT);
        // ===== 生命转换矩阵 =====
        // 控制器（主方块，持有转换状态）
        CHISHI_LIFE_MATRIX_CONTROLLER = be("chishi_life_matrix_controller",
                ChishiLifeMatrixControllerBlockEntity::new, ModBlocks.CHISHI_LIFE_MATRIX_CONTROLLER);
        // 能量输入口（赤能源缓冲，仅管道供能）
        CHISHI_LIFE_MATRIX_ENERGY_INPUT = be("chishi_life_matrix_energy_input",
                ChishiLifeMatrixEnergyInputPortBlockEntity::new, ModBlocks.CHISHI_LIFE_MATRIX_ENERGY_INPUT);
        // 能量输出口（生命能量缓冲，仅管道抽取）
        CHISHI_LIFE_MATRIX_ENERGY_OUTPUT = be("chishi_life_matrix_energy_output",
                ChishiLifeMatrixEnergyOutputPortBlockEntity::new, ModBlocks.CHISHI_LIFE_MATRIX_ENERGY_OUTPUT);
        // ===== 无线赤能源 =====
        // 终端（外墙主方块：注册网络、授权卡、绑定储能、中转口能量）
        CHISHI_WIRELESS_TERMINAL = be("chishi_wireless_terminal", ChishiWirelessTerminalBlockEntity::new,
                ModBlocks.CHISHI_WIRELESS_TERMINAL);
        // 输入口（能量管道 → 终端网络的发送端）
        CHISHI_WIRELESS_INPUT_PORT = be("chishi_wireless_input_port", ChishiWirelessInputPortBlockEntity::new,
                ModBlocks.CHISHI_WIRELESS_INPUT_PORT);
        // 输出口（无线频道 → 能量管道的接收端）
        CHISHI_WIRELESS_OUTPUT_PORT = be("chishi_wireless_output_port", ChishiWirelessOutputPortBlockEntity::new,
                ModBlocks.CHISHI_WIRELESS_OUTPUT_PORT);
        // 区块加载构架（内腔功能件，持有 BE：自持中枢区块弱加载 ticket）
        CHISHI_WIRELESS_CHUNK_LOADER = be("chishi_wireless_chunk_loader", ChishiWirelessChunkLoaderBlockEntity::new,
                ModBlocks.CHISHI_WIRELESS_CHUNK_LOADER);
    }
}
