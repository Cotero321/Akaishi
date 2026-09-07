package com.example.akaishi.gametest;

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
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import java.util.List;

/**
 * 燃料系统端到端测试（GameTest，dev 环境以 -Dakaishi.gametest=1 触发）。
 * 单结构内放置装罐机 + 混合器，顺序验证：
 * 1. 装罐机：5000mb 终极混合燃料 → 半满罐（5000/10000 留在输入槽）；补液灌满 → 满罐送出
 * 2. 混合器：巨龙 100mb + 至纯 100mb + 2M 赤能源 → 终极混合燃料 50mb（1:1:1 消耗）
 */
public final class AkaishiFuelSystemTests {

    private AkaishiFuelSystemTests() {
    }

    @GameTest(template = "empty")
    public static void fuelSystemEndToEnd(GameTestHelper helper) {
        BlockPos cannerPos = new BlockPos(0, 0, 0);
        BlockPos mixerPos = new BlockPos(0, 0, 3);
        helper.setBlock(cannerPos, ModBlocks.CHISHI_FUEL_CANNER.get());
        helper.setBlock(mixerPos, ModBlocks.CHISHI_FUEL_MIXER.get());
        helper.runAfterDelay(1, () -> {
            AkaishiFuelCannerBlockEntity canner = asCanner(helper, cannerPos);
            AkaishiFuelMixerBlockEntity mixer = asMixer(helper, mixerPos);
            if (canner == null || mixer == null) {
                helper.fail("机器方块实体未生成");
                return;
            }
            Fluid ultimate = ModFluids.get(ModFluids.ULTIMATE_MIXTURE_FUEL_ID);

            // ===== 场景 1a：装罐机灌装 5000mb → 半满罐 =====
            canner.getLiquidTank().setStack(FluidStack.create(ultimate, 5000));
            canner.inventory().setItem(AkaishiFuelCannerBlockEntity.INPUT_SLOT,
                    new ItemStack(ModItems.fuelCell.get()));

            // ===== 场景 2：混合器终极配方 =====
            Fluid dragon = ModFluids.get(ModFluids.DRAGON_FUEL_ID);
            Fluid pure = ModFluids.get(ModFluids.PURE_FUEL_ID);
            List<FluidTank> tanks = mixer.getFluidTanks(); // [in1, in2, out]
            tanks.get(0).setStack(FluidStack.create(dragon, 100));
            tanks.get(1).setStack(FluidStack.create(pure, 100));
            ((AkaishiEnergyStorage) mixer.getEnergyStorage()).setEnergy(ModConfig.fuelMixerChishiCost);

            // 灌装 5 tick + 结算 2 tick，等 20 tick 冗余后统一验证
            helper.runAfterDelay(20, () -> {
                ItemStack input = canner.inventory().getItem(AkaishiFuelCannerBlockEntity.INPUT_SLOT);
                ItemStack output = canner.inventory().getItem(AkaishiFuelCannerBlockEntity.OUTPUT_SLOT);
                helper.assertTrue(!input.isEmpty(), "[装罐机] 输入槽应保留半满燃料罐");
                helper.assertTrue(AkaishiFuelCellItem.getAmount(input) == 5000,
                        "[装罐机] 半满罐应为 5000/10000，实际 " + AkaishiFuelCellItem.getAmount(input));
                helper.assertTrue(AkaishiFuelCellItem.getFluid(input) == ultimate, "[装罐机] 罐内应为终极混合燃料");
                helper.assertTrue(canner.getLiquidTank().isEmpty(), "[装罐机] 液体罐应被抽空");
                helper.assertTrue(output.isEmpty(), "[装罐机] 未满罐不应送出");

                helper.assertTrue(tanks.get(2).getFluid() == ultimate && tanks.get(2).getAmount() == 50,
                        "[混合器] 输出罐应为终极混合燃料 50mb，实际 " + tanks.get(2).getAmount());
                helper.assertTrue(tanks.get(0).getAmount() == 50, "[混合器] 输入1应剩 50mb");
                helper.assertTrue(tanks.get(1).getAmount() == 50, "[混合器] 输入2应剩 50mb");

                // ===== 场景 1b：补液 5000 → 灌满 → 满罐送出，输入槽空 =====
                canner.getLiquidTank().setStack(FluidStack.create(ultimate, 5000));
                helper.runAfterDelay(20, () -> {
                    ItemStack out = canner.inventory().getItem(AkaishiFuelCannerBlockEntity.OUTPUT_SLOT);
                    helper.assertTrue(!out.isEmpty(), "[装罐机] 输出槽应有满罐");
                    helper.assertTrue(AkaishiFuelCellItem.getAmount(out) == AkaishiFuelCellItem.CAPACITY,
                            "[装罐机] 满罐应为 10000，实际 " + AkaishiFuelCellItem.getAmount(out));
                    helper.assertTrue(canner.inventory().getItem(AkaishiFuelCannerBlockEntity.INPUT_SLOT).isEmpty(),
                            "[装罐机] 灌满后输入槽应被消耗");
                    helper.succeed();
                });
            });
        });
    }

    private static AkaishiFuelCannerBlockEntity asCanner(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos) instanceof AkaishiFuelCannerBlockEntity be ? be : null;
    }

    private static AkaishiFuelMixerBlockEntity asMixer(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos) instanceof AkaishiFuelMixerBlockEntity be ? be : null;
    }
}
