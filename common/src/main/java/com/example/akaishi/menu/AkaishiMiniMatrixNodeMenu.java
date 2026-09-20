package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiMiniMatrixNetworkNodeBlockEntity;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/**
 * 网络节点界面：只读展示「绑定终端 / 归属者 / 子场域」，并提供<b>本节点独立</b>的节点屏障开关。
 * <p>
 * <b>为什么要有这个界面</b>（用户口径）：节点此前是"纯结构件 + 无界面"，它的申领状态只能靠
 * 方块模型的通电款与场域屏障间接看出，玩家无法确认"到底绑给了谁"；而屏障本身也没有关闭手段
 * （释放时若区块未加载还会留下视觉残留）。故补一个 176×112 的小面板：三行事实 + 一个开关。
 * <p>
 * <b>无任何槽位</b>：节点不需要搬物品，故不带背包区（空菜单在 {@code AbstractContainerScreen} 下正常渲染）。
 * 绑定关系由矩阵终端的申领决定，此处<b>只读</b>。
 */
public class AkaishiMiniMatrixNodeMenu extends AbstractContainerMenu {

    /** 面板尺寸（与设计稿一致：176×112，无背包区） */
    public static final int PANEL_W = 176;
    public static final int PANEL_H = 112;
    /** 内容左右边界（值一律右对齐到 RIGHT，并按可用宽度截断） */
    public static final int CONTENT_LEFT = 8;
    public static final int CONTENT_RIGHT = 168;
    /** 状态行（右上角） */
    public static final int STATE_Y = 5;
    /** 三行只读事实 */
    public static final int ROW_BOUND_Y = 26;
    public static final int ROW_OWNER_Y = 38;
    public static final int ROW_FIELD_Y = 50;
    /** 分隔线 */
    public static final int DIVIDER_Y = 62;
    /** 开关行（本界面的焦点控件）与开关按钮 */
    public static final int TOGGLE_Y = 68;
    public static final int PILL_W = 54;
    public static final int PILL_H = 14;
    public static final int PILL_X = CONTENT_RIGHT - PILL_W;
    /** 只读说明 */
    public static final int HINT_Y = 88;

    /** 服务端：节点方块实体；客户端：同一坐标上的客户端实例（数据由同步标签填充） */
    @Nullable
    private final AkaishiMiniMatrixNetworkNodeBlockEntity node;

    public AkaishiMiniMatrixNodeMenu(int id, Inventory inv, @Nullable AkaishiMiniMatrixNetworkNodeBlockEntity node) {
        super(ModMenus.CHISHI_MINI_MATRIX_NODE.get(), id);
        this.node = node;
    }

    /** BE 缺失（跨维度/距离过远）时的空白兜底：没有槽位，不存在索引错位 */
    public static AkaishiMiniMatrixNodeMenu empty(int id, Inventory inv) {
        return new AkaishiMiniMatrixNodeMenu(id, inv, null);
    }

    @Nullable
    public AkaishiMiniMatrixNetworkNodeBlockEntity node() {
        return node;
    }

    @Override
    public boolean stillValid(Player player) {
        // 与矩阵终端同口径：距离 + "那个坐标上仍是同一个方块实体"（拆掉方块后 BE 只是 setRemoved）
        return node != null && !node.isRemoved()
                && player.level().getBlockEntity(node.getBlockPos()) == node
                && player.distanceToSqr(node.getBlockPos().getCenter()) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // 本界面没有任何槽位
    }
}
