package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * 转基因域方块注册：转基因植物（凋零藤）的作物方块。
 * 根/茎为纯植物方块（无物品形态），由转基因植物种子（见 item 域）右键种植生成根，
 * 根随机刻长茎、成熟顶端可收获凝聚体。与 {@code AkaishiTransgeneItems} 同属
 * 转基因工厂-凋零藤作物体系。
 * <p>
 * 从 ModBlocks 拆分出的域注册类。所有静态字段显式初始化为 null，由 {@link #register()}
 * 在 {@link AkaishiMod#init()} 阶段填充；任何消费方都须在 register() 之后访问。
 */
public final class AkaishiTransgeneBlocks {

    /** 凋零藤根：整株第 1 格（种子种下/挖掘产出种子），随机刻长出茎 */
    public static RegistrySupplier<Block> CHISHI_WITHER_ROOT = null;
    /** 凋零藤茎：整株第 2/3 格（无物品、只能由根长出），成熟顶端可收凝聚体 */
    public static RegistrySupplier<Block> CHISHI_WITHER_STEM = null;

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
    }
}
