package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

/**
 * 无线赤能源方块族注册表（无线终端多方块体系）。
 * <p>
 * 从 ModBlocks 拆分出的域注册类：14 个无线终端/端口/组件方块。字段由
 * {@link #register()} 在 {@link com.example.akaishi.AkaishiMod#init()} 阶段填充，
 * 消费方须在 register() 之后访问。
 */
public final class AkaishiWirelessBlocks {

    /** 无线赤能源终端外壳：无线终端多方块（5×5×5）墙面填充方块 */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_SHELL = null;
    /** 无线终端结构玻璃：半透明观察窗，可替代无线赤能源终端外壳 */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_STRUCTURE_GLASS = null;
    /** 无线赤能源终端方块：外墙主方块（GUI 入口，成型后为网络能量中枢） */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_TERMINAL = null;
    /** 无线赤能源终端安全方块：外墙方块 + 安全卡认证页直达入口 */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_SECURITY = null;
    /** 无线赤能源终端核心：内腔中心方块（恰 1 个），拆掉结构即失效 */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_CORE = null;
    /** 无线赤能源控制器：外墙纯结构件（无 GUI 无 BE） */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_CONTROLLER = null;
    /** 无线赤能源输入口：能量管道 → 无线频道的发送端 */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_INPUT_PORT = null;
    /** 无线赤能源输出口：无线频道 → 能量管道的接收端 */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_OUTPUT_PORT = null;
    /** 终端跨维组件：内腔 ≥1 个解锁跨维度传输 */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_DIM_BRIDGE = null;
    /** 区块加载构架：内腔 ≥1 个使网络区块弱加载（离线运转） */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_CHUNK_LOADER = null;
    /** 区块加载扩展组件：内腔 ≥1 个使弱加载范围扩为 3×3 区块（终端与口） */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_CHUNK_RANGE = null;
    /** 输入损耗抑制组件：内腔每个降低输入口方向损耗（可叠加） */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_INPUT_LOSS = null;
    /** 输出损耗抑制组件：内腔每个降低输出口方向损耗（可叠加） */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_OUTPUT_LOSS = null;

    private AkaishiWirelessBlocks() {
    }

    /** 注册全部无线方块（由 AkaishiMod.init 调用） */
    public static void register() {
        Registrar<Block> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);
        CHISHI_WIRELESS_SHELL = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_wireless_shell", AkaishiWirelessShellBlock::new);
        CHISHI_WIRELESS_STRUCTURE_GLASS = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_wireless_structure_glass", AkaishiStructureGlassBlock::new);
        CHISHI_WIRELESS_TERMINAL = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_wireless_terminal", AkaishiWirelessTerminalBlock::new);
        CHISHI_WIRELESS_SECURITY = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_wireless_security", AkaishiWirelessSecurityBlock::new);
        CHISHI_WIRELESS_CORE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_wireless_core", AkaishiWirelessCoreBlock::new);
        CHISHI_WIRELESS_CONTROLLER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_wireless_controller", AkaishiWirelessControllerBlock::new);
        CHISHI_WIRELESS_INPUT_PORT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_wireless_input_port", AkaishiWirelessInputPortBlock::new);
        CHISHI_WIRELESS_OUTPUT_PORT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_wireless_output_port", AkaishiWirelessOutputPortBlock::new);
        CHISHI_WIRELESS_DIM_BRIDGE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_wireless_dim_bridge", AkaishiWirelessDimBridgeBlock::new);
        CHISHI_WIRELESS_CHUNK_LOADER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_wireless_chunk_loader", AkaishiWirelessChunkLoaderBlock::new);
        CHISHI_WIRELESS_CHUNK_RANGE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_wireless_chunk_range", AkaishiWirelessChunkRangeBlock::new);
        CHISHI_WIRELESS_INPUT_LOSS = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_wireless_input_loss", AkaishiWirelessInputLossBlock::new);
        CHISHI_WIRELESS_OUTPUT_LOSS = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_wireless_output_loss", AkaishiWirelessOutputLossBlock::new);
    }
}
