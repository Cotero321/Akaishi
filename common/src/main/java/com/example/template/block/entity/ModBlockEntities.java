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

    private ModBlockEntities() {
    }

    @SuppressWarnings("unchecked")
    public static void register() {
        CHISHI_PURIFIER = (RegistrySupplier<BlockEntityType<ChishiPurifierBlockEntity>>) (Object) RegistrarManager
                .get(TemplateMod.MOD_ID).get(Registries.BLOCK_ENTITY_TYPE)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_purifier"),
                        () -> BlockEntityType.Builder.of(ChishiPurifierBlockEntity::new, ModBlocks.CHISHI_PURIFIER.get())
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
    }
}
