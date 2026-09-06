package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;

/**
 * 赤石水晶方块族注册表。
 * <p>
 * 从 ModBlocks 拆分出的域注册类：4 级水晶母岩（晶洞）、水晶簇、水晶块、
 * 4 级催化器、4 级自动收集器——围绕「母岩长簇 → 收获精华」的水晶循环体系。
 * 所有静态字段显式初始化为 null，由 {@link #register()} 在 {@link AkaishiMod#init()}
 * 阶段填充；任何消费方都须在 register() 之后访问，否则会触发 NPE。
 */
public final class AkaishiCrystalBlocks {

    /** 赤石水晶母岩（瑕疵）：晶洞外层自然生成，可生长水晶簇，可在聚合器升级 */
    public static RegistrySupplier<Block> CHISHI_GEODE_FLAWED = null;
    /** 赤石水晶母岩（普通） */
    public static RegistrySupplier<Block> CHISHI_GEODE_NORMAL = null;
    /** 赤石水晶母岩（完好） */
    public static RegistrySupplier<Block> CHISHI_GEODE_PRISTINE = null;
    /** 赤石水晶母岩（完美） */
    public static RegistrySupplier<Block> CHISHI_GEODE_PERFECT = null;
    /** 赤石水晶簇：母岩生长/晶洞生成，破坏掉落赤石精华 */
    public static RegistrySupplier<Block> CHISHI_CRYSTAL_CLUSTER = null;
    /** 赤石水晶块：9 簇合成，提纯器提纯成精华 */
    public static RegistrySupplier<Block> CHISHI_CRYSTAL_BLOCK = null;
    /** 赤石催化器（初级）：催生范围内母岩生长水晶簇 */
    public static RegistrySupplier<Block> CHISHI_CATALYST_BASIC = null;
    /** 赤石催化器（中级） */
    public static RegistrySupplier<Block> CHISHI_CATALYST_MEDIUM = null;
    /** 赤石催化器（高级） */
    public static RegistrySupplier<Block> CHISHI_CATALYST_ADVANCED = null;
    /** 赤石催化器（终极） */
    public static RegistrySupplier<Block> CHISHI_CATALYST_ULTIMATE = null;
    /** 自动收集器（初级）：自动收获范围内水晶簇 */
    public static RegistrySupplier<Block> CHISHI_COLLECTOR_BASIC = null;
    /** 自动收集器（中级） */
    public static RegistrySupplier<Block> CHISHI_COLLECTOR_MEDIUM = null;
    /** 自动收集器（高级） */
    public static RegistrySupplier<Block> CHISHI_COLLECTOR_ADVANCED = null;
    /** 自动收集器（终极） */
    public static RegistrySupplier<Block> CHISHI_COLLECTOR_ULTIMATE = null;

    private AkaishiCrystalBlocks() {
    }

    /** 注册全部水晶体系方块（由 AkaishiMod.init 调用） */
    public static void register() {
        Registrar<Block> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);
        // 赤石水晶母岩（4 级）：晶洞外层自然生成，放置后生长水晶簇，聚合器可升级
        CHISHI_GEODE_FLAWED = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_geode_flawed",
                () -> new AkaishiGeodeBlock(AkaishiGeodeBlock.GeodeTier.FLAWED, MapColor.COLOR_LIGHT_GRAY));
        CHISHI_GEODE_NORMAL = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_geode_normal",
                () -> new AkaishiGeodeBlock(AkaishiGeodeBlock.GeodeTier.NORMAL, MapColor.COLOR_RED));
        CHISHI_GEODE_PRISTINE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_geode_pristine",
                () -> new AkaishiGeodeBlock(AkaishiGeodeBlock.GeodeTier.PRISTINE, MapColor.GOLD));
        CHISHI_GEODE_PERFECT = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_geode_perfect",
                () -> new AkaishiGeodeBlock(AkaishiGeodeBlock.GeodeTier.PERFECT, MapColor.COLOR_PURPLE));

        // 赤石水晶簇（破坏掉落精华）
        CHISHI_CRYSTAL_CLUSTER = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_crystal_cluster",
                AkaishiCrystalClusterBlock::new);
        // 赤石水晶块（提纯器提纯成精华）
        CHISHI_CRYSTAL_BLOCK = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_crystal_block",
                AkaishiCrystalBlock::new);

        // 赤石催化器（4 级）：催生范围内母岩生长水晶簇，消耗赤能源
        CHISHI_CATALYST_BASIC = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_catalyst_basic",
                () -> new AkaishiCatalystBlock(AkaishiCatalystBlock.CatalystTier.BASIC));
        CHISHI_CATALYST_MEDIUM = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_catalyst_medium",
                () -> new AkaishiCatalystBlock(AkaishiCatalystBlock.CatalystTier.MEDIUM));
        CHISHI_CATALYST_ADVANCED = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_catalyst_advanced",
                () -> new AkaishiCatalystBlock(AkaishiCatalystBlock.CatalystTier.ADVANCED));
        CHISHI_CATALYST_ULTIMATE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_catalyst_ultimate",
                () -> new AkaishiCatalystBlock(AkaishiCatalystBlock.CatalystTier.ULTIMATE));

        // 自动收集器（4 级）：自动收获范围内水晶簇，精华存入内部 27 槽容器
        CHISHI_COLLECTOR_BASIC = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_collector_basic",
                () -> new AkaishiAutoCollectorBlock(AkaishiAutoCollectorBlock.CollectorTier.BASIC));
        CHISHI_COLLECTOR_MEDIUM = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_collector_medium",
                () -> new AkaishiAutoCollectorBlock(AkaishiAutoCollectorBlock.CollectorTier.MEDIUM));
        CHISHI_COLLECTOR_ADVANCED = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_collector_advanced",
                () -> new AkaishiAutoCollectorBlock(AkaishiAutoCollectorBlock.CollectorTier.ADVANCED));
        CHISHI_COLLECTOR_ULTIMATE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_collector_ultimate",
                () -> new AkaishiAutoCollectorBlock(AkaishiAutoCollectorBlock.CollectorTier.ULTIMATE));
    }
}
