package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.energy.EnergyCellTier;
import com.example.akaishi.energy.EnergyPipeTier;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

/**
 * 赤能源（赤石能量体系）方块族注册表。
 * <p>
 * 从 ModBlocks 拆分出的域注册类：3 级能量储存单元、4 级能量管道、赤能源发生机、
 * 小型组合结构、储存串联器、超级发生器核心、赤石能量聚合器、能量液化/加工、
 * 创造能量源（赤能源版）。围绕「发电 → 传输 → 储存 → 液化燃料」的赤能源主线。
 * 所有静态字段显式初始化为 null，由 {@link #register()} 在 {@link AkaishiMod#init()}
 * 阶段填充；任何消费方都须在 register() 之后访问，否则会触发 NPE。
 */
public final class AkaishiEnergyBlocks {

    /** 赤能源储存单元（基础级） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_CELL_BASIC = null;
    /** 赤能源储存单元（高级） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_CELL_ADVANCED = null;
    /** 赤能源储存单元（超级） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_CELL_SUPER = null;
    /** 赤能源管道（基础，能量网络中继） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_PIPE = null;
    /** 赤能源管道（高级） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_PIPE_ADVANCED = null;
    /** 赤能源管道（精英） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_PIPE_ELITE = null;
    /** 赤能源管道（终极） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_PIPE_ULTIMATE = null;
    /** 赤能源发生机（燃烧赤石材料产赤能源，单方块 / 多方块外壳） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_GENERATOR = null;
    /** 小型赤能源组合结构（多方块主方块） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_ASSEMBLY = null;
    /** 赤能源储存串联器（多方块主方块） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_CELL_SERIALIZER = null;
    /** 超级发生器架构核心（5×5×5 多方块主方块） */
    public static RegistrySupplier<Block> CHISHI_SUPER_GENERATOR_CORE = null;
    /** 赤石能量聚合器（赤能源 + 下界合金锭 → 赤石锭） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_AGGREGATOR = null;
    /** 创造赤能源储存原件（无限输出测试方块） */
    public static RegistrySupplier<Block> CHISHI_CREATIVE_ENERGY_CELL = null;
    /** 能量液化装置（赤能源驱动，下界之星 → 至纯能量 / 凋零玫瑰 → 复合能量） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_LIQUEFIER = null;
    /** 能量加工器（赤能源驱动，生命固态物 + 下界能量液体 → 反应堆燃料） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_PROCESSOR = null;

    private AkaishiEnergyBlocks() {
    }

    /** 注册全部赤能源体系方块（由 AkaishiMod.init 调用） */
    public static void register() {
        Registrar<Block> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);
        // 赤能源储存单元（基础/高级/超级）
        CHISHI_ENERGY_CELL_BASIC = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_energy_cell_basic",
                () -> new AkaishiEnergyCellBlock(EnergyCellTier.BASIC));
        CHISHI_ENERGY_CELL_ADVANCED = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_energy_cell_advanced",
                () -> new AkaishiEnergyCellBlock(EnergyCellTier.ADVANCED));
        CHISHI_ENERGY_CELL_SUPER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_energy_cell_super",
                () -> new AkaishiEnergyCellBlock(EnergyCellTier.SUPER));

        // 赤能源管道（基础/高级/精英/终极）
        CHISHI_ENERGY_PIPE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_energy_pipe",
                () -> new AkaishiEnergyPipeBlock(EnergyPipeTier.BASIC));
        CHISHI_ENERGY_PIPE_ADVANCED = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_energy_pipe_advanced",
                () -> new AkaishiEnergyPipeBlock(EnergyPipeTier.ADVANCED));
        CHISHI_ENERGY_PIPE_ELITE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_energy_pipe_elite",
                () -> new AkaishiEnergyPipeBlock(EnergyPipeTier.ELITE));
        CHISHI_ENERGY_PIPE_ULTIMATE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_energy_pipe_ultimate",
                () -> new AkaishiEnergyPipeBlock(EnergyPipeTier.ULTIMATE));

        // 赤能源发生机 + 小型赤能源组合结构（含各自 BlockItem）
        CHISHI_ENERGY_GENERATOR = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_energy_generator",
                AkaishiEnergyGeneratorBlock::new);
        CHISHI_ENERGY_ASSEMBLY = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_energy_assembly",
                AkaishiEnergyAssemblyBlock::new);
        // 赤能源储存串联器（3×3×3 多方块主方块，26 个储存单元环绕成型）
        CHISHI_ENERGY_CELL_SERIALIZER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_energy_cell_serializer",
                AkaishiEnergyCellSerializerBlock::new);
        // 超级发生器架构核心（5×5×5 多方块主方块，124 台发生机环绕成型）
        CHISHI_SUPER_GENERATOR_CORE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_super_generator_core",
                AkaishiSuperGeneratorCoreBlock::new);

        // 赤石能量聚合器（10M 赤能源 + 下界合金锭 → 赤石锭）
        CHISHI_ENERGY_AGGREGATOR = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_energy_aggregator",
                AkaishiEnergyAggregatorBlock::new);

        // 能量液化装置（下界之星 → 至纯能量 / 凋零玫瑰 → 复合能量）
        CHISHI_ENERGY_LIQUEFIER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_energy_liquefier",
                AkaishiEnergyLiquefierBlock::new);
        // 能量加工器（生命固态物 + 下界能量液体 → 反应堆燃料）
        CHISHI_ENERGY_PROCESSOR = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_energy_processor",
                AkaishiEnergyProcessorBlock::new);

        // 创造模式能量源（测试用，无限输出，赤能源版；生命能量版在生命域）
        CHISHI_CREATIVE_ENERGY_CELL = AkaishiBlockRegistrar.registerMachineBlock(registrar, "creative_akaishi_energy_cell",
                () -> new CreativeEnergySourceBlock(AkaishiEnergyType.INSTANCE));
    }
}
