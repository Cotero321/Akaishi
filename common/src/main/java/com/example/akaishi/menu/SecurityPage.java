package com.example.akaishi.menu;

import com.example.akaishi.api.security.AkaishiSecurityPermission;
import com.example.akaishi.item.AkaishiWirelessIdentityCardItem;
import com.example.akaishi.wireless.TerminalSecurity;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 终端「安全认证」页的共享实现（赤能源无线终端 / 生命无线终端 / 物品终端共用）。
 * <p>
 * 版式（相对面板原点，适配 176×198 小面板的 90px 内容区，玩家背包自 y=124 起）：
 * <pre>
 *   y=32  授权槽(8,32 18×18)   「登记」(30,34 64×12)   「移除」(98,34 64×12)
 *   y=50  归属者：Player1（左）                已启用/未启用（右对齐）
 *   y=60  列名：存 取 请 布 安（与勾选框列对齐）
 *   y=68  权限表：6 行 × 9px（行 0..4 = 身份，行 5 = 默认权限条目）
 *         每行 = 身份名（≤100px 截断）+ 5 个 8×8 勾选框（x=116 起，步进 10）
 * </pre>
 * 尺寸口径：行高 9px 恰好容纳 8px 字与 8×8 勾选框；6 行刚好落在 y=122（背包前）之内。
 * 勾选框含义由「列名单字 + 悬停提示」双重表达，不做表头长文字（放不下）。
 * <p>
 * 服务端动作（登记 / 移除 / 勾选 / 卡权限回写）也集中在此，各菜单的 C2S 入口只是转发。
 */
public final class SecurityPage {

    /** 视图数据源：由持有权限表快照的菜单实现 */
    public interface Source {
        /** 归属者名（未知为空串） */
        String securityOwnerName();

        /** 权限表条目（顺序与服务端一致） */
        List<AkaishiTerminalSecuritySync.Entry> securityEntries();

        /** 是否登记了默认权限条目 */
        boolean securityHasDefault();

        /** 默认权限条目掩码 */
        int securityDefaultPerms();

        /** 是否已启用安全（登记了任何条目） */
        boolean securityEnabled();
    }

    /** 权限校验回调：无线终端走注册表镜像（端口/便携终端读同一份），物品终端查自己持有的权威表 */
    public interface PermissionCheck {
        boolean test(Player player, AkaishiSecurityPermission perm);
    }

    // ===== 版式常量（三个终端共用同一套相对坐标） =====

    /** 授权槽坐标（安全页占用库区/搜索区，槽落在左上） */
    public static final int CARD_SLOT_X = 8;
    public static final int CARD_SLOT_Y = 32;
    /** 授权槽边长（原版槽位 18×18：16 物品区 + 双侧 1px 边框） */
    public static final int CARD_SLOT_SIZE = 18;

    public static final int BTN_Y = 34;
    public static final int BTN_W = 64;
    public static final int BTN_H = 12;
    public static final int REGISTER_X = 30;
    public static final int REMOVE_X = 98;

    /** 归属者 / 状态行（同一行：左归属者、右状态） */
    public static final int OWNER_Y = 50;
    /** 归属者文本可用宽度（其后留给右侧状态） */
    public static final int OWNER_W = 104;

    /** 列名（权限单字）行 */
    public static final int LEGEND_Y = 60;

    public static final int LIST_X = 8;
    public static final int LIST_Y = 68;
    public static final int ROW_H = 9;
    /** 身份名可用宽度（其后留给勾选框列） */
    public static final int NAME_W = 100;
    public static final int BOX_X = 116;
    public static final int BOX_STEP = 10;
    public static final int BOX_SIZE = 8;
    /** 权限表区域右边界（hover 行高亮用） */
    public static final int LIST_RIGHT = 168;

    /** 勾选框配色：已授予 = 绿芯，未授予 = 深灰空框 */
    private static final int BOX_BG = 0xFF7A7A7A;
    private static final int BOX_ON = 0xFF3F9B44;

    private SecurityPage() {
    }

    // ===== 客户端：绘制 =====

