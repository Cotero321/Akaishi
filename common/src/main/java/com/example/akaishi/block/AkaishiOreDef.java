package com.example.akaishi.block;

/**
 * 赤石矿组合定义：浓度 × 环境 → 唯一方块 id。
 * 16 个方块（4 浓度 × 4 环境）均由本记录派生。
 */
public record AkaishiOreDef(AkaishiOreTier tier, AkaishiOreEnvironment env) {

    /** 方块注册 id，如 deepslate_akaishi_ore_perfect */
    public String id() {
        return env.idPrefix() + "akaishi_ore_" + tier.suffix();
    }
}
