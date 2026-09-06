package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.fluid.FluidTankTier;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

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
    public static final List<AkaishiOreDef> ALL_ORES = buildAllOres();

    /** 组合定义 → 方块延迟注册引用 */
    private static final Map<AkaishiOreDef, RegistrySupplier<Block>> BLOCK_BY_DEF = new ConcurrentHashMap<>();

    /** 粗制赤石块（9 赤石晶合成，提纯器原料） */
    public static RegistrySupplier<Block> RAW_CHISHI_BLOCK;
    /** 赤石提纯器（消耗赤能源提纯粗制块/水晶块，可作提纯矩阵中心） */
    public static RegistrySupplier<Block> CHISHI_PURIFIER;
    /** 高级提纯构建方块（单方块直接消耗赤能源提纯，提纯矩阵 3×3×3 外壳） */
    public static RegistrySupplier<Block> CHISHI_ADVANCED_PURIFIER;
    /** 浓缩赤石精华块（9 个浓缩精华压缩，装饰与储备用） */
    public static RegistrySupplier<Block> CHISHI_ESSENCE_BLOCK;
    /** 赤石装备打造器（赤能源 + 赤石锭 → 赤石装备） */
    public static RegistrySupplier<Block> CHISHI_EQUIPMENT_FORGER;
    /** 赤红升级台（模板 + 槽位 → 升级赤石装备） */
    public static RegistrySupplier<Block> CHISHI_UPGRADE_STATION;
    /** 生命的融合砧（赤石护甲 + 生命的融合锭 → 生命融合护甲） */
    public static RegistrySupplier<Block> CHISHI_LIFE_FUSION_ANVIL;
    /** 物品管道（基础）：物流网络中继，1 个/tick */
    public static RegistrySupplier<Block> CHISHI_ITEM_PIPE;
    /** 物品管道（高级） */
    public static RegistrySupplier<Block> CHISHI_ITEM_PIPE_ADVANCED;
    /** 物品管道（精英） */
    public static RegistrySupplier<Block> CHISHI_ITEM_PIPE_ELITE;
    /** 物品管道（终极）：64 个/tick */
    public static RegistrySupplier<Block> CHISHI_ITEM_PIPE_ULTIMATE;
    /** 液体管道：传输下界能量/燃料液体，可对接 MEK 等外部液体方块 */
    public static RegistrySupplier<Block> CHISHI_FLUID_PIPE;
    /** 封闭性衰竭管道：废料专用（单缓冲） */
    public static RegistrySupplier<Block> CHISHI_EXHAUSTED_PIPE;
    /** 多流体废料管道：废料专用（多缓冲） */
    public static RegistrySupplier<Block> CHISHI_MULTI_FLUID_WASTE_PIPE;
    /** 燃料装罐机：液体燃料灌装进 10L 燃料罐 */
    public static RegistrySupplier<Block> CHISHI_FUEL_CANNER;
    /** 燃料混合器：两种燃料液体 1:1:1 调和为高阶混合燃料 */
    public static RegistrySupplier<Block> CHISHI_FUEL_MIXER;
    /** 生命活化器：消耗生命能量缓慢无害化衰竭燃料（废料进、活化液出） */
    public static RegistrySupplier<Block> CHISHI_LIFE_ACTIVATOR;
    /** 生命离心机：分离活化燃料为活化结晶 + 衰竭结晶 */
    public static RegistrySupplier<Block> CHISHI_LIFE_CENTRIFUGE;
    /** 物品重构仪：以衰竭结晶为代价嬗变物品 */
    public static RegistrySupplier<Block> CHISHI_ITEM_RECONSTRUCTOR;
    /** 赤石植物培养机：消耗赤能源培养植物（种子保留） */
    public static RegistrySupplier<Block> CHISHI_PLANT_CULTIVATOR;
    /** 赤石压缩机：粉末 → 块、赤石精华 → 浓缩赤石精华 */
    public static RegistrySupplier<Block> CHISHI_COMPRESSOR;
    /** 赤石打粉机：矿物/赤石/黑曜石 → 粉末 */
    public static RegistrySupplier<Block> CHISHI_PULVERIZER;
    /** 赤石变化器：青金石粉 → 冷却基底、矿物 → 矿石基底 */
    public static RegistrySupplier<Block> CHISHI_TRANSFORMER;
    /** 活化分馏器（活化结晶深度拆分：活化成分 + 衰竭结晶） */
    public static RegistrySupplier<Block> CHISHI_ACTIVATED_FRACTIONATOR;
    /** 离子体填装器（等离子体 + 反应棒 → 燃料棒） */
    public static RegistrySupplier<Block> CHISHI_PLASMA_FILLER;
    /** 等离子体管道（第三传输家族，仅传等离子体） */
    public static RegistrySupplier<Block> CHISHI_PLASMA_PIPE;
    /** 液体储罐（基础/高级/超级：16k/64k/256k mb，管道存取液体） */
    public static RegistrySupplier<Block> CHISHI_FLUID_TANK_BASIC;
    public static RegistrySupplier<Block> CHISHI_FLUID_TANK_ADVANCED;
    public static RegistrySupplier<Block> CHISHI_FLUID_TANK_SUPER;
    /** 等离子体燃料储罐（仅存储等离子体，仅等离子体管道可对接） */
    public static RegistrySupplier<Block> CHISHI_PLASMA_TANK;
    /** 凋零藤根：整株第 1 格（种子种下/挖掘产出种子），随机刻长出茎 */
    public static RegistrySupplier<Block> CHISHI_WITHER_ROOT;
    /** 凋零藤茎：整株第 2/3 格（无物品、只能由根长出），成熟顶端可收凝聚体 */
    public static RegistrySupplier<Block> CHISHI_WITHER_STEM;
    /** 衰变净化塔：消耗赤能源净化范围内衰竭区域（加速区域消散） */
    public static RegistrySupplier<Block> CHISHI_DECAY_PURIFIER;
    /** 衰竭土壤：衰竭区域内泥土腐化后的终态（区域污染产物/原料） */
    public static RegistrySupplier<Block> CHISHI_DECAY_SOIL;
    /** 衰竭木：衰竭区域内原木污染后的终态柱状方块（区域污染产物/原料） */
    public static RegistrySupplier<Block> CHISHI_DECAY_LOG;

    private ModBlocks() {
    }

    public static void register() {
        for (AkaishiOreDef def : ALL_ORES) {
            ResourceLocation id = new ResourceLocation(AkaishiMod.MOD_ID, def.id());

            // 方块实例必须延迟到注册事件中创建：new Block 会创建侵入式 Holder，
            // 若在注册表冻结后执行将抛 "Registry is already frozen"
            Registrar<Block> blockRegistrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);
            RegistrySupplier<Block> block = blockRegistrar.register(id, AkaishiOreBlock::new);
            // 方块物品一并注册，方便玩家在创造模式取用
            RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                    .register(id, () -> new BlockItem(block.get(), new Item.Properties()));

            BLOCK_BY_DEF.put(def, block);
        }

        // 粗制赤石块 + 赤石提纯器（含各自 BlockItem）
        Registrar<Block> blockRegistrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);
        RAW_CHISHI_BLOCK = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "raw_akaishi_block"), AkaishiBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "raw_akaishi_block"),
                        () -> new BlockItem(RAW_CHISHI_BLOCK.get(), new Item.Properties()));

        CHISHI_PURIFIER = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_purifier"), AkaishiPurifierBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_purifier"),
                        () -> new BlockItem(CHISHI_PURIFIER.get(), new Item.Properties()));

        // 高级提纯构建方块：单方块直接消耗赤能源提纯，作为"提纯矩阵"（3×3×3）外壳
        CHISHI_ADVANCED_PURIFIER = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_advanced_purifier"), AkaishiAdvancedPurifierBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_advanced_purifier"),
                        () -> new BlockItem(CHISHI_ADVANCED_PURIFIER.get(), new Item.Properties()));

        // 浓缩赤石精华块（普通方块 + BlockItem）
        CHISHI_ESSENCE_BLOCK = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_essence_block"),
                () -> new Block(Block.Properties.of().strength(3.0F, 6.0F).requiresCorrectToolForDrops()));
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_essence_block"),
                        () -> new BlockItem(CHISHI_ESSENCE_BLOCK.get(), new Item.Properties()));

        // 赤石装备打造器（赤能源 + 赤石锭 → 赤石装备）
        CHISHI_EQUIPMENT_FORGER = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_equipment_forger"),
                AkaishiEquipmentForgerBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_equipment_forger"),
                        () -> new BlockItem(CHISHI_EQUIPMENT_FORGER.get(), new Item.Properties()));

        // 赤红升级台（模板 + 槽位 + 赤能源 → 升级赤石装备）
        CHISHI_UPGRADE_STATION = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_upgrade_station"),
                AkaishiUpgradeStationBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_upgrade_station"),
                        () -> new BlockItem(CHISHI_UPGRADE_STATION.get(), new Item.Properties()));

        // 生命的融合砧（赤石护甲 + 生命的融合锭 → 生命融合护甲，保留升级数据）
        CHISHI_LIFE_FUSION_ANVIL = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_fusion_anvil"),
                AkaishiLifeFusionAnvilBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_fusion_anvil"),
                        () -> new BlockItem(CHISHI_LIFE_FUSION_ANVIL.get(), new Item.Properties()));

        // 物品管道（4 级）：物流网络中继，传输物品到相连容器/机器，终极 64 个/tick
        CHISHI_ITEM_PIPE = registerItemPipe(blockRegistrar, "akaishi_item_pipe", AkaishiItemPipeBlock.ItemPipeTier.BASIC);
        CHISHI_ITEM_PIPE_ADVANCED = registerItemPipe(blockRegistrar, "akaishi_item_pipe_advanced", AkaishiItemPipeBlock.ItemPipeTier.ADVANCED);
        CHISHI_ITEM_PIPE_ELITE = registerItemPipe(blockRegistrar, "akaishi_item_pipe_elite", AkaishiItemPipeBlock.ItemPipeTier.ELITE);
        CHISHI_ITEM_PIPE_ULTIMATE = registerItemPipe(blockRegistrar, "akaishi_item_pipe_ultimate", AkaishiItemPipeBlock.ItemPipeTier.ULTIMATE);

        // 液体管道（单级，传输下界能量/燃料液体）
        CHISHI_FLUID_PIPE = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fluid_pipe"),
                AkaishiFluidPipeBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fluid_pipe"),
                        () -> new BlockItem(CHISHI_FLUID_PIPE.get(), new Item.Properties()));

        // 封闭性衰竭管道（废料专用，单缓冲；与普通液体管道网络隔离）
        CHISHI_EXHAUSTED_PIPE = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_exhausted_pipe"),
                AkaishiExhaustedPipeBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_exhausted_pipe"),
                        () -> new BlockItem(CHISHI_EXHAUSTED_PIPE.get(), new Item.Properties()));

        // 多流体废料管道（废料专用，多缓冲，多种废料可混输）
        CHISHI_MULTI_FLUID_WASTE_PIPE = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_multi_fluid_waste_pipe"),
                AkaishiMultiFluidWastePipeBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_multi_fluid_waste_pipe"),
                        () -> new BlockItem(CHISHI_MULTI_FLUID_WASTE_PIPE.get(), new Item.Properties()));

        // 燃料装罐机（液体燃料 → 10L 燃料罐）
        CHISHI_FUEL_CANNER = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fuel_canner"),
                AkaishiFuelCannerBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fuel_canner"),
                        () -> new BlockItem(CHISHI_FUEL_CANNER.get(), new Item.Properties()));

        // 燃料混合器（燃料液体 1:1:1 调和 → 高级/终极混合燃料）
        CHISHI_FUEL_MIXER = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fuel_mixer"),
                AkaishiFuelMixerBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fuel_mixer"),
                        () -> new BlockItem(CHISHI_FUEL_MIXER.get(), new Item.Properties()));

        // 生命活化器（生命能量无害化衰竭燃料：废料管道进、普通管道抽活化液）
        CHISHI_LIFE_ACTIVATOR = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_activator"),
                AkaishiLifeActivatorBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_activator"),
                        () -> new BlockItem(CHISHI_LIFE_ACTIVATOR.get(), new Item.Properties()));

        // 生命离心机（赤能源分离活化燃料：活化结晶 + 衰竭结晶）
        CHISHI_LIFE_CENTRIFUGE = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_centrifuge"),
                AkaishiLifeCentrifugeBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_centrifuge"),
                        () -> new BlockItem(CHISHI_LIFE_CENTRIFUGE.get(), new Item.Properties()));

        // 物品重构仪（以衰竭结晶为代价嬗变物品）
        CHISHI_ITEM_RECONSTRUCTOR = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_item_reconstructor"),
                AkaishiItemReconstructorBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_item_reconstructor"),
                        () -> new BlockItem(CHISHI_ITEM_RECONSTRUCTOR.get(), new Item.Properties()));

        // 赤石植物培养机（消耗赤能源培养植物，种子保留不消耗）
        CHISHI_PLANT_CULTIVATOR = registerReactorBlock(blockRegistrar, "akaishi_plant_cultivator", AkaishiPlantCultivatorBlock::new);
        // 赤石压缩机（粉末 → 块、赤石精华 → 浓缩赤石精华）
        CHISHI_COMPRESSOR = registerReactorBlock(blockRegistrar, "akaishi_compressor", AkaishiCompressorBlock::new);
        // 赤石打粉机（矿物/赤石/黑曜石 → 粉末）
        CHISHI_PULVERIZER = registerReactorBlock(blockRegistrar, "akaishi_pulverizer", AkaishiPulverizerBlock::new);
        // 赤石变化器（青金石粉 → 冷却基底、矿物 → 矿石基底）
        CHISHI_TRANSFORMER = registerReactorBlock(blockRegistrar, "akaishi_transformer", AkaishiTransformerBlock::new);

        // 活化分馏器（活化结晶深度拆分：活化成分 + 衰竭结晶）
        CHISHI_ACTIVATED_FRACTIONATOR = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_activated_fractionator"),
                AkaishiActivatedFractionatorBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_activated_fractionator"),
                        () -> new BlockItem(CHISHI_ACTIVATED_FRACTIONATOR.get(), new Item.Properties()));

        // 离子体填装器（等离子体 + 反应棒 → 燃料棒）
        CHISHI_PLASMA_FILLER = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_plasma_filler"),
                AkaishiPlasmaFillerBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_plasma_filler"),
                        () -> new BlockItem(CHISHI_PLASMA_FILLER.get(), new Item.Properties()));

        // 等离子体管道（第三传输家族，仅传等离子体）
        CHISHI_PLASMA_PIPE = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_plasma_pipe"),
                AkaishiPlasmaPipeBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_plasma_pipe"),
                        () -> new BlockItem(CHISHI_PLASMA_PIPE.get(), new Item.Properties()));

        // 液体储罐（基础/高级/超级，容量递增，可被液体管道注入/抽取）
        CHISHI_FLUID_TANK_BASIC = registerFluidTank(blockRegistrar, "akaishi_fluid_tank_basic", FluidTankTier.BASIC);
        CHISHI_FLUID_TANK_ADVANCED = registerFluidTank(blockRegistrar, "akaishi_fluid_tank_advanced", FluidTankTier.ADVANCED);
        CHISHI_FLUID_TANK_SUPER = registerFluidTank(blockRegistrar, "akaishi_fluid_tank_super", FluidTankTier.SUPER);

        // 等离子体燃料储罐：仅存储等离子体（罐层拒收非等离子体液体）
        CHISHI_PLASMA_TANK = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_plasma_tank"),
                AkaishiPlasmaTankBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_plasma_tank"),
                        () -> new BlockItem(CHISHI_PLASMA_TANK.get(), new Item.Properties()));

        CHISHI_DECAY_PURIFIER = registerReactorBlock(blockRegistrar, "akaishi_decay_purifier", AkaishiDecayPurifierBlock::new);
        // 凋零藤根/茎：纯植物方块（无物品），种子种植生成根、根随机刻长茎
        CHISHI_WITHER_ROOT = blockRegistrar.register(
                new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_wither_root"), AkaishiWitherRootBlock::new);
        CHISHI_WITHER_STEM = blockRegistrar.register(
                new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_wither_stem"), AkaishiWitherStemBlock::new);

        // 衰竭区域污染产物：衰竭土壤（泥土终态）+ 衰竭木（原木终态），均可被玩家采集作装饰/原料
        CHISHI_DECAY_SOIL = registerReactorBlock(blockRegistrar, "akaishi_decay_soil", AkaishiDecaySoilBlock::new);
        CHISHI_DECAY_LOG = registerReactorBlock(blockRegistrar, "akaishi_decay_log", AkaishiDecayLogBlock::new);
    }

    /** 注册一个指定等级的液体储罐及其 BlockItem */
    private static RegistrySupplier<Block> registerFluidTank(Registrar<Block> blockRegistrar, String id, FluidTankTier tier) {
        RegistrySupplier<Block> block = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                () -> new AkaishiFluidTankBlock(tier));
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                        () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** 注册一个指定等级的物品管道及其 BlockItem */
    private static RegistrySupplier<Block> registerItemPipe(Registrar<Block> blockRegistrar, String id,
                                                            AkaishiItemPipeBlock.ItemPipeTier tier) {
        RegistrySupplier<Block> block = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                () -> new AkaishiItemPipeBlock(tier));
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                        () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** 注册一个反应堆方块及其 BlockItem（无参数构造） */
    private static RegistrySupplier<Block> registerReactorBlock(Registrar<Block> blockRegistrar, String id,
                                                                java.util.function.Supplier<Block> factory) {
        RegistrySupplier<Block> block = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, id), factory);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                        () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** 获取对应组合定义的方块（注册完成后可用） */
    public static Block get(AkaishiOreDef def) {
        return BLOCK_BY_DEF.get(def).get();
    }

    /** 生成 4 × 4 全部组合 */
    private static List<AkaishiOreDef> buildAllOres() {
        List<AkaishiOreDef> defs = new ArrayList<>(16);
        for (AkaishiOreEnvironment env : AkaishiOreEnvironment.values()) {
            for (AkaishiOreTier tier : AkaishiOreTier.values()) {
                defs.add(new AkaishiOreDef(tier, env));
            }
        }
        return List.copyOf(defs);
    }
}
