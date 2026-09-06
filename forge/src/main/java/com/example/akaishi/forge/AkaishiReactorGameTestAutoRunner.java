package com.example.akaishi.forge;

import com.example.akaishi.block.AkaishiReactorBlocks;
import com.example.akaishi.block.entity.AkaishiReactorControllerBlockEntity;
import com.example.akaishi.block.entity.AkaishiReactorCoolerBlockEntity;
import com.example.akaishi.block.entity.AkaishiReactorEnergyOutputBlockEntity;
import com.example.akaishi.block.entity.AkaishiReactorFuelPortBlockEntity;
import com.example.akaishi.block.entity.AkaishiReactorWastePortBlockEntity;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.fluid.ModFluids;
import com.example.akaishi.item.AkaishiFuelCellItem;
import com.example.akaishi.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * dev 环境反应堆体系端到端自动测试（以 -Dakaishi.gametest.reactor=1 启动服务端触发）。
 * 在出生点区块搭建 5×5×5 单层封闭长方体反应堆（控制器顶面中心），分 4 阶段验证：
 * <p>
 * 0. 稳定燃烧：2 燃料棒 + 4 散热片（劣质 1%）+ 1 核心，注入满罐世界基础燃料 →
 *    成型、温度平衡约 417℃（2 棒基础热值 120 / 散热 4%）、能量产出、废品生成、散热片耐久消耗
 * 1. 拆件停机：拆除一角外壳 → 结构失效，立即停止燃烧、温度回落、能量停增
 * 2. 超温警告：重建 10 燃料棒 + 0 散热 → 温度冲过 850℃ 警告线并直达 1000℃ 满热量
 *    → 进入高温警告（不停机、继续燃烧减产）并启动 10 秒爆炸倒计时
 * 3. 倒计时继续：爆炸倒计时应持续递减且反应堆保持运行（本测试在爆炸前结束）
 * <p>
 * 全部通过打印 PASS，存在失败打印 FAIL 明细，随后自动关闭服务端（便于脚本检查日志）。
 */
public final class AkaishiReactorGameTestAutoRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AkaishiReactorGameTestAutoRunner.class);

    /** 各阶段结算等待 tick */
    private static final int TICKS_PHASE0 = 200;
    private static final int TICKS_PHASE1 = 40;
    private static final int TICKS_PHASE2 = 150;
    private static final int TICKS_PHASE3 = 30;

    // ===== 5×5×5 结构坐标（相对 base）=====
    private static final BlockPos MIN = new BlockPos(0, 0, 0);
    private static final BlockPos MAX = new BlockPos(4, 4, 4);
    private static final BlockPos CONTROLLER = new BlockPos(2, 4, 2);
    private static final BlockPos ENERGY_OUT = new BlockPos(0, 2, 2);
    private static final BlockPos WASTE_OUT = new BlockPos(4, 2, 2);
    private static final BlockPos FUEL_PORT = new BlockPos(2, 0, 2);
    private static final BlockPos CORNER_SHELL = new BlockPos(0, 0, 0);
    private static final BlockPos CORE = new BlockPos(2, 2, 2);
    /** 阶段 A 燃料棒（2 根） */
    private static final BlockPos[] RODS_A = {new BlockPos(1, 2, 2), new BlockPos(3, 2, 2)};
    /** 阶段 A 散热组件（4 个，均贴邻燃料棒） */
    private static final BlockPos[] COOLERS_A = {
            new BlockPos(1, 2, 1), new BlockPos(1, 2, 3),
            new BlockPos(3, 2, 1), new BlockPos(3, 2, 3)};

    private static MinecraftServer server;
    private static ServerLevel level;
    private static BlockPos base;

    private static int phase = -1;
    private static int ticks = 0;
    private static int passed = 0;
    private static int total = 0;
    private static boolean finished = false;

    // 阶段间传递的快照
    private static int phase0Temp;
    private static long phase0Energy;
    private static int phase2Countdown;

    private AkaishiReactorGameTestAutoRunner() {
    }

    /** 服务端启动后调用：搭建初始结构并注入燃料 */
    public static void start(MinecraftServer srv) {
        if (finished) {
            return;
        }
        server = srv;
        level = server.overworld();
        if (level == null) {
            LOGGER.error("[ReactorTest] 主世界未就绪，跳过测试");
            server.halt(false);
            return;
        }
        BlockPos spawn = level.getSharedSpawnPos();
        // 5×5×5 结构，置于出生点区块上方偏移处
        base = new BlockPos(spawn.getX() + 8, spawn.getY() + 4, spawn.getZ() + 8);
        // 用 FORCED ticket（=forceload 命令）把结构覆盖的区块强制加载到实体 tick 状态，
        // 否则无玩家在线时 spawn chunk 会降级，方块实体停止 tick（结构扫描不执行）。
        // PORTAL ticket 只加载到 FULL 状态、不做实体 tick，故改用 setChunkForced。
        net.minecraft.world.level.ChunkPos minC = new net.minecraft.world.level.ChunkPos(at(MIN));
        net.minecraft.world.level.ChunkPos maxC = new net.minecraft.world.level.ChunkPos(at(MAX));
        for (int cx = minC.x; cx <= maxC.x; cx++) {
            for (int cz = minC.z; cz <= maxC.z; cz++) {
                level.setChunkForced(cx, cz, true);
                level.getChunk(cx, cz); // 同步加载，确保后续 setBlock 落在已加载区块
            }
        }

        buildPhaseA();
        phase = 0;
        ticks = 0;
        LOGGER.info("[ReactorTest] 阶段0：稳定燃烧（2 棒 + 4 劣质散热），等待 {} tick 结算", TICKS_PHASE0);
    }

    /** 每 tick 驱动（服务端 tick END 阶段调用） */
    public static void tick() {
        if (finished || phase < 0 || server == null) {
            return;
        }
        ticks++;
        if (ticks < phaseTicks(phase)) {
            return;
        }
        switch (phase) {
            case 0 -> runPhase0();
            case 1 -> runPhase1();
            case 2 -> runPhase2();
            case 3 -> runPhase3();
            default -> finish();
        }
    }

    private static int phaseTicks(int p) {
        return switch (p) {
            case 0 -> TICKS_PHASE0;
            case 1 -> TICKS_PHASE1;
            case 2 -> TICKS_PHASE2;
            case 3 -> TICKS_PHASE3;
            default -> 1;
        };
    }

    // ===== 结构搭建 =====

    /** 完整 5×5×5 外壳：默认外壳块，特殊位置覆盖控制器/输出口/废品口/投放口 */
    private static void buildShell() {
        for (BlockPos p : BlockPos.betweenClosed(MIN, MAX)) {
            if (!isWall(p)) {
                continue;
            }
            if (p.equals(CONTROLLER)) {
                setBlock(p, AkaishiReactorBlocks.CHISHI_REACTOR_CONTROLLER.get());
            } else if (p.equals(ENERGY_OUT)) {
                setBlock(p, AkaishiReactorBlocks.CHISHI_REACTOR_ENERGY_OUTPUT.get());
            } else if (p.equals(WASTE_OUT)) {
                setBlock(p, AkaishiReactorBlocks.CHISHI_REACTOR_WASTE_PORT.get());
            } else if (p.equals(FUEL_PORT)) {
                setBlock(p, AkaishiReactorBlocks.CHISHI_REACTOR_FUEL_PORT.get());
            } else {
                setBlock(p, AkaishiReactorBlocks.CHISHI_REACTOR_SHELL.get());
            }
        }
    }

    private static boolean isWall(BlockPos p) {
        return p.getX() == MIN.getX() || p.getX() == MAX.getX()
                || p.getY() == MIN.getY() || p.getY() == MAX.getY()
                || p.getZ() == MIN.getZ() || p.getZ() == MAX.getZ();
    }

    /** 阶段 A：2 燃料棒 + 4 散热组件（插劣质散热片）+ 1 核心 */
    private static void buildPhaseA() {
        buildShell();
        setBlock(CORE, AkaishiReactorBlocks.CHISHI_REACTOR_CORE.get());
        for (BlockPos rod : RODS_A) {
            setBlock(rod, AkaishiReactorBlocks.CHISHI_REACTOR_FUEL_ROD.get());
        }
        for (BlockPos cooler : COOLERS_A) {
            setBlock(cooler, AkaishiReactorBlocks.CHISHI_REACTOR_COOLER.get());
            // getBlockEntity 需用绝对坐标（at = base.offset），否则查到世界原点附近空位置
            if (level.getBlockEntity(at(cooler)) instanceof AkaishiReactorCoolerBlockEntity c) {
                c.insertHeatSink(new ItemStack(ModItems.heatSinkPoor.get()));
            }
        }
        // 通过燃料投放口注入 2 个满罐世界基础燃料（验证自动分配逻辑）
        AkaishiReactorFuelPortBlockEntity port = asFuelPort();
        if (port != null) {
            port.insertCell(fullCell(ModFluids.SCULK_LIFE_FUEL_ID));
            port.insertCell(fullCell(ModFluids.SCULK_LIFE_FUEL_ID));
        }
    }

    /** 阶段 D：10 燃料棒 + 0 散热 + 1 核心（用于超温警告 + 爆炸倒计时） */
    private static void buildPhaseD() {
        buildShell(); // 补回阶段 1 拆掉的墙角
        // 清空内腔（含旧棒/散热/核心）
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(1, 1, 1), new BlockPos(3, 3, 3))) {
            setBlock(p, Blocks.AIR);
        }
        setBlock(CORE, AkaishiReactorBlocks.CHISHI_REACTOR_CORE.get());
        // 10 棒：y=1 层 9 个 + (1,2,1)
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                setBlock(new BlockPos(x, 1, z), AkaishiReactorBlocks.CHISHI_REACTOR_FUEL_ROD.get());
            }
        }
        setBlock(new BlockPos(1, 2, 1), AkaishiReactorBlocks.CHISHI_REACTOR_FUEL_ROD.get());
        // 直接注入控制器燃料槽（10 棒终极混合 → 无散热必爆）
        AkaishiReactorControllerBlockEntity controller = asController();
        if (controller != null) {
            for (int i = 0; i < 10; i++) {
                controller.fuelSlots().setItem(i, fullCell(ModFluids.ULTIMATE_MIXTURE_FUEL_ID));
            }
        }
    }

    // ===== 阶段结算 =====

    private static void runPhase0() {
        AkaishiReactorControllerBlockEntity c = asController();
        if (c == null) {
            LOGGER.error("[ReactorTest] 阶段0：控制器丢失，测试中止");
            finish();
            return;
        }
        LOGGER.info("[ReactorTest] ===== 阶段0 断言（稳定燃烧） =====");
        check(c.isFormed(), "[成型] 5×5×5 结构应判定成型");
        check(c.getRodCount() == 2, "[成型] 燃料棒应为 2，实际 " + c.getRodCount());
        check(c.data().get(AkaishiReactorControllerBlockEntity.DATA_EFFECTIVE_COOLERS) == 4,
                "[成型] 有效散热组件应为 4，实际 " + c.data().get(AkaishiReactorControllerBlockEntity.DATA_EFFECTIVE_COOLERS));
        check(c.data().get(AkaishiReactorControllerBlockEntity.DATA_COOLING_PERCENT) == 4,
                "[成型] 散热百分比应为 4%，实际 " + c.data().get(AkaishiReactorControllerBlockEntity.DATA_COOLING_PERCENT));

        int temp = c.data().get(AkaishiReactorControllerBlockEntity.DATA_TEMP);
        check(temp >= 400 && temp <= 470,
                "[燃烧] 温度应平衡在约 417℃（世界基础燃料热值 120 × 2 + 散热 4%），实际 " + temp);
        check(c.data().get(AkaishiReactorControllerBlockEntity.DATA_ACTIVE_SLOTS) == 2,
                "[燃烧] 活跃槽应为 2，实际 " + c.data().get(AkaishiReactorControllerBlockEntity.DATA_ACTIVE_SLOTS));

        AkaishiReactorEnergyOutputBlockEntity energy = asEnergyOut();
        long stored = energy == null ? 0 : energy.energy().getEnergyStored();
        check(energy != null && stored > 1_000_000,
                "[燃烧] 能量输出口应有产出，实际 " + stored);
        AkaishiReactorWastePortBlockEntity waste = asWasteOut();
        long wasteAmt = waste == null ? 0 : waste.wasteTank().getAmount();
        check(waste != null && wasteAmt > 0,
                "[燃烧] 废品输出口应有衰竭燃料（5mb→1mb），实际 " + wasteAmt);

        ItemStack cell = c.fuelSlots().getItem(0);
        int amount = AkaishiFuelCellItem.getAmount(cell);
        check(amount > 0 && amount < 10_000,
                "[燃烧] 燃料罐应被消耗，实际剩 " + amount);

        boolean allDamaged = true;
        for (BlockPos cooler : COOLERS_A) {
            if (level.getBlockEntity(at(cooler)) instanceof AkaishiReactorCoolerBlockEntity cc) {
                if (cc.getItem(AkaishiReactorCoolerBlockEntity.SINK_SLOT).getDamageValue() <= 0) {
                    allDamaged = false;
                }
            }
        }
        check(allDamaged, "[散热] 4 片散热片耐久应已被消耗");

        phase0Temp = temp;
        phase0Energy = stored;
        // 进入阶段 1：拆墙角外壳
        setBlock(CORNER_SHELL, Blocks.AIR);
        phase = 1;
        ticks = 0;
        LOGGER.info("[ReactorTest] 阶段1：拆除墙角外壳，等待 {} tick 结算", TICKS_PHASE1);
    }

    private static void runPhase1() {
        AkaishiReactorControllerBlockEntity c = asController();
        if (c == null) {
            LOGGER.error("[ReactorTest] 阶段1：控制器丢失，测试中止");
            finish();
            return;
        }
        LOGGER.info("[ReactorTest] ===== 阶段1 断言（拆件停机） =====");
        check(!c.isFormed(), "[停机] 拆墙后结构应失效");
        check(c.data().get(AkaishiReactorControllerBlockEntity.DATA_ACTIVE_SLOTS) == 0,
                "[停机] 活跃槽应为 0，实际 " + c.data().get(AkaishiReactorControllerBlockEntity.DATA_ACTIVE_SLOTS));
        int temp = c.data().get(AkaishiReactorControllerBlockEntity.DATA_TEMP);
        check(temp < phase0Temp - 50,
                "[停机] 温度应回落（拆前 " + phase0Temp + "，当前 " + temp + "）");
        long stored = asEnergyOut() == null ? 0 : asEnergyOut().energy().getEnergyStored();
        check(stored <= phase0Energy + 1,
                "[停机] 能量应停止增长（拆前 " + phase0Energy + "，当前 " + stored + "）");

        // 进入阶段 2：重建 10 棒 0 散热结构
        buildPhaseD();
        phase = 2;
        ticks = 0;
        LOGGER.info("[ReactorTest] 阶段2：超温警告 + 爆炸倒计时（10 棒 + 0 散热），等待 {} tick 结算", TICKS_PHASE2);
    }

    private static void runPhase2() {
        AkaishiReactorControllerBlockEntity c = asController();
        if (c == null) {
            LOGGER.error("[ReactorTest] 阶段2：控制器丢失，测试中止");
            finish();
            return;
        }
        LOGGER.info("[ReactorTest] ===== 阶段2 断言（超温警告 + 爆炸倒计时） =====");
        int temp = c.data().get(AkaishiReactorControllerBlockEntity.DATA_TEMP);
        check(c.isWarning(), "[警告] 10 棒 0 散热应进入高温警告，当前温度 " + temp);
        check(c.data().get(AkaishiReactorControllerBlockEntity.DATA_ACTIVE_SLOTS) == 10,
                "[警告] 警告期间应继续燃烧（不停机），活跃槽应为 10，实际 "
                        + c.data().get(AkaishiReactorControllerBlockEntity.DATA_ACTIVE_SLOTS));
        check(temp >= ModConfig.reactorTempWarn,
                "[警告] 温度应超过警告线（850），实际 " + temp);
        int countdown = c.getExplosionCountdown();
        check(countdown > 0 && countdown < ModConfig.reactorExplosionDelayTicks,
                "[爆炸] 满热量后应启动爆炸倒计时，剩余 " + countdown + " tick");

        phase2Countdown = countdown;
        phase = 3;
        ticks = 0;
        LOGGER.info("[ReactorTest] 阶段3：验证爆炸倒计时继续递减，等待 {} tick 结算", TICKS_PHASE3);
    }

    private static void runPhase3() {
        AkaishiReactorControllerBlockEntity c = asController();
        if (c == null) {
            LOGGER.error("[ReactorTest] 阶段3：控制器丢失，测试中止");
            finish();
            return;
        }
        LOGGER.info("[ReactorTest] ===== 阶段3 断言（倒计时继续，未提前爆炸） =====");
        int countdown = c.getExplosionCountdown();
        check(countdown > 0, "[爆炸] 倒计时应仍大于 0（本测试应在爆炸前结束），剩余 " + countdown);
        check(countdown < phase2Countdown,
                "[爆炸] 倒计时应持续递减（阶段2 " + phase2Countdown + "，当前 " + countdown + "）");
        check(c.data().get(AkaishiReactorControllerBlockEntity.DATA_ACTIVE_SLOTS) == 10,
                "[警告] 爆炸前反应堆应保持运行，活跃槽应为 10，实际 "
                        + c.data().get(AkaishiReactorControllerBlockEntity.DATA_ACTIVE_SLOTS));
        finish();
    }

    // ===== 工具 =====

    private static ItemStack fullCell(String fluidId) {
        ItemStack cell = new ItemStack(ModItems.fuelCell.get());
        AkaishiFuelCellItem.setFluid(cell, ModFluids.get(fluidId), AkaishiFuelCellItem.CAPACITY);
        return cell;
    }

    private static void setBlock(BlockPos local, Block block) {
        level.setBlock(base.offset(local), block.defaultBlockState(), 3);
    }

    private static BlockPos at(BlockPos local) {
        return base.offset(local);
    }

    private static AkaishiReactorControllerBlockEntity asController() {
        return level.getBlockEntity(at(CONTROLLER)) instanceof AkaishiReactorControllerBlockEntity be ? be : null;
    }

    private static AkaishiReactorEnergyOutputBlockEntity asEnergyOut() {
        return level.getBlockEntity(at(ENERGY_OUT)) instanceof AkaishiReactorEnergyOutputBlockEntity be ? be : null;
    }

    private static AkaishiReactorWastePortBlockEntity asWasteOut() {
        return level.getBlockEntity(at(WASTE_OUT)) instanceof AkaishiReactorWastePortBlockEntity be ? be : null;
    }

    private static AkaishiReactorFuelPortBlockEntity asFuelPort() {
        return level.getBlockEntity(at(FUEL_PORT)) instanceof AkaishiReactorFuelPortBlockEntity be ? be : null;
    }

    private static void check(boolean condition, String message) {
        total++;
        if (condition) {
            passed++;
            LOGGER.info("[ReactorTest]   [PASS] {}", message);
        } else {
            LOGGER.error("[ReactorTest]   [FAIL] {}", message);
        }
    }

    private static void finish() {
        if (finished) {
            return;
        }
        finished = true;
        LOGGER.info("[ReactorTest] 汇总: 通过 {} / 总 {} {}", passed, total,
                passed == total ? "（全部通过）" : "（存在失败）");
        server.halt(false);
    }
}
