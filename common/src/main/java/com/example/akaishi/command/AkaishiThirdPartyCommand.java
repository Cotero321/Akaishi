package com.example.akaishi.command;

import com.example.akaishi.craft.MachineProcessEnergy;
import com.example.akaishi.craft.thirdparty.ThirdPartyProcesses;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 第三方工序兼容的诊断指令：{@code /akaishi_thirdparty list|scan}。
 * <p>
 * <b>为什么需要它</b>（P1 的"随包引导"）：第三方工序的<b>精确档</b>要求整合包在
 * {@code data/<命名空间>/third_party_process/*.json} 里声明"哪个配方类型由哪些机器方块提供、耗多少能、跑多久"。
 * 但作者无从知道"我这套整合包里到底有哪些第三方配方类型"，于是只能一直停在粗粒度档
 *（能耗按 0 算、耗时按默认 100 tick，且不校验机器）—— 声明表写不出来，精确档就等于不存在。
 * <p>
 * 本指令把那份"待声明清单"直接算出来给他：扫原版 {@code RecipeManager}，凡不属于本模组机器族、
 * 又不属于原版免机台类型的配方类型，就是需要声明的第三方工序（附带配方数与示例产物，便于对号入座）。
 * <p><b>只读</b>：不改任何状态，也不需要玩家在场（控制台可用）。
 */
public final class AkaishiThirdPartyCommand {

    /** scan 里"未声明"一栏最多列几条：聊天栏不是日志，刷屏只会让人放弃看 */
    private static final int MAX_UNDECLARED_LINES = 20;

    private AkaishiThirdPartyCommand() {
    }

    /** 注册指令树（由 {@code ModCommands.build} 调用） */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("akaishi_thirdparty")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("list").executes(AkaishiThirdPartyCommand::list))
                .then(Commands.literal("scan").executes(AkaishiThirdPartyCommand::scan)));
    }

    /**
     * 已声明条目一览（作者自检）：声明是否真的被载入、成本与耗时是否如预期。
     * <p>空表也要明确回一句 —— "什么都没显示"与"没载入"在诊断时是两件事。
     */
    private static int list(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Collection<ThirdPartyProcesses.Entry> entries = ThirdPartyProcesses.entries();
        source.sendSuccess(() -> Component.translatable("commands.akaishi.thirdparty.declared.header",
                entries.size()), false);
        for (ThirdPartyProcesses.Entry entry : entries) {
            source.sendSuccess(() -> Component.translatable("commands.akaishi.thirdparty.declared.line",
                    entry.processId().toString(), entry.blocks().size(), entry.chishi(), entry.ticks()), false);
        }
        return entries.size();
    }

    /**
     * 扫配方表，把第三方配方类型分成「已声明 / 未声明」两栏。
     * <p>
     * 判定用的是内核那份唯一判据 {@link MachineProcessEnergy#isThirdPartyProcess}，
     * 与规划器/准入判定同源 —— 否则这份清单会与"实际能不能跑"对不上。
     */
    private static int scan(CommandContext<CommandSourceStack> ctx) {
        RecipeManager manager = ctx.getSource().getServer().getRecipeManager();
        Map<RecipeType<?>, Integer> counts = new HashMap<>();
        Map<RecipeType<?>, Item> samples = new HashMap<>();
        for (Recipe<?> recipe : manager.getRecipes()) {
            RecipeType<?> type = recipe.getType();
            if (!MachineProcessEnergy.isThirdPartyProcess(type)) {
                continue;
            }
            counts.merge(type, 1, Integer::sum);
            if (!samples.containsKey(type)) {
                ItemStack result = recipe.getResultItem(ctx.getSource().getServer().registryAccess());
                if (!result.isEmpty()) {
                    samples.put(type, result.getItem());
                }
            }
        }
        List<RecipeType<?>> declared = new ArrayList<>();
        List<RecipeType<?>> undeclared = new ArrayList<>();
        for (RecipeType<?> type : counts.keySet()) {
            if (ThirdPartyProcesses.isDeclared(type)) {
                declared.add(type);
            } else {
                undeclared.add(type);
            }
        }
        // 输出顺序固定：注册表遍历序不稳定，两次执行的行序不同会让人以为数据变了
        Comparator<RecipeType<?>> byId = Comparator.comparing(type -> {
            ResourceLocation id = BuiltInRegistries.RECIPE_TYPE.getKey(type);
            return id == null ? "" : id.toString();
        });
        declared.sort(byId);
        undeclared.sort(byId);

        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.translatable("commands.akaishi.thirdparty.scan.header",
                counts.size(), declared.size(), undeclared.size()), false);
        for (RecipeType<?> type : declared) {
            ResourceLocation id = BuiltInRegistries.RECIPE_TYPE.getKey(type);
            int recipes = counts.getOrDefault(type, 0);
            source.sendSuccess(() -> Component.translatable("commands.akaishi.thirdparty.scan.declared",
                    id == null ? "?" : id.toString(), recipes), false);
        }
        int shown = Math.min(undeclared.size(), MAX_UNDECLARED_LINES);
        for (int i = 0; i < shown; i++) {
            RecipeType<?> type = undeclared.get(i);
            ResourceLocation id = BuiltInRegistries.RECIPE_TYPE.getKey(type);
            int recipes = counts.getOrDefault(type, 0);
            Item sample = samples.get(type);
            String sampleName = sample == null ? "-" : sample.getDescription().getString();
            source.sendSuccess(() -> Component.translatable("commands.akaishi.thirdparty.scan.undeclared",
                    id == null ? "?" : id.toString(), recipes, sampleName), false);
        }
        if (undeclared.size() > shown) {
            int rest = undeclared.size() - shown;
            source.sendSuccess(() -> Component.translatable("commands.akaishi.thirdparty.scan.more", rest), false);
        }
        if (!undeclared.isEmpty()) {
            // 骨架刻意逐字段给全：作者照着填最省事，也避免解析器逐条警告"缺 process / 没有有效 blocks"
            source.sendSuccess(() -> Component.translatable("commands.akaishi.thirdparty.scan.skeleton"), false);
        }
        return counts.size();
    }
}
