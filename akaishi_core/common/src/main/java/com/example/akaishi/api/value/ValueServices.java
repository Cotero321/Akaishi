package com.example.akaishi.api.value;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 价值分服务注册中心（接口隔离 + 依赖倒置）：
 * 平台层注册具体实现，业务层只依赖 {@link IValueService} 抽象。
 */
public final class ValueServices {

    /** 未注册时的零值兜底，保证调用方永远拿到非 null */
    private static final IValueService FALLBACK = new IValueService() {
        @Override
        public double itemValue(ItemStack stack) {
            return 0.0;
        }

        @Override
        public double itemValue(Item item) {
            return 0.0;
        }

        @Override
        public double fluidValue(FluidStack stack) {
            return 0.0;
        }

        @Override
        public int craftingCost(Item item) {
            return 0;
        }

        @Override
        public int version() {
            return 0;
        }
    };

    private static volatile IValueService instance;

    private ValueServices() {
    }

    /** 注册实现（重复注册以最后一次为准，便于配置热重载时替换） */
    public static void register(IValueService service) {
        if (service != null) {
            instance = service;
        }
    }

    /** 取当前实现；未注册时返回零值兜底 */
    public static IValueService get() {
        IValueService s = instance;
        return s != null ? s : FALLBACK;
    }
}
