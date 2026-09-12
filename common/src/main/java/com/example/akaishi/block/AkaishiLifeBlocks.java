package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.energy.LifeEnergyCellTier;
import com.example.akaishi.energy.LifeEnergyPipeTier;
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

    /** 生命能量管道（基础，传输生命能量类型，1000/tick） */
    public static RegistrySupplier<Block> CHISHI_LIFE_ENERGY_PIPE = null;
    /** 生命能量管道（中级，4000/tick） */
    public static RegistrySupplier<Block> CHISHI_LIFE_ENERGY_PIPE_ADVANCED = null;
    /** 生命能量管道（高级，16000/tick） */
    public static RegistrySupplier<Block> CHISHI_LIFE_ENERGY_PIPE_ELITE = null;
    /** 生命能量管道（超级，64000/tick） */
    public static RegistrySupplier<Block> CHISHI_LIFE_ENERGY_PIPE_ULTIMATE = null;
    /** 生命聚合转换器（消耗赤能源聚合生命能量，单方块 / 生命转换架构外壳） */
    public static RegistrySupplier<Block> CHISHI_LIFE_AGGREGATION_CONVERTER = null;
    /** 生命转换架构（3×3×3 多方块主方块） */
    public static RegistrySupplier<Block> CHISHI_LIFE_CONVERSION_ARCHITECTURE = null;
    /** 生命能量储存器·基础（纯生命能量存储，保留旧 id） */
    public static RegistrySupplier<Block> CHISHI_LIFE_ENERGY_CELL = null;
    /** 生命能量储存器·高级 */
    public static RegistrySupplier<Block> CHISHI_LIFE_ENERGY_CELL_ADVANCED = null;
    /** 生命能量储存器·超级 */
    public static RegistrySupplier<Block> CHISHI_LIFE_ENERGY_CELL_SUPER = null;
    /** 生命储存串联器（3×3×3 多方块主方块，聚合 26 台储存器容量） */
    public static RegistrySupplier<Block> CHISHI_LIFE_ENERGY_CELL_SERIALIZER = null;
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

    // ===== 生命无线终端方块族（镜像赤能源无线族；能量走生命体系黄/白/金配色） =====
    /** 生命无线终端外壳：生命无线终端多方块（5×5×5）墙面填充方块（纯结构判定，无方块实体） */
    public static RegistrySupplier<Block> CHISHI_LIFE_WIRELESS_SHELL = null;
    /** 生命无线终端结构玻璃：半透明观察窗，可替代生命无线终端外壳 */
    public static RegistrySupplier<Block> CHISHI_LIFE_WIRELESS_STRUCTURE_GLASS = null;
    /** 生命无线终端方块：外墙主方块（成型后为生命无线网络的能量中枢与 GUI 入口） */
    public static RegistrySupplier<Block> CHISHI_LIFE_WIRELESS_TERMINAL = null;
    /** 生命无线终端核心：内腔中心方块（恰 1 个），拆掉结构即失效 */
    public static RegistrySupplier<Block> CHISHI_LIFE_WIRELESS_CORE = null;
    /** 生命无线终端控制器：外墙纯结构件（无 GUI 无 BE） */
    public static RegistrySupplier<Block> CHISHI_LIFE_WIRELESS_CONTROLLER = null;
    /** 生命无线输入口：生命能量管道 → 生命无线网络的发送端（仅接收，不可抽取） */
    public static RegistrySupplier<Block> CHISHI_LIFE_WIRELESS_INPUT_PORT = null;
    /** 生命无线输出口：生命无线网络 → 生命能量管道的接收端（纯发电，仅可抽取） */
    public static RegistrySupplier<Block> CHISHI_LIFE_WIRELESS_OUTPUT_PORT = null;
    /** 生命无线跨维组件：内腔至少一个时解锁跨维传输 */
    public static RegistrySupplier<Block> CHISHI_LIFE_WIRELESS_DIM_BRIDGE = null;
    /** 生命无线区块加载构架：维持终端与已认证端口所在区块 */
    public static RegistrySupplier<Block> CHISHI_LIFE_WIRELESS_CHUNK_LOADER = null;
    /** 生命无线区块加载范围组件：将加载范围扩展为 3×3 区块 */
    public static RegistrySupplier<Block> CHISHI_LIFE_WIRELESS_CHUNK_RANGE = null;
    /** 生命无线输入损耗抑制组件 */
    public static RegistrySupplier<Block> CHISHI_LIFE_WIRELESS_INPUT_LOSS = null;
    /** 生命无线输出损耗抑制组件 */
    public static RegistrySupplier<Block> CHISHI_LIFE_WIRELESS_OUTPUT_LOSS = null;

    private AkaishiLifeBlocks() {
    }

    /** 注册全部生命系统方块（由 AkaishiMod.init 调用） */
    public static void register() {
        Registrar<Block> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);
        // 生命能量管道（基础/中级/高级/超级），速率对齐物品管道分级
        CHISHI_LIFE_ENERGY_PIPE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_energy_pipe",
                () -> new AkaishiLifeEnergyPipeBlock(LifeEnergyPipeTier.BASIC));
        CHISHI_LIFE_ENERGY_PIPE_ADVANCED = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_energy_pipe_advanced",
                () -> new AkaishiLifeEnergyPipeBlock(LifeEnergyPipeTier.ADVANCED));
        CHISHI_LIFE_ENERGY_PIPE_ELITE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_energy_pipe_elite",
                () -> new AkaishiLifeEnergyPipeBlock(LifeEnergyPipeTier.ELITE));
        CHISHI_LIFE_ENERGY_PIPE_ULTIMATE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_energy_pipe_ultimate",
                () -> new AkaishiLifeEnergyPipeBlock(LifeEnergyPipeTier.ULTIMATE));
        CHISHI_LIFE_AGGREGATION_CONVERTER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_aggregation_converter",
                AkaishiLifeAggregationConverterBlock::new);
        CHISHI_LIFE_CONVERSION_ARCHITECTURE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_conversion_architecture",
                AkaishiLifeConversionArchitectureBlock::new);
        // 生命能量储存器：基础（保留旧 id）/ 高级 / 超级，三档镜像赤能源储存单元分级
        CHISHI_LIFE_ENERGY_CELL = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_energy_cell",
                () -> new AkaishiLifeEnergyCellBlock(LifeEnergyCellTier.BASIC));
        CHISHI_LIFE_ENERGY_CELL_ADVANCED = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_energy_cell_advanced",
                () -> new AkaishiLifeEnergyCellBlock(LifeEnergyCellTier.ADVANCED));
        CHISHI_LIFE_ENERGY_CELL_SUPER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_energy_cell_super",
                () -> new AkaishiLifeEnergyCellBlock(LifeEnergyCellTier.SUPER));
        // 生命储存串联器（3×3×3 多方块主方块）
        CHISHI_LIFE_ENERGY_CELL_SERIALIZER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_energy_cell_serializer",
                AkaishiLifeEnergyCellSerializerBlock::new);
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
        // ===== 生命无线终端方块族（5×5×5 多方块，镜像赤能源无线族；结构玻璃复用 AkaishiStructureGlassBlock） =====
        CHISHI_LIFE_WIRELESS_SHELL = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_wireless_shell",
                AkaishiLifeWirelessShellBlock::new);
        CHISHI_LIFE_WIRELESS_STRUCTURE_GLASS = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_wireless_structure_glass",
                AkaishiStructureGlassBlock::new);
        CHISHI_LIFE_WIRELESS_TERMINAL = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_wireless_terminal",
                AkaishiLifeWirelessTerminalBlock::new);
        CHISHI_LIFE_WIRELESS_CORE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_wireless_core",
                AkaishiLifeWirelessCoreBlock::new);
        CHISHI_LIFE_WIRELESS_CONTROLLER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_wireless_controller",
                AkaishiLifeWirelessControllerBlock::new);
        CHISHI_LIFE_WIRELESS_INPUT_PORT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_wireless_input_port",
                AkaishiLifeWirelessInputPortBlock::new);
        CHISHI_LIFE_WIRELESS_OUTPUT_PORT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_wireless_output_port",
                AkaishiLifeWirelessOutputPortBlock::new);
        CHISHI_LIFE_WIRELESS_DIM_BRIDGE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_wireless_dim_bridge",
                AkaishiLifeWirelessDimBridgeBlock::new);
        CHISHI_LIFE_WIRELESS_CHUNK_LOADER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_wireless_chunk_loader",
                AkaishiLifeWirelessChunkLoaderBlock::new);
        CHISHI_LIFE_WIRELESS_CHUNK_RANGE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_wireless_chunk_range",
                AkaishiLifeWirelessChunkRangeBlock::new);
        CHISHI_LIFE_WIRELESS_INPUT_LOSS = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_wireless_input_loss",
                AkaishiLifeWirelessInputLossBlock::new);
        CHISHI_LIFE_WIRELESS_OUTPUT_LOSS = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_wireless_output_loss",
                AkaishiLifeWirelessOutputLossBlock::new);
    }
}
