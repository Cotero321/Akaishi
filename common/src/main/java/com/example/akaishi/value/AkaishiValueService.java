package com.example.akaishi.value;

import java.util.function.ToDoubleFunction;

import com.example.akaishi.api.value.IValueService;
import com.example.akaishi.api.value.ValueServices;

import dev.architectury.fluid.FluidStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

/**
 * 价值分服务默认实现：把静态表、估值内核与快照缓存串成 {@link IValueService}。
 *
 * <p>降级策略：取不到服务端快照（客户端主菜单、数据包未加载）时退回自身分，
 * 保证调用方任何时候都能拿到非零可用值，而不是静默 0。
 */
public final class AkaishiValueService implements IValueService {

    private static final AkaishiValueService INSTANCE = new AkaishiValueService();

    private static final ToDoubleFunction<Item> NO_LOOT = item -> 0.0;

    private volatile ValueTables tables = ValueTables.fromConfig();
    private volatile ToDoubleFunction<Item> lootResolver = NO_LOOT;

    private AkaishiValueService() {
    }

    public static AkaishiValueService instance() {
        return INSTANCE;
    }

    /** 注册为全局服务；重复调用安全 */
    public static void install() {
        ValueServices.register(INSTANCE);
    }

    /** 掉落来源解析器注入点（掉落索引就绪后由平台层调用） */
    public void setLootResolver(ToDoubleFunction<Item> resolver) {
        this.lootResolver = resolver == null ? NO_LOOT : resolver;
    }

    /** 配置热重载：重建静态表并失效快照，无需重启 */
    public void reload() {
        this.tables = ValueTables.fromConfig();
        ValueCache.invalidate();
    }

    public ValueTables tables() {
        return tables;
    }

    /** 当前估值快照；无服务端时返回 null */
    public ValueCache.Snapshot snapshot() {
        MinecraftServer server = ValuePlatform.server();
        if (server == null) {
            return null;
        }
        return ValueCache.get(server.getRecipeManager(), () -> ValueKernel.buildSnapshot(
                server.getRecipeManager(), server.registryAccess(), tables, lootResolver));
    }

    @Override
    public double itemValue(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0.0;
        }
        return itemValue(stack.getItem());
    }

    @Override
    public double itemValue(Item item) {
        if (item == null || item == Items.AIR) {
            return 0.0;
        }
        ValueCache.Snapshot snapshot = snapshot();
        if (snapshot == null) {
            return ValueKernel.intrinsicScore(item, tables);
        }
        double value = snapshot.itemValue(item);
        // 快照未收录（异常配方集）时退回自身分，避免出现 0 价
        return value > 0.0 ? value : ValueKernel.intrinsicScore(item, tables);
    }

    @Override
    public double fluidValue(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0.0;
        }
        return fluidValue(stack.getFluid(), stack.getAmount());
    }

    /** 流体价值分（总量 = 每 mB 分 × mB） */
    public double fluidValue(Fluid fluid, long mb) {
        if (fluid == null || mb <= 0) {
            return 0.0;
        }
        ValueCache.Snapshot snapshot = snapshot();
        return snapshot == null ? 0.0 : snapshot.fluidValue(fluid, mb);
    }

    /** 造价分：亲和修正与产出数量按「无亲和、单产出」计，仅用于界面展示对比 */
    @Override
    public int craftingCost(Item item) {
        if (item == null || item == Items.AIR) {
            return 0;
        }
        ValueCache.Snapshot snapshot = snapshot();
        if (snapshot == null) {
            return (int) Math.round(Math.max(1.0, ValueKernel.intrinsicScore(item, tables)));
        }
        return ValueKernel.computeCost(item, snapshot, 0, 1);
    }

    @Override
    public int version() {
        return ValueCache.generation();
    }
}
