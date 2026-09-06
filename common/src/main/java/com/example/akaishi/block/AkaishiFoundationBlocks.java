package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
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
 * 赤石基础域方块注册：16 种矿石 + 粗制块/提纯器/精华块等基础材料块，以及
 * 装备打造器/升级台/融合砧/压缩机/打粉机/变化器/活化处理链/燃料链等通用加工机器。
 * <p>
 * 从 ModBlocks 拆分出的域注册类（矿石走「方块 + BlockItem」循环，机器经
 * {@link AkaishiBlockRegistrar#registerMachineBlock} 统一注册）。
 * 所有静态字段显式初始化为 null，由 {@link #register()} 在 {@link AkaishiMod#init()}
 * 阶段填充；任何消费方都须在 register() 之后访问，否则会触发 NPE。
 */
public final class AkaishiFoundationBlocks {

    /** 全部 16 个矿石组合定义 */
    public static final List<AkaishiOreDef> ALL_ORES = buildAllOres();

    /** 组合定义 → 方块延迟注册引用 */
    private static final Map<AkaishiOreDef, RegistrySupplier<Block>> BLOCK_BY_DEF = new ConcurrentHashMap<>();

    /** 粗制赤石块（9 赤石晶合成，提纯器原料） */
    public static RegistrySupplier<Block> RAW_CHISHI_BLOCK = null;
    /** 赤石提纯器（消耗赤能源提纯粗制块/水晶块，可作提纯矩阵中心） */
    public static RegistrySupplier<Block> CHISHI_PURIFIER = null;
    /** 高级提纯构建方块（单方块直接消耗赤能源提纯，提纯矩阵 3×3×3 外壳） */
    public static RegistrySupplier<Block> CHISHI_ADVANCED_PURIFIER = null;
    /** 浓缩赤石精华块（9 个浓缩精华压缩，装饰与储备用） */
    public static RegistrySupplier<Block> CHISHI_ESSENCE_BLOCK = null;
    /** 赤石装备打造器（赤能源 + 赤石锭 → 赤石装备） */
    public static RegistrySupplier<Block> CHISHI_EQUIPMENT_FORGER = null;
    /** 赤红升级台（模板 + 槽位 → 升级赤石装备） */
    public static RegistrySupplier<Block> CHISHI_UPGRADE_STATION = null;
    /** 生命的融合砧（赤石护甲 + 生命的融合锭 → 生命融合护甲，保留升级数据） */
    public static RegistrySupplier<Block> CHISHI_LIFE_FUSION_ANVIL = null;
    /** 燃料装罐机：液体燃料灌装进 10L 燃料罐 */
    public static RegistrySupplier<Block> CHISHI_FUEL_CANNER = null;
    /** 燃料混合器：两种燃料液体 1:1:1 调和为高阶混合燃料 */
    public static RegistrySupplier<Block> CHISHI_FUEL_MIXER = null;
    /** 生命活化器：消耗生命能量缓慢无害化衰竭燃料（废料进、活化液出） */
    public static RegistrySupplier<Block> CHISHI_LIFE_ACTIVATOR = null;
    /** 生命离心机：分离活化燃料为活化结晶 + 衰竭结晶 */
    public static RegistrySupplier<Block> CHISHI_LIFE_CENTRIFUGE = null;
    /** 物品重构仪：以衰竭结晶为代价嬗变物品 */
    public static RegistrySupplier<Block> CHISHI_ITEM_RECONSTRUCTOR = null;
    /** 赤石植物培养机：消耗赤能源培养植物（种子保留） */
    public static RegistrySupplier<Block> CHISHI_PLANT_CULTIVATOR = null;
    /** 赤石压缩机：粉末 → 块、赤石精华 → 浓缩赤石精华 */
    public static RegistrySupplier<Block> CHISHI_COMPRESSOR = null;
    /** 赤石打粉机：矿物/赤石/黑曜石 → 粉末 */
    public static RegistrySupplier<Block> CHISHI_PULVERIZER = null;
    /** 赤石变化器：青金石粉 → 冷却基底、矿物 → 矿石基底 */
    public static RegistrySupplier<Block> CHISHI_TRANSFORMER = null;
    /** 活化分馏器（活化结晶深度拆分：活化成分 + 衰竭结晶） */
    public static RegistrySupplier<Block> CHISHI_ACTIVATED_FRACTIONATOR = null;
    /** 离子体填装器（等离子体 + 反应棒 → 燃料棒） */
    public static RegistrySupplier<Block> CHISHI_PLASMA_FILLER = null;

    private AkaishiFoundationBlocks() {
    }

    /** 注册全部基础方块（由 ModBlocks 门面在 AkaishiMod.init 阶段统一调用） */
    public static void register() {
        Registrar<Block> blockRegistrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);

        // 16 种矿石：方块实例必须延迟到注册事件中创建（new Block 会创建侵入式 Holder，
        // 若在注册表冻结后执行将抛 "Registry is already frozen"）
        for (AkaishiOreDef def : ALL_ORES) {
            ResourceLocation id = new ResourceLocation(AkaishiMod.MOD_ID, def.id());
            RegistrySupplier<Block> block = blockRegistrar.register(id, AkaishiOreBlock::new);
            // 方块物品一并注册，方便玩家在创造模式取用
            RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                    .register(id, () -> new BlockItem(block.get(), new Item.Properties()));
            BLOCK_BY_DEF.put(def, block);
        }

        // 粗制赤石块（普通方块 + BlockItem）
        RAW_CHISHI_BLOCK = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "raw_akaishi_block", AkaishiBlock::new);
        // 赤石提纯器（可作提纯矩阵中心）
        CHISHI_PURIFIER = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "akaishi_purifier", AkaishiPurifierBlock::new);
        // 高级提纯构建方块：单方块直接提纯，作为"提纯矩阵"（3×3×3）外壳
        CHISHI_ADVANCED_PURIFIER = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "akaishi_advanced_purifier", AkaishiAdvancedPurifierBlock::new);
        // 浓缩赤石精华块（普通方块 + BlockItem）
        CHISHI_ESSENCE_BLOCK = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "akaishi_essence_block",
                () -> new Block(Block.Properties.of().strength(3.0F, 6.0F).requiresCorrectToolForDrops()));
        // 赤石装备打造器（赤能源 + 赤石锭 → 赤石装备）
        CHISHI_EQUIPMENT_FORGER = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "akaishi_equipment_forger", AkaishiEquipmentForgerBlock::new);
        // 赤红升级台（模板 + 槽位 + 赤能源 → 升级赤石装备）
        CHISHI_UPGRADE_STATION = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "akaishi_upgrade_station", AkaishiUpgradeStationBlock::new);
        // 生命的融合砧
        CHISHI_LIFE_FUSION_ANVIL = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "akaishi_life_fusion_anvil", AkaishiLifeFusionAnvilBlock::new);
        // 燃料装罐机（液体燃料 → 10L 燃料罐）
        CHISHI_FUEL_CANNER = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "akaishi_fuel_canner", AkaishiFuelCannerBlock::new);
        // 燃料混合器（燃料液体 1:1:1 调和 → 高级/终极混合燃料）
        CHISHI_FUEL_MIXER = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "akaishi_fuel_mixer", AkaishiFuelMixerBlock::new);
        // 生命活化器（生命能量无害化衰竭燃料：废料管道进、普通管道抽活化液）
        CHISHI_LIFE_ACTIVATOR = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "akaishi_life_activator", AkaishiLifeActivatorBlock::new);
        // 生命离心机（赤能源分离活化燃料：活化结晶 + 衰竭结晶）
        CHISHI_LIFE_CENTRIFUGE = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "akaishi_life_centrifuge", AkaishiLifeCentrifugeBlock::new);
        // 物品重构仪（以衰竭结晶为代价嬗变物品）
        CHISHI_ITEM_RECONSTRUCTOR = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "akaishi_item_reconstructor", AkaishiItemReconstructorBlock::new);
        // 赤石植物培养机（消耗赤能源培养植物，种子保留不消耗）
        CHISHI_PLANT_CULTIVATOR = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "akaishi_plant_cultivator", AkaishiPlantCultivatorBlock::new);
        // 赤石压缩机（粉末 → 块、赤石精华 → 浓缩赤石精华）
        CHISHI_COMPRESSOR = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "akaishi_compressor", AkaishiCompressorBlock::new);
        // 赤石打粉机（矿物/赤石/黑曜石 → 粉末）
        CHISHI_PULVERIZER = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "akaishi_pulverizer", AkaishiPulverizerBlock::new);
        // 赤石变化器（青金石粉 → 冷却基底、矿物 → 矿石基底）
        CHISHI_TRANSFORMER = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "akaishi_transformer", AkaishiTransformerBlock::new);
        // 活化分馏器（活化结晶深度拆分：活化成分 + 衰竭结晶）
        CHISHI_ACTIVATED_FRACTIONATOR = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "akaishi_activated_fractionator", AkaishiActivatedFractionatorBlock::new);
        // 离子体填装器（等离子体 + 反应棒 → 燃料棒）
        CHISHI_PLASMA_FILLER = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "akaishi_plasma_filler", AkaishiPlasmaFillerBlock::new);
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
