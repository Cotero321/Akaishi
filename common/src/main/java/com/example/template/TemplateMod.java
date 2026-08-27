package com.example.template;

import com.example.template.api.energy.EnergyTypeRegistry;
import com.example.template.block.ModBlocks;
import com.example.template.block.entity.ModBlockEntities;
import com.example.template.energy.ChishiEnergyType;
import com.example.template.item.ModCreativeTabs;
import com.example.template.item.ModItems;
import com.example.template.menu.ModMenus;

/**
 * 模组通用入口，承载跨平台共享的初始化逻辑。
 */
public final class TemplateMod {
    /** 模组 ID，需与 mods.toml 中的 modId 保持一致 */
    public static final String MOD_ID = "template_mod";

    /** 通用初始化入口，由各平台加载器的入口类调用 */
    public static void init() {
        EnergyTypeRegistry.register(ChishiEnergyType.INSTANCE);
        ModItems.register();
        ModBlocks.register();
        ModBlockEntities.register();
        ModCreativeTabs.register();
        ModMenus.register();
    }
}
