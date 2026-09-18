package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.miniature.IMiniatureChipView;
import com.example.akaishi.block.AkaishiMiniMatrixBlocks;
import com.example.akaishi.block.AkaishiMiniMatrixTerminalBlock;
import com.example.akaishi.block.AkaishiMiniatureTerminalBlock;
import com.example.akaishi.multiblock.MatrixStructure;
import com.example.akaishi.menu.AkaishiMiniMatrixTerminalMenu;
import com.example.akaishi.block.AkaishiMiniMatrixNetworkNodeBlock;
import com.example.akaishi.block.AkaishiMiniMatrixUpgradeBlock;
import com.example.akaishi.block.AkaishiMiniMatrixUpgradeType;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.storage.IItemTerminalHost;
import com.example.akaishi.craft.CraftLibrary;
import com.example.akaishi.craft.VirtualCraftPlanner;
import com.example.akaishi.craft.VirtualCraftTask;
import com.example.akaishi.craft.WirelessMachineScanner;
import com.example.akaishi.wireless.TerminalSecurity;
import com.example.akaishi.wireless.WirelessFieldManager;
import com.example.akaishi.wireless.WirelessNodeRegistry;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 微缩矩阵终端方块实体：5×5×5 空腔箱体的成型判定中枢，并<b>读取贴在墙面上的芯片</b>。
 * <p>
 * 结构：复用 {@link MatrixStructure#scan}（六面闭合 + 内腔全为空气）；控制器必须贴墙。
 * 墙面白名单见 {@link #isWallBlock}，其中包含微缩终端（芯片）——玩家把「墙面砖块换成微缩终端」
 * 就是本阶段要支持的行为。
 * <p>
 * 芯片识别：芯片是<b>墙面格上的方块实体</b>，故扫描箱体表面各格，收集实现了
 * {@link IMiniatureChipView} 的 {@link MiniatureTerminalBlockEntity}。只认读数面接口，
 * 不认具体终端族（新增族只加适配器，本类无需改动）。
 * <p>
 * 缓存策略：结构方块几乎不变，故用 {@code structureDirty} 标记 + 每 {@link #RESCAN_INTERVAL} tick
 * 兜底重扫（与物品终端同范式）；扫描结果按实例去重（幂等）、跳过已移除者。
 * <p>
 * 本阶段（P1a）只做「识别与读数」，不涉升级/场域/加工；故无额外 NBT 持久化字段。
 * 实现 {@link IDataCarrier} 以随 {@code AkaishiMachineBlock} 获得掉落数据保留。
 */
public class AkaishiMiniMatrixTerminalBlockEntity extends BlockEntity implements ExtendedMenuProvider, IDataCarrier {

    /** 箱体边长（5×5×5，控制器贴墙） */
    public static final int SIZE = 5;
    /** 重扫间隔（tick）：结构变化不频繁，定时兜底即可，避免每 tick 全量扫描 */
    private static final int RESCAN_INTERVAL = 20;
    /** 网络节点自带子场域的半径（区块）：需求口径「半径 1 区块」 */
    private static final int NODE_FIELD_RADIUS = 1;
    /** 无线能源直供的轮询间隔（tick）：闸门① 拉取节流，不逐 tick 遍历场域 */
    private static final int ENERGY_INTERVAL = 40;
    /** 单台机器每轮最多接受的能量：让多台机器雨露均沾，而不是被最近的一台吃干 */
    private static final long PER_MACHINE_ENERGY = 20_000L;
    /** 单源芯片每轮向同一目标芯片最多搬运的能量（与机器直供同量级，避免一轮把源抽空） */
    private static final long PER_CHIP_TRANSFER = 20_000L;
    /** 方块实体同步标签里的归属者键（屏障可见性过滤用） */
    private static final String TAG_FIELD_OWNER = "FieldOwner";
    private static final String TAG_FIELD_OWNER_NAME = "FieldOwnerName";
    /** 已申领节点坐标落盘键（见 {@link #claimNodes}：节点登记表靠区块加载驱动，必须落盘才能跨重启复申领） */
    private static final String TAG_CLAIMED_NODES = "ClaimedNodes";

    /** 结构扫描缓存失效标记（放置/读档后为 true，首 tick 立即扫一次） */
    private boolean structureDirty = true;
    /** 距离下次重扫的 tick 数 */
    private int scanCooldown;
    /** 墙面识别到的芯片（只读对外；未成型时为空列表） */
    private List<IMiniatureChipView> chips = List.of();
    /**
     * 安全表：矩阵是「主卡」唯一编辑入口，登记后由 {@link #applySecurityToChips()} 写入全部绑定芯片。
     * <p>
     * 芯片仍持自己的权威表（判定时读芯片的），矩阵只是统一入口 —— 见设计记忆 §15.2 口径 5。
     */
    private final TerminalSecurity security = new TerminalSecurity(this::setChanged);
    /**
     * 内腔升级组件计数（下标 = {@link AkaishiMiniMatrixUpgradeType} 序数）：
     * 升级件是方块而非物品，故不存 NBT，每次重扫随结构一起从世界读出。
     */
    private final int[] upgradeCounts = new int[AkaishiMiniMatrixUpgradeType.values().length];
    /**
     * 已申领的网络节点坐标（仅服务端主线程读写，无需持久化）：
     * 用来在节点被取消申领 / 终端被拆时释放对应子场域票据。
     */
    private final Set<BlockPos> claimedNodes = new LinkedHashSet<>();

    /**
     * 屏障可见性过滤用的归属者（<b>客户端</b>由方块实体更新包同步；服务端不用）。
     * 场域屏障是纯客户端表现，若把整张安全表同步下去代价过大，故只带这两项。
     */
    @Nullable
    private UUID fieldOwnerId;
    @Nullable
    private String fieldOwnerName;

    /**
     * 当前虚拟加工任务（<b>并发上限 1</b>：性能闸门之一 —— 加工会占用库与赤能源缓冲，
     * 单任务串行既保证扣料原子性，也避免一台终端把全库锁死；上限留待后续按升级等级放宽）。
     */
    @Nullable
    private VirtualCraftTask craftTask;

    /** 能源直供轮询冷却（与重扫/快照各自独立节流） */
    private int energyCooldown;
    /** 上一轮直供实际送出的能量（状态回执用，不持久化） */
    private long lastPushedEnergy;
    /** 上一轮芯片间搬运的能量（状态回执用，不持久化） */
    private long lastChipTransfer;

    public AkaishiMiniMatrixTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_MINI_MATRIX_TERMINAL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiMiniMatrixTerminalBlockEntity be) {
        be.tickServer();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, AkaishiMiniMatrixTerminalBlockEntity be) {
        be.tickClient();
    }

    private void tickServer() {
        if (structureDirty || --scanCooldown <= 0) {
            rescan();
        }
        // 虚拟加工任务逐 tick 推进（倒计时必须按真实 tick 走，不能挂在 20 tick 的重扫节流上）
        if (craftTask != null && level instanceof ServerLevel serverLevel2) {
            if (craftTask.tick(serverLevel2)) {
                craftTask = null;
            }
        }
        // 无线能源直供：每 ENERGY_INTERVAL tick 一次（不是每 tick），只在装了「场域联动升级」时跑
        if (energyCooldown > 0) {
            energyCooldown--;
        } else if (level instanceof ServerLevel serverLevel3) {
            energyCooldown = ENERGY_INTERVAL;
            pushWirelessEnergy(serverLevel3);
        }
    }

    /**
     * 无线能源直供（P5）：把墙上芯片的能量缓冲灌给场域内装了「无线接收升级」的机器。
     * <p>
     * <b>为什么由终端推、而不是机器来取</b>：机器侧没有统一调用点 —— 本项目二十多个机器方块实体
     * 各有自己的 tick 与耗能路径，逐个挂钩的侵入面远大于收益；而"每 40 tick 遍历一次场域内
     * <b>已加载</b>区块的方块实体"成本很低（同一个 {@link WirelessMachineScanner}），
     * 且天然只在装了联动升级时才跑。机器若需要更精确的节奏，仍可自行调用
     * {@code IUpgradeableMachine#isWirelessReady} 走"主动来取"。
     * <p>
     * <b>能量类型</b>：芯片自己就是 {@link IEnergyProvider}，其缓冲的 {@code getType()} 决定这枚芯片
     * 提供的是赤能源还是生命能源 —— 于是"按族直供"不需要任何硬编码分支，
     * 机器侧用同类型的 {@code getEnergyStorage(type)} 接收即可。
     * <p>
     * <b>不变量</b>：先模拟（{@code addEnergy(x, true)}）能收多少，再真加，最后按真实入账量从缓冲扣除；
     * 任何一步失败都不产生能量，绝不凭空造能或销毁能量。
     */
    private void pushWirelessEnergy(ServerLevel level) {
        // 先归零再算：提前 return 的几轮（没装链接升级 / 无场域 / 场域内无机器）也必须反映为"本轮送 0"，
        // 否则界面会把上一轮的非零值一直显示成"还在供能"
        lastPushedEnergy = 0L;
        lastChipTransfer = 0L;
        // ① 芯片间搬运：矩阵内部总线（见 relayEnergyBetweenChips），与场域/联动升级无关，
        //    故必须放在 LINK 门禁之前 —— 否则"装了赤能源芯片却没装联动升级"时储存终端永远充不上电
        lastChipTransfer = relayEnergyBetweenChips();
        // ② 直供准入：操控（对机器写能的准入）+ 联动（供能链路）缺一不可。
        //    操控组件原本是派发的门禁，派发移除后它唯一有意义的落点就是"对外写能"这件事本身，
        //    这样 5 类组件里没有一件是"装了却没有任何效果"的。
        if (!hasUpgrade(AkaishiMiniMatrixUpgradeType.CONTROL)
                || !hasUpgrade(AkaishiMiniMatrixUpgradeType.LINK)) {
            return;
        }
        int radius = fieldRadiusChunks();
        if (radius <= 0) {
            return;
        }
        List<BlockPos> machines = WirelessMachineScanner.scan(level, worldPosition, radius);
        if (machines.isEmpty()) {
            return;
        }
        long pushed = 0L;
        for (IMiniatureChipView chip : chips) {
            if (!(chip instanceof MiniatureTerminalBlockEntity be)) {
                continue;
            }
            IEnergyStorage buffer = be.getEnergyStorage();
            if (buffer == null || buffer.getEnergyStored() <= 0L) {
                continue;
            }
            for (BlockPos pos : machines) {
                if (buffer.getEnergyStored() <= 0L) {
                    break;
                }
                if (!(level.getBlockEntity(pos) instanceof IEnergyProvider provider) || !provider.canInputEnergy()) {
                    continue;
                }
                IEnergyStorage target = provider.getEnergyStorage(buffer.getType());
                if (target == null) {
                    continue; // 该机器不接受这种能量（如只吃赤能源的机器遇到生命能源芯片）
                }
                long room = target.getMaxEnergy() - target.getEnergyStored();
                if (room <= 0L) {
                    continue;
                }
                long want = Math.min(Math.min(room, PER_MACHINE_ENERGY), buffer.getEnergyStored());
                long fit = target.addEnergy(want, true);
                if (fit <= 0L) {
                    continue;
                }
                long moved = target.addEnergy(fit, false);
                if (moved > 0L) {
                    buffer.extractEnergy(moved, false);
                    pushed += moved;
                }
            }
        }
        lastPushedEnergy = pushed;
    }

    /**
     * 芯片间能量搬运（矩阵内部总线）：把<b>有能芯片</b>的能量补给<b>能收的芯片</b>。
     * 典型场景：赤能源芯片 → 物品储存终端（后者 {@code canInputEnergy()=true}，加工的赤能源费用由此支付）。
     * <p>
     * <b>为什么不依赖场域与联动升级</b>：这是「矩阵读取并使用墙上芯片」能力的延伸，与场域无关 ——
     * 需求口径是「赤能源芯片组可以直接供应给储存终端」，故放在 LINK 门禁之前执行。
     * <p>
     * <b>判据</b>：目标 = {@code canInputEnergy()} 且未满；源 = 同类能量且有余额。
     * 刻意<b>不查源侧的 {@code canOutputEnergy()}</b>：那一条的语义是"是否向能量管道开口"，
     * 而无线能源族终端明确不向管道开口（{@code WirelessTerminalMiniatureState} 两者皆 false），
     * 却正是本需求里的供电方 —— 用管道口径做判据会把唯一的电源挡在门外。
     * <p>
     * <b>守恒</b>：先 {@code addEnergy(x, true)} 模拟入账 → 真加 → 按<b>真实入账量</b>
     * {@code extractEnergy}，任何一步失败都不产生能量，也绝不凭空造能或销毁能量。
     *
     * @return 本轮实际搬运量（供界面回执）
     */
    private long relayEnergyBetweenChips() {
        List<MiniatureTerminalBlockEntity> loaded = new ArrayList<>();
        for (IMiniatureChipView chip : chips) {
            if (chip instanceof MiniatureTerminalBlockEntity be && be.getEnergyStorage() != null) {
                loaded.add(be);
            }
        }
        if (loaded.size() < 2) {
            return 0L; // 只有一块芯片时没有搬运对象
        }
        long moved = 0L;
        for (MiniatureTerminalBlockEntity sinkChip : loaded) {
            IEnergyStorage sink = sinkChip.getEnergyStorage();
            if (sink == null || !sinkChip.canInputEnergy()) {
                continue;
            }
            long room = sink.getMaxEnergy() - sink.getEnergyStored();
            for (MiniatureTerminalBlockEntity sourceChip : loaded) {
                if (room <= 0L) {
                    break;
                }
                if (sourceChip == sinkChip) {
                    continue;
                }
                IEnergyStorage src = sourceChip.getEnergyStorage();
                if (src == null || src.getType() != sink.getType() || src.getEnergyStored() <= 0L) {
                    continue; // 类型必须一致：赤能源只补给赤能源容器
                }
                long want = Math.min(Math.min(room, src.getEnergyStored()), PER_CHIP_TRANSFER);
                long fit = sink.addEnergy(want, true);
                if (fit <= 0L) {
                    continue;
                }
                long accepted = sink.addEnergy(fit, false);
                if (accepted > 0L) {
                    src.extractEnergy(accepted, false);
                    room -= accepted;
                    moved += accepted;
                }
            }
        }
        return moved;
    }

    /** 上一轮芯片间搬运的能量（状态回执用） */
    public long lastChipTransfer() {
        return lastChipTransfer;
    }

    // ===== 虚拟加工（P4a） =====

    /**
     * 启动一次虚拟加工（目标物品 → 配方树 → 扣料/扣费 → 倒计时 → 产物入库）。
     *
     * @return 失败原因（成功返回 null）
     */
    @Nullable
    public String startCraft(ServerLevel level, ItemStack target) {
        if (!isFormed() || !hasUpgrade(AkaishiMiniMatrixUpgradeType.CRAFT)) {
            return "craft_upgrade_missing";
        }
        if (craftTask != null) {
            return "busy";
        }
        IItemTerminalHost host = findLedgerHost();
        if (host == null) {
            return "no_chip";
        }
        // 与界面同一份账：逐级先扣库存（库里有的木板/木棍直接用掉，缺的才现做），
        // 否则会出现"界面说能做、开工说不够"
        Map<Item, Long> stock = CraftLibrary.stock(host.storageUnits());
        VirtualCraftPlanner.Plan plan = VirtualCraftPlanner.plan(level.getRecipeManager(),
                level.registryAccess(), target, stock);
        if (plan == null) {
            return "unresolvable";
        }
        VirtualCraftTask started = VirtualCraftTask.start(level, host, plan);
        if (started == null) {
            return "not_enough";
        }
        craftTask = started;
        return null;
    }

    /** 当前加工任务（界面进度条用；无任务返回 null） */
    @Nullable
    public VirtualCraftTask craftTask() {
        return craftTask;
    }

    /** 终止当前加工并回填锁定材料（方块被拆时由方块调用） */
    public void abortCraft(ServerLevel level) {
        if (craftTask != null) {
            craftTask.fail(level);
            craftTask = null;
        }
    }

    /** 上一轮无线能源直供实际送出的能量（联调回执用） */
    public long lastPushedEnergy() {
        return lastPushedEnergy;
    }

    /**
     * 找一枚「带物品库」的芯片当账本宿主：矩阵自身没有能量也没有库，
     * 库与赤能源缓冲都在物品族微缩芯片上（设计记忆 §15.9）。
     */
    @Nullable
    public IItemTerminalHost findLedgerHost() {
        for (IMiniatureChipView chip : chips) {
            if (chip instanceof MiniatureTerminalBlockEntity be) {
                IItemTerminalHost host = be.itemHost();
                if (host != null && !host.isRemoved()) {
                    return host;
                }
            }
        }
        return null;
    }

    /**
     * 客户端只读重扫：只为<b>场域屏障渲染</b>提供内腔升级读数。
     * <p>
     * 刻意不写方块状态（{@code FORMED} 由服务端权威同步，客户端写会与服务端打架），
     * 也不碰场域/安全等任何服务端状态 —— 屏障是纯表现层，因此<b>不需要新增同步包</b>；
     * 渲染器只在本区块被渲染时才会被调用，远处矩阵自然也看不到，无需额外距离判断。
     */
    private void tickClient() {
        if (--scanCooldown > 0) {
            return;
        }
        scanCooldown = RESCAN_INTERVAL;
        MatrixStructure.Result box = MatrixStructure.scan(level, worldPosition, SIZE,
                AkaishiMiniMatrixTerminalBlockEntity::isWallBlock,
                AkaishiMiniMatrixTerminalBlockEntity::isInteriorAllowed);
        Arrays.fill(upgradeCounts, 0);
        if (box != null) {
            scanUpgrades(box);
        }
    }

    /**
     * 重扫结构与芯片：成型状态写回方块的 {@code FORMED} 属性，芯片列表整体替换（幂等）。
     * <p>
     * 未成型时立即清空芯片与升级缓存：结构一旦破损就不该再对外声称持有那些能力。
     */
    private void rescan() {
        structureDirty = false;
        scanCooldown = RESCAN_INTERVAL;
        // 内腔放行本族升级组件（其余仍必须为空），从而与无线终端族的内腔组件范式对齐
        MatrixStructure.Result box = MatrixStructure.scan(level, worldPosition, SIZE,
                AkaishiMiniMatrixTerminalBlockEntity::isWallBlock,
                AkaishiMiniMatrixTerminalBlockEntity::isInteriorAllowed);
        boolean formed = box != null;
        if (getBlockState().getValue(AkaishiMiniMatrixTerminalBlock.FORMED) != formed) {
            level.setBlock(worldPosition, getBlockState().setValue(AkaishiMiniMatrixTerminalBlock.FORMED, formed), 3);
        }
        this.chips = formed ? scanChips(box) : List.of();
        if (formed) {
            scanUpgrades(box);
        } else {
            Arrays.fill(upgradeCounts, 0);
            // 结构一旦破损，在跑的加工立即终止并回填锁定材料（不给"拆一半还能出货"的口子）
            if (craftTask != null && level instanceof ServerLevel serverLevel) {
                abortCraft(serverLevel);
            }
        }
        syncField();
    }

    /**
     * 把场域登记与内腔读数对齐：成型且有场域升级 ⇒ 注册/刷新；否则释放。
     * <p>
     * 与重扫同频（{@link #RESCAN_INTERVAL} tick），票据的增删由
     * {@link WirelessFieldManager} 内部做参数 diff，重复调用是幂等的。
     */
    private void syncField() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int radius = fieldRadiusChunks();
        if (radius > 0) {
            // 持有者坐标 = 终端自身坐标：节点子场域也由本终端申领（同一持有者身份）
            WirelessFieldManager.refresh(serverLevel, worldPosition, radius, worldPosition);
            claimNodes(serverLevel);
        } else {
            WirelessFieldManager.release(serverLevel, worldPosition, worldPosition);
            releaseNodes(serverLevel);
        }
    }

    /**
     * 申领网络节点：从<b>已申领名单 + 登记表</b>里就近优先取 {@link #extendNodeCount()} 个节点，
     * 各挂 1 区块子场域（需求里的「让专用网络节点附加无线场域」）；不再申领的立即释放。
     * <p>
     * <b>不限距离（用户拍板）</b>：节点可放在主场域之外的任意位置，不再要求落在主场域内
     * （原口径「必须在主场域内、否则是隔空拉线」据此作废）。开销由<b>最多 3 个节点</b>钳住：
     * 单节点票据半径 1 区块 = 3×3 区块，故单台终端最多弱加载 3 片 3×3 区域。
     * <p>
     * <b>为什么名单要落盘（NBT {@code ClaimedNodes}）</b>：登记表由节点方块实体在<b>区块加载</b>时写入，
     * 远处节点的区块平时是卸载的 ⇒ 表里根本没有它 ⇒ 重启后若不靠落盘名单复申领，
     * 「无线拓展升级」会静默失效，玩家只能把节点拆掉重放。复申领本身经
     * {@link WirelessFieldManager#refresh} 挂上 PORTAL 票据，节点区块因此被弱加载、随后自然回到登记表。
     * <p>
     * 复核顺序按代价从便宜到昂贵：① 名单里<b>已加载</b>的坐标复核方块类型；② 登记表候选先看区块是否已加载；
     * ③ 才读方块类型。<b>绝不先 {@code getBlockState} 未加载坐标</b>：那会同步加载/生成区块，
     * 而本方法每 {@link #RESCAN_INTERVAL} tick 就跑一次。
     * <p>
     * 未加载坐标在<b>新发现</b>路径上直接跳过（不触发同步加载，等它加载的那一轮再申领）；
     * 而<b>已申领</b>坐标即使未加载也照旧保留 —— 弱加载票据正是靠这条路径续上的。
     */
    private void claimNodes(ServerLevel serverLevel) {
        int allow = extendNodeCount();
        if (allow <= 0) {
            releaseNodes(serverLevel); // 没有拓展升级（或被拆掉）：全部释放，不留票据
            return;
        }
        // 1) 修剪失效申领：区块已加载却不再是节点方块（被拆/被换）⇒ 摘占位并释放
        claimedNodes.removeIf(pos -> {
            if (!isStaleClaim(serverLevel, pos)) {
                return false;
            }
            WirelessFieldManager.release(serverLevel, pos, worldPosition);
            setNodeClaimed(serverLevel, pos, false);
            return true;
        });
        // 2) 候选 = 已申领（保留）+ 登记表新发现（就近优先），再按名额截断
        List<BlockPos> candidates = new ArrayList<>(claimedNodes);
        for (BlockPos pos : WirelessNodeRegistry.nodesIn(serverLevel)) {
            if (candidates.contains(pos)) {
                continue;
            }
            ChunkPos cp = new ChunkPos(pos);
            if (serverLevel.getChunkSource().getChunkNow(cp.x, cp.z) == null) {
                continue; // 未加载：不触发同步加载，等它加载的那一轮再申领
            }
            if (!(serverLevel.getBlockState(pos).getBlock() instanceof AkaishiMiniMatrixNetworkNodeBlock)) {
                continue; // 失效登记（方块已被替换）
            }
            candidates.add(pos);
        }
        candidates.sort(Comparator.comparingDouble(pos -> pos.distSqr(worldPosition)));
        List<BlockPos> wanted = new ArrayList<>(candidates.subList(0, Math.min(allow, candidates.size())));
        // 3) diff：先放掉不再申领的（只摘本终端的占位），再幂等刷新增补的
        for (BlockPos pos : claimedNodes) {
            if (!wanted.contains(pos)) {
                WirelessFieldManager.release(serverLevel, pos, worldPosition);
                setNodeClaimed(serverLevel, pos, false);
            }
        }
        for (BlockPos pos : wanted) {
            WirelessFieldManager.refresh(serverLevel, pos, NODE_FIELD_RADIUS, worldPosition);
            setNodeClaimed(serverLevel, pos, true);
        }
        claimedNodes.clear();
        claimedNodes.addAll(wanted);
    }

    /**
     * 写入节点的「已申领」可见状态与申领方归属者（子场域屏障与主场域同一套可见性口径）。
     * <p>
     * 只在<b>该区块已加载</b>时写：这只是外观状态，绝不能为了一次刷新触发同步加载/生成
     * （已申领但尚未加载的坐标，等票据把区块带上来后的下一轮自然会写）。
     * <p>
     * 改属性是同方块替换：原版 {@code LevelChunk#setBlockState} 对同方块只做 {@code setBlockState}、
     * 不重建方块实体，因此不会打断登记表的生命周期（节点的 {@code onRemove}/{@code onPlace} 都带
     * "方块是否真变了"的守卫）。
     */
    private void setNodeClaimed(ServerLevel level, BlockPos pos, boolean claimed) {
        ChunkPos cp = new ChunkPos(pos);
        if (level.getChunkSource().getChunkNow(cp.x, cp.z) == null) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AkaishiMiniMatrixNetworkNodeBlock)) {
            return;
        }
        if (state.getValue(AkaishiMiniMatrixNetworkNodeBlock.ACTIVE) != claimed) {
            level.setBlock(pos, state.setValue(AkaishiMiniMatrixNetworkNodeBlock.ACTIVE, claimed), 3);
        }
        if (level.getBlockEntity(pos) instanceof AkaishiMiniMatrixNetworkNodeBlockEntity node) {
            // 归属者只在申领时下发、释放时清掉；setClaimant 自身幂等，不会每轮刷包
            node.setClaimant(claimed ? fieldOwnerId() : null, claimed ? fieldOwnerName() : null);
        }
    }

    /**
     * 已申领坐标是否失效：区块已加载却不再是网络节点。
     * 未加载时<b>不下结论</b>（返回 false）—— 远处节点的区块本来就靠本终端的票据带上来，
     * 等它加载后的下一轮再复核。
     */
    private static boolean isStaleClaim(ServerLevel level, BlockPos pos) {
        ChunkPos cp = new ChunkPos(pos);
        if (level.getChunkSource().getChunkNow(cp.x, cp.z) == null) {
            return false;
        }
        return !(level.getBlockState(pos).getBlock() instanceof AkaishiMiniMatrixNetworkNodeBlock);
    }

    /** 释放全部已申领节点（场域归零 / 结构破损 / 终端被拆） */
    private void releaseNodes(ServerLevel serverLevel) {
        for (BlockPos pos : claimedNodes) {
            WirelessFieldManager.release(serverLevel, pos, worldPosition);
            setNodeClaimed(serverLevel, pos, false); // 节点"通电"外观要跟着灭掉，否则显示与场域不一致
        }
        claimedNodes.clear();
    }

    /**
     * 释放本终端登记的场域（方块被拆时由方块调用）。
     * <p>
     * 与弱加载票据同纪律：<b>不能挪进 {@code setRemoved()}</b> —— 区块卸载同样会触发它，
     * 那样一卸载就把场域放了，弱加载机制会自废。
     */
    public void releaseField() {
        if (level instanceof ServerLevel serverLevel) {
            WirelessFieldManager.release(serverLevel, worldPosition, worldPosition);
            releaseNodes(serverLevel);
        }
    }

    /**
     * 方块实体被移除：<b>只在服务端</b>终止在跑任务（加工）。
     * <p>
     * <b>为什么必须在这里终止</b>：{@code craftTask} 是纯内存状态、不落盘，
     * 而扣料与预扣手续费在启动时就已从芯片库/能量里划走。区块卸载或服务器停机时若不终止，
     * 那批料与费用会随任务一起蒸发（芯片 NBT 里已被扣掉），同时在途名额也永远收不回来。
     * {@link #abortCraft(ServerLevel)} 会 {@code fail} 任务：回填锁定材料并释放名额，
     * 任务对象自身带状态守卫（{@code RUNNING}/{@code WAITING}）故不会重复回填或重复释放。
     * <p>
     * <b>刻意不在这里释放场域/节点票据</b>：区块卸载同样会触发本方法，放这里会让弱加载机制
     * 一卸载就自废（见 {@link #releaseField()}，那才是拆机路径）。
     * <p>
     * 不另覆写 {@code onChunkUnloaded()}：原版 {@code LevelChunk#clearAllBlockEntities()}
     * （区块卸载唯一入口，见 {@code ServerLevel#unload(LevelChunk)}）已经保证先调
     * {@code onChunkUnloaded()} 再调 {@code setRemoved()}，两者同时覆写只会把终止动作做两遍。
     */
    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            abortCraft(serverLevel);
        }
        super.setRemoved();
    }

    /**
     * 内腔格放行规则：空气，或本族升级组件方块（升级件装在腔内而非墙面）。
     * 其余方块一律拒绝 —— 与旧的「内腔必须全空」等价，只是多放行了指定族。
     */
    public static boolean isInteriorAllowed(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.getBlock() instanceof AkaishiMiniMatrixUpgradeBlock;
    }

    /** 清点内腔的升级组件（下标 = 类型序数），未放行方块由结构校验拦下，此处只数放行族 */
    private void scanUpgrades(MatrixStructure.Result box) {
        Arrays.fill(upgradeCounts, 0);
        for (int x = box.min.getX() + 1; x < box.max.getX(); x++) {
            for (int y = box.min.getY() + 1; y < box.max.getY(); y++) {
                for (int z = box.min.getZ() + 1; z < box.max.getZ(); z++) {
                    Block block = level.getBlockState(new BlockPos(x, y, z)).getBlock();
                    if (block instanceof AkaishiMiniMatrixUpgradeBlock upgrade) {
                        upgradeCounts[upgrade.type().ordinal()]++;
                    }
                }
            }
        }
    }

    /**
     * 遍历箱体表面各格收集芯片。
     * <p>
     * 先去方块类型再取方块实体：墙面白名单里只有控制器与芯片是 EntityBlock，
     * 但仍显式判定，避免对无关方块触发方块实体创建。
     */
    private List<IMiniatureChipView> scanChips(MatrixStructure.Result box) {
        int minX = box.min.getX(), maxX = box.max.getX();
        int minY = box.min.getY(), maxY = box.max.getY();
        int minZ = box.min.getZ(), maxZ = box.max.getZ();
        Set<IMiniatureChipView> found = new LinkedHashSet<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (x != minX && x != maxX && y != minY && y != maxY && z != minZ && z != maxZ) {
                        continue; // 只看表面
                    }
                    BlockPos p = new BlockPos(x, y, z);
                    if (!(level.getBlockState(p).getBlock() instanceof AkaishiMiniatureTerminalBlock)) {
                        continue;
                    }
                    // LinkedHashSet 以方块实体实例为键：同一格重复扫到不会重复计数（幂等）
                    if (level.getBlockEntity(p) instanceof MiniatureTerminalBlockEntity chip && !chip.isRemoved()) {
                        found.add(chip);
                    }
                }
            }
        }
        return found.isEmpty() ? List.of() : List.copyOf(found);
    }

    /**
     * 墙面白名单（族隔离）：本族外壳与结构玻璃（二者复用矩阵族通用类，故按<b>方块实例</b>比对）、
     * 本控制器，以及贴放在墙面上的微缩终端（芯片）。
     * <p>
     * 单一真源：命令生成后的自检也调用本方法，避免两处白名单走样。
     */
    public static boolean isWallBlock(Block block) {
        return block == AkaishiMiniMatrixBlocks.CHISHI_MINI_MATRIX_CASING.get()
                || block == AkaishiMiniMatrixBlocks.CHISHI_MINI_MATRIX_STRUCTURE_GLASS.get()
                || block instanceof AkaishiMiniMatrixTerminalBlock
                || block instanceof AkaishiMiniatureTerminalBlock;
    }

    /** 结构是否成型（读方块状态，端口/命令与摘要共用） */
    public boolean isFormed() {
        return getBlockState().getValue(AkaishiMiniMatrixTerminalBlock.FORMED);
    }

    /**
     * 诊断「为什么不成型」：返回可读的多行回执（首行给箱体范围与不合规总数，随后逐条列出问题格）；
     * 已成型时返回 null。
     * <p>
     * 未成型原本是<b>静默</b>的（方块状态不变、界面空白），玩家无从知道是尺寸搭错、
     * 用了别族同贴图方块、还是内腔放了东西 —— 这里把<b>最接近成型</b>的那套箱体与问题格报出来，
     * 右键终端与 {@code /akaishi_miniature status} 都会带上。
     * <p>
     * 只在玩家主动询问时调用（代价见 {@link MatrixStructure#diagnose}），不进每 tick 重扫。
     * 必须在服务端主线程调用（读世界）；译名交给客户端渲染，服务端只带 key 与参数。
     */
    @Nullable
    public List<Component> describeStructureProblem() {
        if (isFormed() || level == null) {
            return null;
        }
        MatrixStructure.Failure failure = MatrixStructure.diagnose(level, worldPosition, SIZE,
                AkaishiMiniMatrixTerminalBlockEntity::isWallBlock,
                AkaishiMiniMatrixTerminalBlockEntity::isInteriorAllowed);
        if (failure == null) {
            return null; // 判定与状态不一致（如状态刚被拆毁），交给调用方回退到通用提示
        }
        List<Component> lines = new ArrayList<>(failure.problems.size() + 1);
        lines.add(Component.translatable("message.akaishi.mini_matrix.problem_header",
                posText(failure.min), posText(failure.max), String.valueOf(failure.total)));
        for (MatrixStructure.Problem problem : failure.problems) {
            Block found = level.getBlockState(problem.pos).getBlock();
            String key;
            if (problem.wall) {
                key = "message.akaishi.mini_matrix.problem_wall";
            } else if (found instanceof AkaishiMiniMatrixNetworkNodeBlock) {
                // 专属提示：网络节点名字同族、看着像内腔组件，是最容易被误放进内腔的方块
                key = "message.akaishi.mini_matrix.problem_node_in_cavity";
            } else {
                key = "message.akaishi.mini_matrix.problem_interior";
            }
            lines.add(Component.translatable(key, posText(problem.pos), found.getName()));
        }
        return lines;
    }

    /** 坐标文本「x, y, z」 */
    private static String posText(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    /** 墙面识别到的芯片（只读，最多滞后 {@link #RESCAN_INTERVAL} tick） */
    public List<IMiniatureChipView> chips() {
        return chips;
    }

    /** 识别到的芯片数量 */
    public int chipCount() {
        return chips.size();
    }

    /**
     * 已识别芯片里坐标等于 {@code pos} 的那一枚（打开它自己的界面用）。
     * <p>
     * 界面左列把"目标坐标"交给服务端，所以这里必须做白名单查询：
     * 只有矩阵当前识别到的芯片才允许被打开，否则改包的客户端能让服务端打开任意方块的界面。
     */
    @Nullable
    public MiniatureTerminalBlockEntity chipAt(BlockPos pos) {
        for (IMiniatureChipView chip : chips) {
            if (chip instanceof MiniatureTerminalBlockEntity be && be.getBlockPos().equals(pos)) {
                return be;
            }
        }
        return null;
    }

    /** 已申领的网络节点数量（联调回执用；实际票据已在 {@link WirelessFieldManager} 挂上） */
    public int claimedNodeCount() {
        return claimedNodes.size();
    }

    // ===== 内腔升级组件读数（供界面与后续阶段（场域/加工/联动）消费） =====

    /** 某类升级组件的内腔数量（0 = 未安装；上限见 {@link AkaishiMiniMatrixUpgradeType#maxCount()}） */
    public int upgradeCount(AkaishiMiniMatrixUpgradeType type) {
        return upgradeCounts[type.ordinal()];
    }

    /** 按序数读（网络快照按序数编码） */
    public int upgradeCount(int ordinal) {
        return ordinal >= 0 && ordinal < upgradeCounts.length ? upgradeCounts[ordinal] : 0;
    }

    /** 该类升级是否已安装（≥1 块） */
    public boolean hasUpgrade(AkaishiMiniMatrixUpgradeType type) {
        return upgradeCount(type) > 0;
    }

    /** 场域半径（区块）：0 = 未安装（无场域），1..3 = 内腔内装了几块场域升级 */
    public int fieldRadiusChunks() {
        return Math.min(upgradeCount(AkaishiMiniMatrixUpgradeType.FIELD),
                WirelessFieldManager.MAX_RADIUS_CHUNKS);
    }

    /**
     * 可申领的网络节点数：0..{@link AkaishiMiniMatrixUpgradeType#EXTEND}{@code .maxCount()}（=3）。
     * <p>
     * 内腔 3×3×3 最多能塞 27 块拓展升级，必须按升级声明的上限钳制：每个被申领的节点都会挂
     * 1 区块子场域的弱加载票据，不钳制就是票据预算失控。写法与 {@link #fieldRadiusChunks()} 同口径。
     */
    public int extendNodeCount() {
        return Math.min(upgradeCount(AkaishiMiniMatrixUpgradeType.EXTEND),
                AkaishiMiniMatrixUpgradeType.EXTEND.maxCount());
    }

    /**
     * 视图版本号：成型状态 / 芯片集合与读数 / 内腔升级数量任一变化即变。
     * <p>
     * 菜单按此比对决定是否重推快照 —— 逐字段深比较要遍历全部芯片，版本号只需一次整数运算；
     * 溢出只可能造成「本次该推没推」，下一次变化仍会推，不会丢状态。
     */
    public int viewRevision() {
        int revision = (isFormed() ? 1 : 0) * 31 + chips.size();
        for (IMiniatureChipView chip : chips) {
            revision = revision * 31 + chipFingerprint(chip);
        }
        for (int count : upgradeCounts) {
            revision = revision * 31 + count;
        }
        // 能源直供量也要进指纹：它是升级页显示的服务端权威值，
        // 不计入就会"看着还在供能、其实已停"（快照只在指纹变化时才下发）
        revision = revision * 31 + Long.hashCode(lastPushedEnergy);
        return revision;
    }

    /** 单枚芯片的读数指纹（短 ID + 装载态 + 存量）：只关心「界面看得见的变化」 */
    private static int chipFingerprint(IMiniatureChipView chip) {
        UUID id = chip.terminalId();
        int hash = (id == null ? 0 : id.hashCode()) * 31 + (chip.loaded() ? 1 : 0);
        IMiniatureChipView.IpReadout ip = chip.itemReadout();
        if (ip != null) {
            hash = hash * 31 + Long.hashCode(ip.stored());
        }
        IMiniatureChipView.EnergyReadout energy = chip.energyReadout();
        if (energy != null) {
            hash = hash * 31 + Long.hashCode(energy.stored());
        }
        return hash;
    }

    // ===== 安全（主卡统一入口） =====

    public TerminalSecurity security() {
        return security;
    }

    /** 记录归属者（控制器放置时由方块调用）；归属者恒全权限，不占权限表条目 */
    public void setOwner(UUID owner, String name) {
        security.setOwner(owner, name);
        // 立刻把归属者推给客户端：屏障可见性过滤要用（否则要等下一轮区块重发）
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** 屏障可见性过滤用归属者 ID（客户端读同步值，服务端读权威表） */
    @Nullable
    public UUID fieldOwnerId() {
        return fieldOwnerId != null ? fieldOwnerId : security.owner();
    }

    /** 屏障可见性过滤用归属者名（同队判定用名字比对队伍成员表，离线也能判；空名归一为 null） */
    @Nullable
    public String fieldOwnerName() {
        String name = fieldOwnerName != null ? fieldOwnerName : security.ownerName();
        return name == null || name.isEmpty() ? null : name;
    }

    /**
     * 把矩阵当前安全表同步写入每一枚绑定芯片（口径 5：一次登记，绑定的终端一同生效）。
     * <p>
     * 空壳与无安全表者由 {@link MiniatureTerminalBlockEntity#adoptSecurity} 自行跳过，
     * 故返回值是<b>实际写入成功</b>的芯片数，供回执如实显示。
     */
    public int applySecurityToChips() {
        int applied = 0;
        for (IMiniatureChipView chip : chips) {
            if (chip instanceof MiniatureTerminalBlockEntity be && be.adoptSecurity(security)) {
                applied++;
            }
        }
        return applied;
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        security.save(tag);
        // 内腔升级组件无需持久化：它们是世界里的方块，重扫时从结构读出
        // 已申领节点：必须落盘 —— 节点登记表是内存表且靠区块加载驱动，远处节点重启后不在表里
        if (!claimedNodes.isEmpty()) {
            tag.putLongArray(TAG_CLAIMED_NODES, claimedNodes.stream().mapToLong(BlockPos::asLong).toArray());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        security.load(tag);
        // 客户端：承载屏障可见性过滤所需的归属者（服务端存档里没有这两个键，故服务端保持 null）
        if (tag.hasUUID(TAG_FIELD_OWNER)) {
            fieldOwnerId = tag.getUUID(TAG_FIELD_OWNER);
        }
        if (tag.contains(TAG_FIELD_OWNER_NAME)) {
            fieldOwnerName = tag.getString(TAG_FIELD_OWNER_NAME);
        }
        // 已申领节点：读档即恢复申领名单，首轮重扫据此重新挂票据（远处节点因此被弱加载并重新登记）
        claimedNodes.clear();
        for (long packed : tag.getLongArray(TAG_CLAIMED_NODES)) {
            claimedNodes.add(BlockPos.of(packed));
        }
    }

    /**
     * 方块实体同步标签：只带归属者两项（<b>不</b>带整张安全表）。
     * <p>
     * 场域屏障是纯客户端表现，只有"仅归属者/同队可见"这一条配置需要知道归属者；
     * 为此同步整张权限表代价过大，故只带 ID 与名字。
     */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        UUID owner = security.owner();
        if (owner != null) {
            tag.putUUID(TAG_FIELD_OWNER, owner);
        }
        String name = security.ownerName();
        if (name != null && !name.isEmpty()) {
            tag.putString(TAG_FIELD_OWNER_NAME, name);
        }
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ===== ExtendedMenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_mini_matrix_terminal");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiMiniMatrixTerminalMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }
}
