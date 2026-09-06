package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

/**
 * 矩阵多方块方块族注册表。
 * <p>
 * 从 ModBlocks 拆分出的域注册类：发生器矩阵（GEN_MATRIX，低级 3×3×3 / 高级 5×5×5）、
 * 提纯矩阵（PURIFIER_MATRIX）与生命转换矩阵（LIFE_MATRIX），三类矩阵共用结构玻璃
 * {@link AkaishiStructureGlassBlock}，共 17 方块。
 * 所有静态字段显式初始化为 null，由 {@link #register()} 在 {@link AkaishiMod#init()}
 * 阶段填充；任何消费方都须在 register() 之后访问，否则会触发 NPE。
 */
public final class AkaishiMatrixBlocks {

    /** 发生器矩阵外壳：类反应堆式矩阵外壁（端口可替代外壳） */
    public static RegistrySupplier<Block> CHISHI_GEN_MATRIX_CASING = null;
    /** 发生器矩阵结构玻璃：半透明观察窗，可替代发生器矩阵外壳 */
    public static RegistrySupplier<Block> CHISHI_GEN_MATRIX_STRUCTURE_GLASS = null;
    /** 发生器矩阵控制器（低级 3×3×3，45 倍，沿用组合结构数据） */
    public static RegistrySupplier<Block> CHISHI_GEN_MATRIX_CONTROLLER_BASIC = null;
    /** 发生器矩阵控制器（高级 5×5×5，200 倍，沿用超级架构数据） */
    public static RegistrySupplier<Block> CHISHI_GEN_MATRIX_CONTROLLER_ADVANCED = null;
    /** 发生器矩阵能量输出口（纯发电，仅管道抽取） */
    public static RegistrySupplier<Block> CHISHI_GEN_ENERGY_OUTPUT = null;
    /** 发生器矩阵燃料输入口（燃料物品输入，管道/漏斗） */
    public static RegistrySupplier<Block> CHISHI_GEN_FUEL_INPUT = null;
    /** 提纯矩阵外壳：类反应堆式矩阵外壁（端口可替代外壳） */
    public static RegistrySupplier<Block> CHISHI_PURIFIER_MATRIX_CASING = null;
    /** 提纯矩阵结构玻璃：半透明观察窗，可替代提纯矩阵外壳 */
    public static RegistrySupplier<Block> CHISHI_PURIFIER_MATRIX_STRUCTURE_GLASS = null;
    /** 提纯矩阵控制器：主方块，结构成型后集中提纯 */
    public static RegistrySupplier<Block> CHISHI_PURIFIER_MATRIX_CONTROLLER = null;
    /** 提纯矩阵能量输入口（赤能源输入，仅管道供能） */
    public static RegistrySupplier<Block> CHISHI_PURIFIER_ENERGY_INPUT = null;
    /** 提纯矩阵物品输入口（提纯原料输入，管道/漏斗） */
    public static RegistrySupplier<Block> CHISHI_PURIFIER_ITEM_INPUT = null;
    /** 提纯矩阵物品输出口（提纯产物输出，仅管道抽取） */
    public static RegistrySupplier<Block> CHISHI_PURIFIER_ITEM_OUTPUT = null;
    /** 生命转换矩阵外壳：类反应堆式矩阵外壁（端口可替代外壳） */
    public static RegistrySupplier<Block> CHISHI_LIFE_MATRIX_CASING = null;
    /** 生命转换矩阵结构玻璃：半透明观察窗，可替代生命转换矩阵外壳 */
    public static RegistrySupplier<Block> CHISHI_LIFE_MATRIX_STRUCTURE_GLASS = null;
    /** 生命转换矩阵控制器：主方块，结构成型后 45 倍集中转换 */
    public static RegistrySupplier<Block> CHISHI_LIFE_MATRIX_CONTROLLER = null;
    /** 生命转换矩阵能量输入口（赤能源输入，仅管道供能） */
    public static RegistrySupplier<Block> CHISHI_LIFE_MATRIX_ENERGY_INPUT = null;
    /** 生命转换矩阵能量输出口（生命能量输出，仅管道抽取） */
    public static RegistrySupplier<Block> CHISHI_LIFE_MATRIX_ENERGY_OUTPUT = null;

    private AkaishiMatrixBlocks() {
    }

    /** 注册全部矩阵方块（由 AkaishiMod.init 调用） */
    public static void register() {
        Registrar<Block> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);
        // ===== 发生器矩阵（类反应堆式：立方体外壁成型，端口可替代外壳）=====
        CHISHI_GEN_MATRIX_CASING = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_gen_matrix_casing",
                AkaishiGenMatrixCasingBlock::new);
        CHISHI_GEN_MATRIX_STRUCTURE_GLASS = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_gen_matrix_structure_glass",
                AkaishiStructureGlassBlock::new);
        // 低级/高级控制器共用方块类，等级由实例决定
        CHISHI_GEN_MATRIX_CONTROLLER_BASIC = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_gen_matrix_controller_basic",
                () -> new AkaishiGenMatrixControllerBlock(AkaishiGenMatrixTier.BASIC));
        CHISHI_GEN_MATRIX_CONTROLLER_ADVANCED = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_gen_matrix_controller_advanced",
                () -> new AkaishiGenMatrixControllerBlock(AkaishiGenMatrixTier.ADVANCED));
        CHISHI_GEN_ENERGY_OUTPUT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_gen_energy_output",
                AkaishiGenEnergyOutputPortBlock::new);
        CHISHI_GEN_FUEL_INPUT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_gen_fuel_input",
                AkaishiGenFuelInputPortBlock::new);

        // ===== 提纯矩阵（类反应堆式：立方体外壁成型，端口可替代外壳）=====
        CHISHI_PURIFIER_MATRIX_CASING = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_purifier_matrix_casing",
                AkaishiPurifierMatrixCasingBlock::new);
        CHISHI_PURIFIER_MATRIX_STRUCTURE_GLASS = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_purifier_matrix_structure_glass",
                AkaishiStructureGlassBlock::new);
        CHISHI_PURIFIER_MATRIX_CONTROLLER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_purifier_matrix_controller",
                AkaishiPurifierMatrixControllerBlock::new);
        CHISHI_PURIFIER_ENERGY_INPUT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_purifier_energy_input",
                AkaishiPurifierEnergyInputPortBlock::new);
        CHISHI_PURIFIER_ITEM_INPUT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_purifier_item_input",
                AkaishiPurifierItemInputPortBlock::new);
        CHISHI_PURIFIER_ITEM_OUTPUT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_purifier_item_output",
                AkaishiPurifierItemOutputPortBlock::new);

        // ===== 生命转换矩阵（类反应堆式：立方体外壁成型，端口可替代外壳）=====
        CHISHI_LIFE_MATRIX_CASING = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_matrix_casing",
                AkaishiLifeMatrixCasingBlock::new);
        CHISHI_LIFE_MATRIX_STRUCTURE_GLASS = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_matrix_structure_glass",
                AkaishiStructureGlassBlock::new);
        CHISHI_LIFE_MATRIX_CONTROLLER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_matrix_controller",
                AkaishiLifeMatrixControllerBlock::new);
        CHISHI_LIFE_MATRIX_ENERGY_INPUT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_matrix_energy_input",
                AkaishiLifeMatrixEnergyInputPortBlock::new);
        CHISHI_LIFE_MATRIX_ENERGY_OUTPUT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_life_matrix_energy_output",
                AkaishiLifeMatrixEnergyOutputPortBlock::new);
    }
}
