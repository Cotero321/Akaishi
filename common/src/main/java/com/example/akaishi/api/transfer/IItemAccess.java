package com.example.akaishi.api.transfer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;

/**
 * 相邻容器物品访问（loader 无关）。
 * <p>
 * 储存无线输入/输出口需要<b>读写别人的容器</b>（漏斗、MEK 管道、AE2/RS 等），
 * 而 common 模块编译期拿不到加载器 API，所以这里只定接口，实现由各加载器提供：
 * <ul>
 *   <li>forge：{@code ForgeItemAccess}（ForgeCapabilities.ITEM_HANDLER，退回 InvWrapper）；</li>
 *   <li>未注册任何实现时由 {@link ItemAccessHolder} 的原版 {@code Container} 兜底（箱子/漏斗/多数简单机器）。</li>
 * </ul>
 * 一律是<b>复制语义</b>：调用方拿到的堆与容器内容互不共享引用，避免改动泄漏。
 */
public interface IItemAccess {

    /**
     * 往相邻容器塞入。
     *
     * @return 未能塞入的余量（空 = 全部塞入）
     */
    ItemStack insert(Level level, BlockPos pos, Direction side, ItemStack stack);

    /**
     * 从相邻容器取出一批（返回值已从容器扣除）。
     *
     * @param maxCount 最多取多少件
     * @param filter   只取满足条件的堆（null = 不过滤）
     * @return 实际取出的堆（空 = 无可取）
     */
    ItemStack extract(Level level, BlockPos pos, Direction side, int maxCount, Predicate<ItemStack> filter);

    /**
     * <b>零副作用</b>预检：这一堆最多能被相邻容器接收多少件。
     * <p>
     * 存在的意义是"按实际塞得进的量计费"：输出口若按"从终端抽出的量"扣费，
     * 一旦对面满仓、物品又整批塞回终端，就会白扣一笔 —— 这正是本方法要堵的窗口。
     * 实现必须是纯查询（不得改动容器、不得触发 {@code setChanged()}）。
     *
     * @return 可接收件数（0 = 完全塞不进；不会超过 {@code stack} 的件数）
     */
    int acceptable(Level level, BlockPos pos, Direction side, ItemStack stack);

    /**
     * <b>零副作用</b>能力判定：这个位置有没有可读写的物品容器。
     * <p>与 {@link #acceptable} 的分工：那个回答"能塞进多少"（<b>满仓时为 0</b>），
     * 本方法回答"有没有容器"—— 用于<b>识别</b>第三方机器（装满的机器同样是机器，
     * 用 {@code acceptable} 当识别判据会把满仓机器判成"不存在"）。
     */
    boolean hasItemCapability(Level level, BlockPos pos, Direction side);
}
