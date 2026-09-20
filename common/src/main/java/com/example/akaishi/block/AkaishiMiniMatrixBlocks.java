package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

/**
 * 微缩矩阵方块族注册表（P1a：结构 + 芯片识别）。
 * <p>
 * 5×5×5 空腔箱体，控制器贴墙；墙面可贴放已完成的<b>微缩终端（芯片）</b>方块，
 * 由控制器方块实体识别并读取其读数面（{@code api/miniature/IMiniatureChipView}）。
 * <p>
 * 外壳与结构玻璃<b>复用矩阵族既有类与贴图</b>（RULES §7 允许同族复用，不新造美术）；
 * 因类被复用，墙面白名单按<b>方块实例</b>比对以保证族隔离（见
 * {@code AkaishiMiniMatrixTerminalBlockEntity.isWallBlock}）。
 * 字段由 {@link #register()} 在 {@link AkaishiMod#init()} 阶段填充，消费方须在其后访问。
 */
public final class AkaishiMiniMatrixBlocks {

    /** 微缩矩阵外壳：箱体墙面填充块（可被结构玻璃/控制器/芯片替代） */
    public static RegistrySupplier<Block> CHISHI_MINI_MATRIX_CASING = null;
    /** 微缩矩阵结构玻璃：观察窗，可替代外壳 */
    public static RegistrySupplier<Block> CHISHI_MINI_MATRIX_STRUCTURE_GLASS = null;
    /** 微缩矩阵终端：控制器/主方块（贴墙，成型判定与芯片读取中枢，右键开界面） */
    public static RegistrySupplier<Block> CHISHI_MINI_MATRIX_TERMINAL = null;

    // ===== 内腔升级组件（5 类，装在箱体内腔，控制器扫描计数生效） =====

    /** ①无线能源操控组件 */
    public static RegistrySupplier<Block> CHISHI_MINI_MATRIX_UPGRADE_CONTROL = null;
    /** ②无线场域升级（每块 +1 区块半径，最高 3） */
    public static RegistrySupplier<Block> CHISHI_MINI_MATRIX_UPGRADE_FIELD = null;
    /** ③无线拓展升级（每块 1 个附加节点，最高 3） */
    public static RegistrySupplier<Block> CHISHI_MINI_MATRIX_UPGRADE_EXTEND = null;
    /** ④无线加工升级 */
    public static RegistrySupplier<Block> CHISHI_MINI_MATRIX_UPGRADE_CRAFT = null;
    /** ⑤无线场域联动升级 */
    public static RegistrySupplier<Block> CHISHI_MINI_MATRIX_UPGRADE_LINK = null;

    /** 无线网络节点：配合「无线拓展升级」，每块自带 1 区块子场域（最多 3 块生效） */
    public static RegistrySupplier<Block> CHISHI_MINI_MATRIX_NETWORK_NODE = null;

    /** 无线接入器：贴相邻的第三方机器，把它接入无线场域（第三方认可架构的载体） */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_ACCESS_ADAPTER = null;

    private AkaishiMiniMatrixBlocks() {
    }

    /** 注册全部微缩矩阵方块（由 AkaishiMod.init 在 ModBlockEntities.register 之前调用） */
    public static void register() {
        Registrar<Block> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);
        // 外壳/玻璃直接复用矩阵族既有方块类，仅注册独立 id（族隔离靠实例比对，见 BE 的 isWallBlock）
        CHISHI_MINI_MATRIX_CASING = AkaishiBlockRegistrar.registerMachineBlock(registrar,
                "akaishi_mini_matrix_casing", AkaishiPurifierMatrixCasingBlock::new);
        CHISHI_MINI_MATRIX_STRUCTURE_GLASS = AkaishiBlockRegistrar.registerMachineBlock(registrar,
                "akaishi_mini_matrix_structure_glass", AkaishiStructureGlassBlock::new);
        CHISHI_MINI_MATRIX_TERMINAL = AkaishiBlockRegistrar.registerMachineBlock(registrar,
                "akaishi_mini_matrix_terminal", AkaishiMiniMatrixTerminalBlock::new);
        // 内腔升级组件：5 类同构，仅构造注入的类型不同（读数与资源均按类型分派）
        CHISHI_MINI_MATRIX_UPGRADE_CONTROL = registerUpgrade(registrar, AkaishiMiniMatrixUpgradeType.CONTROL);
        CHISHI_MINI_MATRIX_UPGRADE_FIELD = registerUpgrade(registrar, AkaishiMiniMatrixUpgradeType.FIELD);
        CHISHI_MINI_MATRIX_UPGRADE_EXTEND = registerUpgrade(registrar, AkaishiMiniMatrixUpgradeType.EXTEND);
        CHISHI_MINI_MATRIX_UPGRADE_CRAFT = registerUpgrade(registrar, AkaishiMiniMatrixUpgradeType.CRAFT);
        CHISHI_MINI_MATRIX_UPGRADE_LINK = registerUpgrade(registrar, AkaishiMiniMatrixUpgradeType.LINK);
        // 网络节点：独立放置的世界方块（不是内腔组件），由场域内的矩阵按拓展升级数量申领
        CHISHI_MINI_MATRIX_NETWORK_NODE = AkaishiBlockRegistrar.registerMachineBlock(registrar,
                "akaishi_mini_matrix_network_node", AkaishiMiniMatrixNetworkNodeBlock::new);
        // 接入器：独立放置的世界方块，贴相邻第三方机器即把它接入无线场域
        CHISHI_WIRELESS_ACCESS_ADAPTER = AkaishiBlockRegistrar.registerMachineBlock(registrar,
                "akaishi_wireless_access_adapter", AkaishiWirelessAccessAdapterBlock::new);
    }

    private static RegistrySupplier<Block> registerUpgrade(Registrar<Block> registrar,
            AkaishiMiniMatrixUpgradeType type) {
        return AkaishiBlockRegistrar.registerMachineBlock(registrar, type.blockId(),
                () -> new AkaishiMiniMatrixUpgradeBlock(type));
    }
}
