package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * 方块注册门面（历史兼容壳）。
 * <p>
 * 注册实现已按功能域拆分到 {@link AkaishiFoundationBlocks} / {@link AkaishiLogisticsBlocks} /
 * {@link AkaishiTransgeneBlocks}；衰竭区域方块归并至 {@link AkaishiDecayBlocks}
 * （在 {@link AkaishiMod#init()} 中先于本门面注册）。本类仅保留字段转发与矿石查询入口，
 * 供历史代码经 {@code ModBlocks.xxx} 访问；新增注册一律写入对应功能域类，禁止再向本类堆积。
 */
public final class ModBlocks {

    /** 全部 16 个矿石组合定义（转发基础域） */
    public static final List<AkaishiOreDef> ALL_ORES = AkaishiFoundationBlocks.ALL_ORES;

    // ==================== 字段转发壳（注册完成后指向对应域类）====================

    // —— 基础域（AkaishiFoundationBlocks）——
    public static RegistrySupplier<Block> RAW_CHISHI_BLOCK;
    public static RegistrySupplier<Block> CHISHI_PURIFIER;
    public static RegistrySupplier<Block> CHISHI_ADVANCED_PURIFIER;
    public static RegistrySupplier<Block> CHISHI_ESSENCE_BLOCK;
    public static RegistrySupplier<Block> CHISHI_EQUIPMENT_FORGER;
    public static RegistrySupplier<Block> CHISHI_UPGRADE_STATION;
    public static RegistrySupplier<Block> CHISHI_LIFE_FUSION_ANVIL;
    public static RegistrySupplier<Block> CHISHI_FUEL_CANNER;
    public static RegistrySupplier<Block> CHISHI_FUEL_MIXER;
    public static RegistrySupplier<Block> CHISHI_LIFE_ACTIVATOR;
    public static RegistrySupplier<Block> CHISHI_LIFE_CENTRIFUGE;
    public static RegistrySupplier<Block> CHISHI_ITEM_RECONSTRUCTOR;
    public static RegistrySupplier<Block> CHISHI_PLANT_CULTIVATOR;
    public static RegistrySupplier<Block> CHISHI_COMPRESSOR;
    public static RegistrySupplier<Block> CHISHI_PULVERIZER;
    public static RegistrySupplier<Block> CHISHI_TRANSFORMER;
    public static RegistrySupplier<Block> CHISHI_ACTIVATED_FRACTIONATOR;
    public static RegistrySupplier<Block> CHISHI_PLASMA_FILLER;

    // —— 物流域（AkaishiLogisticsBlocks）——
    public static RegistrySupplier<Block> CHISHI_ITEM_PIPE;
    public static RegistrySupplier<Block> CHISHI_ITEM_PIPE_ADVANCED;
    public static RegistrySupplier<Block> CHISHI_ITEM_PIPE_ELITE;
    public static RegistrySupplier<Block> CHISHI_ITEM_PIPE_ULTIMATE;
    public static RegistrySupplier<Block> CHISHI_FLUID_PIPE;
    public static RegistrySupplier<Block> CHISHI_EXHAUSTED_PIPE;
    public static RegistrySupplier<Block> CHISHI_MULTI_FLUID_WASTE_PIPE;
    public static RegistrySupplier<Block> CHISHI_PLASMA_PIPE;
    public static RegistrySupplier<Block> CHISHI_FLUID_TANK_BASIC;
    public static RegistrySupplier<Block> CHISHI_FLUID_TANK_ADVANCED;
    public static RegistrySupplier<Block> CHISHI_FLUID_TANK_SUPER;
    public static RegistrySupplier<Block> CHISHI_PLASMA_TANK;

    // —— 转基因域（AkaishiTransgeneBlocks）：转基因植物（凋零藤）——
    public static RegistrySupplier<Block> CHISHI_WITHER_ROOT;
    public static RegistrySupplier<Block> CHISHI_WITHER_STEM;

    // —— 衰竭域（AkaishiDecayBlocks）：区域治理与终态方块 ——
    public static RegistrySupplier<Block> CHISHI_DECAY_PURIFIER;
    public static RegistrySupplier<Block> CHISHI_DECAY_SOIL;
    public static RegistrySupplier<Block> CHISHI_DECAY_LOG;

    private ModBlocks() {
    }

    /** 注册入口：先完成各域注册，再转发字段供历史引用读取 */
    public static void register() {
        // 衰竭域（AkaishiDecayBlocks）须在 init() 中先于本方法注册，故此处只转发不注册
        AkaishiFoundationBlocks.register();
        AkaishiLogisticsBlocks.register();
        AkaishiTransgeneBlocks.register();

        // —— 基础域 ——
        RAW_CHISHI_BLOCK = AkaishiFoundationBlocks.RAW_CHISHI_BLOCK;
        CHISHI_PURIFIER = AkaishiFoundationBlocks.CHISHI_PURIFIER;
        CHISHI_ADVANCED_PURIFIER = AkaishiFoundationBlocks.CHISHI_ADVANCED_PURIFIER;
        CHISHI_ESSENCE_BLOCK = AkaishiFoundationBlocks.CHISHI_ESSENCE_BLOCK;
        CHISHI_EQUIPMENT_FORGER = AkaishiFoundationBlocks.CHISHI_EQUIPMENT_FORGER;
        CHISHI_UPGRADE_STATION = AkaishiFoundationBlocks.CHISHI_UPGRADE_STATION;
        CHISHI_LIFE_FUSION_ANVIL = AkaishiFoundationBlocks.CHISHI_LIFE_FUSION_ANVIL;
        CHISHI_FUEL_CANNER = AkaishiFoundationBlocks.CHISHI_FUEL_CANNER;
        CHISHI_FUEL_MIXER = AkaishiFoundationBlocks.CHISHI_FUEL_MIXER;
        CHISHI_LIFE_ACTIVATOR = AkaishiFoundationBlocks.CHISHI_LIFE_ACTIVATOR;
        CHISHI_LIFE_CENTRIFUGE = AkaishiFoundationBlocks.CHISHI_LIFE_CENTRIFUGE;
        CHISHI_ITEM_RECONSTRUCTOR = AkaishiFoundationBlocks.CHISHI_ITEM_RECONSTRUCTOR;
        CHISHI_PLANT_CULTIVATOR = AkaishiFoundationBlocks.CHISHI_PLANT_CULTIVATOR;
        CHISHI_COMPRESSOR = AkaishiFoundationBlocks.CHISHI_COMPRESSOR;
        CHISHI_PULVERIZER = AkaishiFoundationBlocks.CHISHI_PULVERIZER;
        CHISHI_TRANSFORMER = AkaishiFoundationBlocks.CHISHI_TRANSFORMER;
        CHISHI_ACTIVATED_FRACTIONATOR = AkaishiFoundationBlocks.CHISHI_ACTIVATED_FRACTIONATOR;
        CHISHI_PLASMA_FILLER = AkaishiFoundationBlocks.CHISHI_PLASMA_FILLER;

        // —— 物流域 ——
        CHISHI_ITEM_PIPE = AkaishiLogisticsBlocks.CHISHI_ITEM_PIPE;
        CHISHI_ITEM_PIPE_ADVANCED = AkaishiLogisticsBlocks.CHISHI_ITEM_PIPE_ADVANCED;
        CHISHI_ITEM_PIPE_ELITE = AkaishiLogisticsBlocks.CHISHI_ITEM_PIPE_ELITE;
        CHISHI_ITEM_PIPE_ULTIMATE = AkaishiLogisticsBlocks.CHISHI_ITEM_PIPE_ULTIMATE;
        CHISHI_FLUID_PIPE = AkaishiLogisticsBlocks.CHISHI_FLUID_PIPE;
        CHISHI_EXHAUSTED_PIPE = AkaishiLogisticsBlocks.CHISHI_EXHAUSTED_PIPE;
        CHISHI_MULTI_FLUID_WASTE_PIPE = AkaishiLogisticsBlocks.CHISHI_MULTI_FLUID_WASTE_PIPE;
        CHISHI_PLASMA_PIPE = AkaishiLogisticsBlocks.CHISHI_PLASMA_PIPE;
        CHISHI_FLUID_TANK_BASIC = AkaishiLogisticsBlocks.CHISHI_FLUID_TANK_BASIC;
        CHISHI_FLUID_TANK_ADVANCED = AkaishiLogisticsBlocks.CHISHI_FLUID_TANK_ADVANCED;
        CHISHI_FLUID_TANK_SUPER = AkaishiLogisticsBlocks.CHISHI_FLUID_TANK_SUPER;
        CHISHI_PLASMA_TANK = AkaishiLogisticsBlocks.CHISHI_PLASMA_TANK;

        // —— 转基因域 ——
        CHISHI_WITHER_ROOT = AkaishiTransgeneBlocks.CHISHI_WITHER_ROOT;
        CHISHI_WITHER_STEM = AkaishiTransgeneBlocks.CHISHI_WITHER_STEM;

        // —— 衰竭域（注册在 init() 中先于本方法完成）——
        CHISHI_DECAY_PURIFIER = AkaishiDecayBlocks.CHISHI_DECAY_PURIFIER;
        CHISHI_DECAY_SOIL = AkaishiDecayBlocks.CHISHI_DECAY_SOIL;
        CHISHI_DECAY_LOG = AkaishiDecayBlocks.CHISHI_DECAY_LOG;
    }

    /** 获取对应组合定义的方块（转发基础域，注册完成后可用） */
    public static Block get(AkaishiOreDef def) {
        return AkaishiFoundationBlocks.get(def);
    }
}
