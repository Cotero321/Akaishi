package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

/**
 * 聚变堆方块族注册表。
 * <p>
 * 从 ModBlocks 拆分出的域注册类：11 个聚变多方块方块（外壳/玻璃/隔热层/控制器/
 * 能量输出口/物品输入输出口/核心/散热框架/燃料框架/效率框架）+ 聚变燃料聚合器。
 * 字段由 {@link #register()} 在 {@link AkaishiMod#init()} 阶段填充，消费方须在
 * register() 之后访问。
 */
public final class AkaishiFusionBlocks {

    /** 聚变燃料聚合器（活化成分 → 等离子体） */
    public static RegistrySupplier<Block> CHISHI_FUSION_FUEL_AGGREGATOR = null;
    /** 耐高温聚变外壳：聚变堆多方块外壁（控制器/能量输出口/物品输入口/物品输出口也属外壁） */
    public static RegistrySupplier<Block> CHISHI_FUSION_SHELL = null;
    /** 聚变结构玻璃：半透明观察窗，可替代耐高温聚变外壳（仅外壳层，隔热层不可替代） */
    public static RegistrySupplier<Block> CHISHI_FUSION_STRUCTURE_GLASS = null;
    /** 聚变隔热层：外壳与框架层之间的第二层，必须全部填充 */
    public static RegistrySupplier<Block> CHISHI_FUSION_INSULATION = null;
    /** 聚变控制器：主方块，持有全部聚变状态，右键打开三页界面 */
    public static RegistrySupplier<Block> CHISHI_FUSION_CONTROLLER = null;
    /** 聚变能量输出口：产出赤能源的墙面缓冲口（纯发电，管道只可抽取） */
    public static RegistrySupplier<Block> CHISHI_FUSION_ENERGY_OUTPUT = null;
    /** 聚变物品输入口：燃料棒投放口（管道/漏斗/手动投料） */
    public static RegistrySupplier<Block> CHISHI_FUSION_ITEM_INPUT = null;
    /** 聚变物品输出口：生命灰烬输出口（管道/手动收取） */
    public static RegistrySupplier<Block> CHISHI_FUSION_ITEM_OUTPUT = null;
    /** 聚变核心：结构中心方块（恰 1 个），拆掉结构即失效 */
    public static RegistrySupplier<Block> CHISHI_FUSION_CORE = null;
    /** 聚变散热框架：框架层纯结构件，为控制器解锁散热片槽（上限 10） */
    public static RegistrySupplier<Block> CHISHI_FUSION_COOLER_FRAME = null;
    /** 聚变燃料框架：框架层结构件，每框架解锁 1 个燃料槽（上限 4） */
    public static RegistrySupplier<Block> CHISHI_FUSION_FUEL_FRAME = null;
    /** 聚变效率框架：框架层结构件，每个使产率/产热 ×1.15（上限 12） */
    public static RegistrySupplier<Block> CHISHI_FUSION_EFFICIENCY_FRAME = null;

    private AkaishiFusionBlocks() {
    }

    /** 注册全部聚变方块（由 AkaishiMod.init 调用） */
    public static void register() {
        Registrar<Block> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);
        CHISHI_FUSION_FUEL_AGGREGATOR = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_fusion_fuel_aggregator", AkaishiFusionFuelAggregatorBlock::new);
        CHISHI_FUSION_SHELL = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_fusion_shell", AkaishiFusionShellBlock::new);
        CHISHI_FUSION_STRUCTURE_GLASS = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_fusion_structure_glass", AkaishiStructureGlassBlock::new);
        CHISHI_FUSION_INSULATION = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_fusion_insulation", AkaishiFusionInsulationBlock::new);
        CHISHI_FUSION_CONTROLLER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_fusion_controller", AkaishiFusionControllerBlock::new);
        CHISHI_FUSION_ENERGY_OUTPUT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_fusion_energy_output", AkaishiFusionEnergyOutputBlock::new);
        CHISHI_FUSION_ITEM_INPUT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_fusion_item_input", AkaishiFusionItemInputPortBlock::new);
        CHISHI_FUSION_ITEM_OUTPUT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_fusion_item_output", AkaishiFusionItemOutputPortBlock::new);
        CHISHI_FUSION_CORE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_fusion_core", AkaishiFusionCoreBlock::new);
        CHISHI_FUSION_COOLER_FRAME = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_fusion_cooler_frame", AkaishiFusionCoolerFrameBlock::new);
        CHISHI_FUSION_FUEL_FRAME = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_fusion_fuel_frame", AkaishiFusionFuelFrameBlock::new);
        CHISHI_FUSION_EFFICIENCY_FRAME = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_fusion_efficiency_frame", AkaishiFusionEfficiencyFrameBlock::new);
    }
}
