package com.example.akaishi.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.akaishi.api.value.IValueService;
import com.example.akaishi.api.value.ValueServices;
import com.example.akaishi.value.ValueReloadHooks;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 估值内核诊断指令：{@code /akaishi_value hand|reload|top [count]}。
 *
 * <p>仅管理员可见，用于核对「估值是否按预期生效」：手持取值、强制重建、全库排名。
 */
public final class AkaishiValueCommand {

    private static final int DEFAULT_TOP = 10;
    private static final int MAX_TOP = 50;

    private AkaishiValueCommand() {
    }

    /** 注册指令树（由 {@code ModCommands.build} 调用） */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("akaishi_value")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("hand").executes(AkaishiValueCommand::hand))
                .then(Commands.literal("reload").executes(AkaishiValueCommand::reload))
                .then(Commands.literal("top")
                        .executes(ctx -> top(ctx, DEFAULT_TOP))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, MAX_TOP))
                                .executes(ctx -> top(ctx, IntegerArgumentType.getInteger(ctx, "count"))))));
    }

    /** 手持物品的价值分与造价分 */
    private static int hand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("commands.akaishi.value.hand.empty"));
            return 0;
        }
        IValueService service = ValueServices.get();
        double value = service.itemValue(stack);
        int cost = service.craftingCost(stack.getItem());
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.akaishi.value.hand.result",
                stack.getHoverName(), format(value), cost), false);
        return Command.SINGLE_SUCCESS;
    }

    /** 强制作废快照并重建掉落索引（配方/配置刚改动时用） */
    private static int reload(CommandContext<CommandSourceStack> ctx) {
        ValueReloadHooks.onReload();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.akaishi.value.reload"), true);
        return Command.SINGLE_SUCCESS;
    }

    /** 全注册表按价值分降序取前 N 名 */
    private static int top(CommandContext<CommandSourceStack> ctx, int count) {
        IValueService service = ValueServices.get();
        List<Map.Entry<Item, Double>> ranked = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            double value = service.itemValue(item);
            if (value > 0.0) {
                ranked.add(Map.entry(item, value));
            }
        }
        ranked.sort((left, right) -> Double.compare(right.getValue(), left.getValue()));
        int size = Math.min(count, ranked.size());
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.translatable("commands.akaishi.value.top.header", size), false);
        for (int i = 0; i < size; i++) {
            Map.Entry<Item, Double> entry = ranked.get(i);
            int rank = i + 1;
            source.sendSuccess(() -> Component.translatable("commands.akaishi.value.top.line",
                    rank, entry.getKey().getDescription(), format(entry.getValue())), false);
        }
        return size;
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
