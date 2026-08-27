package com.example.template.block;

import com.example.template.TemplateMod;
import com.example.template.energy.ChishiEnergyType;
import com.example.template.energy.EnergyCellTier;
import com.example.template.energy.EnergyPipeTier;
import com.example.template.energy.LifeEnergyType;
import com.example.template.fluid.FluidTankTier;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 赤石矿石块注册表。
 * 循环注册 16 个方块（4 浓度 × 4 环境）及其 BlockItem。
 */
public final class ModBlocks {

    /** 全部 16 个矿石组合定义 */
    public static final List<ChishiOreDef> ALL_ORES = buildAllOres();

    /** 组合定义 → 方块延迟注册引用 */
    private static final Map<ChishiOreDef, RegistrySupplier<Block>> BLOCK_BY_DEF = new ConcurrentHashMap<>();

    /** 粗制赤石块（9 赤石晶合成，提纯器原料） */
    public static RegistrySupplier<Block> RAW_CHISHI_BLOCK;
    /** 赤石提纯器（消耗赤能源提纯粗制块/水晶块，可作提纯矩阵中心） */
    public static RegistrySupplier<Block> CHISHI_PURIFIER;
    /** 高级提纯构建方块（单方块直接消耗赤能源提纯，提纯矩阵 3×3×3 外壳） */
    public static RegistrySupplier<Block> CHISHI_ADVANCED_PURIFIER;
    /** 赤能源储存单元（基础级） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_CELL_BASIC;
    /** 赤能源储存单元（高级） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_CELL_ADVANCED;
    /** 赤能源储存单元（超级） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_CELL_SUPER;
    /** 赤能源管道（基础，能量网络中继） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_PIPE;
    /** 赤能源管道（高级） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_PIPE_ADVANCED;
    /** 赤能源管道（精英） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_PIPE_ELITE;
    /** 赤能源管道（终极） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_PIPE_ULTIMATE;
    /** 浓缩赤石精华块（9 个浓缩精华压缩，装饰与储备用） */
    public static RegistrySupplier<Block> CHISHI_ESSENCE_BLOCK;
    /** 赤能源发生机（燃烧赤石材料产赤能源，单方块 / 多方块外壳） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_GENERATOR;
    /** 小型赤能源组合结构（多方块主方块） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_ASSEMBLY;
    /** 赤能源储存串联器（多方块主方块） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_CELL_SERIALIZER;
    /** 超级发生器架构核心（5×5×5 多方块主方块） */
    public static RegistrySupplier<Block> CHISHI_SUPER_GENERATOR_CORE;
    /** 生命能量管道（传输生命能量类型） */
    public static RegistrySupplier<Block> CHISHI_LIFE_ENERGY_PIPE;
    /** 生命聚合转换器（消耗赤能源聚合生命能量，单方块 / 生命转换架构外壳） */
    public static RegistrySupplier<Block> CHISHI_LIFE_AGGREGATION_CONVERTER;
    /** 生命转换架构（3×3×3 多方块主方块） */
    public static RegistrySupplier<Block> CHISHI_LIFE_CONVERSION_ARCHITECTURE;
    /** 生命能量储存器（纯生命能量存储） */
    public static RegistrySupplier<Block> CHISHI_LIFE_ENERGY_CELL;
    /** 赤石能量聚合器（赤能源 + 下界合金锭 → 赤石锭） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_AGGREGATOR;
    /** 赤石装备打造器（赤能源 + 赤石锭 → 赤石装备） */
    public static RegistrySupplier<Block> CHISHI_EQUIPMENT_FORGER;
    /** 赤红升级台（模板 + 槽位 → 升级赤石装备） */
    public static RegistrySupplier<Block> CHISHI_UPGRADE_STATION;
    /** 创造赤能源储存原件（无限输出测试方块） */
    public static RegistrySupplier<Block> CHISHI_CREATIVE_ENERGY_CELL;
    /** 创造生命能量储存原件（无限输出测试方块） */
    public static RegistrySupplier<Block> CHISHI_CREATIVE_LIFE_CELL;
    /** 赤石水晶母岩（瑕疵）：晶洞外层自然生成，可生长水晶簇，可在聚合器升级 */
    public static RegistrySupplier<Block> CHISHI_GEODE_FLAWED;
    /** 赤石水晶母岩（普通） */
    public static RegistrySupplier<Block> CHISHI_GEODE_NORMAL;
    /** 赤石水晶母岩（完好） */
    public static RegistrySupplier<Block> CHISHI_GEODE_PRISTINE;
    /** 赤石水晶母岩（完美） */
    public static RegistrySupplier<Block> CHISHI_GEODE_PERFECT;
    /** 赤石水晶簇：母岩生长/晶洞生成，破坏掉落赤石精华 */
    public static RegistrySupplier<Block> CHISHI_CRYSTAL_CLUSTER;
    /** 赤石水晶块：9 簇合成，提纯器提纯成精华 */
    public static RegistrySupplier<Block> CHISHI_CRYSTAL_BLOCK;
    /** 赤石催化器（初级）：催生范围内母岩生长水晶簇 */
    public static RegistrySupplier<Block> CHISHI_CATALYST_BASIC;
    /** 赤石催化器（中级） */
    public static RegistrySupplier<Block> CHISHI_CATALYST_MEDIUM;
    /** 赤石催化器（高级） */
    public static RegistrySupplier<Block> CHISHI_CATALYST_ADVANCED;
    /** 赤石催化器（终极） */
    public static RegistrySupplier<Block> CHISHI_CATALYST_ULTIMATE;
    /** 自动收集器（初级）：自动收获范围内水晶簇 */
    public static RegistrySupplier<Block> CHISHI_COLLECTOR_BASIC;
    /** 自动收集器（中级） */
    public static RegistrySupplier<Block> CHISHI_COLLECTOR_MEDIUM;
    /** 自动收集器（高级） */
    public static RegistrySupplier<Block> CHISHI_COLLECTOR_ADVANCED;
    /** 自动收集器（终极） */
    public static RegistrySupplier<Block> CHISHI_COLLECTOR_ULTIMATE;
    /** 物品管道（基础）：物流网络中继，1 个/tick */
    public static RegistrySupplier<Block> CHISHI_ITEM_PIPE;
    /** 物品管道（高级） */
    public static RegistrySupplier<Block> CHISHI_ITEM_PIPE_ADVANCED;
    /** 物品管道（精英） */
    public static RegistrySupplier<Block> CHISHI_ITEM_PIPE_ELITE;
    /** 物品管道（终极）：64 个/tick */
    public static RegistrySupplier<Block> CHISHI_ITEM_PIPE_ULTIMATE;
    /** 生命能量提纯器（赤能源驱动，1000 生命能量 + 10M 赤能源 → 1 生命能量固态物） */
    public static RegistrySupplier<Block> CHISHI_LIFE_PURIFIER;
    /** 液体管道：传输下界能量/燃料液体，可对接 MEK 等外部液体方块 */
    public static RegistrySupplier<Block> CHISHI_FLUID_PIPE;
    /** 能量液化装置（赤能源驱动，下界之星/凋零玫瑰 → 下界能量液体） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_LIQUEFIER;
    /** 能量加工器（赤能源驱动，生命固态物 + 下界能量液体 → 反应堆燃料） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_PROCESSOR;
    /** 燃料装罐机：液体燃料灌装进 10L 燃料罐 */
    public static RegistrySupplier<Block> CHISHI_FUEL_CANNER;
    /** 燃料混合器：两种燃料液体 1:1:1 调和为高阶混合燃料 */
    public static RegistrySupplier<Block> CHISHI_FUEL_MIXER;
    /** 液体储罐（基础/高级/超级：16k/64k/256k mb，管道存取液体） */
    public static RegistrySupplier<Block> CHISHI_FLUID_TANK_BASIC;
    public static RegistrySupplier<Block> CHISHI_FLUID_TANK_ADVANCED;
    public static RegistrySupplier<Block> CHISHI_FLUID_TANK_SUPER;
    /** 反应堆外壳：多方块外壁（控制器/投放口/输出口/废品口也属外壁），右键打开控制器 */
    public static RegistrySupplier<Block> CHISHI_REACTOR_SHELL;
    /** 反应堆控制器：主方块，持有全部反应堆状态，右键打开控制界面 */
    public static RegistrySupplier<Block> CHISHI_REACTOR_CONTROLLER;
    /** 燃料投放口：燃料罐物品输入（管道+手动），自动分配到控制器空燃料槽 */
    public static RegistrySupplier<Block> CHISHI_REACTOR_FUEL_PORT;
    /** 能量输出口：赤能源输出（纯发电，管道只可抽取） */
    public static RegistrySupplier<Block> CHISHI_REACTOR_ENERGY_OUTPUT;
    /** 废品输出口：衰竭燃料输出（液体管道只可抽取） */
    public static RegistrySupplier<Block> CHISHI_REACTOR_WASTE_PORT;
    /** 燃料棒组件：每根解锁 1 个燃料槽 */
    public static RegistrySupplier<Block> CHISHI_REACTOR_FUEL_ROD;
    /** 散热组件：装入散热片，贴邻燃料棒才有效 */
    public static RegistrySupplier<Block> CHISHI_REACTOR_COOLER;
    /** 反应核心：燃烧结算中心 */
    public static RegistrySupplier<Block> CHISHI_REACTOR_CORE;
    /** 衰竭保存桶：专储衰竭的生命燃料，带 GUI 液位 */
    public static RegistrySupplier<Block> CHISHI_EXHAUSTED_BARREL;

