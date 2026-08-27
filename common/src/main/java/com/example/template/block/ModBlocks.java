package com.example.template.block;

import com.example.template.TemplateMod;
import com.example.template.energy.ChishiEnergyType;
import com.example.template.energy.EnergyCellTier;
import com.example.template.energy.EnergyPipeTier;
import com.example.template.energy.LifeEnergyType;
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
    /** 赤石提纯器（消耗赤能源提纯粗制块） */
    public static RegistrySupplier<Block> CHISHI_PURIFIER;
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
