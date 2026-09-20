package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiMiniMatrixTerminalBlockEntity;

import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.menu.MenuRegistry;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 微缩矩阵终端「加工」页网络包。
 * <p>
 * <b>C2S 动作</b>：请求可合成物目录（{@code ACTION_SEARCH}）、选中、开始加工。
 * <b>过滤不在服务端做</b>：专用服务器没有客户端语言注入，{@code getHoverName()} 会退化成翻译键，
 * 中文名与模组名在服务端搜不到；故服务端只发<b>全量目录</b>，客户端用与库页同一套
 * {@code ItemTerminalSearch} 本地过滤（口径一致，且每键不再往返网络）。
 * <p>
 * <b>S2C 视图</b>：三种负载共用一个通道，用类型字节区分：
 * {@code CATALOG}（可合成物目录，只发物品 id，最多 {@link #MAX_CATALOG} 条）、
 * {@code PLAN}（选中项的配方树详情）、{@code TASK}（在跑任务的进度）。
 * <p>
 * <b>条数钳制</b>：目录可能上千条，<b>绝不整表无上限下发</b>；解码端也不信任对端声明的条数
 * （照 {@code AkaishiMiniMatrixSync} 的钳制范式，畸形包不会让客户端预分配爆内存）。
 */
public final class AkaishiMatrixCraftSync {

    /** C2S：加工页动作（请求目录 / 选中 / 开始） */
    public static final ResourceLocation ACTION_CHANNEL =
            new ResourceLocation(AkaishiMod.MOD_ID, "matrix_craft_action");
    /** S2C：加工页视图（目录 / 详情 / 任务） */
    public static final ResourceLocation VIEW_CHANNEL =
            new ResourceLocation(AkaishiMod.MOD_ID, "matrix_craft_view");

    /**
     * 请求可合成物目录：服务端回一次全量目录，之后过滤全在客户端做。
     * <p>
     * 沿用原「搜索」的动作字节（值不变，语义由"带查询串搜索"改为"要目录"）：
     * 查询串字段保留但服务端不再读取，目录只需在切页/重开界面时拉一次、配方表变化后重拉。
     */
    public static final byte ACTION_SEARCH = 0;
    /** 开始加工：target 有效 */
    public static final byte ACTION_START = 1;
    /** 选中一项：target 有效，服务端回该目标的配方树详情 */
    public static final byte ACTION_SELECT = 2;

    /**
     * 打开某一枚已识别芯片自己的界面（界面左列的三个页签）：pos = 那枚芯片的坐标。
     * <p>
     * <b>服务端必须白名单校验</b>：坐标由客户端给出，若不校验，改包的客户端能让服务端
     * 打开世界里任意方块的界面（越权）。校验口径 = 坐标必须落在矩阵当前已识别的芯片集合里。
     */
    public static final byte ACTION_OPEN_CHIP = 4;

    /**
     * 从芯片界面返回矩阵终端（芯片界面左侧那枚「◀ 矩阵」页签）：pos = 来源矩阵的坐标。
     * <p>
     * 与其它动作不同，它<b>不依赖当前打开的菜单</b>（玩家此刻在芯片界面里，那个菜单不是本类的
     * {@link Target}），所以接收端会先分流处理它。服务端校验该坐标是矩阵终端方块且玩家在
     * {@link #BACK_RANGE_SQR} 内 —— 越权面与"直接右键矩阵方块"一致，不因这条便捷入口放大。
     */
    public static final byte ACTION_BACK_TO_MATRIX = 5;

    /** 返回矩阵的最大距离平方（64 格，与矩阵侧其它交互口径一致） */
    private static final double BACK_RANGE_SQR = 64.0D * 64.0D;

    /** S2C 视图类型 */
    public static final byte VIEW_CATALOG = 0;
    public static final byte VIEW_PLAN = 1;
    public static final byte VIEW_TASK = 2;

    /**
     * 可合成物目录条数上限：只发注册 id（约 30 B/条）⇒ 单包 ≤ 约 30 KB，
     * 只在切到加工页/重开界面时下发一次，不随输入往返。
     */
    public static final int MAX_CATALOG = 1024;
    /** 材料清单上限（详情只展示最主要的几项，完整树留在服务端） */
    public static final int MAX_LEAVES = 12;
    /** 查询串长度上限 */
    private static final int MAX_QUERY = 48;
    /** 单个物品注册 id 长度上限（目录按 id 字符串下发，写读同口径） */
    private static final int MAX_ITEM_ID = 128;

    /** 一条基础材料（含真实需求量） */
    /**
     * 材料/直接材料行。
     *
     * @param enough 该材料"够不够"：库里够、或它自己能由基础材料做出来（多级也算）⇒ true。
     *               详情页据此把缺的格子标红，避免玩家看着"缺料"却不知道能不能做
     */
    public record LeafView(ItemStack stack, long count, boolean enough) {
    }

    /**
     * 选中项的配方树详情。
     * <p>
     * {@code target} = 这份详情对应的物品，{@code amount} = 对应的<b>请求件数回显</b>：
     * 详情是异步回包，界面必须同时比对"物品 + 件数"，否则玩家改数量时旧的包后到，
     * 会把显示永久卡在旧账上（现象就是"有时显示不正确"）。
     * <p>
     * {@code machineMissing} = 本单要跑的机械工序在场域内找不到对应机台（界面据此说明为何不能开工）。
     * <p>
     * {@code powerMissing} = 机台在场但<b>拿不到能量</b>（终端缺「操控」/「联动」升级）：真机加工里机台要自己转，
     * 没电就是干等到超时。与 {@code machineMissing} <b>分开报</b>，因为补救办法完全不同（装机器 vs 装升级）。
     */
    public record PlanView(ItemStack target, int amount, long materialIp, long resultIp, long ticks, long energy,
                           long lifeEnergy, boolean machineMissing, boolean powerMissing, int steps,
                           boolean affordable, List<LeafView> leaves, List<ProcessView> processes) {
    }

    /**
     * 一单所需的单条工序来源（界面「这条工序由谁提供」用）。
     * <p>
     * {@code tier} 与 {@code MachineProcessEnergy#sourceTier} 同源：0 自研机台族 / 1 第三方已声明 /
     * 2 第三方未声明（粗粒度）。{@code owners} 只放<b>方块 id</b>，由客户端翻成当前语言的名字。
     */
    public record ProcessView(String processId, byte tier, List<String> owners) {
    }

    /** 单包最多携带的工序条数 / id 与提供方块的上限（本模组最多 13 族，留足余量） */
    private static final int MAX_PROCESSES = 24;
    private static final int MAX_PROCESS_ID = 128;
    private static final int MAX_PROCESS_OWNERS = 8;

    /** 失败原因代号的长度上限：都是 {@code no_machine} 这类短代号，收发两端同口径 */
    private static final int MAX_FAIL_REASON = 32;

    /** 任务视图类型：虚拟加工（有总时长承诺） */
    public static final byte TASK_CRAFT = 0;

    /**
     * 在跑任务的进度（无任务时传 null）。
     * <p>
     * {@code type = TASK_CRAFT} 用 {@code remainingTicks/totalTicks}（<b>预估</b>：真机加工的实际耗时由机台决定）；
     * {@code collected/elapsed} = 真机加工的<b>节点进度</b>（已完成节点 / 总节点）——机台不归我们计时，
     * 节点数才是真实进度。
     */
    public record TaskView(byte type, ItemStack target, int remainingTicks, int totalTicks,
                           int collected, int elapsed) {
    }

    /** 接收方：只有矩阵终端菜单实现 */
    public interface Target {
        /**
         * C2S：执行一次加工页动作。
         *
         * @param target 目标物品（选中/开工用）
         */
        void acceptCraftAction(Player actor, byte action, String query, ItemStack target, ItemStack extra,
                BlockPos pos);

        /**
         * S2C：可合成物目录（客户端本地过滤用；只读）。
         *
         * @param building true = 服务端索引正在分片构建，目录还不可用（界面显示"准备中"，稍后会再推一次）
         */
        void acceptCraftCatalog(List<ItemStack> catalog, boolean building, int readyCount);

        /** S2C：选中项详情；null = 无可规划方案 */
        void acceptCraftPlan(@Nullable PlanView plan);

        /** S2C：任务进度；task = null 且 failure = null = 当前无任务也无失败记录 */
        void acceptCraftTask(@Nullable TaskView task, @Nullable String failure);
    }

    private AkaishiMatrixCraftSync() {
    }

    // ===== C2S =====

    /** 服务端注册接收器（AkaishiMod.init 调用） */
    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ACTION_CHANNEL, (buf, context) -> {
            int containerId = buf.readInt();
            byte action = buf.readByte();
            String query = buf.readUtf(MAX_QUERY);
            ItemStack target = AkaishiItemTerminalSync.readFullStack(buf);
            ItemStack extra = AkaishiItemTerminalSync.readFullStack(buf);
            BlockPos pos = buf.readBlockPos();
            context.queue(() -> {
                Player player = context.getPlayer();
                if (player == null) {
                    return;
                }
                // 返回矩阵：此刻玩家在芯片界面里，容器不是矩阵菜单，故先分流（校验见 backToMatrix）
                if (action == ACTION_BACK_TO_MATRIX) {
                    backToMatrix(player, pos);
                    return;
                }
                // 必须是打开中的矩阵终端菜单且容器 ID 一致，拦截伪造包
                if (player.containerMenu instanceof Target actionTarget
                        && player.containerMenu.containerId == containerId) {
                    actionTarget.acceptCraftAction(player, action, query, target, extra, pos);
                }
            });
        });
    }

    /** 客户端：发送一次动作（搜索 / 选中 / 开始加工） */
    public static void sendAction(int containerId, byte action, String query, ItemStack target,
            ItemStack extra, BlockPos pos) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(containerId);
        buf.writeByte(action);
        buf.writeUtf(query == null ? "" : query, MAX_QUERY);
        AkaishiItemTerminalSync.writeFullStack(buf, target == null ? ItemStack.EMPTY : target);
        AkaishiItemTerminalSync.writeFullStack(buf, extra == null ? ItemStack.EMPTY : extra);
        buf.writeBlockPos(pos == null ? BlockPos.ZERO : pos);
        NetworkManager.sendToServer(ACTION_CHANNEL, buf);
    }

    /** 便捷重载：单个目标物品（选中 / 开工） */
    public static void sendAction(int containerId, byte action, String query, ItemStack target) {
        sendAction(containerId, action, query, target, ItemStack.EMPTY, BlockPos.ZERO);
    }

    /**
     * 打开来源矩阵终端（芯片界面左侧那枚返回页签）。
     * <p>
     * 坐标由客户端在跳转时记下，所以这里只认"该坐标上确实是矩阵终端方块 + 玩家在有效距离内"：
     * 越权面与直接右键矩阵方块相同（矩阵方块自身不设开门权限），不因这条便捷入口而放大。
     */
    private static void backToMatrix(Player player, BlockPos pos) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof AkaishiMiniMatrixTerminalBlockEntity matrix
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= BACK_RANGE_SQR) {
            MenuRegistry.openExtendedMenu(serverPlayer, matrix);
        } else {
            // 静默失败等于"点了没反应"：必须给回执，让玩家知道是距离/方块问题而不是功能坏了
            serverPlayer.displayClientMessage(
                    Component.translatable("message.akaishi.matrix.back.too_far"), true);
        }
    }

    // ===== S2C =====

    /** 客户端注册接收器（AkaishiMod.init 的客户端分支调用） */
    public static void registerClient() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, VIEW_CHANNEL, (buf, context) -> {
            int containerId = buf.readInt();
            byte type = buf.readByte();
            List<ItemStack> results = List.of();
            PlanView plan = null;
            TaskView task = null;
            String taskFail = null;
            boolean building = false;
            int readyCount = 0;
            switch (type) {
                case VIEW_CATALOG -> {
                    building = buf.readBoolean();
                    // 可直接制作的条目数：越界值直接钳到 [0, 条数]，不信任对端
                    int readyDeclared = buf.readVarInt();
                    int declared = buf.readVarInt();
                    int count = Math.min(Math.max(declared, 0), MAX_CATALOG);
                    readyCount = Math.min(Math.max(readyDeclared, 0), count);
                    List<ItemStack> read = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        // 只传物品 id：解析失败或对端注册表里没有的 id 退化为空气，空栈不进目录
                        ResourceLocation id = ResourceLocation.tryParse(buf.readUtf(MAX_ITEM_ID));
                        if (id == null) {
                            continue;
                        }
                        Item item = BuiltInRegistries.ITEM.get(id);
                        if (item != Items.AIR) {
                            read.add(new ItemStack(item));
                        }
                    }
                    results = read;
                }
                case VIEW_PLAN -> plan = buf.readBoolean() ? readPlan(buf) : null;
                case VIEW_TASK -> {
                    if (buf.readBoolean()) {
                        task = readTask(buf);
                    } else {
                        // 无任务时才带失败原因（失败的任务对象已被服务端丢弃）：空串 = 没有失败
                        String reason = buf.readUtf(MAX_FAIL_REASON);
                        taskFail = reason.isEmpty() ? null : reason;
                    }
                }
                default -> {
                    return; // 未知类型：直接丢弃，不做任何落地
                }
            }
            final List<ItemStack> accepted = results;
            final PlanView acceptedPlan = plan;
            final TaskView acceptedTask = task;
            final String acceptedFail = taskFail;
            final boolean acceptedBuilding = building;
            final int acceptedReady = readyCount;
            // 网络线程只解码，落地回客户端主线程，避免与渲染线程并发读写
            Minecraft.getInstance().execute(() -> {
                var player = Minecraft.getInstance().player;
                if (player != null && player.containerMenu instanceof Target target
                        && player.containerMenu.containerId == containerId) {
                    if (type == VIEW_CATALOG) {
                        target.acceptCraftCatalog(accepted, acceptedBuilding, acceptedReady);
                    } else if (type == VIEW_PLAN) {
                        target.acceptCraftPlan(acceptedPlan);
                    } else {
                        target.acceptCraftTask(acceptedTask, acceptedFail);
                    }
                }
            });
        });
    }

    /**
     * 服务端：下发可合成物目录（只发物品 id；条数按 {@link #MAX_CATALOG} 钳制）。
     *
     * @param building true = 索引还在分片构建，本次只是"准备中"回执（items 必为空）
     */
    public static void sendCatalog(ServerPlayer player, int containerId, List<Item> items, int readyCount,
            boolean building) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(containerId);
        buf.writeByte(VIEW_CATALOG);
        buf.writeBoolean(building);
        // 可直接制作的条目数（目录前段；其余为"有配方但缺料"，界面压暗）—— 写读顺序必须一致
        buf.writeVarInt(Math.max(0, Math.min(readyCount, items.size())));
        int count = Math.min(items.size(), MAX_CATALOG);
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buf.writeUtf(BuiltInRegistries.ITEM.getKey(items.get(i)).toString(), MAX_ITEM_ID);
        }
        NetworkManager.sendToPlayer(player, VIEW_CHANNEL, buf);
    }

    /** 服务端：下发选中项详情（null = 不可规划） */
    public static void sendPlan(ServerPlayer player, int containerId, @Nullable PlanView plan) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(containerId);
        buf.writeByte(VIEW_PLAN);
        buf.writeBoolean(plan != null);
        if (plan != null) {
            AkaishiItemTerminalSync.writeFullStack(buf,
                    plan.target() == null ? ItemStack.EMPTY : plan.target());
            // 件数回显：紧随 target 之后，写读顺序必须一致（见 §17.2 断线教训）
            buf.writeVarInt(plan.amount());
            buf.writeVarLong(plan.materialIp());
            buf.writeVarLong(plan.resultIp());
            buf.writeVarLong(plan.ticks());
            buf.writeVarLong(plan.energy());
            buf.writeVarLong(plan.lifeEnergy());
            buf.writeBoolean(plan.machineMissing());
            buf.writeBoolean(plan.powerMissing());
            buf.writeVarInt(plan.steps());
            buf.writeBoolean(plan.affordable());
            int leaves = Math.min(plan.leaves().size(), MAX_LEAVES);
            buf.writeVarInt(leaves);
            for (int i = 0; i < leaves; i++) {
                LeafView leaf = plan.leaves().get(i);
                AkaishiItemTerminalSync.writeFullStack(buf, leaf.stack());
                buf.writeVarLong(leaf.count());
                buf.writeBoolean(leaf.enough());
            }
            // 工序来源紧随材料之后（写读顺序必须一致，见 §17.2 断线教训）
            int processes = Math.min(plan.processes().size(), MAX_PROCESSES);
            buf.writeVarInt(processes);
            for (int i = 0; i < processes; i++) {
                ProcessView process = plan.processes().get(i);
                buf.writeUtf(process.processId(), MAX_PROCESS_ID);
                buf.writeByte(process.tier());
                int owners = Math.min(process.owners().size(), MAX_PROCESS_OWNERS);
                buf.writeVarInt(owners);
                for (int k = 0; k < owners; k++) {
                    buf.writeUtf(process.owners().get(k), MAX_PROCESS_ID);
                }
            }
        }
        NetworkManager.sendToPlayer(player, VIEW_CHANNEL, buf);
    }

    /**
     * 服务端：下发任务进度（null = 无任务）。
     *
     * @param failure 上一次加工的失败原因（内部代号，如 {@code no_machine}）；无则 null。
     *                失败时任务对象本身已被丢弃（{@code task == null}），原因必须随同一包带下去，
     *                否则玩家只看到"加工莫名停了"（界面无从解释）
     */
    public static void sendTask(ServerPlayer player, int containerId, @Nullable TaskView task,
            @Nullable String failure) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(containerId);
        buf.writeByte(VIEW_TASK);
        buf.writeBoolean(task != null);
        if (task != null) {
            buf.writeByte(task.type());
            AkaishiItemTerminalSync.writeFullStack(buf, task.target());
            buf.writeVarInt(task.remainingTicks());
            buf.writeVarInt(task.totalTicks());
            buf.writeVarInt(task.collected());
            buf.writeVarInt(task.elapsed());
        } else {
            buf.writeUtf(failure == null ? "" : failure, MAX_FAIL_REASON);
        }
        NetworkManager.sendToPlayer(player, VIEW_CHANNEL, buf);
    }

    private static PlanView readPlan(FriendlyByteBuf buf) {
        ItemStack target = AkaishiItemTerminalSync.readFullStack(buf);
        int amount = buf.readVarInt();
        long materialIp = buf.readVarLong();
        long resultIp = buf.readVarLong();
        long ticks = buf.readVarLong();
        long energy = buf.readVarLong();
        long lifeEnergy = buf.readVarLong();
        boolean machineMissing = buf.readBoolean();
        boolean powerMissing = buf.readBoolean();
        int steps = buf.readVarInt();
        boolean affordable = buf.readBoolean();
        int declared = buf.readVarInt();
        int count = Math.min(Math.max(declared, 0), MAX_LEAVES);
        List<LeafView> leaves = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            leaves.add(new LeafView(AkaishiItemTerminalSync.readFullStack(buf), buf.readVarLong(),
                    buf.readBoolean()));
        }
        // 工序来源：条数与 id 长度都对端给的值，一律钳制（解码端不信任对端）
        int declaredProcesses = buf.readVarInt();
        int processes = Math.min(Math.max(declaredProcesses, 0), MAX_PROCESSES);
        List<ProcessView> processViews = new ArrayList<>(processes);
        for (int i = 0; i < processes; i++) {
            String processId = buf.readUtf(MAX_PROCESS_ID);
            byte tier = buf.readByte();
            int declaredOwners = buf.readVarInt();
            int owners = Math.min(Math.max(declaredOwners, 0), MAX_PROCESS_OWNERS);
            List<String> ownerIds = new ArrayList<>(owners);
            for (int k = 0; k < owners; k++) {
                ownerIds.add(buf.readUtf(MAX_PROCESS_ID));
            }
            processViews.add(new ProcessView(processId, tier, List.copyOf(ownerIds)));
        }
        return new PlanView(target, Math.max(1, amount), materialIp, resultIp, ticks, energy, lifeEnergy,
                machineMissing, powerMissing, steps, affordable, leaves, processViews);
    }

    private static TaskView readTask(FriendlyByteBuf buf) {
        return new TaskView(buf.readByte(), AkaishiItemTerminalSync.readFullStack(buf),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }
}
