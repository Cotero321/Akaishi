package com.example.akaishi.api.value;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 价值分统一服务：为任意物品/流体（含第三方模组内容）给出「相对价值分」。
 *
 * <p>价值分是纯计算量，只用于统一存储库的<b>排序 / 统计 / 筛选 / 取物优先级</b>，
 * <b>不做任何经济兑换</b>，因此不存在刷分漏洞。
 *
 * <p>实现由各平台在模组初始化时注册（见 {@link ValueServices}）；
 * 未注册时 {@link ValueServices#get()} 返回零值兜底实现。
 */
public interface IValueService {

    /** 物品价值分：自身分 + 原料链（4 遍不动点）迭代结果，取大 */
    double itemValue(ItemStack stack);

    /** 物品价值分（忽略 NBT 差异，同物品同分） */
    double itemValue(Item item);

    /** 流体价值分：按 stack 的 mB 量折算（每 mB 上限由配置决定） */
    double fluidValue(FluidStack stack);

    /** 单次合成造价分（自身分 + 原料项×权重 + 功能项 + 掉落项），用于界面展示 */
    int craftingCost(Item item);

    /** 缓存版本号：配方/配置变动后自增，供外部判断展示值是否需要刷新 */
    int version();
}
