package com.example.akaishi.menu;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/**
 * 芯片界面里的「返回矩阵终端」页签。
 * <p>
 * 矩阵终端的左列把玩家送到芯片自己的界面后必须给一条路回来（否则只能关界面再右键矩阵）。
 * 这里刻意做成<b>贴在面板左侧外的一枚页签</b>（与矩阵界面的左列对称）：
 * 落在面板外 ⇒ 不与任何芯片界面的既有版式抢位置，三个芯片界面共用同一套几何。
 * <p>
 * 只做"按钮 + 一次发包"；坐标由客户端在跳转时记下（{@link #mark}），
 * <b>服务端不信任它</b>，会校验该处确实是矩阵终端方块且玩家在有效距离内。
 */
public final class MiniMatrixReturn {

    /** 页签尺寸与位置（面板左侧外 2px，与面板顶部页签行同高） */
    private static final int TAB_W = 44;
    private static final int TAB_H = 14;
    private static final int TAB_GAP = 2;
    private static final int TAB_Y = 16;

    /** 客户端会话记忆：跳转来源的矩阵坐标；非"从矩阵跳来"状态时为 null */
    @Nullable
    private static BlockPos origin;

    private MiniMatrixReturn() {
    }

    /** 从矩阵跳转时记下来源（null 表示客户端不知道来源，则不显示返回页签） */
    public static void mark(@Nullable BlockPos matrixPos) {
        origin = matrixPos;
    }

    /** 回到矩阵界面即清空：避免离开后再右键别的芯片时冒出"其实还能用但对不上语境"的返回页签 */
    public static void clear() {
        origin = null;
    }

    /** 面板左侧外那枚页签的左上角 x（面板左边界已知时调用） */
    private static int tabX(int leftPos) {
        return leftPos - TAB_GAP - TAB_W;
    }

    /** 画返回页签（无跳转来源则什么都不画：不做点了没反应的按钮） */
    public static void render(GuiGraphics gui, Font font, int leftPos, int topPos) {
        if (origin == null) {
            return;
        }
        GuiWidgets.buttonText(gui, font, tabX(leftPos), topPos + TAB_Y, TAB_W, TAB_H,
                Component.translatable("gui.akaishi.matrix.back"), true);
    }

    /**
     * 点击处理：命中页签则发起返回并清空记忆（避免重复发包）。
     *
     * @param containerId 当前菜单 id，仅供服务端做常规一致性口径
     * @return 是否已消费这次点击
     */
    public static boolean mouseClicked(double mouseX, double mouseY, int button, int leftPos, int topPos,
            int containerId) {
        if (button != 0 || origin == null) {
            return false;
        }
        int x = tabX(leftPos);
        if (mouseX < x || mouseX >= x + TAB_W || mouseY < topPos + TAB_Y || mouseY >= topPos + TAB_Y + TAB_H) {
            return false;
        }
        AkaishiMatrixCraftSync.sendAction(containerId, AkaishiMatrixCraftSync.ACTION_BACK_TO_MATRIX,
                "", ItemStack.EMPTY, ItemStack.EMPTY, origin);
        clear();
        return true;
    }
}
