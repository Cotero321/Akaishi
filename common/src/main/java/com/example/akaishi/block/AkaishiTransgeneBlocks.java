package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * 转基因域方块注册：转基因植物（凋零藤 / 烈焰花）的作物方块。
 * 根/茎、花株/花冠为纯植物方块（无物品形态），由转基因植物种子（见 item 域）右键种植生成根，
 * 根随机刻长茎、成熟顶端可收获凋零果（烈焰花体系同理：花株成株长花冠、盛开花冠收获烈焰花瓣）。
 * 与 {@code AkaishiTransgeneItems} 同属转基因工厂作物体系。
 * <p>
 * 从 ModBlocks 拆分出的域注册类。所有静态字段显式初始化为 null，由 {@link #register()}
 * 在 {@link AkaishiMod#init()} 阶段填充；任何消费方都须在 register() 之后访问。
 */
public final class AkaishiTransgeneBlocks {

    /** 凋零藤根：整株第 1 格（种子种下/挖掘产出种子），随机刻长出茎 */
    public static RegistrySupplier<Block> CHISHI_WITHER_ROOT = null;
    /** 凋零藤茎：整株第 2/3 格（无物品、只能由根长出），成熟顶端可收凋零果 */
    public static RegistrySupplier<Block> CHISHI_WITHER_STEM = null;
    /** 烈焰花株：整株第 1 格（种子仅可种于灵魂沙），成株后随机刻长出花冠 */
    public static RegistrySupplier<Block> CHISHI_BLAZE_FLOWER_ROOT = null;
    /** 烈焰花冠：顶端开花格（无物品、只能由成株花株长出），盛开可收烈焰花瓣 */
    public static RegistrySupplier<Block> CHISHI_BLAZE_BLOOM = null;
    /** 咒怨垂蔓根：整株第 1 格（吊挂在方块底面，挖掘产出种子），随机刻向下长出茎 */
    public static RegistrySupplier<Block> CHISHI_CURSE_VINE_ROOT = null;
    /** 咒怨垂蔓茎：整株第 2~5 格（无物品、只能由根长出），最底端成熟可收咒怨花 */
    public static RegistrySupplier<Block> CHISHI_CURSE_VINE_STEM = null;

    private AkaishiTransgeneBlocks() {
    }

    /** 注册全部转基因植物方块（由 ModBlocks 门面在 AkaishiMod.init 阶段统一调用） */
    public static void register() {
        Registrar<Block> blockRegistrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);
        // 凋零藤根/茎：纯植物方块（无物品），种子种植生成根、根随机刻长茎
        CHISHI_WITHER_ROOT = blockRegistrar.register(
                new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_wither_root"), AkaishiWitherRootBlock::new);
        CHISHI_WITHER_STEM = blockRegistrar.register(
                new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_wither_stem"), AkaishiWitherStemBlock::new);
        // 烈焰花株/花冠：纯植物方块（无物品），种子种下生成花株、成株花株随机刻长出花冠
        CHISHI_BLAZE_FLOWER_ROOT = blockRegistrar.register(
                new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_blaze_flower_root"), AkaishiBlazeFlowerRootBlock::new);
        CHISHI_BLAZE_BLOOM = blockRegistrar.register(
                new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_blaze_bloom"), AkaishiBlazeBloomBlock::new);
        // 咒怨垂蔓根/茎：纯植物方块（无物品），种子种在方块底面生成根、根随机刻逐节向下垂挂抽蔓（最长 5 格）
        CHISHI_CURSE_VINE_ROOT = blockRegistrar.register(
                new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_curse_vine_root"), AkaishiCurseVineRootBlock::new);
        CHISHI_CURSE_VINE_STEM = blockRegistrar.register(
                new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_curse_vine_stem"), AkaishiCurseVineStemBlock::new);
    }
}
