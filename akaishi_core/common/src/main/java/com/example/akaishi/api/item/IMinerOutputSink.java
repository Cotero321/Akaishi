package com.example.akaishi.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * 矿机产物接收端接口：转口 / 物品输出口等可接收控制器推送挖矿产物的方块实体实现。
 * 控制器推送产物时遍历结构端口并对实现本接口的端口执行 {@link #receivePartial}。
 */
public interface IMinerOutputSink {

    /**
     * 尝试接收一叠产物，支持部分合并到同种/空槽。
     *
     * @param incoming 控制器推送的产物
     * @return 未能放入的剩余部分（空表示全部接收）
     */
    ItemStack receivePartial(ItemStack incoming);

    /**
     * 灌注顺序（<b>数值小的先灌</b>）：产物按此顺序依次尝试，灌满一个再给下一个。
     * <p>
     * 必须由接收端自己声明而不是靠结构扫描顺序 —— 扫描顺序由坐标决定，
     * 顶层中心的转口可能排在立柱输出口之前，那样产物会被转口全部吃掉、物品输出口一直空着。
     * 专属产物输出口用默认值 0（优先），兜底/暂存型的口（转口）给大值排在最后。
     * <p>
     * 排在后面的口若还实现 {@link net.minecraft.world.Container}，其存量会被控制器<b>再平衡</b>
     * 回填给前面仍有空位的口（单向搬迁，物料守恒）。
     */
    default int fillOrder() {
        return 0;
    }
}
