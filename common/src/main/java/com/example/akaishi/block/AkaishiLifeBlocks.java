package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.energy.LifeEnergyType;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

/**
 * 生命系统方块族注册表。
 * <p>
 * 从 ModBlocks 拆分出的域注册类：生命能量基础设施（管道/聚合转换器/转换架构/储存器）、
 * 生命能量提纯器、创造生命能量电池，以及生命科技机器（躯体检查/基因分析/培育/结构台/
 * 手术仓/药剂台/储藏库/转基因工厂，共 19 方块）。
 * 所有静态字段显式初始化为 null，由 {@link #register()} 在 {@link AkaishiMod#init()}
 * 阶段填充；任何消费方都须在 register() 之后访问，否则会触发 NPE。
 */
public final class AkaishiLifeBlocks {

    /** 生命能量管道（传输生命能量类型） */
    public static RegistrySupplier<Block> CHISHI_LIFE_ENERGY_PIPE = null;
    /** 生命聚合转换器（消耗赤能源聚合生命能量，单方块 / 生命转换架构外壳） */
    public static RegistrySupplier<Block> CHISHI_LIFE_AGGREGATION_CONVERTER = null;
    /** 生命转换架构（3×3×3 多方块主方块） */
    public static RegistrySupplier<Block> CHISHI_LIFE_CONVERSION_ARCHITECTURE = null;
    /** 生命能量储存器（纯生命能量存储） */
    public static RegistrySupplier<Block> CHISHI_LIFE_ENERGY_CELL = null;
    /** 创造生命能量储存原件（无限输出测试方块） */
    public static RegistrySupplier<Block> CHISHI_CREATIVE_LIFE_CELL = null;
    /** 生命能量提纯器（赤能源驱动，1000 生命能量 + 10M 赤能源 → 1 生命能量固态物） */
    public static RegistrySupplier<Block> CHISHI_LIFE_PURIFIER = null;
    /** 躯体检查仪：展示玩家躯体状态（9 槽位器官/肢体 + 排斥值） */
    public static RegistrySupplier<Block> CHISHI_BODY_SCANNER = null;
    /** 基因管理器：管理已吸收基因强化（最多 4 种来源，可卸载） */
    public static RegistrySupplier<Block> CHISHI_GENE_MANAGER = null;
    /** 生命分析台：纯度 100 样本解构为基因序列片段（有失败率） */
    public static RegistrySupplier<Block> CHISHI_GENE_ANALYZER = null;
    /** 部件培养舱：样本提纯 + 器官品质升级（双模式） */
    public static RegistrySupplier<Block> CHISHI_CULTIVATOR = null;
    /** 生命结构台：基因序列解析为指定槽位器官 */
    public static RegistrySupplier<Block> CHISHI_LIFE_STRUCT = null;
    /** 生命培育器：器官 + 同源序列 + 衰竭结晶 → 突变器官（成功率由纯度决定） */
    public static RegistrySupplier<Block> CHISHI_LIFE_BREEDER = null;
    /** 词条重铸仪：衰竭结晶 + 生命能量 → 原位替换指定第 N 条突变词条（确定性必成） */
    public static RegistrySupplier<Block> CHISHI_TRAIT_REFORGER = null;
    /** 手术仓：移植/摘除玩家躯体器官（消耗固态 + 生命能量，带进度） */
    public static RegistrySupplier<Block> CHISHI_SURGERY = null;
    /** 药剂台：样本（纯度 ≥25）+ 固态 + 生命能量 → 永久/突破药剂 */
    public static RegistrySupplier<Block> CHISHI_POTION_TABLE = null;
    /** 器官储藏库：按躯体槽位分页的器官仓库（生命能量维持活性） */
    public static RegistrySupplier<Block> CHISHI_ORGAN_VAULT = null;
    /** 药剂库：大容量药剂仓库（同 NBT 自动合并，按模板筛选） */
    public static RegistrySupplier<Block> CHISHI_POTION_CABINET = null;
    /** 样本库：大容量生命样本仓库（同 NBT 自动合并，机器联动存取） */
    public static RegistrySupplier<Block> CHISHI_SAMPLE_VAULT = null;
    /** 转基因工厂：凋零骷髅基因（纯度≥50）+ 缠怨藤 + 凋零玫瑰 + 固态物 → 凋零藤种子 */
    public static RegistrySupplier<Block> CHISHI_TRANSGENE_FACTORY = null;

    private AkaishiLifeBlocks() {
    }

    /** 注册全部生命系统方块（由 AkaishiMod.init 调用） */
    public static void register() {
        Registrar<Block> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);
        CHISHI_LIFE_ENERGY_PIPE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_energy_pipe",
                AkaishiLifeEnergyPipeBlock::new);
        CHISHI_LIFE_AGGREGATION_CONVERTER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_aggregation_converter",
                AkaishiLifeAggregationConverterBlock::new);
        CHISHI_LIFE_CONVERSION_ARCHITECTURE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_conversion_architecture",
                AkaishiLifeConversionArchitectureBlock::new);
        CHISHI_LIFE_ENERGY_CELL = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_energy_cell",
                AkaishiLifeEnergyCellBlock::new);
        // 创造模式能量源（测试用，无限输出）：生命能量版（赤能源版在 AkaishiEnergyBlocks）
        CHISHI_CREATIVE_LIFE_CELL = AkaishiBlockRegistrar.registerMachineBlock(registrar, "creative_life_energy_cell",
                () -> new CreativeEnergySourceBlock(LifeEnergyType.INSTANCE));
        CHISHI_LIFE_PURIFIER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_purifier",
                AkaishiLifePurifierBlock::new);
        // ===== 生命科技 =====
        CHISHI_BODY_SCANNER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_body_scanner", AkaishiBodyScannerBlock::new);
        // 基因管理器（生命科技：已吸收基因强化管理）
        CHISHI_GENE_MANAGER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_gene_manager", AkaishiGeneManagerBlock::new);
        CHISHI_GENE_ANALYZER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_gene_analyzer", AkaishiGeneAnalyzerBlock::new);
        CHISHI_CULTIVATOR = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_cultivator", AkaishiCultivatorBlock::new);
        CHISHI_LIFE_STRUCT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_struct", AkaishiLifeStructBlock::new);
        CHISHI_LIFE_BREEDER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_breeder", AkaishiLifeBreederBlock::new);
        CHISHI_TRAIT_REFORGER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_trait_reforger", AkaishiTraitReforgerBlock::new);
        CHISHI_SURGERY = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_surgery", AkaishiSurgeryBlock::new);
        CHISHI_POTION_TABLE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_potion_table", AkaishiPotionTableBlock::new);
        CHISHI_ORGAN_VAULT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_organ_vault", AkaishiOrganVaultBlock::new);
        CHISHI_POTION_CABINET = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_potion_cabinet", AkaishiPotionCabinetBlock::new);
        CHISHI_SAMPLE_VAULT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_sample_vault", AkaishiSampleVaultBlock::new);
        CHISHI_TRANSGENE_FACTORY = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_transgene_factory", AkaishiTransgeneFactoryBlock::new);
    }
}
