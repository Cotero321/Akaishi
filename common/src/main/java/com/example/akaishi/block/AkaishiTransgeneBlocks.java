package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * 转基因域方块注册：转基因植物（凋零藤 / 烈焰花 / 咒怨垂蔓 / 回响花 / 末影花）的作物方块。
 * 每株植物各自独立成类（照其参考的原版植物建模，不做通用抽象）：
 * 回响花=紫颂（茎→顶花）、末影花=杜鹃花丛（单丛）；均为纯植物方块（无物品形态），
 * 由对应种子（见 item 域）右键种植生成。与 {@code AkaishiTransgeneItems} 同属转基因工厂作物体系。
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
    /** 回响花茎：参考紫颂植株，第 1 格（种子仅可种于幽匿块），随机刻在顶端长出顶花；支撑消失自灭 */
    public static RegistrySupplier<Block> CHISHI_ECHO_STEM = null;
    /** 回响花顶花：参考紫颂花，顶端第 2 格（无物品、只能由茎长出），有 age 成熟度，盛开可收幽匿果 */
    public static RegistrySupplier<Block> CHISHI_ECHO_FLOWER = null;
    /** 末影花丛：参考杜鹃花丛，单块丛状（种子仅可种于末地石），成熟可收末影果 */
    public static RegistrySupplier<Block> CHISHI_ENDER_BUSH = null;

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
        // 回响花茎/顶花：参考紫颂（茎→顶花两格），种子仅可种于幽匿块，盛开顶花可收幽匿果
        CHISHI_ECHO_STEM = blockRegistrar.register(
                new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_echo_stem"), AkaishiEchoStemBlock::new);
        CHISHI_ECHO_FLOWER = blockRegistrar.register(
                new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_echo_flower"), AkaishiEchoFlowerBlock::new);
        // 末影花丛：参考杜鹃花丛（单块丛状），种子仅可种于末地石，成熟可收末影果
        CHISHI_ENDER_BUSH = blockRegistrar.register(
                new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_ender_bush"), AkaishiEnderBushBlock::new);
    }
}
