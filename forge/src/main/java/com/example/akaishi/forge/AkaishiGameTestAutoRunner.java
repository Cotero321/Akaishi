package com.example.akaishi.forge;

import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.block.entity.AkaishiFuelCannerBlockEntity;
import com.example.akaishi.block.entity.AkaishiFuelMixerBlockEntity;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.fluid.FluidTank;
import com.example.akaishi.fluid.ModFluids;
import com.example.akaishi.item.AkaishiFuelCellItem;
import com.example.akaishi.item.ModItems;
import dev.architectury.fluid.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * dev 环境燃料系统端到端自动测试（以 -Dakaishi.gametest=1 启动服务端触发）。
 * 不走 GameTest 结构文件，直接在真实世界中放置装罐机 + 混合器（出生点区块保证实时 tick），
 * 通过方块实体 API 注入液体/能量，驱动真实 tick 验证：
 * 1. 装罐机：5000mb 终极混合燃料 → 半满罐（5000/10000 留在输入槽，未满不出）
 * 2. 混合器：巨龙 100mb + 至纯 100mb + 2M 赤能源 → 终极混合燃料 50mb（1:1:1 消耗）
 * 3. 补液 5000mb → 灌满 10000 → 满罐送出、输入槽空
 * 全部通过打印 PASS，存在失败打印 FAIL 明细，随后自动关闭服务端（便于脚本检查日志）。
 */
public final class AkaishiGameTestAutoRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AkaishiGameTestAutoRunner.class);

    /** 每个场景的结算等待 tick（灌装 5 tick + 混合 2 tick，20 tick 冗余足够） */
    private static final int TICKS_PER_PHASE = 20;

    private static MinecraftServer server;
    private static ServerLevel level;
    private static BlockPos cannerPos;
    private static BlockPos mixerPos;

    private static int phase = -1;   // 0=场景1a+2 结算，1=场景1b 结算
    private static int ticks = 0;
    private static int passed = 0;
    private static int total = 0;
    private static boolean finished = false;

    private AkaishiGameTestAutoRunner() {
    }

    /** 服务端启动后调用：放置机器并注入初始状态 */
    public static void start(MinecraftServer srv) {
        if (finished) {
            return;
        }
        server = srv;
        level = server.overworld();
        if (level == null) {
            LOGGER.error("[AkaishiTest] 主世界未就绪，跳过测试");
            server.halt(false);
            return;
        }
        // 放在世界出生点附近（出生点区块必然实时 tick），偏移避免干扰
        BlockPos spawn = level.getSharedSpawnPos();
        cannerPos = new BlockPos(spawn.getX() + 8, spawn.getY() + 4, spawn.getZ() + 8);
        mixerPos = cannerPos.offset(0, 0, 3);
        level.setBlock(cannerPos, ModBlocks.CHISHI_FUEL_CANNER.get().defaultBlockState(), 3);
        level.setBlock(mixerPos, ModBlocks.CHISHI_FUEL_MIXER.get().defaultBlockState(), 3);

        AkaishiFuelCannerBlockEntity canner = asCanner();
        AkaishiFuelMixerBlockEntity mixer = asMixer();
        if (canner == null || mixer == null) {
            LOGGER.error("[AkaishiTest] 机器方块实体未生成，跳过测试");
            server.halt(false);
            return;
        }
        Fluid ultimate = ModFluids.get(ModFluids.ULTIMATE_MIXTURE_FUEL_ID);
        Fluid dragon = ModFluids.get(ModFluids.DRAGON_FUEL_ID);
        Fluid pure = ModFluids.get(ModFluids.PURE_FUEL_ID);

        // ===== 场景 1a：装罐机灌装 5000mb → 半满罐 =====
        canner.getLiquidTank().setStack(FluidStack.create(ultimate, 5000));
        canner.inventory().setItem(AkaishiFuelCannerBlockEntity.INPUT_SLOT,
                new ItemStack(ModItems.fuelCell.get()));

        // ===== 场景 2：混合器终极配方（巨龙 100 + 至纯 100 + 2M 能量） =====
        List<FluidTank> tanks = mixer.getFluidTanks(); // [in1, in2, out]
        tanks.get(0).setStack(FluidStack.create(dragon, 100));
        tanks.get(1).setStack(FluidStack.create(pure, 100));
        ((AkaishiEnergyStorage) mixer.getEnergyStorage()).setEnergy(ModConfig.fuelMixerChishiCost);

        phase = 0;
        ticks = 0;
        passed = 0;
        total = 0;
        LOGGER.info("[AkaishiTest] 测试开始：装罐机 5000mb + 混合器终极配方，等待 {} tick 结算", TICKS_PER_PHASE);
    }

    /** 每 tick 驱动（服务端 tick END 阶段调用） */
    public static void tick() {
        if (finished || phase < 0 || server == null) {
            return;
        }
        ticks++;
        if (ticks < TICKS_PER_PHASE) {
            return;
        }
        switch (phase) {
            case 0 -> runPhase0();
            case 1 -> runPhase1();
            default -> finish();
        }
    }

    /** 场景 1a + 场景 2 断言，随后注入场景 1b 初始状态 */
    private static void runPhase0() {
        AkaishiFuelCannerBlockEntity canner = asCanner();
        AkaishiFuelMixerBlockEntity mixer = asMixer();
        if (canner == null || mixer == null) {
            LOGGER.error("[AkaishiTest] 阶段1：方块实体丢失，测试中止");
            finish();
            return;
        }
        Fluid ultimate = ModFluids.get(ModFluids.ULTIMATE_MIXTURE_FUEL_ID);
        List<FluidTank> tanks = mixer.getFluidTanks();

        LOGGER.info("[AkaishiTest] ===== 阶段1 断言（半满罐 + 终极混合） =====");
        ItemStack input = canner.inventory().getItem(AkaishiFuelCannerBlockEntity.INPUT_SLOT);
        ItemStack output = canner.inventory().getItem(AkaishiFuelCannerBlockEntity.OUTPUT_SLOT);
        check(!input.isEmpty(), "[装罐机] 输入槽应保留半满燃料罐");
        check(AkaishiFuelCellItem.getAmount(input) == 5000,
                "[装罐机] 半满罐应为 5000/10000，实际 " + AkaishiFuelCellItem.getAmount(input));
        check(AkaishiFuelCellItem.getFluid(input) == ultimate, "[装罐机] 罐内应为终极混合燃料");
        check(canner.getLiquidTank().isEmpty(),
                "[装罐机] 液体罐应被抽空，实际剩 " + canner.getLiquidTank().getAmount());
        check(output.isEmpty(), "[装罐机] 未满罐不应送出");

        check(tanks.get(2).getFluid() == ultimate && tanks.get(2).getAmount() == 50,
                "[混合器] 输出罐应为终极混合燃料 50mb，实际 " + tanks.get(2).getAmount());
        check(tanks.get(0).getAmount() == 50, "[混合器] 输入1应剩 50mb，实际 " + tanks.get(0).getAmount());
        check(tanks.get(1).getAmount() == 50, "[混合器] 输入2应剩 50mb，实际 " + tanks.get(1).getAmount());

        // ===== 场景 1b：补液 5000 → 灌满 → 满罐送出 =====
        canner.getLiquidTank().setStack(FluidStack.create(ultimate, 5000));
        phase = 1;
        ticks = 0;
        LOGGER.info("[AkaishiTest] 场景1b：补液 5000mb，等待 {} tick 结算", TICKS_PER_PHASE);
    }

    /** 场景 1b 断言（灌满送出） */
    private static void runPhase1() {
        AkaishiFuelCannerBlockEntity canner = asCanner();
        if (canner == null) {
            LOGGER.error("[AkaishiTest] 阶段2：装罐机方块实体丢失，测试中止");
            finish();
            return;
        }
        LOGGER.info("[AkaishiTest] ===== 阶段2 断言（补液灌满送出） =====");
        ItemStack out = canner.inventory().getItem(AkaishiFuelCannerBlockEntity.OUTPUT_SLOT);
        check(!out.isEmpty(), "[装罐机] 输出槽应有满罐");
        check(AkaishiFuelCellItem.getAmount(out) == AkaishiFuelCellItem.CAPACITY,
                "[装罐机] 满罐应为 10000，实际 " + AkaishiFuelCellItem.getAmount(out));
        check(canner.inventory().getItem(AkaishiFuelCannerBlockEntity.INPUT_SLOT).isEmpty(),
                "[装罐机] 灌满后输入槽应被消耗");
        finish();
    }

    private static void check(boolean condition, String message) {
        total++;
        if (condition) {
            passed++;
            LOGGER.info("[AkaishiTest]   [PASS] {}", message);
        } else {
            LOGGER.error("[AkaishiTest]   [FAIL] {}", message);
        }
    }

    private static void finish() {
        if (finished) {
            return;
        }
        finished = true;
        LOGGER.info("[AkaishiTest] 汇总: 通过 {} / 总 {} {}", passed, total,
                passed == total ? "（全部通过）" : "（存在失败）");
        server.halt(false);
    }

    private static AkaishiFuelCannerBlockEntity asCanner() {
        return level.getBlockEntity(cannerPos) instanceof AkaishiFuelCannerBlockEntity be ? be : null;
    }

    private static AkaishiFuelMixerBlockEntity asMixer() {
        return level.getBlockEntity(mixerPos) instanceof AkaishiFuelMixerBlockEntity be ? be : null;
    }
}