    private ModBlocks() {
    }

    public static void register() {
        for (ChishiOreDef def : ALL_ORES) {
            ResourceLocation id = new ResourceLocation(TemplateMod.MOD_ID, def.id());

            // 方块实例必须延迟到注册事件中创建：new Block 会创建侵入式 Holder，
            // 若在注册表冻结后执行将抛 "Registry is already frozen"
            Registrar<Block> blockRegistrar = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.BLOCK);
            RegistrySupplier<Block> block = blockRegistrar.register(id, ChishiOreBlock::new);
            // 方块物品一并注册，方便玩家在创造模式取用
            RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                    .register(id, () -> new BlockItem(block.get(), new Item.Properties()));

            BLOCK_BY_DEF.put(def, block);
        }

        // 粗制赤石块 + 赤石提纯器（含各自 BlockItem）
        Registrar<Block> blockRegistrar = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.BLOCK);
        RAW_CHISHI_BLOCK = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "raw_chishi_block"), ChishiBlock::new);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "raw_chishi_block"),
                        () -> new BlockItem(RAW_CHISHI_BLOCK.get(), new Item.Properties()));

        CHISHI_PURIFIER = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_purifier"), ChishiPurifierBlock::new);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_purifier"),
                        () -> new BlockItem(CHISHI_PURIFIER.get(), new Item.Properties()));

        // 高级提纯构建方块：单方块直接消耗赤能源提纯，作为"提纯矩阵"（3×3×3）外壳
        CHISHI_ADVANCED_PURIFIER = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_advanced_purifier"), ChishiAdvancedPurifierBlock::new);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_advanced_purifier"),
                        () -> new BlockItem(CHISHI_ADVANCED_PURIFIER.get(), new Item.Properties()));

        // 赤能源储存单元（基础/高级/超级）+ 赤能源管道，均为含方块实体的参数化/独立方块
        CHISHI_ENERGY_CELL_BASIC = registerCell(blockRegistrar, "chishi_energy_cell_basic", EnergyCellTier.BASIC);
        CHISHI_ENERGY_CELL_ADVANCED = registerCell(blockRegistrar, "chishi_energy_cell_advanced", EnergyCellTier.ADVANCED);
        CHISHI_ENERGY_CELL_SUPER = registerCell(blockRegistrar, "chishi_energy_cell_super", EnergyCellTier.SUPER);

        CHISHI_ENERGY_PIPE = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_pipe"),
                () -> new ChishiEnergyPipeBlock(EnergyPipeTier.BASIC));
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_pipe"),
                        () -> new BlockItem(CHISHI_ENERGY_PIPE.get(), new Item.Properties()));

        CHISHI_ENERGY_PIPE_ADVANCED = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_pipe_advanced"),
                () -> new ChishiEnergyPipeBlock(EnergyPipeTier.ADVANCED));
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_pipe_advanced"),
                        () -> new BlockItem(CHISHI_ENERGY_PIPE_ADVANCED.get(), new Item.Properties()));

        CHISHI_ENERGY_PIPE_ELITE = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_pipe_elite"),
                () -> new ChishiEnergyPipeBlock(EnergyPipeTier.ELITE));
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_pipe_elite"),
                        () -> new BlockItem(CHISHI_ENERGY_PIPE_ELITE.get(), new Item.Properties()));

        CHISHI_ENERGY_PIPE_ULTIMATE = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_pipe_ultimate"),
                () -> new ChishiEnergyPipeBlock(EnergyPipeTier.ULTIMATE));
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_pipe_ultimate"),
                        () -> new BlockItem(CHISHI_ENERGY_PIPE_ULTIMATE.get(), new Item.Properties()));

        // 浓缩赤石精华块（普通方块 + BlockItem）
        CHISHI_ESSENCE_BLOCK = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_essence_block"),
                () -> new Block(Block.Properties.of().strength(3.0F, 6.0F).requiresCorrectToolForDrops()));
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_essence_block"),
                        () -> new BlockItem(CHISHI_ESSENCE_BLOCK.get(), new Item.Properties()));

        // 赤能源发生机 + 小型赤能源组合结构（含各自 BlockItem）
        CHISHI_ENERGY_GENERATOR = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_generator"), ChishiEnergyGeneratorBlock::new);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_generator"),
                        () -> new BlockItem(CHISHI_ENERGY_GENERATOR.get(), new Item.Properties()));

        CHISHI_ENERGY_ASSEMBLY = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_assembly"), ChishiEnergyAssemblyBlock::new);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_assembly"),
                        () -> new BlockItem(CHISHI_ENERGY_ASSEMBLY.get(), new Item.Properties()));

        // 赤能源储存串联器（3×3×3 多方块主方块，26 个储存单元环绕成型）
        CHISHI_ENERGY_CELL_SERIALIZER = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_cell_serializer"),
                ChishiEnergyCellSerializerBlock::new);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_cell_serializer"),
                        () -> new BlockItem(CHISHI_ENERGY_CELL_SERIALIZER.get(), new Item.Properties()));

        // 超级发生器架构核心（5×5×5 多方块主方块，124 台发生机环绕成型）
        CHISHI_SUPER_GENERATOR_CORE = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_super_generator_core"),
                ChishiSuperGeneratorCoreBlock::new);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_super_generator_core"),
                        () -> new BlockItem(CHISHI_SUPER_GENERATOR_CORE.get(), new Item.Properties()));

        // 生命能量管道（独立能量类型，与赤能源管道互不连通）
        CHISHI_LIFE_ENERGY_PIPE = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_life_energy_pipe"),
                ChishiLifeEnergyPipeBlock::new);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_life_energy_pipe"),
                        () -> new BlockItem(CHISHI_LIFE_ENERGY_PIPE.get(), new Item.Properties()));

        // 生命聚合转换器（单方块独立转换 / 生命转换架构外壳）+ 生命转换架构（3×3×3 多方块主方块）
        CHISHI_LIFE_AGGREGATION_CONVERTER = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_life_aggregation_converter"),
                ChishiLifeAggregationConverterBlock::new);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_life_aggregation_converter"),
                        () -> new BlockItem(CHISHI_LIFE_AGGREGATION_CONVERTER.get(), new Item.Properties()));

        CHISHI_LIFE_CONVERSION_ARCHITECTURE = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_life_conversion_architecture"),
                ChishiLifeConversionArchitectureBlock::new);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_life_conversion_architecture"),
                        () -> new BlockItem(CHISHI_LIFE_CONVERSION_ARCHITECTURE.get(), new Item.Properties()));

        // 生命能量储存器（纯生命能量存储，单方块）
        CHISHI_LIFE_ENERGY_CELL = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_life_energy_cell"),
                ChishiLifeEnergyCellBlock::new);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_life_energy_cell"),
                        () -> new BlockItem(CHISHI_LIFE_ENERGY_CELL.get(), new Item.Properties()));

        // 赤石能量聚合器（10M 赤能源 + 下界合金锭 → 赤石锭）
        CHISHI_ENERGY_AGGREGATOR = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_aggregator"),
                ChishiEnergyAggregatorBlock::new);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_aggregator"),
                        () -> new BlockItem(CHISHI_ENERGY_AGGREGATOR.get(), new Item.Properties()));

        // 赤石装备打造器（赤能源 + 赤石锭 + 下界合金装备 → 赤石装备）
        CHISHI_EQUIPMENT_FORGER = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_equipment_forger"),
                ChishiEquipmentForgerBlock::new);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_equipment_forger"),
                        () -> new BlockItem(CHISHI_EQUIPMENT_FORGER.get(), new Item.Properties()));

        // 赤红升级台（模板 + 槽位 + 赤能源 → 升级赤石装备）
        CHISHI_UPGRADE_STATION = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_upgrade_station"),
                ChishiUpgradeStationBlock::new);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_upgrade_station"),
                        () -> new BlockItem(CHISHI_UPGRADE_STATION.get(), new Item.Properties()));

        // 赤石水晶母岩（4 级）：晶洞外层自然生成，放置后生长水晶簇，聚合器可升级
        CHISHI_GEODE_FLAWED = registerGeode(blockRegistrar, "chishi_geode_flawed", ChishiGeodeBlock.GeodeTier.FLAWED, MapColor.COLOR_LIGHT_GRAY);
        CHISHI_GEODE_NORMAL = registerGeode(blockRegistrar, "chishi_geode_normal", ChishiGeodeBlock.GeodeTier.NORMAL, MapColor.COLOR_RED);
        CHISHI_GEODE_PRISTINE = registerGeode(blockRegistrar, "chishi_geode_pristine", ChishiGeodeBlock.GeodeTier.PRISTINE, MapColor.GOLD);
        CHISHI_GEODE_PERFECT = registerGeode(blockRegistrar, "chishi_geode_perfect", ChishiGeodeBlock.GeodeTier.PERFECT, MapColor.COLOR_PURPLE);

        // 赤石水晶簇（破坏掉落精华）
        CHISHI_CRYSTAL_CLUSTER = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_crystal_cluster"),
                ChishiCrystalClusterBlock::new);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_crystal_cluster"),
                        () -> new BlockItem(CHISHI_CRYSTAL_CLUSTER.get(), new Item.Properties()));

        // 赤石水晶块（提纯器提纯成精华）
        CHISHI_CRYSTAL_BLOCK = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_crystal_block"),
                ChishiCrystalBlock::new);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_crystal_block"),
                        () -> new BlockItem(CHISHI_CRYSTAL_BLOCK.get(), new Item.Properties()));

        // 赤石催化器（4 级）：催生范围内母岩生长水晶簇，消耗赤能源
        CHISHI_CATALYST_BASIC = registerCatalyst(blockRegistrar, "chishi_catalyst_basic", ChishiCatalystBlock.CatalystTier.BASIC);
        CHISHI_CATALYST_MEDIUM = registerCatalyst(blockRegistrar, "chishi_catalyst_medium", ChishiCatalystBlock.CatalystTier.MEDIUM);
        CHISHI_CATALYST_ADVANCED = registerCatalyst(blockRegistrar, "chishi_catalyst_advanced", ChishiCatalystBlock.CatalystTier.ADVANCED);
        CHISHI_CATALYST_ULTIMATE = registerCatalyst(blockRegistrar, "chishi_catalyst_ultimate", ChishiCatalystBlock.CatalystTier.ULTIMATE);

        // 自动收集器（4 级）：自动收获范围内水晶簇，精华存入内部 27 槽容器
        CHISHI_COLLECTOR_BASIC = registerCollector(blockRegistrar, "chishi_collector_basic", ChishiAutoCollectorBlock.CollectorTier.BASIC);
        CHISHI_COLLECTOR_MEDIUM = registerCollector(blockRegistrar, "chishi_collector_medium", ChishiAutoCollectorBlock.CollectorTier.MEDIUM);
        CHISHI_COLLECTOR_ADVANCED = registerCollector(blockRegistrar, "chishi_collector_advanced", ChishiAutoCollectorBlock.CollectorTier.ADVANCED);
        CHISHI_COLLECTOR_ULTIMATE = registerCollector(blockRegistrar, "chishi_collector_ultimate", ChishiAutoCollectorBlock.CollectorTier.ULTIMATE);

        // 物品管道（4 级）：物流网络中继，传输物品到相连容器/机器，终极 64 个/tick
        CHISHI_ITEM_PIPE = registerItemPipe(blockRegistrar, "chishi_item_pipe", ChishiItemPipeBlock.ItemPipeTier.BASIC);
        CHISHI_ITEM_PIPE_ADVANCED = registerItemPipe(blockRegistrar, "chishi_item_pipe_advanced", ChishiItemPipeBlock.ItemPipeTier.ADVANCED);
        CHISHI_ITEM_PIPE_ELITE = registerItemPipe(blockRegistrar, "chishi_item_pipe_elite", ChishiItemPipeBlock.ItemPipeTier.ELITE);
        CHISHI_ITEM_PIPE_ULTIMATE = registerItemPipe(blockRegistrar, "chishi_item_pipe_ultimate", ChishiItemPipeBlock.ItemPipeTier.ULTIMATE);

        // 生命能量提纯器（双能量输入：赤能源驱动 + 生命能量原料，输出生命能量固态物）
        CHISHI_LIFE_PURIFIER = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_life_purifier"),
                ChishiLifePurifierBlock::new);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_life_purifier"),
                        () -> new BlockItem(CHISHI_LIFE_PURIFIER.get(), new Item.Properties()));

        // 液体管道（单级，传输下界能量/燃料液体）
        CHISHI_FLUID_PIPE = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_fluid_pipe"),
                ChishiFluidPipeBlock::new);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_fluid_pipe"),
                        () -> new BlockItem(CHISHI_FLUID_PIPE.get(), new Item.Properties()));

        // 能量液化装置（赤能源驱动，下界之星 → 至纯能量 / 凋零玫瑰 → 复合能量）
        CHISHI_ENERGY_LIQUEFIER = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_liquefier"),
                ChishiEnergyLiquefierBlock::new);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_liquefier"),
                        () -> new BlockItem(CHISHI_ENERGY_LIQUEFIER.get(), new Item.Properties()));

        // 能量加工器（赤能源驱动，生命固态物 + 下界能量液体 → 反应堆燃料）
        CHISHI_ENERGY_PROCESSOR = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_processor"),
                ChishiEnergyProcessorBlock::new);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_energy_processor"),
                        () -> new BlockItem(CHISHI_ENERGY_PROCESSOR.get(), new Item.Properties()));

        // 燃料装罐机（液体燃料 → 10L 燃料罐）
        CHISHI_FUEL_CANNER = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_fuel_canner"),
                ChishiFuelCannerBlock::new);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_fuel_canner"),
                        () -> new BlockItem(CHISHI_FUEL_CANNER.get(), new Item.Properties()));

        // 燃料混合器（燃料液体 1:1:1 调和 → 高级/终极混合燃料）
        CHISHI_FUEL_MIXER = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_fuel_mixer"),
                ChishiFuelMixerBlock::new);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "chishi_fuel_mixer"),
                        () -> new BlockItem(CHISHI_FUEL_MIXER.get(), new Item.Properties()));

        // 液体储罐（基础/高级/超级，容量递增，可被液体管道注入/抽取）
        CHISHI_FLUID_TANK_BASIC = registerFluidTank(blockRegistrar, "chishi_fluid_tank_basic", FluidTankTier.BASIC);
        CHISHI_FLUID_TANK_ADVANCED = registerFluidTank(blockRegistrar, "chishi_fluid_tank_advanced", FluidTankTier.ADVANCED);
        CHISHI_FLUID_TANK_SUPER = registerFluidTank(blockRegistrar, "chishi_fluid_tank_super", FluidTankTier.SUPER);

        // 创造模式能量源（测试用，无限输出）：赤能源 / 生命能量
        CHISHI_CREATIVE_ENERGY_CELL = blockRegistrar.register(
                new ResourceLocation(TemplateMod.MOD_ID, "creative_chishi_energy_cell"),
                () -> new CreativeEnergySourceBlock(ChishiEnergyType.INSTANCE));
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "creative_chishi_energy_cell"),
                        () -> new BlockItem(CHISHI_CREATIVE_ENERGY_CELL.get(), new Item.Properties()));
        CHISHI_CREATIVE_LIFE_CELL = blockRegistrar.register(
                new ResourceLocation(TemplateMod.MOD_ID, "creative_life_energy_cell"),
                () -> new CreativeEnergySourceBlock(LifeEnergyType.INSTANCE));
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, "creative_life_energy_cell"),
                        () -> new BlockItem(CHISHI_CREATIVE_LIFE_CELL.get(), new Item.Properties()));

        // ===== 反应堆体系（9 方块）=====
        CHISHI_REACTOR_SHELL = registerReactorBlock(blockRegistrar, "chishi_reactor_shell", ChishiReactorShellBlock::new);
        CHISHI_REACTOR_CONTROLLER = registerReactorBlock(blockRegistrar, "chishi_reactor_controller", ChishiReactorControllerBlock::new);
        CHISHI_REACTOR_FUEL_PORT = registerReactorBlock(blockRegistrar, "chishi_reactor_fuel_port", ChishiReactorFuelPortBlock::new);
        CHISHI_REACTOR_ENERGY_OUTPUT = registerReactorBlock(blockRegistrar, "chishi_reactor_energy_output", ChishiReactorEnergyOutputBlock::new);
        CHISHI_REACTOR_WASTE_PORT = registerReactorBlock(blockRegistrar, "chishi_reactor_waste_port", ChishiReactorWastePortBlock::new);
        CHISHI_REACTOR_FUEL_ROD = registerReactorBlock(blockRegistrar, "chishi_reactor_fuel_rod", ChishiReactorFuelRodBlock::new);
        CHISHI_REACTOR_COOLER = registerReactorBlock(blockRegistrar, "chishi_reactor_cooler", ChishiReactorCoolerBlock::new);
        CHISHI_REACTOR_CORE = registerReactorBlock(blockRegistrar, "chishi_reactor_core", ChishiReactorCoreBlock::new);
        CHISHI_EXHAUSTED_BARREL = registerReactorBlock(blockRegistrar, "chishi_exhausted_barrel", ChishiExhaustedBarrelBlock::new);
    }

    /** 注册一个指定等级的赤能源储存单元及其 BlockItem */
    private static RegistrySupplier<Block> registerCell(Registrar<Block> blockRegistrar, String id, EnergyCellTier tier) {
        RegistrySupplier<Block> block = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, id),
                () -> new ChishiEnergyCellBlock(tier));
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, id),
                        () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** 注册一个指定等级的液体储罐及其 BlockItem */
    private static RegistrySupplier<Block> registerFluidTank(Registrar<Block> blockRegistrar, String id, FluidTankTier tier) {
        RegistrySupplier<Block> block = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, id),
                () -> new ChishiFluidTankBlock(tier));
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, id),
                        () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** 注册一个指定等级的赤石水晶母岩及其 BlockItem */
    private static RegistrySupplier<Block> registerGeode(Registrar<Block> blockRegistrar, String id,
                                                         ChishiGeodeBlock.GeodeTier tier, MapColor color) {
        RegistrySupplier<Block> block = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, id),
                () -> new ChishiGeodeBlock(tier, color));
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, id),
                        () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** 注册一个指定等级的赤石催化器及其 BlockItem */
    private static RegistrySupplier<Block> registerCatalyst(Registrar<Block> blockRegistrar, String id,
                                                            ChishiCatalystBlock.CatalystTier tier) {
        RegistrySupplier<Block> block = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, id),
                () -> new ChishiCatalystBlock(tier));
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, id),
                        () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** 注册一个指定等级的自动收集器及其 BlockItem */
    private static RegistrySupplier<Block> registerCollector(Registrar<Block> blockRegistrar, String id,
                                                             ChishiAutoCollectorBlock.CollectorTier tier) {
        RegistrySupplier<Block> block = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, id),
                () -> new ChishiAutoCollectorBlock(tier));
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, id),
                        () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** 注册一个指定等级的物品管道及其 BlockItem */
    private static RegistrySupplier<Block> registerItemPipe(Registrar<Block> blockRegistrar, String id,
                                                            ChishiItemPipeBlock.ItemPipeTier tier) {
        RegistrySupplier<Block> block = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, id),
                () -> new ChishiItemPipeBlock(tier));
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, id),
                        () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** 注册一个反应堆方块及其 BlockItem（无参数构造） */
    private static RegistrySupplier<Block> registerReactorBlock(Registrar<Block> blockRegistrar, String id,
                                                                java.util.function.Supplier<Block> factory) {
        RegistrySupplier<Block> block = blockRegistrar.register(new ResourceLocation(TemplateMod.MOD_ID, id), factory);
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, id),
                        () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** 获取对应组合定义的方块（注册完成后可用） */
    public static Block get(ChishiOreDef def) {
        return BLOCK_BY_DEF.get(def).get();
    }

    /** 生成 4 × 4 全部组合 */
    private static List<ChishiOreDef> buildAllOres() {
        List<ChishiOreDef> defs = new ArrayList<>(16);
        for (ChishiOreEnvironment env : ChishiOreEnvironment.values()) {
            for (ChishiOreTier tier : ChishiOreTier.values()) {
                defs.add(new ChishiOreDef(tier, env));
            }
        }
        return List.copyOf(defs);
    }
}