    /** 背景层：授权槽外框 + 两个按钮 + 列名 + 勾选框框体（文字由 {@link #renderLabels} 画） */
    public static void renderBg(GuiGraphics gui, int x, int y, Source source, int mouseY) {
        GuiWidgets.slotBox(gui, x + CARD_SLOT_X, y + CARD_SLOT_Y);
        GuiWidgets.button(gui, x + REGISTER_X, y + BTN_Y, BTN_W, BTN_H);
        GuiWidgets.button(gui, x + REMOVE_X, y + BTN_Y, BTN_W, BTN_H);
        int rows = source.securityEntries().size() + 1; // 玩家条目 + 默认条目
        for (int row = 0; row < rows; row++) {
            int rowY = y + LIST_Y + row * ROW_H;
            // 悬停行高亮：一眼看出「勾选框属于哪一行」（行高仅 9px，靠底色区分）
            if (mouseY >= rowY && mouseY < rowY + ROW_H) {
                gui.fill(x + LIST_X - 2, rowY, x + LIST_RIGHT, rowY + ROW_H, 0x30FFFFFF);
            }
            for (int i = 0; i < AkaishiSecurityPermission.values().length; i++) {
                int boxX = x + BOX_X + i * BOX_STEP;
                gui.fill(boxX, rowY, boxX + BOX_SIZE, rowY + BOX_SIZE, BOX_BG);
                if (granted(source, row, i)) {
                    gui.fill(boxX + 2, rowY + 2, boxX + BOX_SIZE - 2, rowY + BOX_SIZE - 2, BOX_ON);
                }
            }
        }
    }

    /** 文字层（调用方已 translate(leftPos, topPos)，此处坐标为 GUI 相对坐标） */
    public static void renderLabels(GuiGraphics gui, Font font, int x, int y, Source source,
            int colorText, int colorDim, int colorGreen) {
        centerText(gui, font, Component.translatable("gui.akaishi.wireless.terminal.authorize"),
                x + REGISTER_X, y + BTN_Y, BTN_W, BTN_H, colorText);
        centerText(gui, font, Component.translatable("gui.akaishi.wireless.terminal.revoke"),
                x + REMOVE_X, y + BTN_Y, BTN_W, BTN_H, colorText);
        // 归属者：放置终端的人，恒全权限（不占权限表条目）
        String owner = source.securityOwnerName();
        gui.drawString(font, font.plainSubstrByWidth(
                        Component.translatable("gui.akaishi.security.owner_line",
                                owner == null || owner.isEmpty() ? "-" : owner).getString(), OWNER_W),
                x + 8, y + OWNER_Y, colorGreen, false);
        // 右上角状态：已启用 / 未启用
        Component state = Component.translatable(source.securityEnabled()
                ? "gui.akaishi.security.state_on" : "gui.akaishi.security.state_off");
        gui.drawString(font, state, x + LIST_RIGHT - font.width(state), y + OWNER_Y, colorDim, false);
        // 列名：权限单字，居中于各勾选框列之上
        AkaishiSecurityPermission[] perms = AkaishiSecurityPermission.values();
        for (int i = 0; i < perms.length; i++) {
            Component shortName = perms[i].shortName();
            gui.drawString(font, shortName,
                    x + BOX_X + i * BOX_STEP + (BOX_SIZE - font.width(shortName)) / 2, y + LEGEND_Y,
                    colorDim, false);
        }
        // 权限表：每行 = 身份名 + 5 个勾选框（框体在背景层画）
        int rowY = y + LIST_Y;
        for (AkaishiTerminalSecuritySync.Entry entry : source.securityEntries()) {
            gui.drawString(font, font.plainSubstrByWidth(entry.name(), NAME_W), x + LIST_X, rowY + 1,
                    colorText, false);
            rowY += ROW_H;
        }
        // 默认权限条目行（来自未绑定身份的卡；未登记也显示，便于直接配默认权限）
        gui.drawString(font, Component.translatable("gui.akaishi.security.default_short"),
                x + LIST_X, rowY + 1, source.securityHasDefault() ? colorText : colorDim, false);
    }

