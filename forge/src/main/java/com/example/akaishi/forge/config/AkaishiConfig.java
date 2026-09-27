package com.example.akaishi.forge.config;

import com.example.akaishi.forge.config.specs.CoreMachineSpecs;
import com.example.akaishi.forge.config.specs.DecayFusionSpecs;
import com.example.akaishi.forge.config.specs.OrganSpecs;
import com.example.akaishi.forge.config.specs.MechSpecs;
import com.example.akaishi.forge.config.specs.SanitySpecs;
import com.example.akaishi.forge.config.specs.CurioSpecs;
import com.example.akaishi.forge.config.specs.LifeMachineSpecs;
import com.example.akaishi.forge.config.specs.BufferValueSpecs;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Forge 原生配置文件（common.toml）。所有可调数值经 ForgeConfigSpec 定义，
 * 通过 {@link AkaishiConfigSync} 在加载/重载时同步到 common 的 {@link com.example.akaishi.config.ModConfig}。
 * Forge 自动生成原生配置界面（Mods 列表 → Config）。
 * <p>字段与各节构建逻辑按域拆分至 config.specs 包；本类保留唯一 SPEC 与构建顺序编排——
 * 下列 build 调用顺序与拆分前 static 块的 push 顺序逐位一致（决定 common.toml 分节顺序），不得调换。
 */
public final class AkaishiConfig {

    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        // 顺序 = 原 common.toml 分节顺序，不得调换
        CoreMachineSpecs.build(b);
        DecayFusionSpecs.build(b);
        OrganSpecs.build(b);
        MechSpecs.build(b);
        SanitySpecs.build(b);
        CurioSpecs.build(b);
        LifeMachineSpecs.build(b);
        BufferValueSpecs.build(b);
        SPEC = b.build();
    }

    private AkaishiConfig() {
    }
}
