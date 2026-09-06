package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

/**
 * 赤石矿机方块族注册表。
 * <p>
 * 从 ModBlocks 拆分出的域注册类：4 级矿机控制器（基础/进阶/超级/终极，等级由方块
 * 实例决定）+ 架构框架 + 升级框架 + 转口 + 3 种升级模块（速度/时运/储能）。
 * 所有静态字段显式初始化为 null，由 {@link #register()} 在 {@link AkaishiMod#init()}
 * 阶段填充；任何消费方都须在 register() 之后访问，否则会触发 NPE。
 */
public final class AkaishiMinerBlocks {

    /** 赤石矿机控制器（基础级，9×9×5 多方块主方块） */
    public static RegistrySupplier<Block> CHISHI_MINER_CONTROLLER_BASIC = null;
    /** 赤石矿机控制器（进阶级） */
    public static RegistrySupplier<Block> CHISHI_MINER_CONTROLLER_ADVANCED = null;
    /** 赤石矿机控制器（高级级） */
    public static RegistrySupplier<Block> CHISHI_MINER_CONTROLLER_SUPER = null;
    /** 赤石矿机控制器（终极级） */
    public static RegistrySupplier<Block> CHISHI_MINER_CONTROLLER_ULTIMATE = null;
    /** 矿机架构（结构框架方块） */
    public static RegistrySupplier<Block> CHISHI_MINER_FRAME = null;
    /** 矿机架构【矿机升级】（升级模块的安装位置，纯结构件） */
    public static RegistrySupplier<Block> CHISHI_MINER_UPGRADE_FRAME = null;
    /** 矿机转口（产物输出 + 赤能源输入） */
    public static RegistrySupplier<Block> CHISHI_MINER_PORT = null;
    /** 钻机钻头（9×9×5 结构最底层中心柱，成型时向下打出信标光束） */
    public static RegistrySupplier<Block> CHISHI_MINER_DRILL_BIT = null;
    /** 矿机架构【外接】（立柱专用结构件，可替换接入位） */
    public static RegistrySupplier<Block> CHISHI_MINER_FRAME_EXTERNAL = null;
    /** 矿机能量输入口（立柱设备：赤能源缓冲 → 控制器） */
    public static RegistrySupplier<Block> CHISHI_MINER_ENERGY_INPUT = null;
    /** 矿机物品输出口（立柱设备：产物缓冲供管道/漏斗抽取） */
    public static RegistrySupplier<Block> CHISHI_MINER_ITEM_OUTPUT = null;
    /** 矿机速度升级模块（方块形式，安装于升级框架位置） */
    public static RegistrySupplier<Block> CHISHI_MINER_SPEED_UPGRADE_BLOCK = null;
    /** 矿机时运升级模块（方块形式，安装于升级框架位置） */
    public static RegistrySupplier<Block> CHISHI_MINER_FORTUNE_UPGRADE_BLOCK = null;
    /** 矿机储能升级模块（方块形式，安装于升级框架位置） */
    public static RegistrySupplier<Block> CHISHI_MINER_STORAGE_UPGRADE_BLOCK = null;

    private AkaishiMinerBlocks() {
    }

    /** 注册全部矿机方块（由 AkaishiMod.init 调用） */
    public static void register() {
        Registrar<Block> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);
        CHISHI_MINER_CONTROLLER_BASIC = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_miner_controller_basic",
                () -> new AkaishiMinerControllerBlock(AkaishiMinerTier.BASIC));
        CHISHI_MINER_CONTROLLER_ADVANCED = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_miner_controller_advanced",
                () -> new AkaishiMinerControllerBlock(AkaishiMinerTier.ADVANCED));
        CHISHI_MINER_CONTROLLER_SUPER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_miner_controller_super",
                () -> new AkaishiMinerControllerBlock(AkaishiMinerTier.SUPER));
        CHISHI_MINER_CONTROLLER_ULTIMATE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_miner_controller_ultimate",
                () -> new AkaishiMinerControllerBlock(AkaishiMinerTier.ULTIMATE));
        CHISHI_MINER_FRAME = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_miner_frame", AkaishiMinerFrameBlock::new);
        CHISHI_MINER_UPGRADE_FRAME = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_miner_upgrade_frame",
                AkaishiMinerUpgradeFrameBlock::new);
        CHISHI_MINER_PORT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_miner_port", AkaishiMinerPortBlock::new);
        // 9×9×5 结构新增件：钻机钻头（底部中心）/ 架构【外接】（立柱可替换结构件）
        CHISHI_MINER_DRILL_BIT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_miner_drill_bit",
                AkaishiMinerDrillBitBlock::new);
        CHISHI_MINER_FRAME_EXTERNAL = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_miner_frame_external",
                AkaishiMinerExternalFrameBlock::new);
        // 立柱设备：能量输入口（赤能源缓冲）/ 物品输出口（产物缓冲）
        CHISHI_MINER_ENERGY_INPUT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_miner_energy_input",
                AkaishiMinerEnergyInputBlock::new);
        CHISHI_MINER_ITEM_OUTPUT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_miner_item_output",
                AkaishiMinerItemOutputBlock::new);
        // 矿机升级模块（方块形式，替换升级框架安装，上限：速度 10 / 时运 4 / 储能 10）
        CHISHI_MINER_SPEED_UPGRADE_BLOCK = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_miner_speed_upgrade_block",
                () -> new AkaishiMinerUpgradeBlock(AkaishiMinerUpgradeType.SPEED));
        CHISHI_MINER_FORTUNE_UPGRADE_BLOCK = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_miner_fortune_upgrade_block",
                () -> new AkaishiMinerUpgradeBlock(AkaishiMinerUpgradeType.FORTUNE));
        CHISHI_MINER_STORAGE_UPGRADE_BLOCK = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_miner_storage_upgrade_block",
                () -> new AkaishiMinerUpgradeBlock(AkaishiMinerUpgradeType.STORAGE));
    }
}
