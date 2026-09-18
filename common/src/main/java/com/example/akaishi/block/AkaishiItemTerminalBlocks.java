package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

/**
 * 物品终端方块族注册表（IP 物品库体系）。
 * <p>
 * 从 ModBlocks 拆分出的域注册类：物品终端本体 + 各阶物品储存单元 + 赤能源接入口。
 * 字段由 {@link #register()} 在 {@link com.example.akaishi.AkaishiMod#init()} 阶段填充，
 * 消费方须在 register() 之后访问。
 */
public final class AkaishiItemTerminalBlocks {

    /** 物品终端：外墙主方块（GUI 入口，复用 5×5×5 同族壳体） */
    public static RegistrySupplier<Block> CHISHI_ITEM_TERMINAL = null;
    /** 物品储存单元·基础（1e6 IP） */
    public static RegistrySupplier<Block> CHISHI_ITEM_STORAGE_UNIT_BASIC = null;
    /** 物品储存单元·进阶（8e6 IP） */
    public static RegistrySupplier<Block> CHISHI_ITEM_STORAGE_UNIT_ADVANCED = null;
    /** 物品储存单元·超级（6.4e7 IP） */
    public static RegistrySupplier<Block> CHISHI_ITEM_STORAGE_UNIT_SUPER = null;
    /** 物品终端赤能源接入口（贴装于结构外侧 1 格，多口并联） */
    public static RegistrySupplier<Block> CHISHI_ITEM_TERMINAL_ENERGY_INPUT = null;
    /** 物品终端外壳：本族多方块墙面填充方块（无线族不认，反之亦然） */
    public static RegistrySupplier<Block> CHISHI_ITEM_TERMINAL_SHELL = null;
    /** 物品终端结构玻璃：本族观察窗，可替代物品终端外壳 */
    public static RegistrySupplier<Block> CHISHI_ITEM_TERMINAL_STRUCTURE_GLASS = null;
    /** 物品终端核心：内腔核心（恰 1 个），与无线终端核心分开成独立方块 */
    public static RegistrySupplier<Block> CHISHI_ITEM_TERMINAL_CORE = null;
    /** 缓冲扩展组件：内腔功能件，每个 +1M 赤能源缓冲（抬高单笔上限），最多 8 个生效 */
    public static RegistrySupplier<Block> CHISHI_ITEM_TERMINAL_BUFFER_MODULE = null;
    /** 费率减免组件：内腔功能件，每个降低 10% 一次性存取费，最多 5 个生效 */
    public static RegistrySupplier<Block> CHISHI_ITEM_TERMINAL_FEE_MODULE = null;
    /** 区块加载构架：内腔功能件，使终端与贴装件所在区块弱加载（只认 1 个） */
    public static RegistrySupplier<Block> CHISHI_ITEM_TERMINAL_CHUNK_LOADER = null;
    /** 区块加载扩展组件：内腔功能件，把弱加载范围扩为 3×3（只认 1 个，需先有构架） */
    public static RegistrySupplier<Block> CHISHI_ITEM_TERMINAL_CHUNK_RANGE = null;
    /** 储存无线输入口：把面朝方向容器的物品抓进绑定的物品终端 */
    public static RegistrySupplier<Block> CHISHI_ITEM_INPUT_PORT = null;
    /** 储存无线输出口：把绑定的物品终端储存推给面朝方向的容器 */
    public static RegistrySupplier<Block> CHISHI_ITEM_OUTPUT_PORT = null;

    private AkaishiItemTerminalBlocks() {
    }

    /** 注册全部物品终端方块（由 AkaishiMod.init 调用） */
    public static void register() {
        Registrar<Block> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);
        CHISHI_ITEM_TERMINAL = AkaishiBlockRegistrar.registerMachineBlock(registrar,
                "akaishi_item_terminal", AkaishiItemTerminalBlock::new);
        // 储存单元三阶：方块按 ItemStorageUnitTier 参数化，共用同一方块实体类型
        CHISHI_ITEM_STORAGE_UNIT_BASIC = AkaishiBlockRegistrar.registerMachineBlock(registrar,
                "akaishi_item_storage_unit_basic", () -> new AkaishiItemStorageUnitBlock(ItemStorageUnitTier.BASIC));
        CHISHI_ITEM_STORAGE_UNIT_ADVANCED = AkaishiBlockRegistrar.registerMachineBlock(registrar,
                "akaishi_item_storage_unit_advanced", () -> new AkaishiItemStorageUnitBlock(ItemStorageUnitTier.ADVANCED));
        CHISHI_ITEM_STORAGE_UNIT_SUPER = AkaishiBlockRegistrar.registerMachineBlock(registrar,
                "akaishi_item_storage_unit_super", () -> new AkaishiItemStorageUnitBlock(ItemStorageUnitTier.SUPER));
        // 赤能源接入口：纯汇口，终端结算时主动抽取（口自身无 ticker）
        CHISHI_ITEM_TERMINAL_ENERGY_INPUT = AkaishiBlockRegistrar.registerMachineBlock(registrar,
                "akaishi_item_terminal_energy_input", AkaishiItemTerminalEnergyInputPortBlock::new);
        // 专属壳体族：与无线终端族完全隔离（无线族白名单不含本族，本族白名单亦不含无线族）
        CHISHI_ITEM_TERMINAL_SHELL = AkaishiBlockRegistrar.registerMachineBlock(registrar,
                "akaishi_item_terminal_shell", AkaishiItemTerminalShellBlock::new);
        CHISHI_ITEM_TERMINAL_STRUCTURE_GLASS = AkaishiBlockRegistrar.registerMachineBlock(registrar,
                "akaishi_item_terminal_structure_glass", AkaishiStructureGlassBlock::new);
        CHISHI_ITEM_TERMINAL_CORE = AkaishiBlockRegistrar.registerMachineBlock(registrar,
                "akaishi_item_terminal_core", AkaishiItemTerminalCoreBlock::new);
        // 内腔功能件（只认内腔、不吃墙面位；效果与上限见各自类）
        CHISHI_ITEM_TERMINAL_BUFFER_MODULE = AkaishiBlockRegistrar.registerMachineBlock(registrar,
                "akaishi_item_terminal_buffer_module", AkaishiItemTerminalBufferModuleBlock::new);
        CHISHI_ITEM_TERMINAL_FEE_MODULE = AkaishiBlockRegistrar.registerMachineBlock(registrar,
                "akaishi_item_terminal_fee_module", AkaishiItemTerminalFeeModuleBlock::new);
        CHISHI_ITEM_TERMINAL_CHUNK_LOADER = AkaishiBlockRegistrar.registerMachineBlock(registrar,
                "akaishi_item_terminal_chunk_loader", AkaishiItemTerminalChunkLoaderBlock::new);
        CHISHI_ITEM_TERMINAL_CHUNK_RANGE = AkaishiBlockRegistrar.registerMachineBlock(registrar,
                "akaishi_item_terminal_chunk_range", AkaishiItemTerminalChunkRangeBlock::new);
        // 储存无线输入/输出口：口朝点击面，把面朝方向容器的物品与终端库互搬（搬运在 AkaishiItemPortBlockEntity）
        CHISHI_ITEM_INPUT_PORT = AkaishiBlockRegistrar.registerMachineBlock(registrar,
                "akaishi_item_input_port", AkaishiItemInputPortBlock::new);
        CHISHI_ITEM_OUTPUT_PORT = AkaishiBlockRegistrar.registerMachineBlock(registrar,
                "akaishi_item_output_port", AkaishiItemOutputPortBlock::new);
    }
}
