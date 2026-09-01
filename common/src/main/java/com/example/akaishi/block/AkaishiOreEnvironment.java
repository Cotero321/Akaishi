package com.example.akaishi.block;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

/**
 * 赤石矿生成环境（维度/地层）。
 * 决定替换的目标方块（RuleTest）与生成高度范围。
 */
public enum AkaishiOreEnvironment {
    OVERWORLD("", new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES), 0, 64),
    DEEPSLATE("deepslate_", new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES), -64, 0),
    NETHER("nether_", new TagMatchTest(BlockTags.NETHER_CARVER_REPLACEABLES), 0, 128),
    END("end_", new BlockMatchTest(Blocks.END_STONE), 0, 128);

    /** 方块 id 前缀，如 deepslate_akaishi_ore_low */
    private final String idPrefix;
    private final RuleTest target;
    private final int minY;
    private final int maxY;

    AkaishiOreEnvironment(String idPrefix, RuleTest target, int minY, int maxY) {
        this.idPrefix = idPrefix;
        this.target = target;
        this.minY = minY;
        this.maxY = maxY;
    }

    public String idPrefix() {
        return idPrefix;
    }

    public RuleTest target() {
        return target;
    }

    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
    }
}