    /** 悬停提示：勾选框 → 单项权限说明；行内其它位置 → 身份 + 权限汇总；按钮 / 归属者 / 状态 → 说明 */
    public static void renderTooltip(GuiGraphics gui, Font font, int leftPos, int topPos,
            int mouseX, int mouseY, Source source) {
        int row = hoveredRow(source, topPos, mouseY);
        int index = hoveredIndex(leftPos, mouseX);
        if (row >= 0 && index >= 0) {
            AkaishiSecurityPermission perm = AkaishiSecurityPermission.values()[index];
            gui.renderComponentTooltip(font, List.of(perm.displayName(), perm.displayHint()), mouseX, mouseY);
            return;
        }
        // 行内任意位置（含身份名区域）都给提示：勾选框只有 8×8，若只挂它等于「整页没提示」
        if (row >= 0 && mouseX >= leftPos + LIST_X && mouseX < leftPos + LIST_RIGHT) {
            boolean player = row < source.securityEntries().size();
            Component name = player ? Component.literal(source.securityEntries().get(row).name())
                    : Component.translatable("gui.akaishi.security.default_entry");
            int mask = player ? source.securityEntries().get(row).perms() : source.securityDefaultPerms();
            gui.renderComponentTooltip(font, List.of(name,
                    Component.translatable("gui.akaishi.security.row_perms", permissionSummary(mask)),
                    Component.translatable("gui.akaishi.security.row_hint")), mouseX, mouseY);
            return;
        }
        if (isIn(leftPos + REGISTER_X, topPos + BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
            gui.renderComponentTooltip(font,
                    List.of(Component.translatable("gui.akaishi.wireless.terminal.auth_hint")), mouseX, mouseY);
        } else if (isIn(leftPos + REMOVE_X, topPos + BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
            gui.renderComponentTooltip(font,
                    List.of(Component.translatable("gui.akaishi.security.remove_hint")), mouseX, mouseY);
        } else if (isIn(leftPos + 8, topPos + OWNER_Y - 1, OWNER_W, 10, mouseX, mouseY)) {
            gui.renderComponentTooltip(font,
                    List.of(Component.translatable("gui.akaishi.security.owner_hint")), mouseX, mouseY);
        } else if (isIn(leftPos + LIST_RIGHT - 40, topPos + OWNER_Y - 1, 40, 10, mouseX, mouseY)) {
            // 状态文字：未启用 ⇒ 说明「全部放行」；已启用 ⇒ 说明勾选操作
            gui.renderComponentTooltip(font, List.of(Component.translatable(source.securityEnabled()
                    ? "gui.akaishi.security.hint" : "gui.akaishi.security.idle")), mouseX, mouseY);
        } else if (isIn(leftPos + CARD_SLOT_X, topPos + CARD_SLOT_Y, CARD_SLOT_SIZE, CARD_SLOT_SIZE,
                mouseX, mouseY)) {
            // 授权槽：空置时没有任何原版物品提示，必须自带说明（放卡后才由物品 tooltip 兜底）
            gui.renderComponentTooltip(font,
                    List.of(Component.translatable("gui.akaishi.security.card_slot_tip")), mouseX, mouseY);
        }
    }

    /** 权限掩码 → 已授予权限的缩写串（空掩码返回「无」） */
    private static Component permissionSummary(int mask) {
        AkaishiSecurityPermission[] values = AkaishiSecurityPermission.values();
        StringBuilder builder = new StringBuilder();
        for (AkaishiSecurityPermission perm : values) {
            if (perm.granted(mask)) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(perm.shortName().getString());
            }
        }
        return builder.length() == 0
                ? Component.translatable("gui.akaishi.security.perm_none")
                : Component.literal(builder.toString());
    }

    /** 点击处理：命中「登记 / 移除 / 勾选框」时下发 C2S 动作并返回 true */
    public static boolean mouseClicked(double mouseX, double mouseY, int leftPos, int topPos,
            Source source, int containerId) {
        if (isIn(leftPos + REGISTER_X, topPos + BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
            AkaishiTerminalSecuritySync.sendAction(containerId, AkaishiTerminalSecuritySync.ACTION_REGISTER, null, 0);
            return true;
        }
        if (isIn(leftPos + REMOVE_X, topPos + BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
            AkaishiTerminalSecuritySync.sendAction(containerId, AkaishiTerminalSecuritySync.ACTION_REMOVE, null, 0);
            return true;
        }
        int row = hoveredRow(source, topPos, mouseY);
        int index = hoveredIndex(leftPos, mouseX);
        if (row >= 0 && index >= 0) {
            // 按目标身份下发（而非行号）：权限表增删后行号会错位
            AkaishiTerminalSecuritySync.sendAction(containerId, AkaishiTerminalSecuritySync.ACTION_TOGGLE,
                    rowTarget(source, row), index);
            return true;
        }
        return false;
    }

    // ===== 服务端：动作 =====

    /** 本地口径判定：终端自己持有权威权限表时用（op 权限等级 4 放行） */
    public static boolean checkLocal(TerminalSecurity security, Player player, AkaishiSecurityPermission perm) {
        if (player == null) {
            return true;
        }
        if (player.hasPermissions(4)) {
            return true;
        }
        return security.check(player.getUUID(), perm);
    }

    /**
     * 执行一次安全页动作。三个动作统一要求 {@link AkaishiSecurityPermission#SECURITY} 权限
     * （归属者恒有、op 放行），失败/越权给动作栏提示，避免"点了没反应"。
     */
    public static void applyAction(TerminalSecurity security, PermissionCheck check, Player actor,
            ItemStack slotCard, byte action, UUID target, int permOrdinal) {
        if (security == null) {
            return;
        }
        if (check != null && !check.test(actor, AkaishiSecurityPermission.SECURITY)) {
            actor.displayClientMessage(Component.translatable("message.akaishi.security.denied"), true);
            return;
        }
        switch (action) {
            case AkaishiTerminalSecuritySync.ACTION_REGISTER -> register(security, actor, slotCard);
            case AkaishiTerminalSecuritySync.ACTION_REMOVE -> remove(security, actor, slotCard);
            case AkaishiTerminalSecuritySync.ACTION_TOGGLE -> toggle(security, slotCard, target, permOrdinal);
            default -> {
            }
        }
    }

    /** 登记：把槽里的身份卡写入权限表（卡未绑定身份 ⇒ 写默认权限条目） */
    private static void register(TerminalSecurity security, Player actor, ItemStack slotCard) {
        if (!(slotCard.getItem() instanceof AkaishiWirelessIdentityCardItem)) {
            actor.displayClientMessage(Component.translatable("message.akaishi.security.need_card"), true);
            return;
        }
        // 卡号只在服务端生成（双端随机会产生不一致卡号）
        AkaishiWirelessIdentityCardItem.ensureUuid(slotCard);
        UUID identity = AkaishiWirelessIdentityCardItem.playerOf(slotCard);
        if (security.register(slotCard)) {
            actor.displayClientMessage(Component.translatable("message.akaishi.security.registered",
                    identity == null ? Component.translatable("gui.akaishi.security.default_entry")
                            : Component.literal(security.displayName(identity))), true);
        } else {
            actor.displayClientMessage(Component.translatable("message.akaishi.security.register_failed"), true);
        }
    }

    /** 移除：按槽里那张卡的身份移除登记（未绑定身份的卡 ⇒ 移除默认条目） */
    private static void remove(TerminalSecurity security, Player actor, ItemStack slotCard) {
        if (!(slotCard.getItem() instanceof AkaishiWirelessIdentityCardItem)) {
            actor.displayClientMessage(Component.translatable("message.akaishi.security.need_card"), true);
            return;
        }
        UUID identity = AkaishiWirelessIdentityCardItem.playerOf(slotCard);
        boolean removed = identity == null ? security.clearDefault() : security.removePlayer(identity);
        actor.displayClientMessage(Component.translatable(removed
                ? "message.akaishi.security.removed" : "message.akaishi.security.remove_missing"), true);
    }

    /**
     * 勾选权限位：按目标身份操作（target == null ⇒ 默认权限条目），点一次取反一次；
     * 目标已被移除时静默丢弃（客户端列表可能落后一 tick）。
     */
    private static void toggle(TerminalSecurity security, ItemStack slotCard, UUID target, int permOrdinal) {
        AkaishiSecurityPermission[] values = AkaishiSecurityPermission.values();
        if (permOrdinal < 0 || permOrdinal >= values.length) {
            return;
        }
        if (target != null && !security.entries().containsKey(target)) {
            return;
        }
        AkaishiSecurityPermission perm = values[permOrdinal];
        security.toggle(target, perm, !perm.granted(security.bitsOf(target)));
        // 槽里的卡若是同一身份：把权限位同步写回卡，避免"卡上写着一套、终端存着另一套"
        if (slotCard.getItem() instanceof AkaishiWirelessIdentityCardItem
                && Objects.equals(AkaishiWirelessIdentityCardItem.playerOf(slotCard), target)) {
            AkaishiWirelessIdentityCardItem.setPerms(slotCard, security.bitsOf(target));
        }
    }

    // ===== 内部 =====

    /** 第 row 行是否含第 index 个权限（row == 条目数 ⇒ 默认权限条目） */
    private static boolean granted(Source source, int row, int index) {
        AkaishiSecurityPermission[] values = AkaishiSecurityPermission.values();
        if (index < 0 || index >= values.length) {
            return false;
        }
        int mask = row < source.securityEntries().size()
                ? source.securityEntries().get(row).perms() : source.securityDefaultPerms();
        return values[index].granted(mask);
    }

    /** 第 row 行对应的身份；默认条目行与越界行返回 null */
    private static UUID rowTarget(Source source, int row) {
        return row < source.securityEntries().size() ? source.securityEntries().get(row).player() : null;
    }

    /** 鼠标所在权限行；不在表内返回 -1（末行 = 默认权限条目） */
    private static int hoveredRow(Source source, int topPos, double mouseY) {
        int relY = (int) mouseY - topPos - LIST_Y;
        if (relY < 0) {
            return -1;
        }
        int row = relY / ROW_H;
        return row <= source.securityEntries().size() ? row : -1;
    }

    /** 鼠标所在勾选框列；不在框内返回 -1 */
    private static int hoveredIndex(int leftPos, double mouseX) {
        int relX = (int) mouseX - leftPos - BOX_X;
        if (relX < 0) {
            return -1;
        }
        int index = relX / BOX_STEP;
        return index < AkaishiSecurityPermission.values().length && relX % BOX_STEP < BOX_SIZE ? index : -1;
    }

    private static void centerText(GuiGraphics gui, Font font, Component text, int bx, int by, int bw, int bh,
            int color) {
        gui.drawString(font, text, bx + (bw - font.width(text)) / 2, by + (bh - 8) / 2 + 1, color, false);
    }

    private static boolean isIn(int x, int y, int w, int h, double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
