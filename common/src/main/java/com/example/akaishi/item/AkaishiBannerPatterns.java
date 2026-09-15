package com.example.akaishi.item;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BannerPattern;

/**
 * 旗帜图案域注册：山羊头徽记。
 * <p>
 * 遮罩贴图放在 {@code textures/entity/banner/goat_skull.png}，会被原版
 * {@code minecraft:banner_patterns} 图集按目录自动收录，模组无需自建 atlases json。
 * 图案本身不含颜色，染色由织布机所选染料决定。
 */
public final class AkaishiBannerPatterns {

    /** 图案注册 id：同时决定贴图路径 entity/banner/&lt;id&gt;.png */
    public static final String GOAT_SKULL_ID = "goat_skull";

    /** NBT 哈希名：写入旗帜 Pattern 字段，须全注册表唯一（原版多为 2~3 字符缩写） */
    public static final String GOAT_SKULL_HASH = "gsk";

    /**
     * 织布机可选性标签：{@code BannerPatternItem} 据此把本图案暴露给织布机。
     * 对应数据包 {@code data/akaishi/tags/banner_pattern/pattern_item/goat_skull.json}。
     */
    public static final TagKey<BannerPattern> GOAT_SKULL_TAG = TagKey.create(
            Registries.BANNER_PATTERN,
            new ResourceLocation(AkaishiMod.MOD_ID, "pattern_item/" + GOAT_SKULL_ID));

    public static RegistrySupplier<BannerPattern> GOAT_SKULL;

    private AkaishiBannerPatterns() {
    }

    /** 由 {@link AkaishiMod#init()} 在注册表冻结前调用（须先于引用该图案的物品注册） */
    public static void register() {
        Registrar<BannerPattern> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BANNER_PATTERN);
        GOAT_SKULL = registrar.register(new ResourceLocation(AkaishiMod.MOD_ID, GOAT_SKULL_ID),
                () -> new BannerPattern(GOAT_SKULL_HASH));
    }
}
