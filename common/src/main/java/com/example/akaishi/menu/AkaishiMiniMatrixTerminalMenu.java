package com.example.akaishi.menu;

import com.example.akaishi.api.storage.IItemTerminalHost;
import com.example.akaishi.block.AkaishiMiniMatrixUpgradeType;
import com.example.akaishi.block.entity.AkaishiMiniMatrixTerminalBlockEntity;
import com.example.akaishi.block.entity.MiniatureTerminalBlockEntity;
import com.example.akaishi.craft.CraftLibrary;
import com.example.akaishi.craft.CraftReadiness;
import com.example.akaishi.craft.VirtualCraftPlanner;
import com.example.akaishi.craft.VirtualCraftTask;
import com.example.akaishi.item.AkaishiWirelessIdentityCardItem;
import com.example.akaishi.wireless.TerminalSecurity;

import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeManager;

import dev.architectury.registry.menu.MenuRegistry;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 微缩矩阵终端菜单：四页互斥（芯片列表 / 总览 / 加工 / 安全认证）。
 * <p>
 * 槽位（服务端权威）：0 = 安全页授权槽（身份卡，即需求里的「主卡」，只进不出），
 * 1..36 = 玩家背包 3 行 + 快捷栏。<b>没有升级槽</b> ——
 * 升级件是装在结构<b>内腔</b>的方块（同无线终端族的内腔组件范式），
 * 界面只读展示控制器扫描出的数量，见 {@link AkaishiMiniMatrixTerminalBlockEntity#upgradeCount(int)}。
 * <p>
 * 数据通道：
 * <ul>
 *   <li>安全表 —— {@link AkaishiTerminalSecuritySync} 快照；</li>
 *   <li>成型状态 / 芯片读数 / 内腔升级数量 —— {@link AkaishiMiniMatrixSync} 快照。</li>
 * </ul>
 * <p>
 * <b>主卡统一入口</b>：安全表版本一变（本机登记 / 移除 / 勾选）就地调用
 * {@code applySecurityToChips()} 下发到全部绑定芯片，无需玩家额外操作。
 */
public class AkaishiMiniMatrixTerminalMenu extends AbstractContainerMenu
        implements SecurityPage.Source, AkaishiTerminalSecuritySync.Target, AkaishiMiniMatrixSync.Target,
        AkaishiMatrixCraftSync.Target {

    // ===== 页面（Screen 本地状态，此处仅定义常量） =====
    public static final int PAGE_CHIPS = 0;
    public static final int PAGE_OVERVIEW = 1;
    public static final int PAGE_CRAFT = 2;
    public static final int PAGE_SECURITY = 3;

    /** 安全页授权槽索引 */
    public static final int CARD_SLOT_INDEX = 0;
    /** 机器区槽数（仅授权槽 1 格），玩家背包紧随其后 */
    public static final int MACHINE_SLOT_END = 1;
    /** 玩家槽数（3 行背包 + 快捷栏） */
    private static final int PLAYER_SLOTS = 36;

    /** 面板尺寸：复用 176×198 终端贴图（与无线终端同规格，槽位图案天然对齐） */
    public static final int PANEL_W = 176;
    public static final int PANEL_H = 198;
    /** 背包行首 y / 快捷栏 y（与 198 高贴图槽位图案一致） */
    private static final int INV_Y = 124;
    private static final int HOTBAR_Y = 180;

    /** 视图快照节流间隔（tick）：芯片读数变化不必逐 tick 推送 */
    private static final int VIEW_INTERVAL = 10;
    /** 任务进度推送节流间隔（tick）：进度条不需要逐 tick 平滑 */
    private static final int TASK_INTERVAL = 10;
    /** 内腔升级种类数（快照定长） */
    private static final int UPGRADE_KINDS = AkaishiMiniMatrixUpgradeType.values().length;

    /** 服务端：矩阵方块实体（客户端为空） */
    @Nullable
    private final AkaishiMiniMatrixTerminalBlockEntity be;
    private final Player player;
    /** 授权槽是菜单内瞬时容器（非方块实体物品栏）：关闭界面时必须返还玩家 */
    private final SimpleContainer cardInv;
    /** 授权槽是否激活（由 Screen 按当前页设置；服务端实例保持 true 以不拦收放） */
    private boolean securitySlotActive = true;

    // ===== 安全页：权限表快照（客户端渲染只读） =====
    private String securityOwnerName = "";
    private boolean securityHasDefault;
    private int securityDefaultPerms;
    private List<AkaishiTerminalSecuritySync.Entry> securityEntries = List.of();
    private int securitySentRevision = -1;

    // ===== 芯片 / 升级页：视图快照（客户端渲染只读） =====
    private boolean formed;
    private int[] upgradeCounts = new int[UPGRADE_KINDS];
    /** 上一轮无线能源直供量（随视图快照同步） */
    private long craftPushedEnergy;
    /** 上一轮芯片间搬运量（客户端快照；赤能源芯片 → 储存终端，与场域直供是两条链路） */
    private long craftChipTransfer;
    /** 目录是否仍在分片构建（客户端镜像） */
    private boolean craftCatalogBuilding;
    /** 服务端：本菜单已请求目录且索引还在分片构建（每 tick 推进一步，建好补推） */
    private boolean indexBuildPending;
    private List<AkaishiMiniMatrixSync.ChipRow> chipRows = List.of();
    private int viewSentRevision = -1;
    private int viewCooldown;

    // ===== 加工页（客户端只读视图） =====
    /** 客户端：可合成物目录（服务端一次下发的全量目录，本地过滤后才是可见列表） */
    private List<ItemStack> craftCatalog = List.of();
    @Nullable
    private AkaishiMatrixCraftSync.PlanView craftPlan;
    @Nullable
    private AkaishiMatrixCraftSync.TaskView craftTaskView;
    /** 服务端：任务进度推送节流 */
    private int taskCooldown;

    public AkaishiMiniMatrixTerminalMenu(int id, Inventory inv,
            @Nullable AkaishiMiniMatrixTerminalBlockEntity be) {
        super(ModMenus.CHISHI_MINI_MATRIX_TERMINAL.get(), id);
        this.be = be;
        this.player = inv.player;
        this.cardInv = new SimpleContainer(1);
        addCardSlot();
        addPlayerSlots(inv);
    }

    /** 授权槽（主卡）：仅接受身份卡；非安全页时由 Screen 置为失活（不渲染、不可点） */
    private void addCardSlot() {
        this.addSlot(new Slot(cardInv, 0, SecurityPage.CARD_SLOT_X, SecurityPage.CARD_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof AkaishiWirelessIdentityCardItem;
            }

            @Override
            public boolean isActive() {
                return securitySlotActive;
            }
        });
    }

    private void addPlayerSlots(Inventory inv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, HOTBAR_Y));
        }
    }

    /** 授权槽激活开关（Screen 每帧按页同步） */
    public void setSecuritySlotActive(boolean active) {
        this.securitySlotActive = active;
    }

    // ===== 服务端：推送快照 =====

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (be == null || !(player instanceof ServerPlayer serverPlayer)
                || serverPlayer.containerMenu != this) {
            return;
        }
        TerminalSecurity security = be.security();
        if (security.revision() != securitySentRevision) {
            securitySentRevision = security.revision();
            // 主卡改动即刻下发全部绑定芯片（口径 5：一次登记，绑定的终端一同生效）
            be.applySecurityToChips();
            AkaishiTerminalSecuritySync.sendSnapshot(serverPlayer, this.containerId, security);
        }
        if (--viewCooldown <= 0) {
            viewCooldown = VIEW_INTERVAL;
            int revision = be.viewRevision();
            if (revision != viewSentRevision) {
                viewSentRevision = revision;
                AkaishiMiniMatrixSync.sendSnapshot(serverPlayer, this.containerId, be);
            }
        }
        // 加工目录的索引分片构建：每 tick 推进一步（不阻塞服务端）；建好即主动补推目录，
        // 玩家不用退出界面再进来才看得到列表
        if (indexBuildPending && be.getLevel() instanceof ServerLevel indexLevel) {
            if (VirtualCraftPlanner.stepBuild(indexLevel.getRecipeManager())) {
                sendCraftCatalog(serverPlayer, indexLevel);
            }
        }
        if (--taskCooldown <= 0) {
            taskCooldown = TASK_INTERVAL;
            VirtualCraftTask task = be.craftTask();
            AkaishiMatrixCraftSync.TaskView view = task == null ? null
                    : new AkaishiMatrixCraftSync.TaskView(AkaishiMatrixCraftSync.TASK_CRAFT,
                            task.plan().target(), task.remainingTicks(),
                            (int) Math.min(task.plan().totalTicks(), Integer.MAX_VALUE), 0, 0);
            AkaishiMatrixCraftSync.sendTask(serverPlayer, this.containerId, view);
        }
    }

    /** 服务端执行一次安全页动作（动作实现集中在 {@link SecurityPage}；本终端持权威表，故走本地判定） */
    @Override
    public void applySecurityAction(Player actor, byte action, UUID target, int permOrdinal) {
        if (be == null) {
            return;
        }
        SecurityPage.applyAction(be.security(),
                (checkPlayer, perm) -> SecurityPage.checkLocal(be.security(), checkPlayer, perm),
                actor, cardInv.getItem(0), action, target, permOrdinal);
    }

    // ===== 客户端：快照落地 =====

    @Override
    public void acceptSecurity(String ownerName, boolean hasDefault, int defaultPerms,
            List<AkaishiTerminalSecuritySync.Entry> entries) {
        this.securityOwnerName = ownerName == null ? "" : ownerName;
        this.securityHasDefault = hasDefault;
        this.securityDefaultPerms = defaultPerms;
        this.securityEntries = List.copyOf(entries);
    }

    @Override
    public void acceptMatrix(boolean formed, int[] upgradeCounts, long pushedEnergy, long chipTransfer,
            List<AkaishiMiniMatrixSync.ChipRow> chips) {
        this.formed = formed;
        this.upgradeCounts = upgradeCounts.clone();
        this.craftPushedEnergy = pushedEnergy;
        this.craftChipTransfer = chipTransfer;
        this.chipRows = List.copyOf(chips);
    }

    // ===== 加工页：服务端（动作落地） =====

    /** C2S 动作落地（{@code query} 已不使用：搜索改为客户端过滤，字段仅为协议兼容保留） */
    @Override
    public void acceptCraftAction(Player actor, byte action, String query, ItemStack target, ItemStack extra,
            BlockPos pos) {
        if (be == null || !(actor instanceof ServerPlayer serverPlayer)
                || !(actor.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        switch (action) {
            // 只回目录，不回"搜索结果"：过滤在客户端做（专用服务器没有客户端语言，中文/模组名搜不到）
            case AkaishiMatrixCraftSync.ACTION_SEARCH -> sendCraftCatalog(serverPlayer, serverLevel);
            case AkaishiMatrixCraftSync.ACTION_SELECT -> sendCraftPlan(serverPlayer, serverLevel, target);
            case AkaishiMatrixCraftSync.ACTION_START -> {
                String failure = be.startCraft(serverLevel, target);
                serverPlayer.displayClientMessage(Component.translatable(failure == null
                        ? "message.akaishi.matrix.craft.started"
                        : "message.akaishi.matrix.craft.fail." + failure), true);
            }
            // 界面左列：把玩家送到"那枚芯片自己的界面"。
            // 坐标来自客户端，故必须走 chipAt 白名单（只认矩阵已识别的芯片），
            // 否则改包的客户端能让服务端打开世界里任意方块的界面。
            case AkaishiMatrixCraftSync.ACTION_OPEN_CHIP -> {
                MiniatureTerminalBlockEntity chip = be.chipAt(pos);
                if (chip != null) {
                    MenuRegistry.openExtendedMenu(serverPlayer, chip);
                }
            }
            default -> {
            }
        }
    }

    /**
     * 下发可合成物<b>目录</b>（不含查询串）：过滤改在客户端做 ——
     * 专用服务器没有客户端语言注入，{@code getHoverName()} 会退化成翻译键，
     * 中文名与模组名在服务端根本搜不到，且与库页的客户端过滤口径不一致。
     * <p>
     * 目录只在切到加工页 / 重开界面时拉取，配方表变化后重拉即可。
     */
    private void sendCraftCatalog(ServerPlayer player, ServerLevel level) {
        if (!be.hasUpgrade(AkaishiMiniMatrixUpgradeType.CRAFT)) {
            indexBuildPending = false;
            AkaishiMatrixCraftSync.sendCatalog(player, containerId, List.of(), 0, false);
            return;
        }
        RecipeManager manager = level.getRecipeManager();
        if (!VirtualCraftPlanner.isIndexReady(manager)) {
            // 索引未就绪：先回"准备中"，由 broadcastChanges 分片推进，建好后主动补推一次目录。
            // 绝不在这里现场建 —— 那是秒级工作量，会把服务端整帧卡住（玩家看到"打开没反应、等一会儿才行"）
            indexBuildPending = true;
            VirtualCraftPlanner.startBuild(manager, level.registryAccess());
            AkaishiMatrixCraftSync.sendCatalog(player, containerId, List.of(), 0, true);
            return;
        }
        indexBuildPending = false;
        List<Item> items = VirtualCraftPlanner.craftableItems(manager, level.registryAccess());
        // 「现在就能做」优先：按储存终端（芯片库）快照判定，可直接制作的排前面（界面高亮），其余压暗。
        // 判定复用开工时的同一条路径（配方树 → 叶子 → 库存核对），不会出现"说能做却开不了工"
        IItemTerminalHost host = be.findLedgerHost();
        Set<Item> ready = host == null ? Set.of()
                : CraftReadiness.ready(manager, level.registryAccess(), items, host.storageUnits());
        List<Item> ordered = new ArrayList<>(items.size());
        int readyCount = 0;
        for (Item item : items) {
            if (ready.contains(item)) {
                ordered.add(item);
                readyCount++;
            }
        }
        for (Item item : items) {
            if (!ready.contains(item)) {
                ordered.add(item);
            }
        }
        AkaishiMatrixCraftSync.sendCatalog(player, containerId, ordered, readyCount, false);
    }

    /** 选中项详情：成本三件 + 材料清单 + 材料是否齐备（界面据此把"不能做"标红） */
    private void sendCraftPlan(ServerPlayer player, ServerLevel level, ItemStack target) {
        // 件数回显：即使算不出来也要回，否则界面无法区分"还没算完"和"这个数量算不出来"，
        // 会永久卡在"正在规划…"（现象：有时显示不正确）
        int requested = Math.max(1, Math.min(target.getCount(), VirtualCraftPlanner.MAX_TARGET_COUNT));
        // 库存先取好再规划：规划要"逐级先扣库存"（下级材料够就不现做），扣料与判定必须同一份账
        IItemTerminalHost host = be.findLedgerHost();
        Map<Item, Long> stock = host == null ? Map.of() : CraftLibrary.stock(host.storageUnits());
        VirtualCraftPlanner.Plan plan = target.isEmpty() ? null
                : VirtualCraftPlanner.plan(level.getRecipeManager(), level.registryAccess(), target, stock);
        if (plan == null) {
            // 空 target + 件数 = "这个物品/这个件数确实规划不出来"（区别于没有回包）
            AkaishiMatrixCraftSync.sendPlan(player, containerId, new AkaishiMatrixCraftSync.PlanView(
                    ItemStack.EMPTY, requested, 0L, 0L, 0L, 0L, 0, false, List.of()));
            return;
        }
        // 齐备判定用 consumables（现采材料 + 直接吃掉的库存）：只看 leaves 会漏掉"库存里直接拿"那部分
        boolean affordable = host != null && CraftLibrary.hasAll(stock, plan.consumables());
        List<AkaishiMatrixCraftSync.LeafView> materials = new ArrayList<>();
        // 明细只给"最上面那一层"的直接材料（玩家看得懂"要 3 木板 + 2 木棍"）；
        // 总账仍按 consumables 扣料与判定，两者互不影响
        for (VirtualCraftPlanner.Leaf leaf : plan.direct()) {
            if (materials.size() >= AkaishiMatrixCraftSync.MAX_LEAVES) {
                break;
            }
            materials.add(new AkaishiMatrixCraftSync.LeafView(new ItemStack(leaf.item()), leaf.count(),
                    materialEnough(level, stock, leaf)));
        }
        // 详情必须带上"这是哪个物品、按几件算的"：界面靠它比对当前请求，杜绝异步错包串台
        AkaishiMatrixCraftSync.sendPlan(player, containerId, new AkaishiMatrixCraftSync.PlanView(
                plan.target(), requested, plan.materialIp(), plan.resultIp(), plan.totalTicks(),
                plan.totalEnergy(), plan.steps(), affordable, materials));
    }

    // ===== 加工页：客户端只读访问器 =====

    /**
     * 某个直接材料"够不够"：库里够 ⇒ 够；库里不够但它自己能做出来（缺的从基础材料补）⇒ 也算够。
     * <p>
     * 不能只看库存：玩家手里通常只有原木，而木板/木棍是虚拟加工自己会补的中间产物。
     */
    private static boolean materialEnough(ServerLevel level, Map<Item, Long> stock,
            VirtualCraftPlanner.Leaf leaf) {
        if (stock.getOrDefault(leaf.item(), 0L) >= leaf.count()) {
            return true;
        }
        // 用"件数由调用方给定"的重载：这个数量是订单算出来的，可能远大于单笔订单上限，
        // 走玩家输入那个入口会被夹小 ⇒ 得出偏松的"够"（大单必错）
        VirtualCraftPlanner.Plan sub = VirtualCraftPlanner.plan(level.getRecipeManager(), level.registryAccess(),
                leaf.item(), (int) Math.min(leaf.count(), Integer.MAX_VALUE), stock);
        return sub != null && CraftLibrary.hasAll(stock, sub.consumables());
    }

    @Override
    public void acceptCraftCatalog(List<ItemStack> catalog, boolean building, int readyCount) {
        this.craftCatalogBuilding = building;
        this.craftReadyCount = Math.max(0, Math.min(readyCount, catalog.size()));
        this.craftCatalog = List.copyOf(catalog);
        this.craftPlan = null; // 换了目录，旧详情立即失效
    }

    /** 可直接制作的条目数（目录前段）：界面据此高亮前段、压暗其余 */
    public int craftReadyCount() {
        return craftReadyCount;
    }

    @Override
    public void acceptCraftPlan(@Nullable AkaishiMatrixCraftSync.PlanView plan) {
        this.craftPlan = plan;
    }

    @Override
    public void acceptCraftTask(@Nullable AkaishiMatrixCraftSync.TaskView task) {
        this.craftTaskView = task;
    }

    /** 可合成物目录（客户端本地过滤的原料表，界面不做服务端往返） */
    public List<ItemStack> craftCatalog() {
        return craftCatalog;
    }

    /** 目录是否仍在服务端分片构建（客户端镜像；界面据此显示"准备中"而不是"没有可合成的物品"） */
    public boolean craftCatalogBuilding() {
        return craftCatalogBuilding;
    }

    @Nullable
    public AkaishiMatrixCraftSync.PlanView craftPlan() {
        return craftPlan;
    }

    @Nullable
    public AkaishiMatrixCraftSync.TaskView craftTaskView() {
        return craftTaskView;
    }

    // ===== 安全页：只读访问器（SecurityPage.Source） =====

    @Override
    public String securityOwnerName() {
        return securityOwnerName;
    }

    @Override
    public List<AkaishiTerminalSecuritySync.Entry> securityEntries() {
        return securityEntries;
    }

    @Override
    public boolean securityHasDefault() {
        return securityHasDefault;
    }

    @Override
    public int securityDefaultPerms() {
        return securityDefaultPerms;
    }

    @Override
    public boolean securityEnabled() {
        return !securityEntries.isEmpty() || securityHasDefault;
    }

    // ===== 芯片 / 升级页：只读访问器 =====

    public boolean isFormed() {
        return formed;
    }

    public List<AkaishiMiniMatrixSync.ChipRow> chipRows() {
        return chipRows;
    }

    /**
     * 矩阵终端自身的坐标（界面左列跳转芯片时要记下"从哪儿来"，芯片界面据此显示返回页签）。
     * 客户端侧方块实体未加载时为 null —— 那就记不下来源，返回页签也不显示（不给假入口）。
     */
    @Nullable
    public BlockPos matrixPos() {
        return be == null ? null : be.getBlockPos();
    }

    /** 加工目录里"可直接制作"的条目数（前段）：界面据此高亮前段、压暗其余 */
    private int craftReadyCount;

    /** 某类升级组件的内腔数量（客户端快照） */
    public int upgradeCount(int ordinal) {
        return ordinal >= 0 && ordinal < upgradeCounts.length ? upgradeCounts[ordinal] : 0;
    }

    /** 上一轮无线能源直供量（客户端快照；供升级页显示「联动升级真的在跑」） */
    public long pushedEnergy() {
        return craftPushedEnergy;
    }

    /** 上一轮芯片间搬运量（客户端快照；赤能源芯片 → 储存终端） */
    public long chipTransfer() {
        return craftChipTransfer;
    }

    // ===== 容器生命周期 =====

    @Override
    public boolean stillValid(Player player) {
        // 8 格内才有效：安全页能改权限，不允许远距离操作
        return be == null || player.distanceToSqr(be.getBlockPos().getCenter()) <= 64.0D;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // 关界面即停止补推（构建进度本身留在 VirtualCraftPlanner 的静态表里，下次开界面接着跑完）
        indexBuildPending = false;
        // 授权槽是 Menu 内瞬时容器：关闭 GUI 时未点「登记」的卡必须返还玩家，防物品丢失
        ItemStack card = cardInv.removeItemNoUpdate(0);
        if (!card.isEmpty() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getInventory().placeItemBackInInventory(card);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // slot 0 = 授权槽（不参与自动搬运）；1..27 背包 ↔ 28..36 快捷栏
        if (index == CARD_SLOT_INDEX) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack current = slot.getItem();
        result = current.copy();
        if (index < MACHINE_SLOT_END + 27) {
            if (!this.moveItemStackTo(current, MACHINE_SLOT_END + 27, MACHINE_SLOT_END + PLAYER_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(current, MACHINE_SLOT_END, MACHINE_SLOT_END + 27, false)) {
            return ItemStack.EMPTY;
        }
        if (current.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (current.getCount() == result.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, current);
        return result;
    }
}
