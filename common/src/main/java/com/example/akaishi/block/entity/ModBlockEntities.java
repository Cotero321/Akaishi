package com.example.akaishi.block.entity;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.ModBlocks;
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
    public static RegistrySupplier<BlockEntityType<AkaishiPurifierBlockEntity>> CHISHI_PURIFIER;
    /** 高级提纯构建方块方块实体类型（提纯矩阵外壳，单放独立提纯） */
    public static RegistrySupplier<BlockEntityType<AkaishiAdvancedPurifierBlockEntity>> CHISHI_ADVANCED_PURIFIER;
    /** 赤能源储存单元方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiEnergyCellBlockEntity>> CHISHI_ENERGY_CELL;
    /** 赤能源管道方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiEnergyPipeBlockEntity>> CHISHI_ENERGY_PIPE;
    /** 赤能源发生机方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiEnergyGeneratorBlockEntity>> CHISHI_ENERGY_GENERATOR;
    /** 小型赤能源组合结构（多方块主方块）方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiEnergyAssemblyBlockEntity>> CHISHI_ENERGY_ASSEMBLY;
    /** 赤能源储存串联器（多方块主方块）方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiEnergyCellSerializerBlockEntity>> CHISHI_ENERGY_CELL_SERIALIZER;
    /** 超级发生器架构核心（5×5×5 多方块主方块）方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiSuperGeneratorCoreBlockEntity>> CHISHI_SUPER_GENERATOR_CORE;
    /** 生命能量管道方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiLifeEnergyPipeBlockEntity>> CHISHI_LIFE_ENERGY_PIPE;
    /** 生命聚合转换器方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiLifeAggregationConverterBlockEntity>> CHISHI_LIFE_AGGREGATION_CONVERTER;
    /** 生命转换架构（3×3×3 多方块主方块）方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiLifeConversionArchitectureBlockEntity>> CHISHI_LIFE_CONVERSION_ARCHITECTURE;
    /** 生命能量储存器方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiLifeEnergyCellBlockEntity>> CHISHI_LIFE_ENERGY_CELL;
    /** 赤石能量聚合器方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiEnergyAggregatorBlockEntity>> CHISHI_ENERGY_AGGREGATOR;
    /** 赤石装备打造器方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiEquipmentForgerBlockEntity>> CHISHI_EQUIPMENT_FORGER;
    /** 赤红升级台方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiUpgradeStationBlockEntity>> CHISHI_UPGRADE_STATION;
    /** 生命的融合砧方块实体类型（赤石护甲 + 融合锭 → 生命融合护甲） */
    public static RegistrySupplier<BlockEntityType<AkaishiLifeFusionAnvilBlockEntity>> CHISHI_LIFE_FUSION_ANVIL;
    /** 创造模式能量源（赤/生命两种方块共用，无限输出） */
    public static RegistrySupplier<BlockEntityType<CreativeEnergySourceBlockEntity>> CREATIVE_ENERGY_SOURCE;
    /** 赤石催化器方块实体类型（4 级共用，等级由方块决定） */
    public static RegistrySupplier<BlockEntityType<AkaishiCatalystBlockEntity>> CHISHI_CATALYST;
    /** 自动收集器方块实体类型（4 级共用，等级由方块决定） */
    public static RegistrySupplier<BlockEntityType<AkaishiAutoCollectorBlockEntity>> CHISHI_AUTO_COLLECTOR;
    /** 物品管道方块实体类型（4 级共用，等级由方块决定） */
    public static RegistrySupplier<BlockEntityType<AkaishiItemPipeBlockEntity>> CHISHI_ITEM_PIPE;
    /** 生命能量提纯器方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiLifePurifierBlockEntity>> CHISHI_LIFE_PURIFIER;
    /** 液体管道方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiFluidPipeBlockEntity>> CHISHI_FLUID_PIPE;
    /** 封闭性衰竭管道方块实体类型（废料专用，单缓冲） */
    public static RegistrySupplier<BlockEntityType<AkaishiExhaustedPipeBlockEntity>> CHISHI_EXHAUSTED_PIPE;
    /** 多流体废料管道方块实体类型（废料专用，多缓冲） */
    public static RegistrySupplier<BlockEntityType<AkaishiMultiFluidWastePipeBlockEntity>> CHISHI_MULTI_FLUID_WASTE_PIPE;
    /** 能量液化装置方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiEnergyLiquefierBlockEntity>> CHISHI_ENERGY_LIQUEFIER;
    /** 能量加工器方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiEnergyProcessorBlockEntity>> CHISHI_ENERGY_PROCESSOR;
    /** 燃料装罐机方块实体 */
    public static RegistrySupplier<BlockEntityType<AkaishiFuelCannerBlockEntity>> CHISHI_FUEL_CANNER;
    /** 燃料混合器方块实体 */
    public static RegistrySupplier<BlockEntityType<AkaishiFuelMixerBlockEntity>> CHISHI_FUEL_MIXER;
    /** 生命活化器方块实体 */
    public static RegistrySupplier<BlockEntityType<AkaishiLifeActivatorBlockEntity>> CHISHI_LIFE_ACTIVATOR;
    /** 生命离心机方块实体 */
    public static RegistrySupplier<BlockEntityType<AkaishiLifeCentrifugeBlockEntity>> CHISHI_LIFE_CENTRIFUGE;
    /** 物品重构仪方块实体 */
    public static RegistrySupplier<BlockEntityType<AkaishiItemReconstructorBlockEntity>> CHISHI_ITEM_RECONSTRUCTOR;
    /** 赤石植物培养机方块实体（单格培养，种子保留） */
    public static RegistrySupplier<BlockEntityType<AkaishiPlantCultivatorBlockEntity>> CHISHI_PLANT_CULTIVATOR;
    /** 赤石压缩机方块实体（粉末 → 块） */
    public static RegistrySupplier<BlockEntityType<AkaishiCompressorBlockEntity>> CHISHI_COMPRESSOR;
    /** 赤石打粉机方块实体（矿物 → 粉末） */
    public static RegistrySupplier<BlockEntityType<AkaishiPulverizerBlockEntity>> CHISHI_PULVERIZER;
    /** 赤石变化器方块实体（物质 → 基底） */
    public static RegistrySupplier<BlockEntityType<AkaishiTransformerBlockEntity>> CHISHI_TRANSFORMER;
    /** 赤石矿机控制器方块实体（4 级控制器方块共用） */
    public static RegistrySupplier<BlockEntityType<AkaishiMinerControllerBlockEntity>> CHISHI_MINER_CONTROLLER;
    /** 矿机转口方块实体（产物缓冲 + 能量输入） */
    public static RegistrySupplier<BlockEntityType<AkaishiMinerPortBlockEntity>> CHISHI_MINER_PORT;
    /** 活化分馏器（活化结晶深度拆分） */
    public static RegistrySupplier<BlockEntityType<AkaishiActivatedFractionatorBlockEntity>> CHISHI_ACTIVATED_FRACTIONATOR;
    /** 聚变燃料聚合器（活化成分 → 等离子体） */
    public static RegistrySupplier<BlockEntityType<AkaishiFusionFuelAggregatorBlockEntity>> CHISHI_FUSION_FUEL_AGGREGATOR;
    /** 离子体填装器（等离子体 + 反应棒 → 燃料棒） */
    public static RegistrySupplier<BlockEntityType<AkaishiPlasmaFillerBlockEntity>> CHISHI_PLASMA_FILLER;
    /** 等离子体管道（第三传输家族，仅传等离子体） */
    public static RegistrySupplier<BlockEntityType<AkaishiPlasmaPipeBlockEntity>> CHISHI_PLASMA_PIPE;
    /** 液体储罐方块实体类型（基础/高级/超级共用） */
    public static RegistrySupplier<BlockEntityType<AkaishiFluidTankBlockEntity>> CHISHI_FLUID_TANK;
    /** 等离子体燃料储罐方块实体类型（仅存储等离子体） */
    public static RegistrySupplier<BlockEntityType<AkaishiPlasmaTankBlockEntity>> CHISHI_PLASMA_TANK;
    /** 反应堆控制器方块实体类型（主方块，持有全部反应堆状态） */
    public static RegistrySupplier<BlockEntityType<AkaishiReactorControllerBlockEntity>> CHISHI_REACTOR_CONTROLLER;
    /** 燃料投放口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiReactorFuelPortBlockEntity>> CHISHI_REACTOR_FUEL_PORT;
    /** 能量输出口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiReactorEnergyOutputBlockEntity>> CHISHI_REACTOR_ENERGY_OUTPUT;
    /** 废品输出口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiReactorWastePortBlockEntity>> CHISHI_REACTOR_WASTE_PORT;
    /** 散热组件方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiReactorCoolerBlockEntity>> CHISHI_REACTOR_COOLER;
    /** 衰竭保存桶方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiExhaustedBarrelBlockEntity>> CHISHI_EXHAUSTED_BARREL;
    /** 躯体检查仪方块实体类型（无 tick 纯展示） */
    public static RegistrySupplier<BlockEntityType<AkaishiBodyScannerBlockEntity>> CHISHI_BODY_SCANNER;
    /** 基因管理器方块实体类型（无 tick 纯管理面板） */
    public static RegistrySupplier<BlockEntityType<AkaishiGeneManagerBlockEntity>> CHISHI_GENE_MANAGER;
    /** 生命分析台方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiGeneAnalyzerBlockEntity>> CHISHI_GENE_ANALYZER;
    /** 部件培养舱方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiCultivatorBlockEntity>> CHISHI_CULTIVATOR;
    /** 生命结构台方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiLifeStructBlockEntity>> CHISHI_LIFE_STRUCT;
    /** 生命培育器方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiLifeBreederBlockEntity>> CHISHI_LIFE_BREEDER;
    /** 词条重铸仪方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiTraitReforgerBlockEntity>> CHISHI_TRAIT_REFORGER;
    /** 手术仓方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiSurgeryBlockEntity>> CHISHI_SURGERY;
    /** 药剂台方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiPotionTableBlockEntity>> CHISHI_POTION_TABLE;
    /** 器官储藏库方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiOrganVaultBlockEntity>> CHISHI_ORGAN_VAULT;
    /** 药剂库方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiPotionCabinetBlockEntity>> CHISHI_POTION_CABINET;
    /** 样本库方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiSampleVaultBlockEntity>> CHISHI_SAMPLE_VAULT;
    /** 衰变净化塔方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiDecayPurifierBlockEntity>> CHISHI_DECAY_PURIFIER;
    /** 母神祭坛（无 GUI 供奉位，NBT 识别 + 供奉物悬浮展示） */
    public static RegistrySupplier<BlockEntityType<AkaishiMotherAltarBlockEntity>> CHISHI_MOTHER_ALTAR;
    /** 发生器矩阵控制器方块实体类型（低级/高级共用，等级由方块决定） */
    public static RegistrySupplier<BlockEntityType<AkaishiGenMatrixControllerBlockEntity>> CHISHI_GEN_MATRIX_CONTROLLER;
    /** 发生器矩阵能量输出口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiGenEnergyOutputPortBlockEntity>> CHISHI_GEN_ENERGY_OUTPUT;
    /** 发生器矩阵燃料输入口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiGenFuelInputPortBlockEntity>> CHISHI_GEN_FUEL_INPUT;
    /** 提纯矩阵控制器方块实体类型（主方块，持有提纯状态） */
    public static RegistrySupplier<BlockEntityType<AkaishiPurifierMatrixControllerBlockEntity>> CHISHI_PURIFIER_MATRIX_CONTROLLER;
    /** 提纯矩阵物品输入口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiPurifierItemInputPortBlockEntity>> CHISHI_PURIFIER_ITEM_INPUT;
    /** 提纯矩阵物品输出口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiPurifierItemOutputPortBlockEntity>> CHISHI_PURIFIER_ITEM_OUTPUT;
    /** 提纯矩阵能量输入口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiPurifierEnergyInputPortBlockEntity>> CHISHI_PURIFIER_ENERGY_INPUT;
    /** 生命转换矩阵控制器方块实体类型（主方块，持有转换状态） */
    public static RegistrySupplier<BlockEntityType<AkaishiLifeMatrixControllerBlockEntity>> CHISHI_LIFE_MATRIX_CONTROLLER;
    /** 生命转换矩阵能量输入口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiLifeMatrixEnergyInputPortBlockEntity>> CHISHI_LIFE_MATRIX_ENERGY_INPUT;
    /** 生命转换矩阵能量输出口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiLifeMatrixEnergyOutputPortBlockEntity>> CHISHI_LIFE_MATRIX_ENERGY_OUTPUT;
    /** 无线赤能源终端方块实体类型（外墙主方块，网络能量中枢） */
    public static RegistrySupplier<BlockEntityType<AkaishiWirelessTerminalBlockEntity>> CHISHI_WIRELESS_TERMINAL;
    /** 无线赤能源输入口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiWirelessInputPortBlockEntity>> CHISHI_WIRELESS_INPUT_PORT;
    /** 无线赤能源输出口方块实体类型 */
    public static RegistrySupplier<BlockEntityType<AkaishiWirelessOutputPortBlockEntity>> CHISHI_WIRELESS_OUTPUT_PORT;
    /** 区块加载构架方块实体：锁所属终端结构控制器区块（弱加载） */
    public static RegistrySupplier<BlockEntityType<AkaishiWirelessChunkLoaderBlockEntity>> CHISHI_WIRELESS_CHUNK_LOADER;
    /** 聚变控制器方块实体类型（主方块，持有全部聚变状态） */
    public static RegistrySupplier<BlockEntityType<AkaishiFusionControllerBlockEntity>> CHISHI_FUSION_CONTROLLER;
    /** 聚变散热框架方块实体类型（单槽散热片） */
    public static RegistrySupplier<BlockEntityType<AkaishiFusionCoolerFrameBlockEntity>> CHISHI_FUSION_COOLER_FRAME;
    /** 聚变能量输出口方块实体类型（赤能源缓冲，纯发电） */
    public static RegistrySupplier<BlockEntityType<AkaishiFusionEnergyOutputBlockEntity>> CHISHI_FUSION_ENERGY_OUTPUT;
    /** 聚变物品输入口方块实体类型（燃料棒缓冲 27 槽） */
    public static RegistrySupplier<BlockEntityType<AkaishiFusionItemInputPortBlockEntity>> CHISHI_FUSION_ITEM_INPUT;
    /** 聚变物品输出口方块实体类型（生命灰烬缓冲 27 槽） */
    public static RegistrySupplier<BlockEntityType<AkaishiFusionItemOutputPortBlockEntity>> CHISHI_FUSION_ITEM_OUTPUT;

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
                .get(AkaishiMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                        () -> BlockEntityType.Builder.of(factory,
                                Arrays.stream(blocks).map(RegistrySupplier::get).toArray(Block[]::new)).build(null));
    }

    public static void register() {
        CHISHI_PURIFIER = be("akaishi_purifier", AkaishiPurifierBlockEntity::new, ModBlocks.CHISHI_PURIFIER);
        CHISHI_ADVANCED_PURIFIER = be("akaishi_advanced_purifier", AkaishiAdvancedPurifierBlockEntity::new,
                ModBlocks.CHISHI_ADVANCED_PURIFIER);
        // 储存单元：基础/高级/超级三个方块共用一个方块实体类型，等级由方块本身决定
        CHISHI_ENERGY_CELL = be("akaishi_energy_cell", AkaishiEnergyCellBlockEntity::new,
                ModBlocks.CHISHI_ENERGY_CELL_BASIC, ModBlocks.CHISHI_ENERGY_CELL_ADVANCED,
                ModBlocks.CHISHI_ENERGY_CELL_SUPER);
        // 管道：基础/高级/精英/终极四个方块共用一个方块实体类型，等级由方块本身决定
        CHISHI_ENERGY_PIPE = be("akaishi_energy_pipe", AkaishiEnergyPipeBlockEntity::new,
                ModBlocks.CHISHI_ENERGY_PIPE, ModBlocks.CHISHI_ENERGY_PIPE_ADVANCED,
                ModBlocks.CHISHI_ENERGY_PIPE_ELITE, ModBlocks.CHISHI_ENERGY_PIPE_ULTIMATE);
        CHISHI_ENERGY_GENERATOR = be("akaishi_energy_generator", AkaishiEnergyGeneratorBlockEntity::new,
                ModBlocks.CHISHI_ENERGY_GENERATOR);
        CHISHI_ENERGY_ASSEMBLY = be("akaishi_energy_assembly", AkaishiEnergyAssemblyBlockEntity::new,
                ModBlocks.CHISHI_ENERGY_ASSEMBLY);
        // 赤能源储存串联器（多方块主方块）
        CHISHI_ENERGY_CELL_SERIALIZER = be("akaishi_energy_cell_serializer", AkaishiEnergyCellSerializerBlockEntity::new,
                ModBlocks.CHISHI_ENERGY_CELL_SERIALIZER);
        // 超级发生器架构核心（5×5×5 多方块主方块）
        CHISHI_SUPER_GENERATOR_CORE = be("akaishi_super_generator_core", AkaishiSuperGeneratorCoreBlockEntity::new,
                ModBlocks.CHISHI_SUPER_GENERATOR_CORE);
        // 生命能量管道（独立方块实体类型，传输生命能量）
        CHISHI_LIFE_ENERGY_PIPE = be("akaishi_life_energy_pipe", AkaishiLifeEnergyPipeBlockEntity::new,
                ModBlocks.CHISHI_LIFE_ENERGY_PIPE);
        // 生命聚合转换器（单方块 / 外壳）
        CHISHI_LIFE_AGGREGATION_CONVERTER = be("akaishi_life_aggregation_converter",
                AkaishiLifeAggregationConverterBlockEntity::new, ModBlocks.CHISHI_LIFE_AGGREGATION_CONVERTER);
        // 生命转换架构（3×3×3 多方块主方块）
        CHISHI_LIFE_CONVERSION_ARCHITECTURE = be("akaishi_life_conversion_architecture",
                AkaishiLifeConversionArchitectureBlockEntity::new, ModBlocks.CHISHI_LIFE_CONVERSION_ARCHITECTURE);
        // 生命能量储存器（纯生命能量存储）
        CHISHI_LIFE_ENERGY_CELL = be("akaishi_life_energy_cell", AkaishiLifeEnergyCellBlockEntity::new,
                ModBlocks.CHISHI_LIFE_ENERGY_CELL);
        // 赤石能量聚合器
        CHISHI_ENERGY_AGGREGATOR = be("akaishi_energy_aggregator", AkaishiEnergyAggregatorBlockEntity::new,
                ModBlocks.CHISHI_ENERGY_AGGREGATOR);
        // 赤石装备打造器
        CHISHI_EQUIPMENT_FORGER = be("akaishi_equipment_forger", AkaishiEquipmentForgerBlockEntity::new,
                ModBlocks.CHISHI_EQUIPMENT_FORGER);
        // 赤红升级台
        CHISHI_UPGRADE_STATION = be("akaishi_upgrade_station", AkaishiUpgradeStationBlockEntity::new,
                ModBlocks.CHISHI_UPGRADE_STATION);
        // 生命的融合砧（赤石护甲 + 融合锭 → 生命融合护甲）
        CHISHI_LIFE_FUSION_ANVIL = be("akaishi_life_fusion_anvil", AkaishiLifeFusionAnvilBlockEntity::new,
                ModBlocks.CHISHI_LIFE_FUSION_ANVIL);
        // 创造模式能量源（赤能源 / 生命能量两种方块共用，无限输出测试）
        CREATIVE_ENERGY_SOURCE = be("creative_energy_source", CreativeEnergySourceBlockEntity::new,
                ModBlocks.CHISHI_CREATIVE_ENERGY_CELL, ModBlocks.CHISHI_CREATIVE_LIFE_CELL);
        // 赤石催化器：初/中/高/终四个方块共用一个方块实体类型，等级由方块本身决定
        CHISHI_CATALYST = be("akaishi_catalyst", AkaishiCatalystBlockEntity::new,
                ModBlocks.CHISHI_CATALYST_BASIC, ModBlocks.CHISHI_CATALYST_MEDIUM,
                ModBlocks.CHISHI_CATALYST_ADVANCED, ModBlocks.CHISHI_CATALYST_ULTIMATE);
        // 自动收集器：初/中/高/终四个方块共用一个方块实体类型，等级由方块本身决定
        CHISHI_AUTO_COLLECTOR = be("akaishi_auto_collector", AkaishiAutoCollectorBlockEntity::new,
                ModBlocks.CHISHI_COLLECTOR_BASIC, ModBlocks.CHISHI_COLLECTOR_MEDIUM,
                ModBlocks.CHISHI_COLLECTOR_ADVANCED, ModBlocks.CHISHI_COLLECTOR_ULTIMATE);
        // 物品管道：基础/高级/精英/终极四个方块共用一个方块实体类型，等级由方块本身决定
        CHISHI_ITEM_PIPE = be("akaishi_item_pipe", AkaishiItemPipeBlockEntity::new,
                ModBlocks.CHISHI_ITEM_PIPE, ModBlocks.CHISHI_ITEM_PIPE_ADVANCED,
                ModBlocks.CHISHI_ITEM_PIPE_ELITE, ModBlocks.CHISHI_ITEM_PIPE_ULTIMATE);
        // 生命能量提纯器（赤能源驱动，输出生命能量固态物）
        CHISHI_LIFE_PURIFIER = be("akaishi_life_purifier", AkaishiLifePurifierBlockEntity::new,
                ModBlocks.CHISHI_LIFE_PURIFIER);
        // 液体管道（传输下界能量/燃料液体）
        CHISHI_FLUID_PIPE = be("akaishi_fluid_pipe", AkaishiFluidPipeBlockEntity::new,
                ModBlocks.CHISHI_FLUID_PIPE);
        // 封闭性衰竭管道（废料专用，单缓冲）
        CHISHI_EXHAUSTED_PIPE = be("akaishi_exhausted_pipe", AkaishiExhaustedPipeBlockEntity::new,
                ModBlocks.CHISHI_EXHAUSTED_PIPE);
        // 多流体废料管道（废料专用，多缓冲）
        CHISHI_MULTI_FLUID_WASTE_PIPE = be("akaishi_multi_fluid_waste_pipe", AkaishiMultiFluidWastePipeBlockEntity::new,
                ModBlocks.CHISHI_MULTI_FLUID_WASTE_PIPE);
        // 能量液化装置（赤能源驱动，产出下界能量液体）
        CHISHI_ENERGY_LIQUEFIER = be("akaishi_energy_liquefier", AkaishiEnergyLiquefierBlockEntity::new,
                ModBlocks.CHISHI_ENERGY_LIQUEFIER);
        // 能量加工器（赤能源驱动，固态物 + 下界能量液体 → 燃料）
        CHISHI_ENERGY_PROCESSOR = be("akaishi_energy_processor", AkaishiEnergyProcessorBlockEntity::new,
                ModBlocks.CHISHI_ENERGY_PROCESSOR);
        // 燃料装罐机（液体燃料 → 10L 燃料罐）
        CHISHI_FUEL_CANNER = be("akaishi_fuel_canner", AkaishiFuelCannerBlockEntity::new,
                ModBlocks.CHISHI_FUEL_CANNER);
        // 燃料混合器（燃料液体 1:1:1 调和 → 高级/终极混合燃料）
        CHISHI_FUEL_MIXER = be("akaishi_fuel_mixer", AkaishiFuelMixerBlockEntity::new,
                ModBlocks.CHISHI_FUEL_MIXER);
        // 生命活化器（生命能量无害化衰竭燃料）
        CHISHI_LIFE_ACTIVATOR = be("akaishi_life_activator", AkaishiLifeActivatorBlockEntity::new,
                ModBlocks.CHISHI_LIFE_ACTIVATOR);
        // 生命离心机（赤能源分离活化燃料为结晶产物）
        CHISHI_LIFE_CENTRIFUGE = be("akaishi_life_centrifuge", AkaishiLifeCentrifugeBlockEntity::new,
                ModBlocks.CHISHI_LIFE_CENTRIFUGE);
        // 物品重构仪（以衰竭结晶为代价嬗变物品）
        CHISHI_ITEM_RECONSTRUCTOR = be("akaishi_item_reconstructor", AkaishiItemReconstructorBlockEntity::new,
                ModBlocks.CHISHI_ITEM_RECONSTRUCTOR);
        // 赤石植物培养机（单格培养，种子保留）
        CHISHI_PLANT_CULTIVATOR = be("akaishi_plant_cultivator", AkaishiPlantCultivatorBlockEntity::new,
                ModBlocks.CHISHI_PLANT_CULTIVATOR);
        // 赤石压缩机（粉末 → 块）
        CHISHI_COMPRESSOR = be("akaishi_compressor", AkaishiCompressorBlockEntity::new,
                ModBlocks.CHISHI_COMPRESSOR);
        // 赤石打粉机（矿物 → 粉末）
        CHISHI_PULVERIZER = be("akaishi_pulverizer", AkaishiPulverizerBlockEntity::new,
                ModBlocks.CHISHI_PULVERIZER);
        // 赤石变化器（物质 → 基底）
        CHISHI_TRANSFORMER = be("akaishi_transformer", AkaishiTransformerBlockEntity::new,
                ModBlocks.CHISHI_TRANSFORMER);
        // 赤石矿机：4 级控制器方块共用一个方块实体类型
        CHISHI_MINER_CONTROLLER = be("akaishi_miner_controller", AkaishiMinerControllerBlockEntity::new,
                ModBlocks.CHISHI_MINER_CONTROLLER_BASIC, ModBlocks.CHISHI_MINER_CONTROLLER_ADVANCED,
                ModBlocks.CHISHI_MINER_CONTROLLER_SUPER, ModBlocks.CHISHI_MINER_CONTROLLER_ULTIMATE);
        CHISHI_MINER_PORT = be("akaishi_miner_port", AkaishiMinerPortBlockEntity::new,
                ModBlocks.CHISHI_MINER_PORT);
        // 活化分馏器（活化结晶深度拆分：活化成分 + 衰竭结晶）
        CHISHI_ACTIVATED_FRACTIONATOR = be("akaishi_activated_fractionator", AkaishiActivatedFractionatorBlockEntity::new,
                ModBlocks.CHISHI_ACTIVATED_FRACTIONATOR);
        // ===== 聚变燃料体系 =====
        // 聚变燃料聚合器（活化成分 → 等离子体，赤能源驱动）
        CHISHI_FUSION_FUEL_AGGREGATOR = be("akaishi_fusion_fuel_aggregator", AkaishiFusionFuelAggregatorBlockEntity::new,
                ModBlocks.CHISHI_FUSION_FUEL_AGGREGATOR);
        // 离子体填装器（等离子体 + 反应棒 → 燃料棒）
        CHISHI_PLASMA_FILLER = be("akaishi_plasma_filler", AkaishiPlasmaFillerBlockEntity::new,
                ModBlocks.CHISHI_PLASMA_FILLER);
        // 等离子体管道（第三传输家族，仅传等离子体）
        CHISHI_PLASMA_PIPE = be("akaishi_plasma_pipe", AkaishiPlasmaPipeBlockEntity::new,
                ModBlocks.CHISHI_PLASMA_PIPE);
        // 液体储罐（一个方块实体类型承载 基础/高级/超级 三个等级）
        CHISHI_FLUID_TANK = be("akaishi_fluid_tank", AkaishiFluidTankBlockEntity::new,
                ModBlocks.CHISHI_FLUID_TANK_BASIC, ModBlocks.CHISHI_FLUID_TANK_ADVANCED,
                ModBlocks.CHISHI_FLUID_TANK_SUPER);
        // 等离子体燃料储罐（仅存储等离子体）
        CHISHI_PLASMA_TANK = be("akaishi_plasma_tank", AkaishiPlasmaTankBlockEntity::new,
                ModBlocks.CHISHI_PLASMA_TANK);
        // ===== 反应堆体系 =====
        // 控制器（多方块主方块）
        CHISHI_REACTOR_CONTROLLER = be("akaishi_reactor_controller", AkaishiReactorControllerBlockEntity::new,
                ModBlocks.CHISHI_REACTOR_CONTROLLER);
        // 燃料投放口（燃料罐缓冲槽）
        CHISHI_REACTOR_FUEL_PORT = be("akaishi_reactor_fuel_port", AkaishiReactorFuelPortBlockEntity::new,
                ModBlocks.CHISHI_REACTOR_FUEL_PORT);
        // 能量输出口（赤能源缓冲）
        CHISHI_REACTOR_ENERGY_OUTPUT = be("akaishi_reactor_energy_output", AkaishiReactorEnergyOutputBlockEntity::new,
                ModBlocks.CHISHI_REACTOR_ENERGY_OUTPUT);
        // 废品输出口（衰竭燃料缓冲）
        CHISHI_REACTOR_WASTE_PORT = be("akaishi_reactor_waste_port", AkaishiReactorWastePortBlockEntity::new,
                ModBlocks.CHISHI_REACTOR_WASTE_PORT);
        // 散热组件（单散热片槽位）
        CHISHI_REACTOR_COOLER = be("akaishi_reactor_cooler", AkaishiReactorCoolerBlockEntity::new,
                ModBlocks.CHISHI_REACTOR_COOLER);
        // 衰竭保存桶（专储衰竭燃料）
        CHISHI_EXHAUSTED_BARREL = be("akaishi_exhausted_barrel", AkaishiExhaustedBarrelBlockEntity::new,
                ModBlocks.CHISHI_EXHAUSTED_BARREL);
        // 躯体检查仪（纯展示，无 tick）
        CHISHI_BODY_SCANNER = be("akaishi_body_scanner", AkaishiBodyScannerBlockEntity::new,
                ModBlocks.CHISHI_BODY_SCANNER);
        // 基因管理器（已吸收基因强化管理，无 tick）
        CHISHI_GENE_MANAGER = be("akaishi_gene_manager", AkaishiGeneManagerBlockEntity::new,
                ModBlocks.CHISHI_GENE_MANAGER);
        // 生命分析台（纯度 100 样本解构）
        CHISHI_GENE_ANALYZER = be("akaishi_gene_analyzer", AkaishiGeneAnalyzerBlockEntity::new,
                ModBlocks.CHISHI_GENE_ANALYZER);
        // 部件培养舱（样本提纯 + 器官升级）
        CHISHI_CULTIVATOR = be("akaishi_cultivator", AkaishiCultivatorBlockEntity::new,
                ModBlocks.CHISHI_CULTIVATOR);
        // 生命结构台（基因序列 → 器官）
        CHISHI_LIFE_STRUCT = be("akaishi_life_struct", AkaishiLifeStructBlockEntity::new,
                ModBlocks.CHISHI_LIFE_STRUCT);
        // 生命培育器（器官 + 同源序列 + 衰竭结晶 → 突变器官）
        CHISHI_LIFE_BREEDER = be("akaishi_life_breeder", AkaishiLifeBreederBlockEntity::new,
                ModBlocks.CHISHI_LIFE_BREEDER);
        // 词条重铸仪（器官 + 衰竭结晶 → 原位替换指定突变词条）
        CHISHI_TRAIT_REFORGER = be("akaishi_trait_reforger", AkaishiTraitReforgerBlockEntity::new,
                ModBlocks.CHISHI_TRAIT_REFORGER);
        // 手术仓（器官移植/摘除）
        CHISHI_SURGERY = be("akaishi_surgery", AkaishiSurgeryBlockEntity::new, ModBlocks.CHISHI_SURGERY);
        // 药剂台（永久/突破药剂制作）
        CHISHI_POTION_TABLE = be("akaishi_potion_table", AkaishiPotionTableBlockEntity::new,
                ModBlocks.CHISHI_POTION_TABLE);
        // 器官储藏库（按躯体槽位分页存储，生命能量维持）
        CHISHI_ORGAN_VAULT = be("akaishi_organ_vault", AkaishiOrganVaultBlockEntity::new,
                ModBlocks.CHISHI_ORGAN_VAULT);
        // 药剂库（大容量药剂仓库，同 NBT 自动合并）
        CHISHI_POTION_CABINET = be("akaishi_potion_cabinet", AkaishiPotionCabinetBlockEntity::new,
                ModBlocks.CHISHI_POTION_CABINET);
        // 样本库（大容量样本仓库，同 NBT 自动合并）
        CHISHI_SAMPLE_VAULT = be("akaishi_sample_vault", AkaishiSampleVaultBlockEntity::new,
                ModBlocks.CHISHI_SAMPLE_VAULT);
        // 衰变净化塔（消耗赤能源净化衰竭区域）
        CHISHI_DECAY_PURIFIER = be("akaishi_decay_purifier", AkaishiDecayPurifierBlockEntity::new,
                ModBlocks.CHISHI_DECAY_PURIFIER);
        // 母神祭坛（黑山羊之母：NBT 识别供奉 + 供奉物悬浮展示）
        CHISHI_MOTHER_ALTAR = be("akaishi_mother_altar", AkaishiMotherAltarBlockEntity::new,
                ModBlocks.CHISHI_MOTHER_ALTAR);
        // ===== 发生器矩阵 =====
        // 控制器（低级/高级两个方块共用一个方块实体类型，等级由方块实例决定）
        CHISHI_GEN_MATRIX_CONTROLLER = be("akaishi_gen_matrix_controller", AkaishiGenMatrixControllerBlockEntity::new,
                ModBlocks.CHISHI_GEN_MATRIX_CONTROLLER_BASIC, ModBlocks.CHISHI_GEN_MATRIX_CONTROLLER_ADVANCED);
        // 能量输出口（赤能源缓冲）
        CHISHI_GEN_ENERGY_OUTPUT = be("akaishi_gen_energy_output", AkaishiGenEnergyOutputPortBlockEntity::new,
                ModBlocks.CHISHI_GEN_ENERGY_OUTPUT);
        // 燃料输入口（燃料缓冲槽）
        CHISHI_GEN_FUEL_INPUT = be("akaishi_gen_fuel_input", AkaishiGenFuelInputPortBlockEntity::new,
                ModBlocks.CHISHI_GEN_FUEL_INPUT);
        // ===== 提纯矩阵 =====
        // 控制器（主方块，持有提纯状态）
        CHISHI_PURIFIER_MATRIX_CONTROLLER = be("akaishi_purifier_matrix_controller",
                AkaishiPurifierMatrixControllerBlockEntity::new, ModBlocks.CHISHI_PURIFIER_MATRIX_CONTROLLER);
        // 物品输入口（原料缓冲槽）
        CHISHI_PURIFIER_ITEM_INPUT = be("akaishi_purifier_item_input", AkaishiPurifierItemInputPortBlockEntity::new,
                ModBlocks.CHISHI_PURIFIER_ITEM_INPUT);
        // 物品输出口（产物缓冲槽）
        CHISHI_PURIFIER_ITEM_OUTPUT = be("akaishi_purifier_item_output", AkaishiPurifierItemOutputPortBlockEntity::new,
                ModBlocks.CHISHI_PURIFIER_ITEM_OUTPUT);
        // 能量输入口（赤能源缓冲，仅管道供能）
        CHISHI_PURIFIER_ENERGY_INPUT = be("akaishi_purifier_energy_input", AkaishiPurifierEnergyInputPortBlockEntity::new,
                ModBlocks.CHISHI_PURIFIER_ENERGY_INPUT);
        // ===== 生命转换矩阵 =====
        // 控制器（主方块，持有转换状态）
        CHISHI_LIFE_MATRIX_CONTROLLER = be("akaishi_life_matrix_controller",
                AkaishiLifeMatrixControllerBlockEntity::new, ModBlocks.CHISHI_LIFE_MATRIX_CONTROLLER);
        // 能量输入口（赤能源缓冲，仅管道供能）
        CHISHI_LIFE_MATRIX_ENERGY_INPUT = be("akaishi_life_matrix_energy_input",
                AkaishiLifeMatrixEnergyInputPortBlockEntity::new, ModBlocks.CHISHI_LIFE_MATRIX_ENERGY_INPUT);
        // 能量输出口（生命能量缓冲，仅管道抽取）
        CHISHI_LIFE_MATRIX_ENERGY_OUTPUT = be("akaishi_life_matrix_energy_output",
                AkaishiLifeMatrixEnergyOutputPortBlockEntity::new, ModBlocks.CHISHI_LIFE_MATRIX_ENERGY_OUTPUT);
        // ===== 无线赤能源 =====
        // 终端（外墙主方块：注册网络、授权卡、绑定储能、中转口能量）
        CHISHI_WIRELESS_TERMINAL = be("akaishi_wireless_terminal", AkaishiWirelessTerminalBlockEntity::new,
                ModBlocks.CHISHI_WIRELESS_TERMINAL);
        // 输入口（能量管道 → 终端网络的发送端）
        CHISHI_WIRELESS_INPUT_PORT = be("akaishi_wireless_input_port", AkaishiWirelessInputPortBlockEntity::new,
                ModBlocks.CHISHI_WIRELESS_INPUT_PORT);
        // 输出口（无线频道 → 能量管道的接收端）
        CHISHI_WIRELESS_OUTPUT_PORT = be("akaishi_wireless_output_port", AkaishiWirelessOutputPortBlockEntity::new,
                ModBlocks.CHISHI_WIRELESS_OUTPUT_PORT);
        // 区块加载构架（内腔功能件，持有 BE：自持中枢区块弱加载 ticket）
        CHISHI_WIRELESS_CHUNK_LOADER = be("akaishi_wireless_chunk_loader", AkaishiWirelessChunkLoaderBlockEntity::new,
                ModBlocks.CHISHI_WIRELESS_CHUNK_LOADER);
        // ===== 聚变堆 =====
        // 控制器（多方块主方块）
        CHISHI_FUSION_CONTROLLER = be("akaishi_fusion_controller", AkaishiFusionControllerBlockEntity::new,
                ModBlocks.CHISHI_FUSION_CONTROLLER);
        // 散热框架（单槽散热片）
        CHISHI_FUSION_COOLER_FRAME = be("akaishi_fusion_cooler_frame", AkaishiFusionCoolerFrameBlockEntity::new,
                ModBlocks.CHISHI_FUSION_COOLER_FRAME);
        // 能量输出口（赤能源缓冲，纯发电）
        CHISHI_FUSION_ENERGY_OUTPUT = be("akaishi_fusion_energy_output", AkaishiFusionEnergyOutputBlockEntity::new,
                ModBlocks.CHISHI_FUSION_ENERGY_OUTPUT);
        // 物品输入口（燃料棒缓冲）
        CHISHI_FUSION_ITEM_INPUT = be("akaishi_fusion_item_input", AkaishiFusionItemInputPortBlockEntity::new,
                ModBlocks.CHISHI_FUSION_ITEM_INPUT);
        // 物品输出口（生命灰烬缓冲）
        CHISHI_FUSION_ITEM_OUTPUT = be("akaishi_fusion_item_output", AkaishiFusionItemOutputPortBlockEntity::new,
                ModBlocks.CHISHI_FUSION_ITEM_OUTPUT);
    }
}
