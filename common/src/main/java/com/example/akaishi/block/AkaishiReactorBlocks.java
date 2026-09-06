package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

/**
 * 反应堆方块族注册表。
 * <p>
 * 从 ModBlocks 拆分出的域注册类：反应堆外壳/结构玻璃/控制器/燃料口/能量输出口/
 * 废品口/燃料棒/散热器/反应核心 + 衰竭保存桶（共 10 方块）。
 * 所有静态字段显式初始化为 null，由 {@link #register()} 在 {@link AkaishiMod#init()}
 * 阶段填充；任何消费方都须在 register() 之后访问，否则会触发 NPE。
 */
public final class AkaishiReactorBlocks {

    /** 反应堆外壳：多方块外壁（控制器/投放口/输出口/废品口也属外壁），右键打开控制器 */
    public static RegistrySupplier<Block> CHISHI_REACTOR_SHELL = null;
    /** 反应堆结构玻璃：半透明观察窗，可替代反应堆外壳 */
    public static RegistrySupplier<Block> CHISHI_REACTOR_STRUCTURE_GLASS = null;
    /** 反应堆控制器：主方块，持有全部反应堆状态，右键打开控制界面 */
    public static RegistrySupplier<Block> CHISHI_REACTOR_CONTROLLER = null;
    /** 燃料投放口：燃料罐物品输入（管道+手动），自动分配到控制器空燃料槽 */
    public static RegistrySupplier<Block> CHISHI_REACTOR_FUEL_PORT = null;
    /** 能量输出口：赤能源输出（纯发电，管道只可抽取） */
    public static RegistrySupplier<Block> CHISHI_REACTOR_ENERGY_OUTPUT = null;
    /** 废品输出口：衰竭燃料输出（液体管道只可抽取） */
    public static RegistrySupplier<Block> CHISHI_REACTOR_WASTE_PORT = null;
    /** 燃料棒组件：每根解锁 1 个燃料槽 */
    public static RegistrySupplier<Block> CHISHI_REACTOR_FUEL_ROD = null;
    /** 散热组件：装入散热片，贴邻燃料棒才有效 */
    public static RegistrySupplier<Block> CHISHI_REACTOR_COOLER = null;
    /** 反应核心：燃烧结算中心 */
    public static RegistrySupplier<Block> CHISHI_REACTOR_CORE = null;
    /** 衰竭保存桶：专储衰竭的生命燃料，带 GUI 液位 */
    public static RegistrySupplier<Block> CHISHI_EXHAUSTED_BARREL = null;

    private AkaishiReactorBlocks() {
    }

    /** 注册全部反应堆方块（由 AkaishiMod.init 调用） */
    public static void register() {
        Registrar<Block> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);
        CHISHI_REACTOR_SHELL = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_reactor_shell", AkaishiReactorShellBlock::new);
        CHISHI_REACTOR_STRUCTURE_GLASS = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_reactor_structure_glass", AkaishiStructureGlassBlock::new);
        CHISHI_REACTOR_CONTROLLER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_reactor_controller", AkaishiReactorControllerBlock::new);
        CHISHI_REACTOR_FUEL_PORT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_reactor_fuel_port", AkaishiReactorFuelPortBlock::new);
        CHISHI_REACTOR_ENERGY_OUTPUT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_reactor_energy_output", AkaishiReactorEnergyOutputBlock::new);
        CHISHI_REACTOR_WASTE_PORT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_reactor_waste_port", AkaishiReactorWastePortBlock::new);
        CHISHI_REACTOR_FUEL_ROD = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_reactor_fuel_rod", AkaishiReactorFuelRodBlock::new);
        CHISHI_REACTOR_COOLER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_reactor_cooler", AkaishiReactorCoolerBlock::new);
        CHISHI_REACTOR_CORE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_reactor_core", AkaishiReactorCoreBlock::new);
        CHISHI_EXHAUSTED_BARREL = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_exhausted_barrel", AkaishiExhaustedBarrelBlock::new);
    }
}
