package com.example.template.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

import java.util.List;

/**
 * 燃料罐：携带液体燃料的容器，最大 10L（10000mb）。
 * 液体以物品 NBT 持久化（液体 id + 数量）；固定不可堆叠（1.20.1 无按 NBT 动态堆叠 API，
 * 与各类能量电池/储液容器一致）。悬停 tooltip 显示罐内燃料与剩余量，右键弹出实时提示；
 * 不可像桶一样倾倒液体（无放置逻辑）。由燃料装罐机充装。
 */
public class ChishiFuelCellItem extends Item {

    /** 燃料罐最大容量（mb），10L */
    public static final int CAPACITY = 10_000;
    /** 物品 NBT 键：罐内液体注册 id */
    private static final String TAG_FLUID = "FuelLiquidId";
    /** 物品 NBT 键：罐内液体量（mb） */
    private static final String TAG_AMOUNT = "FuelAmount";

    public ChishiFuelCellItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** 罐内液体 id（空罐返回空串） */
    public static String getLiquidId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_FLUID) ? tag.getString(TAG_FLUID) : "";
    }

    /** 罐内液体量（mb），无 NBT 视为空罐 */
    public static int getAmount(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_AMOUNT) ? tag.getInt(TAG_AMOUNT) : 0;
    }

    /** 罐内液体，空罐返回 null */
    public static Fluid getFluid(ItemStack stack) {
        String id = getLiquidId(stack);
        if (id.isEmpty()) {
            return null;
        }
        return BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(id));
    }

    /** 写入液体与数量，自动夹取到 [0, CAPACITY] */
    public static void setFluid(ItemStack stack, Fluid fluid, int amount) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_FLUID, BuiltInRegistries.FLUID.getKey(fluid).toString());
        tag.putInt(TAG_AMOUNT, Math.max(0, Math.min(amount, CAPACITY)));
    }

    /** 罐内液体显示名（无液体返回占位文本） */
    public static Component fluidName(ItemStack stack) {
        Fluid fluid = getFluid(stack);
        if (fluid == null) {
            return Component.translatable("gui.template_mod.fuel_cell.none");
        }
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
        return Component.translatable("fluid." + key.getNamespace() + "." + key.getPath());
    }

    /** 是否空罐（无液体或液体量为 0） */
    public static boolean isEmpty(ItemStack stack) {
        return getAmount(stack) <= 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (isEmpty(stack)) {
            tooltip.add(Component.translatable("gui.template_mod.fuel_cell.empty"));
        } else {
            tooltip.add(Component.translatable("gui.template_mod.fuel_cell.liquid",
                    fluidName(stack), getAmount(stack), CAPACITY));
        }
    }

    /** 右键弹出罐内燃料状态提示（仅服务端广播给本人） */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (isEmpty(stack)) {
                player.displayClientMessage(Component.translatable("gui.template_mod.fuel_cell.status_empty"), true);
            } else {
                player.displayClientMessage(Component.translatable("gui.template_mod.fuel_cell.status",
                        fluidName(stack), getAmount(stack), CAPACITY), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
